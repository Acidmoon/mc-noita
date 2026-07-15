package com.mcnoita.entity;

import com.mcnoita.spell.NoitaProjectileBehavior;
import com.mcnoita.spell.NoitaProjectilePayload;
import com.mcnoita.spell.NoitaSpellTriggerMode;
import com.mcnoita.spell.NoitaTriggerPlan;
import com.mcnoita.spell.damage.DamageChannel;
import com.mcnoita.spell.damage.DamageProfile;
import com.mcnoita.spell.damage.SpellDamageService;
import com.mcnoita.spell.trigger.CollisionKey;
import com.mcnoita.spell.trigger.ProjectileTerminationCause;
import com.mcnoita.spell.trigger.ReleaseDecision;
import com.mcnoita.spell.trigger.TriggerEvent;
import com.mcnoita.spell.trigger.TriggerPayloadController;
import com.mcnoita.spell.trigger.TriggerPayloadSpawner;
import com.mcnoita.spell.trigger.TriggerRuntimeBudget;
import com.mcnoita.spell.trigger.TriggerRuntimeDiagnostics;
import com.mcnoita.spell.trigger.TriggerRuntimeState;
import com.mcnoita.spell.trigger.TriggerRuntimeStateNbtCodec;
import com.mcnoita.persistence.NoitaNbtLimits;
import com.mcnoita.persistence.NoitaNbtSafety;
import com.mcnoita.persistence.NoitaNbtSchema;
import com.mcnoita.world.mutation.WorldMutationContext;
import com.mcnoita.world.mutation.WorldMutationKind;
import com.mcnoita.world.mutation.WorldMutationPolicy;
import com.mcnoita.world.mutation.WorldMutationService;
import com.mcnoita.world.mutation.WorldQueryService;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Arm;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import java.util.UUID;

public class BombEntity extends LivingEntity {
    private static final int PERSISTENT_PROP_FUSE_TICKS = 20 * 60 * 5;
    private static final int BOMB_CART_FUSE_TICKS = 420;
    private static final int DAMAGE_TRIGGERED_EXPLOSIVE_FUSE_TICKS = PERSISTENT_PROP_FUSE_TICKS;
    private static final float MINE_HEALTH = 10.0f;
    private static final float PIPE_BOMB_HEALTH = 10.0f;
    private static final float PERSISTENT_PROP_HEALTH = 20.0f;

    private static final String ITEM_PATH_KEY = "ItemPath";
    private static final String BEHAVIOR_KEY = "Behavior";
    private static final String EXPLOSION_RADIUS_KEY = "ExplosionRadius";
    private static final String GRAVITY_KEY = "Gravity";
    private static final String DRAG_KEY = "Drag";
    private static final String RENDER_SCALE_KEY = "RenderScale";
    private static final String TRIGGER_PAYLOAD_RELEASED_KEY = "TriggerPayloadReleased";
    private static final String TRIGGER_PAYLOADS_KEY = "TriggerPayloads";
    private static final String FROZEN_PAYLOAD_KEY = "FrozenPayload";
    private static final String TRIGGER_RUNTIME_STATE_KEY = "TriggerRuntimeState";
    private static final String LEGACY_RUNTIME_CATALOG_HASH = "legacy-runtime";
    private static final String OWNER_UUID_KEY = "OwnerUuid";
    private static final UUID UNOWNED_OWNER_UUID = new UUID(0L, 0L);

    private String itemPath = "bomb";
    private NoitaProjectileBehavior behavior = NoitaProjectileBehavior.FUSED_EXPLOSIVE;
    private float explosionRadius = 4.0f;
    private float gravity = 0.05f;
    private float drag = 0.99f;
    private float renderScale = 1.0f;
    private boolean friendlyFire;
    private int fuseTicks = 1;
    private LivingEntity caster;
    private UUID casterUuid;
    private boolean exploded;
    private NoitaProjectilePayload frozenPayload;
    private NoitaTriggerPlan triggerPlan = NoitaTriggerPlan.none("root");
    private TriggerPayloadController triggerPayloadController = new TriggerPayloadController(
        this.triggerPlan, TriggerRuntimeState.fresh(TriggerRuntimeBudget.DEFAULT)
    );
    private String activeCollisionSignature;

    public BombEntity(EntityType<? extends BombEntity> entityType, World world) {
        super(entityType, world);
    }

    public BombEntity(World world, LivingEntity caster, float explosionRadius, int lifetimeTicks) {
        this(world, caster, "bomb", NoitaProjectileBehavior.FUSED_EXPLOSIVE, explosionRadius, 0.05f, 0.99f, 1.0f, lifetimeTicks, List.of());
    }

    public BombEntity(World world, LivingEntity caster, float explosionRadius, int lifetimeTicks, List<NoitaProjectilePayload> triggerPayloads) {
        this(world, caster, "bomb", NoitaProjectileBehavior.FUSED_EXPLOSIVE, explosionRadius, 0.05f, 0.99f, 1.0f, lifetimeTicks, triggerPayloads);
    }

    public BombEntity(World world, LivingEntity caster, NoitaProjectilePayload payload) {
        this(world, caster, payload.itemPath(), payload.behavior(), payload.explosionRadius(), payload.gravity(), payload.drag(),
            payload.renderScale(), payload.lifetimeTicks(), payload.payloads());
        configureFrozenPayload(payload, TriggerRuntimeState.fresh(payload.runtimeBudget()));
    }

    public BombEntity(
        World world,
        LivingEntity caster,
        String itemPath,
        NoitaProjectileBehavior behavior,
        float explosionRadius,
        float gravity,
        float drag,
        float renderScale,
        int lifetimeTicks,
        List<NoitaProjectilePayload> triggerPayloads
    ) {
        super(ModEntities.BOMB_PROJECTILE, world);
        this.caster = caster;
        this.casterUuid = caster == null ? null : caster.getUuid();
        this.itemPath = itemPath == null || itemPath.isBlank() ? "bomb" : itemPath;
        this.behavior = behavior == null ? NoitaProjectileBehavior.FUSED_EXPLOSIVE : behavior;
        this.explosionRadius = Math.max(0.0f, explosionRadius);
        this.gravity = gravity;
        this.drag = Math.max(0.0f, drag);
        this.renderScale = renderScale <= 0.0f ? 1.0f : renderScale;
        NoitaProjectilePayload legacyPayload = new NoitaProjectilePayload(this.itemPath, this.behavior, 0.0f, 0.0f,
            Math.max(1, lifetimeTicks), 0, this.explosionRadius, 0.0f, 0.0f, this.gravity, this.drag, 0.99f,
            this.renderScale, 0.0f, false, false, 1, 0.0f, NoitaSpellTriggerMode.EXPIRATION, 0, 0,
            List.of(), triggerPayloads == null ? List.of() : triggerPayloads);
        configureFrozenPayload(legacyPayload, TriggerRuntimeState.fresh(legacyPayload.runtimeBudget()));
        this.fuseTicks = getInitialFuseTicks(this.itemPath, lifetimeTicks);
        float maxHp = getInitialHealth(this.itemPath, this.fuseTicks);
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(maxHp);
        this.setHealth(maxHp);
    }

    public static DefaultAttributeContainer.Builder createBombAttributes() {
        return LivingEntity.createLivingAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 1200.0);
    }

    public String getItemPath() {
        return itemPath;
    }

    public float getRenderScale() {
        return renderScale;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && !this.exploded && !this.isRemoved() && this.isAlive()) {
            if (this.triggerPlan.mode() == NoitaSpellTriggerMode.TIMER
                && !this.triggerPayloadController.state().timerExpired()) {
                this.triggerPayloadController.advanceTimer();
            }
            if (this.triggerPlan.mode() == NoitaSpellTriggerMode.TIMER
                && !this.triggerPayloadController.state().timerExpired()
                && this.triggerPayloadController.state().timerElapsedTicks() >= this.triggerPlan.timerDelayTicks()) {
                releaseTriggerPayload(TriggerEvent.timerExpired());
            }
            if (this.behavior == NoitaProjectileBehavior.MINE && this.age % 20 == 0) {
                primeNearbyMortal();
            }
            if (isPersistentExplosiveProp() && this.age < this.fuseTicks) {
                return;
            }
            float hp = this.getHealth() - 1.0f;
            if (hp <= 0.0f) {
                explode(ProjectileTerminationCause.NATURAL_EXPIRY);
            } else {
                this.setHealth(hp);
            }
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.exploded || this.isRemoved() || this.getWorld().isClient || !this.isAlive()) {
            return false;
        }
        float hp = this.getHealth() - amount;
        if (hp <= 0.0f) {
            explode(ProjectileTerminationCause.KILLED);
        } else {
            this.setHealth(hp);
        }
        return true;
    }

    private void explode(ProjectileTerminationCause terminationCause) {
        if (this.exploded || this.isRemoved()) {
            return;
        }
        this.exploded = true;
        World world = this.getWorld();
        if (!world.isClient) {
            releaseTriggerPayload(TriggerEvent.terminated(terminationCause));
            if (this.explosionRadius > 0.0f) {
                mutateExplosion(this.getPos(), this.explosionRadius, true);
            }
            applyNoitaExplosionAftermath(world);
        }
        this.discard();
    }

    @Override
    public void travel(Vec3d movementInput) {
        if (this.isOnGround()) {
            activeCollisionSignature = null;
            if (this.behavior == NoitaProjectileBehavior.MINE) {
                this.setVelocity(Vec3d.ZERO);
                return;
            }
            Vec3d groundVelocity = this.getVelocity();
            this.setVelocity(groundVelocity.x * 0.35, 0.0, groundVelocity.z * 0.35);
            return;
        }
        Vec3d velocity = this.getVelocity();
        Vec3d nextVelocity = new Vec3d(velocity.x * this.drag, velocity.y - this.gravity, velocity.z * this.drag);
        this.setVelocity(nextVelocity);
        if (!this.getWorld().isClient && !handleMovementCollision(nextVelocity)) {
            // A policy-denied ray/query has not proved this path safe. Do not
            // advance into an unloaded or over-budget chunk without collision
            // checks, and retry only after the next normal server tick.
            this.setVelocity(Vec3d.ZERO);
            return;
        }
        this.move(MovementType.SELF, nextVelocity);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canHit() {
        return true;
    }

    @Override
    protected void tickCramming() {
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getEquippedStack(net.minecraft.entity.EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void equipStack(net.minecraft.entity.EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }

    public LivingEntity getCaster() {
        if (this.caster == null && this.casterUuid != null && this.getWorld() instanceof ServerWorld serverWorld) {
            Entity entity = serverWorld.getEntity(this.casterUuid);
            if (entity instanceof LivingEntity living) {
                this.caster = living;
            }
        }
        return this.caster;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        NoitaNbtSchema.writeCurrentVersion(nbt);
        nbt.putString(ITEM_PATH_KEY, this.itemPath);
        nbt.putString(BEHAVIOR_KEY, this.behavior.name());
        nbt.putFloat(EXPLOSION_RADIUS_KEY, this.explosionRadius);
        nbt.putFloat(GRAVITY_KEY, this.gravity);
        nbt.putFloat(DRAG_KEY, this.drag);
        nbt.putFloat(RENDER_SCALE_KEY, this.renderScale);
        NoitaProjectilePayload payload = bindLegacyRuntimeIdentity(currentFrozenPayload())
            .withRuntimeBudget(this.triggerPayloadController.state().remainingBudget());
        nbt.put(FROZEN_PAYLOAD_KEY, payload.toNbt());
        nbt.put(TRIGGER_RUNTIME_STATE_KEY, TriggerRuntimeStateNbtCodec.toNbt(this.triggerPayloadController.state()));
        // v4 always writes an owner marker. The zero UUID is an explicit
        // unowned state, distinct from a missing/corrupt ownership field.
        nbt.putString(OWNER_UUID_KEY, (this.casterUuid == null ? UNOWNED_OWNER_UUID : this.casterUuid).toString());
        // v4 stores one authoritative tree. Writing the legacy flattened copy
        // here could double a legal frozen tree beyond the entity NBT limit.
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        int storedSchemaVersion = NoitaNbtSchema.readStoredVersion(nbt);
        // v3 introduced the frozen tree contract. Preserve that strictness
        // after a v3 entity is migrated to the v4 damage-profile schema.
        boolean frozenSchema = storedSchemaVersion >= 3;
        boolean hasFrozenPayload = nbt.contains(FROZEN_PAYLOAD_KEY, NbtElement.COMPOUND_TYPE);
        if (nbt.getSizeInBytes() > NoitaNbtLimits.MAX_ENTITY_NBT_BYTES
            || storedSchemaVersion < 0
            || !NoitaNbtSchema.migrateToCurrent(nbt, NoitaNbtSchema.Kind.ENTITY)
            || !NoitaNbtSafety.validateTree(nbt, NoitaNbtLimits.MAX_ENTITY_NBT_DEPTH, NoitaNbtLimits.MAX_ENTITY_NBT_NODES, NoitaNbtLimits.MAX_NBT_LIST_ENTRIES)
            || (!hasFrozenPayload && !NoitaNbtSafety.hasValidEnumIfPresent(nbt, BEHAVIOR_KEY, NoitaProjectileBehavior.class))) {
            this.discard();
            return;
        }
        if (frozenSchema && !nbt.contains(OWNER_UUID_KEY, NbtElement.STRING_TYPE)) {
            this.discard();
            return;
        }
        if (nbt.contains(OWNER_UUID_KEY, NbtElement.STRING_TYPE)) {
            try {
                UUID ownerUuid = UUID.fromString(nbt.getString(OWNER_UUID_KEY));
                this.casterUuid = UNOWNED_OWNER_UUID.equals(ownerUuid) ? null : ownerUuid;
            } catch (IllegalArgumentException invalidOwner) {
                this.discard();
                return;
            }
        }
        NoitaProjectilePayload payload;
        if (hasFrozenPayload) {
            payload = NoitaProjectilePayload.tryFromNbt(nbt.getCompound(FROZEN_PAYLOAD_KEY)).orElse(null);
            if (payload == null) {
                this.discard();
                return;
            }
        } else {
            if (frozenSchema) {
                // A v3+ Bomb without the full frozen tree must not regain a
                // fresh budget or reinterpret mutable legacy projection data.
                this.discard();
                return;
            }
            try {
                applyLegacyProjection(nbt);
                this.fuseTicks = getInitialFuseTicks(this.itemPath, Math.round(this.getMaxHealth()));
                List<NoitaProjectilePayload> legacyPayloads = NoitaProjectilePayload.tryFromNbtList(
                    nbt.getList(TRIGGER_PAYLOADS_KEY, NbtElement.COMPOUND_TYPE)
                ).orElse(null);
                if (legacyPayloads == null) {
                    this.discard();
                    return;
                }
                payload = new NoitaProjectilePayload(this.itemPath, this.behavior, 0.0f, 0.0f,
                    Math.max(1, this.fuseTicks), 0, this.explosionRadius, (float) this.getVelocity().length(), 0.0f,
                    this.gravity, this.drag, 0.99f, this.renderScale, 0.0f, false, false, 1, 0.0f,
                    NoitaSpellTriggerMode.EXPIRATION, 0, 0, List.of(), legacyPayloads);
            } catch (IllegalArgumentException invalidLegacyPayload) {
                this.discard();
                return;
            }
        }
        if (frozenSchema && !nbt.contains(TRIGGER_RUNTIME_STATE_KEY, NbtElement.COMPOUND_TYPE)) {
            this.discard();
            return;
        }
        TriggerRuntimeState runtimeState = nbt.contains(TRIGGER_RUNTIME_STATE_KEY, NbtElement.COMPOUND_TYPE)
            ? (frozenSchema
                ? TriggerRuntimeStateNbtCodec.tryFromCurrentNbt(nbt.getCompound(TRIGGER_RUNTIME_STATE_KEY), payload.runtimeBudget())
                : TriggerRuntimeStateNbtCodec.tryFromNbt(nbt.getCompound(TRIGGER_RUNTIME_STATE_KEY), payload.runtimeBudget()))
                .orElse(null)
            : TriggerRuntimeState.fresh(payload.runtimeBudget());
        if (runtimeState == null) {
            this.discard();
            return;
        }
        if (nbt.getBoolean("G03LegacyTriggerReleased") || nbt.getBoolean(TRIGGER_PAYLOAD_RELEASED_KEY)) {
            runtimeState = runtimeState.markInert();
        }
        configureFrozenPayload(payload, runtimeState);
    }

    private void applyLegacyProjection(NbtCompound nbt) {
        if (nbt.contains(ITEM_PATH_KEY, NbtElement.STRING_TYPE)) {
            this.itemPath = nbt.getString(ITEM_PATH_KEY);
        }
        if (nbt.contains(BEHAVIOR_KEY, NbtElement.STRING_TYPE)) {
            this.behavior = NoitaProjectileBehavior.valueOf(nbt.getString(BEHAVIOR_KEY));
        }
        if (nbt.contains(EXPLOSION_RADIUS_KEY, NbtElement.NUMBER_TYPE)) {
            this.explosionRadius = Math.max(0.0f, nbt.getFloat(EXPLOSION_RADIUS_KEY));
        }
        if (nbt.contains(GRAVITY_KEY, NbtElement.NUMBER_TYPE)) {
            this.gravity = nbt.getFloat(GRAVITY_KEY);
        }
        if (nbt.contains(DRAG_KEY, NbtElement.NUMBER_TYPE)) {
            this.drag = Math.max(0.0f, nbt.getFloat(DRAG_KEY));
        }
        if (nbt.contains(RENDER_SCALE_KEY, NbtElement.NUMBER_TYPE)) {
            this.renderScale = Math.max(0.1f, nbt.getFloat(RENDER_SCALE_KEY));
        }
    }

    private void releaseTriggerPayload(TriggerEvent event) {
        ReleaseDecision decision = this.triggerPayloadController.accept(event);
        if (!decision.shouldRelease()) {
            TriggerRuntimeDiagnostics.reportBudgetExhaustion(this.getWorld(), getCaster(), currentFrozenPayload().executionIdentity(),
                this.triggerPlan.mode(), this.triggerPayloadController.state(), decision.budgetExhaustion());
            return;
        }
        TriggerPayloadSpawner.spawn(this.getWorld(), getCaster(), this.getPos(), getPayloadDirection(), decision);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (reason == Entity.RemovalReason.KILLED && !this.getWorld().isClient && !this.isRemoved()) {
            // /kill bypasses damage(), so preserve Expiration payload semantics
            // without treating unload or administrative discard as a collision.
            releaseTriggerPayload(TriggerEvent.terminated(ProjectileTerminationCause.KILLED));
        }
        super.remove(reason);
    }

    /**
     * Bombs are LivingEntity-based and do not receive ProjectileEntity's
     * onEntityHit/onCollision callbacks. Detect one swept collision before
     * movement so block and entity Hits share one controller event.
     */
    private boolean handleMovementCollision(Vec3d movement) {
        CollisionProbe probe = findMovementCollision(movement);
        if (!probe.admitted()) {
            activeCollisionSignature = null;
            return false;
        }
        HitResult collision = probe.collision();
        if (collision == null) {
            activeCollisionSignature = null;
            return true;
        }

        submitCollision(collisionKey(collision));
        return true;
    }

    private void submitCollision(CollisionKey key) {
        String signature = collisionSignature(key);
        if (signature.equals(activeCollisionSignature)) {
            return;
        }

        // Commit the bounded contact marker before spawning child entities so a
        // reentrant callback cannot release the same frozen tree twice.
        activeCollisionSignature = signature;
        releaseTriggerPayload(TriggerEvent.collision(key));
    }

    private CollisionProbe findMovementCollision(Vec3d movement) {
        if (movement.lengthSquared() <= 1.0E-8) {
            return CollisionProbe.admitted(null);
        }
        Vec3d start = this.getPos();
        Vec3d end = start.add(movement);
        // A denied ray must also suppress entity collision for this movement.
        // Otherwise an unloaded/over-budget block path could still release a
        // Hit payload through the separate entity query below.
        Optional<BlockHitResult> blockProbe = mutationContext()
            .flatMap(context -> WorldQueryService.raycast(context, this, start, end));
        if (blockProbe.isEmpty()) {
            return CollisionProbe.denied();
        }
        CollisionProbe entityProbe = findEntityCollision(start, end, movement);
        if (!entityProbe.admitted()) {
            return CollisionProbe.denied();
        }
        BlockHitResult blockHit = blockProbe.get();
        EntityHitResult entityHit = entityProbe.collision() instanceof EntityHitResult hit ? hit : null;

        if (entityHit == null) {
            return CollisionProbe.admitted(blockHit.getType() == HitResult.Type.BLOCK ? blockHit : null);
        }
        if (blockHit.getType() != HitResult.Type.BLOCK
            || entityHit.getPos().squaredDistanceTo(start) <= blockHit.getPos().squaredDistanceTo(start)) {
            return CollisionProbe.admitted(entityHit);
        }
        return CollisionProbe.admitted(blockHit);
    }

    private CollisionProbe findEntityCollision(Vec3d start, Vec3d end, Vec3d movement) {
        Box sweptBox = this.getBoundingBox().stretch(movement).expand(0.3);
        Optional<List<Entity>> candidates = mutationContext().flatMap(context -> WorldQueryService.tryEntities(
            context, this, sweptBox, this::isValidCollisionEntity, WorldMutationPolicy.MAX_QUERY_RESULTS
        ));
        if (candidates.isEmpty()) {
            return CollisionProbe.denied();
        }
        Entity closest = null;
        Vec3d closestHit = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity candidate : candidates.get()) {
            Vec3d hit = candidate.getBoundingBox().expand(0.3).raycast(start, end).orElse(null);
            if (hit == null) {
                continue;
            }
            double distance = hit.squaredDistanceTo(start);
            if (distance < closestDistance) {
                closest = candidate;
                closestHit = hit;
                closestDistance = distance;
            }
        }
        return CollisionProbe.admitted(closest == null ? null : new EntityHitResult(closest, closestHit));
    }

    private boolean isValidCollisionEntity(Entity candidate) {
        return candidate != this && !candidate.isRemoved() && candidate.isAlive() && !candidate.isSpectator()
            && candidate != getCaster();
    }

    /** Distinguishes a legal miss from a policy-denied collision probe. */
    private record CollisionProbe(boolean admitted, HitResult collision) {
        private static CollisionProbe admitted(HitResult collision) {
            return new CollisionProbe(true, collision);
        }

        private static CollisionProbe denied() {
            return new CollisionProbe(false, null);
        }
    }

    private CollisionKey collisionKey(HitResult hitResult) {
        if (hitResult instanceof EntityHitResult entityHit) {
            return entityCollisionKey(entityHit.getEntity());
        }
        String target = "block:" + ((BlockHitResult) hitResult).getBlockPos().asLong();
        String face = hitResult instanceof BlockHitResult blockHit ? blockHit.getSide().name() : "ENTITY";
        return new CollisionKey(this.getWorld().getTime(), target, face, this.triggerPlan.nodePath());
    }

    private CollisionKey entityCollisionKey(Entity entity) {
        return new CollisionKey(this.getWorld().getTime(), "entity:" + entity.getUuidAsString(), "ENTITY",
            this.triggerPlan.nodePath());
    }

    private static String collisionSignature(CollisionKey key) {
        return key.target() + "|" + key.face() + "|" + key.nodePath();
    }

    private void configureFrozenPayload(NoitaProjectilePayload payload, TriggerRuntimeState runtimeState) {
        payload = bindLegacyRuntimeIdentity(payload);
        this.frozenPayload = payload;
        applyFrozenPayloadMechanics(payload);
        this.triggerPlan = payload.triggerPlan();
        this.triggerPayloadController = new TriggerPayloadController(this.triggerPlan, runtimeState);
        this.activeCollisionSignature = runtimeState.latestCollision() == null ? null : collisionSignature(runtimeState.latestCollision());
    }

    /** Legacy dynamic spawns acquire one server identity before they can be persisted as v4. */
    private NoitaProjectilePayload bindLegacyRuntimeIdentity(NoitaProjectilePayload payload) {
        if (payload.executionIdentity().isBound() || this.getWorld().isClient) {
            return payload;
        }
        return payload.withTreeIdentity(UUID.randomUUID(), 0L, LEGACY_RUNTIME_CATALOG_HASH);
    }

    /** v4 keeps top-level fields only as diagnostics; frozen payload mechanics are authoritative after reload. */
    private void applyFrozenPayloadMechanics(NoitaProjectilePayload payload) {
        this.itemPath = payload.itemPath();
        this.behavior = payload.behavior();
        this.explosionRadius = payload.explosionRadius();
        this.gravity = payload.gravity();
        this.drag = payload.drag();
        this.renderScale = payload.renderScale();
        this.friendlyFire = payload.friendlyFire();
        this.fuseTicks = getInitialFuseTicks(this.itemPath, payload.lifetimeTicks());
    }

    private NoitaProjectilePayload currentFrozenPayload() {
        if (this.frozenPayload != null) {
            return this.frozenPayload;
        }
        return new NoitaProjectilePayload(this.itemPath, this.behavior, 0.0f, 0.0f, Math.max(1, this.fuseTicks),
            0, this.explosionRadius, (float) this.getVelocity().length(), 0.0f, this.gravity, this.drag, 0.99f,
            this.renderScale, 0.0f, this.friendlyFire, false, 1, 0.0f, this.triggerPlan.mode(),
            this.triggerPlan.timerDelayTicks(), 0, List.of(),
            this.triggerPlan.payloads().stream().flatMap(payload -> payload.projectiles().stream()).toList());
    }

    /** Destructive bomb aftermath is denied when its persisted owner is offline. */
    private Optional<WorldMutationContext> mutationContext() {
        return WorldMutationContext.forEntity(this, getCaster(), this.casterUuid, currentFrozenPayload().executionIdentity());
    }

    private boolean mutateExplosion(Vec3d center, float radius, boolean fire) {
        NoitaProjectilePayload payload = currentFrozenPayload();
        DamageProfile profile = DamageProfile.of(DamageChannel.EXPLOSION, payload.damageProfile().totalDamage());
        return mutationContext().map(context -> WorldMutationService.explode(context, this, getCaster(), center, radius,
            profile, this.friendlyFire, fire, true)).orElse(false);
    }

    private boolean mutateBreak(BlockPos pos, boolean drop) {
        return mutationContext().map(context -> WorldMutationService.breakBlock(context, pos, drop, this)).orElse(false);
    }

    private boolean mutateReplace(BlockPos pos, BlockState state, int flags) {
        return mutationContext().map(context -> WorldMutationService.replaceBlock(context, pos, state, flags)).orElse(false);
    }

    private List<Entity> queryEntities(Box area, Predicate<? super Entity> predicate) {
        return mutationContext().map(context -> WorldQueryService.entities(context, this, area, predicate, 128)).orElse(List.of());
    }

    /** World reads must not load chunks or escape the projectile's frozen dimension binding. */
    private Optional<BlockState> queryBlockState(BlockPos pos) {
        return mutationContext().flatMap(context -> WorldQueryService.blockState(context, pos, WorldMutationKind.BLOCK_CHECK));
    }

    /**
     * Bomb aftermath is hostile by default. A missing, dead, removed, or
     * dimension-mismatched owner cannot use delayed status effects to bypass
     * the same self/team policy applied to direct spell damage.
     */
    private boolean canHarmTarget(Entity target) {
        LivingEntity resolvedCaster = getCaster();
        if (this.casterUuid == null || resolvedCaster == null || resolvedCaster.isRemoved()
            || !resolvedCaster.isAlive() || resolvedCaster.getWorld() != this.getWorld()) {
            return false;
        }
        return SpellDamageService.isTargetAllowed(target, resolvedCaster, this.friendlyFire);
    }

    private Vec3d getPayloadDirection() {
        Vec3d velocity = this.getVelocity();
        if (velocity.lengthSquared() > 1.0E-6) {
            return velocity.normalize();
        }
        LivingEntity resolvedCaster = getCaster();
        return resolvedCaster == null ? this.getRotationVec(1.0f) : resolvedCaster.getRotationVec(1.0f);
    }

    private void applyNoitaExplosionAftermath(World world) {
        String path = normalizedItemPath();
        if (path.equals("firebomb")) {
            igniteAround(world, 4, 8);
        } else if (path.equals("mine") || path.equals("mine_death_trigger")) {
            igniteAround(world, 3, 5);
        } else if (path.equals("pipe_bomb") || path.equals("pipe_bomb_death_trigger")) {
            placeGlowstoneRing(world, 1);
        } else if (path.equals("propane_tank")) {
            igniteAround(world, 5, 10);
            freezeNearby(5);
        } else if (path.equals("bomb_holy") || path.equals("bomb_holy_giga")) {
            holyFlash(path.equals("bomb_holy_giga") ? 8.0 : 5.5);
            placeGlowstoneRing(world, path.equals("bomb_holy_giga") ? 4 : 2);
        } else if (path.equals("nuke") || path.equals("nuke_giga")) {
            igniteAround(world, path.equals("nuke_giga") ? 9 : 7, 12);
            vaporizeTerrain(world, path.equals("nuke_giga") ? 6 : 4);
            poisonNearby(path.equals("nuke_giga") ? 10.0 : 7.0);
        } else if (path.equals("tntbox") || path.equals("tntbox_big") || path.equals("bomb_cart")) {
            scatterTntMaterial(world, path.equals("tntbox_big") ? 3 : 2);
        } else if (path.equals("glitter_bomb")) {
            placeGlowstoneRing(world, 2);
        }
    }

    private void igniteAround(World world, int radius, int seconds) {
        Box area = this.getBoundingBox().expand(radius);
        for (Entity entity : queryEntities(area, entity -> entity instanceof LivingEntity)) {
            if (canHarmTarget(entity)) {
                entity.setOnFireFor(seconds);
            }
        }
        BlockPos center = this.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -1, -radius), center.add(radius, 1, radius))) {
            if (this.random.nextInt(4) != 0) {
                continue;
            }
            Optional<BlockState> currentState = queryBlockState(pos);
            if (currentState.isEmpty() || !currentState.get().isAir()) {
                continue;
            }
            BlockState fire = AbstractFireBlock.getState(world, pos);
            if (fire.canPlaceAt(world, pos)) {
                mutateReplace(pos, fire, 11);
            }
        }
    }

    private void freezeNearby(int radius) {
        Box area = this.getBoundingBox().expand(radius);
        for (Entity entity : queryEntities(area, entity -> entity instanceof LivingEntity)) {
            if (canHarmTarget(entity)) {
                entity.setFrozenTicks(Math.max(entity.getFrozenTicks(), 180));
            }
        }
    }

    private void holyFlash(double radius) {
        Box area = this.getBoundingBox().expand(radius);
        for (Entity entity : queryEntities(area, entity -> entity instanceof LivingEntity)) {
            if (entity instanceof LivingEntity living && canHarmTarget(entity)) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 80, 0), this);
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1), this);
            }
        }
    }

    private void poisonNearby(double radius) {
        Box area = this.getBoundingBox().expand(radius);
        for (Entity entity : queryEntities(area, entity -> entity instanceof LivingEntity)) {
            if (entity instanceof LivingEntity living && canHarmTarget(entity)) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 160, 1), this);
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 0), this);
            }
        }
    }

    private void vaporizeTerrain(World world, int radius) {
        BlockPos center = this.getBlockPos();
        int radiusSquared = radius * radius;
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))) {
            if (pos.getSquaredDistance(center) > radiusSquared) {
                continue;
            }
            Optional<BlockState> state = queryBlockState(pos);
            if (state.isEmpty()) {
                continue;
            }
            float hardness = state.get().getHardness(world, pos);
            if (!state.get().isAir() && hardness >= 0.0f && hardness <= 20.0f && this.random.nextInt(3) != 0) {
                mutateBreak(pos, false);
            }
        }
    }

    private void placeGlowstoneRing(World world, int radius) {
        BlockPos center = this.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -1, -radius), center.add(radius, 1, radius))) {
            if (this.random.nextInt(5) != 0) {
                continue;
            }
            Optional<BlockState> currentState = queryBlockState(pos);
            Optional<BlockState> supportState = queryBlockState(pos.down());
            if (currentState.isEmpty() || supportState.isEmpty() || !currentState.get().isAir()) {
                continue;
            }
            if (supportState.get().isSideSolidFullSquare(world, pos.down(), Direction.UP)) {
                mutateReplace(pos, Blocks.GLOWSTONE.getDefaultState(), 11);
            }
        }
    }

    private void scatterTntMaterial(World world, int radius) {
        BlockPos center = this.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, 0, -radius), center.add(radius, 1, radius))) {
            if (this.random.nextInt(6) != 0) {
                continue;
            }
            Optional<BlockState> currentState = queryBlockState(pos);
            Optional<BlockState> supportState = queryBlockState(pos.down());
            if (currentState.isEmpty() || supportState.isEmpty() || !currentState.get().isAir()) {
                continue;
            }
            if (supportState.get().isSideSolidFullSquare(world, pos.down(), Direction.UP)) {
                mutateReplace(pos, Blocks.TNT.getDefaultState(), 11);
            }
        }
    }

    private String normalizedItemPath() {
        return this.itemPath.toLowerCase(Locale.ROOT);
    }

    private int getInitialFuseTicks(String path, int lifetimeTicks) {
        String normalizedPath = path == null ? "" : path.toLowerCase(Locale.ROOT);
        if (normalizedPath.equals("mine")
            || normalizedPath.equals("mine_death_trigger")
            || normalizedPath.equals("pipe_bomb")
            || normalizedPath.equals("pipe_bomb_death_trigger")) {
            return DAMAGE_TRIGGERED_EXPLOSIVE_FUSE_TICKS;
        }
        if (normalizedPath.equals("tntbox") || normalizedPath.equals("tntbox_big")) {
            return PERSISTENT_PROP_FUSE_TICKS;
        }
        if (normalizedPath.equals("bomb_cart")) {
            return BOMB_CART_FUSE_TICKS;
        }
        return Math.max(1, lifetimeTicks);
    }

    private float getInitialHealth(String path, int fuseTicks) {
        String normalizedPath = path == null ? "" : path.toLowerCase(Locale.ROOT);
        if (normalizedPath.equals("mine") || normalizedPath.equals("mine_death_trigger")) {
            return MINE_HEALTH;
        }
        if (normalizedPath.equals("pipe_bomb") || normalizedPath.equals("pipe_bomb_death_trigger")) {
            return PIPE_BOMB_HEALTH;
        }
        if (isPersistentExplosivePropPath(normalizedPath)) {
            return PERSISTENT_PROP_HEALTH;
        }
        return Math.max(1, fuseTicks);
    }

    private boolean isPersistentExplosiveProp() {
        String path = normalizedItemPath();
        return this.behavior == NoitaProjectileBehavior.MINE || isPersistentExplosivePropPath(path);
    }

    private static boolean isPersistentExplosivePropPath(String path) {
        return path.equals("tntbox") || path.equals("tntbox_big") || path.equals("bomb_cart");
    }

    private void primeNearbyMortal() {
        if (!this.isOnGround()) {
            return;
        }
        Box area = this.getBoundingBox().expand(1.8);
        for (Entity entity : queryEntities(area,
            candidate -> candidate instanceof LivingEntity && isValidCollisionEntity(candidate))) {
            if (entity instanceof LivingEntity) {
                submitCollision(entityCollisionKey(entity));
                explode(ProjectileTerminationCause.TERMINAL_COLLISION);
                return;
            }
        }
    }
}

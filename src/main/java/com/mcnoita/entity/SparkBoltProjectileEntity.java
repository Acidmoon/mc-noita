package com.mcnoita.entity;

import com.mcnoita.MCNoita;
import com.mcnoita.item.ModItems;
import com.mcnoita.particle.ModParticles;
import com.mcnoita.spell.NoitaModifierEffect;
import com.mcnoita.spell.NoitaProjectileBehavior;
import com.mcnoita.spell.NoitaProjectilePayload;
import com.mcnoita.spell.NoitaSpellDamage;
import com.mcnoita.spell.NoitaSpellTriggerMode;
import com.mcnoita.world.NoitaTemporaryLightManager;
import java.util.List;
import java.util.Locale;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SparkBoltProjectileEntity extends ThrownItemEntity {
    private static final String ITEM_PATH_KEY = "ItemPath";
    private static final String BEHAVIOR_KEY = "Behavior";
    private static final String DAMAGE_KEY = "Damage";
    private static final String CRITICAL_CHANCE_PERCENT_KEY = "CriticalChancePercent";
    private static final String MAX_AGE_KEY = "MaxAge";
    private static final String TRAIL_LIGHT_STACKS_KEY = "TrailLightStacks";
    private static final String EXPLOSION_RADIUS_KEY = "ExplosionRadius";
    private static final String GRAVITY_KEY = "Gravity";
    private static final String DRAG_KEY = "Drag";
    private static final String BOUNCE_DAMPING_KEY = "BounceDamping";
    private static final String RENDER_SCALE_KEY = "RenderScale";
    private static final String KNOCKBACK_FORCE_KEY = "KnockbackForce";
    private static final String FRIENDLY_FIRE_KEY = "FriendlyFire";
    private static final String PIERCING_KEY = "Piercing";
    private static final String TRIGGER_MODE_KEY = "TriggerMode";
    private static final String TRIGGER_DELAY_TICKS_KEY = "TriggerDelayTicks";
    private static final String BOUNCE_COUNT_KEY = "BounceCount";
    private static final String MODIFIER_EFFECTS_KEY = "ModifierEffects";
    private static final String TRIGGER_PAYLOAD_RELEASED_KEY = "TriggerPayloadReleased";
    private static final String TRIGGER_PAYLOADS_KEY = "TriggerPayloads";
    private static final byte HIT_FLASH_STATUS = 3;
    private static final float CRITICAL_DAMAGE_MULTIPLIER = 5.0f;
    private static final int MAX_BOUNCES = 8;
    private static final int SPELL_EFFECT_INTERVAL_TICKS = 5;

    private String itemPath = "spark_bolt";
    private NoitaProjectileBehavior behavior = NoitaProjectileBehavior.BOLT;
    private float damage = 3.0f;
    private float criticalChancePercent;
    private int maxAge = 60;
    private int trailLightStacks;
    private float explosionRadius;
    private float gravity;
    private float drag = 0.99f;
    private float bounceDamping = 0.65f;
    private float renderScale = 1.0f;
    private float knockbackForce;
    private boolean friendlyFire;
    private boolean piercing;
    private int bounceCount;
    private List<NoitaModifierEffect> modifierEffects = List.of();
    private int bounces;
    private NoitaSpellTriggerMode triggerMode = NoitaSpellTriggerMode.NONE;
    private int triggerDelayTicks;
    private boolean triggerPayloadReleased;
    private List<NoitaProjectilePayload> triggerPayloads = List.of();
    private Vec3d returnAnchor;
    private boolean summonReleased;

    public SparkBoltProjectileEntity(EntityType<? extends SparkBoltProjectileEntity> entityType, World world) {
        super(entityType, world);
        setItemFromPath(this.itemPath);
    }

    public SparkBoltProjectileEntity(World world, LivingEntity owner, NoitaProjectilePayload payload) {
        this(world, owner, payload.itemPath(), payload.behavior(), payload.damage(), payload.criticalChancePercent(), payload.lifetimeTicks(),
            payload.trailLightStacks(), payload.explosionRadius(), payload.gravity(), payload.drag(), payload.bounceDamping(), payload.renderScale(),
            payload.knockbackForce(), payload.friendlyFire(), payload.piercing(), payload.triggerMode(), payload.triggerDelayTicks(), payload.bounceCount(), payload.modifierEffects(),
            payload.payloads());
    }

    public SparkBoltProjectileEntity(
        World world,
        LivingEntity owner,
        String itemPath,
        NoitaProjectileBehavior behavior,
        float damage,
        float criticalChancePercent,
        int maxAge,
        int trailLightStacks,
        float explosionRadius,
        float gravity,
        float drag,
        float bounceDamping,
        float renderScale,
        float knockbackForce,
        boolean friendlyFire,
        boolean piercing,
        NoitaSpellTriggerMode triggerMode,
        int triggerDelayTicks,
        int bounceCount,
        List<NoitaModifierEffect> modifierEffects,
        List<NoitaProjectilePayload> triggerPayloads
    ) {
        super(ModEntities.SPARK_BOLT_PROJECTILE, owner, world);
        this.itemPath = itemPath == null || itemPath.isBlank() ? "spark_bolt" : itemPath;
        this.behavior = behavior == null ? NoitaProjectileBehavior.BOLT : behavior;
        this.damage = Math.max(0.0f, damage);
        this.criticalChancePercent = Math.max(0.0f, criticalChancePercent);
        this.maxAge = Math.max(1, maxAge);
        this.trailLightStacks = Math.max(0, trailLightStacks);
        this.modifierEffects = List.copyOf(modifierEffects);
        this.explosionRadius = hasModifierEffect(NoitaModifierEffect.REMOVE_EXPLOSION) ? 0.0f : Math.max(0.0f, explosionRadius);
        this.gravity = gravity;
        this.drag = Math.max(0.0f, drag);
        this.bounceDamping = Math.max(0.0f, bounceDamping);
        this.renderScale = renderScale <= 0.0f ? 1.0f : renderScale;
        this.knockbackForce = knockbackForce;
        this.friendlyFire = friendlyFire;
        this.piercing = piercing || hasModifierEffect(NoitaModifierEffect.PIERCING_SHOT) || this.behavior.isBeamLike() || itemPathPiercesEntities(this.itemPath);
        this.bounceCount = Math.max(0, bounceCount);
        this.triggerMode = triggerMode == null ? NoitaSpellTriggerMode.NONE : triggerMode;
        this.triggerDelayTicks = Math.max(0, triggerDelayTicks);
        this.triggerPayloads = List.copyOf(triggerPayloads);
        if (hasModifierEffect(NoitaModifierEffect.NOLLA)) {
            this.maxAge = 1;
        }
        if (hasModifierEffect(NoitaModifierEffect.COLOUR_INVIS)) {
            this.setInvisible(true);
        }
        this.setNoGravity(false);
        setItemFromPath(this.itemPath);
    }

    public float getRenderScale() {
        return renderScale;
    }

    public boolean hidesProjectileVisual() {
        return hasModifierEffect(NoitaModifierEffect.COLOUR_INVIS);
    }

    @Override
    public void tick() {
        this.noClip = ignoresWorldCollision();
        applyAmbientBehavior();
        super.tick();

        if (this.age > this.maxAge) {
            if (!this.getWorld().isClient && this.triggerMode == NoitaSpellTriggerMode.DEATH) {
                releaseTriggerPayload();
            }
            if (!this.getWorld().isClient) {
                onLifetimeExpired();
            }
            this.discard();
            return;
        }

        World world = this.getWorld();
        if (world.isClient) {
            spawnTrailParticles();
        } else if (world instanceof ServerWorld serverWorld) {
            if (this.triggerMode == NoitaSpellTriggerMode.TIMER && !this.triggerPayloadReleased && this.age >= this.triggerDelayTicks) {
                releaseTriggerPayload();
            }
            if (this.trailLightStacks > 0 || this.behavior.isBeamLike()) {
                NoitaTemporaryLightManager.illuminateTrail(serverWorld, new Vec3d(this.prevX, this.prevY, this.prevZ), this.getPos(), this.trailLightStacks + 1);
            }
            applyServerTickEffects(serverWorld);
        }
    }

    @Override
    public void handleStatus(byte status) {
        if (status == HIT_FLASH_STATUS) {
            spawnHitFlashParticles();
            return;
        }

        super.handleStatus(status);
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        if (ignoresEntityCollision()) {
            return;
        }
        Entity target = entityHitResult.getEntity();
        Entity owner = this.getOwner();
        if (!this.friendlyFire && owner != null && target == owner) {
            return;
        }
        if (this.behavior == NoitaProjectileBehavior.HEAL && target instanceof LivingEntity livingTarget) {
            livingTarget.heal(Math.max(1.0f, getRolledDamage()));
        } else {
            NoitaSpellDamage.apply(
                target,
                this.getDamageSources().indirectMagic(this, owner == null ? this : owner),
                getRolledDamage()
            );
        }
        applyEntitySpellEffect(target);
        applyProjectileKnockback(target);
        if (this.behavior == NoitaProjectileBehavior.SWAPPER && owner != null) {
            Vec3d ownerPos = owner.getPos();
            owner.requestTeleport(target.getX(), target.getY(), target.getZ());
            target.requestTeleport(ownerPos.x, ownerPos.y, ownerPos.z);
        }
        if (this.behavior == NoitaProjectileBehavior.TELEPORT) {
            handleTeleportCollision(target);
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (this.getWorld().isClient || hitResult.getType() == HitResult.Type.MISS) {
            return;
        }

        String path = normalizedItemPath();
        if (hitResult instanceof EntityHitResult && path.equals("exploding_deer")) {
            explodeNoitaProjectile(this.explosionRadius <= 0.0f ? 3.0f : this.explosionRadius, true);
            return;
        }
        if (hitResult instanceof EntityHitResult && path.equals("summon_egg")) {
            hatchSummonedEgg();
            this.getWorld().sendEntityStatus(this, HIT_FLASH_STATUS);
            this.discard();
            return;
        }
        if (hitResult instanceof EntityHitResult && path.equals("summon_hollow_egg")) {
            burstHollowEgg();
            releaseTriggerPayload();
            this.getWorld().sendEntityStatus(this, HIT_FLASH_STATUS);
            this.discard();
            return;
        }
        if (path.equals("spore_pod")) {
            burstSporePod();
            this.getWorld().sendEntityStatus(this, HIT_FLASH_STATUS);
            this.discard();
            return;
        }

        if (ignoresTerminalCollision(hitResult)) {
            applyNonTerminalCollisionEffect(hitResult);
            return;
        }

        if (hitResult instanceof BlockHitResult blockHitResult && handleBouncyBlockCollision(blockHitResult)) {
            return;
        }

        if (this.behavior.digsOnCollision() && hitResult instanceof BlockHitResult blockHitResult) {
            digBlock(blockHitResult);
        }
        applyBlockSpellEffect(hitResult);
        if (this.behavior == NoitaProjectileBehavior.TELEPORT && !(hitResult instanceof EntityHitResult)) {
            handleTeleportCollision(null);
        }
        if (normalizedItemPath().equals("hook")) {
            pullOwnerToHook();
        }
        if (this.behavior.explodesOnCollision() && this.explosionRadius > 0.0f) {
            this.getWorld().createExplosion(this, this.getX(), this.getY(), this.getZ(), this.explosionRadius, true, World.ExplosionSourceType.TNT);
        }
        if (this.triggerMode == NoitaSpellTriggerMode.HIT || this.triggerMode == NoitaSpellTriggerMode.DEATH) {
            releaseTriggerPayload();
        }
        this.getWorld().sendEntityStatus(this, HIT_FLASH_STATUS);
        if (!this.piercing) {
            this.discard();
        }
    }

    @Override
    protected float getGravity() {
        return this.gravity;
    }

    @Override
    protected Item getDefaultItem() {
        return getVisualItem(this.itemPath);
    }

    @Override
    protected boolean canHit(Entity entity) {
        return !ignoresEntityCollision() && super.canHit(entity);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString(ITEM_PATH_KEY, this.itemPath);
        nbt.putString(BEHAVIOR_KEY, this.behavior.name());
        nbt.putFloat(DAMAGE_KEY, this.damage);
        nbt.putFloat(CRITICAL_CHANCE_PERCENT_KEY, this.criticalChancePercent);
        nbt.putInt(MAX_AGE_KEY, this.maxAge);
        nbt.putInt(TRAIL_LIGHT_STACKS_KEY, this.trailLightStacks);
        nbt.putFloat(EXPLOSION_RADIUS_KEY, this.explosionRadius);
        nbt.putFloat(GRAVITY_KEY, this.gravity);
        nbt.putFloat(DRAG_KEY, this.drag);
        nbt.putFloat(BOUNCE_DAMPING_KEY, this.bounceDamping);
        nbt.putFloat(RENDER_SCALE_KEY, this.renderScale);
        nbt.putFloat(KNOCKBACK_FORCE_KEY, this.knockbackForce);
        nbt.putBoolean(FRIENDLY_FIRE_KEY, this.friendlyFire);
        nbt.putBoolean(PIERCING_KEY, this.piercing);
        nbt.putInt(BOUNCE_COUNT_KEY, this.bounceCount);
        nbt.put(MODIFIER_EFFECTS_KEY, NoitaProjectilePayload.toEffectNbtList(this.modifierEffects));
        nbt.putString(TRIGGER_MODE_KEY, this.triggerMode.name());
        nbt.putInt(TRIGGER_DELAY_TICKS_KEY, this.triggerDelayTicks);
        nbt.putBoolean(TRIGGER_PAYLOAD_RELEASED_KEY, this.triggerPayloadReleased);
        nbt.put(TRIGGER_PAYLOADS_KEY, NoitaProjectilePayload.toNbtList(this.triggerPayloads));
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains(ITEM_PATH_KEY, NbtElement.STRING_TYPE)) {
            this.itemPath = nbt.getString(ITEM_PATH_KEY);
            setItemFromPath(this.itemPath);
        }
        if (nbt.contains(BEHAVIOR_KEY, NbtElement.STRING_TYPE)) {
            try {
                this.behavior = NoitaProjectileBehavior.valueOf(nbt.getString(BEHAVIOR_KEY));
            } catch (IllegalArgumentException ignored) {
                this.behavior = NoitaProjectileBehavior.BOLT;
            }
        }
        if (nbt.contains(DAMAGE_KEY, NbtElement.NUMBER_TYPE)) {
            this.damage = Math.max(0.0f, nbt.getFloat(DAMAGE_KEY));
        }
        if (nbt.contains(CRITICAL_CHANCE_PERCENT_KEY, NbtElement.NUMBER_TYPE)) {
            this.criticalChancePercent = Math.max(0.0f, nbt.getFloat(CRITICAL_CHANCE_PERCENT_KEY));
        }
        if (nbt.contains(MAX_AGE_KEY, NbtElement.NUMBER_TYPE)) {
            this.maxAge = Math.max(1, nbt.getInt(MAX_AGE_KEY));
        }
        if (nbt.contains(TRAIL_LIGHT_STACKS_KEY, NbtElement.NUMBER_TYPE)) {
            this.trailLightStacks = Math.max(0, nbt.getInt(TRAIL_LIGHT_STACKS_KEY));
        }
        if (nbt.contains(EXPLOSION_RADIUS_KEY, NbtElement.NUMBER_TYPE)) {
            this.explosionRadius = Math.max(0.0f, nbt.getFloat(EXPLOSION_RADIUS_KEY));
        }
        if (nbt.contains(GRAVITY_KEY, NbtElement.NUMBER_TYPE)) {
            this.gravity = nbt.getFloat(GRAVITY_KEY);
            this.setNoGravity(false);
        }
        if (nbt.contains(DRAG_KEY, NbtElement.NUMBER_TYPE)) {
            this.drag = Math.max(0.0f, nbt.getFloat(DRAG_KEY));
        }
        if (nbt.contains(BOUNCE_DAMPING_KEY, NbtElement.NUMBER_TYPE)) {
            this.bounceDamping = Math.max(0.0f, nbt.getFloat(BOUNCE_DAMPING_KEY));
        }
        if (nbt.contains(RENDER_SCALE_KEY, NbtElement.NUMBER_TYPE)) {
            this.renderScale = Math.max(0.1f, nbt.getFloat(RENDER_SCALE_KEY));
        }
        if (nbt.contains(KNOCKBACK_FORCE_KEY, NbtElement.NUMBER_TYPE)) {
            this.knockbackForce = nbt.getFloat(KNOCKBACK_FORCE_KEY);
        }
        if (nbt.contains(FRIENDLY_FIRE_KEY, NbtElement.BYTE_TYPE)) {
            this.friendlyFire = nbt.getBoolean(FRIENDLY_FIRE_KEY);
        }
        if (nbt.contains(PIERCING_KEY, NbtElement.BYTE_TYPE)) {
            this.piercing = nbt.getBoolean(PIERCING_KEY);
        }
        if (nbt.contains(BOUNCE_COUNT_KEY, NbtElement.NUMBER_TYPE)) {
            this.bounceCount = Math.max(0, nbt.getInt(BOUNCE_COUNT_KEY));
        }
        this.modifierEffects = NoitaProjectilePayload.fromEffectNbtList(nbt.getList(MODIFIER_EFFECTS_KEY, NbtElement.STRING_TYPE));
        this.setInvisible(hasModifierEffect(NoitaModifierEffect.COLOUR_INVIS));
        if (nbt.contains(TRIGGER_MODE_KEY, NbtElement.STRING_TYPE)) {
            try {
                this.triggerMode = NoitaSpellTriggerMode.valueOf(nbt.getString(TRIGGER_MODE_KEY));
            } catch (IllegalArgumentException ignored) {
                this.triggerMode = NoitaSpellTriggerMode.NONE;
            }
        }
        if (nbt.contains(TRIGGER_DELAY_TICKS_KEY, NbtElement.NUMBER_TYPE)) {
            this.triggerDelayTicks = Math.max(0, nbt.getInt(TRIGGER_DELAY_TICKS_KEY));
        }
        if (nbt.contains(TRIGGER_PAYLOAD_RELEASED_KEY, NbtElement.BYTE_TYPE)) {
            this.triggerPayloadReleased = nbt.getBoolean(TRIGGER_PAYLOAD_RELEASED_KEY);
        }
        this.triggerPayloads = NoitaProjectilePayload.fromNbtList(nbt.getList(TRIGGER_PAYLOADS_KEY, NbtElement.COMPOUND_TYPE));
    }

    public static void spawnPayloadProjectile(World world, LivingEntity owner, Vec3d position, Vec3d direction, NoitaProjectilePayload payload) {
        Vec3d safeDirection = direction.lengthSquared() > 1.0E-6 ? direction.normalize() : Vec3d.ZERO;
        for (int i = 0; i < payload.projectileCount(); i++) {
            Vec3d shotDirection = applyBurstSpread(safeDirection, i, payload.projectileCount(), payload.burstSpreadDegrees());
            if (payload.behavior().usesBombEntity()) {
                BombEntity bomb = new BombEntity(world, owner, payload);
                bomb.setPosition(position);
                bomb.setVelocity(shotDirection.multiply(payload.speed()));
                world.spawnEntity(bomb);
            } else {
                SparkBoltProjectileEntity projectile = new SparkBoltProjectileEntity(world, owner, payload);
                projectile.setPosition(position);
                projectile.setVelocity(shotDirection.x, shotDirection.y, shotDirection.z, payload.speed(), payload.divergence());
                world.spawnEntity(projectile);
            }
        }
    }

    private float getRolledDamage() {
        if (hasModifierEffect(NoitaModifierEffect.ZERO_DAMAGE)) {
            return 0.0f;
        }
        float rolledDamage = this.damage;
        if (hasModifierEffect(NoitaModifierEffect.DAMAGE_RANDOM)) {
            int multiplier = (this.random.nextInt(8) - 3) * this.random.nextInt(3);
            rolledDamage += multiplier * 2.4f;
        }
        if (this.criticalChancePercent <= 0.0f || this.random.nextFloat() * 100.0f >= this.criticalChancePercent) {
            return Math.max(0.0f, rolledDamage);
        }

        return Math.max(0.0f, rolledDamage * CRITICAL_DAMAGE_MULTIPLIER);
    }

    private void releaseTriggerPayload() {
        if (this.triggerPayloadReleased || this.triggerPayloads.isEmpty()) {
            return;
        }
        this.triggerPayloadReleased = true;
        Vec3d direction = getPayloadDirection();
        Entity owner = this.getOwner();
        LivingEntity livingOwner = owner instanceof LivingEntity living ? living : null;
        World world = this.getWorld();
        for (NoitaProjectilePayload payload : this.triggerPayloads) {
            spawnPayloadProjectile(world, livingOwner, this.getPos(), direction, payload);
        }
    }

    private Vec3d getPayloadDirection() {
        Vec3d velocity = this.getVelocity();
        if (velocity.lengthSquared() > 1.0E-6) {
            return velocity.normalize();
        }
        Entity owner = this.getOwner();
        return owner == null ? this.getRotationVec(1.0f) : owner.getRotationVec(1.0f);
    }

    private boolean handleBouncyBlockCollision(BlockHitResult hitResult) {
        if (!canBounceProjectile() || this.bounces >= getMaxBounces()) {
            return false;
        }

        Direction side = hitResult.getSide();
        Vec3d velocity = this.getVelocity();
        double x = side.getAxis() == Direction.Axis.X ? -velocity.x : velocity.x;
        double y = side.getAxis() == Direction.Axis.Y ? -velocity.y : velocity.y;
        double z = side.getAxis() == Direction.Axis.Z ? -velocity.z : velocity.z;
        this.setVelocity(x * this.bounceDamping, y * this.bounceDamping, z * this.bounceDamping);
        this.bounces++;
        applyBounceModifierEffects();
        this.setPosition(this.getPos().add(this.getVelocity().normalize().multiply(0.12)));
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.25f, 1.3f);
        return true;
    }

    private void applyAmbientBehavior() {
        if (this.drag > 0.0f && this.drag != 1.0f) {
            this.setVelocity(this.getVelocity().multiply(this.drag));
        }
        if (this.behavior == NoitaProjectileBehavior.BLACK_HOLE || this.behavior == NoitaProjectileBehavior.WHITE_HOLE) {
            pushNearbyEntities(getHoleForce(), getHoleRadius());
            if (!this.getWorld().isClient && this.age % 2 == 0) {
                eatCellsAround(getHoleEatRadius());
            }
        }
        if (this.behavior == NoitaProjectileBehavior.MIST && !this.getWorld().isClient) {
            this.setVelocity(this.getVelocity().multiply(0.92));
        }
        if (isNoitaStaticAreaProjectile()) {
            this.setVelocity(isStaticCloud(normalizedItemPath()) ? this.getVelocity().multiply(0.96) : Vec3d.ZERO);
        }
        if (isStaticTeleport()) {
            this.setVelocity(Vec3d.ZERO);
        }
    }

    private double getHoleForce() {
        String path = normalizedItemPath();
        if (this.behavior == NoitaProjectileBehavior.WHITE_HOLE) {
            return path.contains("_giga") ? -0.17 : path.endsWith("_big") ? -0.13 : -0.09;
        }
        return path.contains("_giga") ? 0.20 : path.endsWith("_big") ? 0.15 : 0.11;
    }

    private double getHoleRadius() {
        String path = normalizedItemPath();
        return path.contains("_giga") ? 10.0 : path.endsWith("_big") ? 8.0 : 6.0;
    }

    private int getHoleEatRadius() {
        String path = normalizedItemPath();
        if (this.behavior == NoitaProjectileBehavior.WHITE_HOLE) {
            return path.contains("_giga") ? 2 : 1;
        }
        return path.contains("_giga") ? 4 : path.endsWith("_big") ? 3 : 2;
    }

    private void pushNearbyEntities(double force, double radius) {
        World world = this.getWorld();
        Box box = this.getBoundingBox().expand(radius);
        for (Entity entity : world.getOtherEntities(this, box)) {
            Vec3d offset = this.getPos().subtract(entity.getPos());
            double distanceSquared = Math.max(1.0, offset.lengthSquared());
            Vec3d impulse = offset.normalize().multiply(force / distanceSquared * 8.0);
            entity.addVelocity(impulse.x, impulse.y, impulse.z);
            entity.velocityModified = true;
        }
    }

    private void digBlock(BlockHitResult hitResult) {
        World world = this.getWorld();
        BlockState state = world.getBlockState(hitResult.getBlockPos());
        if (!state.isAir() && state.getHardness(world, hitResult.getBlockPos()) >= 0.0f) {
            world.breakBlock(hitResult.getBlockPos(), shouldDropDugBlocks(), this, 16);
        }
    }

    private void handleTeleportCollision(Entity target) {
        if (isHomebringerTeleport() && target != null) {
            teleportTargetToOwner(target);
            return;
        }
        if (isStaticTeleport()) {
            teleportOwnerToReturnAnchor();
            return;
        }
        teleportOwnerToProjectile();
    }

    private boolean ignoresTerminalCollision(HitResult hitResult) {
        if (hitResult instanceof EntityHitResult && ignoresEntityCollision()) {
            return true;
        }
        return hitResult instanceof BlockHitResult && ignoresWorldCollision();
    }

    private void applyNonTerminalCollisionEffect(HitResult hitResult) {
        if (hitResult instanceof BlockHitResult && this.behavior == NoitaProjectileBehavior.BLACK_HOLE) {
            eatCellsAround(2);
        } else if (hitResult instanceof BlockHitResult && this.behavior == NoitaProjectileBehavior.WHITE_HOLE) {
            pushNearbyEntities(-0.12, 6.0);
        } else if (this.behavior == NoitaProjectileBehavior.MIST) {
            applyMistCloud();
        } else if (isShield()) {
            applyShieldField();
        }
    }

    private void teleportOwnerToProjectile() {
        Entity owner = this.getOwner();
        if (owner != null) {
            owner.requestTeleport(this.getX(), this.getY(), this.getZ());
        }
    }

    private void teleportOwnerToReturnAnchor() {
        Entity owner = this.getOwner();
        if (owner == null) {
            return;
        }
        Vec3d anchor = this.returnAnchor == null ? this.getPos() : this.returnAnchor;
        owner.requestTeleport(anchor.x, anchor.y, anchor.z);
    }

    private void teleportTargetToOwner(Entity target) {
        Entity owner = this.getOwner();
        if (owner != null) {
            target.requestTeleport(owner.getX(), owner.getY(), owner.getZ());
        }
    }

    private void applyServerTickEffects(ServerWorld world) {
        String path = normalizedItemPath();
        if (this.returnAnchor == null && isStaticTeleport()) {
            Entity owner = this.getOwner();
            this.returnAnchor = owner == null ? this.getPos() : owner.getPos();
            this.setPosition(this.returnAnchor);
        }
        applyModifierTickEffects(world);
        if (this.behavior == NoitaProjectileBehavior.MIST) {
            applyMistCloud();
            return;
        }
        if (isNoitaStaticAreaProjectile()) {
            applyStaticProjectileTick(world, path);
            return;
        }
        if (path.equals("grenade_anti")) {
            applyAntiGravityGrenadeTick();
        }
        if (path.equals("hook")) {
            applyHookTick();
        }
        if (isShield()) {
            applyShieldField();
            return;
        }
        if (path.equals("air_bullet")) {
            pushNearbyEntities(-0.12, 3.2);
            return;
        }
        if (path.equals("exploding_ducks")) {
            applyExplodingDuckTick();
            return;
        }
        if (path.equals("exploding_deer")) {
            applyExplodingDeerTick();
            return;
        }
        if (path.equals("summon_egg") || path.equals("summon_hollow_egg")) {
            applyEggTick(path);
            return;
        }
        if (path.equals("megalaser")) {
            applyMegalaserChargeTick(world);
            return;
        }
        if (path.equals("tentacle") || path.equals("tentacle_timer") || path.equals("tentacle_portal")) {
            applyTentacleField(path);
            return;
        }
        if (path.equals("pollen") || path.equals("missile") || isDeathCross()) {
            steerTowardNearestLiving(path.equals("pollen") ? 0.28 : path.equals("missile") ? 0.18 : 0.08, path.equals("pollen") ? 7.0 : 12.0);
        }
        if (isDeathCross() && this.age % SPELL_EFFECT_INTERVAL_TICKS == 0) {
            damageLivingInRadius(path.endsWith("_big") ? 4.5 : 3.0, this.damage + 4.0f, true);
            pushNearbyEntities(-0.055, path.endsWith("_big") ? 4.5 : 3.0);
            return;
        }
        if (path.equals("disc_bullet_big")) {
            applyBigSawbladeTick();
            return;
        }
        if (path.equals("disc_bullet_bigger")) {
            applyOmegaSawbladeTick();
            return;
        }
        if (path.equals("worm_shot")) {
            applyWormShotTick();
            return;
        }
        if (this.behavior == NoitaProjectileBehavior.SUMMON && !this.summonReleased && this.age > 2) {
            releaseSummon(world);
            this.summonReleased = true;
            this.discard();
            return;
        }
        if (this.behavior.isBeamLike()) {
            applyBeamTickEffect(path);
            return;
        }
        if (path.equals("darkflame") || path.equals("flamethrower")) {
            igniteAround(1.4, 3, true);
            if (path.equals("darkflame")) {
                applyDarkflameTick();
            }
        } else if (path.equals("firework")) {
            igniteAround(1.2, 2, false);
        } else if (path.equals("crumbling_earth")) {
            crumbleTerrain();
        } else if (path.equals("infestation") && this.age % 10 == 0) {
            spawnInfestationShard();
        } else if (path.equals("spore_pod")) {
            applySporePodTick();
        } else if (path.equals("pollen") && this.age % 8 == 0) {
            applyPollenCloud();
        } else if (path.equals("spiral_shot")) {
            rotateVelocity(0.26);
        } else if (path.equals("chain_bolt") && this.age % 7 == 0) {
            chainToNearbyLiving();
        } else if (path.equals("expanding_orb")) {
            applyExpandingOrbTick();
        } else if (path.equals("cursed_orb")) {
            applyCursedOrbTick();
        } else if (path.equals("funky_spell")) {
            applyFunkyMotion();
        } else if (path.equals("glowing_bolt")) {
            NoitaTemporaryLightManager.illuminateTrail(world, new Vec3d(this.prevX, this.prevY, this.prevZ), this.getPos(), 5);
        }
    }

    private void applyStaticProjectileTick(ServerWorld world, String path) {
        if (path.equals("bomb_detonator")) {
            if (this.age <= 2) {
                detonateNearbyExplosives();
                this.discard();
            }
            return;
        }
        if (isStaticBlast(path)) {
            if (this.age <= 2) {
                applyStaticBlast(path);
            }
            return;
        }
        if (isStaticBarrier(path)) {
            applyStaticBarrier(path);
            return;
        }
        if (isStaticCircleField(path)) {
            applyStaticCircleField(path);
            return;
        }
        if (isStaticProjectileField(path)) {
            applyStaticProjectileField(path);
            return;
        }
        if (isStaticVacuum(path)) {
            applyStaticVacuum(path);
            return;
        }
        if (isStaticCloud(path)) {
            applyStaticCloud(world, path);
            return;
        }
        if (path.equals("purple_explosion_field")) {
            applyPurpleExplosionField();
            return;
        }
        if (path.equals("destruction")) {
            applyDestructionField();
            return;
        }
        if (path.equals("mass_polymorph")) {
            applyMassPolymorphField();
            return;
        }
        if (isStaticSwarm(path)) {
            applyStaticSwarm(path);
            return;
        }
        if (path.equals("meteor_rain")) {
            applyMeteorRain(world);
            return;
        }
        if (path.equals("worm_rain")) {
            applyWormRain(world);
        }
    }

    private void applyStaticBlast(String path) {
        float radius = switch (path) {
            case "explosion" -> 3.6f;
            case "explosion_light" -> 2.6f;
            case "thunder_blast" -> 3.0f;
            case "poison_blast", "alcohol_blast" -> 2.0f;
            default -> 1.8f;
        };
        this.getWorld().createExplosion(this, this.getX(), this.getY(), this.getZ(), radius, path.equals("fireblast") || path.equals("alcohol_blast"), World.ExplosionSourceType.TNT);
        if (path.equals("fireblast") || path.equals("alcohol_blast")) {
            igniteAround(radius + 0.6, path.equals("fireblast") ? 5 : 3, true);
        } else if (path.equals("poison_blast")) {
            poisonLivingInRadius(radius + 1.0, 140);
            poisonWaterAround(this.getBlockPos(), 2);
        } else if (path.equals("thunder_blast")) {
            strikeLightning(this.getPos(), true);
            damageLivingInRadius(radius + 1.0, 4.0f, true);
        }
        this.getWorld().sendEntityStatus(this, HIT_FLASH_STATUS);
        this.discard();
    }

    private void applyStaticBarrier(String path) {
        double radius = path.equals("wall_square") ? 4.4 : 3.2;
        boolean vertical = path.equals("wall_vertical");
        Box area = path.equals("wall_square")
            ? this.getBoundingBox().expand(radius)
            : new Box(this.getX() - (vertical ? 0.55 : radius), this.getY() - (vertical ? radius : 0.55), this.getZ() - radius,
                this.getX() + (vertical ? 0.55 : radius), this.getY() + (vertical ? radius : 0.55), this.getZ() + radius);
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            if (entity == this.getOwner() || entity instanceof LivingEntity) {
                continue;
            }
            Vec3d away = entity.getPos().subtract(this.getPos());
            if (away.lengthSquared() > 1.0E-6) {
                Vec3d impulse = away.normalize().multiply(0.55);
                entity.addVelocity(impulse.x, impulse.y + 0.03, impulse.z);
                entity.velocityModified = true;
            }
        }
        if (this.age % 10 == 0) {
            NoitaTemporaryLightManager.illuminateTrail((ServerWorld) this.getWorld(), new Vec3d(this.prevX, this.prevY, this.prevZ), this.getPos(), 4);
        }
    }

    private void applyStaticCircleField(String path) {
        if (this.age % SPELL_EFFECT_INTERVAL_TICKS != 0) {
            return;
        }
        Box area = this.getBoundingBox().expand(4.0);
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (path.equals("berserk_field")) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 120, 1), this);
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 120, 0), this);
            } else if (path.equals("polymorph_field")) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 2), this);
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 1), this);
            } else if (path.equals("chaos_polymorph_field")) {
                applyRandomFunkyStatus(living);
            } else if (path.equals("electrocution_field")) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 1), this);
                if (this.age % 10 == 0) {
                    strikeLightning(living.getPos(), true);
                }
            } else if (path.equals("freeze_field")) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 3), this);
                living.setFrozenTicks(Math.max(living.getFrozenTicks(), 160));
            } else if (path.equals("regeneration_field")) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 120, 1), this);
            } else if (path.equals("teleportation_field") && this.age % 20 == 0) {
                Vec3d offset = randomHorizontalDirection().multiply(3.0 + this.random.nextDouble() * 3.0);
                living.requestTeleport(living.getX() + offset.x, living.getY() + 0.2, living.getZ() + offset.z);
            } else if (path.equals("levitation_field")) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 80, 0), this);
            } else if (path.equals("shield_field")) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 120, 0), this);
                pushNearbyNonLivingProjectiles(4.2, 0.55);
            }
        }
        if (path.equals("freeze_field")) {
            freezeAround(this.getBlockPos(), 2);
        }
    }

    private void applyStaticProjectileField(String path) {
        if (this.age % SPELL_EFFECT_INTERVAL_TICKS != 0) {
            return;
        }
        double radius = 4.5;
        Box area = this.getBoundingBox().expand(radius);
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            if (entity instanceof LivingEntity || entity == this.getOwner()) {
                continue;
            }
            if (path.equals("projectile_transmutation_field")) {
                entity.discard();
                spawnRadialProjectiles("spark_bolt", NoitaProjectileBehavior.BOLT, 1, Math.max(1.0f, this.damage + 1.0f), 25, 0.55f, 0.0f, 0.0f);
            } else if (path.equals("projectile_thunder_field")) {
                strikeLightning(entity.getPos(), true);
                entity.discard();
            } else if (path.equals("projectile_gravity_field")) {
                pullNearbyEntitiesTo(this.getPos(), radius, 0.34, this);
            }
        }
    }

    private void applyStaticVacuum(String path) {
        pullNearbyEntitiesTo(this.getPos(), 6.0, path.equals("vacuum_entities") ? 0.34 : 0.20, this);
        if (this.age % 4 != 0) {
            return;
        }
        if (path.equals("vacuum_entities")) {
            damageLivingInRadius(1.8, 1.0f, true);
        } else if (path.equals("vacuum_powder")) {
            vacuumTerrain(false);
        } else if (path.equals("vacuum_liquid")) {
            vacuumTerrain(true);
        }
    }

    private void applyStaticCloud(ServerWorld world, String path) {
        this.setVelocity(this.getVelocity().multiply(0.90));
        if (this.age % SPELL_EFFECT_INTERVAL_TICKS != 0) {
            return;
        }
        BlockPos center = this.getBlockPos();
        if (path.equals("cloud_water")) {
            placeMaterialPatch(center, Blocks.WATER.getDefaultState(), 2);
            extinguishLivingInRadius(4.0);
        } else if (path.equals("cloud_oil")) {
            placeMaterialPatch(center, Blocks.BLACK_CONCRETE_POWDER.getDefaultState(), 2);
            slowLivingInRadius(4.0, 80, 0);
        } else if (path.equals("cloud_blood")) {
            placeMaterialPatch(center, Blocks.REDSTONE_WIRE.getDefaultState(), 2);
            healLivingInRadius(4.0, 1.0f);
        } else if (path.equals("cloud_acid")) {
            dissolveSoftBlocks(center, 2);
            poisonLivingInRadius(4.0, 100);
        } else if (path.equals("cloud_thunder")) {
            NoitaTemporaryLightManager.illuminateTrail(world, new Vec3d(this.prevX, this.prevY, this.prevZ), this.getPos(), 6);
            if (this.age % 15 == 0) {
                strikeLightning(this.getPos().add(randomHorizontalDirection().multiply(2.0)), true);
            }
        }
    }

    private void applyPurpleExplosionField() {
        if (this.age % 10 == 0) {
            spawnRadialProjectiles("glitter_bomb", NoitaProjectileBehavior.EXPLOSIVE, 5, 2.0f, 24, 0.45f, 0.0f, 1.0f);
        }
    }

    private void applyDestructionField() {
        if (this.age <= 2) {
            this.getWorld().createExplosion(this, this.getX(), this.getY(), this.getZ(), 6.0f, true, World.ExplosionSourceType.TNT);
        }
        if (this.age % 2 == 0) {
            eatCellsAround(4);
            damageLivingInRadius(6.0, 10.0f, true);
        }
    }

    private void applyMassPolymorphField() {
        if (this.age % SPELL_EFFECT_INTERVAL_TICKS != 0) {
            return;
        }
        Box area = this.getBoundingBox().expand(8.0);
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            if (entity instanceof LivingEntity living) {
                applyRandomFunkyStatus(living);
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 180, 2), this);
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 180, 0), this);
            }
        }
    }

    private void applyStaticSwarm(String path) {
        if (this.age % 6 != 0) {
            return;
        }
        float damageAmount = path.equals("swarm_wasp") ? 2.5f : path.equals("swarm_firebug") ? 2.0f : 1.5f;
        damageLivingInRadius(path.equals("friend_fly") ? 2.8 : 3.4, damageAmount, false);
        steerTowardNearestLiving(path.equals("friend_fly") ? 0.18 : 0.26, 8.0);
        if (path.equals("swarm_firebug")) {
            igniteAround(1.6, 3, false);
        } else if (path.equals("swarm_wasp")) {
            poisonLivingInRadius(2.0, 70);
        } else if (path.equals("friend_fly")) {
            Entity owner = this.getOwner();
            if (owner instanceof LivingEntity livingOwner && this.squaredDistanceTo(owner) < 16.0) {
                livingOwner.heal(0.5f);
            }
        }
    }

    private void applyMeteorRain(ServerWorld world) {
        if (this.age % 12 != 0) {
            return;
        }
        Vec3d center = this.getPos().add(randomHorizontalDirection().multiply(this.random.nextDouble() * 5.0));
        Vec3d spawn = center.add(0.0, 12.0, 0.0);
        spawnProjectileFrom(spawn, new Vec3d(0.0, -1.0, 0.0).add(randomHorizontalDirection().multiply(0.16)), "meteor", NoitaProjectileBehavior.EXPLOSIVE, 8.0f, 80, 1.35f, 0.05f, 3.0f, false);
        NoitaTemporaryLightManager.illuminateTrail(world, spawn, center, 7);
    }

    private void applyWormRain(ServerWorld world) {
        if (this.age % 20 != 0) {
            return;
        }
        Entity spawned = this.random.nextBoolean() ? EntityType.SILVERFISH.create(world) : EntityType.SPIDER.create(world);
        if (spawned == null) {
            return;
        }
        Vec3d position = this.getPos().add(randomHorizontalDirection().multiply(this.random.nextDouble() * 5.0)).add(0.0, 8.0, 0.0);
        spawned.refreshPositionAndAngles(position.x, position.y, position.z, this.getYaw(), this.getPitch());
        spawned.setVelocity(randomHorizontalDirection().multiply(0.15).add(0.0, -0.2, 0.0));
        world.spawnEntity(spawned);
    }

    private void applyEntitySpellEffect(Entity target) {
        String path = normalizedItemPath();
        if (target instanceof LivingEntity livingTarget) {
            applyModifierEntityEffect(livingTarget);
            if (path.equals("antiheal")) {
                livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1), this);
                livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 80, 1), this);
            } else if (path.equals("iceball") || path.equals("freezing_gaze")) {
                livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 3), this);
                livingTarget.setFrozenTicks(Math.max(livingTarget.getFrozenTicks(), 180));
            } else if (path.equals("slimeball") || path.equals("glue_shot")) {
                livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, path.equals("glue_shot") ? 4 : 2), this);
            } else if (path.equals("acidshot") || path.equals("mist_radioactive")) {
                livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 1), this);
            } else if (path.equals("mist_alcohol")) {
                livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 140, 0), this);
            } else if (path.equals("mist_blood")) {
                livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 80, 0), this);
            } else if (path.equals("thunderball") || path.equals("lightning") || path.equals("ball_lightning")) {
                livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 1), this);
                strikeLightning(livingTarget.getPos(), true);
            } else if (path.equals("hook")) {
                pullEntityTowardOwner(livingTarget, 0.7);
            } else if (path.equals("air_bullet")) {
                pushEntityAwayFromSelf(livingTarget, 1.0);
            } else if (path.equals("pollen")) {
                livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 1), this);
                livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 60, 0), this);
            } else if (path.equals("spore_pod")) {
                livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1), this);
                livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 0), this);
            } else if (path.equals("chain_bolt")) {
                chainToNearbyLiving();
            } else if (path.equals("cursed_orb")) {
                livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 120, 0), this);
                livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1), this);
            } else if (path.equals("funky_spell")) {
                applyRandomFunkyStatus(livingTarget);
            }
        }
        if (isFireSpell(path)) {
            target.setOnFireFor(path.equals("darkflame") ? 6 : 4);
        }
        if (hasModifierEffect(NoitaModifierEffect.BURN_TRAIL) || hasModifierEffect(NoitaModifierEffect.FIRE_TRAIL)) {
            target.setOnFireFor(4);
        }
    }

    private void applyBlockSpellEffect(HitResult hitResult) {
        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            return;
        }
        String path = normalizedItemPath();
        BlockPos blockPos = blockHitResult.getBlockPos();
        if (isFireSpell(path)) {
            igniteAround(2.0, path.equals("firebomb") ? 6 : 4, true);
        } else if (path.equals("iceball") || path.equals("freezing_gaze")) {
            freezeAround(blockPos, 2);
        } else if (path.equals("acidshot")) {
            dissolveSoftBlocks(blockPos, 2);
        } else if (path.equals("slimeball") || path.equals("glue_shot")) {
            placeMaterialPatch(blockPos.offset(blockHitResult.getSide()), Blocks.SLIME_BLOCK.getDefaultState(), 1);
        } else if (path.equals("crumbling_earth")) {
            crumbleTerrain();
        } else if (path.equals("thunderball") || path.equals("lightning") || path.equals("ball_lightning")) {
            strikeLightning(Vec3d.ofCenter(blockPos), true);
        }
        applyModifierBlockEffect(blockPos);
    }

    private void applyModifierTickEffects(ServerWorld world) {
        if (this.modifierEffects.isEmpty()) {
            return;
        }

        if (hasModifierEffect(NoitaModifierEffect.ACCELERATING_SHOT)) {
            this.setVelocity(this.getVelocity().multiply(1.035));
        }
        if (hasModifierEffect(NoitaModifierEffect.DECELERATING_SHOT)) {
            this.setVelocity(this.getVelocity().multiply(0.965));
        }
        if (hasModifierEffect(NoitaModifierEffect.SINEWAVE)) {
            rotateVelocity(Math.sin(this.age * 0.45) * 0.13);
        }
        if (hasModifierEffect(NoitaModifierEffect.CHAOTIC_ARC) && this.age % 3 == 0) {
            this.setVelocity(this.getVelocity().add(
                (this.random.nextDouble() - 0.5) * 0.16,
                (this.random.nextDouble() - 0.5) * 0.10,
                (this.random.nextDouble() - 0.5) * 0.16
            ));
        }
        if (hasModifierEffect(NoitaModifierEffect.PINGPONG_PATH) && this.age % 14 == 0) {
            this.setVelocity(this.getVelocity().multiply(-0.85));
        }
        if (hasModifierEffect(NoitaModifierEffect.FLOATING_ARC)) {
            this.setVelocity(this.getVelocity().add(0.0, 0.018, 0.0));
        }
        if (hasModifierEffect(NoitaModifierEffect.FLY_DOWNWARDS)) {
            this.setVelocity(this.getVelocity().add(0.0, -0.035, 0.0));
        }
        if (hasModifierEffect(NoitaModifierEffect.FLY_UPWARDS)) {
            this.setVelocity(this.getVelocity().add(0.0, 0.035, 0.0));
        }
        if (hasModifierEffect(NoitaModifierEffect.HORIZONTAL_ARC)) {
            Vec3d velocity = this.getVelocity();
            this.setVelocity(velocity.x, velocity.y * 0.55, velocity.z);
        }
        if (hasModifierEffect(NoitaModifierEffect.LINE_ARC)) {
            Vec3d velocity = this.getVelocity();
            this.setVelocity(velocity.x, velocity.y * 0.25, velocity.z);
        }
        if (hasModifierEffect(NoitaModifierEffect.PHASING_ARC) && this.age % 8 < 4) {
            digAhead(1);
        }
        if (hasModifierEffect(NoitaModifierEffect.ORBIT_SHOT)) {
            rotateVelocity(0.18);
        }
        if (hasModifierEffect(NoitaModifierEffect.SPIRALING_SHOT)) {
            rotateVelocity(0.08 + this.age * 0.002);
        }
        if (hasModifierEffect(NoitaModifierEffect.TRUE_ORBIT)) {
            rotateVelocity(0.32);
        }
        if (hasModifierEffect(NoitaModifierEffect.AVOIDING_ARC)) {
            steerAwayFromNearestLiving(5.0, 0.08);
        }
        if (hasModifierEffect(NoitaModifierEffect.AUTOAIM)) {
            steerTowardNearestLiving(0.10, 24.0);
        }
        if (hasModifierEffect(NoitaModifierEffect.QUANTUM_SPLIT) && this.age == 2) {
            spawnRadialProjectiles("spark_bolt", NoitaProjectileBehavior.BOLT, 4, Math.max(1.0f, this.damage * 0.45f), 35, 0.65f, 0.0f, 0.0f);
        }
        if (hasModifierEffect(NoitaModifierEffect.ROCKET_DOWNWARDS) && this.age == 3) {
            spawnDirectedProjectiles("rocket", NoitaProjectileBehavior.EXPLOSIVE, 3, new Vec3d(0.0, -1.0, 0.0), 0.85f, Math.max(3.0f, this.damage + 2.0f), 45, 2.0f);
        }
        if (hasModifierEffect(NoitaModifierEffect.ROCKET_OCTAGON) && this.age == 3) {
            spawnRadialProjectiles("rocket", NoitaProjectileBehavior.EXPLOSIVE, 8, Math.max(3.0f, this.damage + 2.0f), 45, 0.75f, 0.0f, 2.0f);
        }
        if (hasModifierEffect(NoitaModifierEffect.FIZZLE)) {
            this.setVelocity(this.getVelocity().multiply(0.94));
            if (this.age > Math.max(4, this.maxAge / 3)) {
                this.discard();
            }
        }
        if (hasModifierEffect(NoitaModifierEffect.AREA_DAMAGE) && this.age % SPELL_EFFECT_INTERVAL_TICKS == 0) {
            damageLivingInRadius(2.4, Math.max(1.0f, this.damage * 0.35f), false);
        }
        if (hasModifierEffect(NoitaModifierEffect.SPELLS_TO_POWER) && this.age % 10 == 0) {
            damageLivingInRadius(4.0, Math.max(2.0f, this.damage * 0.55f), false);
        }
        if (hasModifierEffect(NoitaModifierEffect.CLUSTERMOD) && this.age % 16 == 0) {
            spawnRadialProjectiles("spark_bolt", NoitaProjectileBehavior.BOLT, 3, Math.max(1.0f, this.damage * 0.35f), 24, 0.55f, 0.0f, 0.8f);
        }
        if (hasModifierEffect(NoitaModifierEffect.ESSENCE_TO_POWER)) {
            LivingEntity nearest = findNearestLivingTarget(5.0);
            if (nearest != null) {
                this.damage = Math.min(this.damage + 0.08f, 120.0f);
            }
        }
        if (hasModifierEffect(NoitaModifierEffect.MATTER_EATER) && this.age % 2 == 0) {
            eatCellsAround(1);
        }
        if (hasModifierEffect(NoitaModifierEffect.RANDOM_EXPLOSION) && this.age % 20 == 0 && this.random.nextInt(3) == 0) {
            float radius = 1.0f + this.random.nextFloat() * 2.5f;
            this.getWorld().createExplosion(this, this.getX(), this.getY(), this.getZ(), radius, false, World.ExplosionSourceType.TNT);
        }
        if (hasModifierEffect(NoitaModifierEffect.LASER_EMITTER_WIDER) && this.age % 8 == 0) {
            spawnSideBeams("laser_emitter", 2, 0.9f, Math.max(2.0f, this.damage * 0.45f), 18, 0.0f);
        }
        if (hasModifierEffect(NoitaModifierEffect.FIREBALL_RAY) && this.age % 10 == 0) {
            spawnRadialProjectiles("fireball", NoitaProjectileBehavior.EXPLOSIVE, 1, Math.max(3.0f, this.damage * 0.7f), 35, 0.7f, 0.02f, 2.0f);
        }
        if (hasModifierEffect(NoitaModifierEffect.LIGHTNING_RAY) && this.age % 10 == 0) {
            strikeLightning(this.getPos().add(randomHorizontalDirection().multiply(1.5)), true);
        }
        if (hasModifierEffect(NoitaModifierEffect.TENTACLE_RAY) && this.age % 10 == 0) {
            spawnRadialProjectiles("tentacle", NoitaProjectileBehavior.BEAM, 1, Math.max(3.0f, this.damage * 0.7f), 28, 0.55f, 0.0f, 0.0f);
        }
        if (hasModifierEffect(NoitaModifierEffect.LASER_EMITTER_RAY) && this.age % 9 == 0) {
            spawnRadialProjectiles("laser_emitter", NoitaProjectileBehavior.BEAM, 1, Math.max(2.0f, this.damage * 0.55f), 24, 0.85f, 0.0f, 0.0f);
        }
        if (hasModifierEffect(NoitaModifierEffect.FIREBALL_RAY_LINE) && this.age % 12 == 0) {
            Vec3d forward = getPayloadDirection();
            spawnDirectedProjectiles("fireball", NoitaProjectileBehavior.EXPLOSIVE, 1, forward, 0.7f, Math.max(3.0f, this.damage * 0.7f), 35, 2.0f);
            spawnDirectedProjectiles("fireball", NoitaProjectileBehavior.EXPLOSIVE, 1, forward.multiply(-1.0), 0.7f, Math.max(3.0f, this.damage * 0.7f), 35, 2.0f);
        }
        applyOrbitAndLarpaModifiers();
        applyArcModifiers();
        applyHomingModifiers();
        applyTrailModifiers(world);
    }

    private void applyHomingModifiers() {
        if (hasModifierEffect(NoitaModifierEffect.HOMING_SHOOTER)) {
            steerTowardOwner(0.18, true);
            return;
        }
        if (hasModifierEffect(NoitaModifierEffect.HOMING_CURSOR)) {
            steerTowardCursor(0.16);
            return;
        }

        boolean antiHoming = hasModifierEffect(NoitaModifierEffect.ANTI_HOMING);
        boolean homing = antiHoming
            || hasModifierEffect(NoitaModifierEffect.HOMING)
            || hasModifierEffect(NoitaModifierEffect.HOMING_WAND)
            || hasModifierEffect(NoitaModifierEffect.HOMING_SHORT)
            || hasModifierEffect(NoitaModifierEffect.HOMING_ROTATE)
            || hasModifierEffect(NoitaModifierEffect.HOMING_ACCELERATING)
            || hasModifierEffect(NoitaModifierEffect.HOMING_AREA);
        if (!homing) {
            return;
        }

        double radius = hasModifierEffect(NoitaModifierEffect.HOMING_SHORT) ? 8.0 : 18.0;
        LivingEntity target = findNearestLivingTarget(radius);
        if (target == null) {
            return;
        }
        double strength = hasModifierEffect(NoitaModifierEffect.HOMING_ROTATE) ? 0.11 : 0.18;
        if (hasModifierEffect(NoitaModifierEffect.HOMING_ACCELERATING)) {
            this.setVelocity(this.getVelocity().multiply(1.018));
            strength = 0.22;
        }
        if (hasModifierEffect(NoitaModifierEffect.HOMING_AREA) && this.squaredDistanceTo(target) < 9.0) {
            this.setPosition(target.getX(), target.getBodyY(0.5), target.getZ());
        }
        steerToward(target.getEyePos(), strength, antiHoming);
    }

    private void applyModifierEntityEffect(LivingEntity livingTarget) {
        if (hasModifierEffect(NoitaModifierEffect.FREEZE_CHARGE)) {
            livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 3), this);
            livingTarget.setFrozenTicks(Math.max(livingTarget.getFrozenTicks(), 180));
        }
        if (hasModifierEffect(NoitaModifierEffect.ELECTRIC_CHARGE)) {
            livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 1), this);
            strikeLightning(livingTarget.getPos(), true);
        }
        if (hasModifierEffect(NoitaModifierEffect.POISON_TRAIL) || hasModifierEffect(NoitaModifierEffect.ACID_TRAIL)) {
            livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 1), this);
        }
        if (hasModifierEffect(NoitaModifierEffect.OIL_TRAIL)) {
            livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 0), this);
        }
        if (hasModifierEffect(NoitaModifierEffect.WATER_TRAIL)) {
            livingTarget.extinguish();
        }
        if (hasModifierEffect(NoitaModifierEffect.HITFX_BURNING_CRITICAL_HIT) && livingTarget.isOnFire()) {
            NoitaSpellDamage.apply(livingTarget, this.getDamageSources().indirectMagic(this, this.getOwner() == null ? this : this.getOwner()), Math.max(1.0f, this.damage * 3.0f));
        }
        if (hasModifierEffect(NoitaModifierEffect.HITFX_CRITICAL_WATER) && (livingTarget.isTouchingWaterOrRain() || livingTarget.isWet())) {
            NoitaSpellDamage.apply(livingTarget, this.getDamageSources().indirectMagic(this, this.getOwner() == null ? this : this.getOwner()), Math.max(1.0f, this.damage * 3.0f));
        }
        if (hasModifierEffect(NoitaModifierEffect.HITFX_CRITICAL_OIL) && livingTarget.hasStatusEffect(StatusEffects.SLOWNESS)) {
            NoitaSpellDamage.apply(livingTarget, this.getDamageSources().indirectMagic(this, this.getOwner() == null ? this : this.getOwner()), Math.max(1.0f, this.damage * 2.0f));
        }
        if (hasModifierEffect(NoitaModifierEffect.HITFX_CRITICAL_BLOOD) && livingTarget.getHealth() < livingTarget.getMaxHealth()) {
            NoitaSpellDamage.apply(livingTarget, this.getDamageSources().indirectMagic(this, this.getOwner() == null ? this : this.getOwner()), Math.max(1.0f, this.damage * 2.0f));
        }
        if (hasModifierEffect(NoitaModifierEffect.HITFX_TOXIC_CHARM)) {
            livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 180, 0), this);
            livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 180, 1), this);
        }
        if (hasModifierEffect(NoitaModifierEffect.HITFX_EXPLOSION_SLIME) && livingTarget.hasStatusEffect(StatusEffects.SLOWNESS)) {
            this.getWorld().createExplosion(this, livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(), 2.0f, false, World.ExplosionSourceType.TNT);
        }
        if (hasModifierEffect(NoitaModifierEffect.HITFX_EXPLOSION_SLIME_GIGA) && livingTarget.hasStatusEffect(StatusEffects.SLOWNESS)) {
            this.getWorld().createExplosion(this, livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(), 5.0f, false, World.ExplosionSourceType.TNT);
        }
        if (hasModifierEffect(NoitaModifierEffect.HITFX_EXPLOSION_ALCOHOL) && livingTarget.hasStatusEffect(StatusEffects.NAUSEA)) {
            this.getWorld().createExplosion(this, livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(), 2.0f, true, World.ExplosionSourceType.TNT);
        }
        if (hasModifierEffect(NoitaModifierEffect.HITFX_EXPLOSION_ALCOHOL_GIGA) && livingTarget.hasStatusEffect(StatusEffects.NAUSEA)) {
            this.getWorld().createExplosion(this, livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(), 5.0f, true, World.ExplosionSourceType.TNT);
        }
        if (hasModifierEffect(NoitaModifierEffect.HITFX_PETRIFY)) {
            livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 220, 5), this);
            livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 220, 2), this);
        }
        if (hasModifierEffect(NoitaModifierEffect.NECROMANCY) && livingTarget.isDead()) {
            spawnNecromancyEcho(livingTarget);
        }
        if (hasModifierEffect(NoitaModifierEffect.CLUSTERMOD)) {
            spawnRadialProjectiles("spark_bolt", NoitaProjectileBehavior.BOLT, 5, Math.max(1.0f, this.damage * 0.35f), 24, 0.6f, 0.0f, 0.8f);
        }
        if (hasModifierEffect(NoitaModifierEffect.FIREBALL_RAY_ENEMY)) {
            spawnProjectileFrom(livingTarget.getPos().add(0.0, livingTarget.getHeight() * 0.5, 0.0), randomHorizontalDirection(), "fireball", NoitaProjectileBehavior.EXPLOSIVE, Math.max(3.0f, this.damage * 0.65f), 35, 0.75f, 0.02f, 2.0f, false);
        }
        if (hasModifierEffect(NoitaModifierEffect.LIGHTNING_RAY_ENEMY)) {
            strikeLightning(livingTarget.getPos(), true);
        }
        if (hasModifierEffect(NoitaModifierEffect.TENTACLE_RAY_ENEMY)) {
            spawnProjectileFrom(livingTarget.getPos().add(0.0, livingTarget.getHeight() * 0.5, 0.0), randomHorizontalDirection(), "tentacle", NoitaProjectileBehavior.BEAM, Math.max(3.0f, this.damage * 0.65f), 28, 0.55f, 0.0f, 0.0f, false);
        }
        if (hasModifierEffect(NoitaModifierEffect.GRAVITY_FIELD_ENEMY)) {
            pullNearbyEntitiesTo(livingTarget.getPos(), 4.0, 0.24, livingTarget);
        }
        applyCurseEffects(livingTarget);
    }

    private void applyModifierBlockEffect(BlockPos blockPos) {
        if (hasModifierEffect(NoitaModifierEffect.FREEZE_CHARGE)) {
            freezeAround(blockPos, 1);
        }
        if (hasModifierEffect(NoitaModifierEffect.ELECTRIC_CHARGE)) {
            strikeLightning(Vec3d.ofCenter(blockPos), true);
        }
        if (hasModifierEffect(NoitaModifierEffect.ACID_TRAIL)) {
            dissolveSoftBlocks(blockPos, 1);
        }
        if (hasModifierEffect(NoitaModifierEffect.CLIPPING_SHOT)) {
            digAhead(3);
        }
        if (hasModifierEffect(NoitaModifierEffect.MATTER_EATER)) {
            eatCellsAround(2);
        }
        if (hasModifierEffect(NoitaModifierEffect.WATER_TO_POISON)) {
            poisonWaterAround(blockPos, 2);
        }
        if (hasModifierEffect(NoitaModifierEffect.BLOOD_TO_ACID) || hasModifierEffect(NoitaModifierEffect.TOXIC_TO_ACID)) {
            dissolveSoftBlocks(blockPos, 2);
        }
        if (hasModifierEffect(NoitaModifierEffect.LAVA_TO_BLOOD)) {
            coolLavaAround(blockPos, 2);
        }
        if (hasModifierEffect(NoitaModifierEffect.LIQUID_TO_EXPLOSION) && isNearLiquid(blockPos, 2)) {
            this.getWorld().createExplosion(this, this.getX(), this.getY(), this.getZ(), 3.0f, false, World.ExplosionSourceType.TNT);
        }
        if (hasModifierEffect(NoitaModifierEffect.STATIC_TO_SAND)) {
            sandifyTerrain(blockPos, 2);
        }
        if (hasModifierEffect(NoitaModifierEffect.TRANSMUTATION)) {
            transmuteTerrain(blockPos, 2);
        }
        if (hasModifierEffect(NoitaModifierEffect.CRUMBLING_EARTH_PROJECTILE)) {
            crumbleTerrain();
        }
        if (hasModifierEffect(NoitaModifierEffect.UNSTABLE_GUNPOWDER)) {
            placeMaterialPatch(blockPos, Blocks.TNT.getDefaultState(), 1);
        }
        if (hasModifierEffect(NoitaModifierEffect.CLUSTERMOD)) {
            spawnRadialProjectiles("spark_bolt", NoitaProjectileBehavior.BOLT, 5, Math.max(1.0f, this.damage * 0.35f), 24, 0.6f, 0.0f, 0.8f);
        }
    }

    private void applyTrailModifiers(ServerWorld world) {
        if (this.age % 3 != 0) {
            return;
        }
        BlockPos pos = this.getBlockPos();
        if (hasModifierEffect(NoitaModifierEffect.FIRE_TRAIL) || hasModifierEffect(NoitaModifierEffect.BURN_TRAIL)) {
            igniteAround(1.1, 2, true);
        }
        if (hasModifierEffect(NoitaModifierEffect.UNSTABLE_GUNPOWDER)) {
            placeMaterialPatch(pos, Blocks.TNT.getDefaultState(), 1);
        }
        if (hasModifierEffect(NoitaModifierEffect.ACID_TRAIL)) {
            dissolveSoftBlocks(pos, 1);
        }
        if (hasModifierEffect(NoitaModifierEffect.POISON_TRAIL)) {
            poisonLivingInRadius(1.4, 80);
        }
        if (hasModifierEffect(NoitaModifierEffect.OIL_TRAIL)) {
            placeMaterialPatch(pos, Blocks.BLACK_CONCRETE_POWDER.getDefaultState(), 1);
        }
        if (hasModifierEffect(NoitaModifierEffect.WATER_TRAIL)) {
            placeMaterialPatch(pos, Blocks.WATER.getDefaultState(), 1);
        }
        if (hasModifierEffect(NoitaModifierEffect.GUNPOWDER_TRAIL) && this.random.nextInt(4) == 0) {
            placeMaterialPatch(pos, Blocks.TNT.getDefaultState(), 1);
        }
        if (hasModifierEffect(NoitaModifierEffect.RAINBOW_TRAIL) && this.random.nextInt(3) == 0) {
            placeMaterialPatch(pos, Blocks.GLOWSTONE.getDefaultState(), 1);
            NoitaTemporaryLightManager.illuminateTrail(world, new Vec3d(this.prevX, this.prevY, this.prevZ), this.getPos(), 5);
        }
        if (hasModifierEffect(NoitaModifierEffect.ENERGY_SHIELD_SHOT)) {
            pushNearbyNonLivingProjectiles(2.8, 0.5);
        }
        if (hasModifierEffect(NoitaModifierEffect.COLOUR_RED) || hasModifierEffect(NoitaModifierEffect.COLOUR_ORANGE)) {
            igniteAround(0.8, 1, false);
        }
        if (hasModifierEffect(NoitaModifierEffect.COLOUR_GREEN)) {
            poisonLivingInRadius(0.9, 40);
        }
        if (hasModifierEffect(NoitaModifierEffect.COLOUR_YELLOW) || hasModifierEffect(NoitaModifierEffect.COLOUR_BLUE) || hasModifierEffect(NoitaModifierEffect.COLOUR_PURPLE) || hasModifierEffect(NoitaModifierEffect.COLOUR_RAINBOW)) {
            NoitaTemporaryLightManager.illuminateTrail(world, new Vec3d(this.prevX, this.prevY, this.prevZ), this.getPos(), hasModifierEffect(NoitaModifierEffect.COLOUR_RAINBOW) ? 6 : 4);
        }
    }

    private void applyBounceModifierEffects() {
        if (hasModifierEffect(NoitaModifierEffect.BOUNCE_EXPLOSION)) {
            this.getWorld().createExplosion(this, this.getX(), this.getY(), this.getZ(), 2.6f, false, World.ExplosionSourceType.TNT);
        }
        if (hasModifierEffect(NoitaModifierEffect.BOUNCE_SMALL_EXPLOSION)) {
            this.getWorld().createExplosion(this, this.getX(), this.getY(), this.getZ(), 1.4f, false, World.ExplosionSourceType.TNT);
        }
        if (hasModifierEffect(NoitaModifierEffect.BOUNCE_SPARK)) {
            spawnRadialProjectiles("bubbleshot", NoitaProjectileBehavior.BOLT, 3, Math.max(1.0f, this.damage * 0.35f), 25, 0.65f, 0.0f, 0.0f);
        }
        if (hasModifierEffect(NoitaModifierEffect.BOUNCE_LASER)) {
            spawnRadialProjectiles("laser", NoitaProjectileBehavior.BEAM, 2, Math.max(2.0f, this.damage * 0.45f), 20, 0.9f, 0.0f, 0.0f);
        }
        if (hasModifierEffect(NoitaModifierEffect.BOUNCE_LASER_EMITTER)) {
            spawnRadialProjectiles("laser_emitter", NoitaProjectileBehavior.BEAM, 2, Math.max(2.0f, this.damage * 0.45f), 28, 0.7f, 0.0f, 0.0f);
        }
        if (hasModifierEffect(NoitaModifierEffect.BOUNCE_LARPA)) {
            spawnLarpaCopies(4, 0.65f, 0.45f, 30, false, false);
        }
        if (hasModifierEffect(NoitaModifierEffect.BOUNCE_LIGHTNING)) {
            strikeLightning(this.getPos(), true);
        }
        if (hasModifierEffect(NoitaModifierEffect.BOUNCE_HOLE)) {
            eatCellsAround(2);
            pushNearbyEntities(0.16, 5.0);
        }
    }

    private void applyOrbitAndLarpaModifiers() {
        if (hasModifierEffect(NoitaModifierEffect.ORBIT_DISCS) && this.age % 8 == 0) {
            spawnOrbiter("disc_bullet", NoitaProjectileBehavior.DISC, Math.max(1.5f, this.damage * 0.45f), 28, 0.7f, 0.0f, 0.0f);
        }
        if (hasModifierEffect(NoitaModifierEffect.ORBIT_FIREBALLS) && this.age % 10 == 0) {
            spawnOrbiter("fireball", NoitaProjectileBehavior.EXPLOSIVE, Math.max(3.0f, this.damage * 0.65f), 32, 0.7f, 0.02f, 2.0f);
        }
        if (hasModifierEffect(NoitaModifierEffect.ORBIT_NUKES) && this.age % 20 == 0) {
            spawnOrbiter("nuke", NoitaProjectileBehavior.EXPLOSIVE, Math.max(10.0f, this.damage + 8.0f), 45, 0.45f, 0.02f, 6.0f);
        }
        if (hasModifierEffect(NoitaModifierEffect.ORBIT_LASERS) && this.age % 6 == 0) {
            spawnOrbiter("laser", NoitaProjectileBehavior.BEAM, Math.max(2.0f, this.damage * 0.5f), 20, 0.8f, 0.0f, 0.0f);
        }
        if (hasModifierEffect(NoitaModifierEffect.ORBIT_LARPA) && this.age % 12 == 0) {
            spawnLarpaCopies(2, 0.55f, 0.4f, 24, false, false);
        }
        if (hasModifierEffect(NoitaModifierEffect.LARPA_CHAOS) && this.age % 8 == 0) {
            spawnLarpaCopies(2, 0.75f, 0.5f, 32, false, false);
        }
        if (hasModifierEffect(NoitaModifierEffect.LARPA_DOWNWARDS) && this.age % 8 == 0) {
            spawnDirectedProjectiles(this.itemPath, this.behavior, 1, new Vec3d(0.0, -1.0, 0.0), 0.75f, Math.max(1.0f, this.damage * 0.55f), 32, this.explosionRadius * 0.5f);
        }
        if (hasModifierEffect(NoitaModifierEffect.LARPA_UPWARDS) && this.age % 8 == 0) {
            spawnDirectedProjectiles(this.itemPath, this.behavior, 1, new Vec3d(0.0, 1.0, 0.0), 0.75f, Math.max(1.0f, this.damage * 0.55f), 32, this.explosionRadius * 0.5f);
        }
        if (hasModifierEffect(NoitaModifierEffect.LARPA_CHAOS_2) && this.age % 5 == 0) {
            spawnLarpaCopies(1, 0.95f, 0.35f, 28, true, false);
        }
    }

    private void applyArcModifiers() {
        if (hasModifierEffect(NoitaModifierEffect.CHAIN_SHOT) && this.age % 7 == 0) {
            chainToNearbyLiving();
        }
        if (hasModifierEffect(NoitaModifierEffect.ARC_ELECTRIC) && this.age % 8 == 0) {
            LivingEntity nearest = findNearestLivingTarget(5.0);
            if (nearest != null) {
                strikeLightning(nearest.getPos(), true);
            }
        }
        if (hasModifierEffect(NoitaModifierEffect.ARC_FIRE) && this.age % 6 == 0) {
            igniteAround(1.8, 2, true);
        }
        if (hasModifierEffect(NoitaModifierEffect.ARC_GUNPOWDER) && this.age % 6 == 0) {
            placeMaterialPatch(this.getBlockPos(), Blocks.TNT.getDefaultState(), 1);
        }
        if (hasModifierEffect(NoitaModifierEffect.ARC_POISON) && this.age % 6 == 0) {
            poisonLivingInRadius(2.2, 80);
        }
    }

    private void applyCurseEffects(LivingEntity livingTarget) {
        if (hasModifierEffect(NoitaModifierEffect.CURSE)) {
            livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 160, 1), this);
        }
        if (hasModifierEffect(NoitaModifierEffect.CURSE_WITHER_PROJECTILE)) {
            livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 220, 1), this);
        }
        if (hasModifierEffect(NoitaModifierEffect.CURSE_WITHER_EXPLOSION)) {
            livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 160, 0), this);
            this.explosionRadius = 0.0f;
        }
        if (hasModifierEffect(NoitaModifierEffect.CURSE_WITHER_MELEE)) {
            livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 220, 1), this);
        }
        if (hasModifierEffect(NoitaModifierEffect.CURSE_WITHER_ELECTRICITY)) {
            livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 220, 0), this);
            livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 1), this);
        }
    }

    private void applyProjectileKnockback(Entity target) {
        if (this.knockbackForce == 0.0f) {
            return;
        }
        Vec3d direction = target.getPos().subtract(this.getPos());
        if (direction.lengthSquared() < 1.0E-6) {
            direction = this.getVelocity();
        }
        if (direction.lengthSquared() < 1.0E-6) {
            return;
        }
        double strength = Math.max(-4.0, Math.min(4.0, this.knockbackForce * 0.18));
        Vec3d impulse = direction.normalize().multiply(strength);
        target.addVelocity(impulse.x, impulse.y + Math.abs(strength) * 0.08, impulse.z);
        target.velocityModified = true;
    }

    private void spawnOrbiter(String itemPath, NoitaProjectileBehavior behavior, float damageAmount, int lifetimeTicks, float speed, float gravityAmount, float explosion) {
        double angle = this.age * 0.5;
        Vec3d direction = new Vec3d(Math.cos(angle), 0.15 * Math.sin(angle * 0.7), Math.sin(angle));
        spawnProjectileFrom(this.getPos().add(direction.multiply(0.8)), direction, itemPath, behavior, damageAmount, lifetimeTicks, speed, gravityAmount, explosion, false);
    }

    private void spawnLarpaCopies(int count, float speedScale, float damageScale, int lifetimeTicks, boolean trailDirection, boolean deathExplosion) {
        Vec3d base = trailDirection && this.getVelocity().lengthSquared() > 1.0E-6 ? this.getVelocity().normalize() : getPayloadDirection();
        for (int i = 0; i < count; i++) {
            Vec3d direction = base.add(randomHorizontalDirection().multiply(0.65)).normalize();
            spawnProjectileFrom(
                this.getPos(),
                direction,
                this.itemPath,
                this.behavior,
                Math.max(1.0f, this.damage * damageScale),
                lifetimeTicks,
                Math.max(0.25f, (float) this.getVelocity().length() * speedScale),
                this.gravity,
                deathExplosion ? Math.max(1.5f, this.explosionRadius) : this.explosionRadius * 0.5f,
                false
            );
        }
    }

    private void spawnRadialProjectiles(String itemPath, NoitaProjectileBehavior behavior, int count, float damageAmount, int lifetimeTicks, float speed, float gravityAmount, float explosion) {
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0 * i / Math.max(1, count)) + this.random.nextDouble() * 0.25;
            Vec3d direction = new Vec3d(Math.cos(angle), (this.random.nextDouble() - 0.5) * 0.2, Math.sin(angle));
            spawnProjectileFrom(this.getPos(), direction, itemPath, behavior, damageAmount, lifetimeTicks, speed, gravityAmount, explosion, false);
        }
    }

    private void spawnDirectedProjectiles(String itemPath, NoitaProjectileBehavior behavior, int count, Vec3d direction, float speed, float damageAmount, int lifetimeTicks, float explosion) {
        Vec3d safeDirection = direction.lengthSquared() > 1.0E-6 ? direction.normalize() : getPayloadDirection();
        for (int i = 0; i < count; i++) {
            Vec3d jittered = safeDirection.add(randomHorizontalDirection().multiply(count <= 1 ? 0.0 : 0.2)).normalize();
            spawnProjectileFrom(this.getPos(), jittered, itemPath, behavior, damageAmount, lifetimeTicks, speed, this.gravity, explosion, false);
        }
    }

    private void spawnSideBeams(String itemPath, int count, float speed, float damageAmount, int lifetimeTicks, float explosion) {
        Vec3d forward = getPayloadDirection();
        Vec3d side = new Vec3d(-forward.z, 0.0, forward.x);
        if (side.lengthSquared() < 1.0E-6) {
            side = new Vec3d(1.0, 0.0, 0.0);
        }
        for (int i = 0; i < count; i++) {
            Vec3d direction = i % 2 == 0 ? side : side.multiply(-1.0);
            spawnProjectileFrom(this.getPos(), direction, itemPath, NoitaProjectileBehavior.BEAM, damageAmount, lifetimeTicks, speed, 0.0f, explosion, true);
        }
    }

    private void spawnProjectileFrom(
        Vec3d position,
        Vec3d direction,
        String itemPath,
        NoitaProjectileBehavior behavior,
        float damageAmount,
        int lifetimeTicks,
        float speed,
        float gravityAmount,
        float explosion,
        boolean piercingProjectile
    ) {
        Entity owner = this.getOwner();
        LivingEntity livingOwner = owner instanceof LivingEntity living ? living : null;
        NoitaProjectilePayload payload = new NoitaProjectilePayload(
            itemPath,
            behavior,
            damageAmount,
            0.0f,
            lifetimeTicks,
            Math.max(0, this.trailLightStacks - 1),
            Math.max(0.0f, explosion),
            Math.max(0.1f, speed),
            0.0f,
            gravityAmount,
            0.99f,
            this.bounceDamping,
            0.75f,
            this.knockbackForce * 0.5f,
            this.friendlyFire,
            piercingProjectile || behavior.isBeamLike(),
            1,
            0.0f,
            NoitaSpellTriggerMode.NONE,
            0,
            0,
            List.of(),
            List.of()
        );
        spawnPayloadProjectile(this.getWorld(), livingOwner, position, direction, payload);
    }

    private Vec3d randomHorizontalDirection() {
        Vec3d direction = new Vec3d(this.random.nextDouble() - 0.5, (this.random.nextDouble() - 0.5) * 0.25, this.random.nextDouble() - 0.5);
        if (direction.lengthSquared() < 1.0E-6) {
            return new Vec3d(1.0, 0.0, 0.0);
        }
        return direction.normalize();
    }

    private void onLifetimeExpired() {
        String path = normalizedItemPath();
        if (isStaticTeleport()) {
            teleportOwnerToReturnAnchor();
        } else if (path.equals("hook")) {
            pullOwnerToHook();
        } else if (isDeathCross()) {
            this.getWorld().createExplosion(this, this.getX(), this.getY(), this.getZ(), normalizedItemPath().endsWith("_big") ? 4.0f : 2.6f, true, World.ExplosionSourceType.TNT);
        } else if (path.equals("firework")) {
            this.getWorld().createExplosion(this, this.getX(), this.getY(), this.getZ(), 2.0f, false, World.ExplosionSourceType.TNT);
        } else if (path.equals("infestation")) {
            for (int i = 0; i < 6; i++) {
                spawnInfestationShard();
            }
        } else if (path.equals("spore_pod")) {
            burstSporePod();
        } else if (path.equals("exploding_ducks")) {
            explodeNoitaProjectile(2.5f, false);
        } else if (path.equals("summon_egg")) {
            hatchSummonedEgg();
        } else if (path.equals("summon_hollow_egg")) {
            burstHollowEgg();
            releaseTriggerPayload();
        } else if (path.equals("expanding_orb")) {
            explodeNoitaProjectile(1.8f, false);
        }
        if (hasModifierEffect(NoitaModifierEffect.LARPA_DEATH)) {
            spawnLarpaCopies(6, 0.8f, 0.45f, 32, false, true);
        }
    }

    private void applyMistCloud() {
        if (this.age % SPELL_EFFECT_INTERVAL_TICKS != 0) {
            return;
        }
        String path = normalizedItemPath();
        Box area = this.getBoundingBox().expand(3.0);
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            if (!(entity instanceof LivingEntity livingTarget)) {
                continue;
            }
            NoitaSpellDamage.apply(entity, this.getDamageSources().indirectMagic(this, this.getOwner() == null ? this : this.getOwner()), Math.max(0.5f, this.damage));
            if (path.equals("mist_radioactive")) {
                livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 80, 0), this);
            } else if (path.equals("mist_alcohol")) {
                livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0), this);
                livingTarget.addVelocity((this.random.nextDouble() - 0.5) * 0.08, 0.03, (this.random.nextDouble() - 0.5) * 0.08);
            } else if (path.equals("mist_slime")) {
                livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 2), this);
            } else if (path.equals("mist_blood")) {
                livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 0), this);
            }
        }
        if (path.equals("mist_slime")) {
            placeMaterialPatch(this.getBlockPos(), Blocks.SLIME_BLOCK.getDefaultState(), 1);
        } else if (path.equals("mist_blood")) {
            placeMaterialPatch(this.getBlockPos(), Blocks.REDSTONE_WIRE.getDefaultState(), 1);
        } else if (path.equals("mist_alcohol")) {
            igniteAround(1.8, 1, false);
        }
    }

    private void applyShieldField() {
        Entity owner = this.getOwner();
        if (owner != null) {
            Vec3d anchor = owner.getEyePos().add(owner.getRotationVec(1.0f).multiply(1.3));
            this.setPosition(anchor);
            this.setVelocity(Vec3d.ZERO);
        }
        double radius = normalizedItemPath().startsWith("big_") ? 4.0 : 2.4;
        Box area = this.getBoundingBox().expand(radius);
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            if (entity == owner || entity instanceof LivingEntity) {
                continue;
            }
            Vec3d away = entity.getPos().subtract(this.getPos());
            if (away.lengthSquared() > 1.0E-6) {
                Vec3d impulse = away.normalize().multiply(0.45);
                entity.addVelocity(impulse.x, impulse.y + 0.05, impulse.z);
                entity.velocityModified = true;
            }
        }
    }

    private void applyBeamTickEffect(String path) {
        if (path.equals("lightning") || path.equals("ball_lightning")) {
            if (this.age % 4 == 0) {
                strikeLightning(this.getPos(), true);
            }
        } else if (path.equals("freezing_gaze")) {
            freezeAround(this.getBlockPos(), 1);
            damageLivingInRadius(1.6, 1.0f, false);
        } else if (path.equals("laser_emitter") || path.equals("laser_emitter_four") || path.equals("laser_emitter_cutter")) {
            applyPlasmaEmitterTick(path);
        }
        if (this.behavior.digsOnCollision() || path.contains("laser_emitter") || path.equals("laser") || path.equals("megalaser")) {
            digAhead(path.equals("chainsaw") ? 1 : 2);
        }
        if (path.equals("megalaser_beam")) {
            digAhead(3);
            damageLivingInRadius(1.8, Math.max(18.0f, this.damage), true);
        }
        if (path.equals("chainsaw")) {
            damageLivingInRadius(1.2, this.damage + 1.0f, false);
        }
    }

    private void applyPlasmaEmitterTick(String path) {
        this.setVelocity(this.getVelocity().multiply(0.93));
        if (this.age < 2) {
            return;
        }
        int beamCount = path.equals("laser_emitter_four") ? 4 : 1;
        double baseYaw = Math.atan2(this.getVelocity().z, this.getVelocity().x);
        if (this.getVelocity().lengthSquared() < 1.0E-6) {
            baseYaw = Math.toRadians(this.getYaw());
        }
        double length = path.equals("laser_emitter_cutter") ? 4.0 : path.equals("laser_emitter_four") ? 7.0 : 10.0;
        float beamDamage = path.equals("laser_emitter_four") ? 1.6f : path.equals("laser_emitter_cutter") ? 1.8f : 2.4f;
        int digDepth = path.equals("laser_emitter_cutter") ? 4 : 2;
        for (int i = 0; i < beamCount; i++) {
            double angle = baseYaw + (Math.PI * 2.0 * i / beamCount);
            Vec3d direction = new Vec3d(Math.cos(angle), 0.0, Math.sin(angle));
            traceBeam(direction, length, beamDamage, digDepth);
        }
    }

    private void traceBeam(Vec3d direction, double length, float beamDamage, int digDepth) {
        World world = this.getWorld();
        for (int step = 1; step <= Math.ceil(length); step++) {
            Vec3d point = this.getPos().add(direction.multiply(step));
            NoitaTemporaryLightManager.illuminateTrail((ServerWorld) world, this.getPos(), point, 3);
            if (step <= digDepth) {
                BlockPos blockPos = BlockPos.ofFloored(point);
                BlockState state = world.getBlockState(blockPos);
                if (!state.isAir() && state.getHardness(world, blockPos) >= 0.0f && state.getHardness(world, blockPos) <= 14.0f) {
                    world.breakBlock(blockPos, false, this, 16);
                }
            }
            Box area = Box.of(point, 0.9, 0.9, 0.9);
            for (Entity entity : world.getOtherEntities(this, area)) {
                if (entity instanceof LivingEntity) {
                    NoitaSpellDamage.apply(entity, this.getDamageSources().indirectMagic(this, this.getOwner() == null ? this : this.getOwner()), beamDamage);
                }
            }
        }
    }

    private void applyHookTick() {
        if (this.age % 3 == 0) {
            NoitaTemporaryLightManager.illuminateTrail((ServerWorld) this.getWorld(), this.getOwner() == null ? this.getPos() : this.getOwner().getEyePos(), this.getPos(), 3);
        }
    }

    private void applyAntiGravityGrenadeTick() {
        Entity owner = this.getOwner();
        if (owner == null || this.age % 2 != 0) {
            return;
        }
        double verticalPush = owner.getY() > this.getY() ? 0.09 : -0.09;
        Vec3d velocity = this.getVelocity();
        this.setVelocity(velocity.x * 0.98, velocity.y + verticalPush, velocity.z * 0.98);
        this.velocityModified = true;
    }

    private void applyDarkflameTick() {
        if (this.age % 3 != 0) {
            return;
        }
        Vec3d offset = new Vec3d(
            (this.random.nextDouble() - 0.5) * 1.0,
            (this.random.nextDouble() - 0.5) * 0.5,
            (this.random.nextDouble() - 0.5) * 1.0
        );
        Vec3d flamePos = this.getPos().add(offset);
        Box area = Box.of(flamePos, 1.6, 1.6, 1.6);
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            entity.setOnFireFor(5);
            if (entity instanceof LivingEntity) {
                NoitaSpellDamage.apply(entity, this.getDamageSources().indirectMagic(this, this.getOwner() == null ? this : this.getOwner()), 1.0f);
            }
        }
        BlockPos blockPos = BlockPos.ofFloored(flamePos);
        if (this.random.nextInt(3) == 0 && this.getWorld().getBlockState(blockPos).isAir()) {
            BlockState fire = AbstractFireBlock.getState(this.getWorld(), blockPos);
            if (fire.canPlaceAt(this.getWorld(), blockPos)) {
                this.getWorld().setBlockState(blockPos, fire, 11);
            }
        }
    }

    private void applyExpandingOrbTick() {
        double progress = Math.min(1.0, this.age / 115.0);
        double radius = 4.0 + (1.0 - progress) * 4.0;
        float damageAmount = (float) (Math.max(0.1, this.damage) * (1.0 - progress) + 0.1 * progress);
        if (this.age % 15 == 0) {
            damageLivingInRadius(radius, damageAmount, true);
        }
        if (this.age % 8 == 0) {
            pushNearbyEntities(-0.035 * (1.0 - progress), radius);
            NoitaTemporaryLightManager.illuminateTrail((ServerWorld) this.getWorld(), this.getPos(), this.getPos().add(0.0, 0.1, 0.0), (int) Math.ceil(radius));
        }
    }

    private void applyCursedOrbTick() {
        if (this.age % 8 != 0) {
            return;
        }
        Box area = this.getBoundingBox().expand(2.0);
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            if (entity instanceof LivingEntity living) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 80, 0), this);
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 0), this);
                NoitaSpellDamage.apply(entity, this.getDamageSources().indirectMagic(this, this.getOwner() == null ? this : this.getOwner()), Math.max(1.0f, this.damage * 0.4f));
            }
        }
    }

    private void applyMegalaserChargeTick(ServerWorld world) {
        this.setVelocity(this.getVelocity().multiply(0.82));
        NoitaTemporaryLightManager.illuminateTrail(world, new Vec3d(this.prevX, this.prevY, this.prevZ), this.getPos(), 7);
        if (this.age == 30) {
            launchMegalaserBeams();
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 0.8f, 1.25f);
        }
    }

    private void launchMegalaserBeams() {
        Vec3d forward = getPayloadDirection();
        Vec3d side = new Vec3d(-forward.z, 0.0, forward.x);
        side = side.lengthSquared() < 1.0E-6 ? new Vec3d(1.0, 0.0, 0.0) : side.normalize();
        Entity owner = this.getOwner();
        LivingEntity livingOwner = owner instanceof LivingEntity living ? living : null;
        for (int offset = -2; offset <= 2; offset++) {
            double brake = 1.0 - Math.abs(offset) * 0.16;
            NoitaProjectilePayload payload = new NoitaProjectilePayload(
                "megalaser_beam",
                NoitaProjectileBehavior.BEAM,
                Math.max(18.0f, this.damage + 8.0f),
                this.criticalChancePercent,
                250,
                Math.max(6, this.trailLightStacks),
                0.0f,
                1.0f,
                0.0f,
                0.0f,
                1.0f,
                this.bounceDamping,
                1.35f,
                this.knockbackForce,
                this.friendlyFire,
                true,
                1,
                0.0f,
                NoitaSpellTriggerMode.NONE,
                0,
                0,
                this.modifierEffects,
                List.of()
            );
            Vec3d spawnPos = this.getPos().add(side.multiply(offset * 0.35));
            spawnPayloadProjectile(this.getWorld(), livingOwner, spawnPos, forward.multiply(brake), payload);
        }
    }

    private void applyExplodingDuckTick() {
        if (this.age % 2 == 0) {
            applyChaoticArc(0.4);
        }
        steerTowardNearestLiving(0.08, 3.0);
        if (this.age % 10 == 0) {
            damageLivingInRadius(1.0, Math.max(1.6f, this.damage * 0.2f), false);
        }
    }

    private void applyExplodingDeerTick() {
        LivingEntity nearest = findNearestLivingTarget(12.0);
        if (nearest == null) {
            return;
        }
        Vec3d away = this.getPos().subtract(nearest.getPos());
        if (away.lengthSquared() > 1.0E-6) {
            this.setVelocity(this.getVelocity().multiply(0.92).add(away.normalize().multiply(0.12)));
            this.velocityModified = true;
        }
        if (this.squaredDistanceTo(nearest) <= 4.0) {
            explodeNoitaProjectile(this.explosionRadius <= 0.0f ? 3.0f : this.explosionRadius, true);
        }
    }

    private void applyEggTick(String path) {
        if (this.age % 6 == 0) {
            this.setVelocity(this.getVelocity().multiply(0.97));
        }
        if (this.age >= 19) {
            if (path.equals("summon_egg")) {
                hatchSummonedEgg();
            } else {
                burstHollowEgg();
                releaseTriggerPayload();
            }
            this.getWorld().sendEntityStatus(this, HIT_FLASH_STATUS);
            this.discard();
        }
    }

    private void applyOmegaSawbladeTick() {
        if (this.age % 3 == 0) {
            eatCellsAround(1);
        }
        damageLivingInRadius(2.6, Math.max(2.5f, this.damage + 2.0f), true);
    }

    private void applyBigSawbladeTick() {
        if (this.age % 5 == 0) {
            eatCellsAround(1);
        }
        damageLivingInRadius(1.8, Math.max(2.0f, this.damage + 1.5f), true);
    }

    private void applyWormShotTick() {
        steerTowardNearestLiving(0.12, 8.0);
        if (this.age % 2 == 0) {
            eatCellsAround(1);
        }
        damageLivingInRadius(1.8, Math.max(1.0f, this.damage), true);
        this.setVelocity(this.getVelocity().multiply(0.985));
    }

    private void applyTentacleField(String path) {
        double radius = path.equals("tentacle_portal") ? 4.0 : 2.6;
        if (this.age % SPELL_EFFECT_INTERVAL_TICKS == 0) {
            damageLivingInRadius(radius, this.damage + (path.equals("tentacle_portal") ? 3.0f : 1.0f), false);
        }
        Box area = this.getBoundingBox().expand(radius);
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            if (entity == this.getOwner()) {
                continue;
            }
            Vec3d pull = this.getPos().subtract(entity.getPos());
            if (pull.lengthSquared() > 1.0E-6) {
                Vec3d impulse = pull.normalize().multiply(path.equals("tentacle_portal") ? 0.18 : 0.11);
                entity.addVelocity(impulse.x, impulse.y + 0.02, impulse.z);
                entity.velocityModified = true;
            }
        }
        if (path.equals("tentacle_portal") && this.age % 3 == 0) {
            eatCellsAround(1);
        }
    }

    private void applyPollenCloud() {
        Box area = this.getBoundingBox().expand(2.0);
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            if (entity instanceof LivingEntity living) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 0), this);
                NoitaSpellDamage.apply(entity, this.getDamageSources().indirectMagic(this, this.getOwner() == null ? this : this.getOwner()), 1.0f);
            }
        }
    }

    private void applySporePodTick() {
        if (this.age % 4 == 0) {
            applyChaoticArc(0.08);
            this.setVelocity(this.getVelocity().multiply(0.985));
            this.velocityModified = true;
        }
        if (this.age % 10 == 0) {
            damageLivingInRadius(2.4, Math.max(1.0f, this.damage * 0.25f), false);
            poisonLivingInRadius(2.4, 60);
            slowLivingInRadius(2.4, 50, 0);
            placeMaterialPatch(this.getBlockPos(), Blocks.MOSS_CARPET.getDefaultState(), 1);
        }
        if (this.age % 20 == 0) {
            spawnSporeShard(0.45f);
        }
    }

    private void chainToNearbyLiving() {
        Entity owner = this.getOwner();
        Box area = this.getBoundingBox().expand(5.0);
        int chained = 0;
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            if (entity == owner || !(entity instanceof LivingEntity)) {
                continue;
            }
            NoitaSpellDamage.apply(entity, this.getDamageSources().indirectMagic(this, owner == null ? this : owner), Math.max(2.0f, this.damage * 0.75f));
            pullEntityTowardSelf(entity, 0.18);
            if (++chained >= 3) {
                break;
            }
        }
    }

    private void applyFunkyMotion() {
        if (this.age % 3 != 0) {
            return;
        }
        Vec3d velocity = this.getVelocity();
        Vec3d jitter = new Vec3d(
            (this.random.nextDouble() - 0.5) * 0.18,
            (this.random.nextDouble() - 0.5) * 0.12,
            (this.random.nextDouble() - 0.5) * 0.18
        );
        this.setVelocity(velocity.add(jitter).multiply(0.98));
        if (this.random.nextInt(10) == 0) {
            igniteAround(1.0, 1, false);
        }
    }

    private void applyChaoticArc(double scaleMultiplier) {
        Vec3d velocity = this.getVelocity();
        double scale = Math.max(Math.abs(velocity.x), Math.max(Math.abs(velocity.y), Math.abs(velocity.z))) * scaleMultiplier;
        double adjustment = (this.random.nextDouble() * 2.0 - 1.0) * scale;
        this.setVelocity(velocity.add(adjustment, adjustment * 0.35, adjustment));
        this.velocityModified = true;
    }

    private void applyRandomFunkyStatus(LivingEntity livingTarget) {
        int roll = this.random.nextInt(4);
        StatusEffect effect = switch (roll) {
            case 0 -> StatusEffects.POISON;
            case 1 -> StatusEffects.NAUSEA;
            case 2 -> StatusEffects.LEVITATION;
            default -> StatusEffects.SLOWNESS;
        };
        livingTarget.addStatusEffect(new StatusEffectInstance(effect, 80, roll == 2 ? 0 : 1), this);
    }

    private void releaseSummon(ServerWorld world) {
        String path = normalizedItemPath();
        Vec3d pos = this.getPos();
        Entity spawned;
        if (path.equals("fish")) {
            spawned = EntityType.COD.create(world);
        } else if (path.equals("pebble") || path.equals("summon_rock")) {
            spawned = FallingBlockEntity.spawnFromBlock(world, this.getBlockPos(), path.equals("pebble") ? Blocks.MOSSY_COBBLESTONE.getDefaultState() : Blocks.COBBLESTONE.getDefaultState());
            if (spawned instanceof FallingBlockEntity fallingBlock) {
                fallingBlock.setVelocity(this.getVelocity());
                fallingBlock.setHurtEntities(path.equals("pebble") ? 14.0f : 10.0f, path.equals("pebble") ? 80 : 40);
                fallingBlock.dropItem = false;
            }
        } else if (path.equals("summon_egg") || path.equals("summon_hollow_egg")) {
            spawned = new ItemEntity(world, pos.x, pos.y, pos.z, new ItemStack(Items.EGG));
            if (spawned instanceof ItemEntity itemEntity) {
                itemEntity.setThrower(this.getOwner());
                itemEntity.setPickupDelay(40);
                itemEntity.setNeverDespawn();
            }
        } else {
            spawned = null;
        }
        if (spawned != null && !(spawned instanceof FallingBlockEntity)) {
            spawned.refreshPositionAndAngles(pos.x, pos.y, pos.z, this.getYaw(), this.getPitch());
            spawned.setVelocity(this.getVelocity());
            world.spawnEntity(spawned);
        }
    }

    private void hatchSummonedEgg() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        EntityType<?>[] hatchTypes = {
            EntityType.CHICKEN,
            EntityType.SLIME,
            EntityType.SPIDER,
            EntityType.SILVERFISH,
            EntityType.BAT
        };
        Entity spawned = hatchTypes[this.random.nextInt(hatchTypes.length)].create(serverWorld);
        if (spawned == null) {
            return;
        }
        spawned.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
        spawned.setVelocity(this.getVelocity().multiply(0.35));
        serverWorld.spawnEntity(spawned);
    }

    private void burstHollowEgg() {
        if (!(this.getWorld() instanceof ServerWorld)) {
            return;
        }
        damageLivingInRadius(2.4, Math.max(2.0f, this.damage * 0.35f), false);
        placeMaterialPatch(this.getBlockPos(), Blocks.SLIME_BLOCK.getDefaultState(), 1);
    }

    private void burstSporePod() {
        if (this.getWorld().isClient) {
            return;
        }
        damageLivingInRadius(3.0, Math.max(2.0f, this.damage * 0.45f), false);
        poisonLivingInRadius(3.0, 100);
        slowLivingInRadius(3.0, 80, 1);
        placeMaterialPatch(this.getBlockPos(), Blocks.MOSS_CARPET.getDefaultState(), 2);
        for (int i = 0; i < 7; i++) {
            spawnSporeShard(0.55f + this.random.nextFloat() * 0.35f);
        }
    }

    private void spawnSporeShard(float speed) {
        if (this.getWorld().isClient) {
            return;
        }
        Vec3d direction = new Vec3d(
            this.random.nextDouble() - 0.5,
            this.random.nextDouble() * 0.75 - 0.1,
            this.random.nextDouble() - 0.5
        );
        if (direction.lengthSquared() < 1.0E-6) {
            direction = this.getRotationVec(1.0f);
        }
        NoitaProjectilePayload payload = new NoitaProjectilePayload(
            "pollen",
            NoitaProjectileBehavior.BOLT,
            Math.max(1.0f, this.damage * 0.35f),
            0.0f,
            42,
            this.trailLightStacks,
            0.0f,
            speed,
            16.0f,
            0.01f,
            0.96f,
            0.6f,
            0.65f,
            0.0f,
            this.friendlyFire,
            false,
            1,
            0.0f,
            NoitaSpellTriggerMode.NONE,
            0,
            0,
            List.of(),
            List.of()
        );
        Entity owner = this.getOwner();
        SparkBoltProjectileEntity.spawnPayloadProjectile(this.getWorld(), owner instanceof LivingEntity living ? living : null, this.getPos(), direction, payload);
    }

    private void spawnInfestationShard() {
        if (this.getWorld().isClient) {
            return;
        }
        Vec3d direction = new Vec3d(this.random.nextDouble() - 0.5, this.random.nextDouble() * 0.4 - 0.1, this.random.nextDouble() - 0.5);
        if (direction.lengthSquared() < 1.0E-6) {
            direction = this.getRotationVec(1.0f);
        }
        NoitaProjectilePayload payload = new NoitaProjectilePayload(
            "spitter",
            NoitaProjectileBehavior.BOLT,
            Math.max(1.0f, this.damage * 0.5f),
            0.0f,
            35,
            this.trailLightStacks,
            0.0f,
            0.7f,
            8.0f,
            0.02f,
            0.99f,
            0.65f,
            0.75f,
            0.0f,
            this.friendlyFire,
            false,
            1,
            0.0f,
            NoitaSpellTriggerMode.NONE,
            0,
            0,
            List.of(),
            List.of()
        );
        Entity owner = this.getOwner();
        SparkBoltProjectileEntity.spawnPayloadProjectile(this.getWorld(), owner instanceof LivingEntity living ? living : null, this.getPos(), direction, payload);
    }

    private void eatCellsAround(int radius) {
        World world = this.getWorld();
        BlockPos center = this.getBlockPos();
        int radiusSquared = radius * radius;
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))) {
            if (pos.getSquaredDistance(center) > radiusSquared) {
                continue;
            }
            BlockState state = world.getBlockState(pos);
            if (!canEatBlock(world, pos, state)) {
                continue;
            }
            world.breakBlock(pos, false, this, 16);
        }
    }

    private boolean canEatBlock(World world, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        float hardness = state.getHardness(world, pos);
        return hardness >= 0.0f && hardness <= 18.0f;
    }

    private void damageLivingInRadius(double radius, float damageAmount, boolean includeOwner) {
        Entity owner = this.getOwner();
        Box area = this.getBoundingBox().expand(radius);
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            if (!includeOwner && entity == owner) {
                continue;
            }
            if (entity instanceof LivingEntity) {
                NoitaSpellDamage.apply(entity, this.getDamageSources().indirectMagic(this, owner == null ? this : owner), damageAmount);
            }
        }
    }

    private void poisonLivingInRadius(double radius, int durationTicks) {
        Box area = this.getBoundingBox().expand(radius);
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            if (entity instanceof LivingEntity living) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, durationTicks, 0), this);
            }
        }
    }

    private void slowLivingInRadius(double radius, int durationTicks, int amplifier) {
        Box area = this.getBoundingBox().expand(radius);
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            if (entity instanceof LivingEntity living) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, durationTicks, amplifier), this);
            }
        }
    }

    private void healLivingInRadius(double radius, float amount) {
        Box area = this.getBoundingBox().expand(radius);
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            if (entity instanceof LivingEntity living) {
                living.heal(amount);
            }
        }
    }

    private void extinguishLivingInRadius(double radius) {
        Box area = this.getBoundingBox().expand(radius);
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            if (entity instanceof LivingEntity living) {
                living.extinguish();
            }
        }
    }

    private void pushNearbyNonLivingProjectiles(double radius, double strength) {
        Box area = this.getBoundingBox().expand(radius);
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            if (entity instanceof LivingEntity || entity == this.getOwner()) {
                continue;
            }
            Vec3d away = entity.getPos().subtract(this.getPos());
            if (away.lengthSquared() > 1.0E-6) {
                Vec3d impulse = away.normalize().multiply(strength);
                entity.addVelocity(impulse.x, impulse.y + 0.04, impulse.z);
                entity.velocityModified = true;
            }
        }
    }

    private void pullNearbyEntitiesTo(Vec3d position, double radius, double strength, Entity excluded) {
        Box area = new Box(position, position).expand(radius);
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            if (entity == excluded || entity == this.getOwner()) {
                continue;
            }
            Vec3d pull = position.subtract(entity.getPos());
            if (pull.lengthSquared() > 1.0E-6) {
                Vec3d impulse = pull.normalize().multiply(strength);
                entity.addVelocity(impulse.x, impulse.y + 0.03, impulse.z);
                entity.velocityModified = true;
            }
        }
    }

    private void spawnNecromancyEcho(LivingEntity source) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld) || this.random.nextInt(3) != 0) {
            return;
        }
        Entity spawned = EntityType.ZOMBIE.create(serverWorld);
        if (spawned == null) {
            return;
        }
        spawned.refreshPositionAndAngles(source.getX(), source.getY(), source.getZ(), source.getYaw(), source.getPitch());
        spawned.setVelocity(randomHorizontalDirection().multiply(0.2));
        serverWorld.spawnEntity(spawned);
    }

    private void pullEntityTowardOwner(Entity target, double strength) {
        Entity owner = this.getOwner();
        if (owner == null) {
            return;
        }
        Vec3d pull = owner.getPos().subtract(target.getPos());
        if (pull.lengthSquared() > 1.0E-6) {
            Vec3d velocity = pull.normalize().multiply(strength);
            target.addVelocity(velocity.x, velocity.y + 0.08, velocity.z);
            target.velocityModified = true;
        }
    }

    private void pullOwnerToHook() {
        Entity owner = this.getOwner();
        if (owner == null) {
            return;
        }
        Vec3d pull = this.getPos().subtract(owner.getPos());
        if (pull.lengthSquared() > 1.0E-6) {
            Vec3d velocity = pull.normalize().multiply(Math.min(4.0, Math.max(1.2, pull.length() * 0.35)));
            owner.addVelocity(velocity.x, velocity.y + 0.15, velocity.z);
            owner.velocityModified = true;
        }
    }

    private void pullEntityTowardSelf(Entity target, double strength) {
        Vec3d pull = this.getPos().subtract(target.getPos());
        if (pull.lengthSquared() > 1.0E-6) {
            Vec3d velocity = pull.normalize().multiply(strength);
            target.addVelocity(velocity.x, velocity.y + 0.04, velocity.z);
            target.velocityModified = true;
        }
    }

    private void steerTowardNearestLiving(double strength, double radius) {
        LivingEntity nearest = findNearestLivingTarget(radius);
        if (nearest != null) {
            steerToward(nearest.getEyePos(), strength, false);
        }
    }

    private void steerAwayFromNearestLiving(double radius, double strength) {
        LivingEntity nearest = findNearestLivingTarget(radius);
        if (nearest != null) {
            steerToward(nearest.getEyePos(), strength, true);
        }
    }

    private LivingEntity findNearestLivingTarget(double radius) {
        Entity owner = this.getOwner();
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        Box area = this.getBoundingBox().expand(radius);
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            if (entity == owner || !(entity instanceof LivingEntity living)) {
                continue;
            }
            double distance = this.squaredDistanceTo(entity);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = living;
            }
        }
        return nearest;
    }

    private void steerTowardOwner(double strength, boolean away) {
        Entity owner = this.getOwner();
        if (owner != null) {
            steerToward(owner.getEyePos(), strength, away);
        }
    }

    private void steerTowardCursor(double strength) {
        Entity owner = this.getOwner();
        if (owner == null) {
            return;
        }
        steerToward(owner.getEyePos().add(owner.getRotationVec(1.0f).multiply(24.0)), strength, false);
    }

    private void steerToward(Vec3d targetPosition, double strength, boolean away) {
        Vec3d current = this.getVelocity();
        Vec3d desired = away ? this.getPos().subtract(targetPosition) : targetPosition.subtract(this.getPos());
        if (desired.lengthSquared() < 1.0E-6) {
            return;
        }
        double speed = Math.max(0.15, current.length());
        Vec3d steered = current.multiply(1.0 - strength).add(desired.normalize().multiply(speed * strength));
        this.setVelocity(steered);
        this.velocityModified = true;
    }

    private void pushEntityAwayFromSelf(Entity target, double strength) {
        Vec3d push = target.getPos().subtract(this.getPos());
        if (push.lengthSquared() > 1.0E-6) {
            Vec3d velocity = push.normalize().multiply(strength);
            target.addVelocity(velocity.x, velocity.y + 0.12, velocity.z);
            target.velocityModified = true;
        }
    }

    private void igniteAround(double radius, int seconds, boolean placeFire) {
        World world = this.getWorld();
        Box area = this.getBoundingBox().expand(radius);
        for (Entity entity : world.getOtherEntities(this, area)) {
            entity.setOnFireFor(seconds);
        }
        if (!placeFire) {
            return;
        }
        BlockPos center = this.getBlockPos();
        int intRadius = (int) Math.ceil(radius);
        for (BlockPos pos : BlockPos.iterate(center.add(-intRadius, -1, -intRadius), center.add(intRadius, 1, intRadius))) {
            if (this.random.nextInt(4) != 0 || !world.getBlockState(pos).isAir()) {
                continue;
            }
            BlockState fire = AbstractFireBlock.getState(world, pos);
            if (fire.canPlaceAt(world, pos)) {
                world.setBlockState(pos, fire, 11);
            }
        }
    }

    private void freezeAround(BlockPos center, int radius) {
        World world = this.getWorld();
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))) {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(Blocks.WATER)) {
                world.setBlockState(pos, Blocks.ICE.getDefaultState(), 11);
            } else if (state.isAir() && world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos.down(), Direction.UP) && this.random.nextInt(4) == 0) {
                world.setBlockState(pos, Blocks.SNOW.getDefaultState(), 11);
            }
        }
    }

    private void dissolveSoftBlocks(BlockPos center, int radius) {
        World world = this.getWorld();
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))) {
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            float hardness = state.getHardness(world, pos);
            if (hardness >= 0.0f && hardness <= 4.0f) {
                world.breakBlock(pos, false, this, 16);
            }
        }
    }

    private void poisonWaterAround(BlockPos center, int radius) {
        World world = this.getWorld();
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))) {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(Blocks.WATER)) {
                world.setBlockState(pos, Blocks.LIME_CONCRETE_POWDER.getDefaultState(), 11);
            }
        }
        poisonLivingInRadius(radius + 1.0, 100);
    }

    private void vacuumTerrain(boolean liquidOnly) {
        World world = this.getWorld();
        BlockPos center = this.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(center.add(-3, -3, -3), center.add(3, 3, 3))) {
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (liquidOnly) {
                if (state.isOf(Blocks.WATER) || state.isOf(Blocks.LAVA)) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), 11);
                }
            } else {
                float hardness = state.getHardness(world, pos);
                if (hardness >= 0.0f && hardness <= 2.5f && this.random.nextInt(3) == 0) {
                    world.breakBlock(pos, false, this, 16);
                }
            }
        }
    }

    private void coolLavaAround(BlockPos center, int radius) {
        World world = this.getWorld();
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))) {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(Blocks.LAVA)) {
                world.setBlockState(pos, Blocks.REDSTONE_BLOCK.getDefaultState(), 11);
            }
        }
    }

    private boolean isNearLiquid(BlockPos center, int radius) {
        World world = this.getWorld();
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))) {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(Blocks.WATER) || state.isOf(Blocks.LAVA)) {
                return true;
            }
        }
        return false;
    }

    private void sandifyTerrain(BlockPos center, int radius) {
        World world = this.getWorld();
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))) {
            BlockState state = world.getBlockState(pos);
            float hardness = state.getHardness(world, pos);
            if (!state.isAir() && hardness >= 0.0f && hardness <= 8.0f && this.random.nextInt(3) == 0) {
                world.setBlockState(pos, Blocks.SAND.getDefaultState(), 11);
            }
        }
    }

    private void transmuteTerrain(BlockPos center, int radius) {
        BlockState[] palette = {
            Blocks.COPPER_ORE.getDefaultState(),
            Blocks.GLOWSTONE.getDefaultState(),
            Blocks.SLIME_BLOCK.getDefaultState(),
            Blocks.REDSTONE_BLOCK.getDefaultState(),
            Blocks.ICE.getDefaultState(),
            Blocks.SAND.getDefaultState()
        };
        World world = this.getWorld();
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))) {
            BlockState state = world.getBlockState(pos);
            float hardness = state.getHardness(world, pos);
            if (!state.isAir() && hardness >= 0.0f && hardness <= 8.0f && this.random.nextInt(4) == 0) {
                world.setBlockState(pos, palette[this.random.nextInt(palette.length)], 11);
            }
        }
    }

    private void placeMaterialPatch(BlockPos center, BlockState material, int radius) {
        World world = this.getWorld();
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, 0, -radius), center.add(radius, 0, radius))) {
            if (this.random.nextInt(3) != 0 || !world.getBlockState(pos).isAir()) {
                continue;
            }
            if (material.canPlaceAt(world, pos)) {
                world.setBlockState(pos, material, 11);
            }
        }
    }

    private void crumbleTerrain() {
        World world = this.getWorld();
        BlockPos center = this.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(center.add(-2, -2, -2), center.add(2, 1, 2))) {
            if (this.random.nextInt(5) != 0) {
                continue;
            }
            BlockState state = world.getBlockState(pos);
            if (!state.isAir() && state.getHardness(world, pos) >= 0.0f && state.getHardness(world, pos) <= 8.0f) {
                FallingBlockEntity.spawnFromBlock(world, pos, state);
            }
        }
    }

    private void digAhead(int depth) {
        World world = this.getWorld();
        Vec3d direction = this.getVelocity().lengthSquared() > 1.0E-6 ? this.getVelocity().normalize() : this.getRotationVec(1.0f);
        for (int i = 0; i < depth; i++) {
            BlockPos pos = BlockPos.ofFloored(this.getPos().add(direction.multiply(i + 0.5)));
            BlockState state = world.getBlockState(pos);
            if (!state.isAir() && state.getHardness(world, pos) >= 0.0f && state.getHardness(world, pos) <= 12.0f) {
                world.breakBlock(pos, shouldDropDugBlocks(), this, 16);
            }
        }
    }

    private void strikeLightning(Vec3d position, boolean cosmetic) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(serverWorld);
        if (lightning == null) {
            return;
        }
        lightning.refreshPositionAfterTeleport(position);
        lightning.setCosmetic(cosmetic);
        serverWorld.spawnEntity(lightning);
    }

    private void explodeNoitaProjectile(float radius, boolean fire) {
        this.getWorld().createExplosion(this, this.getX(), this.getY(), this.getZ(), radius, fire, World.ExplosionSourceType.TNT);
        if (this.triggerMode == NoitaSpellTriggerMode.HIT || this.triggerMode == NoitaSpellTriggerMode.DEATH) {
            releaseTriggerPayload();
        }
        this.getWorld().sendEntityStatus(this, HIT_FLASH_STATUS);
        this.discard();
    }

    private void detonateNearbyExplosives() {
        Box area = this.getBoundingBox().expand(8.0);
        for (Entity entity : this.getWorld().getOtherEntities(this, area)) {
            String entityPath = Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
            if (entity instanceof BombEntity || entityPath.contains("tnt")) {
                entity.damage(this.getDamageSources().indirectMagic(this, this.getOwner() == null ? this : this.getOwner()), 1000.0f);
            }
        }
        BlockPos center = this.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(center.add(-4, -4, -4), center.add(4, 4, 4))) {
            if (this.getWorld().getBlockState(pos).isOf(Blocks.TNT)) {
                this.getWorld().breakBlock(pos, false, this, 16);
                this.getWorld().createExplosion(this, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4.0f, true, World.ExplosionSourceType.TNT);
            }
        }
        this.getWorld().createExplosion(this, this.getX(), this.getY(), this.getZ(), 1.2f, false, World.ExplosionSourceType.TNT);
    }

    private void rotateVelocity(double radians) {
        Vec3d velocity = this.getVelocity();
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        this.setVelocity(velocity.x * cos - velocity.z * sin, velocity.y, velocity.x * sin + velocity.z * cos);
    }

    private boolean shouldDropDugBlocks() {
        String path = normalizedItemPath();
        return path.equals("digger") || path.equals("powerdigger");
    }

    private boolean isFireSpell(String path) {
        return path.equals("fireball")
            || path.equals("meteor")
            || path.equals("flamethrower")
            || path.equals("darkflame")
            || path.equals("firebomb")
            || path.equals("firework");
    }

    private boolean isNoitaStaticAreaProjectile() {
        String path = normalizedItemPath();
        return path.equals("bomb_detonator")
            || path.equals("delayed_spell")
            || isStaticBlast(path)
            || isStaticBarrier(path)
            || isStaticCircleField(path)
            || isStaticProjectileField(path)
            || isStaticVacuum(path)
            || isStaticCloud(path)
            || isStaticSwarm(path)
            || path.equals("purple_explosion_field")
            || path.equals("destruction")
            || path.equals("mass_polymorph")
            || path.equals("meteor_rain")
            || path.equals("worm_rain");
    }

    private boolean isStaticBlast(String path) {
        return path.equals("explosion")
            || path.equals("explosion_light")
            || path.equals("fireblast")
            || path.equals("poison_blast")
            || path.equals("alcohol_blast")
            || path.equals("thunder_blast");
    }

    private boolean isStaticBarrier(String path) {
        return path.equals("wall_horizontal")
            || path.equals("wall_vertical")
            || path.equals("wall_square");
    }

    private boolean isStaticCircleField(String path) {
        return path.equals("berserk_field")
            || path.equals("polymorph_field")
            || path.equals("chaos_polymorph_field")
            || path.equals("electrocution_field")
            || path.equals("freeze_field")
            || path.equals("regeneration_field")
            || path.equals("teleportation_field")
            || path.equals("levitation_field")
            || path.equals("shield_field");
    }

    private boolean isStaticProjectileField(String path) {
        return path.equals("projectile_transmutation_field")
            || path.equals("projectile_thunder_field")
            || path.equals("projectile_gravity_field");
    }

    private boolean isStaticVacuum(String path) {
        return path.equals("vacuum_powder")
            || path.equals("vacuum_liquid")
            || path.equals("vacuum_entities");
    }

    private boolean isStaticCloud(String path) {
        return path.equals("cloud_water")
            || path.equals("cloud_oil")
            || path.equals("cloud_blood")
            || path.equals("cloud_acid")
            || path.equals("cloud_thunder");
    }

    private boolean isStaticSwarm(String path) {
        return path.equals("swarm_fly")
            || path.equals("swarm_firebug")
            || path.equals("swarm_wasp")
            || path.equals("friend_fly");
    }

    private boolean isShield() {
        String path = normalizedItemPath();
        return path.equals("magic_shield") || path.equals("big_magic_shield");
    }

    private boolean isDeathCross() {
        String path = normalizedItemPath();
        return path.equals("death_cross") || path.equals("death_cross_big");
    }

    private boolean isStaticTeleport() {
        return normalizedItemPath().equals("teleport_projectile_static");
    }

    private boolean isHomebringerTeleport() {
        return normalizedItemPath().equals("teleport_projectile_closer");
    }

    private boolean hasModifierEffect(NoitaModifierEffect modifierEffect) {
        return this.modifierEffects.contains(modifierEffect);
    }

    private boolean ignoresWorldCollision() {
        return this.behavior == NoitaProjectileBehavior.BLACK_HOLE
            || this.behavior == NoitaProjectileBehavior.WHITE_HOLE
            || this.behavior == NoitaProjectileBehavior.MIST
            || hasModifierEffect(NoitaModifierEffect.CLIPPING_SHOT)
            || hasModifierEffect(NoitaModifierEffect.PHASING_ARC)
            || isNoitaBeamField()
            || isNoitaPersistentField()
            || isNoitaStaticAreaProjectile()
            || isShield();
    }

    private boolean ignoresEntityCollision() {
        return this.behavior == NoitaProjectileBehavior.BLACK_HOLE
            || this.behavior == NoitaProjectileBehavior.WHITE_HOLE
            || this.behavior == NoitaProjectileBehavior.MIST
            || isNoitaEntityPhasingProjectile()
            || isNoitaStaticAreaProjectile()
            || isShield();
    }

    private boolean isNoitaBeamField() {
        String path = normalizedItemPath();
        return path.equals("megalaser")
            || path.equals("megalaser_beam")
            || path.equals("laser_emitter")
            || path.equals("laser_emitter_four")
            || path.equals("laser_emitter_cutter");
    }

    private boolean isNoitaPersistentField() {
        String path = normalizedItemPath();
        return path.equals("death_cross")
            || path.equals("death_cross_big")
            || path.equals("expanding_orb")
            || path.equals("glowing_bolt")
            || path.equals("tentacle")
            || path.equals("tentacle_timer")
            || path.equals("spiral_shot")
            || path.equals("teleport_projectile_static");
    }

    private boolean isNoitaEntityPhasingProjectile() {
        String path = normalizedItemPath();
        return path.equals("spiral_shot")
            || path.equals("darkflame")
            || path.equals("expanding_orb")
            || path.equals("megalaser")
            || path.equals("megalaser_beam")
            || path.equals("worm_shot");
    }

    private String normalizedItemPath() {
        return this.itemPath.toLowerCase(Locale.ROOT);
    }

    private static boolean itemPathPiercesEntities(String path) {
        if (path == null) {
            return false;
        }
        String normalizedPath = path.toLowerCase(Locale.ROOT);
        return normalizedPath.equals("lance_holy")
            || normalizedPath.equals("chain_bolt")
            || normalizedPath.equals("freezing_gaze")
            || normalizedPath.equals("disc_bullet_big")
            || normalizedPath.equals("disc_bullet_bigger")
            || normalizedPath.equals("worm_shot")
            || normalizedPath.equals("tentacle")
            || normalizedPath.equals("tentacle_timer")
            || normalizedPath.equals("magic_shield")
            || normalizedPath.equals("big_magic_shield");
    }

    private int getMaxBounces() {
        String path = normalizedItemPath();
        int pathBounces = MAX_BOUNCES;
        if (path.equals("disc_bullet_bigger")) {
            pathBounces = 10;
        } else if (path.equals("exploding_ducks")) {
            pathBounces = 5;
        } else if (path.equals("exploding_deer")) {
            pathBounces = 4;
        } else if (path.equals("cursed_orb")) {
            pathBounces = 2;
        }
        return Math.max(pathBounces, this.bounceCount);
    }

    private boolean canBounceProjectile() {
        if (hasModifierEffect(NoitaModifierEffect.REMOVE_BOUNCE)) {
            return false;
        }
        String path = normalizedItemPath();
        return this.bounceCount > 0
            || this.behavior.bounces()
            || path.equals("cursed_orb")
            || path.equals("exploding_ducks")
            || path.equals("exploding_deer");
    }

    private void spawnTrailParticles() {
        World world = this.getWorld();
        int count = this.behavior.isBeamLike() || this.behavior == NoitaProjectileBehavior.MIST ? 2 : 1;
        for (int i = 0; i < count; i++) {
            world.addParticle(
                ModParticles.SPARK_TRAIL,
                this.getX(),
                this.getY(),
                this.getZ(),
                (this.random.nextDouble() - 0.5) * 0.02,
                (this.random.nextDouble() - 0.5) * 0.02,
                (this.random.nextDouble() - 0.5) * 0.02
            );
        }
    }

    private void spawnHitFlashParticles() {
        World world = this.getWorld();
        int count = this.behavior.explodesOnCollision() ? 30 : 18;
        for (int i = 0; i < count; i++) {
            world.addParticle(
                ModParticles.SPARK_TRAIL,
                this.getX(),
                this.getY(),
                this.getZ(),
                (this.random.nextDouble() - 0.5) * 0.22,
                (this.random.nextDouble() - 0.5) * 0.22,
                (this.random.nextDouble() - 0.5) * 0.22
            );
        }
    }

    private void setItemFromPath(String path) {
        this.setItem(new ItemStack(getVisualItem(path)));
    }

    private static Item getVisualItem(String path) {
        if ("megalaser_beam".equals(path)) {
            path = "megalaser";
        }
        Item item = Registries.ITEM.get(MCNoita.id(path));
        return item == net.minecraft.item.Items.AIR ? ModItems.LIGHT_BULLET : item;
    }

    private static Vec3d applyBurstSpread(Vec3d direction, int index, int count, float burstSpreadDegrees) {
        if (count <= 1 || burstSpreadDegrees <= 0.0f) {
            return direction;
        }
        double offset = Math.toRadians(((index - (count - 1) / 2.0) / Math.max(1.0, count - 1.0)) * burstSpreadDegrees);
        double cos = Math.cos(offset);
        double sin = Math.sin(offset);
        return new Vec3d(direction.x * cos - direction.z * sin, direction.y, direction.x * sin + direction.z * cos).normalize();
    }
}

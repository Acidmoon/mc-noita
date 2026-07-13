package com.mcnoita.entity;

import com.mcnoita.spell.NoitaProjectileBehavior;
import com.mcnoita.spell.NoitaProjectilePayload;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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

    private String itemPath = "bomb";
    private NoitaProjectileBehavior behavior = NoitaProjectileBehavior.FUSED_EXPLOSIVE;
    private float explosionRadius = 4.0f;
    private float gravity = 0.05f;
    private float drag = 0.99f;
    private float renderScale = 1.0f;
    private int fuseTicks = 1;
    private LivingEntity caster;
    private boolean exploded;
    private boolean triggerPayloadReleased;
    private List<NoitaProjectilePayload> triggerPayloads = List.of();

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
        this.itemPath = itemPath == null || itemPath.isBlank() ? "bomb" : itemPath;
        this.behavior = behavior == null ? NoitaProjectileBehavior.FUSED_EXPLOSIVE : behavior;
        this.explosionRadius = Math.max(0.0f, explosionRadius);
        this.gravity = gravity;
        this.drag = Math.max(0.0f, drag);
        this.renderScale = renderScale <= 0.0f ? 1.0f : renderScale;
        this.triggerPayloads = List.copyOf(triggerPayloads);
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
            if (this.behavior == NoitaProjectileBehavior.MINE && this.age % 20 == 0) {
                primeNearbyMortal();
            }
            if (isPersistentExplosiveProp() && this.age < this.fuseTicks) {
                return;
            }
            float hp = this.getHealth() - 1.0f;
            if (hp <= 0.0f) {
                explode();
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
            explode();
        } else {
            this.setHealth(hp);
        }
        return true;
    }

    private void explode() {
        if (this.exploded || this.isRemoved()) {
            return;
        }
        this.exploded = true;
        World world = this.getWorld();
        this.discard();
        if (!world.isClient) {
            releaseTriggerPayload();
            if (this.explosionRadius > 0.0f) {
                world.createExplosion(
                    this,
                    this.getX(), this.getY(), this.getZ(),
                    this.explosionRadius,
                    true,
                    World.ExplosionSourceType.TNT
                );
            }
            applyNoitaExplosionAftermath(world);
        }
    }

    @Override
    public void travel(Vec3d movementInput) {
        if (this.isOnGround()) {
            if (this.behavior == NoitaProjectileBehavior.MINE) {
                this.setVelocity(Vec3d.ZERO);
                return;
            }
            Vec3d groundVelocity = this.getVelocity();
            this.setVelocity(groundVelocity.x * 0.35, 0.0, groundVelocity.z * 0.35);
            return;
        }
        Vec3d velocity = this.getVelocity();
        this.setVelocity(velocity.x * this.drag, velocity.y - this.gravity, velocity.z * this.drag);
        this.move(MovementType.SELF, this.getVelocity());
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
        return this.caster;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString(ITEM_PATH_KEY, this.itemPath);
        nbt.putString(BEHAVIOR_KEY, this.behavior.name());
        nbt.putFloat(EXPLOSION_RADIUS_KEY, this.explosionRadius);
        nbt.putFloat(GRAVITY_KEY, this.gravity);
        nbt.putFloat(DRAG_KEY, this.drag);
        nbt.putFloat(RENDER_SCALE_KEY, this.renderScale);
        nbt.putBoolean(TRIGGER_PAYLOAD_RELEASED_KEY, this.triggerPayloadReleased);
        nbt.put(TRIGGER_PAYLOADS_KEY, NoitaProjectilePayload.toNbtList(this.triggerPayloads));
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains(ITEM_PATH_KEY, NbtElement.STRING_TYPE)) {
            this.itemPath = nbt.getString(ITEM_PATH_KEY);
        }
        if (nbt.contains(BEHAVIOR_KEY, NbtElement.STRING_TYPE)) {
            try {
                this.behavior = NoitaProjectileBehavior.valueOf(nbt.getString(BEHAVIOR_KEY));
            } catch (IllegalArgumentException ignored) {
                this.behavior = NoitaProjectileBehavior.FUSED_EXPLOSIVE;
            }
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
        this.fuseTicks = getInitialFuseTicks(this.itemPath, Math.round(this.getMaxHealth()));
        if (nbt.contains(TRIGGER_PAYLOAD_RELEASED_KEY, NbtElement.BYTE_TYPE)) {
            this.triggerPayloadReleased = nbt.getBoolean(TRIGGER_PAYLOAD_RELEASED_KEY);
        }
        this.triggerPayloads = NoitaProjectilePayload.fromNbtList(nbt.getList(TRIGGER_PAYLOADS_KEY, NbtElement.COMPOUND_TYPE));
    }

    private void releaseTriggerPayload() {
        if (this.triggerPayloadReleased || this.triggerPayloads.isEmpty()) {
            return;
        }
        this.triggerPayloadReleased = true;
        Vec3d direction = getPayloadDirection();
        for (NoitaProjectilePayload payload : this.triggerPayloads) {
            SparkBoltProjectileEntity.spawnPayloadProjectile(this.getWorld(), this.caster, this.getPos(), direction, payload);
        }
    }

    private Vec3d getPayloadDirection() {
        Vec3d velocity = this.getVelocity();
        if (velocity.lengthSquared() > 1.0E-6) {
            return velocity.normalize();
        }
        return this.caster == null ? this.getRotationVec(1.0f) : this.caster.getRotationVec(1.0f);
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
        for (net.minecraft.entity.Entity entity : world.getOtherEntities(this, area, entity -> entity instanceof LivingEntity)) {
            entity.setOnFireFor(seconds);
        }
        BlockPos center = this.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -1, -radius), center.add(radius, 1, radius))) {
            if (this.random.nextInt(4) != 0 || !world.getBlockState(pos).isAir()) {
                continue;
            }
            BlockState fire = AbstractFireBlock.getState(world, pos);
            if (fire.canPlaceAt(world, pos)) {
                world.setBlockState(pos, fire, 11);
            }
        }
    }

    private void freezeNearby(int radius) {
        Box area = this.getBoundingBox().expand(radius);
        for (net.minecraft.entity.Entity entity : this.getWorld().getOtherEntities(this, area, entity -> entity instanceof LivingEntity)) {
            entity.setFrozenTicks(Math.max(entity.getFrozenTicks(), 180));
        }
    }

    private void holyFlash(double radius) {
        Box area = this.getBoundingBox().expand(radius);
        for (net.minecraft.entity.Entity entity : this.getWorld().getOtherEntities(this, area, entity -> entity instanceof LivingEntity)) {
            if (entity instanceof LivingEntity living) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 80, 0), this);
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1), this);
            }
        }
    }

    private void poisonNearby(double radius) {
        Box area = this.getBoundingBox().expand(radius);
        for (net.minecraft.entity.Entity entity : this.getWorld().getOtherEntities(this, area, entity -> entity instanceof LivingEntity)) {
            if (entity instanceof LivingEntity living) {
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
            BlockState state = world.getBlockState(pos);
            float hardness = state.getHardness(world, pos);
            if (!state.isAir() && hardness >= 0.0f && hardness <= 20.0f && this.random.nextInt(3) != 0) {
                world.breakBlock(pos, false, this, 16);
            }
        }
    }

    private void placeGlowstoneRing(World world, int radius) {
        BlockPos center = this.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -1, -radius), center.add(radius, 1, radius))) {
            if (this.random.nextInt(5) != 0 || !world.getBlockState(pos).isAir()) {
                continue;
            }
            if (world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos.down(), Direction.UP)) {
                world.setBlockState(pos, Blocks.GLOWSTONE.getDefaultState(), 11);
            }
        }
    }

    private void scatterTntMaterial(World world, int radius) {
        BlockPos center = this.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, 0, -radius), center.add(radius, 1, radius))) {
            if (this.random.nextInt(6) != 0 || !world.getBlockState(pos).isAir()) {
                continue;
            }
            if (world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos.down(), Direction.UP)) {
                world.setBlockState(pos, Blocks.TNT.getDefaultState(), 11);
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
        for (net.minecraft.entity.Entity entity : this.getWorld().getOtherEntities(this, area)) {
            if (entity instanceof LivingEntity && entity != this.caster) {
                explode();
                return;
            }
        }
    }
}

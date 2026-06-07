package com.mcnoita.entity;

import com.mcnoita.item.ModItems;
import com.mcnoita.particle.ModParticles;
import com.mcnoita.spell.NoitaProjectilePayload;
import com.mcnoita.spell.NoitaSpellDamage;
import com.mcnoita.spell.NoitaSpellTriggerMode;
import com.mcnoita.world.NoitaTemporaryLightManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.List;

public class SparkBoltProjectileEntity extends ThrownItemEntity {
    private static final String DAMAGE_KEY = "Damage";
    private static final String CRITICAL_CHANCE_PERCENT_KEY = "CriticalChancePercent";
    private static final String MAX_AGE_KEY = "MaxAge";
    private static final String TRAIL_LIGHT_STACKS_KEY = "TrailLightStacks";
    private static final String TRIGGER_MODE_KEY = "TriggerMode";
    private static final String TRIGGER_DELAY_TICKS_KEY = "TriggerDelayTicks";
    private static final String TRIGGER_PAYLOAD_RELEASED_KEY = "TriggerPayloadReleased";
    private static final String TRIGGER_PAYLOADS_KEY = "TriggerPayloads";
    private static final byte HIT_FLASH_STATUS = 3;
    private static final float CRITICAL_DAMAGE_MULTIPLIER = 5.0f;

    private float damage = 3.0f;
    private float criticalChancePercent;
    private int maxAge = 60;
    private int trailLightStacks;
    private NoitaSpellTriggerMode triggerMode = NoitaSpellTriggerMode.NONE;
    private int triggerDelayTicks;
    private boolean triggerPayloadReleased;
    private List<NoitaProjectilePayload> triggerPayloads = List.of();

    public SparkBoltProjectileEntity(EntityType<? extends SparkBoltProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    public SparkBoltProjectileEntity(World world, LivingEntity owner, float damage, float criticalChancePercent, int maxAge, int trailLightStacks) {
        this(world, owner, damage, criticalChancePercent, maxAge, trailLightStacks, NoitaSpellTriggerMode.NONE, 0, List.of());
    }

    public SparkBoltProjectileEntity(
        World world,
        LivingEntity owner,
        float damage,
        float criticalChancePercent,
        int maxAge,
        int trailLightStacks,
        NoitaSpellTriggerMode triggerMode,
        int triggerDelayTicks,
        List<NoitaProjectilePayload> triggerPayloads
    ) {
        super(ModEntities.SPARK_BOLT_PROJECTILE, owner, world);
        this.damage = Math.max(0.0f, damage);
        this.criticalChancePercent = Math.max(0.0f, criticalChancePercent);
        this.maxAge = Math.max(1, maxAge);
        this.trailLightStacks = Math.max(0, trailLightStacks);
        this.triggerMode = triggerMode;
        this.triggerDelayTicks = Math.max(0, triggerDelayTicks);
        this.triggerPayloads = List.copyOf(triggerPayloads);
        this.setNoGravity(true);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.age > this.maxAge) {
            if (!this.getWorld().isClient && this.triggerMode == NoitaSpellTriggerMode.DEATH) {
                releaseTriggerPayload();
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
            if (this.trailLightStacks > 0) {
                NoitaTemporaryLightManager.illuminateTrail(serverWorld, new Vec3d(this.prevX, this.prevY, this.prevZ), this.getPos(), this.trailLightStacks);
            }
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
        Entity owner = this.getOwner();
        NoitaSpellDamage.apply(
            entityHitResult.getEntity(),
            this.getDamageSources().indirectMagic(this, owner == null ? this : owner),
            getRolledDamage()
        );
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!this.getWorld().isClient && hitResult.getType() != HitResult.Type.MISS) {
            if (this.triggerMode == NoitaSpellTriggerMode.HIT || this.triggerMode == NoitaSpellTriggerMode.DEATH) {
                releaseTriggerPayload();
            }
            this.getWorld().sendEntityStatus(this, HIT_FLASH_STATUS);
            this.discard();
        }
    }

    @Override
    protected float getGravity() {
        return 0.0f;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.SPARK_BOLT;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putFloat(DAMAGE_KEY, this.damage);
        nbt.putFloat(CRITICAL_CHANCE_PERCENT_KEY, this.criticalChancePercent);
        nbt.putInt(MAX_AGE_KEY, this.maxAge);
        nbt.putInt(TRAIL_LIGHT_STACKS_KEY, this.trailLightStacks);
        nbt.putString(TRIGGER_MODE_KEY, this.triggerMode.name());
        nbt.putInt(TRIGGER_DELAY_TICKS_KEY, this.triggerDelayTicks);
        nbt.putBoolean(TRIGGER_PAYLOAD_RELEASED_KEY, this.triggerPayloadReleased);
        nbt.put(TRIGGER_PAYLOADS_KEY, NoitaProjectilePayload.toNbtList(this.triggerPayloads));
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
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

    private float getRolledDamage() {
        if (this.criticalChancePercent <= 0.0f || this.random.nextFloat() * 100.0f >= this.criticalChancePercent) {
            return this.damage;
        }

        return this.damage * CRITICAL_DAMAGE_MULTIPLIER;
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
            spawnPayloadProjectile(world, livingOwner, direction, payload);
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

    private void spawnPayloadProjectile(World world, LivingEntity owner, Vec3d direction, NoitaProjectilePayload payload) {
        if (payload.kind() == NoitaProjectilePayload.ProjectileKind.BOMB) {
            BombEntity bomb = new BombEntity(world, owner, payload.explosionRadius(), payload.lifetimeTicks(), payload.payloads());
            bomb.setPosition(this.getX(), this.getY(), this.getZ());
            bomb.setVelocity(direction.multiply(payload.speed()));
            world.spawnEntity(bomb);
            return;
        }

        SparkBoltProjectileEntity sparkBolt = new SparkBoltProjectileEntity(
            world,
            owner,
            payload.damage(),
            payload.criticalChancePercent(),
            payload.lifetimeTicks(),
            payload.trailLightStacks(),
            payload.triggerMode(),
            payload.triggerDelayTicks(),
            payload.payloads()
        );
        sparkBolt.setPosition(this.getX(), this.getY(), this.getZ());
        sparkBolt.setVelocity(direction.x, direction.y, direction.z, payload.speed(), payload.divergence());
        world.spawnEntity(sparkBolt);
    }

    private void spawnTrailParticles() {
        World world = this.getWorld();
        world.addParticle(
            ModParticles.SPARK_TRAIL,
            this.getX(),
            this.getY(),
            this.getZ(),
            (this.random.nextDouble() - 0.5) * 0.015,
            (this.random.nextDouble() - 0.5) * 0.015,
            (this.random.nextDouble() - 0.5) * 0.015
        );
        if (this.age % 2 == 0) {
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
        for (int i = 0; i < 18; i++) {
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
}

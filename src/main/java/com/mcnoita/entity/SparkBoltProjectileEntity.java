package com.mcnoita.entity;

import com.mcnoita.item.ModItems;
import com.mcnoita.spell.NoitaSpellDamage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class SparkBoltProjectileEntity extends ThrownItemEntity {
    private static final String DAMAGE_KEY = "Damage";
    private static final String CRITICAL_CHANCE_PERCENT_KEY = "CriticalChancePercent";
    private static final String MAX_AGE_KEY = "MaxAge";
    private static final byte HIT_FLASH_STATUS = 3;
    private static final float CRITICAL_DAMAGE_MULTIPLIER = 5.0f;

    private float damage = 3.0f;
    private float criticalChancePercent;
    private int maxAge = 60;

    public SparkBoltProjectileEntity(EntityType<? extends SparkBoltProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    public SparkBoltProjectileEntity(World world, LivingEntity owner, float damage, float criticalChancePercent, int maxAge) {
        super(ModEntities.SPARK_BOLT_PROJECTILE, owner, world);
        this.damage = Math.max(0.0f, damage);
        this.criticalChancePercent = Math.max(0.0f, criticalChancePercent);
        this.maxAge = Math.max(1, maxAge);
        this.setNoGravity(true);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.age > this.maxAge) {
            this.discard();
            return;
        }

        if (this.getWorld().isClient) {
            spawnTrailParticles();
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
    }

    private float getRolledDamage() {
        if (this.criticalChancePercent <= 0.0f || this.random.nextFloat() * 100.0f >= this.criticalChancePercent) {
            return this.damage;
        }

        return this.damage * CRITICAL_DAMAGE_MULTIPLIER;
    }

    private void spawnTrailParticles() {
        World world = this.getWorld();
        world.addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        if (this.age % 2 == 0) {
            world.addParticle(
                ParticleTypes.ELECTRIC_SPARK,
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
        world.addParticle(ParticleTypes.FLASH, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        for (int i = 0; i < 12; i++) {
            world.addParticle(
                i % 3 == 0 ? ParticleTypes.FIREWORK : ParticleTypes.END_ROD,
                this.getX(),
                this.getY(),
                this.getZ(),
                (this.random.nextDouble() - 0.5) * 0.18,
                (this.random.nextDouble() - 0.5) * 0.18,
                (this.random.nextDouble() - 0.5) * 0.18
            );
        }
    }
}

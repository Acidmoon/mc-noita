package com.mcnoita.entity;

import com.mcnoita.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Collections;

public class BombEntity extends LivingEntity {
    private static final String EXPLOSION_RADIUS_KEY = "ExplosionRadius";

    private float explosionRadius = 4.0f;
    private LivingEntity caster;
    private boolean exploded;

    public BombEntity(EntityType<? extends BombEntity> entityType, World world) {
        super(entityType, world);
    }

    public BombEntity(World world, LivingEntity caster, float explosionRadius, int lifetimeTicks) {
        super(ModEntities.BOMB_PROJECTILE, world);
        this.caster = caster;
        this.explosionRadius = Math.max(0.0f, explosionRadius);
        float maxHp = lifetimeTicks;
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(maxHp);
        this.setHealth(maxHp);
    }

    public static DefaultAttributeContainer.Builder createBombAttributes() {
        return LivingEntity.createLivingAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && !this.exploded && !this.isRemoved() && this.isAlive()) {
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
            world.createExplosion(
                this,
                this.getX(), this.getY(), this.getZ(),
                this.explosionRadius,
                true,
                World.ExplosionSourceType.TNT
            );
        }
    }

    @Override
    public void travel(Vec3d movementInput) {
        if (this.isOnGround()) {
            this.setVelocity(Vec3d.ZERO);
            return;
        }
        Vec3d velocity = this.getVelocity();
        this.setVelocity(velocity.x * 0.99, velocity.y - 0.05, velocity.z * 0.99);
        this.move(net.minecraft.entity.MovementType.SELF, this.getVelocity());
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
        nbt.putFloat(EXPLOSION_RADIUS_KEY, this.explosionRadius);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains(EXPLOSION_RADIUS_KEY, NbtElement.NUMBER_TYPE)) {
            this.explosionRadius = Math.max(0.0f, nbt.getFloat(EXPLOSION_RADIUS_KEY));
        }
    }
}

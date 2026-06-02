package com.mcnoita.entity;

import com.mcnoita.MCNoita;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
    public static final EntityType<SparkBoltProjectileEntity> SPARK_BOLT_PROJECTILE = Registry.register(
        Registries.ENTITY_TYPE,
        MCNoita.id("spark_bolt_projectile"),
        FabricEntityTypeBuilder.<SparkBoltProjectileEntity>create(SpawnGroup.MISC, SparkBoltProjectileEntity::new)
            .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
            .trackRangeBlocks(64)
            .trackedUpdateRate(1)
            .forceTrackedVelocityUpdates(true)
            .disableSaving()
            .build()
    );

    public static final EntityType<BombEntity> BOMB_PROJECTILE = Registry.register(
        Registries.ENTITY_TYPE,
        MCNoita.id("bomb_projectile"),
        FabricEntityTypeBuilder.<BombEntity>createLiving()
            .spawnGroup(SpawnGroup.MISC)
            .entityFactory(BombEntity::new)
            .dimensions(EntityDimensions.fixed(0.4f, 0.4f))
            .trackRangeBlocks(64)
            .trackedUpdateRate(3)
            .forceTrackedVelocityUpdates(true)
            .build()
    );

    private ModEntities() {
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(BOMB_PROJECTILE, BombEntity.createBombAttributes());
        MCNoita.LOGGER.info("Registering MC Noita entities");
    }
}

package com.mcnoita.particle;

import com.mcnoita.MCNoita;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModParticles {
    public static final DefaultParticleType SPARK_TRAIL = Registry.register(
        Registries.PARTICLE_TYPE,
        MCNoita.id("spark_trail"),
        FabricParticleTypes.simple()
    );

    private ModParticles() {
    }

    public static void register() {
        MCNoita.LOGGER.info("Registering MC Noita particles");
    }
}

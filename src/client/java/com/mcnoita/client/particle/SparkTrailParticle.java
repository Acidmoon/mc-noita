package com.mcnoita.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

public class SparkTrailParticle extends SpriteBillboardParticle {
    private static final int LIFETIME_TICKS = 10;

    private SparkTrailParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.maxAge = LIFETIME_TICKS;
        this.collidesWithWorld = false;
        this.velocityMultiplier = 0.86f;
        this.scale = 0.08f + this.random.nextFloat() * 0.05f;
        float flicker = this.random.nextFloat();
        this.setColor(0.62f + flicker * 0.28f, 0.14f + flicker * 0.12f, 1.0f);
        this.setAlpha(0.9f);
        this.setSprite(spriteProvider);
    }

    @Override
    public void tick() {
        super.tick();
        float remaining = 1.0f - this.age / (float) this.maxAge;
        this.setAlpha(Math.max(0.0f, remaining) * 0.9f);
        this.scale *= 0.94f;
    }

    @Override
    public int getBrightness(float tint) {
        return 0xF000F0;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return new SparkTrailParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}

package com.mcnoita.client;

import com.mcnoita.client.hud.NoitaHoverHud;
import com.mcnoita.client.hud.NoitaWandCastHud;
import com.mcnoita.client.item.NoitaTooltipEvents;
import com.mcnoita.client.network.ClientHoverInputEvents;
import com.mcnoita.client.network.ClientHoverNetworking;
import com.mcnoita.client.network.ClientWandCastHudNetworking;
import com.mcnoita.client.network.ClientWandCastEvents;
import com.mcnoita.client.particle.SparkTrailParticle;
import com.mcnoita.client.render.BombEntityRenderer;
import com.mcnoita.client.render.NoitaProjectileEntityRenderer;
import com.mcnoita.client.screen.NoitaWandScreen;
import com.mcnoita.entity.ModEntities;
import com.mcnoita.particle.ModParticles;
import com.mcnoita.screen.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class MCNoitaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.WAND_EDITOR, NoitaWandScreen::new);
        EntityRendererRegistry.register(
            ModEntities.SPARK_BOLT_PROJECTILE,
            NoitaProjectileEntityRenderer::new
        );
        EntityRendererRegistry.register(
            ModEntities.BOMB_PROJECTILE,
            BombEntityRenderer::new
        );
        ParticleFactoryRegistry.getInstance().register(ModParticles.SPARK_TRAIL, SparkTrailParticle.Factory::new);
        ClientHoverInputEvents.register();
        ClientHoverNetworking.register();
        ClientWandCastHudNetworking.register();
        ClientWandCastEvents.register();
        NoitaTooltipEvents.register();
        NoitaHoverHud.register();
        NoitaWandCastHud.register();
    }
}

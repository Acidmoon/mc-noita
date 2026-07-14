package com.mcnoita;

import com.mcnoita.catalog.SpellCatalogService;
import com.mcnoita.entity.ModEntities;
import com.mcnoita.event.ModWandEvents;
import com.mcnoita.item.ModItemGroups;
import com.mcnoita.item.ModItems;
import com.mcnoita.network.ModNetworking;
import com.mcnoita.particle.ModParticles;
import com.mcnoita.player.NoitaHungerManager;
import com.mcnoita.player.NoitaHoverManager;
import com.mcnoita.screen.ModScreenHandlers;
import com.mcnoita.wand.NoitaWandHudSync;
import com.mcnoita.world.NoitaTemporaryLightManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCNoita implements ModInitializer {
    public static final String MOD_ID = "mc-noita";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModScreenHandlers.register();
        ModEntities.register();
        ModParticles.register();
        ModNetworking.registerServerReceivers();
        NoitaHungerManager.register();
        NoitaHoverManager.register();
        NoitaTemporaryLightManager.register();
        NoitaWandHudSync.register();
        ModWandEvents.register();
        ModItems.register();
        SpellCatalogService.getInstance().initializeFromLegacy();
        ModItemGroups.register();
        LOGGER.info("Initializing MC Noita");
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}

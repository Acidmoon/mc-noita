package com.mcnoita.player;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class NoitaHungerManager {
    private static final int FULL_FOOD_LEVEL = 20;
    private static final float FULL_SATURATION_LEVEL = 20.0f;

    private NoitaHungerManager() {
    }

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(NoitaHungerManager::fillAllPlayers);
        ServerTickEvents.END_SERVER_TICK.register(NoitaHungerManager::fillAllPlayers);
    }

    private static void fillAllPlayers(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            fill(player.getHungerManager());
        }
    }

    private static void fill(HungerManager hungerManager) {
        hungerManager.setFoodLevel(FULL_FOOD_LEVEL);
        hungerManager.setSaturationLevel(FULL_SATURATION_LEVEL);
        hungerManager.setExhaustion(0.0f);
    }
}

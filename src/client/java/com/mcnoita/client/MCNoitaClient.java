package com.mcnoita.client;

import com.mcnoita.client.hud.NoitaHoverHud;
import com.mcnoita.client.hud.NoitaWandCastHud;
import com.mcnoita.client.network.ClientHoverInputEvents;
import com.mcnoita.client.network.ClientHoverNetworking;
import com.mcnoita.client.network.ClientWandCastHudNetworking;
import com.mcnoita.client.network.ClientWandCastEvents;
import com.mcnoita.client.screen.NoitaWandScreen;
import com.mcnoita.screen.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class MCNoitaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.WAND_EDITOR, NoitaWandScreen::new);
        ClientHoverInputEvents.register();
        ClientHoverNetworking.register();
        ClientWandCastHudNetworking.register();
        ClientWandCastEvents.register();
        NoitaHoverHud.register();
        NoitaWandCastHud.register();
    }
}

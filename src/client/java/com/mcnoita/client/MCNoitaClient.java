package com.mcnoita.client;

import com.mcnoita.client.screen.NoitaWandScreen;
import com.mcnoita.screen.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class MCNoitaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.WAND_EDITOR, NoitaWandScreen::new);
    }
}

package com.mcnoita;

import com.mcnoita.item.ModItemGroups;
import com.mcnoita.item.ModItems;
import com.mcnoita.screen.ModScreenHandlers;
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
        ModItems.register();
        ModItemGroups.register();
        LOGGER.info("Initializing MC Noita");
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}

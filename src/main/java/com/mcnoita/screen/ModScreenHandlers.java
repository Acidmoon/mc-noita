package com.mcnoita.screen;

import com.mcnoita.MCNoita;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;

public final class ModScreenHandlers {
    public static final ScreenHandlerType<NoitaWandScreenHandler> WAND_EDITOR = Registry.register(
        Registries.SCREEN_HANDLER,
        MCNoita.id("wand_editor"),
        new ExtendedScreenHandlerType<>(NoitaWandScreenHandler::new)
    );

    private ModScreenHandlers() {
    }

    public static void register() {
        MCNoita.LOGGER.info("Registering MC Noita screen handlers");
    }
}

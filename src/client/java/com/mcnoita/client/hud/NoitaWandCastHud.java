package com.mcnoita.client.hud;

import com.mcnoita.client.player.ClientWandCastHudState;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class NoitaWandCastHud {
    private static final int BAR_WIDTH = 81;
    private static final int BAR_HEIGHT = 5;

    private NoitaWandCastHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(NoitaWandCastHud::render);
    }

    private static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || client.player.isSpectator()) {
            return;
        }

        int x = client.getWindow().getScaledWidth() / 2 + 10;
        int y = client.getWindow().getScaledHeight() - 36;
        int filledWidth = Math.round(ClientWandCastHudState.getFraction() * BAR_WIDTH);
        int mode = ClientWandCastHudState.getMode();

        context.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xDD101010);
        context.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF1B1D22);
        if (filledWidth <= 0 || mode == ClientWandCastHudState.MODE_EMPTY) {
            return;
        }

        int color = mode == ClientWandCastHudState.MODE_RECHARGE ? 0xFFFFC247 : 0xFF2F8CFF;
        int highlight = mode == ClientWandCastHudState.MODE_RECHARGE ? 0xFFFFE18A : 0xFF84C6FF;
        context.fill(x, y, x + filledWidth, y + BAR_HEIGHT, color);
        context.fill(x, y, x + filledWidth, y + 1, highlight);
    }
}

package com.mcnoita.client.hud;

import com.mcnoita.client.player.ClientHoverEnergy;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class NoitaHoverHud {
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;

    private NoitaHoverHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(NoitaHoverHud::render);
    }

    private static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || client.player.isSpectator()) {
            return;
        }

        int x = client.getWindow().getScaledWidth() / 2 - BAR_WIDTH / 2;
        int y = client.getWindow().getScaledHeight() - 29;
        int filledWidth = Math.round(ClientHoverEnergy.getFraction() * BAR_WIDTH);

        context.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xDD101010);
        context.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF3C2F1B);
        if (filledWidth > 0) {
            context.fill(x, y, x + filledWidth, y + BAR_HEIGHT, 0xFFFFC247);
            context.fill(x, y, x + filledWidth, y + 1, 0xFFFFE18A);
        }
    }
}

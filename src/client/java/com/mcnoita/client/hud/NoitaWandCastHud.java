package com.mcnoita.client.hud;

import com.mcnoita.client.player.ClientWandCastHudState;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class NoitaWandCastHud {
    private static final int BAR_WIDTH = 81;
    private static final int BAR_HEIGHT = 5;
    private static final int BAR_GAP = 3;

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
        int manaWidth = Math.round(ClientWandCastHudState.getManaFraction() * BAR_WIDTH);
        int mode = ClientWandCastHudState.getMode();

        if (ClientWandCastHudState.hasMana()) {
            int manaY = y - BAR_HEIGHT - BAR_GAP;
            context.fill(x - 1, manaY - 1, x + BAR_WIDTH + 1, manaY + BAR_HEIGHT + 1, 0xDD101010);
            context.fill(x, manaY, x + BAR_WIDTH, manaY + BAR_HEIGHT, 0xFF102014);
            if (manaWidth > 0) {
                context.fill(x, manaY, x + manaWidth, manaY + BAR_HEIGHT, 0xFF2FBF4A);
                context.fill(x, manaY, x + manaWidth, manaY + 1, 0xFF7CFF8B);
            }
        }

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

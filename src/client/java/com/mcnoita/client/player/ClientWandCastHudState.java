package com.mcnoita.client.player;

public final class ClientWandCastHudState {
    public static final int MODE_EMPTY = 0;
    public static final int MODE_CAST_DELAY = 1;
    public static final int MODE_RECHARGE = 2;

    private static int mode;
    private static int progressTicks;
    private static int totalTicks = 1;
    private static float currentMana;
    private static int manaMax;

    private ClientWandCastHudState() {
    }

    public static void set(int mode, int progressTicks, int totalTicks, float currentMana, int manaMax) {
        ClientWandCastHudState.mode = mode;
        ClientWandCastHudState.progressTicks = Math.max(0, progressTicks);
        ClientWandCastHudState.totalTicks = Math.max(1, totalTicks);
        ClientWandCastHudState.currentMana = Math.max(0.0f, currentMana);
        ClientWandCastHudState.manaMax = Math.max(0, manaMax);
    }

    public static int getMode() {
        return mode;
    }

    public static float getFraction() {
        if (mode == MODE_EMPTY) {
            return 0.0f;
        }

        return Math.max(0.0f, Math.min(1.0f, progressTicks / (float) totalTicks));
    }

    public static boolean hasMana() {
        return manaMax > 0;
    }

    public static float getManaFraction() {
        if (manaMax <= 0) {
            return 0.0f;
        }

        return Math.max(0.0f, Math.min(1.0f, currentMana / manaMax));
    }
}

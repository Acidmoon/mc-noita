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
    private static long wandRevision = -1L;
    private static long catalogEpoch = -1L;
    private static int stateHash;
    private static String catalogHash = "";

    private ClientWandCastHudState() {
    }

    public static void set(int mode, int progressTicks, int totalTicks, float currentMana, int manaMax) {
        ClientWandCastHudState.mode = Math.max(MODE_EMPTY, Math.min(MODE_RECHARGE, mode));
        ClientWandCastHudState.totalTicks = Math.max(1, Math.min(72_000, totalTicks));
        ClientWandCastHudState.progressTicks = Math.max(0, Math.min(ClientWandCastHudState.totalTicks, progressTicks));
        ClientWandCastHudState.manaMax = Math.max(0, Math.min(1_000_000, manaMax));
        ClientWandCastHudState.currentMana = Float.isFinite(currentMana)
            ? Math.max(0.0f, Math.min(ClientWandCastHudState.manaMax, currentMana))
            : 0.0f;
    }

    /** Stores only server-projected binding data for the next C2S cast intent. */
    public static void set(
        int mode,
        int progressTicks,
        int totalTicks,
        float currentMana,
        int manaMax,
        long wandRevision,
        long catalogEpoch,
        int stateHash,
        String catalogHash
    ) {
        set(mode, progressTicks, totalTicks, currentMana, manaMax);
        ClientWandCastHudState.wandRevision = Math.max(0L, wandRevision);
        ClientWandCastHudState.catalogEpoch = Math.max(0L, catalogEpoch);
        ClientWandCastHudState.stateHash = stateHash;
        ClientWandCastHudState.catalogHash = catalogHash;
    }

    public static boolean hasCatalogBinding() {
        return catalogEpoch >= 0L && !catalogHash.isEmpty();
    }

    public static long getWandRevision() {
        return wandRevision;
    }

    public static long getCatalogEpoch() {
        return catalogEpoch;
    }

    public static int getStateHash() {
        return stateHash;
    }

    public static String getCatalogHash() {
        return catalogHash;
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

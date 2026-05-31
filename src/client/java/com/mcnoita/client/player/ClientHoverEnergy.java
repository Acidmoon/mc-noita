package com.mcnoita.client.player;

public final class ClientHoverEnergy {
    private static int energy = 1000;
    private static int maxEnergy = 1000;

    private ClientHoverEnergy() {
    }

    public static void set(int energy, int maxEnergy) {
        ClientHoverEnergy.energy = Math.max(0, energy);
        ClientHoverEnergy.maxEnergy = Math.max(1, maxEnergy);
    }

    public static float getFraction() {
        return Math.max(0.0f, Math.min(1.0f, energy / (float) maxEnergy));
    }

    public static boolean hasEnergy() {
        return energy > 0;
    }
}

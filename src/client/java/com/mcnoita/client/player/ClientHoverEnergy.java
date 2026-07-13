package com.mcnoita.client.player;

public final class ClientHoverEnergy {
    private static int energy = 1000;
    private static int maxEnergy = 1000;

    private ClientHoverEnergy() {
    }

    public static void set(int energy, int maxEnergy) {
        ClientHoverEnergy.maxEnergy = Math.max(1, Math.min(1_000_000, maxEnergy));
        ClientHoverEnergy.energy = Math.max(0, Math.min(ClientHoverEnergy.maxEnergy, energy));
    }

    public static float getFraction() {
        return Math.max(0.0f, Math.min(1.0f, energy / (float) maxEnergy));
    }

    public static boolean hasEnergy() {
        return energy > 0;
    }
}

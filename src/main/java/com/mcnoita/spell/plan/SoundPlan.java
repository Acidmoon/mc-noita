package com.mcnoita.spell.plan;

public record SoundPlan(SoundKind kind) {
    public enum SoundKind {
        PROJECTILE_CAST,
        FUSED_EXPLOSIVE_CAST
    }
}

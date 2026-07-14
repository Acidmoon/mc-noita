package com.mcnoita.spell.plan;

import java.util.List;
import java.util.Objects;

/** Pure plan executed only after the server commits the evaluated WandState. */
public record EffectPlan(List<ProjectilePlan> projectiles, List<SoundPlan> sounds, List<RecoilPlan> recoils) {
    public EffectPlan {
        projectiles = List.copyOf(Objects.requireNonNull(projectiles, "projectiles"));
        sounds = List.copyOf(Objects.requireNonNull(sounds, "sounds"));
        recoils = List.copyOf(Objects.requireNonNull(recoils, "recoils"));
    }

    public static EffectPlan empty() {
        return new EffectPlan(List.of(), List.of(), List.of());
    }
}

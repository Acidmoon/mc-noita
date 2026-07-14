package com.mcnoita.spell.plan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Pure typed node plan executed only after the server commits the evaluated WandState. */
public record EffectPlan(List<EffectNode> nodes) {
    public EffectPlan {
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        Set<String> nodePaths = new HashSet<>();
        for (EffectNode node : nodes) {
            Objects.requireNonNull(node, "effect node");
            if (!nodePaths.add(node.nodePath())) {
                throw new IllegalArgumentException("effect node paths must be unique: " + node.nodePath());
            }
        }
    }

    /** Compatibility constructor for the existing pure evaluator output. */
    public EffectPlan(List<ProjectilePlan> projectiles, List<SoundPlan> sounds, List<RecoilPlan> recoils) {
        this(compatibilityNodes(projectiles, sounds, recoils));
    }

    /** Legacy projection retained until all evaluator actions emit typed nodes directly. */
    public List<ProjectilePlan> projectiles() {
        List<ProjectilePlan> values = new ArrayList<>();
        for (EffectNode node : nodes) {
            if (node instanceof ProjectileEffectNode projectile) {
                values.add(projectile.projectile());
            }
        }
        return List.copyOf(values);
    }

    /** Legacy projection retained until all evaluator actions emit typed nodes directly. */
    public List<SoundPlan> sounds() {
        List<SoundPlan> values = new ArrayList<>();
        for (EffectNode node : nodes) {
            if (node instanceof SoundEffectNode sound) {
                values.add(sound.sound());
            }
        }
        return List.copyOf(values);
    }

    /** Legacy projection retained until all evaluator actions emit typed nodes directly. */
    public List<RecoilPlan> recoils() {
        List<RecoilPlan> values = new ArrayList<>();
        for (EffectNode node : nodes) {
            if (node instanceof RecoilEffectNode recoil) {
                values.add(recoil.recoil());
            }
        }
        return List.copyOf(values);
    }

    public static EffectPlan empty() {
        return new EffectPlan(List.of());
    }

    private static List<EffectNode> compatibilityNodes(
        List<ProjectilePlan> projectiles, List<SoundPlan> sounds, List<RecoilPlan> recoils
    ) {
        Objects.requireNonNull(projectiles, "projectiles");
        Objects.requireNonNull(sounds, "sounds");
        Objects.requireNonNull(recoils, "recoils");
        List<EffectNode> nodes = new ArrayList<>(projectiles.size() + sounds.size() + recoils.size());
        for (ProjectilePlan projectile : projectiles) {
            nodes.add(new ProjectileEffectNode(projectile));
        }
        // Preserve the previous server execution order: projectiles, recoil, then sound.
        for (int index = 0; index < recoils.size(); index++) {
            nodes.add(new RecoilEffectNode("compat/recoil/" + index, recoils.get(index)));
        }
        for (int index = 0; index < sounds.size(); index++) {
            nodes.add(new SoundEffectNode("compat/sound/" + index, sounds.get(index)));
        }
        return List.copyOf(nodes);
    }
}

package com.mcnoita.spell.trigger;

import com.mcnoita.spell.plan.EffectNode;
import com.mcnoita.spell.plan.ProjectileEffectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure allocation of frozen Trigger-tree capacity to root projectile instances.
 * The ceiling is supplied by the server's configured per-cast budget, rather
 * than silently reverting to the legacy 32/32 bootstrap default after commit.
 */
public final class RootTriggerBudgetAllocator {
    private RootTriggerBudgetAllocator() {
    }

    public static Allocation allocate(List<EffectNode> nodes, TriggerRuntimeBudget ceiling) {
        Objects.requireNonNull(nodes, "nodes");
        Objects.requireNonNull(ceiling, "ceiling");

        List<RootSlot> slots = new ArrayList<>();
        int entityLimit = ceiling.remainingSpawnedEntities();
        int releaseLimit = ceiling.remainingReleaseEvents();
        for (EffectNode node : nodes) {
            if (!(node instanceof ProjectileEffectNode projectileNode)) {
                continue;
            }
            int projectileCount = projectileNode.projectile().projectileCount();
            int requiredRoots = addCapped(slots.size(), projectileCount);
            if (projectileCount < 1 || requiredRoots > entityLimit) {
                return Allocation.rejected(Rejection.AUTHORITATIVE_ENTITIES, requiredRoots, entityLimit);
            }
            int futureEntities = cappedInt(projectileNode.projectile().futureEntityFootprintPerInstance());
            int releaseEvents = cappedInt(projectileNode.projectile().staticReleaseEventFootprintPerInstance());
            for (int index = 0; index < projectileCount; index++) {
                slots.add(new RootSlot(projectileNode.nodePath(), new RootRequirement(releaseEvents, futureEntities)));
            }
        }
        if (slots.isEmpty()) {
            return Allocation.accepted(Map.of());
        }

        int requiredEvents = 0;
        int requiredEntities = 0;
        for (RootSlot slot : slots) {
            requiredEvents = addCapped(requiredEvents, slot.requirement().releaseEvents());
            requiredEntities = addCapped(requiredEntities, slot.requirement().futureEntities());
        }
        int availableEntities = entityLimit - slots.size();
        if (requiredEntities > availableEntities) {
            return Allocation.rejected(Rejection.AUTHORITATIVE_ENTITIES,
                addCapped(slots.size(), requiredEntities), entityLimit);
        }
        if (requiredEvents > releaseLimit) {
            return Allocation.rejected(Rejection.TRIGGER_RELEASES, requiredEvents, releaseLimit);
        }

        int[] events = new int[slots.size()];
        int[] entities = new int[slots.size()];
        for (int index = 0; index < slots.size(); index++) {
            RootRequirement requirement = slots.get(index).requirement();
            events[index] = requirement.releaseEvents();
            entities[index] = requirement.futureEntities();
        }
        distribute(events, releaseLimit - requiredEvents);
        distribute(entities, availableEntities - requiredEntities);

        Map<String, List<TriggerRuntimeBudget>> budgets = new LinkedHashMap<>();
        for (int index = 0; index < slots.size(); index++) {
            budgets.computeIfAbsent(slots.get(index).nodePath(), ignored -> new ArrayList<>())
                .add(new TriggerRuntimeBudget(events[index], entities[index]));
        }
        Map<String, List<TriggerRuntimeBudget>> immutable = new LinkedHashMap<>();
        for (Map.Entry<String, List<TriggerRuntimeBudget>> entry : budgets.entrySet()) {
            immutable.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Allocation.accepted(Collections.unmodifiableMap(immutable));
    }

    private static void distribute(int[] values, int extras) {
        for (int index = 0; values.length > 0 && index < extras; index++) {
            values[index % values.length]++;
        }
    }

    private static int addCapped(int left, int right) {
        return right > Integer.MAX_VALUE - left ? Integer.MAX_VALUE : left + right;
    }

    private static int cappedInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }

    public enum Rejection {
        AUTHORITATIVE_ENTITIES,
        TRIGGER_RELEASES
    }

    public record Allocation(
        boolean accepted, Rejection rejection, int requested, int limit, Map<String, List<TriggerRuntimeBudget>> budgets
    ) {
        public Allocation {
            budgets = Map.copyOf(Objects.requireNonNull(budgets, "budgets"));
            if (requested < 0 || limit < 0) {
                throw new IllegalArgumentException("trigger budget diagnostics must not be negative");
            }
            if (accepted && rejection != null) {
                throw new IllegalArgumentException("accepted allocation must not have a rejection");
            }
            if (!accepted && rejection == null) {
                throw new IllegalArgumentException("rejected allocation must identify the exhausted budget");
            }
        }

        private static Allocation accepted(Map<String, List<TriggerRuntimeBudget>> budgets) {
            return new Allocation(true, null, 0, 0, budgets);
        }

        private static Allocation rejected(Rejection rejection, int requested, int limit) {
            return new Allocation(false, rejection, requested, limit, Map.of());
        }
    }

    private record RootRequirement(int releaseEvents, int futureEntities) {
    }

    private record RootSlot(String nodePath, RootRequirement requirement) {
    }
}

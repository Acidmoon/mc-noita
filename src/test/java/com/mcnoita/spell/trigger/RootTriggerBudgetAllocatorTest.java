package com.mcnoita.spell.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcnoita.spell.plan.EffectNode;
import com.mcnoita.spell.plan.PayloadPlan;
import com.mcnoita.spell.plan.ProjectileEffectNode;
import com.mcnoita.spell.plan.ProjectilePlan;
import com.mcnoita.spell.plan.TriggerMode;
import com.mcnoita.spell.plan.TriggerPlan;
import com.mcnoita.spell.plan.TriggerReleasePolicy;
import com.mcnoita.wand.model.NoitaDuration;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Regression coverage for the shared pre-commit and runtime Trigger allocator. */
@Tag("regression")
class RootTriggerBudgetAllocatorTest {
    @Test
    void configuredCeilingAboveTheLegacyDefaultAdmitsTheFrozenRootTree() {
        RootTriggerBudgetAllocator.Allocation allocation = RootTriggerBudgetAllocator.allocate(
            List.of(new ProjectileEffectNode(rootWithThirtyTwoChildren())), new TriggerRuntimeBudget(64, 64)
        );

        assertTrue(allocation.accepted());
        assertEquals(1, allocation.budgets().get("root/0").size());
        assertTrue(allocation.budgets().get("root/0").get(0).remainingSpawnedEntities() >= 32,
            "one root plus its 32 frozen children must retain enough persisted local capacity");
    }

    @Test
    void lowConfiguredEntityCeilingRejectsTheWholeFrozenTreeBeforeCommit() {
        RootTriggerBudgetAllocator.Allocation allocation = RootTriggerBudgetAllocator.allocate(
            List.of(new ProjectileEffectNode(rootWithThirtyTwoChildren())), new TriggerRuntimeBudget(64, 1)
        );

        assertFalse(allocation.accepted());
        assertEquals(RootTriggerBudgetAllocator.Rejection.AUTHORITATIVE_ENTITIES, allocation.rejection());
        assertEquals(33, allocation.requested());
        assertEquals(1, allocation.limit());
    }

    private static ProjectilePlan rootWithThirtyTwoChildren() {
        List<ProjectilePlan> children = java.util.stream.IntStream.range(0, 32)
            .mapToObj(index -> projectile("root/0/trigger/0/" + index, null))
            .toList();
        PayloadPlan payload = new PayloadPlan("root/0/trigger/0", 1, children);
        TriggerPlan trigger = new TriggerPlan("root/0/trigger/0", TriggerMode.HIT, NoitaDuration.ZERO, 1,
            TriggerReleasePolicy.COLLISION_WHILE_ALIVE, payload);
        return projectile("root/0", trigger);
    }

    private static ProjectilePlan projectile(String path, TriggerPlan trigger) {
        return new ProjectilePlan(path, "spark_bolt", "BOLT", 1.0, 0.0, NoitaDuration.frames(60), 0,
            0.0, 1.0, 0.0, 0.0, 0.99, 0.65, 1.0, 0.0, false, false, 1, 0.0, trigger, 0, List.of());
    }
}

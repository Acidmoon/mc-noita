package com.mcnoita.spell.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcnoita.spell.NoitaPayloadPlan;
import com.mcnoita.spell.NoitaProjectileBehavior;
import com.mcnoita.spell.NoitaProjectilePayload;
import com.mcnoita.spell.NoitaSpellTriggerMode;
import com.mcnoita.spell.NoitaTriggerPlan;
import com.mcnoita.spell.NoitaTriggerReleasePolicy;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Pure state-machine tests; Minecraft entities only adapt these decisions to callbacks. */
@Tag("regression")
class TriggerPayloadControllerTest {
    @Test
    void hitCollisionIsDeduplicatedButLaterPiercingHitsMayReleaseAgain() {
        TriggerPayloadController controller = controller(NoitaSpellTriggerMode.HIT, new TriggerRuntimeBudget(12, 12));
        CollisionKey firstHit = collision(40, "entity:first");

        ReleaseDecision first = controller.accept(TriggerEvent.collision(firstHit));
        ReleaseDecision duplicateCallback = controller.accept(TriggerEvent.collision(firstHit));
        ReleaseDecision secondHit = controller.accept(TriggerEvent.collision(collision(41, "entity:second")));

        assertTrue(first.shouldRelease());
        assertEquals(1, first.releaseSequence());
        assertFalse(duplicateCallback.shouldRelease());
        assertTrue(secondHit.shouldRelease());
        assertEquals(2, secondHit.releaseSequence());
        assertEquals(1, first.payloads().size());
    }

    @Test
    void timerAllowsCollisionReleasesThenOneFinalExpiryRelease() {
        TriggerPayloadController controller = controller(NoitaSpellTriggerMode.TIMER, new TriggerRuntimeBudget(16, 16));

        assertTrue(controller.accept(TriggerEvent.collision(collision(20, "block:1"))).shouldRelease());
        ReleaseDecision expiry = controller.accept(TriggerEvent.timerExpired());

        assertTrue(expiry.shouldRelease());
        assertEquals(2, expiry.releaseSequence());
        ReleaseDecision rejected = controller.accept(TriggerEvent.timerExpired());

        assertFalse(rejected.shouldRelease());
        assertNull(rejected.budgetExhaustion());
        assertFalse(controller.accept(TriggerEvent.collision(collision(21, "block:after-expiry"))).shouldRelease());
        assertTrue(controller.state().timerExpired());
    }

    @Test
    void expirationIgnoresUnloadAndReleasesAtMostOnceForRealTermination() {
        TriggerPayloadController controller = controller(NoitaSpellTriggerMode.EXPIRATION, new TriggerRuntimeBudget(8, 8));

        assertFalse(controller.accept(TriggerEvent.terminated(ProjectileTerminationCause.UNLOAD)).shouldRelease());
        assertTrue(controller.accept(TriggerEvent.terminated(ProjectileTerminationCause.NATURAL_EXPIRY)).shouldRelease());
        assertFalse(controller.accept(TriggerEvent.terminated(ProjectileTerminationCause.KILLED)).shouldRelease());
        assertTrue(controller.state().expirationReleased());
    }

    @Test
    void parentAndChildReceiveDisjointRuntimeBudgetShares() {
        TriggerRuntimeBudget original = new TriggerRuntimeBudget(9, 9);
        TriggerPayloadController controller = controller(NoitaSpellTriggerMode.HIT, original);

        ReleaseDecision decision = controller.accept(TriggerEvent.collision(collision(5, "block:2")));
        TriggerRuntimeBudget child = decision.payloads().get(0).childBudgets().get(0);

        assertTrue(decision.shouldRelease());
        assertEquals(original.remainingReleaseEvents() - 1,
            controller.state().remainingBudget().remainingReleaseEvents() + child.remainingReleaseEvents());
        assertEquals(original.remainingSpawnedEntities() - 1,
            controller.state().remainingBudget().remainingSpawnedEntities() + child.remainingSpawnedEntities());
    }

    @Test
    void exhaustedBudgetMarksTheFrozenNodeInertWithoutAPartialRelease() {
        TriggerPayloadController controller = controller(NoitaSpellTriggerMode.TIMER, new TriggerRuntimeBudget(1, 0));

        ReleaseDecision rejected = controller.accept(TriggerEvent.timerExpired());

        assertFalse(rejected.shouldRelease());
        assertEquals(TriggerBudgetExhaustion.DIRECT_RELEASE_CAPACITY, rejected.budgetExhaustion());
        assertTrue(controller.state().timerExpired());
        assertTrue(controller.state().inert());
    }

    @Test
    void nestedChildReceivesItsOwnFirstReleaseReservation() {
        NoitaProjectilePayload leaf = projectile(NoitaSpellTriggerMode.NONE, List.of());
        NoitaProjectilePayload nested = projectile(NoitaSpellTriggerMode.HIT, List.of(leaf));
        NoitaTriggerPlan rootPlan = new NoitaTriggerPlan(NoitaSpellTriggerMode.HIT, 0,
            List.of(new NoitaPayloadPlan("root/0/trigger/payload", List.of(nested))), "root/0/trigger", 1,
            NoitaTriggerReleasePolicy.VALID_COLLISION);
        TriggerPayloadController root = new TriggerPayloadController(rootPlan,
            TriggerRuntimeState.fresh(new TriggerRuntimeBudget(2, 2)));

        ReleaseDecision rootRelease = root.accept(TriggerEvent.collision(collision(10, "block:root")));
        TriggerRuntimeBudget nestedBudget = rootRelease.payloads().get(0).childBudgets().get(0);
        TriggerPayloadController child = new TriggerPayloadController(nested.triggerPlan(),
            TriggerRuntimeState.fresh(nestedBudget));

        assertTrue(rootRelease.shouldRelease());
        assertTrue(child.accept(TriggerEvent.collision(collision(11, "block:child"))).shouldRelease());
    }

    private static TriggerPayloadController controller(NoitaSpellTriggerMode mode, TriggerRuntimeBudget budget) {
        NoitaProjectilePayload child = projectile(NoitaSpellTriggerMode.NONE, List.of());
        NoitaPayloadPlan payload = new NoitaPayloadPlan("root/0/trigger/payload", List.of(child));
        NoitaTriggerPlan plan = new NoitaTriggerPlan(mode, 4, List.of(payload), "root/0/trigger", 1,
            NoitaTriggerReleasePolicy.forMode(mode));
        return new TriggerPayloadController(plan, TriggerRuntimeState.fresh(budget));
    }

    private static NoitaProjectilePayload projectile(
        NoitaSpellTriggerMode mode, List<NoitaProjectilePayload> triggerPayloads
    ) {
        return new NoitaProjectilePayload(
            "spark_bolt", NoitaProjectileBehavior.BOLT, 2.0f, 0.0f, 20, 0, 0.0f, 1.0f, 0.0f,
            0.0f, 0.99f, 0.65f, 1.0f, 0.0f, false, false, 1, 0.0f,
            mode, 0, 0, List.of(), triggerPayloads
        );
    }

    private static CollisionKey collision(long tick, String target) {
        return new CollisionKey(tick, target, "ENTITY", "root/0/trigger");
    }
}

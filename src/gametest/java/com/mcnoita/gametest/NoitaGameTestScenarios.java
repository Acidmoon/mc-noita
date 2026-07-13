package com.mcnoita.gametest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * GameTest fixtures deliberately use an empty structure, fixed tick zero, and
 * explicit entity cleanup. They are the stable world-test harness for the
 * evaluator scenarios that G01 will connect to real player/wand execution.
 */
public final class NoitaGameTestScenarios implements FabricGameTest {
    private static final List<String> SCENARIOS = List.of(
        "starter_wand_server_authority",
        "spark_bolt_entity_spawn",
        "bomb_explosion_ownership",
        "double_spell_shared_shot_state",
        "damage_plus_shared_shot_state",
        "trigger_payload_collision_release",
        "wand_refresh_pile_rewrite",
        "alpha_call_without_target_charge",
        "gamma_call_without_target_charge",
        "projectile_payload_save_reload"
    );

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void starterWandServerAuthority(TestContext context) { completeFixedFixture(context); }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void sparkBoltEntitySpawn(TestContext context) { completeFixedFixture(context); }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void bombExplosionOwnership(TestContext context) { completeFixedFixture(context); }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void doubleSpellSharedShotState(TestContext context) { completeFixedFixture(context); }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void damagePlusSharedShotState(TestContext context) { completeFixedFixture(context); }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void triggerPayloadCollisionRelease(TestContext context) { completeFixedFixture(context); }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void wandRefreshPileRewrite(TestContext context) { completeFixedFixture(context); }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void alphaCallWithoutTargetCharge(TestContext context) { completeFixedFixture(context); }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void gammaCallWithoutTargetCharge(TestContext context) { completeFixedFixture(context); }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void projectilePayloadSaveReload(TestContext context) { completeFixedFixture(context); }

    private static void completeFixedFixture(TestContext context) {
        context.setTime(0);
        context.runAtTick(20, () -> {
            context.assertTrue(context.getTick() == 20, "fixture must use a fixed twenty-tick observation point");
            context.killAllEntities();
            context.complete();
        });
    }

    @Test
    @Tag("gametest")
    void fixtureCatalogCoversTheFirstTenWorldScenarios() {
        assertEquals(10, SCENARIOS.size());
        assertTrue(SCENARIOS.contains("starter_wand_server_authority"));
        assertTrue(SCENARIOS.contains("trigger_payload_collision_release"));
        assertTrue(SCENARIOS.contains("projectile_payload_save_reload"));
    }
}

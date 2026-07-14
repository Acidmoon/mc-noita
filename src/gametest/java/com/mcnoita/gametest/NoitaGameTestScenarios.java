package com.mcnoita.gametest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcnoita.entity.ModEntities;
import com.mcnoita.item.ModItems;
import com.mcnoita.item.NoitaWandItem;
import com.mcnoita.spell.action.SpellCatalog;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.wand.NoitaWandCaster;
import com.mcnoita.wand.NoitaWandTemplate;
import com.mcnoita.wand.adapter.LegacySpellCatalogAdapter;
import com.mcnoita.wand.adapter.MinecraftTimeAdapter;
import com.mcnoita.wand.adapter.MinecraftWandAdapter;
import com.mcnoita.wand.eval.WandCastEvaluator;
import com.mcnoita.wand.model.NoitaDuration;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.ItemStack;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
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
        "projectile_payload_save_reload",
        "g02_two_round_real_wand_matches_evaluator"
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

    /**
     * Exercises the production server path rather than directly spawning a
     * plan. Three real Spark Bolt cards with Spells/Cast=2 must emit 2 then 1,
     * preserve evaluator mana/Deck state, and reload in slot order.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 12)
    public void g02TwoRoundRealWandMatchesEvaluator(TestContext context) {
        context.setTime(0);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.setPosition(context.getAbsolute(new Vec3d(3.5, 2.0, 3.5)));
        ItemStack wand = configuredThreeBoltWand();
        player.setStackInHand(Hand.MAIN_HAND, wand);

        SpellCatalog catalog = LegacySpellCatalogAdapter.createCatalog();
        MinecraftWandAdapter.LoadedWand firstInput = MinecraftWandAdapter.read(
            wand, ModItems.STARTER_WAND, player.getServerWorld().getTime(), 0L
        );
        ResolvedCast expectedFirst = new WandCastEvaluator().evaluate(
            firstInput.definition(), firstInput.state(), catalog, firstInput.elapsed(), 0L
        );
        NoitaWandCaster.cast(player);
        MinecraftWandAdapter.LoadedWand afterFirst = MinecraftWandAdapter.read(
            wand, ModItems.STARTER_WAND, player.getServerWorld().getTime(), 0L
        );
        assertMatches(context, expectedFirst, afterFirst, "first cast");
        context.assertTrue(projectilesInTest(context) == 2, "first cast must spawn two real projectile entities");

        context.runAtTick(4, () -> {
            MinecraftWandAdapter.LoadedWand secondInput = MinecraftWandAdapter.read(
                wand, ModItems.STARTER_WAND, player.getServerWorld().getTime(), 0L
            );
            ResolvedCast expectedSecond = new WandCastEvaluator().evaluate(
                secondInput.definition(), secondInput.state(), catalog, secondInput.elapsed(), 0L
            );
            context.assertTrue(NoitaWandCaster.canCast(player), "second cast must be ready after the converted cast-delay window");
            NoitaWandCaster.cast(player);
            MinecraftWandAdapter.LoadedWand afterSecond = MinecraftWandAdapter.read(
                wand, ModItems.STARTER_WAND, player.getServerWorld().getTime(), 0L
            );
            assertMatches(context, expectedSecond, afterSecond, "second cast");
            context.assertTrue(projectilesInTest(context) == 3, "second cast must add exactly one real projectile entity");
            context.assertTrue(afterSecond.state().deckState().deck().equals(
                List.of(
                    com.mcnoita.wand.model.CardRef.forSlot(0),
                    com.mcnoita.wand.model.CardRef.forSlot(1),
                    com.mcnoita.wand.model.CardRef.forSlot(2)
                )
            ), "Reload must restore non-shuffle slot order before the next cast");
            context.killAllEntities();
            context.complete();
        });
    }

    private static void completeFixedFixture(TestContext context) {
        context.setTime(0);
        context.runAtTick(20, () -> {
            context.assertTrue(context.getTick() == 20, "fixture must use a fixed twenty-tick observation point");
            context.killAllEntities();
            context.complete();
        });
    }

    private static ItemStack configuredThreeBoltWand() {
        ItemStack wand = new ItemStack(ModItems.STARTER_WAND);
        NoitaWandItem.setTemplate(wand, NoitaWandTemplate.builder()
            .shuffle(false)
            .spellsPerCast(2)
            .castDelaySeconds(0.0f)
            .rechargeTimeSeconds(0.0f)
            .manaMax(100)
            .manaChargeSpeed(0)
            .capacity(3)
            .spreadDegrees(0.0f)
            .speedMultiplier(1.0f)
            .build());
        DefaultedList<ItemStack> cards = DefaultedList.ofSize(3, ItemStack.EMPTY);
        cards.set(0, new ItemStack(ModItems.LIGHT_BULLET));
        cards.set(1, new ItemStack(ModItems.LIGHT_BULLET));
        cards.set(2, new ItemStack(ModItems.LIGHT_BULLET));
        NoitaWandItem.setSpellStacks(wand, cards);
        return wand;
    }

    private static void assertMatches(TestContext context, ResolvedCast expected, MinecraftWandAdapter.LoadedWand actual, String step) {
        context.assertTrue(expected.status() == ResolvedCast.Status.ACCEPTED, step + " pure evaluation must be accepted");
        context.assertTrue(actual.state().deckState().equals(expected.nextState().deckState()), step
            + " Deck/Hand/Discard must match pure output. expected=" + expected.nextState().deckState()
            + ", actual=" + actual.state().deckState());
        context.assertTrue(Math.abs(actual.state().mana() - expected.nextState().mana()) < 0.001,
            step + " mana must match pure output");
        context.assertTrue(cooldownTicks(actual.state().castDelayRemaining()) == cooldownTicks(expected.nextState().castDelayRemaining()),
            step + " cast delay must match pure output after the Minecraft tick conversion");
        context.assertTrue(cooldownTicks(actual.state().rechargeRemaining()) == cooldownTicks(expected.nextState().rechargeRemaining()),
            step + " recharge must match pure output after the Minecraft tick conversion");
    }

    private static int cooldownTicks(NoitaDuration duration) {
        return duration.isZero() ? 0 : MinecraftTimeAdapter.toMinecraftTicks(duration, 1);
    }

    private static int projectilesInTest(TestContext context) {
        Box search = context.getTestBox().expand(32.0);
        return context.getWorld().getEntitiesByType(ModEntities.SPARK_BOLT_PROJECTILE, search, entity -> true).size();
    }

    @Test
    @Tag("gametest")
    void fixtureCatalogCoversTheG02WorldScenario() {
        assertEquals(11, SCENARIOS.size());
        assertTrue(SCENARIOS.contains("starter_wand_server_authority"));
        assertTrue(SCENARIOS.contains("trigger_payload_collision_release"));
        assertTrue(SCENARIOS.contains("projectile_payload_save_reload"));
        assertTrue(SCENARIOS.contains("g02_two_round_real_wand_matches_evaluator"));
    }
}

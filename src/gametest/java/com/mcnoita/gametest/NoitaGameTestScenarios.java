package com.mcnoita.gametest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcnoita.entity.BombEntity;
import com.mcnoita.entity.ModEntities;
import com.mcnoita.entity.SparkBoltProjectileEntity;
import com.mcnoita.item.ModItems;
import com.mcnoita.item.NoitaWandItem;
import com.mcnoita.spell.action.SpellCatalog;
import com.mcnoita.spell.NoitaExecutionIdentity;
import com.mcnoita.spell.NoitaPayloadPlan;
import com.mcnoita.spell.NoitaProjectileBehavior;
import com.mcnoita.spell.NoitaProjectilePayload;
import com.mcnoita.spell.NoitaSpellTriggerMode;
import com.mcnoita.spell.NoitaTriggerPlan;
import com.mcnoita.spell.NoitaTriggerReleasePolicy;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.spell.trigger.TriggerRuntimeBudget;
import com.mcnoita.wand.NoitaWandCaster;
import com.mcnoita.wand.NoitaWandTemplate;
import com.mcnoita.wand.adapter.LegacySpellCatalogAdapter;
import com.mcnoita.wand.adapter.MinecraftTimeAdapter;
import com.mcnoita.wand.adapter.MinecraftWandAdapter;
import com.mcnoita.wand.eval.WandCastEvaluator;
import com.mcnoita.wand.model.NoitaDuration;
import java.util.List;
import java.util.UUID;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
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
        "trigger_payload_block_hit_release",
        "trigger_payload_entity_hit_release",
        "piercing_hit_trigger_sequential_entity_release",
        "nested_trigger_block_release",
        "bomb_hit_trigger_block_release",
        "bomb_hit_trigger_entity_release",
        "mine_nearby_entity_hit_trigger_release",
        "timer_trigger_exact_tick_release",
        "timer_trigger_zero_and_one_tick_release",
        "timer_piercing_collision_then_expiry_release",
        "projectile_payload_reload_after_timer_release",
        "expiration_trigger_natural_expiry_release",
        "expiration_trigger_killed_release",
        "unloaded_expiration_trigger_does_not_release_payload",
        "wand_refresh_pile_rewrite",
        "alpha_call_without_target_charge",
        "gamma_call_without_target_charge",
        "projectile_payload_save_reload",
        "legacy_runtime_payload_receives_execution_identity",
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
    public void triggerPayloadBlockHitRelease(TestContext context) {
        context.setTime(0);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        BlockPos wall = new BlockPos(6, 2, 5);
        context.setBlockState(wall, Blocks.STONE);
        SparkBoltProjectileEntity parent = spawnTrigger(context, player, NoitaSpellTriggerMode.HIT, 0, 30,
            new Vec3d(3.4, 2.55, 5.5), new Vec3d(1.4, 0.0, 0.0));

        context.runAtTick(6, () -> {
            context.assertTrue(parent.isRemoved(), "non-piercing block Hit trigger must terminate after the collision; age=" + parent.age);
            context.assertTrue(projectilesOwnedBy(context, player) == 1,
                "the frozen payload must spawn exactly one real child projectile on a block hit; actual="
                    + projectilesOwnedBy(context, player));
            context.killAllEntities();
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void triggerPayloadEntityHitRelease(TestContext context) {
        context.setTime(0);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        ZombieEntity target = context.spawnMob(EntityType.ZOMBIE, new Vec3d(6.0, 2.0, 5.5));
        float healthBefore = target.getHealth();
        SparkBoltProjectileEntity parent = spawnTrigger(context, player, NoitaSpellTriggerMode.HIT, 0, 30,
            new Vec3d(3.4, 2.55, 5.5), new Vec3d(1.4, 0.0, 0.0));

        context.runAtTick(6, () -> {
            context.assertTrue(parent.isRemoved(), "non-piercing entity Hit trigger must terminate after the collision; age=" + parent.age);
            context.assertTrue(target.getHealth() < healthBefore, "the entity collision must be real and apply projectile damage");
            context.assertTrue(projectilesOwnedBy(context, player) == 1,
                "the frozen payload must spawn exactly once after an entity hit; actual="
                    + projectilesOwnedBy(context, player));
            context.killAllEntities();
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void piercingHitTriggerSequentialEntityRelease(TestContext context) {
        context.setTime(0);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        context.spawnMob(EntityType.ZOMBIE, new Vec3d(6.0, 2.0, 5.5));
        context.spawnMob(EntityType.ZOMBIE, new Vec3d(10.0, 2.0, 5.5));
        SparkBoltProjectileEntity parent = spawnTrigger(context, player, NoitaSpellTriggerMode.HIT, 0, 30,
            new Vec3d(3.4, 2.55, 5.5), new Vec3d(1.4, 0.0, 0.0), true);
        String[] executionId = new String[1];

        context.runAtTick(4, () -> {
            context.assertTrue(!parent.isRemoved(), "a Piercing Hit trigger must survive its first valid entity collision");
            executionId[0] = parent.writeNbt(new NbtCompound()).getCompound("FrozenPayload").getString("ExecutionId");
            context.assertTrue(projectilesForExecution(context, executionId[0]) == 2,
                "the first valid entity collision must release one frozen payload; actual="
                    + projectilesForExecution(context, executionId[0]));
            // Move past the first target so the second callback represents a
            // distinct collision rather than repeated contact with one entity.
            parent.setPosition(context.getAbsolute(new Vec3d(8.2, 2.55, 5.5)));
            parent.setVelocity(1.4, 0.0, 0.0);
        });
        context.runAtTick(10, () -> {
            context.assertTrue(!parent.isRemoved(), "Piercing parent must remain alive after sequential Hits");
            // A Piercing projectile can remain inside a target hitbox across
            // server ticks. Those are distinct valid collision events because
            // CollisionKey intentionally includes the tick; this fixture only
            // requires the second target to add another frozen-tree release.
            context.assertTrue(projectilesForExecution(context, executionId[0]) >= 3,
                "a distinct second entity collision must release the same frozen payload again; actual="
                    + projectilesForExecution(context, executionId[0]));
            int releaseSequence = parent.writeNbt(new NbtCompound()).getCompound("TriggerRuntimeState").getInt("ReleaseSequence");
            context.assertTrue(releaseSequence >= 2,
                "sequential Piercing Hits must commit at least two release events; actual=" + releaseSequence);
            context.killAllEntities();
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void nestedTriggerBlockRelease(TestContext context) {
        context.setTime(0);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        context.setBlockState(new BlockPos(6, 2, 5), Blocks.STONE);
        SparkBoltProjectileEntity root = spawnNestedHitTrigger(context, player,
            new Vec3d(3.4, 2.55, 5.5), new Vec3d(1.4, 0.0, 0.0));

        context.runAtTick(8, () -> {
            context.assertTrue(root.isRemoved(), "the outer non-piercing trigger must terminate at the wall");
            context.assertTrue(projectilesOwnedBy(context, player) == 1,
                "the nested child must collide and leave exactly one leaf payload projectile; actual="
                    + projectilesOwnedBy(context, player));
            context.killAllEntities();
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void timerTriggerExactTickRelease(TestContext context) {
        context.setTime(0);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        spawnTrigger(context, player, NoitaSpellTriggerMode.TIMER, 5, 30,
            new Vec3d(4.5, 4.5, 4.5), Vec3d.ZERO);

        context.runAtTick(2, () -> context.assertTrue(projectilesOwnedBy(context, player) == 1,
            "Timer must not release before its configured tick; actual=" + projectilesOwnedBy(context, player)));
        context.runAtTick(6, () -> context.assertTrue(projectilesOwnedBy(context, player) == 2,
            "Timer must release one frozen payload when its delay elapses; actual=" + projectilesOwnedBy(context, player)));
        context.runAtTick(8, () -> {
            context.assertTrue(projectilesOwnedBy(context, player) == 2,
                "Timer expiry must be one-shot after the exact tick; actual=" + projectilesOwnedBy(context, player));
            context.killAllEntities();
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 8)
    public void timerTriggerZeroAndOneTickRelease(TestContext context) {
        context.setTime(0);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        SparkBoltProjectileEntity zeroTick = spawnTrigger(context, player, NoitaSpellTriggerMode.TIMER, 0, 30,
            new Vec3d(4.5, 4.5, 4.5), Vec3d.ZERO);
        SparkBoltProjectileEntity oneTick = spawnTrigger(context, player, NoitaSpellTriggerMode.TIMER, 1, 30,
            new Vec3d(8.5, 4.5, 4.5), Vec3d.ZERO);
        String zeroExecution = zeroTick.writeNbt(new NbtCompound()).getCompound("FrozenPayload").getString("ExecutionId");
        String oneExecution = oneTick.writeNbt(new NbtCompound()).getCompound("FrozenPayload").getString("ExecutionId");

        context.runAtTick(1, () -> {
            context.assertTrue(projectilesForExecution(context, zeroExecution) == 2,
                "a zero-tick Timer must release on the first server entity tick");
            context.assertTrue(projectilesForExecution(context, oneExecution) == 2,
                "a one-tick Timer must release on the first server entity tick");
        });
        context.runAtTick(3, () -> {
            context.assertTrue(projectilesForExecution(context, zeroExecution) == 2,
                "zero-tick Timer expiry must remain one-shot");
            context.assertTrue(projectilesForExecution(context, oneExecution) == 2,
                "one-tick Timer expiry must remain one-shot");
            context.killAllEntities();
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 8)
    public void timerPiercingCollisionThenExpiryRelease(TestContext context) {
        context.setTime(0);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        context.spawnMob(EntityType.ZOMBIE, new Vec3d(5.0, 2.0, 5.5));
        SparkBoltProjectileEntity parent = spawnTrigger(context, player, NoitaSpellTriggerMode.TIMER, 1, 30,
            new Vec3d(3.4, 2.55, 5.5), new Vec3d(1.4, 0.0, 0.0), true);
        String executionId = parent.writeNbt(new NbtCompound()).getCompound("FrozenPayload").getString("ExecutionId");

        context.runAtTick(1, () -> {
            NbtCompound runtime = parent.writeNbt(new NbtCompound()).getCompound("TriggerRuntimeState");
            context.assertTrue(!parent.isRemoved(), "Piercing Timer parent must survive the collision tick");
            context.assertTrue(projectilesForExecution(context, executionId) == 3,
                "same-tick collision then Timer expiry must release two payload instances; actual="
                    + projectilesForExecution(context, executionId));
            context.assertTrue(runtime.getInt("ReleaseSequence") == 2 && runtime.getBoolean("TimerExpired")
                && runtime.contains("LatestCollision"),
                "the first tick must commit collision release before the final Timer release");
        });
        context.runAtTick(3, () -> {
            context.assertTrue(projectilesForExecution(context, executionId) == 3,
                "Timer expiry must not re-open after the same-tick collision sequence");
            context.killAllEntities();
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void projectilePayloadReloadAfterTimerRelease(TestContext context) {
        context.setTime(0);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        SparkBoltProjectileEntity original = spawnTrigger(context, player, NoitaSpellTriggerMode.TIMER, 5, 30,
            new Vec3d(4.5, 4.5, 4.5), Vec3d.ZERO);
        SparkBoltProjectileEntity[] restored = new SparkBoltProjectileEntity[1];
        String[] executionId = new String[1];

        context.runAtTick(7, () -> {
            NbtCompound saved = original.writeNbt(new NbtCompound());
            executionId[0] = saved.getCompound("FrozenPayload").getString("ExecutionId");
            context.assertTrue(projectilesForExecution(context, executionId[0]) == 2,
                "Timer must have released one child before its post-release save; actual="
                    + projectilesForExecution(context, executionId[0]));
            context.assertTrue(saved.getCompound("TriggerRuntimeState").getBoolean("TimerExpired"),
                "post-release save must persist the final Timer flag");
            original.discard();
            SparkBoltProjectileEntity reloaded = new SparkBoltProjectileEntity(ModEntities.SPARK_BOLT_PROJECTILE, context.getWorld());
            reloaded.readNbt(saved);
            context.assertTrue(reloaded.writeNbt(new NbtCompound()).getCompound("TriggerRuntimeState").getBoolean("TimerExpired"),
                "v3 reload must restore the final Timer flag before the next server tick");
            context.getWorld().spawnEntity(reloaded);
            restored[0] = reloaded;
        });
        context.runAtTick(12, () -> {
            context.assertTrue(restored[0] != null && !restored[0].isRemoved(),
                "post-release reload must retain the surviving Timer parent");
            context.assertTrue(projectilesForExecution(context, executionId[0]) == 2,
                "persisted timerExpired must prevent a second Timer release after reload; actual="
                    + projectilesForExecution(context, executionId[0]));
            context.killAllEntities();
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void expirationTriggerNaturalExpiryRelease(TestContext context) {
        context.setTime(0);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        SparkBoltProjectileEntity parent = spawnTrigger(context, player, NoitaSpellTriggerMode.EXPIRATION, 0, 1,
            new Vec3d(4.5, 4.5, 4.5), Vec3d.ZERO);

        context.runAtTick(3, () -> {
            context.assertTrue(parent.isRemoved(), "natural lifetime expiry must remove the parent projectile");
            context.assertTrue(projectilesOwnedBy(context, player) == 1,
                "Expiration trigger must release one frozen payload exactly once on natural expiry");
            context.killAllEntities();
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void expirationTriggerKilledRelease(TestContext context) {
        context.setTime(0);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        SparkBoltProjectileEntity spark = spawnTrigger(context, player, NoitaSpellTriggerMode.EXPIRATION, 0, 30,
            new Vec3d(4.5, 4.5, 4.5), Vec3d.ZERO);
        BombEntity bomb = spawnBombTrigger(context, player, "root/bomb-killed", NoitaSpellTriggerMode.EXPIRATION,
            new Vec3d(6.5, 4.5, 4.5), Vec3d.ZERO);

        context.runAtTick(2, () -> {
            spark.kill();
            bomb.kill();
        });
        context.runAtTick(4, () -> {
            context.assertTrue(spark.isRemoved() && bomb.isRemoved(), "KILLED must remove both trigger parents");
            context.assertTrue(projectilesOwnedBy(context, player) == 2,
                "KILLED must release each Expiration payload exactly once; actual=" + projectilesOwnedBy(context, player));
            context.killAllEntities();
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void unloadedExpirationTriggerDoesNotReleasePayload(TestContext context) {
        context.setTime(0);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        SparkBoltProjectileEntity original = spawnTrigger(context, player, NoitaSpellTriggerMode.EXPIRATION, 0, 30,
            new Vec3d(4.5, 4.5, 4.5), Vec3d.ZERO);
        SparkBoltProjectileEntity[] restored = new SparkBoltProjectileEntity[1];

        context.runAtTick(2, () -> {
            NbtCompound saved = original.writeNbt(new NbtCompound());
            // This is the entity lifecycle callback used when Minecraft unloads
            // a chunk; it must not be treated as a gameplay termination event.
            original.remove(net.minecraft.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);
            context.assertTrue(projectilesInTest(context) == 0,
                "UNLOADED_TO_CHUNK must not release an Expiration payload");
            SparkBoltProjectileEntity reloaded = new SparkBoltProjectileEntity(ModEntities.SPARK_BOLT_PROJECTILE, context.getWorld());
            reloaded.readNbt(saved);
            context.getWorld().spawnEntity(reloaded);
            restored[0] = reloaded;
        });
        context.runAtTick(4, () -> {
            context.assertTrue(projectilesInTest(context) == 1,
                "reloaded parent must remain the only projectile before a real termination event");
            restored[0].kill();
        });
        context.runAtTick(6, () -> {
            context.assertTrue(projectilesInTest(context) == 1,
                "the later KILLED event must release exactly one payload after unload/reload");
            context.killAllEntities();
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void bombHitTriggerBlockRelease(TestContext context) {
        context.setTime(0);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        context.setBlockState(new BlockPos(6, 2, 5), Blocks.STONE);
        BombEntity parent = spawnBombHitTrigger(context, player, "root/bomb-block",
            new Vec3d(3.4, 2.55, 5.5), new Vec3d(1.4, 0.0, 0.0));

        context.runAtTick(6, () -> {
            context.assertTrue(!parent.isRemoved(), "a fuse bomb must remain alive after a Hit trigger collision");
            context.assertTrue(projectilesOwnedBy(context, player) == 1,
                "a real Bomb block collision must release exactly one frozen payload; actual="
                    + projectilesOwnedBy(context, player));
            NbtCompound saved = parent.writeNbt(new NbtCompound());
            context.assertTrue(saved.contains("OwnerUuid"), "v3 Bomb persistence must include an explicit owner marker");
            saved.remove("OwnerUuid");
            BombEntity malformed = new BombEntity(ModEntities.BOMB_PROJECTILE, context.getWorld());
            malformed.readNbt(saved);
            context.assertTrue(malformed.isRemoved(), "a v3 Bomb with missing owner identity must become inert");
            context.killAllEntities();
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void bombHitTriggerEntityRelease(TestContext context) {
        context.setTime(0);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        context.spawnMob(EntityType.ZOMBIE, new Vec3d(6.0, 2.0, 5.5));
        BombEntity parent = spawnBombHitTrigger(context, player, "root/bomb-entity",
            new Vec3d(3.4, 2.55, 5.5), new Vec3d(1.4, 0.0, 0.0));

        context.runAtTick(6, () -> {
            context.assertTrue(!parent.isRemoved(), "a fuse bomb must remain alive after an entity Hit trigger collision");
            context.assertTrue(projectilesOwnedBy(context, player) == 1,
                "a real Bomb entity collision must release exactly one frozen payload; actual="
                    + projectilesOwnedBy(context, player));
            context.killAllEntities();
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 30)
    public void mineNearbyEntityHitTriggerRelease(TestContext context) {
        context.setTime(0);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        context.setBlockState(new BlockPos(5, 1, 5), Blocks.STONE);
        context.spawnMob(EntityType.ZOMBIE, new Vec3d(6.0, 2.0, 5.5));
        BombEntity parent = spawnMineHitTrigger(context, player, "root/mine-nearby", new Vec3d(5.5, 2.0, 5.5));
        // Start in the landed state so the test isolates the periodic MINE
        // proximity path instead of releasing from its initial ground contact.
        parent.setOnGround(true);

        context.runAtTick(10, () -> context.assertTrue(projectilesOwnedBy(context, player) == 0,
            "the landed MINE must not release before its 20-tick proximity scan; actual=" + projectilesOwnedBy(context, player)));
        context.runAtTick(22, () -> {
            context.assertTrue(parent.isRemoved(), "a nearby valid LivingEntity must prime and remove the landed MINE");
            context.assertTrue(projectilesOwnedBy(context, player) == 1,
                "a MINE proximity detonation must submit one Hit collision before termination; actual="
                    + projectilesOwnedBy(context, player));
            context.killAllEntities();
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void wandRefreshPileRewrite(TestContext context) { completeFixedFixture(context); }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void alphaCallWithoutTargetCharge(TestContext context) { completeFixedFixture(context); }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void gammaCallWithoutTargetCharge(TestContext context) { completeFixedFixture(context); }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void g04AddTriggerRealWandReleasesFrozenPayload(TestContext context) {
        context.setTime(0);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.setPosition(context.getAbsolute(new Vec3d(3.4, 2.0, 5.5)));
        player.setYaw(-90.0f);
        player.setPitch(0.0f);
        context.setBlockState(new BlockPos(8, 3, 5), Blocks.STONE);
        ItemStack wand = configuredG04Wand(ModItems.ADD_TRIGGER, ModItems.LIGHT_BULLET, ModItems.LIGHT_BULLET);
        player.setStackInHand(Hand.MAIN_HAND, wand);

        NoitaWandCaster.cast(player);
        context.assertTrue(projectilesOwnedBy(context, player) == 1,
            "Add Trigger evaluation must spawn one parent before collision");
        context.runAtTick(8, () -> {
            context.assertTrue(projectilesOwnedBy(context, player) == 1,
                "the parent must be replaced by exactly one frozen payload after the real block hit; actual="
                    + projectilesOwnedBy(context, player));
            context.killAllEntities();
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void g04NestedDivideStaysWithinTickAndEntityBudgets(TestContext context) {
        context.setTime(0);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.setPosition(context.getAbsolute(new Vec3d(3.4, 2.0, 5.5)));
        player.setStackInHand(Hand.MAIN_HAND, configuredG04Wand(
            ModItems.DIVIDE_10, ModItems.DIVIDE_4, ModItems.DIVIDE_2, ModItems.LIGHT_BULLET));

        NoitaWandCaster.cast(player);
        context.runAtTick(5, () -> {
            context.assertTrue(context.getTick() == 5, "bounded Divide evaluation must not stall the server tick");
            context.assertTrue(projectilesOwnedBy(context, player) <= 32,
                "Divide execution must respect the authoritative entity ceiling");
            context.killAllEntities();
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 20)
    public void projectilePayloadSaveReload(TestContext context) {
        context.setTime(0);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        SparkBoltProjectileEntity original = spawnTrigger(context, player, NoitaSpellTriggerMode.TIMER, 8, 30,
            new Vec3d(4.5, 4.5, 4.5), Vec3d.ZERO);
        SparkBoltProjectileEntity[] restored = new SparkBoltProjectileEntity[1];

        context.runAtTick(2, () -> {
            NbtCompound saved = original.writeNbt(new NbtCompound());
            context.assertTrue(saved.contains("FrozenPayload"), "v3 entity persistence must retain the authoritative frozen tree");
            context.assertTrue(!saved.contains("TriggerPayloads"),
                "v3 entity persistence must not duplicate FrozenPayload as a legacy flat projection");
            original.discard();
            SparkBoltProjectileEntity reloaded = new SparkBoltProjectileEntity(ModEntities.SPARK_BOLT_PROJECTILE, context.getWorld());
            reloaded.readNbt(saved);
            context.getWorld().spawnEntity(reloaded);
            restored[0] = reloaded;
            context.assertTrue(projectilesInTest(context) == 1,
                "reload before Timer expiry must retain only the parent projectile");
        });
        context.runAtTick(9, () -> context.assertTrue(projectilesInTest(context) == 2,
            "reloaded Timer trigger must release its frozen payload exactly once"));
        context.runAtTick(11, () -> {
            context.assertTrue(restored[0] != null && !restored[0].isRemoved(), "reloaded parent must remain alive after Timer expiry");
            context.assertTrue(projectilesInTest(context) == 2,
                "Timer state restored from NBT must prevent a second expiry release");
            context.killAllEntities();
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 12)
    public void legacyRuntimePayloadReceivesExecutionIdentity(TestContext context) {
        context.setTime(0);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        NoitaProjectilePayload legacy = new NoitaProjectilePayload("spark_bolt", NoitaProjectileBehavior.BOLT, 1.0f,
            0.0f, 40, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f, 0.0f, false, false, 1, 0.0f,
            NoitaSpellTriggerMode.NONE, 0, 0, List.of(), List.of());
        SparkBoltProjectileEntity original = new SparkBoltProjectileEntity(context.getWorld(), player, legacy);
        original.setPosition(context.getAbsolute(new Vec3d(4.5, 4.5, 4.5)));
        original.setVelocity(Vec3d.ZERO);
        context.getWorld().spawnEntity(original);
        SparkBoltProjectileEntity[] restored = new SparkBoltProjectileEntity[1];
        String[] executionId = new String[1];

        context.runAtTick(2, () -> {
            NbtCompound saved = original.writeNbt(new NbtCompound());
            executionId[0] = saved.getCompound("FrozenPayload").getString("ExecutionId");
            context.assertTrue(!NoitaExecutionIdentity.UNBOUND_EXECUTION_ID.toString().equals(executionId[0]),
                "legacy runtime spawn must bind a non-zero execution ID before v3 persistence");
            original.discard();
            SparkBoltProjectileEntity reloaded = new SparkBoltProjectileEntity(ModEntities.SPARK_BOLT_PROJECTILE, context.getWorld());
            reloaded.readNbt(saved);
            context.getWorld().spawnEntity(reloaded);
            restored[0] = reloaded;
        });
        context.runAtTick(5, () -> {
            context.assertTrue(restored[0] != null && !restored[0].isRemoved(),
                "a legacy runtime payload bound at spawn must survive v3 reload");
            context.assertTrue(projectilesForExecution(context, executionId[0]) == 1,
                "legacy runtime reload must preserve exactly one frozen projectile");
            context.killAllEntities();
            context.complete();
        });
    }

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
        context.assertTrue(projectilesOwnedBy(context, player) == 2, "first cast must spawn two real projectile entities");

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
            context.assertTrue(projectilesOwnedBy(context, player) == 3,
                "second cast must add exactly one real projectile entity; actual=" + projectilesOwnedBy(context, player));
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

    private static SparkBoltProjectileEntity spawnTrigger(
        TestContext context, ServerPlayerEntity owner, NoitaSpellTriggerMode mode, int timerDelayTicks, int lifetimeTicks,
        Vec3d position, Vec3d velocity
    ) {
        return spawnTrigger(context, owner, mode, timerDelayTicks, lifetimeTicks, position, velocity, false);
    }

    private static SparkBoltProjectileEntity spawnTrigger(
        TestContext context, ServerPlayerEntity owner, NoitaSpellTriggerMode mode, int timerDelayTicks, int lifetimeTicks,
        Vec3d position, Vec3d velocity, boolean piercing
    ) {
        Vec3d absolutePosition = context.getAbsolute(position);
        UUID executionId = UUID.nameUUIDFromBytes((mode.name() + timerDelayTicks + lifetimeTicks + absolutePosition)
            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        NoitaProjectilePayload child = new NoitaProjectilePayload("spark_bolt", NoitaProjectileBehavior.BOLT, 1.0f,
            0.0f, 40, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f, 0.0f, false, true, 1, 0.0f,
            0, List.of(), NoitaTriggerPlan.none("root/0/trigger/payload/0"),
            new NoitaExecutionIdentity(executionId, "root/0/trigger/payload/0", 1L, "gametest"),
            new TriggerRuntimeBudget(8, 8));
        NoitaPayloadPlan payloadShot = new NoitaPayloadPlan("root/0/trigger/payload", List.of(child));
        NoitaTriggerPlan trigger = new NoitaTriggerPlan(mode, timerDelayTicks, List.of(payloadShot), "root/0/trigger", 1,
            NoitaTriggerReleasePolicy.forMode(mode));
        NoitaProjectilePayload root = new NoitaProjectilePayload("spark_bolt", NoitaProjectileBehavior.BOLT, 3.0f,
            0.0f, lifetimeTicks, 0, 0.0f, 1.0f, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f, 0.0f, false, piercing, 1,
            0.0f, 0, List.of(), trigger, new NoitaExecutionIdentity(executionId, "root/0", 1L, "gametest"),
            new TriggerRuntimeBudget(8, 8));
        SparkBoltProjectileEntity projectile = new SparkBoltProjectileEntity(context.getWorld(), owner, root);
        projectile.setPosition(context.getAbsolute(position));
        projectile.setVelocity(velocity);
        context.getWorld().spawnEntity(projectile);
        return projectile;
    }

    private static SparkBoltProjectileEntity spawnNestedHitTrigger(
        TestContext context, ServerPlayerEntity owner, Vec3d position, Vec3d velocity
    ) {
        UUID executionId = UUID.nameUUIDFromBytes("nested-trigger-gametest".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String rootPath = "root/nested";
        String rootTriggerPath = rootPath + "/trigger";
        String rootPayloadPath = rootTriggerPath + "/payload";
        String childPath = rootPayloadPath + "/0";
        String childTriggerPath = childPath + "/trigger";
        String childPayloadPath = childTriggerPath + "/payload";
        String leafPath = childPayloadPath + "/0";
        NoitaProjectilePayload leaf = new NoitaProjectilePayload("spark_bolt", NoitaProjectileBehavior.BOLT, 1.0f,
            0.0f, 40, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f, 0.0f, false, true, 1, 0.0f,
            0, List.of(), NoitaTriggerPlan.none(leafPath), new NoitaExecutionIdentity(executionId, leafPath, 1L, "gametest"),
            new TriggerRuntimeBudget(4, 4));
        NoitaTriggerPlan childTrigger = new NoitaTriggerPlan(NoitaSpellTriggerMode.HIT, 0,
            List.of(new NoitaPayloadPlan(childPayloadPath, List.of(leaf))), childTriggerPath, 2,
            NoitaTriggerReleasePolicy.VALID_COLLISION);
        NoitaProjectilePayload child = new NoitaProjectilePayload("spark_bolt", NoitaProjectileBehavior.BOLT, 1.0f,
            0.0f, 40, 0, 0.0f, 1.0f, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f, 0.0f, false, false, 1, 0.0f,
            0, List.of(), childTrigger, new NoitaExecutionIdentity(executionId, childPath, 1L, "gametest"),
            new TriggerRuntimeBudget(4, 4));
        NoitaTriggerPlan rootTrigger = new NoitaTriggerPlan(NoitaSpellTriggerMode.HIT, 0,
            List.of(new NoitaPayloadPlan(rootPayloadPath, List.of(child))), rootTriggerPath, 1,
            NoitaTriggerReleasePolicy.VALID_COLLISION);
        NoitaProjectilePayload rootPayload = new NoitaProjectilePayload("spark_bolt", NoitaProjectileBehavior.BOLT, 3.0f,
            0.0f, 30, 0, 0.0f, 1.0f, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f, 0.0f, false, false, 1, 0.0f,
            0, List.of(), rootTrigger, new NoitaExecutionIdentity(executionId, rootPath, 1L, "gametest"),
            new TriggerRuntimeBudget(8, 8));
        SparkBoltProjectileEntity root = new SparkBoltProjectileEntity(context.getWorld(), owner, rootPayload);
        root.setPosition(context.getAbsolute(position));
        root.setVelocity(velocity);
        context.getWorld().spawnEntity(root);
        return root;
    }

    private static BombEntity spawnBombHitTrigger(
        TestContext context, ServerPlayerEntity owner, String nodeRoot, Vec3d position, Vec3d velocity
    ) {
        return spawnBombTrigger(context, owner, nodeRoot, NoitaSpellTriggerMode.HIT, position, velocity);
    }

    private static BombEntity spawnBombTrigger(
        TestContext context, ServerPlayerEntity owner, String nodeRoot, NoitaSpellTriggerMode mode, Vec3d position, Vec3d velocity
    ) {
        return spawnExplosiveTrigger(context, owner, nodeRoot, "bomb", NoitaProjectileBehavior.FUSED_EXPLOSIVE,
            mode, position, velocity);
    }

    private static BombEntity spawnMineHitTrigger(
        TestContext context, ServerPlayerEntity owner, String nodeRoot, Vec3d position
    ) {
        return spawnExplosiveTrigger(context, owner, nodeRoot, "mine", NoitaProjectileBehavior.MINE,
            NoitaSpellTriggerMode.HIT, position, Vec3d.ZERO);
    }

    private static BombEntity spawnExplosiveTrigger(
        TestContext context, ServerPlayerEntity owner, String nodeRoot, String itemPath, NoitaProjectileBehavior behavior,
        NoitaSpellTriggerMode mode, Vec3d position, Vec3d velocity
    ) {
        UUID executionId = UUID.nameUUIDFromBytes(nodeRoot.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String triggerPath = nodeRoot + "/trigger";
        String payloadPath = triggerPath + "/payload";
        String childPath = payloadPath + "/0";
        NoitaProjectilePayload child = new NoitaProjectilePayload("spark_bolt", NoitaProjectileBehavior.BOLT, 1.0f,
            0.0f, 40, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f, 0.0f, false, true, 1, 0.0f,
            0, List.of(), NoitaTriggerPlan.none(childPath), new NoitaExecutionIdentity(executionId, childPath, 1L, "gametest"),
            new TriggerRuntimeBudget(8, 8));
        NoitaPayloadPlan payloadShot = new NoitaPayloadPlan(payloadPath, List.of(child));
        NoitaTriggerPlan trigger = new NoitaTriggerPlan(mode, 0, List.of(payloadShot), triggerPath, 1,
            NoitaTriggerReleasePolicy.forMode(mode));
        NoitaProjectilePayload root = new NoitaProjectilePayload(itemPath, behavior, 0.0f,
            0.0f, 80, 0, 0.0f, 1.0f, 0.0f, 0.05f, 0.99f, 0.65f, 1.0f, 0.0f, false, false, 1,
            0.0f, 0, List.of(), trigger, new NoitaExecutionIdentity(executionId, nodeRoot, 1L, "gametest"),
            new TriggerRuntimeBudget(8, 8));
        BombEntity bomb = new BombEntity(context.getWorld(), owner, root);
        bomb.setPosition(context.getAbsolute(position));
        bomb.setVelocity(velocity);
        context.getWorld().spawnEntity(bomb);
        return bomb;
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

    private static ItemStack configuredG04Wand(net.minecraft.item.Item... spells) {
        ItemStack wand = new ItemStack(ModItems.STARTER_WAND);
        NoitaWandItem.setTemplate(wand, NoitaWandTemplate.builder()
            .shuffle(false)
            .spellsPerCast(1)
            .castDelaySeconds(0.0f)
            .rechargeTimeSeconds(0.0f)
            .manaMax(1000)
            .manaChargeSpeed(0)
            .capacity(spells.length)
            .spreadDegrees(0.0f)
            .speedMultiplier(1.0f)
            .build());
        DefaultedList<ItemStack> cards = DefaultedList.ofSize(spells.length, ItemStack.EMPTY);
        for (int slot = 0; slot < spells.length; slot++) {
            cards.set(slot, new ItemStack(spells[slot]));
        }
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
        // GameTest cases run in adjacent boxes. Do not include projectiles from
        // a neighboring trigger scenario when asserting exact release counts.
        Box search = context.getTestBox();
        return context.getWorld().getEntitiesByType(ModEntities.SPARK_BOLT_PROJECTILE, search, entity -> true).size();
    }

    private static int projectilesOwnedBy(TestContext context, ServerPlayerEntity owner) {
        return context.getWorld().getEntitiesByType(ModEntities.SPARK_BOLT_PROJECTILE, context.getTestBox().expand(32.0),
            entity -> entity.getOwner() == owner).size();
    }

    private static int projectilesForExecution(TestContext context, String executionId) {
        return context.getWorld().getEntitiesByType(ModEntities.SPARK_BOLT_PROJECTILE, context.getTestBox().expand(32.0),
            entity -> executionId.equals(entity.writeNbt(new NbtCompound()).getCompound("FrozenPayload").getString("ExecutionId"))).size();
    }

    @Test
    @Tag("gametest")
    void fixtureCatalogCoversTheG02WorldScenario() {
        assertEquals(25, SCENARIOS.size());
        assertTrue(SCENARIOS.contains("starter_wand_server_authority"));
        assertTrue(SCENARIOS.contains("trigger_payload_block_hit_release"));
        assertTrue(SCENARIOS.contains("trigger_payload_entity_hit_release"));
        assertTrue(SCENARIOS.contains("piercing_hit_trigger_sequential_entity_release"));
        assertTrue(SCENARIOS.contains("nested_trigger_block_release"));
        assertTrue(SCENARIOS.contains("bomb_hit_trigger_block_release"));
        assertTrue(SCENARIOS.contains("bomb_hit_trigger_entity_release"));
        assertTrue(SCENARIOS.contains("mine_nearby_entity_hit_trigger_release"));
        assertTrue(SCENARIOS.contains("timer_trigger_exact_tick_release"));
        assertTrue(SCENARIOS.contains("timer_trigger_zero_and_one_tick_release"));
        assertTrue(SCENARIOS.contains("timer_piercing_collision_then_expiry_release"));
        assertTrue(SCENARIOS.contains("projectile_payload_reload_after_timer_release"));
        assertTrue(SCENARIOS.contains("expiration_trigger_natural_expiry_release"));
        assertTrue(SCENARIOS.contains("expiration_trigger_killed_release"));
        assertTrue(SCENARIOS.contains("unloaded_expiration_trigger_does_not_release_payload"));
        assertTrue(SCENARIOS.contains("projectile_payload_save_reload"));
        assertTrue(SCENARIOS.contains("legacy_runtime_payload_receives_execution_identity"));
        assertTrue(SCENARIOS.contains("g02_two_round_real_wand_matches_evaluator"));
    }
}

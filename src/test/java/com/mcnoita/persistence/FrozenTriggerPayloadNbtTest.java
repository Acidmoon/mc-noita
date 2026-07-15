package com.mcnoita.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcnoita.spell.NoitaExecutionIdentity;
import com.mcnoita.spell.NoitaModifierEffect;
import com.mcnoita.spell.NoitaPayloadPlan;
import com.mcnoita.spell.NoitaProjectileBehavior;
import com.mcnoita.spell.NoitaProjectilePayload;
import com.mcnoita.spell.NoitaSpellTriggerMode;
import com.mcnoita.spell.NoitaTriggerPlan;
import com.mcnoita.spell.NoitaTriggerReleasePolicy;
import com.mcnoita.spell.damage.DamageChannel;
import com.mcnoita.spell.damage.DamageProfile;
import com.mcnoita.spell.trigger.CollisionKey;
import com.mcnoita.spell.trigger.TriggerRuntimeBudget;
import com.mcnoita.spell.trigger.TriggerRuntimeState;
import com.mcnoita.spell.trigger.TriggerRuntimeStateNbtCodec;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("regression")
class FrozenTriggerPayloadNbtTest {
    @Test
    void frozenNestedTreeRoundTripsWithIdentityAndMechanicalData() {
        NoitaProjectilePayload original = nestedPayload();
        NbtCompound encoded = original.toNbt();

        assertEquals(NoitaNbtSchema.CURRENT_VERSION, encoded.getInt(NoitaNbtSchema.VERSION_KEY));
        assertEquals(original, NoitaProjectilePayload.tryFromNbt(encoded).orElseThrow());
    }

    @Test
    void configuredRuntimeBudgetAboveTheLegacyDefaultSurvivesFrozenNbtRoundTrip() {
        TriggerRuntimeBudget configured = new TriggerRuntimeBudget(64, 64);
        NoitaProjectilePayload original = nestedPayload().withRuntimeBudget(configured);

        NoitaProjectilePayload decoded = NoitaProjectilePayload.tryFromNbt(original.toNbt()).orElseThrow();

        assertEquals(configured, decoded.runtimeBudget());
    }

    @Test
    void dynamicLegacyChildDerivesABoundUniqueIdentityBeforeWorldAdmission() {
        NoitaProjectilePayload parentPayload = nestedPayload();
        NoitaExecutionIdentity parent = parentPayload.executionIdentity();

        NoitaProjectilePayload first = legacyPayload(List.of()).withDerivedRuntimeIdentity(parent, "secondary", 0);
        NoitaProjectilePayload second = legacyPayload(List.of()).withDerivedRuntimeIdentity(parent, "secondary", 1);

        assertTrue(first.executionIdentity().isBound());
        assertEquals(parent.executionId(), first.executionIdentity().executionId());
        assertEquals(parent.catalogEpoch(), first.executionIdentity().catalogEpoch());
        assertEquals(parent.catalogHash(), first.executionIdentity().catalogHash());
        assertFalse(first.executionIdentity().nodePath().equals(second.executionIdentity().nodePath()));
    }

    @Test
    void maximumSemanticPayloadDepthRoundTripsDespiteItsLargerNbtStructure() {
        UUID executionId = UUID.fromString("e0123456-789a-4bcd-8ef0-123456789abc");
        NoitaProjectilePayload original = payloadTree(0, NoitaNbtLimits.MAX_PAYLOAD_DEPTH, "root/0", executionId);

        NbtCompound encoded = original.toNbt();
        var decoded = NoitaProjectilePayload.tryFromNbt(encoded);
        assertTrue(decoded.isPresent(), () -> "valid depth-" + NoitaNbtLimits.MAX_PAYLOAD_DEPTH
            + " tree was rejected at " + encoded.getSizeInBytes() + " bytes");
        assertEquals(original, decoded.orElseThrow());
    }

    @Test
    void currentSchemaRejectsMissingFrozenFieldsInsteadOfDroppingPayloadState() {
        NbtCompound missingRootPlan = nestedPayload().toNbt();
        missingRootPlan.remove("TriggerPlan");
        assertTrue(NoitaProjectilePayload.tryFromNbt(missingRootPlan).isEmpty());

        NbtCompound missingNestedPlan = nestedPayload().toNbt();
        NbtCompound child = missingNestedPlan.getCompound("TriggerPlan").getList("Payloads", NbtElement.COMPOUND_TYPE)
            .getCompound(0).getList("Projectiles", NbtElement.COMPOUND_TYPE).getCompound(0);
        child.remove("TriggerPlan");
        assertTrue(NoitaProjectilePayload.tryFromNbt(missingNestedPlan).isEmpty());

        NbtCompound missingBudget = nestedPayload().toNbt();
        missingBudget.remove("RuntimeBudget");
        assertTrue(NoitaProjectilePayload.tryFromNbt(missingBudget).isEmpty());

        NbtCompound missingDamageProfile = nestedPayload().toNbt();
        missingDamageProfile.remove(DamageProfileNbtCodec.PROFILE_KEY);
        assertTrue(NoitaProjectilePayload.tryFromNbt(missingDamageProfile).isEmpty());

        NbtCompound unboundExecution = nestedPayload().toNbt();
        unboundExecution.putString("ExecutionId", NoitaExecutionIdentity.UNBOUND_EXECUTION_ID.toString());
        assertTrue(NoitaProjectilePayload.tryFromNbt(unboundExecution).isEmpty());
    }

    @Test
    void logicalPayloadLimitDoesNotCountPlanBookkeepingNodes() {
        UUID executionId = UUID.fromString("e0123456-789a-4bcd-8ef0-123456789abc");
        List<NoitaProjectilePayload> roots = java.util.stream.IntStream.range(0, 32)
            .mapToObj(index -> payloadTree(0, 2, "root/" + index, executionId))
            .toList();

        assertEquals(roots, NoitaProjectilePayload.tryFromNbtList(NoitaProjectilePayload.toNbtList(roots)).orElseThrow());
    }

    @Test
    void malformedPathsAndOversizedMechanicalNumbersAreSafelyRejectedOrBounded() {
        NbtCompound invalidPath = nestedPayload().toNbt();
        invalidPath.putString("ItemPath", "mc-noita:bad path");
        assertTrue(NoitaProjectilePayload.tryFromNbt(invalidPath).isEmpty());

        NbtCompound oversizedRadius = nestedPayload().toNbt();
        oversizedRadius.putDouble("ExplosionRadius", Double.MAX_VALUE);
        NoitaProjectilePayload decoded = NoitaProjectilePayload.tryFromNbt(oversizedRadius).orElseThrow();
        assertEquals(NoitaNbtLimits.MAX_EXPLOSION_RADIUS, decoded.explosionRadius());
    }

    @Test
    void scalarPayloadsMapOnlyToProjectileWhileProfilesRoundTripAllFrozenChannels() {
        NoitaProjectilePayload legacy = legacyPayload(List.of());
        assertEquals(1.0, legacy.damageProfile().amount(DamageChannel.PROJECTILE));
        assertEquals(0.0, legacy.damageProfile().amount(DamageChannel.FIRE));

        NoitaProjectilePayload base = nestedPayload();
        DamageProfile profile = new DamageProfile(Map.of(
            DamageChannel.PROJECTILE, 2.5,
            DamageChannel.FIRE, 1.25,
            DamageChannel.HOLY, 0.5
        ));
        NoitaProjectilePayload multiChannel = new NoitaProjectilePayload(
            base.itemPath(), base.behavior(), profile, base.criticalChancePercent(), base.lifetimeTicks(),
            base.trailLightStacks(), base.explosionRadius(), base.speed(), base.divergence(), base.gravity(), base.drag(),
            base.bounceDamping(), base.renderScale(), base.knockbackForce(), base.friendlyFire(), base.piercing(),
            base.projectileCount(), base.burstSpreadDegrees(), base.bounceCount(), base.modifierEffects(),
            base.triggerPlan(), base.executionIdentity(), base.runtimeBudget()
        );

        NoitaProjectilePayload decoded = NoitaProjectilePayload.tryFromNbt(multiChannel.toNbt()).orElseThrow();
        assertEquals(profile, decoded.damageProfile());
        assertEquals(2.5f, decoded.damage());
    }

    @Test
    void v3ScalarPayloadMigratesToProjectileProfileAndMalformedV4ProfilesAreRejected() {
        NbtCompound v3 = nestedPayload().toNbt();
        downgradePayloadTreeToV3(v3);
        v3.putFloat("Damage", 6.5f);

        NoitaProjectilePayload migrated = NoitaProjectilePayload.tryFromNbt(v3).orElseThrow();
        assertEquals(6.5, migrated.damageProfile().amount(DamageChannel.PROJECTILE));
        assertEquals(0.0, migrated.damageProfile().amount(DamageChannel.ELECTRICITY));
        assertEquals(1.0, migrated.triggerPlan().payloads().get(0).projectiles().get(0)
            .damageProfile().amount(DamageChannel.PROJECTILE));

        NbtCompound unknownChannel = nestedPayload().toNbt();
        unknownChannel.getCompound(DamageProfileNbtCodec.PROFILE_KEY).putDouble("NOT_A_CHANNEL", 1.0);
        assertTrue(NoitaProjectilePayload.tryFromNbt(unknownChannel).isEmpty());

        NbtCompound nonFiniteChannel = nestedPayload().toNbt();
        nonFiniteChannel.getCompound(DamageProfileNbtCodec.PROFILE_KEY).putDouble(DamageChannel.FIRE.name(), Double.NaN);
        assertTrue(NoitaProjectilePayload.tryFromNbt(nonFiniteChannel).isEmpty());

        NbtCompound oversizedChannel = nestedPayload().toNbt();
        oversizedChannel.getCompound(DamageProfileNbtCodec.PROFILE_KEY).putDouble(DamageChannel.FIRE.name(),
            NoitaNbtLimits.MAX_ABSOLUTE_PROJECTILE_DAMAGE + 1.0);
        assertTrue(NoitaProjectilePayload.tryFromNbt(oversizedChannel).isEmpty());

        NbtCompound mismatchedMirror = nestedPayload().toNbt();
        mismatchedMirror.putFloat("Damage", 9.0f);
        assertTrue(NoitaProjectilePayload.tryFromNbt(mismatchedMirror).isEmpty());

        NbtCompound nestedMismatchedMirror = nestedPayload().toNbt();
        NbtCompound nestedChild = nestedMismatchedMirror.getCompound("TriggerPlan")
            .getList("Payloads", NbtElement.COMPOUND_TYPE).getCompound(0)
            .getList("Projectiles", NbtElement.COMPOUND_TYPE).getCompound(0);
        nestedChild.putFloat("Damage", 9.0f);
        assertTrue(NoitaProjectilePayload.tryFromNbt(nestedMismatchedMirror).isEmpty());

        NbtCompound nonFiniteMirror = nestedPayload().toNbt();
        nonFiniteMirror.putFloat("Damage", Float.NaN);
        assertTrue(NoitaProjectilePayload.tryFromNbt(nonFiniteMirror).isEmpty());
    }

    @Test
    void maximumModifierEffectListRoundTripsAndOversizedListsAreRejectedBeforePersistence() {
        NbtCompound encoded = nestedPayload().toNbt();
        encoded.put("ModifierEffects", effectNbtList(NoitaNbtLimits.MAX_MODIFIER_EFFECTS));

        NoitaProjectilePayload decoded = NoitaProjectilePayload.tryFromNbt(encoded).orElseThrow();
        assertEquals(NoitaNbtLimits.MAX_MODIFIER_EFFECTS, decoded.modifierEffects().size());
        assertEquals(decoded, NoitaProjectilePayload.tryFromNbt(decoded.toNbt()).orElseThrow());

        NbtCompound oversized = nestedPayload().toNbt();
        oversized.put("ModifierEffects", effectNbtList(NoitaNbtLimits.MAX_MODIFIER_EFFECTS + 1));
        assertTrue(NoitaProjectilePayload.tryFromNbt(oversized).isEmpty());

        List<NoitaModifierEffect> tooManyEffects = java.util.Collections.nCopies(
            NoitaNbtLimits.MAX_MODIFIER_EFFECTS + 1, NoitaModifierEffect.values()[0]);
        assertThrows(IllegalArgumentException.class,
            () -> leaf("root/too-many-effects", UUID.randomUUID(), tooManyEffects));
    }

    @Test
    void malformedNestedChildRejectsTheEntireFrozenTree() {
        NbtCompound encoded = nestedPayload().toNbt();
        NbtCompound trigger = encoded.getCompound("TriggerPlan");
        NbtCompound payloadShot = trigger.getList("Payloads", NbtElement.COMPOUND_TYPE).getCompound(0);
        payloadShot.getList("Projectiles", NbtElement.COMPOUND_TYPE).getCompound(0).putString("Behavior", "NOT_A_BEHAVIOR");

        assertTrue(NoitaProjectilePayload.tryFromNbt(encoded).isEmpty());
    }

    @Test
    void duplicateNodePathAndUnsupportedNestedSchemaAreRejected() {
        NbtCompound duplicate = nestedPayload().toNbt();
        NbtCompound trigger = duplicate.getCompound("TriggerPlan");
        NbtCompound child = trigger.getList("Payloads", NbtElement.COMPOUND_TYPE).getCompound(0)
            .getList("Projectiles", NbtElement.COMPOUND_TYPE).getCompound(0);
        child.putString("NodePath", "root/0");
        assertTrue(NoitaProjectilePayload.tryFromNbt(duplicate).isEmpty());

        NbtCompound futureChild = nestedPayload().toNbt();
        NbtCompound future = futureChild.getCompound("TriggerPlan").getList("Payloads", NbtElement.COMPOUND_TYPE)
            .getCompound(0).getList("Projectiles", NbtElement.COMPOUND_TYPE).getCompound(0);
        future.putInt(NoitaNbtSchema.VERSION_KEY, NoitaNbtSchema.CURRENT_VERSION + 1);
        assertTrue(NoitaProjectilePayload.tryFromNbt(futureChild).isEmpty());

        NbtCompound downgradedChild = nestedPayload().toNbt();
        NbtCompound legacyChild = downgradedChild.getCompound("TriggerPlan").getList("Payloads", NbtElement.COMPOUND_TYPE)
            .getCompound(0).getList("Projectiles", NbtElement.COMPOUND_TYPE).getCompound(0);
        legacyChild.putInt(NoitaNbtSchema.VERSION_KEY, NoitaNbtSchema.CURRENT_VERSION - 1);
        assertTrue(NoitaProjectilePayload.tryFromNbt(downgradedChild).isEmpty(),
            "a v3 root must reject a legacy child instead of silently applying migrated defaults");

        NbtCompound mixedIdentity = nestedPayload().toNbt();
        NbtCompound differentExecution = mixedIdentity.getCompound("TriggerPlan").getList("Payloads", NbtElement.COMPOUND_TYPE)
            .getCompound(0).getList("Projectiles", NbtElement.COMPOUND_TYPE).getCompound(0);
        differentExecution.putString("ExecutionId", UUID.randomUUID().toString());
        assertTrue(NoitaProjectilePayload.tryFromNbt(mixedIdentity).isEmpty(),
            "a frozen tree must not mix execution identities");

        NbtCompound mixedCatalog = nestedPayload().toNbt();
        NbtCompound differentCatalog = mixedCatalog.getCompound("TriggerPlan").getList("Payloads", NbtElement.COMPOUND_TYPE)
            .getCompound(0).getList("Projectiles", NbtElement.COMPOUND_TYPE).getCompound(0);
        differentCatalog.putString("CatalogHash", "different-catalog");
        assertTrue(NoitaProjectilePayload.tryFromNbt(mixedCatalog).isEmpty(),
            "a frozen tree must not mix catalog snapshots");
    }

    @Test
    void legacyDeathModeNormalizesToExpiration() {
        NoitaProjectilePayload payload = nestedPayload();
        NbtCompound legacy = payload.toNbt();
        legacy.putInt(NoitaNbtSchema.VERSION_KEY, 2);
        legacy.remove("TriggerPlan");
        legacy.putString("TriggerMode", "DEATH");
        legacy.putInt("TriggerDelayTicks", 0);
        legacy.put("Payloads", NoitaProjectilePayload.toNbtList(List.of(leaf("root/0/trigger/0"))));

        assertEquals(NoitaSpellTriggerMode.EXPIRATION,
            NoitaProjectilePayload.tryFromNbt(legacy).orElseThrow().triggerPlan().mode());
    }

    @Test
    void legacyRuntimeTreeGetsDistinctBoundPathsBeforeItIsWrittenAsV3() {
        NoitaProjectilePayload first = legacyPayload(List.of());
        NoitaProjectilePayload second = legacyPayload(List.of());
        NoitaProjectilePayload legacyRoot = new NoitaProjectilePayload(
            "spark_bolt", NoitaProjectileBehavior.BOLT, 2.0f, 0.0f, 20, 0, 0.0f,
            1.0f, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f, 0.0f, false, false,
            1, 0.0f, NoitaSpellTriggerMode.HIT, 0, 0, List.of(), List.of(first, second)
        );

        NoitaProjectilePayload bound = legacyRoot.withTreeIdentity(UUID.randomUUID(), 0L, "legacy-runtime");
        List<NoitaProjectilePayload> children = bound.triggerPlan().payloads().get(0).projectiles();

        assertTrue(bound.executionIdentity().isBound());
        assertEquals("root/trigger/0/0", children.get(0).executionIdentity().nodePath());
        assertEquals("root/trigger/0/1", children.get(1).executionIdentity().nodePath());
        assertEquals(bound, NoitaProjectilePayload.tryFromNbt(bound.toNbt()).orElseThrow());
    }

    @Test
    void runtimeStateRoundTripsBeforeAndAfterARelease() {
        TriggerRuntimeState state = new TriggerRuntimeState(3, true, false, false,
            new CollisionKey(41L, "entity:uuid", "ENTITY", "root/0/trigger"), new TriggerRuntimeBudget(5, 7));

        assertEquals(state, TriggerRuntimeStateNbtCodec.tryFromNbt(TriggerRuntimeStateNbtCodec.toNbt(state),
            TriggerRuntimeBudget.DEFAULT).orElseThrow());

        NbtCompound incompleteCurrentState = TriggerRuntimeStateNbtCodec.toNbt(state);
        incompleteCurrentState.remove("TimerExpired");
        assertTrue(TriggerRuntimeStateNbtCodec.tryFromCurrentNbt(incompleteCurrentState,
            TriggerRuntimeBudget.DEFAULT).isEmpty());
    }

    private static NoitaProjectilePayload nestedPayload() {
        UUID executionId = UUID.fromString("e0123456-789a-4bcd-8ef0-123456789abc");
        NoitaProjectilePayload leaf = leaf("root/0/trigger/payload/0", executionId);
        NoitaPayloadPlan payload = new NoitaPayloadPlan("root/0/trigger/payload", List.of(leaf));
        NoitaTriggerPlan trigger = new NoitaTriggerPlan(NoitaSpellTriggerMode.TIMER, 12, List.of(payload),
            "root/0/trigger", 1, NoitaTriggerReleasePolicy.VALID_COLLISION_AND_TIMER);
        return new NoitaProjectilePayload("spark_bolt", NoitaProjectileBehavior.BOLT, 3.0f, 5.0f, 30, 1,
            0.0f, 1.0f, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f, 0.0f, false, true, 1, 0.0f, 0,
            List.of(), trigger, new NoitaExecutionIdentity(executionId, "root/0", 44L, "catalog-hash"),
            new TriggerRuntimeBudget(8, 8));
    }

    private static NoitaProjectilePayload leaf(String path) {
        return leaf(path, UUID.fromString("e0123456-789a-4bcd-8ef0-123456789abc"));
    }

    private static NoitaProjectilePayload leaf(String path, UUID executionId) {
        return leaf(path, executionId, List.of());
    }

    private static NoitaProjectilePayload leaf(
        String path, UUID executionId, List<NoitaModifierEffect> modifierEffects
    ) {
        return new NoitaProjectilePayload("spark_bolt", NoitaProjectileBehavior.BOLT, 1.0f, 0.0f, 20, 0,
            0.0f, 1.0f, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f, 0.0f, false, false, 1, 0.0f, 0,
            modifierEffects, NoitaTriggerPlan.none(path), new NoitaExecutionIdentity(executionId, path, 44L, "catalog-hash"),
            new TriggerRuntimeBudget(4, 4));
    }

    private static NbtList effectNbtList(int count) {
        NbtList effects = new NbtList();
        String effectName = NoitaModifierEffect.values()[0].name();
        for (int index = 0; index < count; index++) {
            effects.add(net.minecraft.nbt.NbtString.of(effectName));
        }
        return effects;
    }

    private static void downgradePayloadTreeToV3(NbtCompound payload) {
        payload.putInt(NoitaNbtSchema.VERSION_KEY, 3);
        payload.remove(DamageProfileNbtCodec.PROFILE_KEY);
        NbtList payloadShots = payload.getCompound("TriggerPlan").getList("Payloads", NbtElement.COMPOUND_TYPE);
        for (int payloadIndex = 0; payloadIndex < payloadShots.size(); payloadIndex++) {
            NbtList projectiles = payloadShots.getCompound(payloadIndex).getList("Projectiles", NbtElement.COMPOUND_TYPE);
            for (int projectileIndex = 0; projectileIndex < projectiles.size(); projectileIndex++) {
                downgradePayloadTreeToV3(projectiles.getCompound(projectileIndex));
            }
        }
    }

    private static NoitaProjectilePayload payloadTree(int currentDepth, int maximumDepth, String nodePath, UUID executionId) {
        if (currentDepth == maximumDepth) {
            return leaf(nodePath, executionId);
        }
        String triggerPath = nodePath + "/trigger";
        String payloadPath = triggerPath + "/payload";
        NoitaProjectilePayload child = payloadTree(currentDepth + 1, maximumDepth, payloadPath + "/0", executionId);
        NoitaTriggerPlan trigger = new NoitaTriggerPlan(NoitaSpellTriggerMode.HIT, 0,
            List.of(new NoitaPayloadPlan(payloadPath, List.of(child))), triggerPath, currentDepth + 1,
            NoitaTriggerReleasePolicy.VALID_COLLISION);
        return new NoitaProjectilePayload("spark_bolt", NoitaProjectileBehavior.BOLT, 1.0f, 0.0f, 20, 0,
            0.0f, 1.0f, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f, 0.0f, false, true, 1, 0.0f, 0,
            List.of(), trigger, new NoitaExecutionIdentity(executionId, nodePath, 44L, "catalog-hash"),
            new TriggerRuntimeBudget(4, 4));
    }

    private static NoitaProjectilePayload legacyPayload(List<NoitaProjectilePayload> payloads) {
        return new NoitaProjectilePayload(
            "spark_bolt", NoitaProjectileBehavior.BOLT, 1.0f, 0.0f, 20, 0, 0.0f,
            1.0f, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f, 0.0f, false, false,
            1, 0.0f, NoitaSpellTriggerMode.NONE, 0, 0, List.of(), payloads
        );
    }
}

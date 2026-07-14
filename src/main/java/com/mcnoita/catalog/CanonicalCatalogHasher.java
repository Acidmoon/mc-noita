package com.mcnoita.catalog;

import com.mcnoita.spell.action.AddProjectileAction;
import com.mcnoita.spell.action.AddTriggerToNextProjectileAction;
import com.mcnoita.spell.action.BeginTriggerAction;
import com.mcnoita.spell.action.CallSpellAction;
import com.mcnoita.spell.action.DivideAction;
import com.mcnoita.spell.action.DrawAction;
import com.mcnoita.spell.action.DuplicateHandAction;
import com.mcnoita.spell.action.GreekCopyAction;
import com.mcnoita.spell.action.ModifyShotAction;
import com.mcnoita.spell.action.RandomSpellAction;
import com.mcnoita.spell.action.RefreshWandAction;
import com.mcnoita.spell.action.SpellAction;
import com.mcnoita.spell.action.SpellDefinition;
import com.mcnoita.spell.action.TargetQuery;
import com.mcnoita.spell.action.TimingAction;
import com.mcnoita.spell.plan.ProjectileDefinition;
import com.mcnoita.spell.plan.ShotModifier;
import com.mcnoita.wand.model.NoitaDuration;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Encodes every evaluator-visible definition field in a fixed binary form before
 * hashing. It deliberately does not depend on registry iteration, record
 * {@code toString()}, or JVM-specific {@code hashCode()} implementations.
 */
public final class CanonicalCatalogHasher {
    private static final String FORMAT_VERSION = "mc-noita-catalog-v1";

    private CanonicalCatalogHasher() {
    }

    public static String sha256(Map<String, SpellDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                writeString(output, FORMAT_VERSION);
                List<String> ids = new ArrayList<>(definitions.size());
                for (String id : definitions.keySet()) {
                    if (id == null) {
                        throw new IllegalArgumentException("catalog hash cannot encode a null id");
                    }
                    ids.add(id);
                }
                ids.sort(String::compareTo);
                output.writeInt(ids.size());
                for (String id : ids) {
                    SpellDefinition definition = definitions.get(id);
                    if (definition == null || !id.equals(definition.id())) {
                        throw new IllegalArgumentException("catalog hash requires matching non-null definitions: " + id);
                    }
                    writeDefinition(output, definition);
                }
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", failure);
        } catch (IOException failure) {
            throw new IllegalStateException("in-memory catalog encoding failed", failure);
        }
    }

    private static void writeDefinition(DataOutputStream output, SpellDefinition definition) throws IOException {
        writeString(output, definition.id());
        writeEnum(output, definition.category());
        output.writeInt(definition.manaCost());
        output.writeBoolean(definition.recursive());
        writeEnum(output, definition.useConsumptionPolicy());
        writeString(output, definition.relatedProjectile());
        output.writeInt(definition.actions().size());
        for (SpellAction action : definition.actions()) {
            writeAction(output, action);
        }
    }

    private static void writeAction(DataOutputStream output, SpellAction action) throws IOException {
        if (action instanceof AddProjectileAction value) {
            writeString(output, "add_projectile");
            writeProjectile(output, value.projectile());
        } else if (action instanceof AddTriggerToNextProjectileAction value) {
            writeString(output, "add_trigger_to_next_projectile");
            writeEnum(output, value.mode());
            writeDuration(output, value.timerDelay());
        } else if (action instanceof BeginTriggerAction value) {
            writeString(output, "begin_trigger");
            writeEnum(output, value.mode());
            output.writeInt(value.drawCount());
        } else if (action instanceof CallSpellAction value) {
            writeString(output, "call_spell");
            writeTargetQuery(output, value.query());
        } else if (action instanceof DivideAction value) {
            writeString(output, "divide");
            output.writeInt(value.copies());
            output.writeInt(value.iterationThreshold());
            output.writeDouble(value.castDelayFrames());
            output.writeDouble(value.rechargeFrames());
            output.writeDouble(value.damagePenalty());
            output.writeDouble(value.explosionRadiusPenalty());
            output.writeDouble(value.patternDegrees());
        } else if (action instanceof DrawAction value) {
            writeString(output, "draw");
            output.writeInt(value.amount());
        } else if (action instanceof DuplicateHandAction) {
            writeString(output, "duplicate_hand");
        } else if (action instanceof GreekCopyAction value) {
            writeString(output, "greek_copy");
            writeEnum(output, value.kind());
        } else if (action instanceof ModifyShotAction value) {
            writeString(output, "modify_shot");
            writeShotModifier(output, value.modifier());
        } else if (action instanceof RandomSpellAction value) {
            writeString(output, "random_spell");
            writeEnum(output, value.category());
        } else if (action instanceof RefreshWandAction) {
            writeString(output, "refresh_wand");
        } else if (action instanceof TimingAction value) {
            writeString(output, "timing");
            writeEnum(output, value.castDelayOperation());
            output.writeDouble(value.castDelayFrames());
            writeEnum(output, value.rechargeOperation());
            output.writeDouble(value.rechargeFrames());
        } else {
            throw new IllegalArgumentException("unsupported spell action: " + action);
        }
    }

    private static void writeProjectile(DataOutputStream output, ProjectileDefinition value) throws IOException {
        writeString(output, value.itemPath());
        writeString(output, value.behavior());
        output.writeDouble(value.damage());
        output.writeDouble(value.criticalChancePercent());
        writeDuration(output, value.lifetime());
        output.writeDouble(value.castDelayFrames());
        output.writeDouble(value.rechargeFrames());
        output.writeInt(value.trailLightStacks());
        output.writeDouble(value.explosionRadius());
        output.writeDouble(value.speed());
        output.writeDouble(value.spreadDegrees());
        output.writeDouble(value.gravity());
        output.writeDouble(value.drag());
        output.writeDouble(value.bounceDamping());
        output.writeDouble(value.renderScale());
        output.writeDouble(value.knockbackForce());
        output.writeBoolean(value.friendlyFire());
        output.writeBoolean(value.piercing());
        output.writeInt(value.projectileCount());
        output.writeDouble(value.burstSpreadDegrees());
        writeDuration(output, value.triggerDelay());
        output.writeInt(value.bounceCount());
        writeStringList(output, value.effects());
    }

    private static void writeShotModifier(DataOutputStream output, ShotModifier value) throws IOException {
        output.writeDouble(value.damage());
        output.writeDouble(value.explosionRadius());
        output.writeDouble(value.spreadDegrees());
        output.writeDouble(value.speedMultiplier());
        output.writeDouble(value.castDelayFrames());
        output.writeDouble(value.rechargeFrames());
        output.writeDouble(value.criticalChancePercent());
        output.writeDouble(value.lifetimeFrames());
        output.writeDouble(value.recoil());
        output.writeDouble(value.knockbackForce());
        output.writeDouble(value.gravity());
        output.writeInt(value.bounceCount());
        output.writeBoolean(value.removeBounce());
        output.writeBoolean(value.piercing());
        output.writeBoolean(value.friendlyFire());
        output.writeInt(value.trailLightStacks());
        writeStringList(output, value.effects());
    }

    private static void writeTargetQuery(DataOutputStream output, TargetQuery value) throws IOException {
        output.writeInt(value.sources().size());
        for (TargetQuery.Source source : value.sources()) {
            writeEnum(output, source);
        }
        writeEnum(output, value.direction());
        writeSortedStrings(output, value.categories().stream().map(Enum::name).toList());
        writeSortedStrings(output, value.excludedSpellIds());
        output.writeBoolean(value.recursiveTargetsAllowed());
        output.writeBoolean(value.requireRelatedProjectile());
        output.writeInt(value.limit());
    }

    private static void writeDuration(DataOutputStream output, NoitaDuration value) throws IOException {
        output.writeDouble(value.frames());
    }

    private static void writeSortedStrings(DataOutputStream output, Iterable<String> values) throws IOException {
        List<String> ordered = new ArrayList<>();
        for (String value : values) {
            ordered.add(Objects.requireNonNull(value, "catalog string value"));
        }
        ordered.sort(String::compareTo);
        writeStringList(output, ordered);
    }

    private static void writeStringList(DataOutputStream output, List<String> values) throws IOException {
        output.writeInt(values.size());
        for (String value : values) {
            writeString(output, value);
        }
    }

    private static void writeEnum(DataOutputStream output, Enum<?> value) throws IOException {
        writeString(output, Objects.requireNonNull(value, "catalog enum value").name());
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] bytes = Objects.requireNonNull(value, "catalog string value").getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }
}

package com.mcnoita.wand.adapter;

import com.mcnoita.item.NoitaProjectileSpellItem;
import com.mcnoita.item.NoitaSpellItem;
import com.mcnoita.spell.NoitaModifierEffect;
import com.mcnoita.spell.NoitaProjectileBehavior;
import com.mcnoita.spell.NoitaProjectileSpellSpec;
import com.mcnoita.spell.NoitaSpellTemplate;
import com.mcnoita.spell.NoitaSpellTriggerMode;
import com.mcnoita.spell.NoitaSpellType;
import com.mcnoita.spell.action.AddProjectileAction;
import com.mcnoita.spell.action.BeginTriggerAction;
import com.mcnoita.spell.action.CallSelection;
import com.mcnoita.spell.action.CallSpellAction;
import com.mcnoita.spell.action.DrawAction;
import com.mcnoita.spell.action.DuplicateHandAction;
import com.mcnoita.spell.action.ModifyShotAction;
import com.mcnoita.spell.action.RandomSpellAction;
import com.mcnoita.spell.action.RefreshWandAction;
import com.mcnoita.spell.action.SpellAction;
import com.mcnoita.spell.action.SpellCatalog;
import com.mcnoita.spell.action.SpellCategory;
import com.mcnoita.spell.action.SpellDefinition;
import com.mcnoita.spell.action.TimingAction;
import com.mcnoita.spell.plan.ProjectileDefinition;
import com.mcnoita.spell.plan.ShotModifier;
import com.mcnoita.spell.plan.TriggerMode;
import com.mcnoita.wand.model.NoitaDuration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Temporary bridge from the registered legacy item templates to the pure G01
 * catalog. It is intentionally the only catalog layer that imports Registry.
 */
public final class LegacySpellCatalogAdapter {
    private static final double LEGACY_NOITA_GRAVITY_TO_MINECRAFT = 12000.0;

    private LegacySpellCatalogAdapter() {
    }

    public static SpellCatalog createCatalog() {
        Map<String, SpellDefinition> definitions = new LinkedHashMap<>();
        for (Item item : Registries.ITEM) {
            if (item instanceof NoitaSpellItem spellItem) {
                Identifier id = Registries.ITEM.getId(item);
                definitions.put(id.toString(), adapt(id, spellItem));
            }
        }
        return new SpellCatalog(0L, Integer.toUnsignedString(definitions.hashCode(), 16), definitions);
    }

    private static SpellDefinition adapt(Identifier id, NoitaSpellItem item) {
        NoitaSpellTemplate template = item.getTemplate();
        List<SpellAction> actions = new ArrayList<>();
        if (item instanceof NoitaProjectileSpellItem projectileItem) {
            NoitaProjectileSpellSpec spec = projectileItem.getProjectileSpec();
            if (spec.behavior() == NoitaProjectileBehavior.RANDOM) {
                actions.add(new RandomSpellAction(category(template.type())));
            } else {
                actions.add(new AddProjectileAction(projectile(spec)));
                if (template.triggerMode() != NoitaSpellTriggerMode.NONE && template.triggerDrawCount() > 0) {
                    actions.add(new BeginTriggerAction(triggerMode(template.triggerMode()), template.triggerDrawCount()));
                }
            }
        } else if (template.type() == NoitaSpellType.PROJECTILE_MODIFIER) {
            actions.add(new ModifyShotAction(modifier(template)));
            actions.add(new DrawAction(1));
        } else if (template.type() == NoitaSpellType.MULTICAST) {
            actions.add(new DrawAction(Math.max(1, template.drawCount())));
        } else {
            addLegacySpecialAction(id.getPath(), template, actions);
        }
        return new SpellDefinition(id.toString(), category(template.type()), template.manaDrain(), false, actions);
    }

    private static void addLegacySpecialAction(String path, NoitaSpellTemplate template, List<SpellAction> actions) {
        double castFrames = template.castDelaySeconds() * NoitaDuration.FRAMES_PER_SECOND;
        double rechargeFrames = template.rechargeTimeSeconds() * NoitaDuration.FRAMES_PER_SECOND;
        switch (path) {
            case "duplicate" -> {
                actions.add(new TimingAction(castFrames, rechargeFrames));
                actions.add(new DuplicateHandAction());
            }
            case "wand_refresh" -> {
                actions.add(new TimingAction(0.0, rechargeFrames));
                actions.add(new RefreshWandAction());
            }
            case "alpha" -> {
                actions.add(new TimingAction(castFrames, rechargeFrames));
                actions.add(new CallSpellAction(CallSelection.FIRST_AVAILABLE));
            }
            case "gamma" -> {
                actions.add(new TimingAction(castFrames, rechargeFrames));
                actions.add(new CallSpellAction(CallSelection.LAST_AVAILABLE));
            }
            default -> {
                if (castFrames != 0.0 || rechargeFrames != 0.0) {
                    actions.add(new TimingAction(castFrames, rechargeFrames));
                }
            }
        }
    }

    private static ProjectileDefinition projectile(NoitaProjectileSpellSpec spec) {
        NoitaSpellTemplate template = spec.template();
        int lifetimeTicks = template.maxLifetimeTicks() > 0
            ? Math.min(template.lifetimeTicks(), template.maxLifetimeTicks())
            : template.lifetimeTicks();
        return new ProjectileDefinition(
            spec.itemPath(), spec.behavior().name(), template.damage(), template.criticalChancePercent(),
            MinecraftTimeAdapter.fromMinecraftTicks(Math.max(1, lifetimeTicks)),
            template.castDelaySeconds() * NoitaDuration.FRAMES_PER_SECOND,
            template.rechargeTimeSeconds() * NoitaDuration.FRAMES_PER_SECOND, template.trailLightStacks(),
            template.explosionRadius(), template.speed(), template.spreadDegrees() + template.spreadModifierDegrees(),
            spec.gravity(), spec.drag(), spec.bounceDamping(), spec.renderScale(), template.knockbackForce(),
            template.friendlyFire(), template.piercing(), spec.projectileCount(), spec.burstSpreadDegrees(),
            MinecraftTimeAdapter.fromMinecraftTicks(template.triggerDelayTicks()), template.bounceCount(), effectNames(template.modifierEffects())
        );
    }

    private static ShotModifier modifier(NoitaSpellTemplate template) {
        return new ShotModifier(
            template.damage(), template.explosionRadius(), template.spreadDegrees() + template.spreadModifierDegrees(),
            template.speedMultiplier(), template.castDelaySeconds() * NoitaDuration.FRAMES_PER_SECOND,
            template.rechargeTimeSeconds() * NoitaDuration.FRAMES_PER_SECOND, template.criticalChancePercent(),
            MinecraftTimeAdapter.fromMinecraftTicks(template.lifetimeModifierTicks()).frames(), template.recoil(),
            template.knockbackForce(), template.gravity() / LEGACY_NOITA_GRAVITY_TO_MINECRAFT, template.bounceCount(),
            template.modifierEffects().contains(NoitaModifierEffect.REMOVE_BOUNCE), template.piercing(), template.friendlyFire(),
            template.trailLightStacks(), effectNames(template.modifierEffects())
        );
    }

    private static List<String> effectNames(List<NoitaModifierEffect> effects) {
        return effects.stream().map(Enum::name).toList();
    }

    private static SpellCategory category(NoitaSpellType type) {
        return switch (type) {
            case PROJECTILE -> SpellCategory.PROJECTILE;
            case STATIC_PROJECTILE -> SpellCategory.STATIC_PROJECTILE;
            case PROJECTILE_MODIFIER -> SpellCategory.PROJECTILE_MODIFIER;
            case MULTICAST -> SpellCategory.MULTICAST;
            case MATERIAL -> SpellCategory.MATERIAL;
            case OTHER -> SpellCategory.OTHER;
            case UTILITY -> SpellCategory.UTILITY;
            case PASSIVE -> SpellCategory.PASSIVE;
        };
    }

    private static TriggerMode triggerMode(NoitaSpellTriggerMode mode) {
        return switch (mode) {
            case HIT -> TriggerMode.HIT;
            case TIMER -> TriggerMode.TIMER;
            case DEATH -> TriggerMode.DEATH;
            case NONE -> TriggerMode.NONE;
        };
    }
}

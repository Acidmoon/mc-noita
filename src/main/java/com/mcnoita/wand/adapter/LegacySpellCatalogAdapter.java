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
import com.mcnoita.spell.action.CallSpellAction;
import com.mcnoita.spell.action.AddTriggerToNextProjectileAction;
import com.mcnoita.spell.action.DivideAction;
import com.mcnoita.spell.action.DrawAction;
import com.mcnoita.spell.action.DuplicateHandAction;
import com.mcnoita.spell.action.GreekCopyAction;
import com.mcnoita.spell.action.GreekCopyKind;
import com.mcnoita.spell.action.ModifyShotAction;
import com.mcnoita.spell.action.RandomSpellAction;
import com.mcnoita.spell.action.RefreshWandAction;
import com.mcnoita.spell.action.SpellAction;
import com.mcnoita.spell.action.SpellCatalog;
import com.mcnoita.spell.action.SpellCategory;
import com.mcnoita.spell.action.SpellDefinition;
import com.mcnoita.spell.action.TimingAction;
import com.mcnoita.spell.action.TimingOperation;
import com.mcnoita.spell.action.TargetQuery;
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
        String relatedProjectile = "";
        if (item instanceof NoitaProjectileSpellItem projectileItem) {
            NoitaProjectileSpellSpec spec = projectileItem.getProjectileSpec();
            relatedProjectile = spec.itemPath();
            if (spec.behavior() == NoitaProjectileBehavior.RANDOM) {
                actions.add(new RandomSpellAction(category(template.type())));
            } else {
                actions.add(new AddProjectileAction(projectile(spec)));
                if (id.getPath().equals("chainsaw")) {
                    // Fixed gun_actions.lua uses assignment, not addition, for
                    // c.fire_rate_wait. Its -10 reload adjustment is already
                    // carried by the frozen projectile definition.
                    actions.add(new TimingAction(TimingOperation.SET, 0.0, TimingOperation.ADD, 0.0));
                }
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
        return new SpellDefinition(id.toString(), category(template.type()), template.manaDrain(),
            isRecursiveSpecial(id.getPath()), actions,
            SpellDefinition.defaultUseConsumptionPolicy(category(template.type())), relatedProjectile);
    }

    private static void addLegacySpecialAction(String path, NoitaSpellTemplate template, List<SpellAction> actions) {
        double castFrames = template.castDelaySeconds() * NoitaDuration.FRAMES_PER_SECOND;
        double rechargeFrames = template.rechargeTimeSeconds() * NoitaDuration.FRAMES_PER_SECOND;
        switch (path) {
            case "duplicate" -> {
                actions.add(new DuplicateHandAction());
            }
            case "wand_refresh" -> {
                actions.add(new TimingAction(0.0, rechargeFrames));
                actions.add(new RefreshWandAction());
            }
            case "alpha" -> {
                actions.add(new TimingAction(castFrames, rechargeFrames));
                actions.add(new CallSpellAction(TargetQuery.alpha()));
            }
            case "gamma" -> {
                actions.add(new TimingAction(castFrames, rechargeFrames));
                actions.add(new CallSpellAction(TargetQuery.gamma()));
            }
            case "tau" -> actions.addAll(List.of(new TimingAction(35.0, 0.0),
                new GreekCopyAction(GreekCopyKind.TAU)));
            case "omega" -> actions.addAll(List.of(new TimingAction(50.0, 0.0),
                new GreekCopyAction(GreekCopyKind.OMEGA)));
            case "mu" -> actions.addAll(List.of(new TimingAction(50.0, 0.0),
                new GreekCopyAction(GreekCopyKind.MU)));
            case "phi" -> actions.addAll(List.of(new TimingAction(50.0, 0.0),
                new GreekCopyAction(GreekCopyKind.PHI)));
            case "sigma" -> actions.addAll(List.of(new TimingAction(30.0, 0.0),
                new GreekCopyAction(GreekCopyKind.SIGMA)));
            case "zeta" -> actions.add(new GreekCopyAction(GreekCopyKind.ZETA));
            case "divide_2" -> actions.add(new DivideAction(2, 5, 20.0, 0.0, -0.2, -5.0, 5.0));
            case "divide_3" -> actions.add(new DivideAction(3, 4, 35.0, 0.0, -0.4, -10.0, 5.0));
            case "divide_4" -> actions.add(new DivideAction(4, 4, 50.0, 0.0, -0.6, -20.0, 5.0));
            case "divide_10" -> actions.add(new DivideAction(10, 3, 80.0, 20.0, -1.5, -40.0, 5.0));
            case "add_trigger" -> actions.add(new AddTriggerToNextProjectileAction(TriggerMode.HIT));
            case "add_timer" -> actions.add(new AddTriggerToNextProjectileAction(TriggerMode.TIMER));
            case "add_death_trigger" -> actions.add(new AddTriggerToNextProjectileAction(TriggerMode.EXPIRATION));
            default -> {
                if (castFrames != 0.0 || rechargeFrames != 0.0) {
                    actions.add(new TimingAction(castFrames, rechargeFrames));
                }
            }
        }
    }

    private static boolean isRecursiveSpecial(String path) {
        return switch (path) {
            case "alpha", "gamma", "tau", "omega", "mu", "phi", "sigma", "zeta", "duplicate" -> true;
            default -> false;
        };
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
            case EXPIRATION, DEATH -> TriggerMode.EXPIRATION;
            case NONE -> TriggerMode.NONE;
        };
    }
}

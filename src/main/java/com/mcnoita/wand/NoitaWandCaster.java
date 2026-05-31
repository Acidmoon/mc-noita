package com.mcnoita.wand;

import com.mcnoita.entity.SparkBoltProjectileEntity;
import com.mcnoita.item.ModItems;
import com.mcnoita.item.NoitaSpellItem;
import com.mcnoita.item.NoitaWandItem;
import com.mcnoita.spell.NoitaSpellTemplate;
import com.mcnoita.spell.NoitaSpellType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

public final class NoitaWandCaster {
    private static final String CAST_STATE_KEY = "NoitaWandCastState";
    private static final String DECK_KEY = "Deck";
    private static final String SPELLS_HASH_KEY = "SpellsHash";
    private static final String DRAW_INDEX_KEY = "DrawIndex";
    private static final String CAST_DELAY_START_TICK_KEY = "CastDelayStartTick";
    private static final String NEXT_CAST_TICK_KEY = "NextCastTick";
    private static final String RECHARGE_START_TICK_KEY = "RechargeStartTick";
    private static final String RECHARGE_END_TICK_KEY = "RechargeEndTick";
    private static final String CURRENT_MANA_KEY = "CurrentMana";
    private static final String LAST_MANA_TICK_KEY = "LastManaTick";

    private static final float MIN_ARROW_SPEED = 0.2f;
    private static final float MAX_ARROW_SPEED = 8.0f;
    private static final float NOITA_SPEED_TO_ARROW_SPEED = 300.0f;
    private static final int MIN_CAST_DELAY_TICKS = 1;
    private static final int MIN_RECHARGE_TICKS = 1;
    private static final double SPELL_SPAWN_FORWARD_OFFSET = 0.65;
    private static final double SPELL_SPAWN_RIGHT_OFFSET = 0.35;
    private static final double SPELL_SPAWN_DOWN_OFFSET = 0.25;

    private NoitaWandCaster() {
    }

    public static void cast(ServerPlayerEntity player) {
        ItemStack wandStack = player.getStackInHand(Hand.MAIN_HAND);
        if (!(wandStack.getItem() instanceof NoitaWandItem wandItem)) {
            return;
        }

        ServerWorld world = player.getServerWorld();
        long now = world.getTime();
        NoitaWandTemplate wandTemplate = wandItem.getTemplate(wandStack);
        NbtCompound state = getOrCreateCastState(wandStack);
        float currentMana = updateMana(state, now, wandTemplate);
        DefaultedList<ItemStack> configuredSpells = NoitaWandItem.getSpellStacks(wandStack, wandTemplate.capacity());
        List<Integer> spellSlots = getConfiguredSpellSlots(configuredSpells);
        if (spellSlots.isEmpty()) {
            resetTiming(state);
            writeDeck(state, List.of());
            state.putInt(DRAW_INDEX_KEY, 0);
            state.putInt(SPELLS_HASH_KEY, 0);
            return;
        }

        int spellsHash = getSpellsHash(configuredSpells);
        if (state.getInt(SPELLS_HASH_KEY) != spellsHash) {
            resetTiming(state);
            resetDeck(state, spellSlots, wandTemplate.shuffle(), player.getRandom().nextLong(), spellsHash);
        } else if (readDeck(state).isEmpty()) {
            resetDeck(state, spellSlots, wandTemplate.shuffle(), player.getRandom().nextLong(), spellsHash);
        }

        long rechargeEndTick = state.getLong(RECHARGE_END_TICK_KEY);
        if (rechargeEndTick > now) {
            return;
        }
        if (rechargeEndTick != 0L) {
            finishRecharge(state, spellSlots, wandTemplate.shuffle(), player.getRandom().nextLong(), spellsHash);
        }

        if (state.getLong(NEXT_CAST_TICK_KEY) > now) {
            return;
        }

        int drawIndexBefore = state.getInt(DRAW_INDEX_KEY);
        CastBlock castBlock = drawCastBlock(state, configuredSpells, wandTemplate);
        if (castBlock.spells().isEmpty()) {
            startRecharge(state, now, wandTemplate);
            return;
        }

        int manaDrain = getCastManaDrain(castBlock);
        if (manaDrain > 0 && currentMana < manaDrain) {
            state.putInt(DRAW_INDEX_KEY, drawIndexBefore);
            return;
        }
        spendMana(state, now, wandTemplate, currentMana, manaDrain);

        boolean castAnySpell = false;
        float castDelaySeconds = wandTemplate.castDelaySeconds();
        float rechargeTimeSeconds = 0.0f;

        SpellModifiers modifiers = SpellModifiers.EMPTY;
        for (ItemStack spellStack : castBlock.spells()) {
            if (spellStack.getItem() instanceof NoitaSpellItem spellItem) {
                NoitaSpellTemplate spellTemplate = spellItem.getTemplate();
                if (spellTemplate.type() == NoitaSpellType.PROJECTILE_MODIFIER) {
                    modifiers = modifiers.add(spellTemplate);
                    continue;
                }

                if (isProjectileSpell(spellStack)) {
                    NoitaSpellTemplate modifiedTemplate = modifiers.applyTo(spellTemplate);
                    spawnSparkBolt(player, wandTemplate, modifiedTemplate);
                    castDelaySeconds += modifiedTemplate.castDelaySeconds();
                    rechargeTimeSeconds += modifiedTemplate.rechargeTimeSeconds();
                    modifiers = SpellModifiers.EMPTY;
                    castAnySpell = true;
                }
            }
        }

        if (!castAnySpell) {
            startCastDelay(state, now, MIN_CAST_DELAY_TICKS);
            if (castBlock.deckExhausted()) {
                startRecharge(state, now, wandTemplate);
            }
            return;
        }

        startCastDelay(state, now, secondsToTicks(castDelaySeconds, MIN_CAST_DELAY_TICKS));
        if (castBlock.deckExhausted()) {
            startRecharge(state, now, wandTemplate.rechargeTimeSeconds() + rechargeTimeSeconds);
        }
    }

    public static CastHudState getHudState(ServerPlayerEntity player) {
        ItemStack wandStack = player.getStackInHand(Hand.MAIN_HAND);
        if (!(wandStack.getItem() instanceof NoitaWandItem wandItem)) {
            return CastHudState.empty();
        }

        NoitaWandTemplate wandTemplate = wandItem.getTemplate(wandStack);
        NbtCompound state = getOrCreateCastState(wandStack);
        long now = player.getServerWorld().getTime();
        float currentMana = updateMana(state, now, wandTemplate);
        int manaMax = wandTemplate.manaMax();
        long rechargeStart = state.getLong(RECHARGE_START_TICK_KEY);
        long rechargeEnd = state.getLong(RECHARGE_END_TICK_KEY);
        if (rechargeEnd > now && rechargeEnd > rechargeStart) {
            return CastHudState.recharge((int) (now - rechargeStart), (int) (rechargeEnd - rechargeStart), currentMana, manaMax);
        }

        long castDelayStart = state.getLong(CAST_DELAY_START_TICK_KEY);
        long castDelayEnd = state.getLong(NEXT_CAST_TICK_KEY);
        if (castDelayEnd > now && castDelayEnd > castDelayStart) {
            return CastHudState.castDelay((int) (now - castDelayStart), (int) (castDelayEnd - castDelayStart), currentMana, manaMax);
        }

        return CastHudState.idle(currentMana, manaMax);
    }

    private static CastBlock drawCastBlock(NbtCompound state, DefaultedList<ItemStack> configuredSpells, NoitaWandTemplate wandTemplate) {
        List<Integer> deck = readDeck(state);
        int drawIndex = state.getInt(DRAW_INDEX_KEY);
        List<ItemStack> spells = new ArrayList<>();

        int projectileDraws = 0;
        while (projectileDraws < wandTemplate.spellsPerCast()) {
            if (drawIndex >= deck.size()) {
                break;
            }

            int spellSlot = deck.get(drawIndex);
            drawIndex++;
            if (spellSlot >= 0 && spellSlot < configuredSpells.size()) {
                ItemStack spellStack = configuredSpells.get(spellSlot);
                if (!spellStack.isEmpty()) {
                    spells.add(spellStack.copy());
                    if (!isProjectileModifier(spellStack)) {
                        projectileDraws++;
                    }
                }
            }
        }

        state.putInt(DRAW_INDEX_KEY, drawIndex);
        boolean deckExhausted = drawIndex >= deck.size();

        return new CastBlock(spells, deckExhausted);
    }

    private static void startRecharge(NbtCompound state, long now, NoitaWandTemplate wandTemplate) {
        startRecharge(state, now, wandTemplate.rechargeTimeSeconds());
    }

    private static void startRecharge(NbtCompound state, long now, float rechargeTimeSeconds) {
        state.putLong(RECHARGE_START_TICK_KEY, now);
        state.putLong(RECHARGE_END_TICK_KEY, now + secondsToTicks(rechargeTimeSeconds, MIN_RECHARGE_TICKS));
    }

    private static void finishRecharge(NbtCompound state, List<Integer> spellSlots, boolean shuffle, long randomSeed, int spellsHash) {
        resetDeck(state, spellSlots, shuffle, randomSeed, spellsHash);
        state.putLong(RECHARGE_START_TICK_KEY, 0L);
        state.putLong(RECHARGE_END_TICK_KEY, 0L);
    }

    private static void resetDeck(NbtCompound state, List<Integer> spellSlots, boolean shuffle, long randomSeed, int spellsHash) {
        List<Integer> deck = new ArrayList<>(spellSlots);
        if (shuffle) {
            Collections.shuffle(deck, new java.util.Random(randomSeed));
        }

        writeDeck(state, deck);
        state.putInt(DRAW_INDEX_KEY, 0);
        state.putInt(SPELLS_HASH_KEY, spellsHash);
    }

    private static void resetTiming(NbtCompound state) {
        state.putLong(CAST_DELAY_START_TICK_KEY, 0L);
        state.putLong(NEXT_CAST_TICK_KEY, 0L);
        state.putLong(RECHARGE_START_TICK_KEY, 0L);
        state.putLong(RECHARGE_END_TICK_KEY, 0L);
    }

    private static void startCastDelay(NbtCompound state, long now, int ticks) {
        state.putLong(CAST_DELAY_START_TICK_KEY, now);
        state.putLong(NEXT_CAST_TICK_KEY, now + ticks);
    }

    private static NbtCompound getOrCreateCastState(ItemStack wandStack) {
        NbtCompound nbt = wandStack.getOrCreateNbt();
        if (!nbt.contains(CAST_STATE_KEY, NbtElement.COMPOUND_TYPE)) {
            nbt.put(CAST_STATE_KEY, new NbtCompound());
        }

        return nbt.getCompound(CAST_STATE_KEY);
    }

    private static float updateMana(NbtCompound state, long now, NoitaWandTemplate wandTemplate) {
        int manaMax = wandTemplate.manaMax();
        if (manaMax <= 0) {
            state.putFloat(CURRENT_MANA_KEY, 0.0f);
            state.putLong(LAST_MANA_TICK_KEY, now);
            return 0.0f;
        }

        float currentMana = state.contains(CURRENT_MANA_KEY, NbtElement.FLOAT_TYPE) ? state.getFloat(CURRENT_MANA_KEY) : manaMax;
        long lastManaTick = state.contains(LAST_MANA_TICK_KEY, NbtElement.LONG_TYPE) ? state.getLong(LAST_MANA_TICK_KEY) : now;
        if (now > lastManaTick && currentMana < manaMax) {
            currentMana += (now - lastManaTick) * (wandTemplate.manaChargeSpeed() / 20.0f);
        }

        currentMana = clampMana(currentMana, manaMax);
        state.putFloat(CURRENT_MANA_KEY, currentMana);
        state.putLong(LAST_MANA_TICK_KEY, now);
        return currentMana;
    }

    private static void spendMana(NbtCompound state, long now, NoitaWandTemplate wandTemplate, float currentMana, int manaDrain) {
        if (manaDrain == 0) {
            return;
        }

        state.putFloat(CURRENT_MANA_KEY, clampMana(currentMana - manaDrain, wandTemplate.manaMax()));
        state.putLong(LAST_MANA_TICK_KEY, now);
    }

    private static float clampMana(float mana, int manaMax) {
        return Math.max(0.0f, Math.min(manaMax, mana));
    }

    private static List<Integer> getConfiguredSpellSlots(DefaultedList<ItemStack> configuredSpells) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < configuredSpells.size(); slot++) {
            ItemStack spellStack = configuredSpells.get(slot);
            if (!spellStack.isEmpty() && spellStack.getItem() instanceof NoitaSpellItem) {
                slots.add(slot);
            }
        }

        return slots;
    }

    private static int getSpellsHash(DefaultedList<ItemStack> configuredSpells) {
        int hash = 1;
        for (int i = 0; i < configuredSpells.size(); i++) {
            ItemStack spellStack = configuredSpells.get(i);
            Identifier id = Registries.ITEM.getId(spellStack.getItem());
            hash = 31 * hash + i;
            hash = 31 * hash + (spellStack.isEmpty() ? 0 : id.hashCode());
            hash = 31 * hash + spellStack.getCount();
        }

        return hash;
    }

    private static List<Integer> readDeck(NbtCompound state) {
        NbtList nbtDeck = state.getList(DECK_KEY, NbtElement.INT_TYPE);
        List<Integer> deck = new ArrayList<>(nbtDeck.size());
        for (int i = 0; i < nbtDeck.size(); i++) {
            deck.add(nbtDeck.getInt(i));
        }

        return deck;
    }

    private static void writeDeck(NbtCompound state, List<Integer> deck) {
        NbtList nbtDeck = new NbtList();
        for (int slot : deck) {
            nbtDeck.add(NbtInt.of(slot));
        }
        state.put(DECK_KEY, nbtDeck);
    }

    private static int getCastManaDrain(CastBlock castBlock) {
        int manaDrain = 0;
        for (ItemStack spellStack : castBlock.spells()) {
            if (spellStack.getItem() instanceof NoitaSpellItem spellItem) {
                manaDrain += spellItem.getTemplate().manaDrain();
            }
        }

        return manaDrain;
    }

    private static void spawnSparkBolt(ServerPlayerEntity player, NoitaWandTemplate wandTemplate, NoitaSpellTemplate spellTemplate) {
        ServerWorld world = player.getServerWorld();
        SparkBoltProjectileEntity sparkBolt = new SparkBoltProjectileEntity(
            world,
            player,
            spellTemplate.damage(),
            spellTemplate.criticalChancePercent(),
            spellTemplate.lifetimeTicks(),
            spellTemplate.trailLightStacks()
        );
        Vec3d spawnPosition = getSpellSpawnPosition(player);
        Vec3d direction = player.getRotationVec(1.0f);
        sparkBolt.setPosition(spawnPosition);
        sparkBolt.setVelocity(
            direction.x,
            direction.y,
            direction.z,
            getArrowSpeed(wandTemplate, spellTemplate),
            getDivergence(wandTemplate, spellTemplate)
        );

        world.spawnEntity(sparkBolt);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 0.6f, 1.35f);
    }

    private static Vec3d getSpellSpawnPosition(ServerPlayerEntity player) {
        double yawRadians = Math.toRadians(player.getYaw());
        Vec3d forward = new Vec3d(-Math.sin(yawRadians), 0.0, Math.cos(yawRadians));
        Vec3d right = new Vec3d(-Math.cos(yawRadians), 0.0, -Math.sin(yawRadians));
        return new Vec3d(player.getX(), player.getEyeY(), player.getZ())
            .add(forward.multiply(SPELL_SPAWN_FORWARD_OFFSET))
            .add(right.multiply(SPELL_SPAWN_RIGHT_OFFSET))
            .add(0.0, -SPELL_SPAWN_DOWN_OFFSET, 0.0);
    }

    private static float getArrowSpeed(NoitaWandTemplate wandTemplate, NoitaSpellTemplate spellTemplate) {
        float speed = spellTemplate.speed() / NOITA_SPEED_TO_ARROW_SPEED;
        speed *= wandTemplate.speedMultiplier() * spellTemplate.speedMultiplier();
        return Math.max(MIN_ARROW_SPEED, Math.min(MAX_ARROW_SPEED, speed));
    }

    private static float getDivergence(NoitaWandTemplate wandTemplate, NoitaSpellTemplate spellTemplate) {
        return Math.max(0.0f, wandTemplate.spreadDegrees() + spellTemplate.spreadDegrees() + spellTemplate.spreadModifierDegrees());
    }

    private static boolean isProjectileSpell(ItemStack spellStack) {
        return spellStack.isOf(ModItems.SPARK_BOLT) || spellStack.isOf(ModItems.BOUNCING_BURST) || spellStack.isOf(ModItems.LIGHT_BULLET);
    }

    private static boolean isProjectileModifier(ItemStack spellStack) {
        return spellStack.getItem() instanceof NoitaSpellItem spellItem && spellItem.getTemplate().type() == NoitaSpellType.PROJECTILE_MODIFIER;
    }

    private static int secondsToTicks(float seconds, int minimumTicks) {
        return Math.max(minimumTicks, Math.round(Math.max(0.0f, seconds) * 20.0f));
    }

    private record CastBlock(List<ItemStack> spells, boolean deckExhausted) {
    }

    private record SpellModifiers(int trailLightStacks, float castDelaySeconds, float rechargeTimeSeconds, float spreadModifierDegrees) {
        private static final SpellModifiers EMPTY = new SpellModifiers(0, 0.0f, 0.0f, 0.0f);

        private SpellModifiers add(NoitaSpellTemplate modifier) {
            return new SpellModifiers(
                this.trailLightStacks + modifier.trailLightStacks(),
                this.castDelaySeconds + modifier.castDelaySeconds(),
                this.rechargeTimeSeconds + modifier.rechargeTimeSeconds(),
                this.spreadModifierDegrees + modifier.spreadModifierDegrees()
            );
        }

        private NoitaSpellTemplate applyTo(NoitaSpellTemplate projectile) {
            if (this.equals(EMPTY)) {
                return projectile;
            }

            return NoitaSpellTemplate.builder()
                .type(projectile.type())
                .maxUses(projectile.maxUses())
                .manaDrain(projectile.manaDrain())
                .damage(projectile.damage())
                .explosionRadius(projectile.explosionRadius())
                .spreadDegrees(projectile.spreadDegrees())
                .speed(projectile.speed())
                .castDelaySeconds(projectile.castDelaySeconds() + this.castDelaySeconds)
                .rechargeTimeSeconds(projectile.rechargeTimeSeconds() + this.rechargeTimeSeconds)
                .spreadModifierDegrees(projectile.spreadModifierDegrees() + this.spreadModifierDegrees)
                .speedMultiplier(projectile.speedMultiplier())
                .criticalChancePercent(projectile.criticalChancePercent())
                .lifetimeTicks(projectile.lifetimeTicks())
                .maxLifetimeTicks(projectile.maxLifetimeTicks())
                .lifetimeModifierTicks(projectile.lifetimeModifierTicks())
                .recoil(projectile.recoil())
                .piercing(projectile.piercing())
                .friendlyFire(projectile.friendlyFire())
                .trailLightStacks(projectile.trailLightStacks() + this.trailLightStacks)
                .build();
        }
    }

    public record CastHudState(int mode, int progressTicks, int totalTicks, float currentMana, int manaMax) {
        public static final int MODE_EMPTY = 0;
        public static final int MODE_CAST_DELAY = 1;
        public static final int MODE_RECHARGE = 2;

        private static CastHudState empty() {
            return new CastHudState(MODE_EMPTY, 0, 0, 0.0f, 0);
        }

        private static CastHudState idle(float currentMana, int manaMax) {
            return new CastHudState(MODE_EMPTY, 0, 0, Math.max(0.0f, currentMana), Math.max(0, manaMax));
        }

        private static CastHudState castDelay(int progressTicks, int totalTicks, float currentMana, int manaMax) {
            return new CastHudState(MODE_CAST_DELAY, Math.max(0, progressTicks), Math.max(1, totalTicks), Math.max(0.0f, currentMana), Math.max(0, manaMax));
        }

        private static CastHudState recharge(int progressTicks, int totalTicks, float currentMana, int manaMax) {
            return new CastHudState(MODE_RECHARGE, Math.max(0, progressTicks), Math.max(1, totalTicks), Math.max(0.0f, currentMana), Math.max(0, manaMax));
        }
    }
}

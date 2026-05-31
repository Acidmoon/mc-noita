package com.mcnoita.wand;

import com.mcnoita.item.ModItems;
import com.mcnoita.item.NoitaSpellItem;
import com.mcnoita.item.NoitaWandItem;
import com.mcnoita.spell.NoitaSpellTemplate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ArrowEntity;
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

public final class NoitaWandCaster {
    private static final String CAST_STATE_KEY = "NoitaWandCastState";
    private static final String DECK_KEY = "Deck";
    private static final String SPELLS_HASH_KEY = "SpellsHash";
    private static final String DRAW_INDEX_KEY = "DrawIndex";
    private static final String NEXT_CAST_TICK_KEY = "NextCastTick";
    private static final String RECHARGE_END_TICK_KEY = "RechargeEndTick";

    private static final float MIN_ARROW_SPEED = 0.2f;
    private static final float MAX_ARROW_SPEED = 4.0f;
    private static final float NOITA_SPEED_TO_ARROW_SPEED = 300.0f;
    private static final int MIN_CAST_DELAY_TICKS = 1;
    private static final int MIN_RECHARGE_TICKS = 1;

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
        DefaultedList<ItemStack> configuredSpells = NoitaWandItem.getSpellStacks(wandStack, wandTemplate.capacity());
        List<Integer> spellSlots = getConfiguredSpellSlots(configuredSpells);
        if (spellSlots.isEmpty()) {
            clearCastState(wandStack);
            return;
        }

        NbtCompound state = getOrCreateCastState(wandStack);
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

        CastBlock castBlock = drawCastBlock(state, configuredSpells, wandTemplate);
        if (castBlock.spells().isEmpty()) {
            startRecharge(state, now, wandTemplate);
            return;
        }

        boolean castAnySpell = false;
        float castDelaySeconds = wandTemplate.castDelaySeconds();
        float rechargeTimeSeconds = 0.0f;

        for (ItemStack spellStack : castBlock.spells()) {
            if (spellStack.isOf(ModItems.SPARK_BOLT) && spellStack.getItem() instanceof NoitaSpellItem spellItem) {
                NoitaSpellTemplate spellTemplate = spellItem.getTemplate();
                spawnSparkBolt(player, wandTemplate, spellTemplate);
                castDelaySeconds += spellTemplate.castDelaySeconds();
                rechargeTimeSeconds += spellTemplate.rechargeTimeSeconds();
                castAnySpell = true;
            }
        }

        if (!castAnySpell) {
            state.putLong(NEXT_CAST_TICK_KEY, now + MIN_CAST_DELAY_TICKS);
            if (castBlock.deckExhausted()) {
                startRecharge(state, now, wandTemplate);
            }
            return;
        }

        state.putLong(NEXT_CAST_TICK_KEY, now + secondsToTicks(castDelaySeconds, MIN_CAST_DELAY_TICKS));
        if (castBlock.deckExhausted()) {
            startRecharge(state, now, wandTemplate.rechargeTimeSeconds() + rechargeTimeSeconds);
        }
    }

    private static CastBlock drawCastBlock(NbtCompound state, DefaultedList<ItemStack> configuredSpells, NoitaWandTemplate wandTemplate) {
        List<Integer> deck = readDeck(state);
        int drawIndex = state.getInt(DRAW_INDEX_KEY);
        List<ItemStack> spells = new ArrayList<>();

        for (int i = 0; i < wandTemplate.spellsPerCast(); i++) {
            if (drawIndex >= deck.size()) {
                break;
            }

            int spellSlot = deck.get(drawIndex);
            drawIndex++;
            if (spellSlot >= 0 && spellSlot < configuredSpells.size()) {
                ItemStack spellStack = configuredSpells.get(spellSlot);
                if (!spellStack.isEmpty()) {
                    spells.add(spellStack.copy());
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
        state.putLong(RECHARGE_END_TICK_KEY, now + secondsToTicks(rechargeTimeSeconds, MIN_RECHARGE_TICKS));
    }

    private static void finishRecharge(NbtCompound state, List<Integer> spellSlots, boolean shuffle, long randomSeed, int spellsHash) {
        resetDeck(state, spellSlots, shuffle, randomSeed, spellsHash);
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
        state.putLong(NEXT_CAST_TICK_KEY, 0L);
        state.putLong(RECHARGE_END_TICK_KEY, 0L);
    }

    private static NbtCompound getOrCreateCastState(ItemStack wandStack) {
        NbtCompound nbt = wandStack.getOrCreateNbt();
        if (!nbt.contains(CAST_STATE_KEY, NbtElement.COMPOUND_TYPE)) {
            nbt.put(CAST_STATE_KEY, new NbtCompound());
        }

        return nbt.getCompound(CAST_STATE_KEY);
    }

    private static void clearCastState(ItemStack wandStack) {
        NbtCompound nbt = wandStack.getNbt();
        if (nbt != null) {
            nbt.remove(CAST_STATE_KEY);
        }
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

    private static void spawnSparkBolt(ServerPlayerEntity player, NoitaWandTemplate wandTemplate, NoitaSpellTemplate spellTemplate) {
        ServerWorld world = player.getServerWorld();
        ArrowEntity arrow = new ArrowEntity(EntityType.ARROW, world);
        arrow.setOwner(player);
        arrow.setPosition(player.getX(), player.getEyeY() - 0.1, player.getZ());
        arrow.setDamage(Math.max(0.0f, spellTemplate.damage()));
        arrow.setVelocity(
            player,
            player.getPitch(),
            player.getYaw(),
            0.0f,
            getArrowSpeed(wandTemplate, spellTemplate),
            getDivergence(wandTemplate, spellTemplate)
        );

        world.spawnEntity(arrow);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 0.6f, 1.35f);
    }

    private static float getArrowSpeed(NoitaWandTemplate wandTemplate, NoitaSpellTemplate spellTemplate) {
        float speed = spellTemplate.speed() / NOITA_SPEED_TO_ARROW_SPEED;
        speed *= wandTemplate.speedMultiplier() * spellTemplate.speedMultiplier();
        return Math.max(MIN_ARROW_SPEED, Math.min(MAX_ARROW_SPEED, speed));
    }

    private static float getDivergence(NoitaWandTemplate wandTemplate, NoitaSpellTemplate spellTemplate) {
        return Math.max(0.0f, wandTemplate.spreadDegrees() + spellTemplate.spreadDegrees() + spellTemplate.spreadModifierDegrees());
    }

    private static int secondsToTicks(float seconds, int minimumTicks) {
        return Math.max(minimumTicks, Math.round(Math.max(0.0f, seconds) * 20.0f));
    }

    private record CastBlock(List<ItemStack> spells, boolean deckExhausted) {
    }
}

package com.mcnoita.wand.adapter;

import com.mcnoita.item.NoitaSpellItem;
import com.mcnoita.item.NoitaWandItem;
import com.mcnoita.persistence.NoitaNbtLimits;
import com.mcnoita.persistence.NoitaNbtSafety;
import com.mcnoita.persistence.NoitaNbtSchema;
import com.mcnoita.wand.NoitaWandTemplate;
import com.mcnoita.wand.eval.CastRng;
import com.mcnoita.wand.model.CardRef;
import com.mcnoita.wand.model.DeckState;
import com.mcnoita.wand.model.NoitaDuration;
import com.mcnoita.wand.model.SpellCardState;
import com.mcnoita.wand.model.WandDefinition;
import com.mcnoita.wand.model.WandState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.collection.DefaultedList;

/**
 * Persistence boundary for legacy wand ItemStack data. Existing NBT keys stay
 * unchanged while the evaluator sees only immutable model values.
 */
public final class MinecraftWandAdapter {
    public static final String CAST_STATE_KEY = "NoitaWandCastState";
    private static final String DECK_KEY = "Deck";
    private static final String DISCARDED_KEY = "Discarded";
    private static final String SPELLS_HASH_KEY = "SpellsHash";
    private static final String DRAW_INDEX_KEY = "DrawIndex";
    private static final String CAST_DELAY_START_TICK_KEY = "CastDelayStartTick";
    private static final String NEXT_CAST_TICK_KEY = "NextCastTick";
    private static final String RECHARGE_START_TICK_KEY = "RechargeStartTick";
    private static final String RECHARGE_END_TICK_KEY = "RechargeEndTick";
    private static final String CURRENT_MANA_KEY = "CurrentMana";
    private static final String LAST_MANA_TICK_KEY = "LastManaTick";
    private static final String REVISION_KEY = "StateRevision";

    private MinecraftWandAdapter() {
    }

    public static LoadedWand read(ItemStack wandStack, NoitaWandItem wandItem, long now, long randomSeed) {
        if (!wandItem.hasSupportedNbt(wandStack)) {
            return null;
        }
        NoitaWandTemplate legacyDefinition = wandItem.getTemplate(wandStack);
        WandDefinition definition = definition(legacyDefinition);
        NbtCompound persisted = getOrCreateCastState(wandStack);
        if (persisted == null) {
            return null;
        }
        DefaultedList<ItemStack> spellStacks = NoitaWandItem.getSpellStacks(wandStack, legacyDefinition.capacity());
        Map<CardRef, SpellCardState> cards = cards(spellStacks);
        int spellsHash = spellsHash(spellStacks, legacyDefinition);
        List<CardRef> deck = readPile(persisted, DECK_KEY, cards, legacyDefinition.capacity());
        List<CardRef> discard = readPile(persisted, DISCARDED_KEY, cards, legacyDefinition.capacity());

        boolean reset = persisted.getInt(SPELLS_HASH_KEY) != spellsHash || persisted.contains(DRAW_INDEX_KEY, NbtElement.NUMBER_TYPE)
            || (deck.isEmpty() && discard.isEmpty() && !cards.isEmpty());
        long rechargeEnd = persisted.getLong(RECHARGE_END_TICK_KEY);
        if (rechargeEnd != 0L && rechargeEnd <= now) {
            reset = true;
        }
        if (reset) {
            deck = ordered(cards, definition.shuffle(), randomSeed, "initial");
            discard = List.of();
        } else {
            addMissingCardsToDiscard(cards, deck, discard);
        }

        double mana = persisted.contains(CURRENT_MANA_KEY, NbtElement.NUMBER_TYPE) ? persisted.getFloat(CURRENT_MANA_KEY) : definition.manaMax();
        if (!Double.isFinite(mana)) {
            mana = definition.manaMax();
        }
        long lastManaTick = persisted.contains(LAST_MANA_TICK_KEY, NbtElement.LONG_TYPE) ? persisted.getLong(LAST_MANA_TICK_KEY) : now;
        NoitaDuration elapsed = MinecraftTimeAdapter.fromMinecraftTicks(Math.max(0L, now - lastManaTick));
        WandState state = LegacyWandStateAdapter.toWandState(
            cards, slots(deck, cards), slots(discard, cards), Math.max(0.0, Math.min(definition.manaMax(), mana)),
            MinecraftTimeAdapter.fromMinecraftTicks(Math.max(0L, persisted.getLong(NEXT_CAST_TICK_KEY) - now)),
            MinecraftTimeAdapter.fromMinecraftTicks(Math.max(0L, rechargeEnd - now)),
            rechargeEnd > now,
            Math.max(0L, persisted.getLong(REVISION_KEY)),
            spellsHash
        );
        return new LoadedWand(definition, state, spellStacks, spellsHash, elapsed);
    }

    public static void write(ItemStack wandStack, LoadedWand loaded, WandState next, long now) {
        NbtCompound persisted = getOrCreateCastState(wandStack);
        if (persisted == null) {
            throw new IllegalStateException("wand cast state failed persistence validation");
        }
        LegacyWandStateAdapter.LegacyState legacyState = LegacyWandStateAdapter.fromWandState(next);
        writePile(persisted, DECK_KEY, legacyState.deckSlots());
        writePile(persisted, DISCARDED_KEY, legacyState.discardSlots());
        persisted.putInt(SPELLS_HASH_KEY, loaded.spellsHash());
        persisted.putFloat(CURRENT_MANA_KEY, (float) Math.max(0.0, Math.min(loaded.definition().manaMax(), next.mana())));
        persisted.putLong(LAST_MANA_TICK_KEY, now);
        persisted.putLong(REVISION_KEY, next.revision());
        writeCooldown(persisted, CAST_DELAY_START_TICK_KEY, NEXT_CAST_TICK_KEY, next.castDelayRemaining(), now);
        writeCooldown(persisted, RECHARGE_START_TICK_KEY, RECHARGE_END_TICK_KEY, next.rechargeRemaining(), now);
        applyRemainingUses(loaded.spellStacks(), legacyState.remainingUses());
        NoitaWandItem.setSpellStacks(wandStack, loaded.spellStacks());
    }

    public static boolean canCast(ItemStack wandStack, long now) {
        NbtCompound state = getOrCreateCastState(wandStack);
        return state != null && state.getLong(RECHARGE_END_TICK_KEY) <= now && state.getLong(NEXT_CAST_TICK_KEY) <= now;
    }

    public static NbtCompound getOrCreateCastState(ItemStack wandStack) {
        NbtCompound root = wandStack.getOrCreateNbt();
        if (!root.contains(CAST_STATE_KEY, NbtElement.COMPOUND_TYPE)) {
            root.put(CAST_STATE_KEY, new NbtCompound());
        }
        NbtCompound state = root.getCompound(CAST_STATE_KEY);
        return NoitaNbtSchema.migrateToCurrent(state, NoitaNbtSchema.Kind.CAST_STATE)
            && NoitaNbtSafety.validateTree(state, 16, 1024, NoitaNbtLimits.MAX_CAST_STATE_SLOTS) ? state : null;
    }

    public static WandDefinition definition(NoitaWandTemplate template) {
        return new WandDefinition(template.shuffle(), template.spellsPerCast(), NoitaDuration.seconds(template.castDelaySeconds()),
            NoitaDuration.seconds(template.rechargeTimeSeconds()), template.manaMax(), template.manaChargeSpeed(),
            template.capacity(), template.spreadDegrees(), template.speedMultiplier(),
            template.alwaysCastSpells().stream().map(Object::toString).toList());
    }

    private static Map<CardRef, SpellCardState> cards(DefaultedList<ItemStack> spellStacks) {
        Map<CardRef, SpellCardState> cards = new LinkedHashMap<>();
        for (int slot = 0; slot < spellStacks.size(); slot++) {
            ItemStack stack = spellStacks.get(slot);
            if (stack.getItem() instanceof NoitaSpellItem) {
                CardRef ref = CardRef.forSlot(slot);
                cards.put(ref, new SpellCardState(ref, slot, Registries.ITEM.getId(stack.getItem()).toString(), NoitaSpellItem.getRemainingUses(stack)));
            }
        }
        return cards;
    }

    private static int spellsHash(DefaultedList<ItemStack> spellStacks, NoitaWandTemplate template) {
        int hash = 1;
        for (int slot = 0; slot < spellStacks.size(); slot++) {
            ItemStack stack = spellStacks.get(slot);
            hash = 31 * hash + slot;
            hash = 31 * hash + (stack.isEmpty() ? 0 : Registries.ITEM.getId(stack.getItem()).hashCode());
            hash = 31 * hash + stack.getCount();
        }
        return 31 * hash + template.alwaysCastSpells().hashCode();
    }

    private static List<CardRef> readPile(NbtCompound state, String key, Map<CardRef, SpellCardState> cards, int capacity) {
        NbtList values = state.getList(key, NbtElement.INT_TYPE);
        if (values.size() > Math.min(NoitaNbtLimits.MAX_CAST_STATE_SLOTS, Math.max(1, capacity))) {
            return List.of();
        }
        List<CardRef> refs = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            int slot = values.getInt(index);
            CardRef ref = CardRef.forSlot(Math.max(0, slot));
            if (slot < 0 || !cards.containsKey(ref) || refs.contains(ref)) {
                return List.of();
            }
            refs.add(ref);
        }
        return refs;
    }

    private static void addMissingCardsToDiscard(Map<CardRef, SpellCardState> cards, List<CardRef> deck, List<CardRef> discard) {
        for (CardRef ref : cards.keySet()) {
            if (!deck.contains(ref) && !discard.contains(ref)) {
                discard.add(ref);
            }
        }
    }

    private static List<CardRef> ordered(Map<CardRef, SpellCardState> cards, boolean shuffle, long seed, String domain) {
        List<CardRef> refs = new ArrayList<>(cards.keySet());
        refs.sort((left, right) -> Integer.compare(cards.get(left).slot(), cards.get(right).slot()));
        if (shuffle) {
            CastRng rng = new CastRng(seed);
            for (int index = refs.size() - 1; index > 0; index--) {
                int other = rng.nextInt("shuffle:" + domain, index + 1);
                Collections.swap(refs, index, other);
            }
        }
        return List.copyOf(refs);
    }

    private static List<Integer> slots(List<CardRef> refs, Map<CardRef, SpellCardState> cards) {
        return refs.stream().map(ref -> cards.get(ref).slot()).toList();
    }

    private static void writePile(NbtCompound state, String key, List<Integer> slots) {
        NbtList values = new NbtList();
        for (int slot : slots) {
            values.add(NbtInt.of(slot));
        }
        state.put(key, values);
    }

    private static void writeCooldown(NbtCompound state, String startKey, String endKey, NoitaDuration remaining, long now) {
        if (remaining.isZero()) {
            state.putLong(startKey, 0L);
            state.putLong(endKey, 0L);
            return;
        }
        int ticks = MinecraftTimeAdapter.toMinecraftTicks(remaining, 1);
        state.putLong(startKey, now);
        state.putLong(endKey, now + ticks);
    }

    private static void applyRemainingUses(DefaultedList<ItemStack> spellStacks, Map<Integer, Integer> remainingUses) {
        for (Map.Entry<Integer, Integer> entry : remainingUses.entrySet()) {
            int slot = entry.getKey();
            if (slot >= 0 && slot < spellStacks.size()) {
                NoitaSpellItem.setRemainingUses(spellStacks.get(slot), entry.getValue());
            }
        }
    }

    public record LoadedWand(
        WandDefinition definition,
        WandState state,
        DefaultedList<ItemStack> spellStacks,
        int spellsHash,
        NoitaDuration elapsed
    ) {
    }
}

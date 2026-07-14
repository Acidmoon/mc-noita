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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.nbt.NbtShort;
import net.minecraft.nbt.NbtString;
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
    private static final String G02_RELOAD_PREPARED_KEY = "G02ReloadPrepared";

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
        long rechargeEnd = persisted.getLong(RECHARGE_END_TICK_KEY);
        if (!persisted.getBoolean(G02_RELOAD_PREPARED_KEY) && rechargeEnd != 0L && deck.isEmpty() && !discard.isEmpty()) {
            // G01 did this work when cooldown finished, using the next cast's
            // seed. A legacy state cannot recover that seed, so migrate it once
            // from stable persisted identity rather than caller/UI randomness.
            deck = migrateG01ReloadDeck(discard, cards, definition.shuffle(), legacyReloadSeed(persisted, spellsHash));
            discard = List.of();
        }

        boolean reset = persisted.getInt(SPELLS_HASH_KEY) != spellsHash || persisted.contains(DRAW_INDEX_KEY, NbtElement.NUMBER_TYPE)
            || (deck.isEmpty() && discard.isEmpty() && !cards.isEmpty());
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

    /**
     * Legacy migration may initialize NBT while decoding. HUD, validation, and
     * transaction revalidation therefore use this copy-only entry point until a
     * cast has been explicitly committed.
     */
    public static LoadedWand readReadOnly(ItemStack wandStack, NoitaWandItem wandItem, long now, long randomSeed) {
        Objects.requireNonNull(wandStack, "wandStack");
        return read(wandStack.copy(), Objects.requireNonNull(wandItem, "wandItem"), now, randomSeed);
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
        // G02 writes a Deck that is already ordered/shuffled by the accepting
        // evaluator. Cooldown expiry must only release time, never reshuffle it.
        persisted.putBoolean(G02_RELOAD_PREPARED_KEY, true);
        writeCooldown(persisted, CAST_DELAY_START_TICK_KEY, NEXT_CAST_TICK_KEY, next.castDelayRemaining(), now);
        writeCooldown(persisted, RECHARGE_START_TICK_KEY, RECHARGE_END_TICK_KEY, next.rechargeRemaining(), now);
        applyRemainingUses(loaded.spellStacks(), legacyState.remainingUses());
        NoitaWandItem.setSpellStacks(wandStack, loaded.spellStacks());
    }

    public static boolean canCast(ItemStack wandStack, long now) {
        NbtCompound state = getOrCreateCastState(wandStack);
        return state != null && state.getLong(RECHARGE_END_TICK_KEY) <= now && state.getLong(NEXT_CAST_TICK_KEY) <= now;
    }

    /** Same cooldown check as {@link #canCast(ItemStack, long)} without mutating the source stack. */
    public static boolean canCastReadOnly(ItemStack wandStack, long now) {
        Objects.requireNonNull(wandStack, "wandStack");
        return canCast(wandStack.copy(), now);
    }

    /**
     * Reads only the persisted revision used to reject stale C2S intents. This
     * deliberately does not invoke schema migration: packet and HUD polling
     * must never rewrite a live stack before a transaction commits.
     */
    public static long stateRevisionReadOnly(ItemStack wandStack) {
        Objects.requireNonNull(wandStack, "wandStack");
        NbtCompound root = wandStack.getNbt();
        if (root == null || !root.contains(CAST_STATE_KEY, NbtElement.COMPOUND_TYPE)) {
            return 0L;
        }
        return Math.max(0L, root.getCompound(CAST_STATE_KEY).getLong(REVISION_KEY));
    }

    /**
     * Returns a SHA-256 digest of the exact logical NBT tree without invoking
     * legacy migration or allocating a writable tag on the source stack. Keys
     * are sorted recursively because {@link NbtCompound} insertion order is
     * not a valid server binding contract.
     */
    public static String canonicalNbtStateHash(NbtCompound nbt) {
        MessageDigest digest = sha256();
        if (nbt == null) {
            updateByte(digest, (byte) 0);
        } else {
            updateByte(digest, (byte) 1);
            appendCanonicalNbt(digest, nbt);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /** GUI edits invalidate prepared cast bindings even when spell IDs remain in the same slots. */
    public static long incrementStateRevision(ItemStack wandStack) {
        NbtCompound state = getOrCreateCastState(Objects.requireNonNull(wandStack, "wandStack"));
        if (state == null) {
            throw new IllegalStateException("wand cast state failed persistence validation");
        }
        long current = Math.max(0L, state.getLong(REVISION_KEY));
        long next = current == Long.MAX_VALUE ? Long.MAX_VALUE : current + 1L;
        state.putLong(REVISION_KEY, next);
        return next;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            // SHA-256 is mandatory for every supported Java runtime. Keeping
            // this failure explicit prevents a weaker implicit fallback from
            // silently weakening a server-side cast binding.
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void appendCanonicalNbt(MessageDigest digest, NbtElement element) {
        updateByte(digest, element.getType());
        switch (element.getType()) {
            case NbtElement.END_TYPE -> {
                // End is only a structural marker and carries no payload.
            }
            case NbtElement.BYTE_TYPE -> updateByte(digest, ((NbtByte) element).byteValue());
            case NbtElement.SHORT_TYPE -> updateShort(digest, ((NbtShort) element).shortValue());
            case NbtElement.INT_TYPE -> updateInt(digest, ((NbtInt) element).intValue());
            case NbtElement.LONG_TYPE -> updateLong(digest, ((NbtLong) element).longValue());
            case NbtElement.FLOAT_TYPE -> updateInt(digest, Float.floatToRawIntBits(((NbtFloat) element).floatValue()));
            case NbtElement.DOUBLE_TYPE -> updateLong(digest, Double.doubleToRawLongBits(((NbtDouble) element).doubleValue()));
            case NbtElement.BYTE_ARRAY_TYPE -> appendByteArray(digest, ((NbtByteArray) element).getByteArray());
            case NbtElement.STRING_TYPE -> updateString(digest, ((NbtString) element).asString());
            case NbtElement.LIST_TYPE -> appendList(digest, (NbtList) element);
            case NbtElement.COMPOUND_TYPE -> appendCompound(digest, (NbtCompound) element);
            case NbtElement.INT_ARRAY_TYPE -> appendIntArray(digest, ((NbtIntArray) element).getIntArray());
            case NbtElement.LONG_ARRAY_TYPE -> appendLongArray(digest, ((NbtLongArray) element).getLongArray());
            default -> throw new IllegalArgumentException("unsupported NBT element type " + element.getType());
        }
    }

    private static void appendCompound(MessageDigest digest, NbtCompound compound) {
        List<String> keys = new ArrayList<>(compound.getKeys());
        Collections.sort(keys);
        updateInt(digest, keys.size());
        for (String key : keys) {
            updateString(digest, key);
            appendCanonicalNbt(digest, Objects.requireNonNull(compound.get(key), "compound element"));
        }
    }

    private static void appendList(MessageDigest digest, NbtList list) {
        updateByte(digest, list.getHeldType());
        updateInt(digest, list.size());
        for (int index = 0; index < list.size(); index++) {
            appendCanonicalNbt(digest, list.get(index));
        }
    }

    private static void appendByteArray(MessageDigest digest, byte[] values) {
        updateInt(digest, values.length);
        digest.update(values);
    }

    private static void appendIntArray(MessageDigest digest, int[] values) {
        updateInt(digest, values.length);
        for (int value : values) {
            updateInt(digest, value);
        }
    }

    private static void appendLongArray(MessageDigest digest, long[] values) {
        updateInt(digest, values.length);
        for (long value : values) {
            updateLong(digest, value);
        }
    }

    private static void updateString(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        updateInt(digest, bytes.length);
        digest.update(bytes);
    }

    private static void updateByte(MessageDigest digest, byte value) {
        digest.update(value);
    }

    private static void updateShort(MessageDigest digest, short value) {
        updateByte(digest, (byte) (value >>> 8));
        updateByte(digest, (byte) value);
    }

    private static void updateInt(MessageDigest digest, int value) {
        updateByte(digest, (byte) (value >>> 24));
        updateByte(digest, (byte) (value >>> 16));
        updateByte(digest, (byte) (value >>> 8));
        updateByte(digest, (byte) value);
    }

    private static void updateLong(MessageDigest digest, long value) {
        updateByte(digest, (byte) (value >>> 56));
        updateByte(digest, (byte) (value >>> 48));
        updateByte(digest, (byte) (value >>> 40));
        updateByte(digest, (byte) (value >>> 32));
        updateByte(digest, (byte) (value >>> 24));
        updateByte(digest, (byte) (value >>> 16));
        updateByte(digest, (byte) (value >>> 8));
        updateByte(digest, (byte) value);
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

    private static List<CardRef> ordered(
        Iterable<CardRef> refs,
        Map<CardRef, SpellCardState> cards,
        boolean shuffle,
        long seed,
        String domain
    ) {
        List<CardRef> ordered = new ArrayList<>();
        for (CardRef ref : refs) {
            ordered.add(ref);
        }
        ordered.sort((left, right) -> Integer.compare(cards.get(left).slot(), cards.get(right).slot()));
        if (shuffle) {
            CastRng rng = new CastRng(seed);
            for (int index = ordered.size() - 1; index > 0; index--) {
                int other = rng.nextInt("shuffle:" + domain, index + 1);
                Collections.swap(ordered, index, other);
            }
        }
        return List.copyOf(ordered);
    }

    /** Visible to the adapter regression test without exposing Minecraft NBT to the pure evaluator. */
    static List<CardRef> migrateG01ReloadDeck(
        List<CardRef> g01Discard,
        Map<CardRef, SpellCardState> cards,
        boolean shuffle,
        long migrationSeed
    ) {
        return ordered(g01Discard, cards, shuffle, migrationSeed, "g01-reload-migration");
    }

    private static long legacyReloadSeed(NbtCompound state, int spellsHash) {
        long revision = Math.max(0L, state.getLong(REVISION_KEY));
        return ((long) spellsHash << 32) ^ revision ^ 0x474F3132L;
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

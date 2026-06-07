package com.mcnoita.wand;

import com.mcnoita.entity.BombEntity;
import com.mcnoita.entity.SparkBoltProjectileEntity;
import com.mcnoita.item.ModItems;
import com.mcnoita.item.NoitaSpellItem;
import com.mcnoita.item.NoitaWandItem;
import com.mcnoita.spell.NoitaProjectilePayload;
import com.mcnoita.spell.NoitaSpellTemplate;
import com.mcnoita.spell.NoitaSpellTriggerMode;
import com.mcnoita.spell.NoitaSpellType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.Item;
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
    private static final String DISCARDED_KEY = "Discarded";
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
        List<ItemStack> alwaysCastSpells = getAlwaysCastSpellStacks(wandTemplate);
        if (spellSlots.isEmpty() && alwaysCastSpells.isEmpty()) {
            resetEmptyState(state);
            return;
        }

        int spellsHash = getSpellsHash(configuredSpells, wandTemplate);
        if (state.getInt(SPELLS_HASH_KEY) != spellsHash || state.contains(DRAW_INDEX_KEY, NbtElement.NUMBER_TYPE)) {
            resetDeck(state, spellSlots, wandTemplate.shuffle(), player.getRandom().nextLong(), spellsHash);
            resetTiming(state);
        } else if (readDeck(state).isEmpty() && readDiscarded(state).isEmpty() && !spellSlots.isEmpty()) {
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

        CastContext context = new CastContext(
            player,
            wandTemplate,
            configuredSpells,
            readDeck(state),
            readDiscarded(state),
            currentMana,
            !spellSlots.isEmpty()
        );
        for (ItemStack alwaysCastSpell : alwaysCastSpells) {
            context.executePermanentSpell(alwaysCastSpell);
        }
        context.drawActions(wandTemplate.spellsPerCast(), false);
        context.moveHandToDiscarded();

        writeDeck(state, context.deck());
        writeDiscarded(state, context.discarded());
        writeMana(state, now, wandTemplate, context.mana());

        if (!context.castAnySpell()) {
            startCastDelay(state, now, MIN_CAST_DELAY_TICKS);
            if (context.shouldRecharge()) {
                startRecharge(state, now, wandTemplate);
            }
            if (context.consumeLimitedUseSpells()) {
                NoitaWandItem.setSpellStacks(wandStack, configuredSpells);
            }
            return;
        }

        startCastDelay(state, now, secondsToTicks(context.castDelaySeconds(), MIN_CAST_DELAY_TICKS));
        if (context.shouldRecharge()) {
            startRecharge(state, now, wandTemplate.rechargeTimeSeconds() + context.rechargeTimeSeconds());
        }

        if (context.consumeLimitedUseSpells()) {
            NoitaWandItem.setSpellStacks(wandStack, configuredSpells);
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

    private static void resetEmptyState(NbtCompound state) {
        resetTiming(state);
        writeDeck(state, List.of());
        writeDiscarded(state, List.of());
        state.putInt(SPELLS_HASH_KEY, 0);
        state.remove(DRAW_INDEX_KEY);
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
        orderDeck(deck, shuffle, randomSeed);

        writeDeck(state, deck);
        writeDiscarded(state, List.of());
        state.putInt(SPELLS_HASH_KEY, spellsHash);
        state.remove(DRAW_INDEX_KEY);
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

    private static void writeMana(NbtCompound state, long now, NoitaWandTemplate wandTemplate, float mana) {
        state.putFloat(CURRENT_MANA_KEY, clampMana(mana, wandTemplate.manaMax()));
        state.putLong(LAST_MANA_TICK_KEY, now);
    }

    private static float clampMana(float mana, int manaMax) {
        return Math.max(0.0f, Math.min(manaMax, mana));
    }

    private static List<Integer> getConfiguredSpellSlots(DefaultedList<ItemStack> configuredSpells) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < configuredSpells.size(); slot++) {
            ItemStack spellStack = configuredSpells.get(slot);
            if (!spellStack.isEmpty() && spellStack.getItem() instanceof NoitaSpellItem && NoitaSpellItem.hasUsesRemaining(spellStack)) {
                slots.add(slot);
            }
        }

        return slots;
    }

    private static List<ItemStack> getAlwaysCastSpellStacks(NoitaWandTemplate wandTemplate) {
        List<ItemStack> spellStacks = new ArrayList<>();
        for (Identifier spellId : wandTemplate.alwaysCastSpells()) {
            Item item = Registries.ITEM.get(spellId);
            if (item instanceof NoitaSpellItem) {
                spellStacks.add(new ItemStack(item));
            }
        }
        return spellStacks;
    }

    private static int getSpellsHash(DefaultedList<ItemStack> configuredSpells, NoitaWandTemplate wandTemplate) {
        int hash = 1;
        for (int i = 0; i < configuredSpells.size(); i++) {
            ItemStack spellStack = configuredSpells.get(i);
            Identifier id = Registries.ITEM.getId(spellStack.getItem());
            hash = 31 * hash + i;
            hash = 31 * hash + (spellStack.isEmpty() ? 0 : id.hashCode());
            hash = 31 * hash + spellStack.getCount();
        }
        hash = 31 * hash + wandTemplate.alwaysCastSpells().hashCode();
        return hash;
    }

    private static List<Integer> readDeck(NbtCompound state) {
        return readIntList(state, DECK_KEY);
    }

    private static List<Integer> readDiscarded(NbtCompound state) {
        return readIntList(state, DISCARDED_KEY);
    }

    private static List<Integer> readIntList(NbtCompound state, String key) {
        NbtList nbtList = state.getList(key, NbtElement.INT_TYPE);
        List<Integer> values = new ArrayList<>(nbtList.size());
        for (int i = 0; i < nbtList.size(); i++) {
            values.add(nbtList.getInt(i));
        }

        return values;
    }

    private static void writeDeck(NbtCompound state, List<Integer> deck) {
        writeIntList(state, DECK_KEY, deck);
    }

    private static void writeDiscarded(NbtCompound state, List<Integer> discarded) {
        writeIntList(state, DISCARDED_KEY, discarded);
    }

    private static void writeIntList(NbtCompound state, String key, List<Integer> values) {
        NbtList nbtList = new NbtList();
        for (int value : values) {
            nbtList.add(NbtInt.of(value));
        }
        state.put(key, nbtList);
    }

    private static void orderDeck(List<Integer> deck, boolean shuffle, long randomSeed) {
        if (shuffle) {
            Collections.shuffle(deck, new java.util.Random(randomSeed));
        } else {
            Collections.sort(deck);
        }
    }

    private static void spawnSparkBolt(
        ServerPlayerEntity player,
        NoitaWandTemplate wandTemplate,
        NoitaSpellTemplate spellTemplate,
        List<NoitaProjectilePayload> triggerPayloads
    ) {
        ServerWorld world = player.getServerWorld();
        SparkBoltProjectileEntity sparkBolt = new SparkBoltProjectileEntity(
            world,
            player,
            spellTemplate.damage(),
            spellTemplate.criticalChancePercent(),
            spellTemplate.lifetimeTicks(),
            spellTemplate.trailLightStacks(),
            spellTemplate.triggerMode(),
            spellTemplate.triggerDelayTicks(),
            triggerPayloads
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

    private static void spawnBomb(
        ServerPlayerEntity player,
        NoitaWandTemplate wandTemplate,
        NoitaSpellTemplate spellTemplate,
        List<NoitaProjectilePayload> triggerPayloads
    ) {
        ServerWorld world = player.getServerWorld();
        BombEntity bomb = new BombEntity(
            world,
            player,
            spellTemplate.explosionRadius(),
            spellTemplate.lifetimeTicks(),
            triggerPayloads
        );
        Vec3d spawnPosition = getSpellSpawnPosition(player);
        Vec3d direction = player.getRotationVec(1.0f);
        bomb.setPosition(spawnPosition);
        float speed = getArrowSpeed(wandTemplate, spellTemplate);
        bomb.setVelocity(
            direction.x * speed,
            direction.y * speed,
            direction.z * speed
        );

        world.spawnEntity(bomb);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.PLAYERS, 0.8f, 1.0f);
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
        return spellStack.isOf(ModItems.SPARK_BOLT)
            || spellStack.isOf(ModItems.SPARK_BOLT_TRIGGER)
            || spellStack.isOf(ModItems.SPARK_BOLT_TIMER)
            || spellStack.isOf(ModItems.BOUNCING_BURST)
            || spellStack.isOf(ModItems.LIGHT_BULLET)
            || spellStack.isOf(ModItems.BOMB)
            || spellStack.isOf(ModItems.BOMB_DEATH_TRIGGER);
    }

    private static boolean isBombSpell(ItemStack spellStack) {
        return spellStack.isOf(ModItems.BOMB) || spellStack.isOf(ModItems.BOMB_DEATH_TRIGGER);
    }

    private static int secondsToTicks(float seconds, int minimumTicks) {
        return Math.max(minimumTicks, Math.round(Math.max(0.0f, seconds) * 20.0f));
    }

    private static final class CastContext {
        private final ServerPlayerEntity player;
        private final NoitaWandTemplate wandTemplate;
        private final DefaultedList<ItemStack> configuredSpells;
        private final List<Integer> deck;
        private final List<Integer> discarded;
        private final List<Integer> hand = new ArrayList<>();
        private final List<Integer> slotsToConsume = new ArrayList<>();
        private final boolean hasRegularDeck;

        private CastState castState = CastState.EMPTY;
        private float mana;
        private float castDelaySeconds;
        private float rechargeTimeSeconds;
        private boolean castAnySpell;
        private boolean reloading;
        private boolean startReload;
        private int permanentCardDepth;
        private int copiedActionDepth;
        private List<NoitaProjectilePayload> payloadSink;

        private CastContext(
            ServerPlayerEntity player,
            NoitaWandTemplate wandTemplate,
            DefaultedList<ItemStack> configuredSpells,
            List<Integer> deck,
            List<Integer> discarded,
            float mana,
            boolean hasRegularDeck
        ) {
            this.player = player;
            this.wandTemplate = wandTemplate;
            this.configuredSpells = configuredSpells;
            this.deck = new ArrayList<>(deck);
            this.discarded = new ArrayList<>(discarded);
            this.mana = mana;
            this.hasRegularDeck = hasRegularDeck;
            this.castDelaySeconds = wandTemplate.castDelaySeconds();
        }

        private void executePermanentSpell(ItemStack spellStack) {
            permanentCardDepth++;
            executeSpell(spellStack, -1, true);
            permanentCardDepth--;
        }

        private void drawActions(int howMany, boolean instantReloadIfEmpty) {
            if (permanentCardDepth > 0 && howMany == 1) {
                return;
            }

            for (int i = 0; i < howMany; i++) {
                boolean ok = drawAction(instantReloadIfEmpty);
                if (!ok) {
                    while (!deck.isEmpty()) {
                        if (drawAction(instantReloadIfEmpty)) {
                            break;
                        }
                    }
                }

                if (reloading) {
                    return;
                }
            }
        }

        private boolean drawAction(boolean instantReloadIfEmpty) {
            if (deck.isEmpty()) {
                if (instantReloadIfEmpty && !discarded.isEmpty()) {
                    deck.addAll(discarded);
                    discarded.clear();
                    orderDeck(deck, wandTemplate.shuffle(), player.getRandom().nextLong());
                    startReload = true;
                } else {
                    reloading = true;
                    return true;
                }
            }

            if (deck.isEmpty()) {
                return true;
            }

            int slot = deck.remove(0);
            if (slot < 0 || slot >= configuredSpells.size()) {
                return false;
            }

            ItemStack spellStack = configuredSpells.get(slot);
            if (spellStack.isEmpty() || !(spellStack.getItem() instanceof NoitaSpellItem spellItem)) {
                return false;
            }
            if (!NoitaSpellItem.hasUsesRemaining(spellStack)) {
                discarded.add(slot);
                return false;
            }

            NoitaSpellTemplate template = spellItem.getTemplate();
            int manaRequired = template.manaDrain();
            if (manaRequired > mana) {
                discarded.add(slot);
                return false;
            }

            mana -= manaRequired;
            hand.add(slot);
            executeSpell(spellStack.copy(), slot, false);
            return true;
        }

        private void executeSpell(ItemStack spellStack, int slot, boolean permanent) {
            if (!(spellStack.getItem() instanceof NoitaSpellItem spellItem)) {
                return;
            }

            NoitaSpellTemplate template = spellItem.getTemplate();
            if (template.type() == NoitaSpellType.PROJECTILE_MODIFIER) {
                if (permanent) {
                    castState = castState.add(template);
                    drawActions(1, true);
                    return;
                }

                CastState previousState = castState;
                castState = castState.add(template);
                drawActions(1, true);
                castState = previousState;
                return;
            }

            if (template.type() == NoitaSpellType.MULTICAST) {
                drawActions(template.drawCount(), true);
                return;
            }

            if (spellStack.isOf(ModItems.DUPLICATE)) {
                duplicateCurrentHand(template);
                return;
            }

            if (spellStack.isOf(ModItems.ALPHA)) {
                copyFirstAvailableSpell(template);
                return;
            }

            if (spellStack.isOf(ModItems.GAMMA)) {
                copyLastAvailableSpell(template);
                return;
            }

            if (spellStack.isOf(ModItems.WAND_REFRESH)) {
                refreshWand(template);
                return;
            }

            if (!isProjectileSpell(spellStack)) {
                return;
            }

            NoitaSpellTemplate resolvedTemplate = castState.applyTo(template);
            List<NoitaProjectilePayload> triggerPayloads = resolveTriggerPayload(resolvedTemplate);
            if (payloadSink != null) {
                payloadSink.add(toPayload(spellStack, resolvedTemplate, triggerPayloads));
                if (slot >= 0) {
                    slotsToConsume.add(slot);
                }
                return;
            }

            if (isBombSpell(spellStack)) {
                spawnBomb(player, wandTemplate, resolvedTemplate, triggerPayloads);
            } else {
                spawnSparkBolt(player, wandTemplate, resolvedTemplate, triggerPayloads);
            }

            castDelaySeconds += resolvedTemplate.castDelaySeconds();
            rechargeTimeSeconds += resolvedTemplate.rechargeTimeSeconds();
            castAnySpell = true;
            if (slot >= 0) {
                slotsToConsume.add(slot);
            }
        }

        private List<NoitaProjectilePayload> resolveTriggerPayload(NoitaSpellTemplate template) {
            if (template.triggerMode() == NoitaSpellTriggerMode.NONE || template.triggerDrawCount() <= 0) {
                return List.of();
            }

            CastState previousCastState = castState;
            List<NoitaProjectilePayload> previousPayloadSink = payloadSink;
            float previousCastDelaySeconds = castDelaySeconds;
            float previousRechargeTimeSeconds = rechargeTimeSeconds;
            boolean previousCastAnySpell = castAnySpell;
            boolean previousReloading = reloading;

            List<NoitaProjectilePayload> payloads = new ArrayList<>();
            castState = CastState.EMPTY;
            payloadSink = payloads;
            drawActions(template.triggerDrawCount(), true);

            castState = previousCastState;
            payloadSink = previousPayloadSink;
            castDelaySeconds = previousCastDelaySeconds;
            rechargeTimeSeconds = previousRechargeTimeSeconds;
            castAnySpell = previousCastAnySpell;
            reloading = previousReloading;
            return List.copyOf(payloads);
        }

        private void duplicateCurrentHand(NoitaSpellTemplate template) {
            List<Integer> handSnapshot = List.copyOf(hand);
            for (int slot : handSnapshot) {
                if (!isCopyableSlot(slot) || configuredSpells.get(slot).isOf(ModItems.DUPLICATE)) {
                    continue;
                }
                copySpell(slot);
            }

            castDelaySeconds += template.castDelaySeconds();
            rechargeTimeSeconds += template.rechargeTimeSeconds();
            drawActions(1, true);
        }

        private void copyFirstAvailableSpell(NoitaSpellTemplate template) {
            castDelaySeconds += template.castDelaySeconds();
            copySpell(findFirstCopyTarget());
        }

        private void copyLastAvailableSpell(NoitaSpellTemplate template) {
            castDelaySeconds += template.castDelaySeconds();
            copySpell(findLastCopyTarget());
        }

        private int findFirstCopyTarget() {
            for (int slot : discarded) {
                if (isCopyableSlot(slot)) {
                    return slot;
                }
            }
            for (int slot : hand) {
                if (isCopyableSlot(slot)) {
                    return slot;
                }
            }
            for (int slot : deck) {
                if (isCopyableSlot(slot)) {
                    return slot;
                }
            }
            return -1;
        }

        private int findLastCopyTarget() {
            for (int i = deck.size() - 1; i >= 0; i--) {
                int slot = deck.get(i);
                if (isCopyableSlot(slot)) {
                    return slot;
                }
            }
            for (int i = hand.size() - 1; i >= 0; i--) {
                int slot = hand.get(i);
                if (isCopyableSlot(slot)) {
                    return slot;
                }
            }
            return -1;
        }

        private boolean isCopyableSlot(int slot) {
            return slot >= 0
                && slot < configuredSpells.size()
                && !configuredSpells.get(slot).isEmpty()
                && configuredSpells.get(slot).getItem() instanceof NoitaSpellItem;
        }

        private void copySpell(int slot) {
            if (!isCopyableSlot(slot) || copiedActionDepth >= 2) {
                return;
            }

            copiedActionDepth++;
            executeSpell(configuredSpells.get(slot).copy(), -1, false);
            copiedActionDepth--;
        }

        private void refreshWand(NoitaSpellTemplate template) {
            rechargeTimeSeconds += template.rechargeTimeSeconds();
            addSlotsToDiscarded(hand);
            addSlotsToDiscarded(deck);
            hand.clear();
            deck.clear();

            if (!discarded.isEmpty()) {
                deck.addAll(discarded);
                discarded.clear();
                orderDeck(deck, wandTemplate.shuffle(), player.getRandom().nextLong());
            }
            reloading = false;
        }

        private void addSlotsToDiscarded(List<Integer> slots) {
            for (int slot : slots) {
                if (slot >= 0 && slot < configuredSpells.size() && !discarded.contains(slot) && NoitaSpellItem.hasUsesRemaining(configuredSpells.get(slot))) {
                    discarded.add(slot);
                }
            }
        }

        private NoitaProjectilePayload toPayload(ItemStack spellStack, NoitaSpellTemplate template, List<NoitaProjectilePayload> triggerPayloads) {
            return new NoitaProjectilePayload(
                isBombSpell(spellStack) ? NoitaProjectilePayload.ProjectileKind.BOMB : NoitaProjectilePayload.ProjectileKind.SPARK_BOLT,
                template.damage(),
                template.criticalChancePercent(),
                template.lifetimeTicks(),
                template.trailLightStacks(),
                template.explosionRadius(),
                getArrowSpeed(wandTemplate, template),
                getDivergence(wandTemplate, template),
                template.triggerMode(),
                template.triggerDelayTicks(),
                triggerPayloads
            );
        }

        private void moveHandToDiscarded() {
            for (int slot : hand) {
                if (slot >= 0 && slot < configuredSpells.size() && NoitaSpellItem.hasUsesRemaining(configuredSpells.get(slot))) {
                    discarded.add(slot);
                }
            }
            hand.clear();
        }

        private boolean consumeLimitedUseSpells() {
            if (!castAnySpell) {
                return false;
            }

            boolean consumedAny = false;
            for (int slot : slotsToConsume) {
                if (slot >= 0 && slot < configuredSpells.size()) {
                    ItemStack spellStack = configuredSpells.get(slot);
                    int remainingBefore = NoitaSpellItem.getRemainingUses(spellStack);
                    NoitaSpellItem.consumeUse(spellStack);
                    consumedAny |= NoitaSpellItem.getRemainingUses(spellStack) != remainingBefore;
                }
            }
            return consumedAny;
        }

        private List<Integer> deck() {
            return deck;
        }

        private List<Integer> discarded() {
            return discarded;
        }

        private float mana() {
            return mana;
        }

        private float castDelaySeconds() {
            return castDelaySeconds;
        }

        private float rechargeTimeSeconds() {
            return rechargeTimeSeconds;
        }

        private boolean castAnySpell() {
            return castAnySpell;
        }

        private boolean shouldRecharge() {
            return hasRegularDeck && (startReload || deck.isEmpty());
        }
    }

    private record CastState(
        float damage,
        float explosionRadius,
        float spreadModifierDegrees,
        float speedMultiplier,
        float castDelaySeconds,
        float rechargeTimeSeconds,
        float criticalChancePercent,
        int lifetimeModifierTicks,
        float recoil,
        boolean piercing,
        boolean friendlyFire,
        int trailLightStacks
    ) {
        private static final CastState EMPTY = new CastState(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0, 0.0f, false, false, 0);

        private CastState add(NoitaSpellTemplate modifier) {
            return new CastState(
                damage + modifier.damage(),
                explosionRadius + modifier.explosionRadius(),
                spreadModifierDegrees + modifier.spreadDegrees() + modifier.spreadModifierDegrees(),
                speedMultiplier * modifier.speedMultiplier(),
                castDelaySeconds + modifier.castDelaySeconds(),
                rechargeTimeSeconds + modifier.rechargeTimeSeconds(),
                criticalChancePercent + modifier.criticalChancePercent(),
                lifetimeModifierTicks + modifier.lifetimeModifierTicks(),
                recoil + modifier.recoil(),
                piercing || modifier.piercing(),
                friendlyFire || modifier.friendlyFire(),
                trailLightStacks + modifier.trailLightStacks()
            );
        }

        private NoitaSpellTemplate applyTo(NoitaSpellTemplate projectile) {
            int lifetimeTicks = Math.max(1, projectile.lifetimeTicks() + lifetimeModifierTicks);
            if (projectile.maxLifetimeTicks() > 0) {
                lifetimeTicks = Math.min(lifetimeTicks, projectile.maxLifetimeTicks());
            }

            return NoitaSpellTemplate.builder()
                .type(projectile.type())
                .maxUses(projectile.maxUses())
                .manaDrain(projectile.manaDrain())
                .damage(Math.max(0.0f, projectile.damage() + damage))
                .explosionRadius(Math.max(0.0f, projectile.explosionRadius() + explosionRadius))
                .spreadDegrees(projectile.spreadDegrees())
                .speed(projectile.speed())
                .castDelaySeconds(projectile.castDelaySeconds() + castDelaySeconds)
                .rechargeTimeSeconds(projectile.rechargeTimeSeconds() + rechargeTimeSeconds)
                .spreadModifierDegrees(projectile.spreadModifierDegrees() + spreadModifierDegrees)
                .speedMultiplier(projectile.speedMultiplier() * speedMultiplier)
                .criticalChancePercent(projectile.criticalChancePercent() + criticalChancePercent)
                .lifetimeTicks(lifetimeTicks)
                .maxLifetimeTicks(projectile.maxLifetimeTicks())
                .lifetimeModifierTicks(projectile.lifetimeModifierTicks() + lifetimeModifierTicks)
                .recoil(projectile.recoil() + recoil)
                .piercing(projectile.piercing() || piercing)
                .friendlyFire(projectile.friendlyFire() || friendlyFire)
                .trailLightStacks(projectile.trailLightStacks() + trailLightStacks)
                .drawCount(projectile.drawCount())
                .triggerMode(projectile.triggerMode())
                .triggerDrawCount(projectile.triggerDrawCount())
                .triggerDelayTicks(projectile.triggerDelayTicks())
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

package com.mcnoita.item;

import com.mcnoita.screen.NoitaWandScreenHandler;
import com.mcnoita.persistence.NoitaNbtLimits;
import com.mcnoita.persistence.NoitaNbtSafety;
import com.mcnoita.persistence.NoitaNbtSchema;
import com.mcnoita.wand.NoitaWandTemplate;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class NoitaWandItem extends Item {
    private static final String TEMPLATE_KEY = "NoitaWandTemplate";
    private static final String LEGACY_SPELLS_KEY = "NoitaWandSpells";
    private static final String SPELLS_KEY = "NoitaWandSlots";
    private static final String SLOT_ENTRIES_KEY = "Entries";
    private static final String SLOT_KEY = "Slot";

    private final NoitaWandTemplate defaultTemplate;

    public NoitaWandItem(NoitaWandTemplate defaultTemplate, Settings settings) {
        super(settings);
        this.defaultTemplate = defaultTemplate;
    }

    @Override
    public ItemStack getDefaultStack() {
        ItemStack stack = super.getDefaultStack();
        setTemplate(stack, defaultTemplate);
        return stack;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.openHandledScreen(new ExtendedScreenHandlerFactory() {
                @Override
                public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
                    buf.writeEnumConstant(hand);
                }

                @Override
                public Text getDisplayName() {
                    return Text.translatable("screen.mc-noita.wand_editor");
                }

                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                    return new NoitaWandScreenHandler(syncId, playerInventory, hand);
                }
            });
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    public NoitaWandTemplate getTemplate(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(TEMPLATE_KEY)) {
            return defaultTemplate;
        }

        return NoitaWandTemplate.tryFromNbt(nbt.getCompound(TEMPLATE_KEY)).orElse(defaultTemplate);
    }

    public static void setTemplate(ItemStack stack, NoitaWandTemplate template) {
        stack.getOrCreateNbt().put(TEMPLATE_KEY, template.toNbt());
    }

    public static DefaultedList<ItemStack> getSpellStacks(ItemStack wandStack, int capacity) {
        int safeCapacity = Math.min(NoitaNbtLimits.MAX_WAND_CAPACITY, Math.max(1, capacity));
        DefaultedList<ItemStack> spells = DefaultedList.ofSize(safeCapacity, ItemStack.EMPTY);
        NbtCompound nbt = wandStack.getNbt();
        if (nbt == null) {
            return spells;
        }

        NbtCompound slotData = getSlotData(nbt);
        if (slotData == null
            || !NoitaNbtSchema.migrateToCurrent(slotData, NoitaNbtSchema.Kind.WAND_SLOTS)
            || !NoitaNbtSafety.validateTree(slotData, 16, 2048, NoitaNbtLimits.MAX_WAND_SLOT_ENTRIES)) {
            return spells;
        }
        NbtList spellList = slotData.getList(SLOT_ENTRIES_KEY, NbtElement.COMPOUND_TYPE);
        if (spellList.size() > NoitaNbtLimits.MAX_WAND_SLOT_ENTRIES
            || !NoitaNbtSafety.hasUniqueBoundedSlots(spellList, safeCapacity)) {
            return spells;
        }
        for (int i = 0; i < spellList.size(); i++) {
            NbtCompound spellNbt = spellList.getCompound(i);
            int slot = spellNbt.getInt(SLOT_KEY);
            if (slot >= 0 && slot < safeCapacity) {
                try {
                    ItemStack spellStack = ItemStack.fromNbt(spellNbt);
                    if (isSpellStack(spellStack)) {
                        spells.set(slot, spellStack);
                    }
                } catch (RuntimeException ignored) {
                    // Invalid item data is ignored rather than crashing an inventory load.
                }
            }
        }

        return spells;
    }

    public static void setSpellStacks(ItemStack wandStack, DefaultedList<ItemStack> spells) {
        NbtList spellList = new NbtList();

        for (int slot = 0; slot < spells.size(); slot++) {
            ItemStack spellStack = spells.get(slot);
            if (!spellStack.isEmpty() && isSpellStack(spellStack)) {
                NbtCompound spellNbt = new NbtCompound();
                spellStack.writeNbt(spellNbt);
                spellNbt.putInt(SLOT_KEY, slot);
                spellList.add(spellNbt);
            }
        }

        NbtCompound slotData = new NbtCompound();
        NoitaNbtSchema.writeCurrentVersion(slotData);
        slotData.put(SLOT_ENTRIES_KEY, spellList);
        NbtCompound nbt = wandStack.getOrCreateNbt();
        nbt.put(SPELLS_KEY, slotData);
        nbt.remove(LEGACY_SPELLS_KEY);
    }

    public static boolean isSpellStack(ItemStack stack) {
        return stack.getItem() instanceof NoitaSpellItem;
    }

    public boolean hasSupportedNbt(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) {
            return true;
        }
        if (nbt.contains(TEMPLATE_KEY, NbtElement.COMPOUND_TYPE)
            && NoitaWandTemplate.tryFromNbt(nbt.getCompound(TEMPLATE_KEY)).isEmpty()) {
            return false;
        }
        NbtCompound slotData = getSlotData(nbt);
        return slotData == null
            || (NoitaNbtSchema.migrateToCurrent(slotData, NoitaNbtSchema.Kind.WAND_SLOTS)
                && NoitaNbtSafety.validateTree(slotData, 16, 2048, NoitaNbtLimits.MAX_WAND_SLOT_ENTRIES));
    }

    private static NbtCompound getSlotData(NbtCompound root) {
        if (root.contains(SPELLS_KEY, NbtElement.COMPOUND_TYPE)) {
            return root.getCompound(SPELLS_KEY);
        }
        if (!root.contains(LEGACY_SPELLS_KEY, NbtElement.LIST_TYPE)) {
            return null;
        }

        // v0 stored the entry list directly on the wand root. Move it into the
        // versioned slot structure before decoding so future migrations stay central.
        NbtCompound slotData = new NbtCompound();
        slotData.put(SLOT_ENTRIES_KEY, root.getList(LEGACY_SPELLS_KEY, NbtElement.COMPOUND_TYPE).copy());
        root.put(SPELLS_KEY, slotData);
        root.remove(LEGACY_SPELLS_KEY);
        return slotData;
    }

}

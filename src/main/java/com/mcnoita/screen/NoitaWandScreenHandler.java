package com.mcnoita.screen;

import com.mcnoita.item.NoitaWandItem;
import com.mcnoita.wand.NoitaWandTemplate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;

public class NoitaWandScreenHandler extends ScreenHandler {
    public static final int BACKGROUND_WIDTH = 224;
    public static final int SPELL_SLOTS_PER_ROW = 9;
    public static final int SLOT_SIZE = 18;
    private static final int SLOT_GRID_WIDTH = SPELL_SLOTS_PER_ROW * SLOT_SIZE;
    public static final int WAND_DESCRIPTION_START_Y = 18;
    public static final int WAND_DESCRIPTION_HEIGHT = 46;
    public static final int SPELL_START_X = (BACKGROUND_WIDTH - SLOT_GRID_WIDTH) / 2;
    public static final int SPELL_START_Y = WAND_DESCRIPTION_START_Y + WAND_DESCRIPTION_HEIGHT + 10;
    public static final int PLAYER_INVENTORY_START_X = SPELL_START_X;

    private static final int PLAYER_INVENTORY_ROWS = 3;
    private static final int PLAYER_INVENTORY_COLUMNS = 9;
    private static final int PLAYER_INVENTORY_GAP = 14;
    private static final int HOTBAR_GAP = 4;
    private static final int BOTTOM_PADDING = 8;

    private final PlayerEntity player;
    private final Hand hand;
    private final ItemStack wandStack;
    private final NoitaWandTemplate wandTemplate;
    private final DefaultedList<ItemStack> spellStacks;
    private final Inventory wandInventory;
    private final int spellSlotCount;
    private final int spellRows;
    private final int playerInventoryY;

    public NoitaWandScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, buf.readEnumConstant(Hand.class));
    }

    public NoitaWandScreenHandler(int syncId, PlayerInventory playerInventory, Hand hand) {
        super(ModScreenHandlers.WAND_EDITOR, syncId);
        this.player = playerInventory.player;
        this.hand = hand;
        this.wandStack = player.getStackInHand(hand);

        this.wandTemplate = this.wandStack.getItem() instanceof NoitaWandItem wandItem
            ? wandItem.getTemplate(this.wandStack)
            : NoitaWandTemplate.builder().build();
        this.spellSlotCount = Math.max(1, this.wandTemplate.capacity());
        this.spellRows = (this.spellSlotCount + SPELL_SLOTS_PER_ROW - 1) / SPELL_SLOTS_PER_ROW;
        this.playerInventoryY = SPELL_START_Y + this.spellRows * SLOT_SIZE + PLAYER_INVENTORY_GAP;
        this.spellStacks = NoitaWandItem.getSpellStacks(this.wandStack, this.spellSlotCount);
        this.wandInventory = new WandSpellInventory();

        addWandSlots();
        addPlayerInventorySlots(playerInventory);
    }

    public int getSpellSlotCount() {
        return spellSlotCount;
    }

    public int getSpellRows() {
        return spellRows;
    }

    public int getPlayerInventoryY() {
        return playerInventoryY;
    }

    public NoitaWandTemplate getWandTemplate() {
        return wandTemplate;
    }

    public static int getBackgroundHeight(int spellRows) {
        int playerInventoryHeight = PLAYER_INVENTORY_ROWS * SLOT_SIZE + HOTBAR_GAP + SLOT_SIZE;
        return SPELL_START_Y + spellRows * SLOT_SIZE + PLAYER_INVENTORY_GAP + playerInventoryHeight + BOTTOM_PADDING;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player.getStackInHand(this.hand) == this.wandStack
            && this.wandStack.getItem() instanceof NoitaWandItem;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasStack()) {
            ItemStack stack = slot.getStack();
            originalStack = stack.copy();

            int wandEnd = this.spellSlotCount;
            int inventoryStart = wandEnd;
            int hotbarStart = inventoryStart + PLAYER_INVENTORY_ROWS * PLAYER_INVENTORY_COLUMNS;
            int hotbarEnd = hotbarStart + PLAYER_INVENTORY_COLUMNS;

            if (index < wandEnd) {
                if (!this.insertItem(stack, inventoryStart, hotbarEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (NoitaWandItem.isSpellStack(stack)) {
                if (!this.insertItem(stack, 0, wandEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= inventoryStart && index < hotbarStart) {
                if (!this.insertItem(stack, hotbarStart, hotbarEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= hotbarStart && index < hotbarEnd) {
                if (!this.insertItem(stack, inventoryStart, hotbarStart, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }

            if (stack.getCount() == originalStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTakeItem(player, stack);
        }

        return originalStack;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        saveSpells();
    }

    private void addWandSlots() {
        for (int slot = 0; slot < this.spellSlotCount; slot++) {
            int x = SPELL_START_X + (slot % SPELL_SLOTS_PER_ROW) * SLOT_SIZE;
            int y = SPELL_START_Y + (slot / SPELL_SLOTS_PER_ROW) * SLOT_SIZE;
            this.addSlot(new SpellSlot(this.wandInventory, slot, x, y));
        }
    }

    private void addPlayerInventorySlots(PlayerInventory playerInventory) {
        for (int row = 0; row < PLAYER_INVENTORY_ROWS; row++) {
            for (int column = 0; column < PLAYER_INVENTORY_COLUMNS; column++) {
                int inventoryIndex = column + (row + 1) * PLAYER_INVENTORY_COLUMNS;
                int x = PLAYER_INVENTORY_START_X + column * SLOT_SIZE;
                int y = this.playerInventoryY + row * SLOT_SIZE;
                this.addSlot(new Slot(playerInventory, inventoryIndex, x, y));
            }
        }

        int hotbarY = this.playerInventoryY + PLAYER_INVENTORY_ROWS * SLOT_SIZE + HOTBAR_GAP;
        for (int column = 0; column < PLAYER_INVENTORY_COLUMNS; column++) {
            int x = PLAYER_INVENTORY_START_X + column * SLOT_SIZE;
            Slot slot = this.hand == Hand.MAIN_HAND && column == playerInventory.selectedSlot
                ? new LockedSlot(playerInventory, column, x, hotbarY)
                : new Slot(playerInventory, column, x, hotbarY);
            this.addSlot(slot);
        }
    }

    private void saveSpells() {
        if (!this.player.getWorld().isClient && canUse(this.player)) {
            NoitaWandItem.setSpellStacks(this.wandStack, this.spellStacks);
        }
    }

    private final class WandSpellInventory implements Inventory {
        @Override
        public int size() {
            return spellStacks.size();
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack stack : spellStacks) {
                if (!stack.isEmpty()) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public ItemStack getStack(int slot) {
            return spellStacks.get(slot);
        }

        @Override
        public ItemStack removeStack(int slot, int amount) {
            ItemStack result = Inventories.splitStack(spellStacks, slot, amount);
            if (!result.isEmpty()) {
                markDirty();
            }

            return result;
        }

        @Override
        public ItemStack removeStack(int slot) {
            ItemStack result = Inventories.removeStack(spellStacks, slot);
            if (!result.isEmpty()) {
                markDirty();
            }

            return result;
        }

        @Override
        public void setStack(int slot, ItemStack stack) {
            if (!stack.isEmpty() && !NoitaWandItem.isSpellStack(stack)) {
                stack = ItemStack.EMPTY;
            }

            spellStacks.set(slot, stack);
            if (stack.getCount() > getMaxCountPerStack()) {
                stack.setCount(getMaxCountPerStack());
            }
            markDirty();
        }

        @Override
        public int getMaxCountPerStack() {
            return 1;
        }

        @Override
        public void markDirty() {
            saveSpells();
        }

        @Override
        public boolean canPlayerUse(PlayerEntity player) {
            return NoitaWandScreenHandler.this.canUse(player);
        }

        @Override
        public void clear() {
            for (int i = 0; i < spellStacks.size(); i++) {
                spellStacks.set(i, ItemStack.EMPTY);
            }
            markDirty();
        }
    }

    private static final class SpellSlot extends Slot {
        private SpellSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return NoitaWandItem.isSpellStack(stack);
        }

        @Override
        public int getMaxItemCount() {
            return 1;
        }
    }

    private static final class LockedSlot extends Slot {
        private LockedSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) {
            return false;
        }
    }
}

package com.mcnoita.item;

import com.mcnoita.screen.NoitaWandScreenHandler;
import com.mcnoita.wand.NoitaWandTemplate;
import java.util.List;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.client.item.TooltipContext;
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
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class NoitaWandItem extends Item {
    private static final String TEMPLATE_KEY = "NoitaWandTemplate";
    private static final String SPELLS_KEY = "NoitaWandSpells";
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
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        NoitaWandTemplate template = getTemplate(stack);

        tooltip.add(Text.translatable("tooltip.mc-noita.wand.shuffle", yesNo(template.shuffle())).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mc-noita.wand.spells_per_cast", template.spellsPerCast()).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mc-noita.wand.cast_delay", template.castDelaySeconds()).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mc-noita.wand.recharge_time", template.rechargeTimeSeconds()).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mc-noita.wand.mana_max", template.manaMax()).formatted(Formatting.BLUE));
        tooltip.add(Text.translatable("tooltip.mc-noita.wand.mana_charge_speed", template.manaChargeSpeed()).formatted(Formatting.BLUE));
        tooltip.add(Text.translatable("tooltip.mc-noita.wand.capacity", template.capacity()).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mc-noita.wand.loaded_spells", getLoadedSpellCount(stack, template.capacity()), template.capacity()).formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.mc-noita.wand.spread", template.spreadDegrees()).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mc-noita.wand.always_cast_count", template.alwaysCastSpells().size()).formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.translatable("tooltip.mc-noita.wand.speed_multiplier", template.speedMultiplier()).formatted(Formatting.DARK_GRAY));
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

        return NoitaWandTemplate.fromNbt(nbt.getCompound(TEMPLATE_KEY));
    }

    public static void setTemplate(ItemStack stack, NoitaWandTemplate template) {
        stack.getOrCreateNbt().put(TEMPLATE_KEY, template.toNbt());
    }

    public static DefaultedList<ItemStack> getSpellStacks(ItemStack wandStack, int capacity) {
        DefaultedList<ItemStack> spells = DefaultedList.ofSize(capacity, ItemStack.EMPTY);
        NbtCompound nbt = wandStack.getNbt();
        if (nbt == null || !nbt.contains(SPELLS_KEY, NbtElement.LIST_TYPE)) {
            return spells;
        }

        NbtList spellList = nbt.getList(SPELLS_KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < spellList.size(); i++) {
            NbtCompound spellNbt = spellList.getCompound(i);
            int slot = spellNbt.getInt(SLOT_KEY);
            if (slot >= 0 && slot < capacity) {
                ItemStack spellStack = ItemStack.fromNbt(spellNbt);
                if (isSpellStack(spellStack)) {
                    spells.set(slot, spellStack);
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

        NbtCompound nbt = wandStack.getOrCreateNbt();
        if (spellList.isEmpty()) {
            nbt.remove(SPELLS_KEY);
            return;
        }

        nbt.put(SPELLS_KEY, spellList);
    }

    public static boolean isSpellStack(ItemStack stack) {
        return stack.getItem() instanceof NoitaSpellItem;
    }

    private static int getLoadedSpellCount(ItemStack stack, int capacity) {
        int count = 0;
        for (ItemStack spellStack : getSpellStacks(stack, capacity)) {
            if (!spellStack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static Text yesNo(boolean value) {
        return Text.translatable(value ? "tooltip.mc-noita.yes" : "tooltip.mc-noita.no");
    }
}

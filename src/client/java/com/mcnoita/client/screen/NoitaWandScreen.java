package com.mcnoita.client.screen;

import com.mcnoita.screen.NoitaWandScreenHandler;
import com.mcnoita.wand.NoitaWandTemplate;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

public class NoitaWandScreen extends HandledScreen<NoitaWandScreenHandler> {
    public NoitaWandScreen(NoitaWandScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = NoitaWandScreenHandler.BACKGROUND_WIDTH;
        this.backgroundHeight = NoitaWandScreenHandler.getBackgroundHeight(handler.getSpellRows());
        this.titleX = 8;
        this.titleY = 6;
        this.playerInventoryTitleX = NoitaWandScreenHandler.PLAYER_INVENTORY_START_X;
        this.playerInventoryTitleY = handler.getPlayerInventoryY() - 11;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.fill(x, y, x + this.backgroundWidth, y + this.backgroundHeight, 0xFF1F2028);
        context.fill(x + 1, y + 1, x + this.backgroundWidth - 1, y + this.backgroundHeight - 1, 0xFF343641);
        context.fill(
            x + 6,
            y + NoitaWandScreenHandler.WAND_DESCRIPTION_START_Y,
            x + this.backgroundWidth - 6,
            y + NoitaWandScreenHandler.WAND_DESCRIPTION_START_Y + NoitaWandScreenHandler.WAND_DESCRIPTION_HEIGHT,
            0xFF292B35
        );
        context.fill(
            x + 6,
            y + NoitaWandScreenHandler.SPELL_START_Y - 3,
            x + this.backgroundWidth - 6,
            y + NoitaWandScreenHandler.SPELL_START_Y + this.handler.getSpellRows() * NoitaWandScreenHandler.SLOT_SIZE + 3,
            0xFF292B35
        );
        drawSlotBackgrounds(context, x, y);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0xF0E7FF, false);
        drawWandDescription(context);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0xE0E0E0, false);
    }

    private void drawWandDescription(DrawContext context) {
        NoitaWandTemplate template = this.handler.getWandTemplate();
        int y = NoitaWandScreenHandler.WAND_DESCRIPTION_START_Y + 5;
        int leftX = 12;
        int rightX = this.backgroundWidth / 2 + 4;
        int columnWidth = this.backgroundWidth / 2 - 18;

        drawStat(context, Text.translatable("tooltip.mc-noita.wand.shuffle", yesNo(template.shuffle())), leftX, y, columnWidth);
        drawStat(context, Text.translatable("tooltip.mc-noita.wand.mana_max", template.manaMax()), rightX, y, columnWidth);
        y += 10;
        drawStat(context, Text.translatable("tooltip.mc-noita.wand.spells_per_cast", template.spellsPerCast()), leftX, y, columnWidth);
        drawStat(context, Text.translatable("tooltip.mc-noita.wand.mana_charge_speed", template.manaChargeSpeed()), rightX, y, columnWidth);
        y += 10;
        drawStat(context, Text.translatable("tooltip.mc-noita.wand.cast_delay", formatDecimal(template.castDelaySeconds())), leftX, y, columnWidth);
        drawStat(context, Text.translatable("tooltip.mc-noita.wand.capacity", template.capacity()), rightX, y, columnWidth);
        y += 10;
        drawStat(context, Text.translatable("tooltip.mc-noita.wand.recharge_time", formatDecimal(template.rechargeTimeSeconds())), leftX, y, columnWidth);
        drawStat(context, Text.translatable("tooltip.mc-noita.wand.spread", formatDecimal(template.spreadDegrees())), rightX, y, columnWidth);
    }

    private void drawStat(DrawContext context, Text text, int x, int y, int width) {
        String value = text.getString();
        if (this.textRenderer.getWidth(value) > width) {
            value = this.textRenderer.trimToWidth(value, width - this.textRenderer.getWidth("...")) + "...";
        }

        context.drawText(this.textRenderer, value, x, y, 0xD7DECB, false);
    }

    private static Text yesNo(boolean value) {
        return Text.translatable(value ? "tooltip.mc-noita.yes" : "tooltip.mc-noita.no");
    }

    private static String formatDecimal(float value) {
        if (Math.abs(value - Math.round(value)) < 0.005f) {
            return Integer.toString(Math.round(value));
        }

        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private void drawSlotBackgrounds(DrawContext context, int x, int y) {
        for (Slot slot : this.handler.slots) {
            int slotX = x + slot.x;
            int slotY = y + slot.y;
            context.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF171820);
            context.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF555864);
            context.fill(slotX + 1, slotY + 1, slotX + 15, slotY + 15, 0xFF2D2F39);
        }
    }
}

package com.mcnoita.client.screen;

import com.mcnoita.screen.NoitaWandScreenHandler;
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
        this.playerInventoryTitleX = 8;
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
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0xE0E0E0, false);
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

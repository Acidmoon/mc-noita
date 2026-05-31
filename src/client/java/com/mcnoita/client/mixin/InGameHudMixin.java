package com.mcnoita.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Shadow
    @Final
    private static Identifier FOOD_EMPTY_HUNGER_TEXTURE;
    @Shadow
    @Final
    private static Identifier FOOD_HALF_HUNGER_TEXTURE;
    @Shadow
    @Final
    private static Identifier FOOD_FULL_HUNGER_TEXTURE;
    @Shadow
    @Final
    private static Identifier FOOD_EMPTY_TEXTURE;
    @Shadow
    @Final
    private static Identifier FOOD_HALF_TEXTURE;
    @Shadow
    @Final
    private static Identifier FOOD_FULL_TEXTURE;

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void mcnoita$hideExperienceBar(DrawContext context, int x, CallbackInfo ci) {
        ci.cancel();
    }

    @Redirect(
        method = "renderStatusBars",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V")
    )
    private void mcnoita$hideFoodBar(DrawContext context, Identifier texture, int x, int y, int width, int height) {
        if (!isFoodTexture(texture)) {
            context.drawGuiTexture(texture, x, y, width, height);
        }
    }

    private static boolean isFoodTexture(Identifier texture) {
        return FOOD_EMPTY_HUNGER_TEXTURE.equals(texture)
            || FOOD_HALF_HUNGER_TEXTURE.equals(texture)
            || FOOD_FULL_HUNGER_TEXTURE.equals(texture)
            || FOOD_EMPTY_TEXTURE.equals(texture)
            || FOOD_HALF_TEXTURE.equals(texture)
            || FOOD_FULL_TEXTURE.equals(texture);
    }
}

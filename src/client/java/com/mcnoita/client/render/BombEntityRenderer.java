package com.mcnoita.client.render;

import com.mcnoita.MCNoita;
import com.mcnoita.entity.BombEntity;
import com.mcnoita.item.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class BombEntityRenderer extends EntityRenderer<BombEntity> {
    private final ItemRenderer itemRenderer;

    public BombEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = MinecraftClient.getInstance().getItemRenderer();
    }

    @Override
    public void render(BombEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        float scale = 0.75f * entity.getRenderScale();
        matrices.scale(scale, scale, scale);
        matrices.multiply(this.dispatcher.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));
        this.itemRenderer.renderItem(
            new ItemStack(getVisualItem(entity.getItemPath())),
            ModelTransformationMode.GROUND,
            light,
            OverlayTexture.DEFAULT_UV,
            matrices,
            vertexConsumers,
            entity.getWorld(),
            entity.getId()
        );
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(BombEntity entity) {
        return MCNoita.id("textures/item/" + entity.getItemPath() + ".png");
    }

    private static Item getVisualItem(String path) {
        Item item = Registries.ITEM.get(MCNoita.id(path));
        return item == net.minecraft.item.Items.AIR ? ModItems.BOMB : item;
    }
}

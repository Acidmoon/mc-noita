package com.mcnoita.client.render;

import com.mcnoita.entity.SparkBoltProjectileEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

public class NoitaProjectileEntityRenderer extends FlyingItemEntityRenderer<SparkBoltProjectileEntity> {
    private final ItemRenderer itemRenderer;

    public NoitaProjectileEntityRenderer(EntityRendererFactory.Context context) {
        super(context, 0.55f, true);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(SparkBoltProjectileEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        if (entity.hidesProjectileVisual()) {
            return;
        }
        if (entity.getRenderScale() == 1.0f) {
            super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
            return;
        }

        ItemStack stack = entity.getStack();
        matrices.push();
        matrices.scale(entity.getRenderScale(), entity.getRenderScale(), entity.getRenderScale());
        matrices.multiply(this.dispatcher.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));
        this.itemRenderer.renderItem(
            stack,
            ModelTransformationMode.GROUND,
            light,
            OverlayTexture.DEFAULT_UV,
            matrices,
            vertexConsumers,
            entity.getWorld(),
            entity.getId()
        );
        matrices.pop();
    }
}

package com.deadseasons.client.render;

import com.deadseasons.DeadSeasons;
import com.deadseasons.entity.BoxTruckEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

public class TruckRenderer extends LivingEntityRenderer<BoxTruckEntity, TruckModel> {
    private static final ResourceLocation TEXTURE = DeadSeasons.id("textures/entity/box_truck.png");

    public TruckRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new TruckModel(ctx.bakeLayer(TruckModel.LAYER)), 1.6f);
    }

    @Override
    public ResourceLocation getTextureLocation(BoxTruckEntity entity) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(BoxTruckEntity entity) {
        return entity.hasCustomName() && super.shouldShowName(entity);
    }

    @Override
    protected float getFlipDegrees(BoxTruckEntity entity) {
        return 0f; // don't tip over when destroyed
    }
}

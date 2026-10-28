package com.deadseasons.client.render;

import com.deadseasons.entity.zombie.DSZombie;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/** Zombie-shaped renderer with a per-type texture and size. */
public class DSZombieRenderer<T extends DSZombie> extends HumanoidMobRenderer<T, ZombieModel<T>> {
    private final ResourceLocation texture;
    private final float size;

    public DSZombieRenderer(EntityRendererProvider.Context ctx, ResourceLocation texture, float size) {
        super(ctx, new ZombieModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f * size);
        this.texture = texture;
        this.size = size;
        this.addLayer(new HumanoidArmorLayer<>(this,
                new ZombieModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new ZombieModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
                ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        if (size != 1.0f) poseStack.scale(size, size, size);
    }
}

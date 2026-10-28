package com.deadseasons.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Armor piece that also affects body temperature. Warmth helps in cold, cooling helps in heat. */
public class ClothingItem extends ArmorItem {
    private final float warmth;
    private final float cooling;
    private final String extraTooltip;

    public ClothingItem(ArmorMaterial material, Type type, Properties props, float warmth, float cooling, @Nullable String extraTooltip) {
        super(material, type, props);
        this.warmth = warmth;
        this.cooling = cooling;
        this.extraTooltip = extraTooltip;
    }

    public float getWarmth() { return warmth; }
    public float getCooling() { return cooling; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (warmth > 0) tooltip.add(Component.translatable("tooltip.deadseasons.warmth", String.format("%.1f", warmth)).withStyle(ChatFormatting.AQUA));
        if (cooling > 0) tooltip.add(Component.translatable("tooltip.deadseasons.cooling", String.format("%.1f", cooling)).withStyle(ChatFormatting.GOLD));
        if (extraTooltip != null) tooltip.add(Component.translatable(extraTooltip).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}

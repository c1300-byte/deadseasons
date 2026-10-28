package com.deadseasons.item;

import com.deadseasons.entity.BoxTruckEntity;
import com.deadseasons.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Creative-only helper that places a box truck. Survival players must find one in a ruined town. */
public class TruckSpawnerItem extends Item {
    public TruckSpawnerItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (!level.isClientSide) {
            BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
            BoxTruckEntity truck = ModEntities.BOX_TRUCK.get().create(level);
            if (truck != null) {
                float yaw = ctx.getPlayer() != null ? ctx.getPlayer().getYRot() : 0f;
                truck.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0f);
                truck.setYBodyRot(yaw);
                truck.setFuel(50f);
                level.addFreshEntity(truck);
                if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.deadseasons.truck_spawner").withStyle(ChatFormatting.GRAY));
    }
}

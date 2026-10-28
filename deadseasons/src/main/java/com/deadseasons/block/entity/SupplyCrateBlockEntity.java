package com.deadseasons.block.entity;

import com.deadseasons.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SupplyCrateBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    private int smokeTicks = 0;

    public SupplyCrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUPPLY_CRATE.get(), pos, state);
    }

    public void startSmoke() {
        this.smokeTicks = 20 * 60 * 4;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SupplyCrateBlockEntity be) {
        if (be.smokeTicks <= 0) return;
        be.smokeTicks--;
        if (be.smokeTicks % 5 == 0 && level instanceof ServerLevel server) {
            for (ServerPlayer p : server.players()) {
                server.sendParticles(p, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true,
                        pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 1, 0.1, 0.2, 0.1, 0.02);
                if (be.smokeTicks % 20 == 0) {
                    server.sendParticles(p, net.minecraft.core.particles.DustParticleOptions.REDSTONE, true,
                            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 6, 0.2, 0.4, 0.2, 0.1);
                }
            }
        }
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.deadseasons.supply_crate");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.threeRows(id, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!this.trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, items);
        tag.putInt("Smoke", smokeTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(tag)) ContainerHelper.loadAllItems(tag, items);
        smokeTicks = tag.getInt("Smoke");
    }
}

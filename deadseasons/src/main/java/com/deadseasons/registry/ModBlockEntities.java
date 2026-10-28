package com.deadseasons.registry;

import com.deadseasons.DeadSeasons;
import com.deadseasons.block.entity.AlarmBellBlockEntity;
import com.deadseasons.block.entity.SupplyCrateBlockEntity;
import com.deadseasons.block.entity.TurretBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DeadSeasons.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<AlarmBellBlockEntity>> ALARM_BELL = BLOCK_ENTITIES.register("alarm_bell",
            () -> BlockEntityType.Builder.of(AlarmBellBlockEntity::new, ModBlocks.ALARM_BELL.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<TurretBlockEntity>> AUTO_TURRET = BLOCK_ENTITIES.register("auto_turret",
            () -> BlockEntityType.Builder.of(TurretBlockEntity::new, ModBlocks.AUTO_TURRET.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<SupplyCrateBlockEntity>> SUPPLY_CRATE = BLOCK_ENTITIES.register("supply_crate",
            () -> BlockEntityType.Builder.of(SupplyCrateBlockEntity::new, ModBlocks.SUPPLY_CRATE.get()).build(null));

    private ModBlockEntities() {}
}

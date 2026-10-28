package com.deadseasons.registry;

import com.deadseasons.DeadSeasons;
import com.deadseasons.world.TownPiece;
import com.deadseasons.world.TownStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, DeadSeasons.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, DeadSeasons.MODID);

    public static final RegistryObject<StructureType<TownStructure>> TOWN = STRUCTURE_TYPES.register("town", () -> () -> TownStructure.CODEC);

    public static final RegistryObject<StructurePieceType> TOWN_LOT = PIECES.register("town_lot",
            () -> (StructurePieceType.ContextlessType) TownPiece::new);

    private ModStructures() {}
}

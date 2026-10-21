package dev.autarkia.registry;

import dev.autarkia.Autarkia;
import dev.autarkia.energy.HandCrankBlock;
import dev.autarkia.energy.RopePulleyBlock;
import dev.autarkia.logistics.ChuteBlock;
import dev.autarkia.processing.CrucibleBlock;
import dev.autarkia.processing.QuernBlock;
import dev.autarkia.storage.CrateBlock;
import dev.autarkia.storage.TallyBoardBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Every Autarkia block (08a section 1). Block items are registered in {@link AutarkiaItems}. */
public final class AutarkiaBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Autarkia.MOD_ID);

    public static final DeferredBlock<Block> TIN_ORE = BLOCKS.registerSimpleBlock("tin_ore",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3f, 3f));
    public static final DeferredBlock<Block> DEEPSLATE_TIN_ORE = BLOCKS.registerSimpleBlock("deepslate_tin_ore",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
            .strength(4.5f, 3f).sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> RAW_TIN_BLOCK = BLOCKS.registerSimpleBlock("raw_tin_block",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(5f, 6f));
    public static final DeferredBlock<Block> TIN_BLOCK = BLOCKS.registerSimpleBlock("tin_block",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3f, 6f).sound(SoundType.METAL));
    public static final DeferredBlock<Block> BRONZE_BLOCK = BLOCKS.registerSimpleBlock("bronze_block",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops().strength(5f, 6f).sound(SoundType.COPPER));
    public static final DeferredBlock<Block> FOUNDATION = BLOCKS.registerSimpleBlock("foundation",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(6f, 12f));

    public static final DeferredBlock<HandCrankBlock> HAND_CRANK = BLOCKS.registerBlock("hand_crank", HandCrankBlock::new,
        p -> p.mapColor(MapColor.STONE).strength(1.5f).sound(SoundType.STONE).noOcclusion());
    public static final DeferredBlock<RopePulleyBlock> ROPE_PULLEY = BLOCKS.registerBlock("rope_pulley", RopePulleyBlock::new,
        p -> p.mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<QuernBlock> QUERN = BLOCKS.registerBlock("quern", QuernBlock::new,
        p -> p.mapColor(MapColor.STONE).strength(2f).sound(SoundType.STONE).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<BigMachineBlock> STAMP_MILL = BLOCKS.registerBlock("stamp_mill",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.STAMP_MILL, BigMachineBlock.box(0, 0, 0, 0, 1, 0), true),
        p -> p.mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<CrucibleBlock> CRUCIBLE = BLOCKS.registerBlock("crucible", CrucibleBlock::new,
        p -> p.mapColor(MapColor.TERRACOTTA_ORANGE).strength(2f).sound(SoundType.DECORATED_POT).noOcclusion()
            .lightLevel(s -> s.getValue(CrucibleBlock.MOLTEN) ? 9 : 0));
    public static final DeferredBlock<CrateBlock> CRATE = BLOCKS.registerBlock("crate", CrateBlock::new,
        p -> p.mapColor(MapColor.WOOD).strength(2f).sound(SoundType.WOOD));
    public static final DeferredBlock<TallyBoardBlock> TALLY_BOARD = BLOCKS.registerBlock("tally_board", TallyBoardBlock::new,
        p -> p.mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<ChuteBlock> CHUTE = BLOCKS.registerBlock("chute", ChuteBlock::new,
        p -> p.mapColor(MapColor.WOOD).strength(1f).sound(SoundType.WOOD).noOcclusion());

    public static final DeferredBlock<dev.autarkia.energy.TreadwheelBlock> TREADWHEEL = BLOCKS.registerBlock("treadwheel",
        dev.autarkia.energy.TreadwheelBlock::new, p -> p.mapColor(MapColor.WOOD).strength(2f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<BigMachineBlock> WINDMILL = BLOCKS.registerBlock("windmill",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.WINDMILL, BigMachineBlock.box(0, 0, 0, 0, 3, 0), true),
        p -> p.mapColor(MapColor.WOOD).strength(2f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<dev.autarkia.energy.StoneFlywheelBlock> STONE_FLYWHEEL = BLOCKS.registerBlock("stone_flywheel",
        dev.autarkia.energy.StoneFlywheelBlock::new, p -> p.mapColor(MapColor.STONE).strength(3f).sound(SoundType.STONE).noOcclusion()
            .requiresCorrectToolForDrops());

    public static final DeferredBlock<dev.autarkia.colony.ColonyCoreBlock> COLONY_CORE = BLOCKS.registerBlock("colony_core",
        dev.autarkia.colony.ColonyCoreBlock::new, p -> p.mapColor(MapColor.COLOR_ORANGE).strength(5f, 1200f).sound(SoundType.COPPER).noOcclusion()
            .lightLevel(s -> 9));
    public static final DeferredBlock<dev.autarkia.colony.ProjectBoardBlock> PROJECT_BOARD = BLOCKS.registerBlock("project_board",
        dev.autarkia.colony.ProjectBoardBlock::new, p -> p.mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD));
    public static final DeferredBlock<BigMachineBlock> WORKER_ASSEMBLER = BLOCKS.registerBlock("worker_assembler",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.WORKER_ASSEMBLER, BigMachineBlock.box(0, 0, 0, 1, 1, 1), true),
        p -> p.mapColor(MapColor.COLOR_ORANGE).strength(3f).sound(SoundType.COPPER).noOcclusion());
    public static final DeferredBlock<dev.autarkia.worker.WindingPostBlock> WINDING_POST = BLOCKS.registerBlock("winding_post",
        dev.autarkia.worker.WindingPostBlock::new, p -> p.mapColor(MapColor.WOOD).strength(2f).sound(SoundType.WOOD).noOcclusion());

    private AutarkiaBlocks() {
    }
}

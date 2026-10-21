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

    private AutarkiaBlocks() {
    }
}

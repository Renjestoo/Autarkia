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

    public static final DeferredBlock<dev.autarkia.energy.PoleBlock> WOODEN_POLE = BLOCKS.registerBlock("wooden_pole",
        p -> new dev.autarkia.energy.PoleBlock(p, AutarkiaBlockEntities.WOODEN_POLE, 3, dev.autarkia.energy.CableTier.COPPER, 4, 0.35),
        p -> p.mapColor(MapColor.WOOD).strength(2f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<dev.autarkia.energy.IndustrialLampBlock> INDUSTRIAL_LAMP = BLOCKS.registerBlock("industrial_lamp",
        dev.autarkia.energy.IndustrialLampBlock::new, p -> p.mapColor(MapColor.METAL).strength(1.5f).sound(SoundType.LANTERN)
            .lightLevel(s -> s.getValue(dev.autarkia.energy.IndustrialLampBlock.LIT) ? 15 : 0));
    public static final DeferredBlock<dev.autarkia.energy.CapacitorBankBlock> CAPACITOR_BANK = BLOCKS.registerBlock("capacitor_bank",
        dev.autarkia.energy.CapacitorBankBlock::new, p -> p.mapColor(MapColor.COLOR_ORANGE).strength(3f).sound(SoundType.COPPER).requiresCorrectToolForDrops());
    public static final DeferredBlock<BigMachineBlock> LIGHTNING_MAST = BLOCKS.registerBlock("lightning_mast",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.LIGHTNING_MAST, BigMachineBlock.box(0, 0, 0, 0, 3, 0), true),
        p -> p.mapColor(MapColor.COLOR_ORANGE).strength(3f).sound(SoundType.COPPER).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<dev.autarkia.energy.LightningArresterBlock> LIGHTNING_ARRESTER = BLOCKS.registerBlock("lightning_arrester",
        dev.autarkia.energy.LightningArresterBlock::new, p -> p.mapColor(MapColor.COLOR_ORANGE).strength(2f).sound(SoundType.COPPER).noOcclusion());
    public static final DeferredBlock<BigMachineBlock> WATERWHEEL = BLOCKS.registerBlock("waterwheel",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.WATERWHEEL, BigMachineBlock.box(0, 0, -1, 0, 2, 1), true),
        p -> p.mapColor(MapColor.WOOD).strength(2f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<dev.autarkia.energy.BoilerBlock> BOILER = BLOCKS.registerBlock("boiler", dev.autarkia.energy.BoilerBlock::new,
        p -> p.mapColor(MapColor.COLOR_RED).strength(4f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()
            .lightLevel(s -> 4));
    public static final DeferredBlock<dev.autarkia.energy.PressureValveBlock> PRESSURE_VALVE = BLOCKS.registerBlock("pressure_valve",
        dev.autarkia.energy.PressureValveBlock::new, p -> p.mapColor(MapColor.METAL).strength(2f).sound(SoundType.COPPER).noOcclusion());
    public static final DeferredBlock<BigMachineBlock> STEAM_ENGINE = BLOCKS.registerBlock("steam_engine",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.STEAM_ENGINE, BigMachineBlock.box(0, 0, 0, 0, 1, 1), true),
        p -> p.mapColor(MapColor.METAL).strength(3.5f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> CATWALK = BLOCKS.registerSimpleBlock("catwalk",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 6f).sound(SoundType.METAL).noOcclusion()
            .isSuffocating((s, l, p) -> false).isRedstoneConductor((s, l, p) -> false));
    public static final DeferredBlock<net.minecraft.world.level.block.StairBlock> CATWALK_STAIRS = BLOCKS.registerBlock("catwalk_stairs",
        p -> new net.minecraft.world.level.block.StairBlock(CATWALK.get().defaultBlockState(), p),
        p -> p.mapColor(MapColor.METAL).strength(2f, 6f).sound(SoundType.METAL).noOcclusion().isSuffocating((s, l, q) -> false));
    public static final DeferredBlock<Block> HAZARD_BLOCK = BLOCKS.registerSimpleBlock("hazard_block",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(2f, 6f).sound(SoundType.STONE).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> ZINC_ORE = BLOCKS.registerSimpleBlock("zinc_ore",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3f, 3f));
    public static final DeferredBlock<Block> DEEPSLATE_ZINC_ORE = BLOCKS.registerSimpleBlock("deepslate_zinc_ore",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
            .strength(4.5f, 3f).sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> RAW_ZINC_BLOCK = BLOCKS.registerSimpleBlock("raw_zinc_block",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
            .strength(5f, 6f));
    public static final DeferredBlock<Block> ZINC_BLOCK = BLOCKS.registerSimpleBlock("zinc_block",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(4f, 6f).sound(SoundType.METAL));
    public static final DeferredBlock<Block> BRASS_BLOCK = BLOCKS.registerSimpleBlock("brass_block",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).requiresCorrectToolForDrops().strength(5f, 6f).sound(SoundType.METAL));
    public static final DeferredBlock<Block> STEEL_BLOCK = BLOCKS.registerSimpleBlock("steel_block",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(6f, 8f).sound(SoundType.METAL));
    public static final DeferredBlock<BigMachineBlock> SLUICE = BLOCKS.registerBlock("sluice",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.SLUICE, BigMachineBlock.box(0, 0, -1, 0, 0, 1), true),
        p -> p.mapColor(MapColor.WOOD).strength(2f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<BigMachineBlock> BLAST_SMELTER = BLOCKS.registerBlock("blast_smelter",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.BLAST_SMELTER, BigMachineBlock.box(0, 0, 0, 1, 1, 1), true),
        p -> p.mapColor(MapColor.COLOR_RED).strength(4f).sound(SoundType.STONE).noOcclusion().requiresCorrectToolForDrops().lightLevel(s -> 6));
    public static final DeferredBlock<dev.autarkia.processing.FormingMachineBlock> PLATE_PRESS = BLOCKS.registerBlock("plate_press",
        p -> new dev.autarkia.processing.FormingMachineBlock(p, AutarkiaBlockEntities.PLATE_PRESS),
        p -> p.mapColor(MapColor.METAL).strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<dev.autarkia.processing.FormingMachineBlock> WIRE_DRAWER = BLOCKS.registerBlock("wire_drawer",
        p -> new dev.autarkia.processing.FormingMachineBlock(p, AutarkiaBlockEntities.WIRE_DRAWER),
        p -> p.mapColor(MapColor.METAL).strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<dev.autarkia.logistics.BeltBlock> BELT = BLOCKS.registerBlock("belt", dev.autarkia.logistics.BeltBlock::new,
        p -> p.mapColor(MapColor.COLOR_BLACK).strength(1.5f).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredBlock<dev.autarkia.logistics.FilterBlock> BELT_SPLITTER = BLOCKS.registerBlock("belt_splitter",
        p -> new dev.autarkia.logistics.FilterBlock(p, AutarkiaBlockEntities.BELT_SPLITTER),
        p -> p.mapColor(MapColor.METAL).strength(2f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredBlock<dev.autarkia.logistics.FilterBlock> SORTING_CHUTE = BLOCKS.registerBlock("sorting_chute",
        p -> new dev.autarkia.logistics.FilterBlock(p, AutarkiaBlockEntities.SORTING_CHUTE),
        p -> p.mapColor(MapColor.WOOD).strength(2f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<dev.autarkia.storage.RackBlock> WOOD_RACK = BLOCKS.registerBlock("wood_rack",
        p -> new dev.autarkia.storage.RackBlock(p, AutarkiaBlockEntities.WOOD_RACK, 2, 2048,
            () -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK, 3)),
        p -> p.mapColor(MapColor.WOOD).strength(2f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<dev.autarkia.colony.PatternLoomBlock> PATTERN_LOOM = BLOCKS.registerBlock("pattern_loom",
        dev.autarkia.colony.PatternLoomBlock::new, p -> p.mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD));

    public static final DeferredBlock<dev.autarkia.energy.PoleBlock> STEEL_PYLON = BLOCKS.registerBlock("steel_pylon",
        p -> new dev.autarkia.energy.PoleBlock(p, AutarkiaBlockEntities.STEEL_PYLON, 5, dev.autarkia.energy.CableTier.INSULATED, 6, 0.4),
        p -> p.mapColor(MapColor.METAL).strength(4f, 8f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<dev.autarkia.energy.WallInsulatorBlock> WALL_INSULATOR = BLOCKS.registerBlock("wall_insulator",
        dev.autarkia.energy.WallInsulatorBlock::new, p -> p.mapColor(MapColor.METAL).strength(1.5f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredBlock<dev.autarkia.energy.TransformerBlock> TRANSFORMER = BLOCKS.registerBlock("transformer",
        dev.autarkia.energy.TransformerBlock::new, p -> p.mapColor(MapColor.METAL).strength(4f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<dev.autarkia.energy.FuseBoxBlock> FUSE_BOX = BLOCKS.registerBlock("fuse_box",
        dev.autarkia.energy.FuseBoxBlock::new, p -> p.mapColor(MapColor.METAL).strength(3f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<BigMachineBlock> STEAM_TURBINE = BLOCKS.registerBlock("steam_turbine",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.STEAM_TURBINE, BigMachineBlock.box(-1, 0, 0, 1, 2, 1), true),
        p -> p.mapColor(MapColor.METAL).strength(5f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<dev.autarkia.processing.FormingMachineBlock> BATTERY_BANK = BLOCKS.registerBlock("battery_bank",
        p -> new dev.autarkia.processing.FormingMachineBlock(p, AutarkiaBlockEntities.BATTERY_BANK),
        p -> p.mapColor(MapColor.METAL).strength(4f).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final DeferredBlock<dev.autarkia.storage.RackBlock> STEEL_RACK = BLOCKS.registerBlock("steel_rack",
        p -> new dev.autarkia.storage.RackBlock(p, AutarkiaBlockEntities.STEEL_RACK, 3, 8192,
            () -> new net.minecraft.world.item.ItemStack(AutarkiaItems.STEEL_NUGGET.get(), 2)),
        p -> p.mapColor(MapColor.METAL).strength(4f, 6f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<BigMachineBlock> JAW_CRUSHER =BLOCKS.registerBlock("jaw_crusher",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.JAW_CRUSHER, BigMachineBlock.box(0, 0, 0, 1, 1, 1), true),
        p -> p.mapColor(MapColor.METAL).strength(5f, 8f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<dev.autarkia.processing.FormingMachineBlock> MAGNETIC_SEPARATOR = BLOCKS.registerBlock("magnetic_separator",
        p -> new dev.autarkia.processing.FormingMachineBlock(p, AutarkiaBlockEntities.MAGNETIC_SEPARATOR),
        p -> p.mapColor(MapColor.METAL).strength(4f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<dev.autarkia.processing.FormingMachineBlock> ELECTROLYZER = BLOCKS.registerBlock("electrolyzer",
        p -> new dev.autarkia.processing.FormingMachineBlock(p, AutarkiaBlockEntities.ELECTROLYZER),
        p -> p.mapColor(MapColor.METAL).strength(4f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<BigMachineBlock> FABRICATOR = BLOCKS.registerBlock("fabricator",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.FABRICATOR, BigMachineBlock.box(0, 0, 0, 1, 1, 0), true),
        p -> p.mapColor(MapColor.METAL).strength(4f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<dev.autarkia.processing.FormingMachineBlock> WAREHOUSE_CONTROLLER = BLOCKS.registerBlock("warehouse_controller",
        p -> new dev.autarkia.processing.FormingMachineBlock(p, AutarkiaBlockEntities.WAREHOUSE_CONTROLLER),
        p -> p.mapColor(MapColor.METAL).strength(4f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<dev.autarkia.processing.FormingMachineBlock> LEDGER_TERMINAL = BLOCKS.registerBlock("ledger_terminal",
        p -> new dev.autarkia.processing.FormingMachineBlock(p, AutarkiaBlockEntities.LEDGER_TERMINAL),
        p -> p.mapColor(MapColor.METAL).strength(3f).sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(s -> 4));
    public static final DeferredBlock<dev.autarkia.logistics.CapsuleTubeBlock> CAPSULE_TUBE = BLOCKS.registerBlock("capsule_tube",
        dev.autarkia.logistics.CapsuleTubeBlock::new, p -> p.mapColor(MapColor.METAL).strength(1.5f).sound(SoundType.GLASS).noOcclusion());
    public static final DeferredBlock<dev.autarkia.logistics.CapsuleStationBlock> CAPSULE_STATION = BLOCKS.registerBlock("capsule_station",
        dev.autarkia.logistics.CapsuleStationBlock::new,
        p -> p.mapColor(MapColor.METAL).strength(3.5f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<BigMachineBlock> OUTPOST_CORE = BLOCKS.registerBlock("outpost_core",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.OUTPOST_CORE, BigMachineBlock.box(0, 0, 0, 1, 1, 1), true),
        p -> p.mapColor(MapColor.METAL).strength(5f, 12f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops().lightLevel(s -> 8));
    public static final DeferredBlock<BigMachineBlock> REPLICATOR_FOUNDRY = BLOCKS.registerBlock("replicator_foundry",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.REPLICATOR_FOUNDRY, BigMachineBlock.box(-1, 0, -1, 1, 2, 1), true),
        p -> p.mapColor(MapColor.COLOR_PURPLE).strength(6f, 12f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops().lightLevel(s -> 6));
    public static final DeferredBlock<BigMachineBlock> RESTORATION_PROJECTOR = BLOCKS.registerBlock("restoration_projector",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.RESTORATION_PROJECTOR, BigMachineBlock.box(0, 0, 0, 0, 1, 0), true),
        p -> p.mapColor(MapColor.COLOR_BLACK).strength(6f, 12f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops().lightLevel(s -> 8));
    public static final DeferredBlock<dev.autarkia.colony.DomainBeaconBlock> DOMAIN_BEACON = BLOCKS.registerBlock("domain_beacon",
        dev.autarkia.colony.DomainBeaconBlock::new, p -> p.mapColor(MapColor.METAL).strength(4f, 8f).sound(SoundType.METAL).requiresCorrectToolForDrops()
            .lightLevel(s -> 10));
    public static final DeferredBlock<Block> PROP_BEAM = BLOCKS.registerSimpleBlock("prop_beam",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<dev.autarkia.processing.FormingMachineBlock> RECHARGE_DOCK = BLOCKS.registerBlock("recharge_dock",
        p -> new dev.autarkia.processing.FormingMachineBlock(p, AutarkiaBlockEntities.RECHARGE_DOCK),
        p -> p.mapColor(MapColor.METAL).strength(4f).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> VANTITE_ORE = BLOCKS.registerSimpleBlock("vantite_ore",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
            .strength(5f, 9f).sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> RAW_VANTITE_BLOCK = BLOCKS.registerSimpleBlock("raw_vantite_block",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
            .strength(5f, 6f));
    public static final DeferredBlock<Block> VANTITE_BLOCK = BLOCKS.registerSimpleBlock("vantite_block",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).requiresCorrectToolForDrops().strength(6f, 9f).sound(SoundType.METAL));
    public static final DeferredBlock<Block> VANTITE_STEEL_BLOCK = BLOCKS.registerSimpleBlock("vantite_steel_block",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_STEM).requiresCorrectToolForDrops().strength(7f, 12f).sound(SoundType.METAL));
    public static final DeferredBlock<dev.autarkia.processing.OreOutcropBlock> ORE_OUTCROP = BLOCKS.registerBlock("ore_outcrop",
        dev.autarkia.processing.OreOutcropBlock::new, p -> p.mapColor(MapColor.STONE).strength(1f, 1f).sound(SoundType.STONE).noOcclusion());
    public static final DeferredBlock<dev.autarkia.processing.MagmaVentBlock> MAGMA_VENT = BLOCKS.registerBlock("magma_vent",
        dev.autarkia.processing.MagmaVentBlock::new, p -> p.mapColor(MapColor.NETHER).strength(3f, 6f).sound(SoundType.STONE).requiresCorrectToolForDrops()
            .lightLevel(s -> 6));
    public static final DeferredBlock<BigMachineBlock> FROTH_FLOTATION_PLANT = BLOCKS.registerBlock("froth_flotation_plant",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.FROTH_FLOTATION_PLANT, BigMachineBlock.box(-1, 0, -1, 1, 2, 1), true),
        p -> p.mapColor(MapColor.METAL).strength(5f, 8f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<dev.autarkia.energy.PoleBlock> HV_PYLON = BLOCKS.registerBlock("hv_pylon",
        p -> new dev.autarkia.energy.PoleBlock(p, AutarkiaBlockEntities.HV_PYLON, 7, dev.autarkia.energy.CableTier.HV, 4, 0.45),
        p -> p.mapColor(MapColor.METAL).strength(5f, 10f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<BigMachineBlock> HYDRO_TURBINE = BLOCKS.registerBlock("hydro_turbine",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.HYDRO_TURBINE, BigMachineBlock.box(-1, 0, -1, 1, 2, 1), true),
        p -> p.mapColor(MapColor.METAL).strength(5f, 10f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<BigMachineBlock> WIND_TOWER = BLOCKS.registerBlock("wind_tower",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.WIND_TOWER, BigMachineBlock.box(0, 0, 0, 0, 7, 0), true),
        p -> p.mapColor(MapColor.QUARTZ).strength(5f, 10f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<BigMachineBlock> MAGMA_TAP = BLOCKS.registerBlock("magma_tap",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.MAGMA_TAP, BigMachineBlock.box(0, 0, 0, 0, 1, 0), true),
        p -> p.mapColor(MapColor.METAL).strength(5f, 10f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<BigMachineBlock> CABLE_CAR_STATION = BLOCKS.registerBlock("cable_car_station",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.CABLE_CAR_STATION, BigMachineBlock.box(0, 0, 0, 1, 2, 1), true),
        p -> p.mapColor(MapColor.METAL).strength(5f, 8f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<BigMachineBlock> CRATE_LAUNCHER = BLOCKS.registerBlock("crate_launcher",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.CRATE_LAUNCHER, BigMachineBlock.box(0, 0, 0, 0, 1, 0), true),
        p -> p.mapColor(MapColor.METAL).strength(4f, 8f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<dev.autarkia.logistics.CatchNetBlock> CATCH_NET = BLOCKS.registerBlock("catch_net",
        dev.autarkia.logistics.CatchNetBlock::new, p -> p.mapColor(MapColor.WOOL).strength(1.5f).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredBlock<MachineBlock> DEPOT = BLOCKS.registerBlock("depot",
        p -> new MachineBlock(p, AutarkiaBlockEntities.DEPOT, false),
        p -> p.mapColor(MapColor.METAL).strength(4f, 8f).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final DeferredBlock<dev.autarkia.processing.LatticeClusterBlock> LATTICE_CLUSTER = BLOCKS.registerBlock("lattice_cluster",
        dev.autarkia.processing.LatticeClusterBlock::new, p -> p.mapColor(MapColor.COLOR_PURPLE).forceSolidOn().noOcclusion().sound(SoundType.AMETHYST_CLUSTER)
            .strength(1.5f).lightLevel(s -> 7).pushReaction(net.minecraft.world.level.material.PushReaction.POPPED).requiresCorrectToolForDrops());
    public static final DeferredBlock<dev.autarkia.processing.LatticeGeodeShellBlock> LATTICE_GEODE_SHELL = BLOCKS.registerBlock("lattice_geode_shell",
        dev.autarkia.processing.LatticeGeodeShellBlock::new, p -> p.mapColor(MapColor.COLOR_PURPLE).strength(2f, 6f).sound(SoundType.AMETHYST)
            .requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> LATTICE_ALLOY_BLOCK = BLOCKS.registerSimpleBlock("lattice_alloy_block",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(8f, 12f).sound(SoundType.METAL));
    public static final DeferredBlock<Block> LATTICE_BLOCK = BLOCKS.registerSimpleBlock("lattice_block",
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(6f, 1200f).sound(SoundType.METAL));
    public static final DeferredBlock<BigMachineBlock> PLASMA_SEPARATOR = BLOCKS.registerBlock("plasma_separator",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.PLASMA_SEPARATOR, BigMachineBlock.box(0, 0, 0, 1, 1, 1), true),
        p -> p.mapColor(MapColor.COLOR_PURPLE).strength(5f, 8f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());

    public static final DeferredBlock<dev.autarkia.energy.PoleBlock> CRYO_NODE = BLOCKS.registerBlock("cryo_node",
        p -> new dev.autarkia.energy.PoleBlock(p, AutarkiaBlockEntities.CRYO_NODE, 3, dev.autarkia.energy.CableTier.SUPERCONDUCTOR, 4, 0.3),
        p -> p.mapColor(MapColor.ICE).strength(6f, 12f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<BigMachineBlock> SUPERCONDUCTING_COIL = BLOCKS.registerBlock("superconducting_coil",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.SUPERCONDUCTING_COIL, BigMachineBlock.box(0, 0, 0, 1, 1, 1), true),
        p -> p.mapColor(MapColor.COLOR_PURPLE).strength(6f, 12f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<BigMachineBlock> PLASMA_GENERATOR = BLOCKS.registerBlock("plasma_generator",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.PLASMA_GENERATOR, BigMachineBlock.box(-1, 0, -1, 1, 2, 1), true),
        p -> p.mapColor(MapColor.COLOR_PURPLE).strength(6f, 12f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<BigMachineBlock> SINGULARITY_CORE = BLOCKS.registerBlock("singularity_core",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.SINGULARITY_CORE, BigMachineBlock.box(-2, 0, -2, 2, 4, 2), true),
        p -> p.mapColor(MapColor.COLOR_BLACK).strength(8f, 1200f).sound(SoundType.NETHERITE_BLOCK).noOcclusion().requiresCorrectToolForDrops()
            .lightLevel(s -> 10));
    public static final DeferredBlock<BigMachineBlock> GYRO_HEART = BLOCKS.registerBlock("gyro_heart",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.GYRO_HEART, BigMachineBlock.box(-2, 0, -2, 2, 2, 2), true),
        p -> p.mapColor(MapColor.COLOR_BLACK).strength(8f, 1200f).sound(SoundType.NETHERITE_BLOCK).noOcclusion().requiresCorrectToolForDrops()
            .lightLevel(s -> 6));
    public static final DeferredBlock<dev.autarkia.energy.PhaseNodeBlock> PHASE_NODE = BLOCKS.registerBlock("phase_node",
        dev.autarkia.energy.PhaseNodeBlock::new, p -> p.mapColor(MapColor.COLOR_CYAN).strength(6f, 12f).sound(SoundType.AMETHYST)
            .requiresCorrectToolForDrops().lightLevel(s -> 7));

    public static final DeferredBlock<BigMachineBlock> MATTER_BUFFER = BLOCKS.registerBlock("matter_buffer",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.MATTER_BUFFER, BigMachineBlock.box(0, 0, 0, 1, 1, 1), true),
        p -> p.mapColor(MapColor.COLOR_PURPLE).strength(5f, 12f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops().lightLevel(s -> 6));
    public static final DeferredBlock<dev.autarkia.logistics.MagRailBlock> MAG_RAIL = BLOCKS.registerBlock("mag_rail",
        dev.autarkia.logistics.MagRailBlock::new, p -> p.mapColor(MapColor.METAL).noCollision().strength(0.7f).sound(SoundType.METAL));
    public static final DeferredBlock<dev.autarkia.logistics.MagRailStationBlock> MAG_RAIL_STATION = BLOCKS.registerBlock("mag_rail_station",
        p -> new dev.autarkia.logistics.MagRailStationBlock(p, AutarkiaBlockEntities.MAG_RAIL_STATION),
        p -> p.mapColor(MapColor.COLOR_PURPLE).strength(4f, 8f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<dev.autarkia.storage.RackBlock> LATTICE_RACK = BLOCKS.registerBlock("lattice_rack",
        p -> new dev.autarkia.storage.RackBlock(p, AutarkiaBlockEntities.LATTICE_RACK, 4, 65536,
            () -> new net.minecraft.world.item.ItemStack(AutarkiaItems.LATTICE_SHARD.get(), 2)),
        p -> p.mapColor(MapColor.COLOR_PURPLE).strength(5f, 8f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());

    public static final DeferredBlock<BigMachineBlock> NANITE_HIVE = BLOCKS.registerBlock("nanite_hive",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.NANITE_HIVE, BigMachineBlock.box(0, 0, 0, 1, 1, 1), true),
        p -> p.mapColor(MapColor.COLOR_GRAY).strength(5f, 12f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops().lightLevel(s -> 5));
    public static final DeferredBlock<dev.autarkia.colony.ClaimMarkerBlock> CLAIM_MARKER = BLOCKS.registerBlock("claim_marker",
        dev.autarkia.colony.ClaimMarkerBlock::new, p -> p.mapColor(MapColor.COLOR_PURPLE).strength(0.5f).sound(SoundType.AMETHYST).noCollision()
            .noOcclusion().lightLevel(s -> 7));
    public static final DeferredBlock<BigMachineBlock> MATTER_COMPILER = BLOCKS.registerBlock("matter_compiler",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.MATTER_COMPILER, BigMachineBlock.box(0, 0, 0, 1, 1, 1), true),
        p -> p.mapColor(MapColor.COLOR_BLACK).strength(6f, 12f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops().lightLevel(s -> 6));
    public static final DeferredBlock<dev.autarkia.logistics.MatterConduitBlock> MATTER_CONDUIT = BLOCKS.registerBlock("matter_conduit",
        p -> new dev.autarkia.logistics.MatterConduitBlock(p, AutarkiaBlockEntities.MATTER_CONDUIT),
        p -> p.mapColor(MapColor.GOLD).strength(5f, 12f).sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(s -> 4));
    public static final DeferredBlock<dev.autarkia.processing.FormingMachineBlock> DOMAIN_ARCHIVE = BLOCKS.registerBlock("domain_archive",
        p -> new dev.autarkia.processing.FormingMachineBlock(p, AutarkiaBlockEntities.DOMAIN_ARCHIVE),
        p -> p.mapColor(MapColor.COLOR_BLACK).strength(5f, 12f).sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(s -> 6));

    public static final DeferredBlock<BigMachineBlock> SEED_MATRIX = BLOCKS.registerBlock("seed_matrix",
        p -> new BigMachineBlock(p, AutarkiaBlockEntities.SEED_MATRIX, BigMachineBlock.box(-1, 0, -1, 1, 2, 1), true),
        p -> p.mapColor(MapColor.COLOR_BLACK).strength(8f, 24f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops().lightLevel(s -> 7));

    public static final DeferredBlock<dev.autarkia.equipment.BastionBlock> BASTION_BLOCK = BLOCKS.registerBlock("bastion_block",
        dev.autarkia.equipment.BastionBlock::new, p -> p.mapColor(MapColor.COLOR_BLACK).strength(50f, 1200f).sound(SoundType.STONE)
            .requiresCorrectToolForDrops());
    public static final DeferredBlock<dev.autarkia.equipment.BastionGlassBlock> BASTION_GLASS = BLOCKS.registerBlock("bastion_glass",
        dev.autarkia.equipment.BastionGlassBlock::new, p -> p.mapColor(MapColor.NONE).strength(50f, 1200f).sound(SoundType.GLASS)
            .requiresCorrectToolForDrops().noOcclusion().isValidSpawn((s, l, pos, type) -> false).isRedstoneConductor((s, l, pos) -> false)
            .isSuffocating((s, l, pos) -> false));
    public static final DeferredBlock<dev.autarkia.equipment.ForgeOfAscensionBlock> FORGE_OF_ASCENSION = BLOCKS.registerBlock("forge_of_ascension",
        p -> new dev.autarkia.equipment.ForgeOfAscensionBlock(p, AutarkiaBlockEntities.FORGE_OF_ASCENSION),
        p -> p.mapColor(MapColor.COLOR_BLACK).strength(8f, 1200f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops().lightLevel(s -> 7));

    private AutarkiaBlocks() {
    }
}

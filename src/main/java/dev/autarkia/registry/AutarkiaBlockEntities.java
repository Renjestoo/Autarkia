package dev.autarkia.registry;

import dev.autarkia.Autarkia;
import dev.autarkia.energy.HandCrankBlockEntity;
import dev.autarkia.energy.RopePulleyBlockEntity;
import dev.autarkia.logistics.ChuteBlockEntity;
import dev.autarkia.processing.CrucibleBlockEntity;
import dev.autarkia.processing.MachineBlockEntity;
import dev.autarkia.processing.QuernBlockEntity;
import dev.autarkia.processing.StampMillBlockEntity;
import dev.autarkia.storage.CrateBlockEntity;
import dev.autarkia.storage.TallyBoardBlockEntity;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nikdo53.tinymultiblocklib.blockentities.IMultiBlockEntity;

/** Block entity types (one per "BE" block of 08a, id = block id) and their capabilities. */
public final class AutarkiaBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Autarkia.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HandCrankBlockEntity>> HAND_CRANK =
        type("hand_crank", HandCrankBlockEntity::new, AutarkiaBlocks.HAND_CRANK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RopePulleyBlockEntity>> ROPE_PULLEY =
        type("rope_pulley", RopePulleyBlockEntity::new, AutarkiaBlocks.ROPE_PULLEY);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuernBlockEntity>> QUERN =
        type("quern", QuernBlockEntity::new, AutarkiaBlocks.QUERN);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StampMillBlockEntity>> STAMP_MILL =
        type("stamp_mill", StampMillBlockEntity::new, AutarkiaBlocks.STAMP_MILL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrucibleBlockEntity>> CRUCIBLE =
        type("crucible", CrucibleBlockEntity::new, AutarkiaBlocks.CRUCIBLE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrateBlockEntity>> CRATE =
        type("crate", CrateBlockEntity::new, AutarkiaBlocks.CRATE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TallyBoardBlockEntity>> TALLY_BOARD =
        type("tally_board", TallyBoardBlockEntity::new, AutarkiaBlocks.TALLY_BOARD);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChuteBlockEntity>> CHUTE =
        type("chute", ChuteBlockEntity::new, AutarkiaBlocks.CHUTE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.energy.TreadwheelBlockEntity>> TREADWHEEL =
        type("treadwheel", dev.autarkia.energy.TreadwheelBlockEntity::new, AutarkiaBlocks.TREADWHEEL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.energy.WindmillBlockEntity>> WINDMILL =
        type("windmill", dev.autarkia.energy.WindmillBlockEntity::new, AutarkiaBlocks.WINDMILL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.energy.StoneFlywheelBlockEntity>> STONE_FLYWHEEL =
        type("stone_flywheel", dev.autarkia.energy.StoneFlywheelBlockEntity::new, AutarkiaBlocks.STONE_FLYWHEEL);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.colony.ColonyCoreBlockEntity>> COLONY_CORE =
        type("colony_core", dev.autarkia.colony.ColonyCoreBlockEntity::new, AutarkiaBlocks.COLONY_CORE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.colony.ProjectBoardBlockEntity>> PROJECT_BOARD =
        type("project_board", dev.autarkia.colony.ProjectBoardBlockEntity::new, AutarkiaBlocks.PROJECT_BOARD);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.worker.WorkerAssemblerBlockEntity>> WORKER_ASSEMBLER =
        type("worker_assembler", dev.autarkia.worker.WorkerAssemblerBlockEntity::new, AutarkiaBlocks.WORKER_ASSEMBLER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.worker.WindingPostBlockEntity>> WINDING_POST =
        type("winding_post", dev.autarkia.worker.WindingPostBlockEntity::new, AutarkiaBlocks.WINDING_POST);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.energy.WoodenPoleBlockEntity>> WOODEN_POLE =
        type("wooden_pole", dev.autarkia.energy.WoodenPoleBlockEntity::new, AutarkiaBlocks.WOODEN_POLE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.energy.IndustrialLampBlockEntity>> INDUSTRIAL_LAMP =
        type("industrial_lamp", dev.autarkia.energy.IndustrialLampBlockEntity::new, AutarkiaBlocks.INDUSTRIAL_LAMP);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.energy.CapacitorBankBlockEntity>> CAPACITOR_BANK =
        type("capacitor_bank", dev.autarkia.energy.CapacitorBankBlockEntity::new, AutarkiaBlocks.CAPACITOR_BANK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.energy.LightningMastBlockEntity>> LIGHTNING_MAST =
        type("lightning_mast", dev.autarkia.energy.LightningMastBlockEntity::new, AutarkiaBlocks.LIGHTNING_MAST);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.energy.WaterwheelBlockEntity>> WATERWHEEL =
        type("waterwheel", dev.autarkia.energy.WaterwheelBlockEntity::new, AutarkiaBlocks.WATERWHEEL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.energy.BoilerBlockEntity>> BOILER =
        type("boiler", dev.autarkia.energy.BoilerBlockEntity::new, AutarkiaBlocks.BOILER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.energy.SteamEngineBlockEntity>> STEAM_ENGINE =
        type("steam_engine", dev.autarkia.energy.SteamEngineBlockEntity::new, AutarkiaBlocks.STEAM_ENGINE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.processing.SluiceBlockEntity>> SLUICE =
        type("sluice", dev.autarkia.processing.SluiceBlockEntity::new, AutarkiaBlocks.SLUICE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.processing.BlastSmelterBlockEntity>> BLAST_SMELTER =
        type("blast_smelter", dev.autarkia.processing.BlastSmelterBlockEntity::new, AutarkiaBlocks.BLAST_SMELTER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.processing.PressMachineBlockEntity>> PLATE_PRESS =
        type("plate_press", dev.autarkia.processing.PressMachineBlockEntity::press, AutarkiaBlocks.PLATE_PRESS);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.processing.PressMachineBlockEntity>> WIRE_DRAWER =
        type("wire_drawer", dev.autarkia.processing.PressMachineBlockEntity::drawer, AutarkiaBlocks.WIRE_DRAWER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.logistics.BeltBlockEntity>> BELT =
        type("belt", dev.autarkia.logistics.BeltBlockEntity::new, AutarkiaBlocks.BELT);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.logistics.FilterBlockEntity>> BELT_SPLITTER =
        type("belt_splitter", dev.autarkia.logistics.FilterBlockEntity::splitter, AutarkiaBlocks.BELT_SPLITTER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.logistics.FilterBlockEntity>> SORTING_CHUTE =
        type("sorting_chute", dev.autarkia.logistics.FilterBlockEntity::sortingChute, AutarkiaBlocks.SORTING_CHUTE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.storage.RackBlockEntity>> WOOD_RACK = TYPES.register("wood_rack",
        () -> new BlockEntityType<>((pos, state) -> new dev.autarkia.storage.RackBlockEntity(AutarkiaBlockEntities.WOOD_RACK.get(), pos, state),
            Set.of(AutarkiaBlocks.WOOD_RACK.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.autarkia.colony.PatternLoomBlockEntity>> PATTERN_LOOM =
        type("pattern_loom", dev.autarkia.colony.PatternLoomBlockEntity::new, AutarkiaBlocks.PATTERN_LOOM);

    private AutarkiaBlockEntities() {
    }

    private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> type(String id, BlockEntityType.BlockEntitySupplier<T> factory,
                                                                                                    Supplier<? extends Block> block) {
        return TYPES.register(id, () -> new BlockEntityType<>(factory, Set.of(block.get())));
    }

    /** Register the deferred register and the capability providers. */
    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        modBus.addListener(AutarkiaBlockEntities::capabilities);
    }

    private static BlockEntity center(BlockEntity be) {
        if (be instanceof IMultiBlockEntity m && !m.isCenter() && be.getLevel() != null) {
            BlockEntity c = be.getLevel().getBlockEntity(m.getCenter());
            return c == null ? be : c;
        }
        return be;
    }

    private static void capabilities(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(Capabilities.Energy.BLOCK, HAND_CRANK.get(),
            (be, side) -> side == MachineBlock.front(be.getBlockState()) ? null : be.buffer());
        e.registerBlockEntity(Capabilities.Energy.BLOCK, ROPE_PULLEY.get(), (be, side) -> be.pool());
        e.registerBlockEntity(Capabilities.Item.BLOCK, QUERN.get(), (be, side) -> be.inventory().automation());
        e.registerBlockEntity(Capabilities.Item.BLOCK, STAMP_MILL.get(),
            (be, side) -> center(be) instanceof MachineBlockEntity m ? m.inventory().automation() : null);
        e.registerBlockEntity(Capabilities.Energy.BLOCK, STAMP_MILL.get(),
            (be, side) -> center(be) instanceof MachineBlockEntity m ? m.energy() : null);
        e.registerBlockEntity(Capabilities.Item.BLOCK, CRUCIBLE.get(), (be, side) -> be.inventory().automation());
        e.registerBlockEntity(Capabilities.Item.BLOCK, CRATE.get(), (be, side) -> be.items());
        e.registerBlockEntity(Capabilities.Item.BLOCK, CHUTE.get(), (be, side) -> be.intake());
        e.registerBlockEntity(Capabilities.Energy.BLOCK, TREADWHEEL.get(),
            (be, side) -> center(be) instanceof dev.autarkia.energy.TreadwheelBlockEntity t ? t.buffer() : null);
        e.registerBlockEntity(Capabilities.Energy.BLOCK, WINDMILL.get(),
            (be, side) -> center(be) instanceof dev.autarkia.energy.WindmillBlockEntity w ? w.buffer() : null);
        e.registerBlockEntity(Capabilities.Energy.BLOCK, WINDING_POST.get(), (be, side) -> be.gridBuffer());
        e.registerBlockEntity(Capabilities.Energy.BLOCK, WORKER_ASSEMBLER.get(),
            (be, side) -> center(be) instanceof dev.autarkia.worker.WorkerAssemblerBlockEntity a ? a.energy() : null);
        e.registerBlockEntity(Capabilities.Item.BLOCK, WORKER_ASSEMBLER.get(),
            (be, side) -> center(be) instanceof dev.autarkia.worker.WorkerAssemblerBlockEntity a ? a.inventory().automation() : null);
        e.registerBlockEntity(Capabilities.Energy.BLOCK, STONE_FLYWHEEL.get(),
            (be, side) -> center(be) instanceof dev.autarkia.energy.StoneFlywheelBlockEntity f && side != MachineBlock.front(f.getBlockState()) ? f.buffer() : null);
        e.registerBlockEntity(Capabilities.Energy.BLOCK, INDUSTRIAL_LAMP.get(), (be, side) -> be.gridBuffer());
        e.registerBlockEntity(Capabilities.Energy.BLOCK, WOODEN_POLE.get(),
            (be, side) -> center(be) instanceof dev.autarkia.energy.WoodenPoleBlockEntity p ? p.pool() : null);
        e.registerBlockEntity(Capabilities.Energy.BLOCK, CAPACITOR_BANK.get(), (be, side) -> be.buffer());
        e.registerBlockEntity(Capabilities.Energy.BLOCK, WATERWHEEL.get(),
            (be, side) -> center(be) instanceof dev.autarkia.energy.WaterwheelBlockEntity w ? w.buffer() : null);
        e.registerBlockEntity(Capabilities.Energy.BLOCK, STEAM_ENGINE.get(),
            (be, side) -> center(be) instanceof dev.autarkia.energy.SteamEngineBlockEntity s ? s.buffer() : null);
        e.registerBlockEntity(Capabilities.Fluid.BLOCK, BOILER.get(),
            (be, side) -> center(be) instanceof dev.autarkia.energy.BoilerBlockEntity b ? b.water() : null);
        e.registerBlockEntity(Capabilities.Item.BLOCK, BOILER.get(),
            (be, side) -> center(be) instanceof dev.autarkia.energy.BoilerBlockEntity b ? b.inventory().automation() : null);
        for (var t : java.util.List.of(SLUICE.get(), BLAST_SMELTER.get())) {
            e.registerBlockEntity(Capabilities.Item.BLOCK, t, (be, side) -> center(be) instanceof MachineBlockEntity m ? m.inventory().automation() : null);
            e.registerBlockEntity(Capabilities.Energy.BLOCK, t, (be, side) -> center(be) instanceof MachineBlockEntity m ? m.energy() : null);
        }
        for (var t : java.util.List.of(PLATE_PRESS.get(), WIRE_DRAWER.get())) {
            e.registerBlockEntity(Capabilities.Item.BLOCK, t, (be, side) -> be.inventory().automation());
            e.registerBlockEntity(Capabilities.Energy.BLOCK, t, (be, side) -> be.energy());
        }
        e.registerBlockEntity(Capabilities.Item.BLOCK, BELT.get(), (be, side) -> be.intake(side));
        e.registerBlockEntity(Capabilities.Energy.BLOCK, BELT.get(), (be, side) -> be.lineBuffer());
        for (var t : java.util.List.of(BELT_SPLITTER.get(), SORTING_CHUTE.get())) {
            e.registerBlockEntity(Capabilities.Item.BLOCK, t, (be, side) -> side == be.facing().getOpposite() ? be.intake() : null);
        }
        e.registerBlockEntity(Capabilities.Energy.BLOCK, BELT_SPLITTER.get(), (be, side) -> be.energy());
        e.registerBlockEntity(Capabilities.Item.BLOCK, WOOD_RACK.get(), (be, side) -> be.items());
    }
}

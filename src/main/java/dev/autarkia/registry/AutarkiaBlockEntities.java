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
        e.registerBlockEntity(Capabilities.Energy.BLOCK, STONE_FLYWHEEL.get(),
            (be, side) -> center(be) instanceof dev.autarkia.energy.StoneFlywheelBlockEntity f && side != MachineBlock.front(f.getBlockState()) ? f.buffer() : null);
    }
}

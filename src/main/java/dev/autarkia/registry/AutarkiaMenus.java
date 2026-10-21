package dev.autarkia.registry;

import dev.autarkia.Autarkia;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Menu types (08b section 3); every block menu is a {@link SlotMenu} with its generated layout. */
public final class AutarkiaMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Autarkia.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> STAMP_MILL = menu("stamp_mill", GuiLayouts.STAMP_MILL);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> CRUCIBLE = menu("crucible", GuiLayouts.CRUCIBLE);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> CRATE = menu("crate", GuiLayouts.CRATE);

    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> WORKER_ASSEMBLER = menu("worker_assembler", GuiLayouts.WORKER_ASSEMBLER);
    public static final DeferredHolder<MenuType<?>, MenuType<dev.autarkia.colony.ColonyMenu>> COLONY_CORE = colony("colony_core",
        dev.autarkia.colony.ColonyMenu.Mode.CORE);
    public static final DeferredHolder<MenuType<?>, MenuType<dev.autarkia.colony.ColonyMenu>> PROJECT_BOARD = colony("project_board",
        dev.autarkia.colony.ColonyMenu.Mode.BOARD);
    public static final DeferredHolder<MenuType<?>, MenuType<dev.autarkia.colony.ColonyMenu>> PLANNING_TABLET = colony("planning_tablet",
        dev.autarkia.colony.ColonyMenu.Mode.TABLET);

    public static final DeferredHolder<MenuType<?>, MenuType<dev.autarkia.energy.BoilerMenu>> BOILER = MENUS.register("boiler",
        () -> IMenuTypeExtension.create(dev.autarkia.energy.BoilerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> ENERGY_STORAGE = menu("energy_storage", GuiLayouts.ENERGY_STORAGE);

    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> SLUICE = menu("sluice", GuiLayouts.SLUICE);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> BLAST_SMELTER = menu("blast_smelter", GuiLayouts.BLAST_SMELTER);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> PLATE_PRESS = menu("plate_press", GuiLayouts.PLATE_PRESS);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> WIRE_DRAWER = menu("wire_drawer", GuiLayouts.WIRE_DRAWER);
    public static final DeferredHolder<MenuType<?>, MenuType<dev.autarkia.logistics.FilterMenu>> BELT_SPLITTER = filter("belt_splitter");
    public static final DeferredHolder<MenuType<?>, MenuType<dev.autarkia.logistics.FilterMenu>> SORTING_CHUTE = filter("sorting_chute");
    public static final DeferredHolder<MenuType<?>, MenuType<dev.autarkia.storage.RackMenu>> RACK = MENUS.register("rack",
        () -> IMenuTypeExtension.create(dev.autarkia.storage.RackMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.autarkia.colony.PatternLoomMenu>> PATTERN_LOOM = MENUS.register("pattern_loom",
        () -> IMenuTypeExtension.create(dev.autarkia.colony.PatternLoomMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> FUSE_BOX = menu("fuse_box", GuiLayouts.FUSE_BOX);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> STEAM_TURBINE = menu("steam_turbine", GuiLayouts.STEAM_TURBINE);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> JAW_CRUSHER = menu("jaw_crusher", GuiLayouts.JAW_CRUSHER);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> MAGNETIC_SEPARATOR = menu("magnetic_separator", GuiLayouts.MAGNETIC_SEPARATOR);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> ELECTROLYZER = menu("electrolyzer", GuiLayouts.ELECTROLYZER);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> FABRICATOR = menu("fabricator", GuiLayouts.FABRICATOR);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> WAREHOUSE_CONTROLLER = menu("warehouse_controller", GuiLayouts.WAREHOUSE_CONTROLLER);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> LEDGER_TERMINAL = menu("ledger_terminal", GuiLayouts.LEDGER_TERMINAL);
    public static final DeferredHolder<MenuType<?>, MenuType<dev.autarkia.logistics.CapsuleStationMenu>> CAPSULE_STATION = MENUS.register("capsule_station",
        () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(dev.autarkia.logistics.CapsuleStationMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> FROTH_FLOTATION_PLANT = menu("froth_flotation_plant", GuiLayouts.FROTH_FLOTATION_PLANT);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> OUTPOST_CORE = menu("outpost_core", GuiLayouts.OUTPOST_CORE);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> REPLICATOR_FOUNDRY = menu("replicator_foundry", GuiLayouts.REPLICATOR_FOUNDRY);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> RESTORATION_PROJECTOR = menu("restoration_projector",
        GuiLayouts.RESTORATION_PROJECTOR);
    public static final DeferredHolder<MenuType<?>, MenuType<dev.autarkia.logistics.CableCarStationMenu>> CABLE_CAR_STATION = MENUS.register(
        "cable_car_station", () -> IMenuTypeExtension.create(dev.autarkia.logistics.CableCarStationMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> CRATE_LAUNCHER = menu("crate_launcher", GuiLayouts.CRATE_LAUNCHER);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> DEPOT = menu("depot", GuiLayouts.DEPOT);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> PLASMA_SEPARATOR = menu("plasma_separator", GuiLayouts.PLASMA_SEPARATOR);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> CRYO_NODE = menu("cryo_node", GuiLayouts.CRYO_NODE);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> SUPERCONDUCTING_COIL = menu("superconducting_coil",
        GuiLayouts.SUPERCONDUCTING_COIL);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> PLASMA_GENERATOR = menu("plasma_generator", GuiLayouts.PLASMA_GENERATOR);
    public static final DeferredHolder<MenuType<?>, MenuType<dev.autarkia.storage.MatterBufferMenu>> MATTER_BUFFER = MENUS.register("matter_buffer",
        () -> IMenuTypeExtension.create(dev.autarkia.storage.MatterBufferMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.autarkia.logistics.MagRailStationMenu>> MAG_RAIL_STATION = MENUS.register(
        "mag_rail_station", () -> IMenuTypeExtension.create(dev.autarkia.logistics.MagRailStationMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> NANITE_HIVE = menu("nanite_hive", GuiLayouts.NANITE_HIVE);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> FORGE_OF_ASCENSION = menu("forge_of_ascension", GuiLayouts.FORGE_OF_ASCENSION);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> SEED_MATRIX = menu("seed_matrix", GuiLayouts.SEED_MATRIX);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> MATTER_COMPILER = menu("matter_compiler", GuiLayouts.MATTER_COMPILER);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> MATTER_CONDUIT = menu("matter_conduit", GuiLayouts.MATTER_CONDUIT);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> DOMAIN_ARCHIVE = menu("domain_archive", GuiLayouts.DOMAIN_ARCHIVE);
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMenu>> SINGULARITY_CORE = menu("singularity_core", GuiLayouts.SINGULARITY_CORE);

    private AutarkiaMenus() {
    }

    private static DeferredHolder<MenuType<?>, MenuType<dev.autarkia.logistics.FilterMenu>> filter(String id) {
        java.util.concurrent.atomic.AtomicReference<DeferredHolder<MenuType<?>, MenuType<dev.autarkia.logistics.FilterMenu>>> self =
            new java.util.concurrent.atomic.AtomicReference<>();
        self.set(MENUS.register(id, () -> IMenuTypeExtension.create((windowId, inv, buf) -> new dev.autarkia.logistics.FilterMenu(self.get().get(), windowId,
            inv, buf))));
        return self.get();
    }

    private static DeferredHolder<MenuType<?>, MenuType<dev.autarkia.colony.ColonyMenu>> colony(String id, dev.autarkia.colony.ColonyMenu.Mode mode) {
        return MENUS.register(id, () -> IMenuTypeExtension.create((windowId, inv, buf) -> dev.autarkia.colony.ColonyMenu.client(mode, windowId, inv, buf)));
    }

    private static DeferredHolder<MenuType<?>, MenuType<SlotMenu>> menu(String id, GuiLayout layout) {
        java.util.concurrent.atomic.AtomicReference<DeferredHolder<MenuType<?>, MenuType<SlotMenu>>> self = new java.util.concurrent.atomic.AtomicReference<>();
        self.set(MENUS.register(id, () -> IMenuTypeExtension.create((windowId, inv, buf) -> new SlotMenu(self.get().get(), windowId, inv, buf, layout))));
        return self.get();
    }
}

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

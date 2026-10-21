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

    private AutarkiaMenus() {
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

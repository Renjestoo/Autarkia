package dev.autarkia.registry;

import dev.autarkia.Autarkia;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * One creative tab per tier (08b section 18). Items join with {@link #add(int, Supplier)} during their static
 * registration; {@link #register(IEventBus)} then registers the tabs of the tiers that hold items.
 */
public final class AutarkiaTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Autarkia.MOD_ID);
    private static final List<List<Supplier<? extends ItemLike>>> BY_TIER = new ArrayList<>();

    static {
        for (int t = 1; t <= 6; t++) {
            BY_TIER.add(new ArrayList<>());
        }
    }

    private AutarkiaTabs() {
    }

    /** Put an item into the tab of a tier (1..6); the first item of a tier is its tab icon. */
    public static void add(int tier, Supplier<? extends ItemLike> item) {
        BY_TIER.get(tier - 1).add(item);
    }

    /** Register the tabs of every tier that holds at least one item. */
    public static void register(IEventBus bus) {
        String previous = null;
        for (int t = 1; t <= 6; t++) {
            List<Supplier<? extends ItemLike>> items = BY_TIER.get(t - 1);
            if (items.isEmpty()) {
                continue;
            }
            final int tier = t;
            final String before = previous;
            TABS.register("tier_" + t, () -> {
                CreativeModeTab.Builder b = CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.autarkia.tier_" + tier))
                    .icon(() -> new ItemStack(items.get(0).get()))
                    .displayItems((params, out) -> items.forEach(s -> out.accept(s.get())));
                if (before != null) {
                    b.withTabsBefore(Autarkia.id(before));
                }
                return b.build();
            });
            previous = "tier_" + t;
        }
        TABS.register(bus);
    }
}

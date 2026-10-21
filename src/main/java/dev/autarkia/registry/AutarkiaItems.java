package dev.autarkia.registry;

import dev.autarkia.Autarkia;
import dev.autarkia.energy.CableItem;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.MultimeterItem;
import dev.autarkia.energy.WrenchItem;
import java.util.function.Function;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Every Autarkia item and block item (08a), registered in creative tab order per tier. */
public final class AutarkiaItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Autarkia.MOD_ID);

    public static final DeferredItem<BlockItem> HAND_CRANK = block(1, AutarkiaBlocks.HAND_CRANK);
    public static final DeferredItem<BlockItem> ROPE_PULLEY = block(1, AutarkiaBlocks.ROPE_PULLEY);
    public static final DeferredItem<CableItem> ROPE_COIL = item(1, "rope_coil", p -> new CableItem(p, CableTier.ROPE));
    public static final DeferredItem<BlockItem> QUERN = block(1, AutarkiaBlocks.QUERN);
    public static final DeferredItem<BlockItem> STAMP_MILL = block(1, AutarkiaBlocks.STAMP_MILL);
    public static final DeferredItem<BlockItem> CRUCIBLE = block(1, AutarkiaBlocks.CRUCIBLE);
    public static final DeferredItem<BlockItem> CRATE = block(1, AutarkiaBlocks.CRATE);
    public static final DeferredItem<BlockItem> TALLY_BOARD = block(1, AutarkiaBlocks.TALLY_BOARD);
    public static final DeferredItem<BlockItem> CHUTE = block(1, AutarkiaBlocks.CHUTE);
    public static final DeferredItem<BlockItem> FOUNDATION = block(1, AutarkiaBlocks.FOUNDATION);
    public static final DeferredItem<WrenchItem> WRENCH = item(1, "wrench", WrenchItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<MultimeterItem> MULTIMETER = item(1, "multimeter", MultimeterItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<BlockItem> TIN_ORE = block(1, AutarkiaBlocks.TIN_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_TIN_ORE = block(1, AutarkiaBlocks.DEEPSLATE_TIN_ORE);
    public static final DeferredItem<BlockItem> RAW_TIN_BLOCK = block(1, AutarkiaBlocks.RAW_TIN_BLOCK);
    public static final DeferredItem<BlockItem> TIN_BLOCK = block(1, AutarkiaBlocks.TIN_BLOCK);
    public static final DeferredItem<BlockItem> BRONZE_BLOCK = block(1, AutarkiaBlocks.BRONZE_BLOCK);
    public static final DeferredItem<Item> RAW_TIN = simple(1, "raw_tin");
    public static final DeferredItem<Item> TIN_INGOT = simple(1, "tin_ingot");
    public static final DeferredItem<Item> TIN_NUGGET = simple(1, "tin_nugget");
    public static final DeferredItem<Item> BRONZE_INGOT = simple(1, "bronze_ingot");
    public static final DeferredItem<Item> BRONZE_NUGGET = simple(1, "bronze_nugget");
    public static final DeferredItem<Item> CRUSHED_IRON = simple(1, "crushed_iron");
    public static final DeferredItem<Item> CRUSHED_COPPER = simple(1, "crushed_copper");
    public static final DeferredItem<Item> CRUSHED_GOLD = simple(1, "crushed_gold");
    public static final DeferredItem<Item> CRUSHED_TIN = simple(1, "crushed_tin");
    public static final DeferredItem<Item> BRONZE_GEAR = simple(1, "bronze_gear");
    public static final DeferredItem<Item> MAINSPRING = simple(1, "mainspring");

    private AutarkiaItems() {
    }

    private static DeferredItem<BlockItem> block(int tier, DeferredBlock<? extends Block> block) {
        DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(block.getId().getPath(), block);
        AutarkiaTabs.add(tier, item);
        return item;
    }

    private static DeferredItem<Item> simple(int tier, String id) {
        DeferredItem<Item> item = ITEMS.registerSimpleItem(id);
        AutarkiaTabs.add(tier, item);
        return item;
    }

    private static <I extends Item> DeferredItem<I> item(int tier, String id, Function<Item.Properties, I> factory) {
        return item(tier, id, factory, p -> p);
    }

    private static <I extends Item> DeferredItem<I> item(int tier, String id, Function<Item.Properties, I> factory,
                                                          java.util.function.UnaryOperator<Item.Properties> props) {
        DeferredItem<I> item = ITEMS.registerItem(id, factory, props);
        AutarkiaTabs.add(tier, item);
        return item;
    }
}

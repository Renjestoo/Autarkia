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
    public static final DeferredItem<BlockItem> TREADWHEEL = block(1, AutarkiaBlocks.TREADWHEEL);
    public static final DeferredItem<BlockItem> WINDMILL = block(1, AutarkiaBlocks.WINDMILL);
    public static final DeferredItem<BlockItem> STONE_FLYWHEEL = block(1, AutarkiaBlocks.STONE_FLYWHEEL);
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
    public static final DeferredItem<Item> BRONZE_PICKAXE = item(1, "bronze_pickaxe", Item::new,
        p -> p.pickaxe(dev.autarkia.equipment.AutarkiaMaterials.BRONZE, 1.0F, -2.8F));
    public static final DeferredItem<Item> BRONZE_AXE = item(1, "bronze_axe", Item::new,
        p -> p.axe(dev.autarkia.equipment.AutarkiaMaterials.BRONZE, 6.0F, -3.1F));
    public static final DeferredItem<Item> BRONZE_SHOVEL = item(1, "bronze_shovel", Item::new,
        p -> p.shovel(dev.autarkia.equipment.AutarkiaMaterials.BRONZE, 1.5F, -3.0F));
    public static final DeferredItem<Item> BRONZE_HOE = item(1, "bronze_hoe", Item::new,
        p -> p.hoe(dev.autarkia.equipment.AutarkiaMaterials.BRONZE, -2.0F, -1.0F));
    public static final DeferredItem<Item> BRONZE_SWORD = item(1, "bronze_sword", Item::new,
        p -> p.sword(dev.autarkia.equipment.AutarkiaMaterials.BRONZE, 3.0F, -2.4F));
    public static final DeferredItem<Item> BRONZE_HELMET = armor("bronze_helmet", net.minecraft.world.item.equipment.ArmorType.HELMET);
    public static final DeferredItem<Item> BRONZE_CHEST = armor("bronze_chest", net.minecraft.world.item.equipment.ArmorType.CHESTPLATE);
    public static final DeferredItem<Item> BRONZE_LEGS = armor("bronze_legs", net.minecraft.world.item.equipment.ArmorType.LEGGINGS);
    public static final DeferredItem<Item> BRONZE_BOOTS = armor("bronze_boots", net.minecraft.world.item.equipment.ArmorType.BOOTS);
    public static final DeferredItem<dev.autarkia.equipment.SlingItem> BRONZE_SLING = item(1, "bronze_sling", dev.autarkia.equipment.SlingItem::new,
        p -> p.durability(250).repairable(net.minecraft.world.item.Items.STRING));

    public static final DeferredItem<dev.autarkia.colony.CoreSeedItem> CORE_SEED = item(1, "core_seed", dev.autarkia.colony.CoreSeedItem::new,
        p -> p.stacksTo(16));
    public static final DeferredItem<BlockItem> PROJECT_BOARD = block(1, AutarkiaBlocks.PROJECT_BOARD);
    public static final DeferredItem<dev.autarkia.colony.PlanningTabletItem> PLANNING_TABLET = item(1, "planning_tablet",
        dev.autarkia.colony.PlanningTabletItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<BlockItem> WORKER_ASSEMBLER = block(1, AutarkiaBlocks.WORKER_ASSEMBLER);
    public static final DeferredItem<BlockItem> WINDING_POST = block(1, AutarkiaBlocks.WINDING_POST);
    public static final DeferredItem<Item> WALKER_FRAME = simple(1, "walker_frame");
    public static final DeferredItem<dev.autarkia.worker.WorkerCapsuleItem> WORKER_CAPSULE = item(1, "worker_capsule",
        dev.autarkia.worker.WorkerCapsuleItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<dev.autarkia.colony.FieldManualItem> FIELD_MANUAL = item(1, "field_manual", dev.autarkia.colony.FieldManualItem::new,
        p -> p.stacksTo(1));

    public static final DeferredItem<BlockItem> WOODEN_POLE = block(2, AutarkiaBlocks.WOODEN_POLE);
    public static final DeferredItem<CableItem> COPPER_WIRE_SPOOL = item(2, "copper_wire_spool", p -> new CableItem(p, CableTier.COPPER));
    public static final DeferredItem<BlockItem> INDUSTRIAL_LAMP = block(2, AutarkiaBlocks.INDUSTRIAL_LAMP);
    public static final DeferredItem<BlockItem> WATERWHEEL = block(2, AutarkiaBlocks.WATERWHEEL);
    public static final DeferredItem<BlockItem> BOILER = block(2, AutarkiaBlocks.BOILER);
    public static final DeferredItem<BlockItem> PRESSURE_VALVE = block(2, AutarkiaBlocks.PRESSURE_VALVE);
    public static final DeferredItem<BlockItem> STEAM_ENGINE = block(2, AutarkiaBlocks.STEAM_ENGINE);
    public static final DeferredItem<BlockItem> LIGHTNING_MAST = block(2, AutarkiaBlocks.LIGHTNING_MAST);
    public static final DeferredItem<BlockItem> LIGHTNING_ARRESTER = block(2, AutarkiaBlocks.LIGHTNING_ARRESTER);
    public static final DeferredItem<BlockItem> CAPACITOR_BANK = block(2, AutarkiaBlocks.CAPACITOR_BANK);
    public static final DeferredItem<BlockItem> CATWALK = block(2, AutarkiaBlocks.CATWALK);
    public static final DeferredItem<BlockItem> CATWALK_STAIRS = block(2, AutarkiaBlocks.CATWALK_STAIRS);
    public static final DeferredItem<BlockItem> HAZARD_BLOCK = block(2, AutarkiaBlocks.HAZARD_BLOCK);
    public static final DeferredItem<dev.autarkia.colony.CoreUpgradeItem> CORE_UPGRADE_T2 = item(2, "core_upgrade_t2",
        p -> new dev.autarkia.colony.CoreUpgradeItem(p, 2), p -> p.stacksTo(16));

    public static final DeferredItem<BlockItem> ZINC_ORE = block(2, AutarkiaBlocks.ZINC_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_ZINC_ORE = block(2, AutarkiaBlocks.DEEPSLATE_ZINC_ORE);
    public static final DeferredItem<BlockItem> RAW_ZINC_BLOCK = block(2, AutarkiaBlocks.RAW_ZINC_BLOCK);
    public static final DeferredItem<BlockItem> ZINC_BLOCK = block(2, AutarkiaBlocks.ZINC_BLOCK);
    public static final DeferredItem<BlockItem> BRASS_BLOCK = block(2, AutarkiaBlocks.BRASS_BLOCK);
    public static final DeferredItem<BlockItem> STEEL_BLOCK = block(2, AutarkiaBlocks.STEEL_BLOCK);
    public static final DeferredItem<BlockItem> SLUICE = block(2, AutarkiaBlocks.SLUICE);
    public static final DeferredItem<BlockItem> BLAST_SMELTER = block(2, AutarkiaBlocks.BLAST_SMELTER);
    public static final DeferredItem<BlockItem> PLATE_PRESS = block(2, AutarkiaBlocks.PLATE_PRESS);
    public static final DeferredItem<BlockItem> WIRE_DRAWER = block(2, AutarkiaBlocks.WIRE_DRAWER);
    public static final DeferredItem<BlockItem> BELT = block(2, AutarkiaBlocks.BELT);
    public static final DeferredItem<BlockItem> BELT_SPLITTER = block(2, AutarkiaBlocks.BELT_SPLITTER);
    public static final DeferredItem<BlockItem> SORTING_CHUTE = block(2, AutarkiaBlocks.SORTING_CHUTE);
    public static final DeferredItem<BlockItem> WOOD_RACK = block(2, AutarkiaBlocks.WOOD_RACK);
    public static final DeferredItem<BlockItem> PATTERN_LOOM = block(2, AutarkiaBlocks.PATTERN_LOOM);
    public static final DeferredItem<Item> RAW_ZINC = simple(2, "raw_zinc");
    public static final DeferredItem<Item> ZINC_INGOT = simple(2, "zinc_ingot");
    public static final DeferredItem<Item> ZINC_NUGGET = simple(2, "zinc_nugget");
    public static final DeferredItem<Item> BRASS_INGOT = simple(2, "brass_ingot");
    public static final DeferredItem<Item> STEEL_INGOT = simple(2, "steel_ingot");
    public static final DeferredItem<Item> STEEL_NUGGET = simple(2, "steel_nugget");
    public static final DeferredItem<Item> SLAG = simple(2, "slag");
    public static final DeferredItem<Item> CRUSHED_ZINC = simple(2, "crushed_zinc");
    public static final DeferredItem<Item> WASHED_IRON = simple(2, "washed_iron");
    public static final DeferredItem<Item> WASHED_COPPER = simple(2, "washed_copper");
    public static final DeferredItem<Item> WASHED_GOLD = simple(2, "washed_gold");
    public static final DeferredItem<Item> WASHED_TIN = simple(2, "washed_tin");
    public static final DeferredItem<Item> WASHED_ZINC = simple(2, "washed_zinc");
    public static final DeferredItem<Item> STEEL_GEAR = simple(2, "steel_gear");
    public static final DeferredItem<Item> COPPER_COIL = simple(2, "copper_coil");
    public static final DeferredItem<Item> STEEL_PLATE = simple(2, "steel_plate");
    public static final DeferredItem<Item> BRASS_PLATE = simple(2, "brass_plate");
    public static final DeferredItem<Item> CRAB_FRAME = simple(2, "crab_frame");
    public static final DeferredItem<dev.autarkia.colony.PatternCardItem> PATTERN_CARD = item(2, "pattern_card", dev.autarkia.colony.PatternCardItem::new,
        p -> p.stacksTo(16));
    public static final DeferredItem<dev.autarkia.energy.WireCutterItem> WIRE_CUTTER = item(2, "wire_cutter", dev.autarkia.energy.WireCutterItem::new,
        p -> p.durability(250).repairable(STEEL_INGOT.get()));
    public static final DeferredItem<Item> STEEL_PICKAXE = item(2, "steel_pickaxe", Item::new,
        p -> p.pickaxe(dev.autarkia.equipment.AutarkiaMaterials.STEEL, 1.0F, -2.8F));
    public static final DeferredItem<Item> STEEL_AXE = item(2, "steel_axe", Item::new,
        p -> p.axe(dev.autarkia.equipment.AutarkiaMaterials.STEEL, 5.5F, -3.0F));
    public static final DeferredItem<Item> STEEL_SHOVEL = item(2, "steel_shovel", Item::new,
        p -> p.shovel(dev.autarkia.equipment.AutarkiaMaterials.STEEL, 1.5F, -3.0F));
    public static final DeferredItem<Item> STEEL_SWORD = item(2, "steel_sword", Item::new,
        p -> p.sword(dev.autarkia.equipment.AutarkiaMaterials.STEEL, 3.0F, -2.4F));
    public static final DeferredItem<dev.autarkia.equipment.SledgehammerItem> STEEL_SLEDGEHAMMER = item(2, "steel_sledgehammer",
        dev.autarkia.equipment.SledgehammerItem::new, p -> p.tool(dev.autarkia.equipment.AutarkiaMaterials.STEEL, net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE,
            7.0F, -3.4F, 5.0F).durability(2400).repairable(STEEL_PLATE.get())
            .component(AutarkiaComponents.TOOL_MODE.get(), dev.autarkia.equipment.ToolMode.SINGLE));
    public static final DeferredItem<Item> STEEL_HELMET = steelArmor("steel_helmet", net.minecraft.world.item.equipment.ArmorType.HELMET);
    public static final DeferredItem<Item> STEEL_CHEST = steelArmor("steel_chest", net.minecraft.world.item.equipment.ArmorType.CHESTPLATE);
    public static final DeferredItem<Item> STEEL_LEGS = steelArmor("steel_legs", net.minecraft.world.item.equipment.ArmorType.LEGGINGS);
    public static final DeferredItem<Item> STEEL_BOOTS = steelArmor("steel_boots", net.minecraft.world.item.equipment.ArmorType.BOOTS);
    public static final DeferredItem<dev.autarkia.equipment.SteamRepeaterItem> STEAM_REPEATER = item(2, "steam_repeater",
        dev.autarkia.equipment.SteamRepeaterItem::new, p -> p.durability(1000).repairable(STEEL_PLATE.get()).component(AutarkiaComponents.PRESSURE.get(), 0));

    private AutarkiaItems() {
    }

    private static DeferredItem<Item> steelArmor(String id, net.minecraft.world.item.equipment.ArmorType type) {
        return item(2, id, Item::new, p -> p.humanoidArmor(dev.autarkia.equipment.AutarkiaMaterials.STEEL_RIG, type)
            .attributes(dev.autarkia.equipment.AutarkiaMaterials.steelRig(type)));
    }

    private static DeferredItem<BlockItem> block(int tier, DeferredBlock<? extends Block> block) {
        DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(block.getId().getPath(), block);
        AutarkiaTabs.add(tier, item);
        return item;
    }

    private static DeferredItem<Item> armor(String id, net.minecraft.world.item.equipment.ArmorType type) {
        return item(1, id, Item::new, p -> p.humanoidArmor(dev.autarkia.equipment.AutarkiaMaterials.BRONZE_ARMOR, type));
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

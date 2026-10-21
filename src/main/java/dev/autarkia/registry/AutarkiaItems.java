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

    public static final DeferredItem<BlockItem> STEEL_PYLON = block(3, AutarkiaBlocks.STEEL_PYLON);
    public static final DeferredItem<BlockItem> WALL_INSULATOR = block(3, AutarkiaBlocks.WALL_INSULATOR);
    public static final DeferredItem<BlockItem> TRANSFORMER = block(3, AutarkiaBlocks.TRANSFORMER);
    public static final DeferredItem<BlockItem> FUSE_BOX = block(3, AutarkiaBlocks.FUSE_BOX);
    public static final DeferredItem<BlockItem> STEAM_TURBINE = block(3, AutarkiaBlocks.STEAM_TURBINE);
    public static final DeferredItem<BlockItem> BATTERY_BANK = block(3, AutarkiaBlocks.BATTERY_BANK);
    public static final DeferredItem<CableItem> INSULATED_CABLE_SPOOL = item(3, "insulated_cable_spool", p -> new CableItem(p, CableTier.INSULATED));
    public static final DeferredItem<BlockItem> STEEL_RACK = block(3, AutarkiaBlocks.STEEL_RACK);
    public static final DeferredItem<BlockItem> JAW_CRUSHER = block(3, AutarkiaBlocks.JAW_CRUSHER);
    public static final DeferredItem<BlockItem> MAGNETIC_SEPARATOR = block(3, AutarkiaBlocks.MAGNETIC_SEPARATOR);
    public static final DeferredItem<BlockItem> ELECTROLYZER = block(3, AutarkiaBlocks.ELECTROLYZER);
    public static final DeferredItem<BlockItem> FABRICATOR = block(3, AutarkiaBlocks.FABRICATOR);
    public static final DeferredItem<Item> PURE_IRON = simple(3, "pure_iron");
    public static final DeferredItem<Item> PURE_COPPER = simple(3, "pure_copper");
    public static final DeferredItem<Item> PURE_GOLD = simple(3, "pure_gold");
    public static final DeferredItem<Item> PURE_TIN = simple(3, "pure_tin");
    public static final DeferredItem<Item> PURE_ZINC = simple(3, "pure_zinc");
    public static final DeferredItem<Item> BASIC_CIRCUIT = simple(3, "basic_circuit");
    public static final DeferredItem<Item> SERVO = simple(3, "servo");
    public static final DeferredItem<dev.autarkia.equipment.PoweredGeoItem> ELECTRIC_DRILL = item(3, "electric_drill",
        p -> new dev.autarkia.equipment.PoweredGeoItem(p, 400000, 2000, 200, "electric_drill", "spin"),
        p -> fe(p.pickaxe(dev.autarkia.equipment.AutarkiaMaterials.ELECTRIC, 1.0F, -3.0F)).component(net.minecraft.core.component.DataComponents.TOOL,
            drillTool()));
    public static final DeferredItem<dev.autarkia.equipment.PoweredGeoItem> ELECTRIC_CHAINSAW = item(3, "electric_chainsaw",
        p -> new dev.autarkia.equipment.PoweredGeoItem(p, 400000, 2000, dev.autarkia.equipment.TreeFelling.LOG_COST, "electric_chainsaw", "run"),
        p -> fe(p.axe(dev.autarkia.equipment.AutarkiaMaterials.ELECTRIC, 5.0F, -3.2F)));
    public static final DeferredItem<dev.autarkia.equipment.ShockBatonItem> SHOCK_BATON = item(3, "shock_baton", dev.autarkia.equipment.ShockBatonItem::new,
        p -> fe(p.sword(dev.autarkia.equipment.AutarkiaMaterials.ELECTRIC, 2.0F, -2.4F)));
    public static final DeferredItem<dev.autarkia.equipment.ArcRifleItem> ARC_RIFLE = item(3, "arc_rifle", dev.autarkia.equipment.ArcRifleItem::new,
        p -> fe(p).stacksTo(1).useCooldown(1.0F));
    public static final DeferredItem<dev.autarkia.equipment.SuitItem> EXO_RIG_HELMET = exoRig("exo_rig_helmet", net.minecraft.world.item.equipment.ArmorType.HELMET);
    public static final DeferredItem<dev.autarkia.equipment.SuitItem> EXO_RIG_CHEST = exoRig("exo_rig_chest", net.minecraft.world.item.equipment.ArmorType.CHESTPLATE);
    public static final DeferredItem<dev.autarkia.equipment.SuitItem> EXO_RIG_LEGS = exoRig("exo_rig_legs", net.minecraft.world.item.equipment.ArmorType.LEGGINGS);
    public static final DeferredItem<dev.autarkia.equipment.SuitItem> EXO_RIG_BOOTS = exoRig("exo_rig_boots", net.minecraft.world.item.equipment.ArmorType.BOOTS);
    public static final DeferredItem<BlockItem> WAREHOUSE_CONTROLLER = block(3, AutarkiaBlocks.WAREHOUSE_CONTROLLER);
    public static final DeferredItem<BlockItem> LEDGER_TERMINAL = block(3, AutarkiaBlocks.LEDGER_TERMINAL);
    public static final DeferredItem<BlockItem> RECHARGE_DOCK = block(3, AutarkiaBlocks.RECHARGE_DOCK);
    public static final DeferredItem<BlockItem> CAPSULE_TUBE = block(3, AutarkiaBlocks.CAPSULE_TUBE);
    public static final DeferredItem<BlockItem> CAPSULE_STATION = block(3, AutarkiaBlocks.CAPSULE_STATION);
    public static final DeferredItem<BlockItem> DOMAIN_BEACON = block(3, AutarkiaBlocks.DOMAIN_BEACON);
    public static final DeferredItem<BlockItem> PROP_BEAM = block(3, AutarkiaBlocks.PROP_BEAM);
    public static final DeferredItem<BlockItem> CABLE_CAR_STATION = block(4, AutarkiaBlocks.CABLE_CAR_STATION);
    public static final DeferredItem<BlockItem> CRATE_LAUNCHER = block(4, AutarkiaBlocks.CRATE_LAUNCHER);
    public static final DeferredItem<BlockItem> CATCH_NET = block(4, AutarkiaBlocks.CATCH_NET);
    public static final DeferredItem<BlockItem> DEPOT = block(4, AutarkiaBlocks.DEPOT);
    public static final DeferredItem<dev.autarkia.colony.CoreUpgradeItem> CORE_UPGRADE_T3 = item(3, "core_upgrade_t3",
        p -> new dev.autarkia.colony.CoreUpgradeItem(p, 3), p -> p.stacksTo(16));
    public static final DeferredItem<Item> SURVEY_LENS = item(3, "survey_lens", Item::new, p -> p.stacksTo(1));
    public static final DeferredItem<Item> DRONE_FRAME = simple(3, "drone_frame");
    public static final DeferredItem<dev.autarkia.worker.WorkerUpgradeItem> WORKER_UPGRADE_SPEED = upgrade("worker_upgrade_speed",
        dev.autarkia.worker.WorkerUpgrades.Kind.SPEED);
    public static final DeferredItem<dev.autarkia.worker.WorkerUpgradeItem> WORKER_UPGRADE_CARGO = upgrade("worker_upgrade_cargo",
        dev.autarkia.worker.WorkerUpgrades.Kind.CARGO);
    public static final DeferredItem<dev.autarkia.worker.WorkerUpgradeItem> WORKER_UPGRADE_ARMOR = upgrade("worker_upgrade_armor",
        dev.autarkia.worker.WorkerUpgrades.Kind.ARMOR);
    public static final DeferredItem<dev.autarkia.worker.WorkerUpgradeItem> WORKER_UPGRADE_RANGE = upgrade("worker_upgrade_range",
        dev.autarkia.worker.WorkerUpgrades.Kind.RANGE);

    public static final DeferredItem<Item> RAW_VANTITE = simple(4, "raw_vantite");
    public static final DeferredItem<Item> VANTITE_INGOT = simple(4, "vantite_ingot");
    public static final DeferredItem<Item> VANTITE_STEEL_INGOT = simple(4, "vantite_steel_ingot");
    public static final DeferredItem<Item> CRUSHED_VANTITE = simple(4, "crushed_vantite");
    public static final DeferredItem<Item> WASHED_VANTITE = simple(4, "washed_vantite");
    public static final DeferredItem<Item> PURE_VANTITE = simple(4, "pure_vantite");
    public static final DeferredItem<Item> IRON_CONCENTRATE = simple(4, "iron_concentrate");
    public static final DeferredItem<Item> COPPER_CONCENTRATE = simple(4, "copper_concentrate");
    public static final DeferredItem<Item> GOLD_CONCENTRATE = simple(4, "gold_concentrate");
    public static final DeferredItem<Item> TIN_CONCENTRATE = simple(4, "tin_concentrate");
    public static final DeferredItem<Item> ZINC_CONCENTRATE = simple(4, "zinc_concentrate");
    public static final DeferredItem<Item> VANTITE_CONCENTRATE = simple(4, "vantite_concentrate");
    public static final DeferredItem<Item> ADVANCED_CIRCUIT = simple(4, "advanced_circuit");
    public static final DeferredItem<dev.autarkia.colony.OutpostSeedItem> OUTPOST_SEED = item(4, "outpost_seed", dev.autarkia.colony.OutpostSeedItem::new,
        p -> p.stacksTo(16));
    public static final DeferredItem<dev.autarkia.colony.CoreUpgradeItem> CORE_UPGRADE_T4 = item(4, "core_upgrade_t4",
        p -> new dev.autarkia.colony.CoreUpgradeItem(p, 4), p -> p.stacksTo(16));
    public static final DeferredItem<BlockItem> VANTITE_ORE = block(4, AutarkiaBlocks.VANTITE_ORE);
    public static final DeferredItem<BlockItem> RAW_VANTITE_BLOCK = block(4, AutarkiaBlocks.RAW_VANTITE_BLOCK);
    public static final DeferredItem<BlockItem> VANTITE_BLOCK = block(4, AutarkiaBlocks.VANTITE_BLOCK);
    public static final DeferredItem<BlockItem> VANTITE_STEEL_BLOCK = block(4, AutarkiaBlocks.VANTITE_STEEL_BLOCK);
    public static final DeferredItem<BlockItem> ORE_OUTCROP = block(4, AutarkiaBlocks.ORE_OUTCROP);
    public static final DeferredItem<BlockItem> MAGMA_VENT = block(4, AutarkiaBlocks.MAGMA_VENT);
    public static final DeferredItem<BlockItem> FROTH_FLOTATION_PLANT = block(4, AutarkiaBlocks.FROTH_FLOTATION_PLANT);
    public static final DeferredItem<BlockItem> HV_PYLON = block(4, AutarkiaBlocks.HV_PYLON);
    public static final DeferredItem<CableItem> HV_CABLE_SPOOL = item(4, "hv_cable_spool", p -> new CableItem(p, CableTier.HV));
    public static final DeferredItem<BlockItem> HYDRO_TURBINE = block(4, AutarkiaBlocks.HYDRO_TURBINE);
    public static final DeferredItem<BlockItem> WIND_TOWER = block(4, AutarkiaBlocks.WIND_TOWER);
    public static final DeferredItem<BlockItem> MAGMA_TAP = block(4, AutarkiaBlocks.MAGMA_TAP);
    public static final DeferredItem<dev.autarkia.equipment.VantiteExcavatorItem> VANTITE_EXCAVATOR = item(4, "vantite_excavator",
        dev.autarkia.equipment.VantiteExcavatorItem::new, p -> fe(p.shovel(dev.autarkia.equipment.AutarkiaMaterials.VANTITE, 1.5F, -3.0F))
            .component(net.minecraft.core.component.DataComponents.TOOL, excavatorTool())
            .component(AutarkiaComponents.TOOL_MODE.get(), dev.autarkia.equipment.ToolMode.SINGLE));
    public static final DeferredItem<dev.autarkia.equipment.GrappleLauncherItem> GRAPPLE_LAUNCHER = item(4, "grapple_launcher",
        dev.autarkia.equipment.GrappleLauncherItem::new, p -> fe(p).stacksTo(1));
    public static final DeferredItem<dev.autarkia.equipment.DashBladeItem> VANTITE_BLADE = item(4, "vantite_blade", dev.autarkia.equipment.DashBladeItem::new,
        p -> p.sword(dev.autarkia.equipment.AutarkiaMaterials.VANTITE, 3.0F, -2.4F));
    public static final DeferredItem<dev.autarkia.equipment.FrontierSuitItem> FRONTIER_SUIT_HELMET = frontier("frontier_suit_helmet",
        net.minecraft.world.item.equipment.ArmorType.HELMET);
    public static final DeferredItem<dev.autarkia.equipment.FrontierSuitItem> FRONTIER_SUIT_CHEST = frontier("frontier_suit_chest",
        net.minecraft.world.item.equipment.ArmorType.CHESTPLATE);
    public static final DeferredItem<dev.autarkia.equipment.FrontierSuitItem> FRONTIER_SUIT_LEGS = frontier("frontier_suit_legs",
        net.minecraft.world.item.equipment.ArmorType.LEGGINGS);
    public static final DeferredItem<dev.autarkia.equipment.FrontierSuitItem> FRONTIER_SUIT_BOOTS = frontier("frontier_suit_boots",
        net.minecraft.world.item.equipment.ArmorType.BOOTS);
    public static final DeferredItem<dev.autarkia.equipment.RailRifleItem> RAIL_RIFLE = item(4, "rail_rifle", dev.autarkia.equipment.RailRifleItem::new,
        p -> fe(p).stacksTo(1));

    private static DeferredItem<dev.autarkia.worker.WorkerUpgradeItem> upgrade(String id, dev.autarkia.worker.WorkerUpgrades.Kind kind) {
        return item(3, id, p -> new dev.autarkia.worker.WorkerUpgradeItem(p, kind), p -> p.stacksTo(16));
    }

    /** Return the FE items of 07 section 1.4 with their capacity and charge rate (for the item energy capability). */
    public static java.util.List<DeferredItem<? extends Item>> feItems() {
        return java.util.List.of(ELECTRIC_DRILL, ELECTRIC_CHAINSAW, SHOCK_BATON, ARC_RIFLE, EXO_RIG_HELMET, EXO_RIG_CHEST, EXO_RIG_LEGS, EXO_RIG_BOOTS,
            VANTITE_EXCAVATOR, GRAPPLE_LAUNCHER, FRONTIER_SUIT_HELMET, FRONTIER_SUIT_CHEST, FRONTIER_SUIT_LEGS, FRONTIER_SUIT_BOOTS, RAIL_RIFLE);
    }

    private static Item.Properties fe(Item.Properties p) {
        return p.component(net.minecraft.core.component.DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE).component(AutarkiaComponents.ENERGY.get(), 0);
    }

    private static net.minecraft.world.item.component.Tool drillTool() {
        var blocks = net.minecraft.core.registries.BuiltInRegistries.acquireBootstrapRegistrationLookup(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
        return new net.minecraft.world.item.component.Tool(java.util.List.of(
            net.minecraft.world.item.component.Tool.Rule.deniesDrops(blocks.getOrThrow(net.minecraft.tags.BlockTags.INCORRECT_FOR_DIAMOND_TOOL)),
            net.minecraft.world.item.component.Tool.Rule.minesAndDrops(blocks.getOrThrow(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE), 12.0F),
            net.minecraft.world.item.component.Tool.Rule.minesAndDrops(blocks.getOrThrow(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL), 12.0F)), 1.0F, 1, true);
    }

    private static net.minecraft.world.item.component.Tool excavatorTool() {
        var blocks = net.minecraft.core.registries.BuiltInRegistries.acquireBootstrapRegistrationLookup(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
        return new net.minecraft.world.item.component.Tool(java.util.List.of(
            net.minecraft.world.item.component.Tool.Rule.deniesDrops(blocks.getOrThrow(net.minecraft.tags.BlockTags.INCORRECT_FOR_NETHERITE_TOOL)),
            net.minecraft.world.item.component.Tool.Rule.minesAndDrops(blocks.getOrThrow(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE), 16.0F),
            net.minecraft.world.item.component.Tool.Rule.minesAndDrops(blocks.getOrThrow(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL), 16.0F)), 1.0F, 1, true);
    }

    private static DeferredItem<dev.autarkia.equipment.FrontierSuitItem> frontier(String id, net.minecraft.world.item.equipment.ArmorType type) {
        return item(4, id, p -> new dev.autarkia.equipment.FrontierSuitItem(p, type), p -> p.humanoidArmor(dev.autarkia.equipment.AutarkiaMaterials.FRONTIER, type)
            .component(AutarkiaComponents.ENERGY.get(), 0).component(AutarkiaComponents.SUIT_ENABLED.get(), true));
    }

    private static DeferredItem<dev.autarkia.equipment.SuitItem> exoRig(String id, net.minecraft.world.item.equipment.ArmorType type) {
        return item(3, id, p -> new dev.autarkia.equipment.SuitItem(p, type), p -> p.humanoidArmor(dev.autarkia.equipment.AutarkiaMaterials.EXO_RIG, type)
            .component(AutarkiaComponents.ENERGY.get(), 0).component(AutarkiaComponents.SUIT_ENABLED.get(), true).repairable(STEEL_PLATE.get()));
    }

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

package dev.autarkia.worker;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.ColonyJob;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * A worker type (06 section 1): tier, rig, body stats, spring energy model with drains and action costs, carry
 * slots, accepted jobs, shown optional bones, repair item and cap cost. One {@link EntityType} has one type.
 *
 * @param capacity energy capacity in tension T (spring) or FE
 * @param idleDrain T per second while awake and idle
 * @param moveDrain T per second while moving
 * @param carryDrain extra T per second while carrying
 * @param placeCost T per placed block
 * @param reach blocks from the target a step works at
 */
public record WorkerType(Identifier id, int tier, String rig, double health, double armor, double speed, int capacity, int idleDrain, int moveDrain,
                         int carryDrain, int placeCost, int cargoStacks, Set<ColonyJob.Type> jobs, List<String> shownBones, Supplier<Item> repairItem,
                         float repairHealth, int capCost, double reach, List<Integer> subCaps) {
    private static final Map<EntityType<?>, WorkerType> BY_TYPE = new HashMap<>();
    public static final List<String> OPTIONAL_BONES = List.of("carry_rack", "spring_key", "battery_pack", "tool_belt", "climb_spikes", "cable_reel",
        "leg_aux_l", "leg_aux_r", "drill", "pod_shell_top", "pod_shell_l", "pod_shell_r", "lamp_mast", "claw_l", "claw_r", "cargo_bed", "debris_scoop",
        "fork_mast", "fork_carriage", "fork_tines", "pallet_slot", "upg_armor", "upg_cargo", "upg_range", "upg_speed", "cargo_claw", "weld_arm", "weld_tip",
        "nozzle", "scanner_dish", "scanner_beam", "spool", "winch_hook", "heavy_plates", "arc_gun", "arc_coil");
    public static final List<String> UPGRADE_BONES = List.of("upg_speed", "upg_cargo", "upg_armor", "upg_range");

    public static final WorkerType PORTER = new WorkerType(Autarkia.id("porter"), 1, "walker", 20, 2, 0.25, 6000, 1, 20, 10, 40, 1,
        Set.of(ColonyJob.Type.FETCH, ColonyJob.Type.CARRY, ColonyJob.Type.DELIVER, ColonyJob.Type.LEVEL_CUT, ColonyJob.Type.LEVEL_FILL, ColonyJob.Type.PLACE,
            ColonyJob.Type.STRING_CABLE, ColonyJob.Type.CRANK, ColonyJob.Type.SIGN, ColonyJob.Type.FOUND_OUTPOST),
        List.of("carry_rack", "spring_key"), () -> dev.autarkia.registry.AutarkiaItems.BRONZE_INGOT.get(), 4, 1, 3.5, List.of(4, 6, 6, 6, 6, 6));
    public static final WorkerType HAULER_CRAB = new WorkerType(Autarkia.id("hauler_crab"), 2, "crab", 30, 4, 0.22, 12000, 1, 24, 12, 40, 4,
        Set.of(ColonyJob.Type.FETCH, ColonyJob.Type.CARRY, ColonyJob.Type.DELIVER, ColonyJob.Type.COLLECT_DEBRIS, ColonyJob.Type.FOUND_OUTPOST),
        List.of("claw_l", "claw_r", "cargo_bed", "debris_scoop", "spring_key"), () -> dev.autarkia.registry.AutarkiaItems.BRASS_PLATE.get(), 6, 1, 2.5,
        List.of(0, 4, 6, 6, 8, 8));

    public static final WorkerType BUILDER_DRONE = new WorkerType(Autarkia.id("builder_drone"), 3, "drone", 16, 2, 0.5, 80000, 100, 400, 0, 250, 2,
        Set.of(ColonyJob.Type.LEVEL_CUT, ColonyJob.Type.LEVEL_FILL, ColonyJob.Type.PLACE, ColonyJob.Type.STRING_CABLE, ColonyJob.Type.SIGN,
            ColonyJob.Type.DELIVER),
        List.of("cargo_claw", "weld_arm", "weld_tip", "nozzle", "battery_pack"), () -> dev.autarkia.registry.AutarkiaItems.STEEL_PLATE.get(), 6, 1, 4.0,
        List.of(0, 0, 4, 6, 8, 10));
    public static final WorkerType LOADER = new WorkerType(Autarkia.id("loader"), 3, "crab", 40, 8, 0.20, 120000, 20, 300, 100, 0, 27,
        Set.of(ColonyJob.Type.FETCH, ColonyJob.Type.CARRY, ColonyJob.Type.DELIVER, ColonyJob.Type.WAREHOUSE_DELIVER, ColonyJob.Type.WAREHOUSE_STORE),
        List.of("battery_pack", "fork_mast", "fork_carriage", "fork_tines", "pallet_slot"), () -> dev.autarkia.registry.AutarkiaItems.STEEL_PLATE.get(), 6, 1,
        2.5, List.of(0, 0, 2, 3, 4, 4));
    public static final WorkerType LINEMAN = new WorkerType(Autarkia.id("lineman"), 3, "walker", 28, 4, 0.25, 60000, 20, 200, 0, 500, 3,
        Set.of(ColonyJob.Type.PLACE, ColonyJob.Type.STRING_CABLE, ColonyJob.Type.CUT_SPAN),
        List.of("battery_pack", "tool_belt", "climb_spikes", "cable_reel"), () -> dev.autarkia.registry.AutarkiaItems.STEEL_PLATE.get(), 6, 1, 3.0,
        List.of(0, 0, 2, 3, 4, 4));

    public static final WorkerType SURVEYOR = new WorkerType(Autarkia.id("surveyor"), 4, "drone", 14, 2, 0.70, 60000, 100, 300, 0, 0, 1,
        Set.of(ColonyJob.Type.SURVEY), List.of("scanner_dish", "scanner_beam", "battery_pack"), () -> dev.autarkia.registry.AutarkiaItems.STEEL_PLATE.get(), 6,
        1, 4.0, List.of(0, 0, 0, 2, 2, 3));
    public static final WorkerType CABLE_LAYER = new WorkerType(Autarkia.id("cable_layer_drone"), 4, "drone", 30, 6, 0.45, 200000, 200, 600, 0, 5000, 3,
        Set.of(ColonyJob.Type.PLACE, ColonyJob.Type.STRING_CABLE), List.of("cargo_claw", "spool", "winch_hook", "heavy_plates", "battery_pack"),
        () -> dev.autarkia.registry.AutarkiaItems.STEEL_PLATE.get(), 6, 1, 4.0, List.of(0, 0, 0, 2, 3, 4));
    public static final WorkerType SENTINEL = new WorkerType(Autarkia.id("sentinel"), 4, "drone", 40, 10, 0.60, 150000, 100, 400, 0, 0, 0,
        Set.of(ColonyJob.Type.ESCORT), List.of("arc_gun", "arc_coil", "heavy_plates", "battery_pack"), () -> dev.autarkia.registry.AutarkiaItems.STEEL_PLATE.get(),
        6, 1, 4.0, List.of(0, 0, 0, 4, 6, 8));

    /** Debris collect cost in T per collected item (06 section 1.2). */
    public static final int DEBRIS_COST = 30;
    /** Surveyor scan cost in FE per scan point (06 section 1.2). */
    public static final int SCAN_COST = 2000;
    /** Sentinel arc shot cost in FE (06 section 1.2). */
    public static final int ARC_SHOT_COST = 1500;

    /** Return true for FE workers (T3 and up: battery, recharge dock, energy capability); spring workers use tension T. */
    public boolean fe() {
        return tier >= 3;
    }

    /** Return true for flying workers (drone rig). */
    public boolean flying() {
        return "drone".equals(rig);
    }

    /**
     * Return the highest cable tier the worker strings (05 section 7.2: Porter rope only, T3 workers up to insulated, the
     * Cable-Layer HV).
     */
    public dev.autarkia.energy.CableTier maxCable() {
        if (this == CABLE_LAYER) {
            return dev.autarkia.energy.CableTier.HV;
        }
        return tier >= 3 ? dev.autarkia.energy.CableTier.INSULATED : dev.autarkia.energy.CableTier.ROPE;
    }

    /** Return the energy per strung span (06 section 1.2: 1,000 FE for T3 workers, 2,000 for the Cable-Layer, nothing for spring workers). */
    public int stringCost() {
        if (this == CABLE_LAYER) {
            return 2000;
        }
        return fe() ? 1000 : 0;
    }

    /** Return the ARGB lamp color of the type palette (06 section 2.5), used for the recharge glow. */
    public int lamp() {
        if (this == SURVEYOR) {
            return 0xFF7CFF6B;
        }
        if (this == CABLE_LAYER) {
            return 0xFFFF9A3C;
        }
        if (this == SENTINEL) {
            return 0xFFFF3B3B;
        }
        return this == BUILDER_DRONE ? 0xFF4FE3FF : this == LOADER ? 0xFFF2F2F2 : this == LINEMAN ? 0xFFFFE066 : 0xFFFFC040;
    }

    /** Return the render scale of the rig for this type (06 section 1.1: Loader 1.2, Surveyor 0.8, Cable-Layer 1.3, Sentinel 1.1). */
    public float scale() {
        if (this == SURVEYOR) {
            return 0.8f;
        }
        if (this == CABLE_LAYER) {
            return 1.3f;
        }
        if (this == SENTINEL) {
            return 1.1f;
        }
        return this == LOADER ? 1.2f : 1.0f;
    }

    /** Return the sub-cap of this type at a Core tier (06 section 6.1), scaled by the cap multiplier (min 1; 0 when locked). */
    public int subCap(int coreTier) {
        int base = subCaps.get(Math.max(1, Math.min(6, coreTier)) - 1);
        return base == 0 ? 0 : Math.max(1, (int) (base * dev.autarkia.config.AutarkiaConfig.WORKER_CAP_MULTIPLIER.get()));
    }

    private static final Map<String, WorkerType> BY_PATH = new HashMap<>();

    /** Bind a type to its entity type (called when the entity types register). */
    public static void bind(EntityType<?> type, WorkerType worker) {
        BY_TYPE.put(type, worker);
        BY_PATH.put(worker.id().getPath(), worker);
    }

    /** Return the worker type by its id path (renderers). */
    public static WorkerType byPath(String path) {
        WorkerType w = BY_PATH.get(path);
        if (w == null) {
            throw new IllegalStateException("No worker type '" + path + "'");
        }
        return w;
    }

    /** Return the worker type of an entity type. */
    public static WorkerType of(EntityType<?> type) {
        WorkerType w = BY_TYPE.get(type);
        if (w == null) {
            throw new IllegalStateException("Entity type " + type + " has no worker type");
        }
        return w;
    }

    /** Return the T drained per block walked (move drain at about 2.5 blocks per second). */
    public double drainPerBlock() {
        return moveDrain / 2.5;
    }
}

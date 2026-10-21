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
        "leg_aux_l", "leg_aux_r", "drill", "pod_shell_top", "pod_shell_l", "pod_shell_r", "lamp_mast", "upg_armor", "upg_cargo", "upg_range", "upg_speed");

    public static final WorkerType PORTER = new WorkerType(Autarkia.id("porter"), 1, "walker", 20, 2, 0.25, 6000, 1, 20, 10, 40, 1,
        Set.of(ColonyJob.Type.FETCH, ColonyJob.Type.CARRY, ColonyJob.Type.DELIVER, ColonyJob.Type.LEVEL_CUT, ColonyJob.Type.LEVEL_FILL, ColonyJob.Type.PLACE,
            ColonyJob.Type.STRING_CABLE, ColonyJob.Type.CRANK, ColonyJob.Type.SIGN),
        List.of("carry_rack", "spring_key"), () -> dev.autarkia.registry.AutarkiaItems.BRONZE_INGOT.get(), 4, 1, 3.5, List.of(4, 6, 6, 6, 6, 6));

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

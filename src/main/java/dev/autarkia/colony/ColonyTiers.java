package dev.autarkia.colony;

/** Tier parameters of the colony Core (05 section 1.2 and 06 section 6.1), indexed by tier 1 to 6. */
public final class ColonyTiers {
    private static final int[] DOMAIN_HALF = {16, 24, 32, 48, 64, 96};
    private static final int[] WORKER_CAP = {4, 8, 14, 20, 26, 32};
    private static final int[] MAX_ACTIVE = {1, 2, 3, 4, 6, 8};
    private static final int[] BUDGET = {64, 160, 400, 1000, 2500, 6000};
    private static final int[] LEVELING_EDITS = {64, 128, 256, 512, 1024, 2048};
    private static final int[] COLUMN_DIFF = {2, 3, 4, 5, 6, 8};
    private static final int[] PATTERN_SLOTS = {0, 4, 12, 24, 48, 128};
    private static final int[] PATTERN_MAX = {0, 16, 24, 32, 40, 48};
    private static final int[] MAX_BEACONS = {0, 0, 2, 3, 4, 6};
    private static final int[] BEACON_HALF = {0, 0, 16, 16, 24, 24};

    /** Return the maximum number of domain beacons. */
    public static int maxBeacons(int tier) {
        return at(MAX_BEACONS, tier);
    }

    /** Return the half-size of the square a domain beacon adds. */
    public static int beaconHalf(int tier) {
        return at(BEACON_HALF, tier);
    }

    /** Return the Pattern library size. */
    public static int patternSlots(int tier) {
        return at(PATTERN_SLOTS, tier);
    }

    /** Return the maximum Pattern capture size per axis (0 = no capture). */
    public static int patternMax(int tier) {
        return at(PATTERN_MAX, tier);
    }

    private ColonyTiers() {
    }

    private static int at(int[] table, int tier) {
        return table[Math.max(1, Math.min(6, tier)) - 1];
    }

    /** Return the domain half-size in blocks. */
    public static int domainHalf(int tier) {
        return at(DOMAIN_HALF, tier);
    }

    /** Return the colony worker cap (sum of cap costs), scaled by the worker cap multiplier (rounded down, min 1). */
    public static int workerCap(int tier) {
        return Math.max(1, (int) (at(WORKER_CAP, tier) * dev.autarkia.config.AutarkiaConfig.WORKER_CAP_MULTIPLIER.get()));
    }

    /** Return the maximum number of active (approved, unfinished) projects. */
    public static int maxActiveProjects(int tier) {
        return at(MAX_ACTIVE, tier);
    }

    /** Return the daily budget D in points. */
    public static int dailyBudget(int tier) {
        return at(BUDGET, tier);
    }

    /** Return the maximum leveling edits per project. */
    public static int maxLevelingEdits(int tier) {
        return at(LEVELING_EDITS, tier);
    }

    /** Return the maximum column height difference for a site. */
    public static int maxColumnDiff(int tier) {
        return at(COLUMN_DIFF, tier);
    }
}

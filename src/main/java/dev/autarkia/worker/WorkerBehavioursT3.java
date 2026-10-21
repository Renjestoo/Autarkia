package dev.autarkia.worker;

import dev.autarkia.colony.ColonyJob;
import dev.autarkia.energy.PoleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.Vec3;

/** The T3 worker behaviours of 06 section 3.6: the Lineman's climb, string, cut and weld. */
public final class WorkerBehavioursT3 {
    public static final double CLIMB_SPEED = 0.1;
    public static final int CLIMB_COST_PER_TICK = 30;

    private WorkerBehavioursT3() {
    }

    /** Return how high a worker climbs to work at a node: the top block of a pole, 0 for other nodes. */
    public static int climbHeight(WorkerEntity w, BlockPos node) {
        return w.level().getBlockState(node).getBlock() instanceof PoleBlock pole ? pole.height() - 1 : 0;
    }

    /**
     * Lineman STRING_CABLE and CUT_SPAN (06 sections 3.6 and 3.7): at each node base he moves next to the column, climbs
     * it at 0.1 blocks per tick without gravity (600 FE per second, action climb), strings or cuts at the top (string
     * trigger), climbs down; for a new span he then walks to the second node, climbs it and welds there.
     */
    public static final class ClimbAndString extends WorkerBehaviours.Step {
        private int stage;
        private double groundY;
        private WorkerActions.@org.jspecify.annotations.Nullable Result failed;

        @Override
        boolean active(WorkerEntity w, ColonyJob job) {
            return WorkerBehaviours.phase(w) == WorkerMemories.Phase.WORK && w.workerType == WorkerType.LINEMAN
                && (job.type == ColonyJob.Type.STRING_CABLE || job.type == ColonyJob.Type.CUT_SPAN);
        }

        @Override
        protected void start(WorkerEntity w) {
            stage = 0;
            groundY = w.getY();
        }

        @Override
        protected void tick(WorkerEntity w) {
            ColonyJob job = w.currentJob();
            if (job == null || job.nodeB == null) {
                return;
            }
            boolean cut = job.type == ColonyJob.Type.CUT_SPAN;
            BlockPos node = stage < 4 ? job.target : job.nodeB;
            switch (stage) {
                case 0, 4 -> {
                    if (approach(w, node)) {
                        groundY = w.getY();
                        stage++;
                    }
                }
                case 1, 5 -> {
                    if (climb(w, node.getY() + climbHeight(w, node))) {
                        stage++;
                    }
                }
                case 2 -> {
                    WorkerActions.Result r = cut ? WorkerActions.cutSpan(w, job) : WorkerActions.string(w, job);
                    failed = r == WorkerActions.Result.DONE ? null : r;
                    stage = failed == null ? 3 : 8;
                }
                case 8 -> {
                    if (climb(w, groundY)) {
                        land(w);
                        WorkerBehaviours.finish(w, failed);
                    }
                }
                case 3 -> {
                    if (climb(w, groundY)) {
                        land(w);
                        if (cut) {
                            w.jobDone();
                        } else {
                            stage = 4;
                        }
                    }
                }
                case 6 -> {
                    w.trigger("weld");
                    w.sound("entity.worker.weld", 0.6f, 1.6f);
                    stage = 7;
                }
                default -> {
                    if (climb(w, groundY)) {
                        land(w);
                        w.jobDone();
                    }
                }
            }
        }

        @Override
        protected void stop(WorkerEntity w) {
            land(w);
        }
    }

    /**
     * Loader WAREHOUSE_DELIVER and WAREHOUSE_STORE (04 section 2.5, 06 LiftPallet): at each trip crate it lifts the crate
     * onto the fork (40 ticks, {@code lift}, 1,500 FE), drives to the terminal, lowers and hands over (20 ticks,
     * {@code lower}; a full output buffer is retried for 600 ticks, then the request fails "output full"), drives each
     * crate back and lowers it into its position. A crate stays in a cargo slot while on the fork.
     */
    public static final class WarehouseTrip extends WorkerBehaviours.Step {
        private static final int LIFT_COST = 1500;
        private static final int WAIT_OUTPUT = 600;
        private final java.util.List<LedgerRequestStop> stops = new java.util.ArrayList<>();
        private dev.autarkia.storage.@org.jspecify.annotations.Nullable LedgerRequest request;
        private dev.autarkia.storage.@org.jspecify.annotations.Nullable WarehouseControllerBlockEntity controller;
        private int stage;
        private int index;
        private int timer;
        private long waitSince;

        /** A trip stop and the cargo slot its crate sits in (-1 before the lift). */
        private record LedgerRequestStop(dev.autarkia.storage.LedgerRequest.Reservation at, int[] slot) {
        }

        @Override
        boolean active(WorkerEntity w, ColonyJob job) {
            return WorkerBehaviours.phase(w) == WorkerMemories.Phase.WORK
                && (job.type == ColonyJob.Type.WAREHOUSE_DELIVER || job.type == ColonyJob.Type.WAREHOUSE_STORE);
        }

        @Override
        protected void start(WorkerEntity w) {
            stops.clear();
            stage = 0;
            index = 0;
            timer = 0;
            waitSince = -1;
            ColonyJob job = w.currentJob();
            net.minecraft.server.level.ServerLevel sl = (net.minecraft.server.level.ServerLevel) w.level();
            controller = job == null || job.nodeB == null ? null : dev.autarkia.storage.Warehouses.ownerOf(sl, job.nodeB);
            request = controller == null || job.requestId == null ? null : controller.request(job.requestId);
            if (request == null || !request.open()) {
                stage = -1;
                return;
            }
            for (dev.autarkia.storage.LedgerRequest.Reservation r : controller.beginTrip(request, w)) {
                stops.add(new LedgerRequestStop(r, new int[] {-1}));
            }
        }

        @Override
        protected void tick(WorkerEntity w) {
            ColonyJob job = w.currentJob();
            if (job == null) {
                return;
            }
            if (stage == -1 || controller == null || controller.isRemoved() || request == null) {
                w.jobFailed(ColonyJob.Failure.TARGET_OBSTRUCTED);
                return;
            }
            switch (stage) {
                case 0 -> fetch(w);
                case 1 -> {
                    if (drive(w, job.nodeB)) {
                        stage = 2;
                        timer = 0;
                    }
                }
                case 2 -> handOver(w);
                case 3 -> putBack(w);
                default -> {
                    controller.tripFinished(request);
                    w.jobDone();
                }
            }
        }

        private void fetch(WorkerEntity w) {
            if (index >= stops.size()) {
                stage = stops.stream().anyMatch(s -> s.slot()[0] >= 0) ? 1 : 4;
                index = 0;
                return;
            }
            LedgerRequestStop s = stops.get(index);
            BlockPos at = s.at().index() < 0 ? request.terminal : s.at().rack();
            if (!drive(w, at)) {
                return;
            }
            if (timer == 0) {
                w.trigger("lift");
                w.sound("loader_lift", 0.7f, 0.6f);
                w.spend(LIFT_COST);
            }
            if (++timer < dev.autarkia.storage.WarehouseControllerBlockEntity.LIFT_TICKS) {
                return;
            }
            timer = 0;
            net.minecraft.world.item.ItemStack crate = controller.lift(request, s.at());
            int slot = -1;
            for (int i = 0; slot < 0 && i < w.cargo.size(); i++) {
                slot = w.cargo.get(i).isEmpty() ? i : -1;
            }
            if (!crate.isEmpty() && slot >= 0) {
                w.cargo.set(slot, crate);
                w.syncCargo();
                s.slot()[0] = slot;
            } else if (!crate.isEmpty()) {
                controller.returnCrate(request, s.at(), crate);
            }
            index++;
        }

        private void handOver(WorkerEntity w) {
            if (timer == 0) {
                w.trigger("lower");
                w.sound("loader_lift", 0.6f, 0.8f);
            }
            if (++timer < dev.autarkia.storage.WarehouseControllerBlockEntity.HANDOVER_TICKS || timer % 20 != 0) {
                return;
            }
            java.util.List<net.minecraft.world.item.ItemStack> crates = new java.util.ArrayList<>();
            for (LedgerRequestStop s : stops) {
                if (s.slot()[0] >= 0) {
                    crates.add(w.cargo.get(s.slot()[0]));
                }
            }
            boolean done = controller.handOver(request, crates);
            w.syncCargo();
            if (waitSince < 0) {
                waitSince = w.level().getGameTime();
            }
            if (!done && w.level().getGameTime() - waitSince < WAIT_OUTPUT) {
                return;
            }
            if (!done) {
                controller.outputFull(request);
            }
            stage = 3;
            index = 0;
            timer = 0;
        }

        private void putBack(WorkerEntity w) {
            while (index < stops.size() && stops.get(index).slot()[0] < 0) {
                index++;
            }
            if (index >= stops.size()) {
                stage = 4;
                return;
            }
            LedgerRequestStop s = stops.get(index);
            if (!drive(w, controller.returnPos(request, s.at()))) {
                return;
            }
            w.trigger("lower");
            w.sound("loader_lift", 0.6f, 0.8f);
            net.minecraft.world.item.ItemStack crate = w.cargo.get(s.slot()[0]);
            w.cargo.set(s.slot()[0], net.minecraft.world.item.ItemStack.EMPTY);
            w.syncCargo();
            controller.returnCrate(request, s.at(), crate);
            s.slot()[0] = -1;
            index++;
        }

        private static boolean drive(WorkerEntity w, BlockPos target) {
            if (WorkerBehaviours.near(w, target, w.reach())) {
                w.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                w.getLookControl().setLookAt(Vec3.atCenterOf(target));
                return true;
            }
            WorkerBrain.walkTo(w, WorkerBrain.waypoint(w, target), 1);
            return false;
        }
    }

    private static boolean approach(WorkerEntity w, BlockPos node) {
        Direction side = Direction.getApproximateNearest(w.getX() - (node.getX() + 0.5), 0, w.getZ() - (node.getZ() + 0.5));
        BlockPos spot = node.relative(side.getAxis().isHorizontal() ? side : Direction.SOUTH);
        Vec3 c = Vec3.atBottomCenterOf(spot);
        if (Math.hypot(w.getX() - c.x, w.getZ() - c.z) > 0.6) {
            WorkerBrain.walkTo(w, spot, 0);
            return false;
        }
        w.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        w.getNavigation().stop();
        w.getLookControl().setLookAt(Vec3.atCenterOf(node));
        return true;
    }

    private static boolean climb(WorkerEntity w, double targetY) {
        double dy = targetY - w.getY();
        if (Math.abs(dy) <= 1e-3) {
            return true;
        }
        w.setNoGravity(true);
        w.setDeltaMovement(Vec3.ZERO);
        w.setAction(WorkerEntity.ACTION_CLIMB);
        w.setPos(w.getX(), w.getY() + Math.signum(dy) * Math.min(CLIMB_SPEED, Math.abs(dy)), w.getZ());
        w.spend(CLIMB_COST_PER_TICK);
        return false;
    }

    private static void land(WorkerEntity w) {
        if (!w.workerType.flying()) {
            w.setNoGravity(false);
        }
        if (w.action() == WorkerEntity.ACTION_CLIMB) {
            w.setAction(WorkerEntity.ACTION_NONE);
        }
    }
}

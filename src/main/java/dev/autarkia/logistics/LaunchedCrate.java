package dev.autarkia.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaAdvancements;
import dev.autarkia.registry.AutarkiaEntities;
import dev.autarkia.storage.FallingCrateEntity;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/**
 * Flight of a {@code falling_crate} in mode {@code LAUNCHED} (04 section 1.6). Each tick the crate accelerates by gravity
 * and the FrozenLib wind at its position ({@link LaunchArc}), deals 4 damage to a living entity it meets (once each) and
 * loses half its horizontal speed. When it crosses the catch plane within {@code launcherCatchRadius} (x and z) of the
 * target net's centre, the net takes it ({@link CatchNetBlock#tryCatch}); a net that refuses lets it bounce off and land
 * beside the net (a miss, reason "Net output full"). Any other landing is a miss: the crate becomes a crate block
 * (04 section 2.3.5). After 600 ticks in the air it lands where it is. The launcher hears every result.
 */
public final class LaunchedCrate {
    /** The flight record a launched crate carries; {@code hit} holds the entities it already struck (not saved). */
    public static final class Flight {
        public static final Codec<Flight> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("launcher").forGetter(f -> f.launcher), BlockPos.CODEC.fieldOf("net").forGetter(f -> f.net),
            Codec.DOUBLE.fieldOf("distance").forGetter(f -> f.distance), Codec.BOOL.optionalFieldOf("refused", false).forGetter(f -> f.refused),
            Codec.INT.optionalFieldOf("ticks", 0).forGetter(f -> f.ticks)).apply(i, Flight::new));
        public final BlockPos launcher;
        public final BlockPos net;
        public final double distance;
        private boolean refused;
        private int ticks;
        private final IntOpenHashSet hit = new IntOpenHashSet();

        /** Create a flight from a launcher to the centre of a catch net over a horizontal distance in blocks. */
        public Flight(BlockPos launcher, BlockPos net, double distance, boolean refused, int ticks) {
            this.launcher = launcher.immutable();
            this.net = net.immutable();
            this.distance = distance;
            this.refused = refused;
            this.ticks = ticks;
        }

        /** Return true after the net refused the crate. */
        public boolean refused() {
            return refused;
        }

        /** Return the ticks in the air. */
        public int ticks() {
            return ticks;
        }
    }

    private LaunchedCrate() {
    }

    /** Return the number of crates in the air in mode LAUNCHED in the level (cap {@code launcherMaxInFlight}). */
    public static int inFlight(ServerLevel sl) {
        return sl.getEntities(AutarkiaEntities.FALLING_CRATE.get(), e -> e.launched()).size();
    }

    /** Advance one tick of the flight; returns true when the crate is gone (caught or landed). */
    public static boolean tick(FallingCrateEntity e, ServerLevel sl, Flight f) {
        f.ticks++;
        Vec3 accel = LaunchArc.acceleration(LaunchArc.wind(sl, e.position()), AutarkiaConfig.GONDOLA_WIND_FACTOR.get());
        Vec3 v = e.getDeltaMovement();
        Vec3 step = LaunchArc.step(v, accel);
        double planeY = f.net.getY() + CatchNetBlock.PLANE;
        Vec3 p = e.position();
        Vec3 next = p.add(step);
        if (!f.refused && step.y < 0 && p.y > planeY && next.y <= planeY) {
            Vec3 c = p.add(step.scale((p.y - planeY) / (p.y - next.y)));
            double r = AutarkiaConfig.LAUNCHER_CATCH_RADIUS.get();
            if (Math.abs(c.x - (f.net.getX() + 0.5)) <= r && Math.abs(c.z - (f.net.getZ() + 0.5)) <= r) {
                if (sl.isLoaded(f.net) && sl.getBlockState(f.net).getBlock() instanceof CatchNetBlock && CatchNetBlock.tryCatch(sl, f.net, e.crate())) {
                    report(sl, f, true, c);
                    e.discard();
                    return true;
                }
                f.refused = true;
                Vec3 out = new Vec3(c.x - (f.net.getX() + 0.5), 0, c.z - (f.net.getZ() + 0.5));
                out = out.lengthSqr() < 0.01 ? new Vec3(v.x, 0, v.z).normalize() : out.normalize();
                e.setPos(c.x, planeY + 0.05, c.z);
                e.setDeltaMovement(out.x * 0.18, 0.3, out.z * 0.18);
                CatchNetBlock.bounce(sl, f.net);
                Autarkia.LOG.info("[launcher] the net at {} refused a crate", f.net.toShortString());
                return false;
            }
        }
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, e.getBoundingBox().expandTowards(step).inflate(0.1))) {
            if (f.hit.add(le.getId())) {
                le.hurtServer(sl, sl.damageSources().fallingBlock(e), 4.0f);
                v = new Vec3(v.x * 0.5, v.y, v.z * 0.5);
                step = LaunchArc.step(v, accel);
            }
        }
        Vec3 nv = v.add(accel);
        e.move(MoverType.SELF, step);
        if (e.verticalCollision && !e.onGround()) {
            nv = new Vec3(nv.x, 0, nv.z);
        }
        e.setDeltaMovement(nv);
        if (e.getY() < sl.getMinY() - 16) {
            Autarkia.LOG.info("[launcher] a crate fell below the world at {}", e.blockPosition().toShortString());
            report(sl, f, false, e.position());
            e.discard();
            return true;
        }
        if (e.isInLava() || sl.getFluidState(e.blockPosition()).is(FluidTags.LAVA)) {
            FallingCrateEntity.spill(sl, e.blockPosition(), e.crate(), e.position());
            report(sl, f, false, e.position());
            e.discard();
            return true;
        }
        if (e.horizontalCollision || e.onGround() || f.ticks >= 600) {
            BlockPos landed = FallingCrateEntity.land(sl, e.blockPosition(), e.crate(), e.position());
            report(sl, f, false, landed == null ? e.position() : Vec3.atBottomCenterOf(landed));
            e.discard();
            return true;
        }
        return false;
    }

    private static void report(ServerLevel sl, Flight f, boolean hit, Vec3 at) {
        double dx = at.x - (f.net.getX() + 0.5);
        double dz = at.z - (f.net.getZ() + 0.5);
        double miss = hit ? 0 : Math.sqrt(dx * dx + dz * dz);
        CrateLauncherBlockEntity launcher = sl.isLoaded(f.launcher) && sl.getBlockEntity(f.launcher) instanceof CrateLauncherBlockEntity l ? l : null;
        if (launcher != null) {
            launcher.shotResult(hit, miss, f.refused);
        }
        if (hit && f.distance >= 48) {
            Vec3 c = Vec3.atCenterOf(f.net);
            if (launcher != null) {
                launcher.award(sl, c, "logistics/special_delivery");
            } else {
                AutarkiaAdvancements.awardNear(sl, c, "logistics/special_delivery");
            }
        }
        Autarkia.LOG.info("[launcher] shot from {}: {}", f.launcher.toShortString(), hit ? "HIT" : String.format(java.util.Locale.ROOT, "MISS by %.1f", miss));
    }

    /** Return a flight's catch point: the net centre on its catch plane. */
    public static Vec3 target(BlockPos net) {
        return new Vec3(net.getX() + 0.5, net.getY() + CatchNetBlock.PLANE, net.getZ() + 0.5);
    }
}

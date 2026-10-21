package dev.autarkia.energy;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.registry.Ticking;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import org.jspecify.annotations.Nullable;

/**
 * Gyro Heart, 5x3x5 inertia storage with the center at the bottom middle (02 section 6.2, decision G2): 2,000,000,000
 * FE, {@code omega = sqrt(E / Emax)}, I/O {@code floor(1,048,576 x max(0.1, omega))} FE/t, self-loss {@code 1 + 1e-7 E}
 * FE per tick, no rolling release. While E is at least 1% (20,000,000 FE) a survival break is cancelled with "Spin it down
 * first." and no explosion can break it (both in {@link PowerEvents}); a creative break works. Each capability face is
 * NONE, IN, OUT or BOTH (default BOTH, front NONE); the wrench cycles the clicked face. Storage priority group 3.
 * {@code gyro.spin} plays every 40 ticks at pitch {@code 0.4 + omega} while omega is above 0.05.
 */
public class GyroHeartBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, GeoBlockEntity, JadeInfo, Wrenchable {
    public static final int CAPACITY = 2_000_000_000;
    public static final int MAX_IO = 1_048_576;
    public static final int LOCK_FE = CAPACITY / 100;

    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.gyro_heart.spin");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.gyro_heart.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer buffer = new EnergyBuffer(CAPACITY, MAX_IO, MAX_IO, this::setChanged);
    private final byte[] faces = {-1, -1, -1, -1, -1, -1};
    private final EnergyHandler[] handlers = new EnergyHandler[6];
    private double loss;
    private int lastSyncedSpin = -1;
    private int lastIn;
    private int lastOut;
    private int pendingIn;
    private int pendingOut;

    public GyroHeartBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.GYRO_HEART.get(), pos, state);
        for (Direction d : Direction.values()) {
            handlers[d.ordinal()] = new Face(d);
        }
    }

    /** Return the energy buffer (internal access, no face or omega limit). */
    public EnergyBuffer buffer() {
        return buffer;
    }

    /** Return the spin omega, 0 to 1. */
    public double omega() {
        return Math.sqrt(buffer.stored() / (double) CAPACITY);
    }

    /** Return the I/O limit in FE/t: floor(1,048,576 x max(0.1, omega)). */
    public int io() {
        return (int) Math.floor(MAX_IO * Math.max(0.1, omega()));
    }

    /** Return the self-loss of one tick at the current energy, 1 + 1e-7 E. */
    public double selfLoss() {
        return 1 + 1e-7 * buffer.stored();
    }

    /** Return true while E is at least 1% of the capacity (break and explosion lock). */
    public boolean locked() {
        return buffer.stored() >= LOCK_FE;
    }

    /** Return the FE taken from the grid in the last tick. */
    public int lastIn() {
        return lastIn;
    }

    /** Return the FE given to the grid in the last tick. */
    public int lastOut() {
        return lastOut;
    }

    /** Return the mode of a world face (default BOTH, the front NONE). */
    public SuperconductingCoilBlockEntity.FaceMode faceMode(Direction d) {
        byte m = faces[d.ordinal()];
        if (m >= 0) {
            return SuperconductingCoilBlockEntity.FaceMode.values()[m];
        }
        return d == MachineBlock.front(getBlockState()) ? SuperconductingCoilBlockEntity.FaceMode.NONE : SuperconductingCoilBlockEntity.FaceMode.BOTH;
    }

    /** Return the energy handler of a face for the capability: null for a NONE face, the plain buffer for internal access. */
    public @Nullable EnergyHandler face(@Nullable Direction side) {
        if (side == null) {
            return buffer;
        }
        return faceMode(side) == SuperconductingCoilBlockEntity.FaceMode.NONE ? null : handlers[side.ordinal()];
    }

    @Override
    public void serverTick() {
        loss += selfLoss();
        if (loss >= 1) {
            buffer.drain((int) loss);
            loss -= (int) loss;
        }
        lastIn = pendingIn;
        lastOut = pendingOut;
        pendingIn = 0;
        pendingOut = 0;
        double w = omega();
        int spin = (int) Math.round(w * 20);
        if (spin != lastSyncedSpin) {
            lastSyncedSpin = spin;
            syncNow();
        }
        if (w > 0.05 && level.getGameTime() % 40 == 0) {
            level.playSound(null, worldPosition.above(), AutarkiaSounds.of("gyro.spin"), SoundSource.BLOCKS, 1f, (float) (0.4 + w));
        }
    }

    @Override
    public void gridTransferred(int amount) {
        if (amount < 0) {
            pendingIn += -amount;
        } else {
            pendingOut += amount;
        }
    }

    @Override
    public Role gridRole() {
        return Role.STORAGE;
    }

    @Override
    public int gridPriority() {
        return 3;
    }

    @Override
    public EnergyBuffer gridBuffer() {
        return buffer;
    }

    @Override
    public int gridMaxOut() {
        return io();
    }

    @Override
    public int gridMaxIn() {
        return io();
    }

    @Override
    public boolean gridValid() {
        return !isRemoved();
    }

    @Override
    public InteractionResult wrench(ServerPlayer player) {
        StringBuilder b = new StringBuilder();
        for (Direction d : Direction.values()) {
            b.append(d.getName().charAt(0)).append('=').append(faceMode(d).name()).append(' ');
        }
        player.sendOverlayMessage(Component.translatable("message.autarkia.coil.faces", b.toString().trim()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult wrench(ServerPlayer player, BlockPos clicked, Direction face) {
        if (!isCenter() && level.getBlockEntity(getCenter()) instanceof GyroHeartBlockEntity c) {
            return c.wrench(player, clicked, face);
        }
        SuperconductingCoilBlockEntity.FaceMode next = SuperconductingCoilBlockEntity.FaceMode.values()[(faceMode(face).ordinal() + 1)
            % SuperconductingCoilBlockEntity.FaceMode.values().length];
        faces[face.ordinal()] = (byte) next.ordinal();
        setChanged();
        for (BlockPos p : ((BaseMultiblock) getBlockState().getBlock()).getMultiblockShape(level, worldPosition, getBlockState()).getGlobalPositions()) {
            level.invalidateCapabilities(p);
        }
        player.sendOverlayMessage(Component.translatable("message.autarkia.coil.face", Component.translatable("direction.autarkia." + face.getName()),
            Component.translatable(next.key())));
        return InteractionResult.SUCCESS;
    }

    @Override
    public String jadeGroup() {
        return "power";
    }

    @Override
    public void jadeLines(List<Component> out) {
        out.add(Component.translatable("jade.autarkia.stored", buffer.stored(), CAPACITY, buffer.stored() * 100L / CAPACITY));
        out.add(Component.translatable("jade.autarkia.io", lastIn, lastOut));
        out.add(Component.translatable("jade.autarkia.spin", Math.round(omega() * 100)));
        out.add(Component.translatable("jade.autarkia.gyro.energy", buffer.stored()));
    }

    @Override
    public float jadeProgress() {
        return buffer.fillRatio();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GyroHeartBlockEntity>("rotor", 4, test -> {
            double w = test.animatable().omega();
            test.setControllerSpeed((float) Math.max(0.05, w));
            return test.setAndContinue(w > 0.02 ? SPIN : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        buffer.serialize(out.child("energy"));
        out.putDouble("loss", loss);
        int[] f = new int[faces.length];
        for (int i = 0; i < f.length; i++) {
            f[i] = faces[i];
        }
        out.putIntArray("faces", f);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(buffer::deserialize);
        loss = in.getDoubleOr("loss", 0);
        in.getIntArray("faces").ifPresent(f -> {
            for (int i = 0; i < Math.min(f.length, faces.length); i++) {
                faces[i] = (byte) Math.clamp(f[i], -1, SuperconductingCoilBlockEntity.FaceMode.values().length - 1);
            }
        });
    }

    /** Energy handler of one face: its mode and the current I/O limit gate the buffer. */
    private final class Face implements EnergyHandler {
        private final Direction side;

        Face(Direction side) {
            this.side = side;
        }

        @Override
        public long getAmountAsLong() {
            return buffer.getAmountAsLong();
        }

        @Override
        public long getCapacityAsLong() {
            return buffer.getCapacityAsLong();
        }

        @Override
        public int insert(int amount, TransactionContext tx) {
            SuperconductingCoilBlockEntity.FaceMode m = faceMode(side);
            return m == SuperconductingCoilBlockEntity.FaceMode.IN || m == SuperconductingCoilBlockEntity.FaceMode.BOTH
                ? buffer.insert(Math.min(amount, io()), tx) : 0;
        }

        @Override
        public int extract(int amount, TransactionContext tx) {
            SuperconductingCoilBlockEntity.FaceMode m = faceMode(side);
            return m == SuperconductingCoilBlockEntity.FaceMode.OUT || m == SuperconductingCoilBlockEntity.FaceMode.BOTH
                ? buffer.extract(Math.min(amount, io()), tx) : 0;
        }
    }
}

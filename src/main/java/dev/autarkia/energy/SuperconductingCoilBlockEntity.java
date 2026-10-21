package dev.autarkia.energy;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.logistics.LinkerRef;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import org.jspecify.annotations.Nullable;

/**
 * Superconducting coil, 2x2x2 storage (02 sections 1.2, 6.2 and 4.6, decision G8): 400,000,000 FE, I/O 65,536 FE/t,
 * halved while T is at least 0.5; 4 coolant slots drawn at 2 units per tick. With coolant T falls by 0.005 per tick,
 * dry it rises by 0.001 (kept as integer thousandths, so the dry quench comes after exactly 1,000 ticks). At T 1.0 it
 * quenches: an explosion of power 2 + 4 E / Emax at its center (block damage only with quenchBreaksBlocks), E = 0,
 * {@code quench.blast}, failure event {@code quench} and {@code power/quench} to the players within 32 blocks and its
 * tender; it stays quenched with no I/O until T is 0 again. Each capability face is NONE, IN, OUT or BOTH (default BOTH,
 * front NONE); the wrench cycles the clicked face. Storage priority group 2.
 */
public class SuperconductingCoilBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, GeoBlockEntity, JadeInfo, MenuProvider,
    Wrenchable {
    public static final int CAPACITY = 400_000_000;
    public static final int IO = 65536;
    public static final int USE = 2;

    /** Capability mode of one face. */
    public enum FaceMode {
        NONE, IN, OUT, BOTH;

        /** Return the translation key of the mode name. */
        public String key() {
            return "face.autarkia." + name().toLowerCase(Locale.ROOT);
        }
    }

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.superconducting_coil.idle");
    private static final RawAnimation CHARGE = RawAnimation.begin().thenLoop("animation.superconducting_coil.charge");
    private static final RawAnimation WARM = RawAnimation.begin().thenLoop("animation.superconducting_coil.warm");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer buffer = new EnergyBuffer(CAPACITY, IO, IO, this::setChanged);
    private final CoolantSlots coolant = new CoolantSlots(4, this::setChanged);
    private final LinkerRef tender = new LinkerRef();
    private final byte[] faces = {-1, -1, -1, -1, -1, -1};
    private final EnergyHandler[] handlers = new EnergyHandler[6];
    private int tempMilli;
    private boolean quenched;
    private int anim;
    private float lastBlast = -1;
    private int lastIn;
    private int lastOut;
    private int pendingIn;
    private int pendingOut;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> tempMilli;
                case 1 -> 1000;
                case 2 -> buffer.stored();
                case 3 -> buffer.capacity();
                case 4 -> lastIn;
                case 5 -> lastOut;
                case 6 -> coolant.units() / USE;
                case 7 -> quenched ? 1 : 0;
                default -> io();
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    public SuperconductingCoilBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.SUPERCONDUCTING_COIL.get(), pos, state);
        for (Direction d : Direction.values()) {
            handlers[d.ordinal()] = new Face(d);
        }
    }

    /** Return the energy buffer (internal access, no face or quench limits). */
    public EnergyBuffer buffer() {
        return buffer;
    }

    /** Return the coolant cell slots. */
    public CoolantSlots coolant() {
        return coolant;
    }

    /** Return the temperature T, 0 to 1. */
    public double temperature() {
        return tempMilli / 1000.0;
    }

    /** Return true while the coil is quenched (no I/O until T is 0 again). */
    public boolean quenched() {
        return quenched;
    }

    /** Return true while the coil is cold: it has coolant and is not quenched (a superconductor span end stays cold). */
    public boolean cold() {
        return !quenched && coolant.units() > 0;
    }

    /** Return the current I/O limit in FE/t: 65,536, 32,768 while T is at least 0.5, 0 while quenched. */
    public int io() {
        return quenched ? 0 : tempMilli >= 500 ? IO / 2 : IO;
    }

    /** Return the FE taken from the grid in the last tick. */
    public int lastIn() {
        return lastIn;
    }

    /** Return the FE given to the grid in the last tick. */
    public int lastOut() {
        return lastOut;
    }

    /** Return the power of the last quench explosion, or -1 when the coil has not quenched since it loaded. */
    public float lastBlastPower() {
        return lastBlast;
    }

    /** Return the mode of a world face (default BOTH, the front NONE). */
    public FaceMode faceMode(Direction d) {
        byte m = faces[d.ordinal()];
        if (m >= 0) {
            return FaceMode.values()[m];
        }
        return d == MachineBlock.front(getBlockState()) ? FaceMode.NONE : FaceMode.BOTH;
    }

    /** Return the energy handler of a face for the capability: null for a NONE face, the plain buffer for internal access. */
    public @Nullable EnergyHandler face(@Nullable Direction side) {
        if (side == null) {
            return buffer;
        }
        return faceMode(side) == FaceMode.NONE ? null : handlers[side.ordinal()];
    }

    @Override
    public void serverTick() {
        boolean wet = coolant.draw(USE) > 0;
        tempMilli = wet ? Math.max(0, tempMilli - 5) : Math.min(1000, tempMilli + 1);
        if (!quenched && tempMilli >= 1000) {
            quench();
        } else if (quenched && tempMilli == 0) {
            quenched = false;
        }
        lastIn = pendingIn;
        lastOut = pendingOut;
        pendingIn = 0;
        pendingOut = 0;
        int nowAnim = quenched || tempMilli >= 300 ? 2 : lastIn + lastOut > 0 ? 1 : 0;
        if (nowAnim != anim) {
            anim = nowAnim;
            syncThrottled(10);
        } else {
            flushSync(10);
        }
    }

    private void quench() {
        ServerLevel sl = serverLevel();
        Vec3 c = middle();
        lastBlast = 2f + 4f * buffer.stored() / (float) CAPACITY;
        sl.explode(null, c.x, c.y, c.z, lastBlast, false,
            AutarkiaConfig.QUENCH_BREAKS_BLOCKS.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
        buffer.drain(buffer.stored());
        quenched = true;
        sl.playSound(null, c.x, c.y, c.z, AutarkiaSounds.of("quench.blast"), SoundSource.BLOCKS, 1f, 1f);
        dev.autarkia.colony.ColonyMemory.at(sl, worldPosition, "quench", Long.toString(worldPosition.asLong()));
        tender.award(sl, c, "power/quench");
        Autarkia.LOG.info("[grid] superconducting coil at {} quenched (blast power {})", worldPosition.toShortString(),
            String.format(Locale.ROOT, "%.2f", lastBlast));
    }

    /** Return the middle of the 2x2x2 footprint. */
    public Vec3 middle() {
        double x = 0;
        double y = 0;
        double z = 0;
        java.util.Set<BlockPos> parts = ((BaseMultiblock) getBlockState().getBlock()).getMultiblockShape(level, worldPosition, getBlockState())
            .getGlobalPositions();
        for (BlockPos p : parts) {
            x += p.getX() + 0.5;
            y += p.getY() + 0.5;
            z += p.getZ() + 0.5;
        }
        int n = Math.max(1, parts.size());
        return new Vec3(x / n, y / n, z / n);
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
        return 2;
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
        if (!isCenter() && level.getBlockEntity(getCenter()) instanceof SuperconductingCoilBlockEntity c) {
            return c.wrench(player, clicked, face);
        }
        FaceMode next = FaceMode.values()[(faceMode(face).ordinal() + 1) % FaceMode.values().length];
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
        out.add(Component.translatable("jade.autarkia.stored", buffer.stored(), buffer.capacity(), buffer.stored() * 100L / buffer.capacity()));
        out.add(Component.translatable("jade.autarkia.io", lastIn, lastOut));
        out.add(Component.translatable("jade.autarkia.coolant", coolant.units() / USE / 20));
        out.add(Component.translatable("jade.autarkia.temperature", Math.round(tempMilli / 10.0)));
        if (quenched) {
            out.add(Component.translatable("jade.autarkia.quenched"));
        }
    }

    @Override
    public float jadeProgress() {
        return buffer.fillRatio();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide() && isCenter()) {
            for (int i = 0; i < coolant.size(); i++) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, coolant.get(i).copy());
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SuperconductingCoilBlockEntity>("coil", 10, test -> {
            SuperconductingCoilBlockEntity be = test.animatable();
            return test.setAndContinue(be.anim == 2 ? WARM : be.anim == 1 ? CHARGE : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.autarkia.superconducting_coil");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        if (player instanceof ServerPlayer sp) {
            tender.set(sp);
        }
        return new SlotMenu(AutarkiaMenus.SUPERCONDUCTING_COIL.get(), id, inv, worldPosition, coolant, data, GuiLayouts.SUPERCONDUCTING_COIL);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        buffer.serialize(out.child("energy"));
        coolant.serialize(out.child("coolant"));
        tender.save(out);
        out.putInt("temp", tempMilli);
        out.putBoolean("quenched", quenched);
        out.putInt("anim", anim);
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
        in.child("coolant").ifPresent(coolant::deserialize);
        tender.load(in);
        tempMilli = in.getIntOr("temp", 0);
        quenched = in.getBooleanOr("quenched", false);
        anim = in.getIntOr("anim", 0);
        in.getIntArray("faces").ifPresent(f -> {
            for (int i = 0; i < Math.min(f.length, faces.length); i++) {
                faces[i] = (byte) Math.clamp(f[i], -1, FaceMode.values().length - 1);
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
            FaceMode m = faceMode(side);
            return m == FaceMode.IN || m == FaceMode.BOTH ? buffer.insert(Math.min(amount, io()), tx) : 0;
        }

        @Override
        public int extract(int amount, TransactionContext tx) {
            FaceMode m = faceMode(side);
            return m == FaceMode.OUT || m == FaceMode.BOTH ? buffer.extract(Math.min(amount, io()), tx) : 0;
        }
    }
}

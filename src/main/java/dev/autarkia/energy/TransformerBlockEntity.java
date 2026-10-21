package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.Ticking;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Transformer state (02 section 6.3). It is a member of two networks: a consumer in the source network and a producer
 * in the target network, with one buffer of 2 x rated(lower tier). Transfer per tick is at most rated(lower tier) and
 * 2% of the intake is lost. Mode AUTO moves energy from the network with the larger surplus last tick (its own share
 * left out) to the other; the wrench cycles AUTO, A_TO_B and B_TO_A.
 */
public class TransformerBlockEntity extends AutarkiaBlockEntity implements GridMultiMember, Ticking, JadeInfo, Wrenchable {
    /** Transfer direction setting. */
    public enum Mode { AUTO, A_TO_B, B_TO_A }

    private final EnergyBuffer buffer = new EnergyBuffer(0, 0, 0, this::setChanged);
    private final Side sideA = new Side(true);
    private final Side sideB = new Side(false);
    private @Nullable CableTier tierA;
    private @Nullable CableTier tierB;
    private Mode mode = Mode.AUTO;
    private boolean aToB = true;
    private double lossCarry;
    private int flow;
    private int pendingFlow;
    private int intake;
    private int pendingIntake;

    /** One side of the transformer as a grid member of the network of its tier. */
    private final class Side implements GridMember {
        private final boolean isA;

        Side(boolean isA) {
            this.isA = isA;
        }

        private boolean source() {
            return isA == aToB;
        }

        @Override
        public Role gridRole() {
            return source() ? Role.CONSUMER : Role.GENERATOR;
        }

        @Override
        public EnergyBuffer gridBuffer() {
            return buffer;
        }

        @Override
        public int gridMaxOut() {
            return source() ? 0 : lowerRated();
        }

        @Override
        public int gridMaxIn() {
            return source() ? lowerRated() : 0;
        }

        @Override
        public int workDraw() {
            return 0;
        }

        @Override
        public boolean gridValid() {
            return !isRemoved() && tierA != null && tierB != null;
        }

        @Override
        public void gridTransferred(int amount) {
            if (amount < 0 && source()) {
                pendingIntake -= amount;
                lossCarry += 0.02 * -amount;
                int lost = (int) lossCarry;
                buffer.drain(lost);
                lossCarry -= lost;
            } else if (amount > 0 && !source()) {
                pendingFlow += amount;
            }
        }
    }

    public TransformerBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.TRANSFORMER.get(), pos, state);
    }

    /** Return the transfer mode. */
    public Mode mode() {
        return mode;
    }

    /** Set the transfer mode. */
    public void setMode(Mode mode) {
        this.mode = mode;
        setChanged();
    }

    /** Return the FE given to the target network in the last tick. */
    public int flow() {
        return flow;
    }

    /** Return the FE taken from the source network in the last tick (before the 2% loss). */
    public int intake() {
        return intake;
    }

    /** Return true while energy moves from side A to side B. */
    public boolean aToB() {
        return aToB;
    }

    /** Return the tier of side A, or null before the first span. */
    public @Nullable CableTier tierA() {
        return tierA;
    }

    /** Return the tier of side B, or null before a span of a second tier. */
    public @Nullable CableTier tierB() {
        return tierB;
    }

    private int lowerRated() {
        return tierA == null || tierB == null ? 0 : Math.min(tierA.rated, tierB.rated);
    }

    private void refreshSides() {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        GridSavedData data = Grid.of(sl).data();
        IntArrayList ids = new IntArrayList(data.at(worldPosition.asLong()));
        ids.sort(null);
        CableTier a = null;
        CableTier b = null;
        for (int i = 0; i < ids.size(); i++) {
            Span s = data.spans.get(ids.getInt(i));
            if (s == null) {
                continue;
            }
            if (a == null) {
                a = s.tier;
            } else if (b == null && s.tier != a) {
                b = s.tier;
            }
        }
        tierA = a;
        tierB = b;
        buffer.setCapacity(2 * lowerRated());
    }

    @Override
    public @Nullable GridMember gridSide(CableTier tier) {
        refreshSides();
        return tier == tierA ? sideA : tier == tierB ? sideB : null;
    }

    @Override
    public void serverTick() {
        refreshSides();
        flow = pendingFlow;
        pendingFlow = 0;
        intake = pendingIntake;
        pendingIntake = 0;
        if (tierA == null || tierB == null) {
            return;
        }
        boolean before = aToB;
        aToB = switch (mode) {
            case A_TO_B -> true;
            case B_TO_A -> false;
            case AUTO -> autoDirection();
        };
        if (before != aToB) {
            setChanged();
        }
    }

    private boolean autoDirection() {
        GridLevel g = Grid.of((ServerLevel) level);
        GridNetwork netA = g.network(worldPosition.asLong(), tierA);
        GridNetwork netB = g.network(worldPosition.asLong(), tierB);
        if (netA == null || netB == null) {
            return aToB;
        }
        double sa = netA.surplusWithout(sideA);
        double sb = netB.surplusWithout(sideB);
        if (sa > sb + 1) {
            return true;
        }
        return sb > sa + 1 ? false : aToB;
    }

    @Override
    public InteractionResult wrench(ServerPlayer player) {
        setMode(Mode.values()[(mode.ordinal() + 1) % Mode.values().length]);
        level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f);
        player.sendOverlayMessage(Component.translatable("message.autarkia.transformer.mode", Component.translatable("mode.autarkia.transformer."
            + mode.name().toLowerCase(java.util.Locale.ROOT))));
        return InteractionResult.SUCCESS;
    }

    @Override
    public String jadeGroup() {
        return "power";
    }

    @Override
    public void jadeLines(List<Component> out) {
        refreshSides();
        out.add(Component.translatable("jade.autarkia.transformer.sides", tierName(tierA), tierName(tierB)));
        out.add(Component.translatable("jade.autarkia.transformer.mode", Component.translatable("mode.autarkia.transformer."
            + mode.name().toLowerCase(java.util.Locale.ROOT))));
        out.add(Component.translatable("jade.autarkia.transformer.flow", flow));
    }

    private static Component tierName(@Nullable CableTier t) {
        return t == null ? Component.translatable("jade.autarkia.transformer.none") : Component.translatable("tier.autarkia." + t.getSerializedName());
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        buffer.serialize(out.child("energy"));
        out.putString("mode", mode.name());
        out.putBoolean("aToB", aToB);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        buffer.setCapacity(Integer.MAX_VALUE);
        in.child("energy").ifPresent(buffer::deserialize);
        mode = Mode.valueOf(in.getStringOr("mode", Mode.AUTO.name()));
        aToB = in.getBooleanOr("aToB", true);
    }
}

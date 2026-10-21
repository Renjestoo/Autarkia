package dev.autarkia.energy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** The six cable tiers of 02 section 3 with their rated flow, span limits, loss, sag and heat constants. */
public enum CableTier implements StringRepresentable {
    ROPE("rope", 40, 12, 0.005, 0.060, 8, 0.06f, 0, 0, 100, 3f, false),
    COPPER("copper", 256, 16, 0.002, 0.040, 16, 0.04f, 0.0025, 0.0020, 140, 4f, true),
    INSULATED("insulated", 1024, 24, 0.001, 0.030, 16, 0.06f, 0.0020, 0.0015, 140, 6f, true),
    HV("hv", 8192, 48, 0.0002, 0.020, 16, 0.08f, 0.0015, 0.0012, 200, 10f, true),
    SUPERCONDUCTOR("superconductor", 65536, 32, 0.0, 0.010, 8, 0.07f, 0, 0, 60, 6f, true),
    PHASE("phase", 1048576, 64, 0.0, 0.0, 0, 0.05f, 0, 0, 0, 0f, false);

    public static final StreamCodec<ByteBuf, CableTier> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);
    public static final com.mojang.serialization.Codec<CableTier> CODEC = StringRepresentable.fromEnum(CableTier::values);

    private final String id;
    public final int rated;
    public final int maxSpan;
    public final double lossPerBlock;
    public final double sagFactor;
    public final int blocksPerItem;
    public final float thickness;
    public final double heatGain;
    public final double heatCool;
    public final int snapLifetime;
    public final float snapDamage;
    public final boolean conductsLightning;

    CableTier(String id, int rated, int maxSpan, double lossPerBlock, double sagFactor, int blocksPerItem, float thickness,
              double heatGain, double heatCool, int snapLifetime, float snapDamage, boolean conductsLightning) {
        this.id = id;
        this.rated = rated;
        this.maxSpan = maxSpan;
        this.lossPerBlock = lossPerBlock;
        this.sagFactor = sagFactor;
        this.blocksPerItem = blocksPerItem;
        this.thickness = thickness;
        this.heatGain = heatGain;
        this.heatCool = heatCool;
        this.snapLifetime = snapLifetime;
        this.snapDamage = snapDamage;
        this.conductsLightning = conductsLightning;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** Return true for tiers whose spans heat up under overload. */
    public boolean hasHeat() {
        return heatGain > 0;
    }

    /** Return the number of cable items one span of the given length costs (02 section 3). */
    public int itemCost(double length) {
        return blocksPerItem == 0 ? 0 : (int) Math.ceil(length / blocksPerItem);
    }

    /** Return the next higher tier, or this tier when it is the highest. */
    public CableTier next() {
        return this == PHASE ? PHASE : values()[ordinal() + 1];
    }
}

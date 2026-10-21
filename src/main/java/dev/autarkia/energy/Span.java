package dev.autarkia.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** One cable span between two grid nodes (02 section 2.1); persistent fields are saved, the rest is runtime state. */
public final class Span {
    public static final Codec<Span> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("id").forGetter(s -> s.id),
        Codec.LONG.fieldOf("a").forGetter(s -> s.a),
        Codec.LONG.fieldOf("b").forGetter(s -> s.b),
        Vec3.CODEC.fieldOf("attachA").forGetter(s -> s.attachA),
        Vec3.CODEC.fieldOf("attachB").forGetter(s -> s.attachB),
        CableTier.CODEC.fieldOf("tier").forGetter(s -> s.tier),
        Codec.FLOAT.fieldOf("length").forGetter(s -> s.length),
        Codec.FLOAT.fieldOf("heat").forGetter(s -> s.heat),
        Codec.FLOAT.fieldOf("wear").forGetter(s -> s.wear),
        Codec.FLOAT.fieldOf("temp").forGetter(s -> s.temp)
    ).apply(i, Span::new));

    public final int id;
    public final long a;
    public final long b;
    public final Vec3 attachA;
    public final Vec3 attachB;
    public final CableTier tier;
    public final float length;
    public float heat;
    public float wear;
    public float temp;

    public double flow;
    public double requestedFlow;
    public float load;
    public boolean slip;
    public boolean active;
    public boolean treeEdge;
    public int bundle = 1;
    public byte sentHeat = -1;
    public byte sentLoad = -1;
    public byte sentTemp = -1;
    public boolean sentSlip;
    public boolean sentActive = true;
    public long struckAt = Long.MIN_VALUE / 2;

    public Span(int id, long a, long b, Vec3 attachA, Vec3 attachB, CableTier tier, float length, float heat, float wear, float temp) {
        this.id = id;
        this.a = a;
        this.b = b;
        this.attachA = attachA;
        this.attachB = attachB;
        this.tier = tier;
        this.length = length;
        this.heat = heat;
        this.wear = wear;
        this.temp = temp;
    }

    /** Return the node position at the other end. */
    public long other(long node) {
        return node == a ? b : a;
    }

    /** Return true when the span joins the two nodes in either order. */
    public boolean joins(long x, long y) {
        return (a == x && b == y) || (a == y && b == x);
    }

    /** Return the sag-relevant h of 02 section 4.1: heat for metal tiers, wear for rope, 0 for superconductor. */
    public float sagH() {
        return tier == CableTier.ROPE ? wear : tier == CableTier.SUPERCONDUCTOR ? 0 : heat;
    }

    /** Return the midpoint of the straight chord. */
    public Vec3 mid() {
        return attachA.add(attachB).scale(0.5);
    }

    /** Return the first node position. */
    public BlockPos posA() {
        return BlockPos.of(a);
    }

    /** Return the second node position. */
    public BlockPos posB() {
        return BlockPos.of(b);
    }
}

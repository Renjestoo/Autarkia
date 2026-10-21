package dev.autarkia.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.autarkia.Autarkia;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-dimension grid save data (02 section 2.5): spans, fuses, phase links and masts. Nodes and the chunk index are
 * rebuilt from the span ends on load; networks are never saved.
 */
public final class GridSavedData extends SavedData {
    public static final int VERSION = 1;

    /** Saved fuse state. */
    public record FuseState(long pos, boolean open, long resetAt, int recentTrips, boolean locked) {
        public static final Codec<FuseState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("pos").forGetter(FuseState::pos), Codec.BOOL.fieldOf("open").forGetter(FuseState::open),
            Codec.LONG.fieldOf("resetAt").forGetter(FuseState::resetAt), Codec.INT.fieldOf("recentTrips").forGetter(FuseState::recentTrips),
            Codec.BOOL.optionalFieldOf("locked", false).forGetter(FuseState::locked)).apply(i, FuseState::new));
    }

    /** Saved phase link between two phase nodes. */
    public record PhaseLink(long a, long b) {
        public static final Codec<PhaseLink> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("a").forGetter(PhaseLink::a), Codec.LONG.fieldOf("b").forGetter(PhaseLink::b)).apply(i, PhaseLink::new));
    }

    public static final Codec<GridSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.optionalFieldOf("version", VERSION).forGetter(d -> VERSION),
        Span.CODEC.listOf().optionalFieldOf("spans", List.of()).forGetter(d -> new ArrayList<>(d.spans.values())),
        FuseState.CODEC.listOf().optionalFieldOf("fuses", List.of()).forGetter(d -> new ArrayList<>(d.fuses.values())),
        PhaseLink.CODEC.listOf().optionalFieldOf("phaseLinks", List.of()).forGetter(d -> d.phaseLinks),
        Codec.LONG.listOf().optionalFieldOf("masts", List.of()).forGetter(d -> d.masts),
        Codec.INT.optionalFieldOf("nextSpanId", 1).forGetter(d -> d.nextSpanId)
    ).apply(i, GridSavedData::new));

    public static final SavedDataType<GridSavedData> TYPE = new SavedDataType<>(Autarkia.id("grid"), GridSavedData::new, CODEC);

    public final Int2ObjectLinkedOpenHashMap<Span> spans = new Int2ObjectLinkedOpenHashMap<>();
    public final Long2ObjectOpenHashMap<IntArrayList> byNode = new Long2ObjectOpenHashMap<>();
    public final Long2ObjectOpenHashMap<LongOpenHashSet> byChunk = new Long2ObjectOpenHashMap<>();
    public final Long2ObjectOpenHashMap<FuseState> fuses = new Long2ObjectOpenHashMap<>();
    public final List<PhaseLink> phaseLinks = new ArrayList<>();
    public final List<Long> masts = new ArrayList<>();
    public int nextSpanId = 1;

    public GridSavedData() {
    }

    private GridSavedData(int version, List<Span> spans, List<FuseState> fuses, List<PhaseLink> links, List<Long> masts, int nextId) {
        for (Span s : spans) {
            add(s);
        }
        for (FuseState f : fuses) {
            this.fuses.put(f.pos(), f);
        }
        this.phaseLinks.addAll(links);
        this.masts.addAll(masts);
        this.nextSpanId = Math.max(nextId, spans.stream().mapToInt(s -> s.id).max().orElse(0) + 1);
    }

    /** Return the grid data of a level. */
    public static GridSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Add a span and index its nodes. */
    public void add(Span s) {
        spans.put(s.id, s);
        byNode.computeIfAbsent(s.a, k -> new IntArrayList()).add(s.id);
        byNode.computeIfAbsent(s.b, k -> new IntArrayList()).add(s.id);
        byChunk.computeIfAbsent(chunkKey(s.a), k -> new LongOpenHashSet()).add(s.a);
        byChunk.computeIfAbsent(chunkKey(s.b), k -> new LongOpenHashSet()).add(s.b);
        setDirty();
    }

    /** Return the chunk key (ChunkPos.pack form) of a node. */
    public static long chunkKey(long node) {
        return net.minecraft.world.level.ChunkPos.pack(BlockPos.getX(node) >> 4, BlockPos.getZ(node) >> 4);
    }

    /** Return the nodes in a chunk (by chunk key); empty when none. */
    public LongOpenHashSet nodesIn(long chunkKey) {
        LongOpenHashSet s = byChunk.get(chunkKey);
        return s == null ? new LongOpenHashSet() : s;
    }

    /** Remove a span and unindex its nodes. */
    public Span remove(int id) {
        Span s = spans.remove(id);
        if (s != null) {
            unindex(s.a, id);
            unindex(s.b, id);
            setDirty();
        }
        return s;
    }

    private void unindex(long node, int id) {
        IntArrayList l = byNode.get(node);
        if (l != null) {
            l.rem(id);
            if (l.isEmpty()) {
                byNode.remove(node);
                LongOpenHashSet c = byChunk.get(chunkKey(node));
                if (c != null) {
                    c.remove(node);
                }
            }
        }
    }

    /** Return the span ids at a node (empty list when none). */
    public IntArrayList at(long node) {
        IntArrayList l = byNode.get(node);
        return l == null ? new IntArrayList() : l;
    }
}

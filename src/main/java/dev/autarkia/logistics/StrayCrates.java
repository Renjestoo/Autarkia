package dev.autarkia.logistics;

import com.mojang.serialization.Codec;
import dev.autarkia.Autarkia;
import dev.autarkia.registry.AutarkiaBlocks;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Crate blocks that falling crates left in the open (04 section 3, sensor {@code transport.stray_crates}): every crate
 * block a launcher miss, a rack collapse or a fallen gondola places. Saved per dimension; a position leaves the list once
 * its chunk is loaded and it no longer holds a crate block (Hauler Crabs or players took it).
 */
public final class StrayCrates extends SavedData {
    public static final Codec<StrayCrates> CODEC = Codec.LONG.listOf().xmap(StrayCrates::new, s -> List.copyOf(s.positions));
    public static final SavedDataType<StrayCrates> TYPE = new SavedDataType<>(Autarkia.id("stray_crates"), StrayCrates::new, CODEC);
    private final Set<Long> positions = new LinkedHashSet<>();

    public StrayCrates() {
    }

    private StrayCrates(List<Long> list) {
        positions.addAll(list);
    }

    /** Return the stray crate data of a level. */
    public static StrayCrates get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Record a crate block a falling crate placed. */
    public static void add(ServerLevel level, BlockPos pos) {
        StrayCrates s = get(level);
        if (s.positions.add(pos.asLong())) {
            s.setDirty();
        }
    }

    /** Return the stray crate positions of the level, dropping loaded positions that hold no crate block any more. */
    public static List<BlockPos> list(ServerLevel level) {
        StrayCrates s = get(level);
        List<BlockPos> out = new ArrayList<>();
        boolean changed = false;
        for (Long l : List.copyOf(s.positions)) {
            BlockPos p = BlockPos.of(l);
            if (level.isLoaded(p) && !level.getBlockState(p).is(AutarkiaBlocks.CRATE.get())) {
                s.positions.remove(l);
                changed = true;
            } else {
                out.add(p);
            }
        }
        if (changed) {
            s.setDirty();
        }
        return out;
    }
}

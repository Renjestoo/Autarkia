package dev.autarkia.colony;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.autarkia.Autarkia;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Global save data {@code autarkia:colonies} (05 section 1.5, file {@code <world>/data/autarkia/colonies.dat}). Each
 * colony decodes on its own, so one bad entry is logged and skipped instead of clearing every colony.
 */
public final class ColonyRegistry extends SavedData {
    public static final int VERSION = 1;

    private static final Codec<java.util.Optional<Colony>> LENIENT = Codec.PASSTHROUGH.xmap(d -> Colony.CODEC.parse(d).resultOrPartial(
        e -> Autarkia.LOG.error("Skipping a colony record that does not parse: {}", e)), c -> new com.mojang.serialization.Dynamic<>(
        net.minecraft.nbt.NbtOps.INSTANCE, Colony.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, c.orElseThrow()).getOrThrow()));

    public static final Codec<ColonyRegistry> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.optionalFieldOf("version", VERSION).forGetter(r -> VERSION),
        LENIENT.listOf().optionalFieldOf("colonies", List.of()).forGetter(r -> r.colonies.values().stream().map(java.util.Optional::of).toList())
    ).apply(i, ColonyRegistry::new));

    public static final SavedDataType<ColonyRegistry> TYPE = new SavedDataType<>(Autarkia.id("colonies"), ColonyRegistry::new, CODEC);

    private final Map<UUID, Colony> colonies = new LinkedHashMap<>();

    public ColonyRegistry() {
    }

    private ColonyRegistry(int version, List<java.util.Optional<Colony>> list) {
        list.forEach(o -> o.ifPresent(c -> colonies.put(c.id, c)));
    }

    /** Return the registry of the server. */
    public static ColonyRegistry get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Return every colony in insertion order. */
    public Collection<Colony> all() {
        return colonies.values();
    }

    /** Return the colony with the id, or null. */
    public Colony get(UUID id) {
        return colonies.get(id);
    }

    /** Add a colony and mark the data dirty. */
    public void add(Colony c) {
        colonies.put(c.id, c);
        setDirty();
    }

    /** Remove a colony record and mark the data dirty. */
    public void remove(UUID id) {
        colonies.remove(id);
        setDirty();
    }
}

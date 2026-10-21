package dev.autarkia.storage;

import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The stock sources of a {@code domain_archive} (04 section 2.8): every warehouse controller (its index; deliveries
 * take only unreserved crate contents), depot, matter buffer (stored items, not free MU) and the crates in every tally
 * board area, in the loaded chunks of the colony domain (Core, beacon and outpost squares). A crate inside two tally
 * areas counts once.
 */
public final class ArchiveSources {
    /** Kind of a source. */
    public enum Kind { DEPOT, TALLY, WAREHOUSE, MATTER }

    /** One source block: its kind, position and block entity (a crate for a tally area entry). */
    public record Source(Kind kind, BlockPos pos, BlockEntity be) {
    }

    /** The scanned sources and the merged stock: per item {available, total}. */
    public record Scan(List<Source> sources, Map<ItemResource, long[]> stock) {
        public static final Scan EMPTY = new Scan(List.of(), Map.of());
    }

    private ArchiveSources() {
    }

    /** Scan the loaded chunks of the domain for sources; depots and tally crates first, then warehouses, then matter buffers. */
    public static Scan scan(ServerLevel level, Colony c) {
        List<Source> out = new ArrayList<>();
        Set<BlockPos> crates = new HashSet<>();
        for (long packed : Colonies.chunks(c)) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(net.minecraft.world.level.ChunkPos.getX(packed), net.minecraft.world.level.ChunkPos.getZ(packed));
            if (chunk == null) {
                continue;
            }
            for (BlockEntity be : List.copyOf(chunk.getBlockEntities().values())) {
                BlockPos p = be.getBlockPos();
                if (be.isRemoved() || !Colonies.inside(c, p)) {
                    continue;
                }
                if (be instanceof DepotBlockEntity) {
                    out.add(new Source(Kind.DEPOT, p, be));
                } else if (be instanceof WarehouseControllerBlockEntity) {
                    out.add(new Source(Kind.WAREHOUSE, p, be));
                } else if (be instanceof MatterBufferBlockEntity b && b.isCenter()) {
                    out.add(new Source(Kind.MATTER, p, be));
                } else if (be instanceof TallyBoardBlockEntity) {
                    int r = TallyBoardBlockEntity.RADIUS;
                    for (BlockPos q : BlockPos.betweenClosed(p.offset(-r, -r, -r), p.offset(r, r, r))) {
                        if (level.isLoaded(q) && level.getBlockEntity(q) instanceof CrateBlockEntity crate && crates.add(q.immutable())) {
                            out.add(new Source(Kind.TALLY, q.immutable(), crate));
                        }
                    }
                }
            }
        }
        out.sort(Comparator.comparingInt((Source s) -> s.kind().ordinal()).thenComparingLong(s -> s.pos().asLong()));
        Map<ItemResource, long[]> stock = new LinkedHashMap<>();
        for (Source s : out) {
            if (s.be() instanceof WarehouseControllerBlockEntity w) {
                for (Map.Entry<ItemResource, Long> e : w.stock().entrySet()) {
                    long[] v = stock.computeIfAbsent(e.getKey(), k -> new long[2]);
                    v[0] += w.available(e.getKey());
                    v[1] += e.getValue();
                }
                continue;
            }
            ResourceHandler<ItemResource> h = handler(s);
            for (int i = 0; h != null && i < h.size(); i++) {
                long n = h.getAmountAsLong(i);
                if (n > 0 && !h.getResource(i).isEmpty()) {
                    long[] v = stock.computeIfAbsent(h.getResource(i), k -> new long[2]);
                    v[0] += n;
                    v[1] += n;
                }
            }
        }
        return new Scan(List.copyOf(out), stock);
    }

    private static ResourceHandler<ItemResource> handler(Source s) {
        return switch (s.be()) {
            case DepotBlockEntity d -> d.items();
            case CrateBlockEntity c -> c.items();
            case MatterBufferBlockEntity b -> b.items();
            default -> null;
        };
    }

    /**
     * Move up to amount of the item from the sources into the target inside the transaction; skip a source at skip (the
     * block the target pushes into). Fill moved with the positions that gave items; return the count moved.
     */
    public static int deliver(ServerLevel level, List<Source> sources, ItemResource item, int amount, ResourceHandler<ItemResource> target, BlockPos skip,
                              List<BlockPos> moved, TransactionContext tx) {
        int left = amount;
        for (Source s : sources) {
            if (left <= 0) {
                break;
            }
            if (s.pos().equals(skip) || s.be().isRemoved()) {
                continue;
            }
            int n = 0;
            if (s.be() instanceof WarehouseControllerBlockEntity w) {
                for (LedgerRequest.Reservation r : w.unreserved(item)) {
                    if (left - n <= 0) {
                        break;
                    }
                    if (!r.rack().equals(skip) && level.getBlockEntity(r.rack()) instanceof RackBlockEntity rack && rack.present(r.index())) {
                        n += ResourceHandlerUtil.move(rack.position(r.index()), target, item::equals, Math.min(left - n, r.amount()), tx);
                    }
                }
            } else {
                ResourceHandler<ItemResource> h = handler(s);
                if (h != null) {
                    n = ResourceHandlerUtil.move(h, target, item::equals, left, tx);
                }
            }
            if (n > 0) {
                left -= n;
                moved.add(s.pos());
            }
        }
        return amount - left;
    }
}

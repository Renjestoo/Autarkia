package dev.autarkia.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * A remote site request (04 section 2.6, {@code TransferOrder(item, count, fromSite, toSite)}): the source depot pushes
 * the items into an adjacent sender station (cable car station, crate launcher or capsule station) and the transport
 * carries them; without a sender the order waits with "No transport at site". It ends DONE when all items left the
 * depot, PARTIAL or FAILED when the depot ran out of the item. Orders are saved with the {@link DomainStockData} of the
 * source depot's dimension.
 */
public final class TransferOrder {
    /** Order status. */
    public enum Status {
        WAITING, MOVING, DONE, PARTIAL, FAILED;

        /** Return true while the order still moves items. */
        public boolean open() {
            return this == WAITING || this == MOVING;
        }

        /** Return the translation key of the status. */
        public String key() {
            return "screen.autarkia.sites.order." + name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Codec<TransferOrder> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("id").forGetter(o -> o.id), ItemStack.CODEC.fieldOf("item").forGetter(o -> o.item),
        Codec.INT.fieldOf("count").forGetter(o -> o.count), BlockPos.CODEC.fieldOf("source").forGetter(o -> o.source),
        Codec.STRING.fieldOf("from").forGetter(o -> o.fromSite), Codec.STRING.fieldOf("to").forGetter(o -> o.toSite),
        Codec.INT.optionalFieldOf("moved", 0).forGetter(o -> o.moved), Codec.STRING.optionalFieldOf("status", "WAITING").forGetter(o -> o.status.name()),
        Codec.STRING.optionalFieldOf("reason", "").forGetter(o -> o.reason), Codec.LONG.optionalFieldOf("created", 0L).forGetter(o -> o.created)
    ).apply(i, TransferOrder::new));

    public final int id;
    public final ItemStack item;
    public final int count;
    public final BlockPos source;
    public final String fromSite;
    public final String toSite;
    int moved;
    Status status;
    String reason;
    public final long created;

    private TransferOrder(int id, ItemStack item, int count, BlockPos source, String from, String to, int moved, String status, String reason,
                          long created) {
        this.id = id;
        this.item = item.copyWithCount(1);
        this.count = count;
        this.source = source.immutable();
        this.fromSite = from;
        this.toSite = to;
        this.moved = moved;
        this.status = Status.valueOf(status);
        this.reason = reason;
        this.created = created;
    }

    /** Create a new waiting order to move count of item from the depot at source (site key from) to the site key to. */
    public static TransferOrder create(int id, ItemStack item, int count, BlockPos source, String from, String to, long now) {
        return new TransferOrder(id, item, count, source, from, to, 0, Status.WAITING.name(), "", now);
    }

    /** Return the items moved out of the depot so far. */
    public int moved() {
        return moved;
    }

    /** Return the status. */
    public Status status() {
        return status;
    }

    /** Return the reason key of a wait or an end (empty for none): no_transport or none_available. */
    public String reason() {
        return reason;
    }
}

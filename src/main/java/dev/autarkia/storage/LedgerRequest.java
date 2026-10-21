package dev.autarkia.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * One ledger request (04 section 2.5): item and amount for a terminal and player, its status, the crates reserved for
 * it ({@code plan}) and the crates of the running trip, the amount delivered, the ETA and the Loader on it. A store
 * request (put-away of the terminal input) has the nil player and amount 0. Requests live in their warehouse controller
 * and are not saved: a restart drops open requests and their reservations; a lifted crate stays in the Loader's cargo
 * and goes back to a free rack position.
 */
public final class LedgerRequest {
    /** Request states of 04 section 2.5. */
    public enum Status { QUEUED, ASSIGNED, FETCHING, DELIVERING, DONE, PARTIAL, FAILED }

    /** A reserved crate position: rack, position index and the amount reserved there. */
    public record Reservation(BlockPos rack, int index, int amount) {
    }

    /** The client view of a request in the ledger queue. */
    public record View(UUID id, ItemStack item, int amount, int delivered, Status status, int etaSeconds, String reason) {
        public static final StreamCodec<RegistryFriendlyByteBuf, View> CODEC = StreamCodec.composite(
            net.minecraft.core.UUIDUtil.STREAM_CODEC, View::id, ItemStack.OPTIONAL_STREAM_CODEC, View::item, ByteBufCodecs.VAR_INT, View::amount,
            ByteBufCodecs.VAR_INT, View::delivered, ByteBufCodecs.idMapper(i -> Status.values()[i], Status::ordinal), View::status,
            ByteBufCodecs.VAR_INT, View::etaSeconds, ByteBufCodecs.STRING_UTF8, View::reason, View::new);
    }

    public final UUID id = UUID.randomUUID();
    public final UUID player;
    public net.minecraft.server.level.@Nullable ServerPlayer requester;
    public final BlockPos terminal;
    public final ItemStack item;
    public final int amount;
    public final boolean store;
    public int delivered;
    public Status status = Status.QUEUED;
    public String reason = "";
    public final List<Reservation> plan = new ArrayList<>();
    public final List<Reservation> trip = new ArrayList<>();
    public @Nullable UUID jobId;
    public @Nullable UUID loader;
    public long created;
    public long assignedAt;
    public int etaTicks;
    public long waitSince = -1;

    public LedgerRequest(UUID player, BlockPos terminal, ItemStack item, int amount, boolean store, long created) {
        this.player = player;
        this.terminal = terminal.immutable();
        this.item = item.copyWithCount(1);
        this.amount = amount;
        this.store = store;
        this.created = created;
    }

    /** Return true while the request still runs. */
    public boolean open() {
        return status != Status.DONE && status != Status.PARTIAL && status != Status.FAILED;
    }

    /** Return the amount still to deliver. */
    public int remaining() {
        return amount - delivered;
    }

    /** Return the client view with the ETA left at the given game time. */
    public View view(long now) {
        int eta = status == Status.QUEUED || !open() ? 0 : (int) Math.max(0, (assignedAt + etaTicks - now + 19) / 20);
        return new View(id, item, amount, delivered, status, eta, reason);
    }
}

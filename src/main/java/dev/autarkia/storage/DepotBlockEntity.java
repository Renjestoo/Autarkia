package dev.autarkia.storage;

import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.logistics.CableCarStationBlockEntity;
import dev.autarkia.logistics.CapsuleStationBlockEntity;
import dev.autarkia.logistics.CrateLauncherBlockEntity;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.nikdo53.tinymultiblocklib.blockentities.IMultiBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * {@code depot} (04 section 2.6): 54 slots on all faces (filled crates refused) and a site name. It belongs to the
 * nearest outpost core within 16 blocks, else to the main site, and pushes its stock to {@link DomainStockData} when it
 * changed (at most once per {@code stockSyncIntervalTicks}) and at least every 600 ticks, so it turns stale 1,200 ticks
 * after its chunk unloads. Every 20 ticks it serves the open {@link TransferOrder}s of its site: it pushes the ordered
 * items into an adjacent sender station (cable car station, crate launcher, mag rail station or capsule station input).
 */
public class DepotBlockEntity extends AutarkiaBlockEntity implements Ticking, JadeInfo, MenuProvider, dev.autarkia.colony.ColonyStorage,
    dev.autarkia.colony.ColonySensors.Member {
    public static final int SLOTS = 54;
    public static final int HEARTBEAT = 600;
    public static final int NAME_LENGTH = 32;
    private final ItemStacksResourceHandler items = new ItemStacksResourceHandler(SLOTS) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            return !CrateBlockEntity.isFilledCrate(resource.toStack(1));
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            dirty = true;
            setChanged();
        }
    };
    private String siteName = "";
    private boolean dirty = true;
    private long lastPush = Long.MIN_VALUE / 2;

    public DepotBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.DEPOT.get(), pos, state);
    }

    /** Return the 54 slots (also the handler other blocks see on every face). */
    public ItemStacksResourceHandler items() {
        return items;
    }

    /** Return the slots the colony workers fetch from and deposit into (05 section 8.1: outpost storage). */
    @Override
    public net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> colonyItems() {
        return items;
    }

    @Override
    public java.util.Set<dev.autarkia.colony.ColonySensors.Kind> colonyKinds() {
        return java.util.Set.of(dev.autarkia.colony.ColonySensors.Kind.STORAGE);
    }

    /** Return the site name the player gave (empty for the default name). */
    public String siteName() {
        return siteName;
    }

    /** Set the site name (at most 32 characters). */
    public void setSiteName(String name) {
        siteName = name.length() > NAME_LENGTH ? name.substring(0, NAME_LENGTH) : name;
        dirty = true;
        syncNow();
    }

    /** Return the number of occupied slots. */
    public int usedSlots() {
        int n = 0;
        for (int i = 0; i < SLOTS; i++) {
            n += items.getAmountAsInt(i) > 0 ? 1 : 0;
        }
        return n;
    }

    /** Return the item counts of the depot. */
    public Map<ItemResource, Long> stock() {
        Map<ItemResource, Long> out = new LinkedHashMap<>();
        for (int i = 0; i < SLOTS; i++) {
            int n = items.getAmountAsInt(i);
            if (n > 0) {
                out.merge(items.getResource(i), (long) n, Long::sum);
            }
        }
        return out;
    }

    /** Push the stock to the domain stock data now. */
    public void push(ServerLevel sl) {
        DomainStockData data = DomainStockData.get(sl);
        DomainStockData.Outpost o = data.outpostNear(worldPosition);
        String name = !siteName.isEmpty() ? siteName : o == null ? "" : o.name();
        data.push(DomainStockData.Kind.DEPOT, worldPosition, o == null ? null : o.core(), name, stock(), usedSlots(), SLOTS, sl.getGameTime());
        lastPush = sl.getGameTime();
        dirty = false;
    }

    /** Return the input handler of an adjacent sender station on side d, or null. */
    private @Nullable ResourceHandler<ItemResource> sender(ServerLevel sl, Direction d) {
        BlockPos n = worldPosition.relative(d);
        BlockEntity be = sl.getBlockEntity(n);
        if (be instanceof IMultiBlockEntity m && !m.isCenter()) {
            be = sl.getBlockEntity(m.getCenter());
        }
        if (be instanceof CableCarStationBlockEntity s) {
            return s.items();
        }
        if (be instanceof CrateLauncherBlockEntity l) {
            return l.items();
        }
        if (be instanceof CapsuleStationBlockEntity c) {
            return c.items(d.getOpposite());
        }
        if (be instanceof dev.autarkia.logistics.MagRailStationBlockEntity m) {
            return m.items();
        }
        return null;
    }

    private void serveOrders(ServerLevel sl) {
        DomainStockData data = DomainStockData.get(sl);
        for (TransferOrder o : data.openOrdersFrom(worldPosition)) {
            ResourceHandler<ItemResource> out = null;
            for (Direction d : Direction.values()) {
                out = sender(sl, d);
                if (out != null) {
                    break;
                }
            }
            if (out == null) {
                if (o.status() != TransferOrder.Status.WAITING || !o.reason().equals("no_transport")) {
                    data.update(o, 0, TransferOrder.Status.WAITING, "no_transport");
                }
                continue;
            }
            ItemResource res = ItemResource.of(o.item);
            long have = stock().getOrDefault(res, 0L);
            if (have <= 0) {
                data.update(o, 0, o.moved() > 0 ? TransferOrder.Status.PARTIAL : TransferOrder.Status.FAILED, "none_available");
                continue;
            }
            int want = (int) Math.min(o.count - o.moved(), have);
            int moved;
            try (Transaction tx = Transaction.openRoot()) {
                moved = ResourceHandlerUtil.insertStacking(out, res, want, tx);
                if (moved > 0 && items.extract(res, moved, tx) == moved) {
                    tx.commit();
                } else {
                    moved = 0;
                }
            }
            if (moved > 0) {
                boolean done = o.moved() + moved >= o.count;
                data.update(o, moved, done ? TransferOrder.Status.DONE : TransferOrder.Status.MOVING, "");
                if (done) {
                    Autarkia.LOG.info("[sites] transfer order {} done: {} items left {}", o.id, o.count, worldPosition.toShortString());
                }
            }
        }
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        long now = sl.getGameTime();
        if (dirty && now - lastPush >= AutarkiaConfig.STOCK_SYNC_INTERVAL_TICKS.get() || now - lastPush >= HEARTBEAT) {
            push(sl);
        }
        if ((now + worldPosition.asLong()) % 20 == 0) {
            serveOrders(sl);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel sl) {
            for (int i = 0; i < SLOTS; i++) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, items.getResource(i).toStack(items.getAmountAsInt(i)));
            }
            DomainStockData.get(sl).remove(pos);
        }
    }

    @Override
    public String jadeGroup() {
        return "logistics";
    }

    @Override
    public void jadeLines(List<Component> out) {
        out.add(Component.translatable("jade.autarkia.fill", usedSlots(), SLOTS));
        if (level instanceof ServerLevel sl) {
            DomainStockData.Outpost o = DomainStockData.get(sl).outpostNear(worldPosition);
            out.add(o == null ? Component.translatable("jade.autarkia.depot.main") : Component.translatable("jade.autarkia.depot.outpost",
                o.core().getX(), o.core().getY(), o.core().getZ()));
        }
    }

    @Override
    public Component getDisplayName() {
        return siteName.isEmpty() ? getBlockState().getBlock().getName() : Component.literal(siteName);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.DEPOT.get(), id, playerInv, worldPosition, items, new SimpleContainerData(0), GuiLayouts.DEPOT);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        items.serialize(out.child("items"));
        out.putString("siteName", siteName);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("items").ifPresent(items::deserialize);
        siteName = in.getStringOr("siteName", "");
        dirty = true;
    }
}

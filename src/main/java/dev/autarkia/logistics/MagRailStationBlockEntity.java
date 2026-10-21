package dev.autarkia.logistics;

import com.mojang.serialization.Codec;
import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.EnergyBuffer;
import dev.autarkia.energy.GridMember;
import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.registry.Ticking;
import dev.autarkia.storage.CrateBlockEntity;
import dev.autarkia.storage.FallingCrateEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * {@code mag_rail_station} (04 section 1.7, G6): input 27 slots, crate slot (empty crates, stack 16), output 27 slots,
 * 16,000 FE (input 512 FE/t), a name (16 characters), a destination (a station name on the line, or "Next": the next
 * station in the send direction, the way the placer looked; with none that way the next one the other way) and a filter
 * (rule C8: 9 ghost slots, whitelist or blacklist, match components; an empty filter passes everything) that decides
 * what the input accepts. It sends when the input is full (every slot used) or not empty for 200 ticks, with an empty
 * crate and 2,000 FE, when the line is no closed loop, within the caps (2,048 rails, 16 stations, {@code magRailMaxSleds}
 * sleds per line, C11) and the single-track rules allow it: all 27 input slots pack into the crate, which leaves as a
 * {@code falling_crate} sled in mode RAIL ({@link MagRailSled}); while the lock or the spacing holds it back the sled
 * waits in the station (status LOCKED). An arriving sled unloads into the output, its crate into the crate slot.
 */
public class MagRailStationBlockEntity extends AutarkiaBlockEntity implements Ticking, GridMember, JadeInfo, MenuProvider {
    /** Why the station does not send right now (READY when it does). */
    public enum Status {
        READY, WAITING, NO_LINE, LOOP, OVER_CAP, NO_DESTINATION, NO_CRATE, NO_POWER, BUSY, LOCKED;

        /** Return the translation key of the status. */
        public String key() {
            return "state.autarkia.mag_rail." + name().toLowerCase(Locale.ROOT);
        }
    }

    public static final int SLOTS = 27;
    public static final int CRATE_SLOT = 27;
    public static final int OUT = 28;
    public static final int CAPACITY = 16000;
    public static final int MAX_INPUT = 512;
    public static final int SEND_FE = 2000;
    public static final int IDLE_SEND = 200;
    public static final int NAME_LENGTH = 16;
    public static final int CRATE_STACK = 16;
    private static final Map<Level, Set<BlockPos>> LIVE = new WeakHashMap<>();
    private final NonNullList<ItemStack> filter = NonNullList.withSize(9, ItemStack.EMPTY);
    private boolean whitelist = true;
    private boolean matchComponents;
    private final MachineInventory inv = new MachineInventory(SLOTS + 1, SLOTS, s -> true, this::setChanged) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            ItemStack s = resource.toStack(1);
            if (index < SLOTS) {
                return !CrateBlockEntity.isFilledCrate(s) && passes(s);
            }
            return index == CRATE_SLOT && s.is(AutarkiaItems.CRATE.get()) && !CrateBlockEntity.isFilledCrate(s);
        }

        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return index == CRATE_SLOT ? Math.min(CRATE_STACK, super.getCapacity(index, resource)) : super.getCapacity(index, resource);
        }
    };
    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, MAX_INPUT, 0, this::setChanged);
    private final LinkerRef linker = new LinkerRef();
    private String name = "";
    private String destination = "";
    private long inputSince = -1;
    private int target = -1;
    private Status status = Status.NO_LINE;
    private List<String> lineNames = List.of();
    private int lineRails;
    private int lineStations;
    private int inFlight;
    private int lock;
    private int sent;
    private int received;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> energy.stored();
                case 1 -> energy.capacity();
                case 2 -> status.ordinal();
                case 3 -> inFlight;
                case 4 -> lineRails;
                case 5 -> lineStations;
                case 6 -> whitelist ? 1 : 0;
                default -> matchComponents ? 1 : 0;
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 8;
        }
    };

    public MagRailStationBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.MAG_RAIL_STATION.get(), pos, state);
    }

    /** Return the number of loaded stations of the level that hold a packed send back for the lock or the spacing. */
    public static int levelLocked(Level level) {
        int n = 0;
        for (BlockPos p : List.copyOf(LIVE.getOrDefault(level, Set.of()))) {
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof MagRailStationBlockEntity s && !s.isRemoved() && s.status == Status.LOCKED) {
                n++;
            }
        }
        return n;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel sl) {
            LIVE.computeIfAbsent(sl, k -> new HashSet<>()).add(worldPosition.immutable());
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel sl) {
            Set<BlockPos> s = LIVE.get(sl);
            if (s != null) {
                s.remove(worldPosition);
            }
        }
    }

    /** Return the 55 slots: 0 to 26 input, 27 crate slot, 28 to 54 output. */
    public MachineInventory inventory() {
        return inv;
    }

    /** Return the handler other blocks see: insert into the input and the crate slot, extract from the output. */
    public ResourceHandler<ItemResource> items() {
        return inv.automation();
    }

    /** Return the energy buffer. */
    public EnergyBuffer energy() {
        return energy;
    }

    /** Return the menu data: 0 energy, 1 capacity, 2 status, 3 sleds in flight, 4 line rails, 5 line stations, 6 whitelist, 7 components. */
    public ContainerData data() {
        return data;
    }

    /** Return the status of the last tick. */
    public Status status() {
        return status;
    }

    /** Return the station name ("" when unnamed). */
    public String name() {
        return name;
    }

    /** Return the destination name ("" for Next). */
    public String destination() {
        return destination;
    }

    /** Return the names of the other named stations on the line (synced every 20 ticks). */
    public List<String> lineNames() {
        return lineNames;
    }

    /** Return the number of sleds this station sent since it was placed. */
    public int sent() {
        return sent;
    }

    /** Return the number of sleds this station received since it was placed. */
    public int received() {
        return received;
    }

    /** Return the 9 ghost filter stacks (live list). */
    public NonNullList<ItemStack> filter() {
        return filter;
    }

    /** Set ghost slot i to a one-item copy of the stack (empty clears it). */
    public void setGhost(int i, ItemStack stack) {
        filter.set(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        setChanged();
    }

    /** Return true when the stack passes the filter (an empty filter passes everything). */
    public boolean passes(ItemStack stack) {
        boolean any = false;
        boolean listed = false;
        for (ItemStack f : filter) {
            if (f.isEmpty()) {
                continue;
            }
            any = true;
            if (matchComponents ? ItemStack.isSameItemSameComponents(f, stack) : ItemStack.isSameItem(f, stack)) {
                listed = true;
                break;
            }
        }
        return !any || whitelist == listed;
    }

    /** Apply a menu button: 2 whitelist, 3 match components; returns false for other ids. */
    public boolean button(int id) {
        switch (id) {
            case 2 -> whitelist = !whitelist;
            case 3 -> matchComponents = !matchComponents;
            default -> {
                return false;
            }
        }
        setChanged();
        return true;
    }

    /** Set name and destination (each stripped, at most 16 characters) and remember the player who set them up. */
    public void setSettings(String newName, String newDestination, ServerPlayer by) {
        name = clip(newName);
        destination = clip(newDestination);
        linker.set(by);
        syncNow();
    }

    /** Remember the player who receives {@code logistics/float_on} for the sleds of this station. */
    public void setLinker(ServerPlayer p) {
        linker.set(p);
    }

    private static String clip(String s) {
        String t = s.strip();
        return t.length() > NAME_LENGTH ? t.substring(0, NAME_LENGTH) : t;
    }

    private boolean inputEmpty() {
        for (int i = 0; i < SLOTS; i++) {
            if (!inv.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean inputFull() {
        for (int i = 0; i < SLOTS; i++) {
            if (inv.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        long now = sl.getGameTime();
        if (inputEmpty()) {
            inputSince = -1;
        } else if (inputSince < 0) {
            inputSince = now;
        }
        MagRailLine line = MagRailLine.of(sl, worldPosition);
        if (Math.floorMod(now + worldPosition.asLong(), 20L) == 0) {
            refresh(sl, line);
        }
        Status before = status;
        status = evaluate(sl, line, now);
        if (status == Status.READY) {
            send(sl, line);
        }
        if (status != before) {
            syncNow();
        }
    }

    private Status evaluate(ServerLevel sl, MagRailLine line, long now) {
        if (line == null || line.size() < 2) {
            return Status.NO_LINE;
        }
        if (line.loop) {
            return Status.LOOP;
        }
        if (line.overCap) {
            return Status.OVER_CAP;
        }
        if (inputEmpty() || !inputFull() && now - inputSince < IDLE_SEND) {
            return Status.WAITING;
        }
        int s = line.indexOf(worldPosition);
        target = target(sl, line, s);
        if (target < 0) {
            return Status.NO_DESTINATION;
        }
        if (inv.get(CRATE_SLOT).isEmpty()) {
            return Status.NO_CRATE;
        }
        if (energy.stored() < SEND_FE) {
            return Status.NO_POWER;
        }
        if (line.sledsOn(sl).size() >= AutarkiaConfig.MAG_RAIL_MAX_SLEDS.get()) {
            return Status.BUSY;
        }
        return line.startHold(sl, s, target) == MagRailLine.Hold.NONE ? Status.READY : Status.LOCKED;
    }

    private int sendDirection(MagRailLine line, int s) {
        BlockPos back = worldPosition.relative(MachineBlock.front(getBlockState()).getOpposite());
        if (s + 1 < line.size() && sameColumn(line.cells.get(s + 1), back)) {
            return 1;
        }
        if (s > 0 && sameColumn(line.cells.get(s - 1), back)) {
            return -1;
        }
        return s + 1 < line.size() ? 1 : -1;
    }

    private static boolean sameColumn(BlockPos a, BlockPos b) {
        return a.getX() == b.getX() && a.getZ() == b.getZ();
    }

    private static int nextStation(MagRailLine line, int s, int dir) {
        for (int i = s + dir; i >= 0 && i < line.size(); i += dir) {
            if (line.isStation(i)) {
                return i;
            }
        }
        return -1;
    }

    private int target(ServerLevel sl, MagRailLine line, int s) {
        if (destination.isEmpty()) {
            int dir = sendDirection(line, s);
            int next = nextStation(line, s, dir);
            return next >= 0 ? next : nextStation(line, s, -dir);
        }
        int best = -1;
        for (int i : line.stations()) {
            BlockPos p = line.cells.get(i);
            if (i != s && sl.isLoaded(p) && sl.getBlockEntity(p) instanceof MagRailStationBlockEntity o && o.name.equals(destination)
                && (best < 0 || Math.abs(i - s) < Math.abs(best - s))) {
                best = i;
            }
        }
        return best;
    }

    private void send(ServerLevel sl, MagRailLine line) {
        int s = line.indexOf(worldPosition);
        List<ItemStack> contents = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            contents.add(inv.get(i).copy());
            inv.put(i, ItemStack.EMPTY);
        }
        ItemStack crate = inv.get(CRATE_SLOT).copyWithCount(1);
        inv.shrink(CRATE_SLOT, 1);
        crate.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        energy.drain(SEND_FE);
        MagRailSled.Route route = MagRailSled.Route.of(line.path(s, target), line.railsBetween(s, target));
        ServerPlayer p = linker.resolve(sl);
        if (p != null) {
            route.linker.set(p);
        }
        FallingCrateEntity.sled(sl, crate, route);
        inputSince = -1;
        sent++;
        Autarkia.LOG.info("[mag_rail] {} sent a sled to {} over {} rails", worldPosition.toShortString(), line.cells.get(target).toShortString(),
            route.rails());
        setChanged();
    }

    /** Unload an arriving crate: contents into the output, the empty crate into the crate slot; false (nothing moved) without room. */
    public boolean receive(ItemStack crate) {
        List<ItemStack> contents = new ArrayList<>();
        ItemContainerContents c = crate.get(DataComponents.CONTAINER);
        if (c != null) {
            c.nonEmptyItemCopyStream().forEach(contents::add);
        }
        ItemStack shell = crate.copyWithCount(1);
        shell.remove(DataComponents.CONTAINER);
        ItemStack slot = inv.get(CRATE_SLOT);
        boolean crateFits = slot.isEmpty() || ItemStack.isSameItemSameComponents(slot, shell) && slot.getCount() < CRATE_STACK;
        if (!crateFits || !inv.fitsOutputs(contents)) {
            return false;
        }
        inv.addOutputs(contents);
        inv.put(CRATE_SLOT, slot.isEmpty() ? shell : slot.copyWithCount(slot.getCount() + 1));
        received++;
        syncNow();
        return true;
    }

    private void refresh(ServerLevel sl, MagRailLine line) {
        List<String> names = new ArrayList<>();
        int rails = 0;
        int stations = 0;
        int flying = 0;
        int lk = 0;
        if (line != null && !line.loop) {
            rails = line.rails;
            stations = line.stations().size();
            int s = line.indexOf(worldPosition);
            for (int i : line.stations()) {
                BlockPos p = line.cells.get(i);
                if (i != s && sl.isLoaded(p) && sl.getBlockEntity(p) instanceof MagRailStationBlockEntity o && !o.name.isEmpty() && !names.contains(o.name)) {
                    names.add(o.name);
                }
            }
            for (MagRailLine.SledView v : line.sledsOn(sl)) {
                if (v.arrived()) {
                    continue;
                }
                flying++;
                lk |= (s - v.canon()) * v.dir() > 0 ? 1 : (v.canon() - s) * v.dir() > 0 ? 2 : 0;
            }
        }
        boolean changed = !names.equals(lineNames) || rails != lineRails || stations != lineStations || flying != inFlight || lk != lock;
        lineNames = List.copyOf(names);
        lineRails = rails;
        lineStations = stations;
        inFlight = flying;
        lock = lk;
        if (changed) {
            syncNow();
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel) {
            for (int i = 0; i < inv.size(); i++) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, inv.get(i).copy());
            }
        }
    }

    @Override
    public Role gridRole() {
        return Role.CONSUMER;
    }

    @Override
    public EnergyBuffer gridBuffer() {
        return energy;
    }

    @Override
    public int gridMaxIn() {
        return energy.room() > 0 ? MAX_INPUT : 0;
    }

    @Override
    public int gridMaxOut() {
        return 0;
    }

    @Override
    public int workDraw() {
        return inputEmpty() ? 0 : SEND_FE / IDLE_SEND;
    }

    @Override
    public boolean gridValid() {
        return !isRemoved();
    }

    @Override
    public String jadeGroup() {
        return "logistics";
    }

    @Override
    public void jadeLines(List<Component> out) {
        out.add(Component.translatable("jade.autarkia.mag_rail.name", name.isEmpty() ? Component.translatable("jade.autarkia.mag_rail.unnamed")
            : Component.literal(name)));
        out.add(Component.translatable("jade.autarkia.mag_rail.destination", destination.isEmpty() ? Component.translatable("screen.autarkia.mag_rail.next")
            : Component.literal(destination)));
        out.add(Component.translatable("jade.autarkia.mag_rail.in_flight", inFlight));
        String lk = switch (lock) {
            case 1 -> "inbound";
            case 2 -> "outbound";
            case 3 -> "both";
            default -> "none";
        };
        out.add(Component.translatable("jade.autarkia.mag_rail.lock", Component.translatable("jade.autarkia.mag_rail.lock." + lk)));
        if (status != Status.READY && status != Status.WAITING) {
            out.add(Component.translatable(status.key()));
        }
    }

    @Override
    public float jadeProgress() {
        return energy.fillRatio();
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new MagRailStationMenu(id, playerInv, this);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        inv.serialize(out.child("inv"));
        energy.serialize(out.child("energy"));
        linker.save(out);
        out.putString("name", name);
        out.putString("destination", destination);
        out.store("filter", ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(filter));
        out.putBoolean("whitelist", whitelist);
        out.putBoolean("matchComponents", matchComponents);
        out.putLong("inputSince", inputSince);
        out.putInt("status", status.ordinal());
        out.store("lineNames", Codec.STRING.listOf(), lineNames);
        out.putInt("lineRails", lineRails);
        out.putInt("lineStations", lineStations);
        out.putInt("inFlight", inFlight);
        out.putInt("lock", lock);
        out.putInt("sent", sent);
        out.putInt("received", received);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("inv").ifPresent(inv::deserialize);
        in.child("energy").ifPresent(energy::deserialize);
        linker.load(in);
        name = in.getStringOr("name", "");
        destination = in.getStringOr("destination", "");
        List<ItemStack> f = in.read("filter", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < filter.size(); i++) {
            filter.set(i, i < f.size() ? f.get(i) : ItemStack.EMPTY);
        }
        whitelist = in.getBooleanOr("whitelist", true);
        matchComponents = in.getBooleanOr("matchComponents", false);
        inputSince = in.getLongOr("inputSince", -1);
        status = Status.values()[Math.clamp(in.getIntOr("status", 0), 0, Status.values().length - 1)];
        lineNames = List.copyOf(in.read("lineNames", Codec.STRING.listOf()).orElse(List.of()));
        lineRails = in.getIntOr("lineRails", 0);
        lineStations = in.getIntOr("lineStations", 0);
        inFlight = in.getIntOr("inFlight", 0);
        lock = in.getIntOr("lock", 0);
        sent = in.getIntOr("sent", 0);
        received = in.getIntOr("received", 0);
    }
}

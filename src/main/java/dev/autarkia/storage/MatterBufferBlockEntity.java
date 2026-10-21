package dev.autarkia.storage;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaAdvancements;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.Ticking;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * {@code matter_buffer} (04 section 2.7), a 2x2x2 big block (rules B1 to B7, state in the center) with the GeckoLib loop
 * {@code swirl}. It stores items with their identity (any count per item kind) and free matter units in one capacity
 * ({@code matterBufferCapacity}): an insert is accepted only while used MU (count x MU per item at the current domain
 * tier, G19) plus free MU stays within it. The item handler on every face of every position inserts accepted items
 * ({@link MatterValues#accepted}) and extracts stored items. A dissolve (GUI, irreversible) turns all of one stored item
 * into free MU at 100%. With auto-dissolve on (the default), every 20 ticks while the free MU of the domain (this
 * buffer's own outside a domain) is below {@code matterReserve}, whole stored entries of {@code #autarkia:dissolvable}
 * dissolve until the reserve is reached. Every 100 ticks inside a domain it grants {@code logistics/matter_of_fact} to
 * the players within 32 blocks and the online owner when used + free MU of the domain reach 100,000. Breaking it drops
 * the stored items (free MU is lost).
 */
public class MatterBufferBlockEntity extends AutarkiaBlockEntity implements Ticking, JadeInfo, MenuProvider, GeoBlockEntity,
    dev.autarkia.colony.ColonyStorage, dev.autarkia.colony.ColonySensors.Member {
    public static final long MATTER_OF_FACT = 100_000;
    private static final RawAnimation SWIRL = RawAnimation.begin().thenLoop("animation.matter_buffer.swirl");

    /** One stored item kind and its count. */
    public static final class Entry {
        private final ItemResource resource;
        private long count;

        Entry(ItemResource resource, long count) {
            this.resource = resource;
            this.count = count;
        }

        /** Return the stored item kind (with its components). */
        public ItemResource resource() {
            return resource;
        }

        /** Return the stored count. */
        public long count() {
            return count;
        }
    }

    private record Saved(ItemResource item, long count) {
        static final Codec<Saved> CODEC = RecordCodecBuilder.create(i -> i.group(ItemResource.CODEC.fieldOf("item").forGetter(Saved::item),
            Codec.LONG.fieldOf("count").forGetter(Saved::count)).apply(i, Saved::new));
    }

    private record Snapshot(List<Entry> entries, long free) {
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final List<Entry> entries = new ArrayList<>();
    private final Handler handler = new Handler();
    private long free;
    private boolean auto = true;
    private int tier = 5;
    private long usedCache;
    private boolean usedDirty = true;
    private int usedVersion;
    private int usedMul;
    private int usedTier;

    public MatterBufferBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.MATTER_BUFFER.get(), pos, state);
    }

    /** Return the capacity in MU (config {@code matterBufferCapacity}). */
    public long capacity() {
        return AutarkiaConfig.MATTER_BUFFER_CAPACITY.get();
    }

    /** Return the MU of the stored items at the current domain tier. */
    public long used() {
        int version = MatterValues.version();
        int mul = MatterValues.rawMultiplier(tier);
        if (usedDirty || version != usedVersion || mul != usedMul || tier != usedTier) {
            long u = 0;
            for (Entry e : entries) {
                u += e.count * mu(e.resource);
            }
            usedCache = u;
            usedDirty = false;
            usedVersion = version;
            usedMul = mul;
            usedTier = tier;
        }
        return usedCache;
    }

    /** Return the free MU. */
    public long free() {
        return free;
    }

    /** Return the domain tier the MU are counted at (5 outside a domain). */
    public int tier() {
        return tier;
    }

    /** Return true while auto-dissolve is on. */
    public boolean autoDissolve() {
        return auto;
    }

    /** Switch auto-dissolve on or off. */
    public void setAutoDissolve(boolean on) {
        auto = on;
        contentsChanged();
    }

    /** Return the stored entries (read-only view, in insertion order). */
    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    /** Return the stored count of an item kind. */
    public long count(ItemResource res) {
        Entry e = find(res);
        return e == null ? 0 : e.count;
    }

    /** Return the item handler every face exposes. */
    public ResourceHandler<ItemResource> items() {
        return handler;
    }

    /** Return the stored items as a colony storage: workers fetch from it and deposit into it (06 section 3.6). */
    @Override
    public ResourceHandler<ItemResource> colonyItems() {
        return handler;
    }

    @Override
    public java.util.Set<dev.autarkia.colony.ColonySensors.Kind> colonyKinds() {
        return java.util.Set.of(dev.autarkia.colony.ColonySensors.Kind.STORAGE);
    }

    private long mu(ItemResource res) {
        return MatterValues.of(res.toStack(1), tier);
    }

    private long room(ItemResource res) {
        long each = mu(res);
        long spare = capacity() - used() - free;
        return each <= 0 || spare <= 0 ? 0 : spare / each;
    }

    private @Nullable Entry find(ItemResource res) {
        for (Entry e : entries) {
            if (e.resource.equals(res)) {
                return e;
            }
        }
        return null;
    }

    private void add(ItemResource res, long n) {
        Entry e = find(res);
        if (e == null) {
            entries.add(new Entry(res, n));
        } else {
            e.count += n;
        }
        usedDirty = true;
    }

    private long remove(ItemResource res, long n) {
        Entry e = find(res);
        if (e == null) {
            return 0;
        }
        long take = Math.min(n, e.count);
        e.count -= take;
        if (e.count <= 0) {
            entries.remove(e);
        }
        usedDirty = true;
        return take;
    }

    private void contentsChanged() {
        setChanged();
        if (level instanceof ServerLevel) {
            syncThrottled(10);
        }
    }

    /** Add free MU; returns the MU accepted (bounded by capacity - used - free). */
    public long addFree(long mu) {
        long take = Math.max(0, Math.min(mu, capacity() - used() - free));
        if (take > 0) {
            free += take;
            contentsChanged();
        }
        return take;
    }

    /** Take free MU; returns false and takes nothing when the buffer holds less. */
    public boolean takeFree(long mu) {
        if (mu < 0 || mu > free) {
            return false;
        }
        free -= mu;
        contentsChanged();
        return true;
    }

    /** Insert items (simulate or not); returns the count accepted. */
    public int insertItems(ItemStack stack, boolean simulate) {
        if (!MatterValues.accepted(stack, tier)) {
            return 0;
        }
        ItemResource res = ItemResource.of(stack);
        int n = (int) Math.min(stack.getCount(), room(res));
        if (n > 0 && !simulate) {
            add(res, n);
            contentsChanged();
        }
        return n;
    }

    /** Take up to one stack of an item kind out; returns the stack (empty when none is stored). */
    public ItemStack extractStack(ItemResource res) {
        long n = remove(res, res.getMaxStackSize());
        if (n > 0) {
            contentsChanged();
        }
        return n > 0 ? res.toStack((int) n) : ItemStack.EMPTY;
    }

    /** Dissolve all stored items of one kind into free MU at 100% (irreversible); returns the MU gained. */
    public long dissolve(ItemResource res) {
        Entry e = find(res);
        if (e == null) {
            return 0;
        }
        long gain = e.count * mu(res);
        long count = e.count;
        entries.remove(e);
        usedDirty = true;
        free += gain;
        contentsChanged();
        if (level instanceof ServerLevel sl) {
            Vec3 c = Vec3.atCenterOf(worldPosition).add(0, 0.5, 0);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 40, 0.6, 0.6, 0.6, 0.05);
            sl.playSound(null, worldPosition, AutarkiaSounds.of("matter_dissolve"), SoundSource.BLOCKS, 1.0f, 1.0f);
            Autarkia.LOG.info("[matter] buffer {} dissolved {} x {} into {} MU", worldPosition.toShortString(), count, res, gain);
        }
        return gain;
    }

    /** Return the free MU of the domain that holds this buffer, or this buffer's free MU outside a domain. */
    public long domainFree() {
        if (level instanceof ServerLevel sl && Colonies.server() != null) {
            Colony c = Colonies.at(sl, worldPosition);
            if (c != null) {
                return MatterBank.available(sl, c);
            }
        }
        return free;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel sl && isCenter()) {
            MatterBank.track(sl, worldPosition, true);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel sl) {
            MatterBank.track(sl, worldPosition, false);
        }
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        long phase = Math.floorMod(sl.getGameTime() + worldPosition.asLong(), 100L);
        if (phase % 20 == 0) {
            refreshTier(sl);
            if (auto) {
                dissolveToReserve();
            }
        }
        if (phase == 0) {
            matterOfFact(sl);
        }
        flushSync(10);
    }

    private void refreshTier(ServerLevel sl) {
        Colony c = Colonies.server() == null ? null : Colonies.at(sl, worldPosition);
        int t = c == null ? 5 : c.tier;
        if (t != tier) {
            tier = t;
            usedDirty = true;
            contentsChanged();
        }
    }

    private void dissolveToReserve() {
        long reserve = AutarkiaConfig.MATTER_RESERVE.get();
        for (Entry e : List.copyOf(entries)) {
            if (domainFree() >= reserve) {
                return;
            }
            if (e.resource.toStack(1).is(MatterValues.DISSOLVABLE)) {
                dissolve(e.resource);
            }
        }
    }

    private void matterOfFact(ServerLevel sl) {
        Colony c = Colonies.server() == null ? null : Colonies.at(sl, worldPosition);
        if (c == null) {
            return;
        }
        MatterBank.Totals t = MatterBank.totals(sl, c);
        if (t.used() + t.free() < MATTER_OF_FACT) {
            return;
        }
        AutarkiaAdvancements.awardNear(sl, Vec3.atCenterOf(worldPosition), "logistics/matter_of_fact");
        ServerPlayer owner = c.owner == null ? null : sl.getServer().getPlayerList().getPlayer(c.owner);
        if (owner != null) {
            AutarkiaAdvancements.award(owner, "logistics/matter_of_fact");
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel && isCenter()) {
            for (Entry e : entries) {
                long left = e.count;
                while (left > 0) {
                    int n = (int) Math.min(left, e.resource.getMaxStackSize());
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, e.resource.toStack(n));
                    left -= n;
                }
            }
            entries.clear();
        }
    }

    @Override
    public String jadeGroup() {
        return "logistics";
    }

    @Override
    public void jadeLines(List<Component> out) {
        out.add(Component.translatable("jade.autarkia.matter_buffer.used", fmt(used()), fmt(capacity())));
        out.add(Component.translatable("jade.autarkia.matter_buffer.free", fmt(free)));
    }

    @Override
    public float jadeProgress() {
        return (float) Math.min(1.0, (used() + free) / (double) Math.max(1, capacity()));
    }

    /** Format a MU number with thousands separators. */
    public static String fmt(long n) {
        return String.format(Locale.ROOT, "%,d", n);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new MatterBufferMenu(id, playerInv, this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<MatterBufferBlockEntity>("main", 0, test -> test.setAndContinue(SWIRL)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("entries", Saved.CODEC.listOf(), entries.stream().map(e -> new Saved(e.resource, e.count)).toList());
        out.putLong("free", free);
        out.putBoolean("auto", auto);
        out.putInt("tier", tier);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        entries.clear();
        for (Saved s : in.read("entries", Saved.CODEC.listOf()).orElse(List.of())) {
            if (!s.item().isEmpty() && s.count() > 0) {
                entries.add(new Entry(s.item(), s.count()));
            }
        }
        free = in.getLongOr("free", 0);
        auto = in.getBooleanOr("auto", true);
        tier = in.getIntOr("tier", 5);
        usedDirty = true;
    }

    /** The handler of every face: a virtual slot per stored kind plus one empty slot; inserts check value and room. */
    private final class Handler extends SnapshotJournal<Snapshot> implements ResourceHandler<ItemResource> {
        @Override
        public int size() {
            return entries.size() + 1;
        }

        @Override
        public ItemResource getResource(int index) {
            return index < entries.size() ? entries.get(index).resource : ItemResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            return index < entries.size() ? entries.get(index).count : 0;
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            if (resource.isEmpty() || !isValid(index, resource)) {
                return 0;
            }
            return count(resource) + room(resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            if (resource.isEmpty() || !MatterValues.accepted(resource.toStack(1), tier)) {
                return false;
            }
            return index == entries.size() ? find(resource) == null : index < entries.size() && entries.get(index).resource.equals(resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
            return isValid(index, resource) ? insert(resource, amount, tx) : 0;
        }

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext tx) {
            if (amount <= 0 || resource.isEmpty() || !MatterValues.accepted(resource.toStack(1), tier)) {
                return 0;
            }
            int n = (int) Math.min(amount, room(resource));
            if (n > 0) {
                updateSnapshots(tx);
                add(resource, n);
            }
            return n;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
            return index < entries.size() && entries.get(index).resource.equals(resource) ? extract(resource, amount, tx) : 0;
        }

        @Override
        public int extract(ItemResource resource, int amount, TransactionContext tx) {
            Entry e = find(resource);
            if (e == null || amount <= 0) {
                return 0;
            }
            int n = (int) Math.min(amount, e.count);
            updateSnapshots(tx);
            remove(resource, n);
            return n;
        }

        @Override
        protected Snapshot createSnapshot() {
            return new Snapshot(entries.stream().map(e -> new Entry(e.resource, e.count)).toList(), free);
        }

        @Override
        protected void revertToSnapshot(Snapshot s) {
            entries.clear();
            s.entries().forEach(e -> entries.add(new Entry(e.resource, e.count)));
            free = s.free();
            usedDirty = true;
        }

        @Override
        protected void onRootCommit(Snapshot original) {
            contentsChanged();
        }
    }
}

package dev.autarkia.colony;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.EnergyBuffer;
import dev.autarkia.energy.GridMember;
import dev.autarkia.processing.MachineInput;
import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.AutarkiaRecipes;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import dev.autarkia.worker.NaniteSwarmEntity;
import dev.autarkia.worker.WorkerAssemblyRecipe;
import dev.autarkia.worker.WorkerType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Nanite Hive (05 section 8.3, G5, G23): a 2x2x2 grid consumer with a 200,000 FE buffer (8,000 FE/t input) and one
 * {@code nanite_paste} slot. While its claim runs and it has fewer swarms out than the claim takes (1, 2 at T6) it
 * assembles one from its {@code worker_assembly} recipe (4 paste, 200 ticks at 100 FE/t) when the worker cap and the
 * swarm sub-cap have room, and releases it above itself. It tethers its swarms within 64 blocks at up to 4,000 FE/t
 * each, plays {@code nanite_swarm_loop} every 40 ticks while the claim runs, lists marker pairs for its menu and shows
 * the claim in Jade. Breaking it ends its claim and calls its swarms home.
 */
public class NaniteHiveBlockEntity extends AutarkiaBlockEntity implements Ticking, GridMember, JadeInfo, MenuProvider, GeoBlockEntity {
    public static final int CAPACITY = 200000;
    public static final int INPUT = 8000;
    public static final Identifier ID = Autarkia.id("nanite_hive");
    /** Menu data: 10 fixed values, then 7 per marker pair row (a and b relative to the hive, top y relative to the hive), then 2. */
    public static final int DATA = 10 + 7 * NaniteClaims.MAX_PAIRS + 2;
    public static final int FLAG_NEEDS_PASTE = 1;
    public static final int FLAG_CAP_FULL = 2;
    public static final int FLAG_NO_POWER = 4;
    public static final int FLAG_STOPPED = 8;
    public static final int FLAG_NO_BUFFER = 16;
    public static final int FLAG_NO_RECIPE = 32;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.nanite_hive.idle");
    private static final RawAnimation RELEASE = RawAnimation.begin().thenLoop("animation.nanite_hive.release");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, INPUT, 0, this::setChanged);
    private final MachineInventory paste = new MachineInventory(1, 0, s -> s.is(AutarkiaItems.NANITE_PASTE.get()), this::setChanged);
    private @Nullable UUID colonyId;
    private int assembly;
    private int assemblyTime;
    private int assemblyDraw;
    private int flags;
    private int lastTether;
    private boolean releasing;
    private boolean noBuffer;
    private List<NaniteClaims.Pair> pairs = List.of();
    private long pairsAt = Long.MIN_VALUE / 2;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return value(i);
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return DATA;
        }
    };

    public NaniteHiveBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.NANITE_HIVE.get(), pos, state);
    }

    /** Return the energy buffer (grid and capability). */
    public EnergyBuffer energy() {
        return energy;
    }

    /** Return the paste slot. */
    public MachineInventory inventory() {
        return paste;
    }

    /** Return the colony whose domain holds the hive (synced to clients for the menu), or null. */
    public @Nullable UUID colonyId() {
        return colonyId;
    }

    /** Return the status flags of the last tick (FLAG_*). */
    public int flags() {
        return flags | (NaniteClaims.running() ? 0 : FLAG_STOPPED);
    }

    /** Return the swarm assembly progress in ticks. */
    public int assembly() {
        return assembly;
    }

    /** Put returned paste into the slot; returns what did not fit. */
    public ItemStack returnPaste(ItemStack stack) {
        ItemStack cur = paste.get(0);
        if (!cur.isEmpty() && !ItemStack.isSameItemSameComponents(cur, stack)) {
            return stack;
        }
        int room = stack.getMaxStackSize() - cur.getCount();
        int add = Math.min(room, stack.getCount());
        if (add > 0) {
            paste.put(0, stack.copyWithCount(cur.getCount() + add));
        }
        return stack.copyWithCount(stack.getCount() - add);
    }

    private @Nullable Colony colony() {
        return colonyId == null || Colonies.server() == null ? null : Colonies.registry().get(colonyId);
    }

    /** Return the marker pairs the menu lists, refreshed at most every 20 ticks. */
    public List<NaniteClaims.Pair> pairs() {
        Colony c = colony();
        if (!(level instanceof ServerLevel sl) || c == null) {
            return List.of();
        }
        if (sl.getGameTime() - pairsAt >= 20) {
            pairsAt = sl.getGameTime();
            pairs = NaniteClaims.pairs(sl, c, worldPosition);
        }
        return pairs;
    }

    /** Return the point above the hive footprint center where swarms are released and dissolve. */
    public Vec3 releasePoint() {
        Direction f = MachineBlock.front(getBlockState());
        Vec3 off = switch (f) {
            case SOUTH -> new Vec3(-0.5, 0, -0.5);
            case EAST -> new Vec3(-0.5, 0, 0.5);
            case WEST -> new Vec3(0.5, 0, -0.5);
            default -> new Vec3(0.5, 0, 0.5);
        };
        return Vec3.atBottomCenterOf(worldPosition).add(off).add(0, 2.2, 0);
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        long now = sl.getGameTime();
        if (now % 20 == 0) {
            Colony at = Colonies.at(sl, worldPosition);
            UUID id = at == null ? null : at.id;
            if (!java.util.Objects.equals(id, colonyId)) {
                colonyId = id;
                syncNow();
            }
        }
        Colony c = colony();
        lastTether = c == null ? 0 : tether(sl, c);
        NaniteClaim cl = c == null ? null : NaniteClaims.hiveClaim(c, worldPosition);
        boolean active = cl != null && cl.state != NaniteClaim.State.PAUSED && NaniteClaims.running();
        flags = 0;
        if (active) {
            if (now % 40 == 0) {
                sl.playSound(null, worldPosition, AutarkiaSounds.of("nanite_swarm_loop"), SoundSource.BLOCKS, 1f, 1f);
            }
            if (now % 20 == 0) {
                noBuffer = NaniteEngine.linkedBuffer(sl, c, worldPosition) == null;
            }
            if (noBuffer) {
                flags |= FLAG_NO_BUFFER;
            }
            if (cl.swarms.size() < (c.tier >= 6 ? 2 : 1)) {
                assemble(sl, c, cl);
            } else {
                assembly = 0;
            }
        } else {
            assembly = 0;
        }
        boolean rel = active && (assembly > 0 || !cl.swarms.isEmpty());
        if (rel != releasing) {
            releasing = rel;
            syncNow();
        }
    }

    private int tether(ServerLevel sl, Colony c) {
        int moved = 0;
        for (NaniteClaim cl : c.claims) {
            if (!cl.hivePos.equals(worldPosition)) {
                continue;
            }
            for (UUID id : cl.swarms) {
                if (sl.getEntity(id) instanceof NaniteSwarmEntity s && s.tethered()) {
                    int give = Math.min(NaniteSwarmEntity.TETHER_RATE, Math.min(s.room(), energy.stored()));
                    if (give > 0) {
                        energy.drain(s.charge(give));
                        moved += give;
                    }
                }
            }
        }
        return moved;
    }

    private Optional<WorkerAssemblyRecipe> recipe(ServerLevel sl) {
        return sl.recipeAccess().recipeMap().byType(AutarkiaRecipes.WORKER_ASSEMBLY.get()).stream().map(RecipeHolder::value)
            .filter(r -> r.forMachine(ID)).findFirst();
    }

    private void assemble(ServerLevel sl, Colony c, NaniteClaim cl) {
        Optional<WorkerAssemblyRecipe> found = recipe(sl);
        if (found.isEmpty()) {
            if (sl.getGameTime() % 1200 == 0) {
                Autarkia.LOG.error("[nanite] hive {} cannot assemble a swarm: no worker_assembly recipe names machine {} (data pack removed it?)",
                    worldPosition.toShortString(), ID);
            }
            flags |= FLAG_NO_RECIPE;
            return;
        }
        WorkerAssemblyRecipe r = found.get();
        assemblyTime = r.time();
        assemblyDraw = r.fePerTick();
        WorkerType type = WorkerType.NANITE_SWARM;
        if (c.tier < r.minTier() || c.workerCost() + type.capCost() > ColonyTiers.workerCap(c.tier) || c.workerCount(type.id()) >= type.subCap(c.tier)) {
            flags |= FLAG_CAP_FULL;
            return;
        }
        if (!r.matches(new MachineInput(List.of(ItemStack.EMPTY, paste.get(0))), sl)) {
            flags |= FLAG_NEEDS_PASTE;
            return;
        }
        if (assembly >= assemblyTime) {
            for (var part : r.parts()) {
                paste.shrink(0, part.count());
            }
            Vec3 at = releasePoint();
            NaniteSwarmEntity s = NaniteSwarmEntity.create(sl, at, worldPosition, c.id, cl.id);
            sl.addFreshEntity(s);
            cl.swarms.add(s.getUUID());
            c.workers.put(s.getUUID(), new Colony.WorkerEntry(s.getUUID(), type.id(), type.capCost(), s.chunkPosition().pack(), sl.getGameTime()));
            assembly = 0;
            ColonyEvents.worker(c, "assembled:nanite_swarm");
            Colonies.changed();
            Autarkia.LOG.info("[nanite] hive {} released swarm {} for claim {}", worldPosition.toShortString(), s.getUUID().toString().substring(0, 8),
                cl.id.toString().substring(0, 8));
            return;
        }
        if (energy.stored() >= r.fePerTick()) {
            energy.drain(r.fePerTick());
            assembly++;
            cl.state = NaniteClaim.State.RUN;
        } else {
            flags |= FLAG_NO_POWER;
            cl.state = NaniteClaim.State.WAIT_FE;
        }
    }

    private int value(int i) {
        Colony c = colony();
        NaniteClaim cl = c == null ? null : NaniteClaims.hiveClaim(c, worldPosition);
        if (cl == null && c != null) {
            NaniteClaim last = NaniteClaims.lastClaim(c, worldPosition);
            cl = last != null && last.state == NaniteClaim.State.DONE ? last : null;
        }
        if (i >= 10 && i < 10 + 7 * NaniteClaims.MAX_PAIRS) {
            List<NaniteClaims.Pair> list = pairs();
            int row = (i - 10) / 7;
            if (row >= list.size()) {
                return 0;
            }
            NaniteClaims.Pair p = list.get(row);
            return switch ((i - 10) % 7) {
                case 0 -> p.a().getX() - worldPosition.getX();
                case 1 -> p.a().getY() - worldPosition.getY();
                case 2 -> p.a().getZ() - worldPosition.getZ();
                case 3 -> p.b().getX() - worldPosition.getX();
                case 4 -> p.b().getY() - worldPosition.getY();
                case 5 -> p.b().getZ() - worldPosition.getZ();
                default -> p.yMax() - worldPosition.getY();
            };
        }
        return switch (i) {
            case 0 -> cl == null ? 0 : NaniteClaims.shown(cl).ordinal() + 1;
            case 1 -> cl == null ? 0 : cl.percent();
            case 2 -> cl == null ? 0 : Math.min(cl.layer(), cl.layers());
            case 3 -> cl == null ? 0 : cl.layers();
            case 4 -> energy.stored() / 100;
            case 5 -> energy.capacity() / 100;
            case 6 -> cl == null ? 0 : cl.swarms.size();
            case 7 -> assemblyTime == 0 ? 0 : assembly * 100 / assemblyTime;
            case 8 -> flags();
            case 9 -> pairs().size();
            case 10 + 7 * NaniteClaims.MAX_PAIRS -> AutarkiaConfig.NANITE_MAX_CLAIM_Y.get();
            default -> cl == null ? 0 : cl.refY - cl.min.getY();
        };
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (isCenter() && level instanceof ServerLevel sl) {
            Containers.dropItemStack(sl, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, paste.get(0).copy());
            NaniteClaims.hiveRemoved(sl, pos);
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
    public int gridMaxOut() {
        return 0;
    }

    @Override
    public int gridMaxIn() {
        return Math.min(INPUT, energy.room());
    }

    @Override
    public int workDraw() {
        return assembly > 0 ? assemblyDraw : lastTether;
    }

    @Override
    public boolean gridValid() {
        return !isRemoved();
    }

    @Override
    public String jadeGroup() {
        return "colony";
    }

    @Override
    public void jadeLines(List<Component> out) {
        Colony c = colony();
        NaniteClaim cl = c == null ? null : NaniteClaims.hiveClaim(c, worldPosition);
        if (cl == null) {
            out.add(Component.translatable("jade.autarkia.nanite_hive.idle"));
        } else {
            out.add(Component.translatable("jade.autarkia.nanite_hive.claim", cl.percent(), Math.min(cl.layer(), cl.layers()), cl.layers()));
            out.add(Component.translatable("jade.autarkia.nanite_hive.state",
                Component.translatable("nanite.autarkia.state." + NaniteClaims.shown(cl).getSerializedName())));
        }
        out.add(Component.translatable("jade.autarkia.buffer", energy.stored(), energy.capacity()));
    }

    @Override
    public float jadeProgress() {
        Colony c = colony();
        NaniteClaim cl = c == null ? null : NaniteClaims.hiveClaim(c, worldPosition);
        return cl == null ? -1 : cl.percent() / 100f;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("menu.autarkia.nanite_hive");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SlotMenu(AutarkiaMenus.NANITE_HIVE.get(), id, inv, worldPosition, paste, data, GuiLayouts.NANITE_HIVE);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<NaniteHiveBlockEntity>("main", 6,
            test -> test.setAndContinue(test.animatable().releasing ? RELEASE : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        energy.serialize(out.child("energy"));
        paste.serialize(out.child("items"));
        if (colonyId != null) {
            out.store("colony", UUIDUtil.CODEC, colonyId);
        }
        out.putInt("assembly", assembly);
        out.putBoolean("releasing", releasing);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(energy::deserialize);
        in.child("items").ifPresent(paste::deserialize);
        colonyId = in.read("colony", UUIDUtil.CODEC).orElse(null);
        assembly = in.getIntOr("assembly", 0);
        releasing = in.getBooleanOr("releasing", false);
    }
}

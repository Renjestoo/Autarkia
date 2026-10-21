package dev.autarkia.colony;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.Autarkia;
import dev.autarkia.energy.EnergyBuffer;
import dev.autarkia.energy.GridMember;
import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MenuButtons;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import dev.autarkia.storage.MatterBank;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jspecify.annotations.Nullable;

/**
 * The {@code seed_matrix} (05 section 8.4, 3x3x3, center at the bottom middle, G9): 8 pattern card slots in layout order
 * and one seed output slot. The layout editor sets dx, dz and quarter turns per card (menu buttons, {@link #menuButton});
 * a new card starts east of the cards before it. The layout check is {@link SeedLayout#check} with the outpost square of
 * the colony tier (C4). Pack (owner or trusted, a tier 6 colony domain, an empty output slot) takes {@code ai_core} 1,
 * {@code nanite_paste} 16 and the bills of the crew (2 Porter, 2 Builder Drone) from colony storage, records the Pattern
 * ids in the colony (G20), then runs 1,200 ticks (GeckoLib {@code pack}) as a grid consumer at 834 FE/t until
 * 1,000,000 FE are in, and takes the BOM matter (the Pattern bills priced by {@link KitPrice}) from the domain buffers in
 * 20 equal parts, one at the start of every 60 ticks (reason "Seed"). Progress waits without power or matter. The cards
 * stay. The output is an {@code autarky_seed} with {@code seed_contents}. Breaking the matrix drops the cards, the seed
 * and the items taken for a running pack.
 */
public class SeedMatrixBlockEntity extends AutarkiaBlockEntity implements Ticking, GeoBlockEntity, MenuProvider, GridMember, JadeInfo, MenuButtons {
    /** Matrix states for the GUI and Jade. */
    public enum Status { READY, NO_CARDS, NO_PATTERN, NOT_OUTPOST, OVERLAP, TOO_BIG, NO_FIT, OUTSIDE, TIER_LOW, OUTPUT_FULL, MISSING_ITEMS, RUNNING,
        NO_POWER, NO_MATTER;

        /** Return the translation key of the state line. */
        public String key() {
            return "jade.autarkia.seed_matrix." + name().toLowerCase(Locale.ROOT);
        }
    }

    public static final int CARDS = 8;
    public static final int OUTPUT = 8;
    public static final int DURATION = 1200;
    public static final int FE_PER_TICK = 834;
    public static final long FE_TOTAL = 1_000_000L;
    public static final int MATTER_PARTS = 20;
    public static final int RANGE = 96;
    public static final int BUTTON_PACK = 120;
    public static final int BUTTON_STEP8 = 64;
    public static final int AI_CORES = 1;
    public static final int NANITE_PASTE = 16;
    public static final int DATA = 12 + 3 * CARDS;
    private static final RawAnimation PACK = RawAnimation.begin().thenLoop("animation.seed_matrix.pack");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.seed_matrix.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final MachineInventory inv = new MachineInventory(CARDS, 1, s -> s.is(AutarkiaItems.PATTERN_CARD.get()), this::layoutChanged);
    private final EnergyBuffer energy = new EnergyBuffer(FE_PER_TICK * 200, FE_PER_TICK * 4, 0, this::setChanged);
    private final int[] dx = new int[CARDS];
    private final int[] dz = new int[CARDS];
    private final int[] rot = new int[CARDS];
    private final @Nullable UUID[] ids = new UUID[CARDS];
    private Status status = Status.NO_CARDS;
    private int progress;
    private long feUsed;
    private long muTotal;
    private long muPaid;
    private int partsPaid;
    private @Nullable SeedContents pending;
    private final List<ItemStack> held = new ArrayList<>();
    private SeedLayout.@Nullable Result check;
    private boolean dirty = true;
    private @Nullable String missing;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            if (i >= 12) {
                int k = (i - 12) / 3;
                return switch ((i - 12) % 3) {
                    case 0 -> dx[k];
                    case 1 -> dz[k];
                    default -> rot[k];
                };
            }
            SeedLayout.Result r = check;
            Vec3i size = r == null ? Vec3i.ZERO : r.size();
            int blocks = r == null ? 0 : r.blocks();
            return switch (i) {
                case 0 -> progress;
                case 1 -> DURATION;
                case 2 -> energy.capacity() == 0 ? 0 : (int) (100L * energy.stored() / energy.capacity());
                case 3 -> status.ordinal();
                case 4 -> matterPercent();
                case 5 -> size.getX();
                case 6 -> size.getY();
                case 7 -> size.getZ();
                case 8 -> blocks / 10000;
                case 9 -> blocks % 10000;
                case 10 -> cards();
                default -> r == null ? 0 : r.margin();
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return DATA;
        }
    };

    public SeedMatrixBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.SEED_MATRIX.get(), pos, state);
    }

    /** Return the inventory (cards 0 to 7, seed output 8). */
    public MachineInventory inventory() {
        return inv;
    }

    /** Return the energy buffer. */
    public EnergyBuffer energy() {
        return energy;
    }

    /** Return the status. */
    public Status status() {
        return status;
    }

    /** Return the pack progress in ticks. */
    public int progress() {
        return progress;
    }

    /** Return the FE taken by the running or last pack. */
    public long feUsed() {
        return feUsed;
    }

    /** Return the BOM matter price of the running or last pack and the part paid so far. */
    public long[] matter() {
        return new long[] {muTotal, muPaid};
    }

    /** Return the paid share of the BOM matter in percent (0 without a pack). */
    public int matterPercent() {
        return muTotal <= 0 ? (pending == null ? 0 : 100) : (int) (100 * muPaid / muTotal);
    }

    /** Return the number of card slots holding a recorded Pattern. */
    public int cards() {
        int n = 0;
        for (int i = 0; i < CARDS; i++) {
            n += inv.get(i).has(AutarkiaComponents.PATTERN_REF.get()) ? 1 : 0;
        }
        return n;
    }

    /** Set the layout of one card slot (dx and dz clamped to -96..96, quarter turns 0..3). */
    public void layout(int slot, int x, int z, int turns) {
        dx[slot] = Math.clamp(x, -RANGE, RANGE);
        dz[slot] = Math.clamp(z, -RANGE, RANGE);
        rot[slot] = Math.floorMod(turns, 4);
        dirty = true;
        setChanged();
    }

    /** Return the layout of a card slot as {dx, dz, turns}. */
    public int[] layout(int slot) {
        return new int[] {dx[slot], dz[slot], rot[slot]};
    }

    private void layoutChanged() {
        dirty = true;
        setChanged();
    }

    private List<SeedContents.Entry> entries() {
        List<SeedContents.Entry> out = new ArrayList<>();
        for (int i = 0; i < CARDS; i++) {
            Pattern.Ref ref = inv.get(i).get(AutarkiaComponents.PATTERN_REF.get());
            if (ref != null) {
                out.add(new SeedContents.Entry(ref.id(), dx[i], dz[i], rot[i]));
            } else if (!inv.get(i).isEmpty()) {
                out.add(new SeedContents.Entry(new UUID(0, 0), dx[i], dz[i], rot[i]));
            }
        }
        return out;
    }

    private void syncCards(ServerLevel level) {
        for (int i = 0; i < CARDS; i++) {
            Pattern.Ref ref = inv.get(i).get(AutarkiaComponents.PATTERN_REF.get());
            UUID id = ref == null ? null : ref.id();
            if (java.util.Objects.equals(id, ids[i])) {
                continue;
            }
            ids[i] = id;
            int east = 0;
            for (int j = 0; j < i && id != null; j++) {
                Pattern p = ids[j] == null ? null : Pattern.load(level.getServer(), ids[j]);
                if (p != null) {
                    east = Math.max(east, dx[j] + ColonyPlans.rotatedSize(p.size(), SeedLayout.rotation(rot[j])).getX());
                }
            }
            layout(i, id == null ? 0 : east, 0, 0);
        }
    }

    private Status readiness(ServerLevel level, @Nullable Colony c) {
        if (c == null) {
            return Status.OUTSIDE;
        }
        if (c.tier < 6) {
            return Status.TIER_LOW;
        }
        List<SeedContents.Entry> entries = entries();
        if (entries.isEmpty()) {
            check = null;
            return Status.NO_CARDS;
        }
        if (dirty || check == null || level.getGameTime() % 20 == 0) {
            check = SeedLayout.check(level.getServer(), entries, ColonyTiers.outpostHalf(c.tier));
            dirty = false;
        }
        Status s = switch (check.problem()) {
            case NO_PATTERN -> Status.NO_PATTERN;
            case NOT_OUTPOST -> Status.NOT_OUTPOST;
            case OVERLAP -> Status.OVERLAP;
            case TOO_BIG -> Status.TOO_BIG;
            case NO_FIT -> Status.NO_FIT;
            default -> Status.READY;
        };
        if (s == Status.READY && !inv.get(OUTPUT).isEmpty()) {
            return Status.OUTPUT_FULL;
        }
        return s == Status.READY && missing != null && status == Status.MISSING_ITEMS ? Status.MISSING_ITEMS : s;
    }

    @Override
    public void serverTick() {
        ServerLevel level = serverLevel();
        Status before = status;
        syncCards(level);
        Colony c = Colonies.at(level, worldPosition);
        if (pending != null) {
            status = run(level, c);
        } else {
            status = readiness(level, c);
        }
        if (status != before || progress % 20 == 0) {
            syncNow();
        }
    }

    private Status run(ServerLevel level, @Nullable Colony c) {
        if (c == null) {
            return Status.OUTSIDE;
        }
        if (progress < DURATION && partsPaid < MATTER_PARTS && progress >= partsPaid * (DURATION / MATTER_PARTS)) {
            long part = muTotal * (partsPaid + 1) / MATTER_PARTS - muTotal * partsPaid / MATTER_PARTS;
            if (part > 0 && !MatterBank.withdraw(level, c, part, "Seed")) {
                return Status.NO_MATTER;
            }
            muPaid += part;
            partsPaid++;
            setChanged();
        }
        if (progress < DURATION) {
            int draw = (int) Math.min(FE_PER_TICK, FE_TOTAL - feUsed);
            if (energy.stored() < draw) {
                return Status.NO_POWER;
            }
            energy.drain(draw);
            feUsed += draw;
            progress++;
            return Status.RUNNING;
        }
        ItemStack seed = new ItemStack(AutarkiaItems.AUTARKY_SEED.get());
        seed.set(AutarkiaComponents.SEED_CONTENTS.get(), pending);
        inv.put(OUTPUT, seed);
        Autarkia.LOG.info("[colony] seed matrix at {} packed {} Patterns: {} blocks, {} MU, {} FE", worldPosition.toShortString(), pending.entries().size(),
            pending.blocks(), muPaid, feUsed);
        ColonyRuntime.of(c).log("Autarky Seed packed: " + pending.entries().size() + " Patterns, " + pending.blocks() + " blocks");
        pending = null;
        held.clear();
        progress = 0;
        setChanged();
        return readiness(level, c);
    }

    /** Return the BOM matter price of a checked layout: every Pattern bill item at its {@link KitPrice}. */
    public static long price(ServerLevel level, SeedLayout.Result r) {
        long total = 0;
        for (SeedLayout.Placed p : r.placed()) {
            for (var e : p.pattern().bom().entrySet()) {
                total += KitPrice.price(level, e.getKey()) * e.getValue();
            }
        }
        return total;
    }

    /** Return the items a pack takes from colony storage: {@code ai_core} 1, {@code nanite_paste} 16 and the crew bills. */
    public static List<SizedIngredient> bill(ServerLevel level) {
        List<SizedIngredient> needs = new ArrayList<>();
        needs.add(new SizedIngredient(Ingredient.of(AutarkiaItems.AI_CORE.get()), AI_CORES));
        needs.add(new SizedIngredient(Ingredient.of(AutarkiaItems.NANITE_PASTE.get()), NANITE_PASTE));
        for (var id : SeedWorkers.DEFAULT) {
            needs.addAll(SeedWorkers.bill(level, id));
        }
        return needs;
    }

    /** Start a pack for the player; returns the resulting status (RUNNING on success). */
    public Status pack(Player player) {
        ServerLevel level = serverLevel();
        Colony c = Colonies.at(level, worldPosition);
        if (pending != null) {
            return status;
        }
        Status ready = readiness(level, c);
        if (ready != Status.READY && ready != Status.MISSING_ITEMS) {
            return status = ready;
        }
        if (!c.canManage(player.getUUID())) {
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                sp.sendOverlayMessage(Component.translatable("message.autarkia.colony.no_rights"));
            }
            return status;
        }
        SeedLayout.Result r = SeedLayout.check(level.getServer(), entries(), ColonyTiers.outpostHalf(c.tier));
        if (r.problem() != SeedLayout.Problem.NONE) {
            return status = readiness(level, c);
        }
        List<ItemStack> taken = new ArrayList<>();
        SizedIngredient lack = SeedWorkers.take(level, c, bill(level), taken);
        if (lack != null) {
            missing = lack.ingredient().items().findFirst().map(h -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(h.value()).getPath()).orElse("?") + " x" + lack.count();
            return status = Status.MISSING_ITEMS;
        }
        missing = null;
        held.addAll(taken);
        muTotal = price(level, r);
        muPaid = 0;
        partsPaid = 0;
        progress = 0;
        feUsed = 0;
        pending = new SeedContents(SeedContents.VERSION, r.entries(), SeedWorkers.DEFAULT, FE_TOTAL, r.size(), r.blocks());
        for (UUID id : pending.patterns()) {
            if (!c.seedPatterns.contains(id)) {
                c.seedPatterns.add(id);
            }
        }
        Colonies.changed();
        Autarkia.LOG.info("[colony] seed matrix at {} starts packing: {} Patterns, {} blocks, BOM {} MU", worldPosition.toShortString(), r.placed().size(),
            r.blocks(), muTotal);
        setChanged();
        return status = Status.RUNNING;
    }

    @Override
    public boolean menuButton(Player player, int id) {
        if (id == BUTTON_PACK) {
            pack(player);
            return true;
        }
        int step = id >= BUTTON_STEP8 ? 8 : 1;
        int b = id % BUTTON_STEP8;
        int slot = b / 5;
        if (id < 0 || id >= BUTTON_PACK || slot >= CARDS || pending != null) {
            return false;
        }
        switch (b % 5) {
            case 0 -> layout(slot, dx[slot] - step, dz[slot], rot[slot]);
            case 1 -> layout(slot, dx[slot] + step, dz[slot], rot[slot]);
            case 2 -> layout(slot, dx[slot], dz[slot] - step, rot[slot]);
            case 3 -> layout(slot, dx[slot], dz[slot] + step, rot[slot]);
            default -> layout(slot, dx[slot], dz[slot], rot[slot] + 1);
        }
        return true;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (isCenter() && level != null) {
            for (int i = 0; i < inv.size(); i++) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, inv.get(i).copy());
            }
            held.forEach(s -> Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, s.copy()));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("menu.autarkia.seed_matrix");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.SEED_MATRIX.get(), id, playerInv, worldPosition, inv, data, GuiLayouts.SEED_MATRIX);
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
        return energy.room() > 0 ? FE_PER_TICK * 4 : 0;
    }

    @Override
    public int workDraw() {
        return status == Status.RUNNING || status == Status.NO_POWER ? FE_PER_TICK : 0;
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
        out.add(Component.translatable("jade.autarkia.seed_matrix.patterns", cards(), CARDS));
        out.add(Component.translatable("jade.autarkia.seed_matrix.matter", matterPercent()));
        out.add(Component.translatable(status.key()));
        if (status == Status.MISSING_ITEMS && missing != null) {
            out.add(Component.translatable("jade.autarkia.seed_matrix.lacks", missing));
        }
        out.add(Component.translatable("jade.autarkia.buffer", energy.stored(), energy.capacity()));
    }

    @Override
    public float jadeProgress() {
        return pending == null ? -1 : progress / (float) DURATION;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SeedMatrixBlockEntity>("main", 4,
            test -> test.setAndContinue(test.animatable().status == Status.RUNNING ? PACK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        inv.serialize(out.child("items"));
        energy.serialize(out.child("energy"));
        out.putIntArray("dx", dx.clone());
        out.putIntArray("dz", dz.clone());
        out.putIntArray("rot", rot.clone());
        List<String> known = new ArrayList<>();
        for (UUID id : ids) {
            known.add(id == null ? "" : id.toString());
        }
        out.store("cards", com.mojang.serialization.Codec.STRING.listOf(), known);
        out.putInt("status", status.ordinal());
        out.putInt("progress", progress);
        out.putLong("fe_used", feUsed);
        out.putLong("mu_total", muTotal);
        out.putLong("mu_paid", muPaid);
        out.putInt("parts_paid", partsPaid);
        if (pending != null) {
            out.store("pending", SeedContents.CODEC, pending);
            out.store("held", ItemStack.CODEC.listOf(), List.copyOf(held));
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("items").ifPresent(inv::deserialize);
        in.child("energy").ifPresent(energy::deserialize);
        copy(in.getIntArray("dx").orElse(new int[0]), dx);
        copy(in.getIntArray("dz").orElse(new int[0]), dz);
        copy(in.getIntArray("rot").orElse(new int[0]), rot);
        List<String> known = in.read("cards", com.mojang.serialization.Codec.STRING.listOf()).orElse(List.of());
        for (int i = 0; i < CARDS; i++) {
            ids[i] = i < known.size() && !known.get(i).isEmpty() ? UUID.fromString(known.get(i)) : null;
        }
        status = Status.values()[Math.clamp(in.getIntOr("status", 0), 0, Status.values().length - 1)];
        progress = in.getIntOr("progress", 0);
        feUsed = in.getLongOr("fe_used", 0L);
        muTotal = in.getLongOr("mu_total", 0L);
        muPaid = in.getLongOr("mu_paid", 0L);
        partsPaid = in.getIntOr("parts_paid", 0);
        pending = in.read("pending", SeedContents.CODEC).orElse(null);
        held.clear();
        if (pending != null) {
            in.read("held", ItemStack.CODEC.listOf()).ifPresent(held::addAll);
        }
        dirty = true;
    }

    private static void copy(int[] from, int[] to) {
        for (int i = 0; i < to.length; i++) {
            to[i] = i < from.length ? from[i] : 0;
        }
    }
}

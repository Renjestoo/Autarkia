package dev.autarkia.processing;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.AutarkiaRecipes;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.MenuButtons;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.storage.MatterBank;
import dev.autarkia.storage.MatterBufferBlockEntity;
import dev.autarkia.storage.MatterValues;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import org.jspecify.annotations.Nullable;

/**
 * {@code matter_compiler}, 2x2x2 heavy T6 machine (03 section 4): it writes matter units into the result of the target
 * {@code autarkia:compiling} recipe. The feed slot (slot 0) takes only items with a matter value and credits their MU
 * into the internal buffer of {@value #MU_CAPACITY} MU as fast as the buffer has room (raw ores always at the tier 6
 * multiplier, 03 section 6). When the buffer lacks the MU of the next operation and the compiler is otherwise ready, it
 * pulls whole operations of free MU from the matter buffers of its domain through {@link MatterBank} (fullest first,
 * reason "Compiler"), but only when the domain covers at least the missing amount (M9 G6). An operation runs
 * {@link #ticks(long)} ticks at {@value #FE_PER_TICK} FE/t and takes the recipe MU at its end; results go to the 4
 * output slots (1 to 4). Without a foundation under each bottom position (checked every 20 ticks) or without enough MU
 * the status is NO_CONDITION. Menu buttons: 0 next target, 1 previous target, 2 no target, 100 + i target i of
 * {@link #targets(RecipeMap)}. Sounds: respawn anchor charge at a start, beacon activate per result; particles reverse
 * portal and glow while it runs.
 */
public class MatterCompilerBlockEntity extends MachineBlockEntity implements GeoBlockEntity, MenuButtons {
    /** FE per tick while compiling. */
    public static final int FE_PER_TICK = 16384;
    /** Internal MU buffer capacity. */
    public static final long MU_CAPACITY = 65_536;
    /** Shortest operation in ticks. */
    public static final int MIN_TICKS = 20;
    /** Menu button id of the first direct target choice. */
    public static final int TARGET_BUTTON = 100;
    /** Withdrawal reason of the domain pull. */
    public static final String REASON = "Compiler";
    private static final RawAnimation COMPILE = RawAnimation.begin().thenLoop("animation.matter_compiler.compile");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.matter_compiler.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private long mu;
    private @Nullable Identifier target;
    private boolean compiling;
    private boolean foundation;
    private boolean surveyed;
    private int draw;
    private long energyUsed;
    private long pulled;
    private Vec3 middle = Vec3.ZERO;

    public MatterCompilerBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.MATTER_COMPILER.get(), pos, state, 1, 4, FE_PER_TICK);
    }

    /** Return the base ticks of one operation: max(20, ceil(MU / {@code matter.compilerMuPerTick})). */
    public static int ticks(long matter) {
        long d = AutarkiaConfig.MATTER_COMPILER_MU_PER_TICK.get();
        return (int) Math.max(MIN_TICKS, (matter + d - 1) / d);
    }

    /** Return the FE of one operation at the base rate: {@value #FE_PER_TICK} x {@link #ticks(long)}. */
    public static long energyFor(long matter) {
        return (long) FE_PER_TICK * ticks(matter);
    }

    /** Return the compiling recipes sorted by id (server and client build the same list). */
    public static List<RecipeHolder<MachineRecipe>> targets(RecipeMap map) {
        List<RecipeHolder<MachineRecipe>> list = new ArrayList<>(map.byType(AutarkiaRecipes.type(MachineRecipeKind.COMPILING)));
        list.sort(Comparator.comparing(h -> h.id().identifier().toString()));
        return list;
    }

    /** Return the MU in the internal buffer. */
    public long matter() {
        return mu;
    }

    /** Return the target recipe id, or null. */
    public @Nullable Identifier target() {
        return target;
    }

    /** Return the FE the operations drained from the buffer since placement. */
    public long energyUsed() {
        return energyUsed;
    }

    /** Return the free MU this compiler pulled from the domain since placement. */
    public long pulled() {
        return pulled;
    }

    /** Set the target recipe (null for none); the progress restarts. */
    public void setTarget(@Nullable Identifier id) {
        target = id;
        progress = 0;
        setChanged();
    }

    @Override
    protected boolean acceptsInput(ItemStack stack) {
        return MatterValues.accepted(stack, 6);
    }

    @Override
    public boolean menuButton(Player player, int button) {
        List<RecipeHolder<MachineRecipe>> list = targets(serverLevel().recipeAccess().recipeMap());
        if (button >= TARGET_BUTTON) {
            int i = button - TARGET_BUTTON;
            if (i >= list.size()) {
                return false;
            }
            setTarget(list.get(i).id().identifier());
            return true;
        }
        if (button < 0 || button > 2) {
            return false;
        }
        if (button == 2 || list.isEmpty()) {
            setTarget(null);
            return true;
        }
        int at = indexOf(list, target);
        int next = button == 0 ? at + 1 : at < 0 ? list.size() - 1 : at - 1;
        setTarget(next < 0 || next >= list.size() ? null : list.get(next).id().identifier());
        return true;
    }

    private static int indexOf(List<RecipeHolder<MachineRecipe>> list, @Nullable Identifier id) {
        for (int i = 0; id != null && i < list.size(); i++) {
            if (list.get(i).id().identifier().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /** Return the target recipe on the server, or null without a target or when it is not loaded. */
    public @Nullable MachineRecipe targetRecipe() {
        if (target == null || !(level instanceof ServerLevel sl)) {
            return null;
        }
        for (RecipeHolder<MachineRecipe> h : sl.recipeAccess().recipeMap().byType(AutarkiaRecipes.type(MachineRecipeKind.COMPILING))) {
            if (h.id().identifier().equals(target)) {
                return h.value();
            }
        }
        return null;
    }

    @Override
    protected MachineRecipeKind kind() {
        return MachineRecipeKind.COMPILING;
    }

    @Override
    protected int defaultTime() {
        return MIN_TICKS;
    }

    @Override
    protected int defaultEnergy() {
        return FE_PER_TICK;
    }

    @Override
    protected float workRate() {
        return foundation ? 1 : 0;
    }

    @Override
    public boolean running() {
        return compiling;
    }

    @Override
    public int workDraw() {
        return draw;
    }

    private void survey() {
        Set<BlockPos> parts = ((BaseMultiblock) getBlockState().getBlock()).getMultiblockShape(level, worldPosition, getBlockState()).getGlobalPositions();
        double x = 0;
        double y = 0;
        double z = 0;
        for (BlockPos p : parts) {
            x += p.getX();
            y += p.getY();
            z += p.getZ();
        }
        int n = Math.max(1, parts.size());
        middle = new Vec3(x / n + 0.5, y / n + 0.5, z / n + 0.5);
        foundation = onFoundation();
        surveyed = true;
    }

    private void credit() {
        ItemStack feed = inv.get(0);
        if (feed.isEmpty()) {
            return;
        }
        long each = MatterValues.of(feed, 6);
        if (each <= 0) {
            return;
        }
        int n = (int) Math.min(feed.getCount(), (MU_CAPACITY - mu) / each);
        if (n > 0) {
            mu += n * each;
            inv.shrink(0, n);
            setChanged();
        }
    }

    private boolean pull(ServerLevel sl, long matter) {
        long need = matter - mu;
        if (need <= 0) {
            return true;
        }
        long room = MU_CAPACITY - mu;
        Colony c = Colonies.server() == null ? null : Colonies.at(sl, worldPosition);
        if (c == null || room < need) {
            return false;
        }
        long avail = MatterBank.available(sl, c);
        if (avail < need) {
            return false;
        }
        long take = (mu + Math.min(room, avail)) / matter * matter - mu;
        if (!MatterBank.withdraw(sl, c, take, REASON)) {
            return false;
        }
        mu += take;
        pulled += take;
        setChanged();
        return true;
    }

    private int duration(long matter) {
        return Math.max(1, (int) Math.round(ticks(matter) / AutarkiaConfig.PROCESSING_SPEED_MULTIPLIER.get()));
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        idleTicks++;
        if (sl.getGameTime() % 20 == 0 || !surveyed) {
            survey();
        }
        credit();
        MachineRecipe r = targetRecipe();
        boolean was = compiling;
        int before = energy.stored();
        draw = 0;
        MachineStatus.State next;
        if (r == null || r.matter() <= 0) {
            next = MachineStatus.State.IDLE_NO_INPUT;
            progress = 0;
        } else {
            duration = duration(r.matter());
            int fe = (int) Math.ceil(FE_PER_TICK * AutarkiaConfig.PROCESSING_ENERGY_MULTIPLIER.get());
            if (!inv.fitsOutputs(r.allResults())) {
                next = MachineStatus.State.BLOCKED_OUTPUT_FULL;
            } else if (!foundation || mu < r.matter() && (energy.stored() < fe || !pull(sl, r.matter()))) {
                next = MachineStatus.State.NO_CONDITION;
            } else if (energy.stored() < fe) {
                draw = fe;
                next = MachineStatus.State.NO_POWER;
            } else {
                energy.drain(fe);
                draw = fe;
                progress += 1;
                next = MachineStatus.State.RUNNING;
                if (progress >= duration) {
                    finish(sl, r);
                }
            }
        }
        state = next;
        energyUsed += Math.max(0, before - energy.stored());
        compiling = next == MachineStatus.State.RUNNING;
        if (compiling != was) {
            syncNow();
            if (compiling) {
                sl.playSound(null, BlockPos.containing(middle), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.8f, 1.0f);
            }
        }
        if (compiling && sl.getGameTime() % 3 == 0) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, middle.x, middle.y, middle.z, 6, 0.4, 0.4, 0.4, 0.04);
            sl.sendParticles(ParticleTypes.GLOW, middle.x, middle.y + 0.3, middle.z, 1, 0.25, 0.25, 0.25, 0.01);
        }
    }

    private void finish(ServerLevel sl, MachineRecipe r) {
        mu -= r.matter();
        inv.addOutputs(r.roll(sl.getRandom()));
        progress = 0;
        idleTicks = 0;
        setChanged();
        sl.playSound(null, BlockPos.containing(middle), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6f, 1.4f);
        sl.sendParticles(ParticleTypes.GLOW, middle.x, middle.y, middle.z, 12, 0.5, 0.5, 0.5, 0.05);
    }

    @Override
    protected int extraData(int i) {
        return switch (i) {
            case 0 -> (int) mu;
            case 1 -> level instanceof ServerLevel sl ? indexOf(targets(sl.recipeAccess().recipeMap()), target) + 1 : 0;
            default -> 0;
        };
    }

    @Override
    protected int extraDataCount() {
        return 2;
    }

    @Override
    public void jadeLines(List<Component> out) {
        super.jadeLines(out);
        out.add(Component.translatable("jade.autarkia.matter_compiler.matter", MatterBufferBlockEntity.fmt(mu), MatterBufferBlockEntity.fmt(MU_CAPACITY)));
        MachineRecipe r = targetRecipe();
        if (r != null) {
            out.add(Component.translatable("jade.autarkia.matter_compiler.target", r.results().getFirst().item().create().getHoverName(),
                MatterBufferBlockEntity.fmt(r.matter())));
        } else {
            out.add(Component.translatable("jade.autarkia.matter_compiler.no_target"));
        }
        if (!foundation) {
            out.add(Component.translatable("jade.autarkia.no_foundation"));
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.MATTER_COMPILER.get(), id, playerInv, worldPosition, inv, data, GuiLayouts.MATTER_COMPILER);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<MatterCompilerBlockEntity>("main", 4, test -> test.setAndContinue(test.animatable().running() ? COMPILE : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putLong("matter", mu);
        if (target != null) {
            out.putString("target", target.toString());
        }
        out.putBoolean("compiling", compiling);
        out.putLong("energy_used", energyUsed);
        out.putLong("pulled", pulled);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        mu = in.getLongOr("matter", 0);
        target = in.getString("target").map(Identifier::parse).orElse(null);
        compiling = in.getBooleanOr("compiling", false);
        energyUsed = in.getLongOr("energy_used", 0);
        pulled = in.getLongOr("pulled", 0);
    }
}

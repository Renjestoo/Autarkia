package dev.autarkia.equipment;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.klikli_dev.magicparticleslib.premade.particle.electricarc.ElectricArcParticleOptions;
import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.EnergyBuffer;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.GridMember;
import dev.autarkia.energy.GridNetwork;
import dev.autarkia.integration.AnthologyBridge;
import dev.autarkia.network.AscensionPosePayload;
import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.AutarkiaRecipes;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.EventTrigger;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * The {@code forge_of_ascension} (07 section 3.5, W22, decision G15): a 3x4x3 open chamber whose center is the bottom
 * middle position; the player stands on top of it (the pad). Sneak + use with an empty hand on the pad starts a
 * 600-tick fitting when at least one piece can be fitted (an {@code ascension_fitting} recipe whose worn item the player
 * wears and whose inputs are in the 27 slots) and the forge's grid networks hold {@code forge.feTotal} FE. While
 * locked the player is held on the pad (movement zeroed, put back when more than 0.1 blocks off, -100% movement speed
 * and jump strength) and takes no damage. Timeline: 0 {@code wake} and the {@code autarkia:ascension_fit} pose, 20
 * {@code scan}, then the steps of {@link #STEPS} (the carapace piece comes off at the step start, the recipe FE is drawn
 * evenly per tick from the forge's networks, the inputs are taken and the Aegis piece goes on 10 ticks before the step
 * end, with the camera shake of the step), 520 {@code seal} and {@code autarkia:ascension_rise}, 600 unlock and
 * {@code equipment/ascended} for a full Aegis set. A step without a matching recipe is skipped. Leaving the pad, logging
 * out, dying or a tick the grid cannot pay aborts: the arms {@code retract}, a removed carapace piece without its Aegis
 * piece goes back, fitted pieces stay.
 */
public class ForgeOfAscensionBlockEntity extends AutarkiaBlockEntity implements Ticking, GeoBlockEntity, MenuProvider, GridMember, JadeInfo {
    /** Forge states for the GUI and Jade. */
    public enum Status { IDLE, FITTING, DONE, ABORTED }

    /** One fitting step of 07 section 3.5: slot, tick window, arm trigger, shake, fitting height above the pad (blocks). */
    public record Step(int number, EquipmentSlot slot, int start, int end, String trigger, float shake, int shakeTicks, double height) {
        /** Return the tick at which the Aegis piece goes on (10 ticks before the step end). */
        public int equipTick() {
            return end - 10;
        }
    }

    public static final int SLOTS = 27;
    public static final int TOTAL_TICKS = 600;
    public static final int SCAN_AT = 20;
    public static final int SEAL_AT = 520;
    public static final List<Step> STEPS = List.of(
        new Step(2, EquipmentSlot.FEET, 60, 160, "fit_boots", 0.3F, 10, 0.12),
        new Step(3, EquipmentSlot.LEGS, 160, 280, "fit_legs", 0.4F, 10, 0.56),
        new Step(4, EquipmentSlot.CHEST, 280, 420, "fit_chest", 0.6F, 20, 1.13),
        new Step(5, EquipmentSlot.HEAD, 420, 520, "fit_helmet", 0.4F, 10, 1.69));
    private static final List<String> TRIGGERS = List.of("wake", "scan", "fit_boots", "fit_legs", "fit_chest", "fit_helmet", "seal", "retract");
    private static final int GLOW = 0xFF7FFFF0;
    private static final int ARC = 0xFFF5D76E;
    private static final Identifier LOCK = Autarkia.id("forge_lock");
    private static final Set<UUID> LOCKED = Collections.newSetFromMap(new HashMap<>());
    private static final Map<UUID, GlobalPos> FITTING = new HashMap<>();
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.forge_of_ascension.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final MachineInventory inv = new MachineInventory(SLOTS, 0, s -> true, this::setChanged);
    private final EnergyBuffer port = new EnergyBuffer(0, 0, 0, this::setChanged);
    private Status status = Status.IDLE;
    private int tick;
    private @Nullable UUID fitter;
    private @Nullable ServerPlayer fitterRef;
    private @Nullable Identifier recipe;
    private ItemStack held = ItemStack.EMPTY;
    private int fitted;
    private long drawn;
    private int lastDraw;
    private long gridCache;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> tick;
                case 1 -> TOTAL_TICKS;
                case 2 -> fitted;
                case 3 -> lastDraw;
                case 4 -> status.ordinal();
                default -> (int) Math.min(Integer.MAX_VALUE, gridCache / 1000);
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public ForgeOfAscensionBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.FORGE_OF_ASCENSION.get(), pos, state);
    }

    /** Register the logout abort (game bus). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            GlobalPos at = FITTING.get(e.getEntity().getUUID());
            if (at != null && e.getEntity() instanceof ServerPlayer sp && sp.level().getServer().getLevel(at.dimension()) instanceof ServerLevel sl
                && sl.getBlockEntity(at.pos()) instanceof ForgeOfAscensionBlockEntity forge) {
                forge.abort(sl, sp, "logout");
            }
        });
    }

    /** Return true while the player is locked in a fitting (all damage but bypassing sources is cancelled). */
    public static boolean locked(Player player) {
        return LOCKED.contains(player.getUUID());
    }

    /** Return the 27 input slots. */
    public MachineInventory inventory() {
        return inv;
    }

    /** Return the forge state. */
    public Status status() {
        return status;
    }

    /** Return the fitting tick (0 to 600). */
    public int tick() {
        return tick;
    }

    /** Return the number of pieces the current or last fitting put on. */
    public int fitted() {
        return fitted;
    }

    /** Return the FE the current or last fitting drew from the grid. */
    public long drawn() {
        return drawn;
    }

    /** Return the step whose window holds the current tick while fitting, or null. */
    public @Nullable Step currentStep() {
        if (status != Status.FITTING) {
            return null;
        }
        for (Step s : STEPS) {
            if (tick >= s.start() && tick < s.end()) {
                return s;
            }
        }
        return null;
    }

    /** Return the translation key of the fitting phase at a tick: lock, scan, the step number of {@link #STEPS}, or seal. */
    public static String phaseKey(int tick) {
        if (tick < SCAN_AT) {
            return "jade.autarkia.forge.step.lock";
        }
        if (tick >= SEAL_AT) {
            return "jade.autarkia.forge.step.seal";
        }
        for (Step s : STEPS) {
            if (tick >= s.start() && tick < s.end()) {
                return "jade.autarkia.forge.step." + s.number();
            }
        }
        return "jade.autarkia.forge.step.scan";
    }

    /** Return the position the fitted player stands at: the top centre of the pad. */
    public Vec3 pad() {
        return Vec3.atBottomCenterOf(worldPosition).add(0, 1, 0);
    }

    /** Return true when the player stands on the pad (within 0.6 blocks of its top centre). */
    public boolean onPad(Player player) {
        Vec3 p = player.position();
        Vec3 c = pad();
        return Math.hypot(p.x - c.x, p.z - c.z) <= 0.6 && Math.abs(p.y - c.y) <= 0.6;
    }

    /**
     * Start a fitting for the player; return null when it started, else the refusal shown to the player (forge busy, not
     * on the pad, nothing to fit, or less than {@code forge.feTotal} FE on the forge's grid networks).
     */
    public @Nullable Component start(ServerPlayer player) {
        if (!(level instanceof ServerLevel sl) || !isCenter()) {
            return Component.translatable("message.autarkia.forge.not_formed");
        }
        if (status == Status.FITTING || FITTING.containsKey(player.getUUID())) {
            return Component.translatable("message.autarkia.forge.busy");
        }
        if (!onPad(player)) {
            return Component.translatable("message.autarkia.forge.pad");
        }
        if (STEPS.stream().noneMatch(s -> recipe(sl, s, player) != null)) {
            return Component.translatable("message.autarkia.forge.nothing");
        }
        long grid = gridFe(networks(sl));
        int need = AutarkiaConfig.FORGE_FE_TOTAL.get();
        if (grid < need) {
            return Component.translatable("message.autarkia.forge.grid", need, grid);
        }
        status = Status.FITTING;
        tick = 0;
        fitted = 0;
        drawn = 0;
        lastDraw = 0;
        recipe = null;
        held = ItemStack.EMPTY;
        fitter = player.getUUID();
        fitterRef = player;
        FITTING.put(fitter, GlobalPos.of(sl.dimension(), worldPosition));
        lock(player);
        Autarkia.LOG.info("[forge] fitting of {} started at {}", player.getScoreboardName(), worldPosition.toShortString());
        syncNow();
        return null;
    }

    private @Nullable RecipeHolder<AscensionFittingRecipe> recipe(ServerLevel sl, Step s, Player player) {
        for (RecipeHolder<AscensionFittingRecipe> h : sl.recipeAccess().recipeMap().byType(AutarkiaRecipes.ASCENSION_FITTING.get())) {
            AscensionFittingRecipe r = h.value();
            if (r.step() == s.number() && r.piece() == s.slot() && player.getItemBySlot(s.slot()).is(r.worn()) && r.available(inv)) {
                return h;
            }
        }
        return null;
    }

    private @Nullable AscensionFittingRecipe byId(ServerLevel sl, Identifier id) {
        var h = sl.recipeAccess().recipeMap().byKey(ResourceKey.create(Registries.RECIPE, id));
        return h != null && h.value() instanceof AscensionFittingRecipe r ? r : null;
    }

    private @Nullable ServerPlayer player(ServerLevel sl) {
        if (fitter == null) {
            return null;
        }
        ServerPlayer p = fitterRef != null && fitterRef.getUUID().equals(fitter) ? fitterRef : sl.getServer().getPlayerList().getPlayer(fitter);
        return p == null || p.isRemoved() || p.hasDisconnected() || p.level() != sl || p.isDeadOrDying() ? null : p;
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        if (sl.getGameTime() % 20 == 0) {
            gridCache = gridFe(networks(sl));
        }
        if (status != Status.FITTING) {
            return;
        }
        ServerPlayer p = player(sl);
        if (p == null || p.position().distanceToSqr(pad()) > 2.25) {
            abort(sl, p, "left");
            return;
        }
        hold(p);
        if (!events(sl, p)) {
            return;
        }
        tick++;
        if (tick >= TOTAL_TICKS) {
            finish(sl, p);
        } else {
            syncThrottled(10);
        }
    }

    private boolean events(ServerLevel sl, ServerPlayer p) {
        Vec3 base = Vec3.atBottomCenterOf(worldPosition);
        if (tick == 0) {
            triggerAnim("arms", "wake");
            pose(p, AscensionPosePayload.FIT, true);
            sl.playSound(null, worldPosition, AutarkiaSounds.of("forge.fit"), SoundSource.BLOCKS, 1f, 1f);
        } else if (tick == SCAN_AT) {
            triggerAnim("arms", "scan");
        }
        if (tick >= SCAN_AT && tick < STEPS.getFirst().start() && tick % 4 == 0) {
            GlowParticleOptions glow = GlowParticleOptions.of(GLOW).size(0.12f).age(12);
            double y = p.getY() + p.getBbHeight() * (1 - (tick - SCAN_AT) / 40.0);
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                sl.sendParticles(glow, p.getX() + Math.cos(a) * 0.45, y, p.getZ() + Math.sin(a) * 0.45, 1, 0, 0, 0, 0);
            }
        }
        Step s = currentStep();
        lastDraw = 0;
        if (s != null && !step(sl, p, s, base)) {
            return false;
        }
        if (tick == SEAL_AT) {
            triggerAnim("arms", "seal");
            pose(p, AscensionPosePayload.RISE, true);
            sl.sendParticles(GlowParticleOptions.of(GLOW).size(0.25f).age(30), p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 1.0, 0.6, 0.05);
            sl.playSound(null, worldPosition, AutarkiaSounds.of("forge.seal"), SoundSource.BLOCKS, 1f, 1f);
        }
        return true;
    }

    private boolean step(ServerLevel sl, ServerPlayer p, Step s, Vec3 base) {
        if (tick == s.start()) {
            RecipeHolder<AscensionFittingRecipe> h = recipe(sl, s, p);
            recipe = h == null ? null : h.id().identifier();
            if (h != null) {
                held = p.getItemBySlot(s.slot()).copy();
                p.setItemSlot(s.slot(), ItemStack.EMPTY);
                triggerAnim("arms", s.trigger());
                setChanged();
            }
        }
        if (recipe == null) {
            return true;
        }
        AscensionFittingRecipe r = byId(sl, recipe);
        if (r == null) {
            abort(sl, p, "recipe");
            return false;
        }
        long n = s.end() - s.start();
        long i = tick - s.start();
        long fe = r.energy() * (i + 1) / n - r.energy() * i / n;
        if (!draw(networks(sl), fe)) {
            abort(sl, p, "power");
            return false;
        }
        drawn += fe;
        lastDraw = (int) Math.min(Integer.MAX_VALUE, fe);
        if (tick % 20 == 0) {
            sl.playSound(null, worldPosition, AutarkiaSounds.of("forge.fit"), SoundSource.BLOCKS, 1f, 1f);
        }
        if (tick % 4 == 0) {
            sparks(sl, base, s.height());
        }
        if (tick == s.equipTick()) {
            if (!r.consume(inv)) {
                abort(sl, p, "inputs");
                return false;
            }
            ItemStack out = new ItemStack(r.result());
            int carry = Math.min(PoweredItem.energy(held), AegisTether.capacity(out));
            out.set(AutarkiaComponents.ENERGY.get(), carry);
            p.setItemSlot(s.slot(), out);
            held = ItemStack.EMPTY;
            fitted++;
            AnthologyBridge.shake(sl, base.add(0, 1, 0), s.shake(), s.shakeTicks(), 16F);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, base.x, base.y + s.height(), base.z, 24, 0.4, 0.15, 0.4, 0.2);
            Autarkia.LOG.info("[forge] {} fitted at tick {} for {}", out.getItem(), tick, p.getScoreboardName());
            setChanged();
        }
        if (tick == s.end() - 1) {
            recipe = null;
        }
        return true;
    }

    private void sparks(ServerLevel sl, Vec3 base, double height) {
        Vec3 body = base.add(0, height, 0);
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                Vec3 tip = base.add(dx * 0.45, height + 0.35, dz * 0.45);
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, tip.x, tip.y, tip.z, 2, 0.05, 0.05, 0.05, 0.1);
                sl.sendParticles(new ElectricArcParticleOptions(body, ARC, 0.08F, sl.getRandom().nextInt(), 6), tip.x, tip.y, tip.z, 1, 0, 0, 0, 0);
            }
        }
    }

    private void finish(ServerLevel sl, ServerPlayer p) {
        unlock(p);
        status = Status.DONE;
        boolean full = AegisRule.fullSet(p);
        if (full) {
            EventTrigger.EQUIPMENT.get().fire(p, "ascension_complete");
        }
        Autarkia.LOG.info("[forge] fitting of {} done: {} pieces, {} FE, full Aegis set {}", p.getScoreboardName(), fitted, drawn, full);
        syncNow();
    }

    /** Abort a running fitting (reason for the log and the message key): retract, give back a removed piece, unlock. */
    void abort(ServerLevel sl, @Nullable ServerPlayer p, String reason) {
        if (status != Status.FITTING) {
            return;
        }
        triggerAnim("arms", "retract");
        Step s = currentStep();
        if (!held.isEmpty()) {
            if (p != null && s != null && p.getItemBySlot(s.slot()).isEmpty()) {
                p.setItemSlot(s.slot(), held);
            } else if (p == null || !p.getInventory().add(held)) {
                Vec3 at = pad();
                Containers.dropItemStack(sl, at.x, at.y, at.z, held);
            }
            held = ItemStack.EMPTY;
        }
        if (p != null) {
            pose(p, AscensionPosePayload.FIT, false);
            unlock(p);
            p.sendOverlayMessage(Component.translatable("message.autarkia.forge.abort." + reason));
        } else if (fitter != null) {
            LOCKED.remove(fitter);
            FITTING.remove(fitter);
        }
        recipe = null;
        status = Status.ABORTED;
        Autarkia.LOG.info("[forge] fitting at {} aborted at tick {} ({}), {} pieces fitted", worldPosition.toShortString(), tick, reason, fitted);
        syncNow();
    }

    private void pose(ServerPlayer p, Identifier animation, boolean play) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new AscensionPosePayload(p.getId(), animation, play));
    }

    private void lock(ServerPlayer p) {
        LOCKED.add(p.getUUID());
        modifier(p.getAttribute(Attributes.MOVEMENT_SPEED), true);
        modifier(p.getAttribute(Attributes.JUMP_STRENGTH), true);
    }

    private void unlock(ServerPlayer p) {
        LOCKED.remove(p.getUUID());
        FITTING.remove(p.getUUID());
        modifier(p.getAttribute(Attributes.MOVEMENT_SPEED), false);
        modifier(p.getAttribute(Attributes.JUMP_STRENGTH), false);
    }

    private static void modifier(@Nullable AttributeInstance a, boolean on) {
        if (a == null) {
            return;
        }
        if (on) {
            a.addOrUpdateTransientModifier(new AttributeModifier(LOCK, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            a.removeModifier(LOCK);
        }
    }

    private void hold(ServerPlayer p) {
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        Vec3 c = pad();
        if (p.position().distanceTo(c) > 0.1) {
            NaniteReform.move(p, c);
        }
    }

    /** Return the grid networks the forge is a member of (machine port or a tapping connector). */
    public List<GridNetwork> networks(ServerLevel sl) {
        List<GridNetwork> out = new ArrayList<>();
        for (GridNetwork net : Grid.of(sl).networks()) {
            for (GridNetwork.Attached a : net.members) {
                if (a.member == this) {
                    out.add(net);
                    break;
                }
            }
        }
        return out;
    }

    private static List<GridMember> sources(List<GridNetwork> nets) {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<GridMember> storage = new ArrayList<>();
        List<GridMember> generators = new ArrayList<>();
        for (GridNetwork net : nets) {
            for (GridNetwork.Attached a : net.members) {
                GridMember m = a.member;
                if (m.gridValid() && m.gridBuffer() != null && seen.add(m.gridBuffer())) {
                    if (m.gridRole() == Role.STORAGE) {
                        storage.add(m);
                    } else if (m.gridRole() == Role.GENERATOR) {
                        generators.add(m);
                    }
                }
            }
        }
        storage.addAll(generators);
        return storage;
    }

    /** Return the FE stored in the storage and generator buffers of the networks. */
    public static long gridFe(List<GridNetwork> nets) {
        long sum = 0;
        for (GridMember m : sources(nets)) {
            sum += m.gridBuffer().stored();
        }
        return sum;
    }

    private static boolean draw(List<GridNetwork> nets, long fe) {
        if (fe <= 0) {
            return true;
        }
        List<GridMember> src = sources(nets);
        long have = 0;
        for (GridMember m : src) {
            have += m.gridBuffer().stored();
        }
        if (have < fe) {
            return false;
        }
        long left = fe;
        for (GridMember m : src) {
            if (left <= 0) {
                break;
            }
            left -= m.gridBuffer().drain((int) Math.min(Integer.MAX_VALUE, left));
        }
        return true;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (isCenter() && level instanceof ServerLevel sl) {
            abort(sl, player(sl), "broken");
            for (int i = 0; i < inv.size(); i++) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, inv.get(i).copy());
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.autarkia.forge_of_ascension");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.FORGE_OF_ASCENSION.get(), id, playerInv, worldPosition, inv, data, GuiLayouts.FORGE_OF_ASCENSION);
    }

    @Override
    public Role gridRole() {
        return Role.CONSUMER;
    }

    @Override
    public EnergyBuffer gridBuffer() {
        return port;
    }

    @Override
    public int gridMaxOut() {
        return 0;
    }

    @Override
    public int gridMaxIn() {
        return 0;
    }

    @Override
    public int workDraw() {
        return lastDraw;
    }

    @Override
    public boolean gridValid() {
        return !isRemoved();
    }

    @Override
    public String jadeGroup() {
        return "equipment";
    }

    @Override
    public void jadeLines(List<Component> out) {
        out.add(Component.translatable("jade.autarkia.forge." + status.name().toLowerCase(Locale.ROOT)));
        if (status == Status.FITTING) {
            out.add(Component.translatable("jade.autarkia.forge.progress", Component.translatable(phaseKey(tick)), tick * 100 / TOTAL_TICKS));
            out.add(Component.translatable("jade.autarkia.forge.fe", lastDraw));
        } else {
            out.add(Component.translatable("jade.autarkia.forge.fitted", fitted));
        }
    }

    @Override
    public float jadeProgress() {
        return status == Status.FITTING ? tick / (float) TOTAL_TICKS : -1;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<ForgeOfAscensionBlockEntity> arms = new AnimationController<>("arms", 4, test -> test.setAndContinue(IDLE));
        for (String t : TRIGGERS) {
            arms.triggerableAnim(t, RawAnimation.begin().thenPlay("animation.forge_of_ascension." + t));
        }
        controllers.add(arms);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        inv.serialize(out.child("items"));
        out.putInt("status", status.ordinal());
        out.putInt("tick", tick);
        out.putInt("fitted", fitted);
        out.putLong("drawn", drawn);
        if (fitter != null) {
            out.store("fitter", UUIDUtil.CODEC, fitter);
        }
        if (recipe != null) {
            out.store("recipe", Identifier.CODEC, recipe);
        }
        if (!held.isEmpty()) {
            out.store("held", ItemStack.CODEC, held);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("items").ifPresent(inv::deserialize);
        status = Status.values()[Math.max(0, Math.min(Status.values().length - 1, in.getIntOr("status", 0)))];
        tick = in.getIntOr("tick", 0);
        fitted = in.getIntOr("fitted", 0);
        drawn = in.getLongOr("drawn", 0);
        fitter = in.read("fitter", UUIDUtil.CODEC).orElse(null);
        recipe = in.read("recipe", Identifier.CODEC).orElse(null);
        held = in.read("held", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }
}

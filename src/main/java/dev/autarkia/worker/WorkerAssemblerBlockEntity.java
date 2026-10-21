package dev.autarkia.worker;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyEvents;
import dev.autarkia.colony.ColonyTiers;
import dev.autarkia.energy.EnergyBuffer;
import dev.autarkia.energy.GridMember;
import dev.autarkia.processing.MachineInput;
import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.AutarkiaRecipes;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntitySpawnReason;
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
 * The {@code worker_assembler} (06 sections 5.1 and 5.2): a 2x2x2 workshop with one frame slot and six part slots.
 * It starts when the domain Core tier and the cap and sub-cap have room and all parts are in, runs on FE from any cable
 * (progress pauses without power), checks the cap again at the end ("Worker cap full" holds), and the worker walks out
 * of the front face and unfolds.
 */
public class WorkerAssemblerBlockEntity extends AutarkiaBlockEntity implements Ticking, GeoBlockEntity, MenuProvider, GridMember, JadeInfo {
    /** Assembler states for the GUI and Jade. */
    public enum Status { NO_PARTS, RUNNING, NO_POWER, CAP_FULL, TIER_LOW, OUTSIDE }

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.worker_assembler.idle");
    private static final RawAnimation ASSEMBLE = RawAnimation.begin().thenLoop("animation.worker_assembler.assemble");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final MachineInventory inv = new MachineInventory(7, 0, s -> true, this::setChanged);
    private final EnergyBuffer energy = new EnergyBuffer(40000, 400, 0, this::setChanged);
    private int progress;
    private int duration;
    private int drawPerTick;
    private Status status = Status.NO_PARTS;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> duration;
                case 2 -> energy.stored();
                case 3 -> energy.capacity();
                default -> status.ordinal();
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public WorkerAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.WORKER_ASSEMBLER.get(), pos, state);
    }

    /** Return the inventory (frame slot 0, parts 1 to 6). */
    public MachineInventory inventory() {
        return inv;
    }

    /** Return the build stage 0 to 4 by progress (25/50/75/100 percent) for the chassis bone. */
    public int stage() {
        return duration == 0 ? 0 : Math.min(4, progress * 4 / duration);
    }

    /** Return the status. */
    public Status status() {
        return status;
    }

    private Optional<RecipeHolder<WorkerAssemblyRecipe>> recipe(ServerLevel level) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            items.add(inv.get(i));
        }
        MachineInput input = new MachineInput(items);
        return level.recipeAccess().recipeMap().byType(AutarkiaRecipes.WORKER_ASSEMBLY.get()).stream()
            .filter(h -> h.value().forMachine(WorkerAssemblyRecipe.ASSEMBLER) && h.value().matches(input, level)).findFirst();
    }

    @Override
    public void serverTick() {
        ServerLevel level = serverLevel();
        Optional<RecipeHolder<WorkerAssemblyRecipe>> found = recipe(level);
        Status before = status;
        if (found.isEmpty()) {
            status = Status.NO_PARTS;
            progress = 0;
            duration = 0;
        } else {
            WorkerAssemblyRecipe r = found.get().value();
            duration = r.time();
            drawPerTick = r.fePerTick();
            Colony c = Colonies.at(level, worldPosition);
            WorkerType type = WorkerType.of(r.entityType());
            if (c == null) {
                status = Status.OUTSIDE;
            } else if (c.tier < r.minTier() || c.tier < type.tier()) {
                status = Status.TIER_LOW;
            } else if (!capRoom(c, type)) {
                status = Status.CAP_FULL;
            } else if (progress >= duration) {
                finish(level, c, r, type);
            } else if (energy.stored() >= r.fePerTick()) {
                energy.drain(r.fePerTick());
                progress++;
                status = Status.RUNNING;
                if (progress % 30 == 0) {
                    level.playSound(null, worldPosition, dev.autarkia.registry.AutarkiaSounds.of("block.worker_assembler.work"),
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 1.2f);
                    Vec3 c2 = Vec3.atBottomCenterOf(worldPosition).add(frontOffset());
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c2.x, c2.y + 1.2, c2.z, 6, 0.3, 0.2, 0.3, 0.05);
                }
            } else {
                status = Status.NO_POWER;
            }
        }
        if (status != before || progress % 20 == 0) {
            syncNow();
        }
    }

    private static boolean capRoom(Colony c, WorkerType type) {
        return c.workerCost() + type.capCost() <= ColonyTiers.workerCap(c.tier) && c.workerCount(type.id()) < type.subCap(c.tier);
    }

    private Vec3 frontOffset() {
        Direction f = MachineBlock.front(getBlockState());
        Vec3 center = new Vec3(0.5, 0, 0.5);
        return switch (f) {
            case SOUTH -> new Vec3(-center.x, 0, -center.z);
            case EAST -> new Vec3(-center.z, 0, center.x);
            case WEST -> new Vec3(center.z, 0, -center.x);
            default -> center;
        };
    }

    private void finish(ServerLevel level, Colony c, WorkerAssemblyRecipe r, WorkerType type) {
        if (!(r.entityType().create(level, EntitySpawnReason.MOB_SUMMONED) instanceof WorkerEntity w)) {
            throw new IllegalStateException("Worker assembly recipe names " + r.worker() + ", which is not a worker entity");
        }
        r.frame().ifPresent(f -> inv.shrink(0, f.count()));
        for (var part : r.parts()) {
            for (int s = 1; s < 7; s++) {
                ItemStack st = inv.get(s);
                if (!st.isEmpty() && part.ingredient().test(st) && st.getCount() >= part.count()) {
                    inv.shrink(s, part.count());
                    break;
                }
            }
        }
        Direction front = MachineBlock.front(getBlockState());
        Vec3 spawn = Vec3.atBottomCenterOf(worldPosition).add(frontOffset()).add(front.getStepX() * 1.6, 0, front.getStepZ() * 1.6);
        w.snapTo(spawn.x, spawn.y, spawn.z, front.toYRot(), 0);
        level.addFreshEntity(w);
        w.bind(c);
        w.trigger("unfold");
        triggerAnim("main", "finish");
        level.playSound(null, worldPosition, dev.autarkia.registry.AutarkiaSounds.of("block.worker_assembler.done"),
            net.minecraft.sounds.SoundSource.BLOCKS, 0.7f, 1.4f);
        ColonyEvents.worker(c, "assembled:" + type.id().getPath());
        progress = 0;
        Autarkia.LOG.info("[worker] assembled {} at {}", type.id().getPath(), worldPosition.toShortString());
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (isCenter() && level != null) {
            for (int i = 0; i < inv.size(); i++) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, inv.get(i).copy());
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.autarkia.worker_assembler");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.WORKER_ASSEMBLER.get(), id, playerInv, worldPosition, inv, data, GuiLayouts.WORKER_ASSEMBLER);
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
        return energy.room() > 0 ? 400 : 0;
    }

    @Override
    public int workDraw() {
        return status == Status.RUNNING || status == Status.NO_POWER ? drawPerTick : 0;
    }

    @Override
    public boolean gridValid() {
        return !isRemoved();
    }

    /** Return the energy buffer. */
    public EnergyBuffer energy() {
        return energy;
    }

    @Override
    public String jadeGroup() {
        return "machine";
    }

    @Override
    public void jadeLines(List<Component> out) {
        out.add(Component.translatable("jade.autarkia.assembler." + status.name().toLowerCase(java.util.Locale.ROOT)));
        if (duration > 0) {
            out.add(Component.translatable("jade.autarkia.assembler.progress", progress * 100 / duration));
        }
        out.add(Component.translatable("jade.autarkia.buffer", energy.stored(), energy.capacity()));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<WorkerAssemblerBlockEntity> main = new AnimationController<>("main", 4,
            test -> test.setAndContinue(test.animatable().status == Status.RUNNING ? ASSEMBLE : IDLE));
        main.triggerableAnim("finish", RawAnimation.begin().thenPlay("animation.worker_assembler.finish"));
        controllers.add(main);
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
        out.putInt("progress", progress);
        out.putInt("duration", duration);
        out.putInt("status", status.ordinal());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("items").ifPresent(inv::deserialize);
        in.child("energy").ifPresent(energy::deserialize);
        progress = in.getIntOr("progress", 0);
        duration = in.getIntOr("duration", 0);
        status = Status.values()[Math.max(0, Math.min(Status.values().length - 1, in.getIntOr("status", 0)))];
    }
}

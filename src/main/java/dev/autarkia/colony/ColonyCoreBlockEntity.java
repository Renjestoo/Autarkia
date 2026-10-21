package dev.autarkia.colony;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.integration.AnthologyBridge;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.Ticking;
import dev.autarkia.worker.WorkerCapsuleItem;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Colony Core block entity (05 section 1.1): growth from the seed in 200 ticks (roots 0-60 with root particles,
 * frame 60-140, bloom 140-200 with sound and camera shake), then ACTIVE: the colony record goes active, the owner gets
 * the starter Porter capsule and the field manual, and {@code colony/root} fires. A dormant colony shows the dormant pose.
 */
public class ColonyCoreBlockEntity extends AutarkiaBlockEntity implements Ticking, GeoBlockEntity, JadeInfo {
    public static final int GROWTH_TICKS = 200;
    private static final RawAnimation ROOTS = RawAnimation.begin().thenPlayAndHold("animation.colony_core.roots");
    private static final RawAnimation FRAME = RawAnimation.begin().thenPlayAndHold("animation.colony_core.frame");
    private static final RawAnimation BLOOM = RawAnimation.begin().thenPlayAndHold("animation.colony_core.bloom");
    private static final RawAnimation ACTIVE = RawAnimation.begin().thenLoop("animation.colony_core.active");
    private static final RawAnimation DORMANT = RawAnimation.begin().thenLoop("animation.colony_core.dormant");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID colonyId;
    private int growth;
    private boolean active;
    private String name = "";
    private int tier = 1;

    public ColonyCoreBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.COLONY_CORE.get(), pos, state);
    }

    /** Link the Core to its colony (center only). */
    public void link(Colony c) {
        colonyId = c.id;
        growth = c.state == Colony.State.ACTIVE ? GROWTH_TICKS : 0;
        active = c.state == Colony.State.ACTIVE;
        name = c.name;
        tier = c.tier;
        syncNow();
    }

    /** Return the linked colony id. */
    public @Nullable UUID colonyId() {
        return colonyId;
    }

    /** Return the colony record, or null (client, or the record is gone). */
    public @Nullable Colony colony() {
        return colonyId == null || Colonies.server() == null ? null : Colonies.registry().get(colonyId);
    }

    @Override
    public void serverTick() {
        Colony c = colony();
        ServerLevel level = serverLevel();
        if (c == null || level == null) {
            return;
        }
        if (!name.equals(c.name) || tier != c.tier) {
            name = c.name;
            tier = c.tier;
            syncNow();
        }
        if (c.state != Colony.State.GROWING) {
            return;
        }
        Vec3 center = Vec3.atBottomCenterOf(worldPosition);
        if (growth < 60) {
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                double r = 4.0 * growth / 60.0;
                level.sendParticles(GlowParticleOptions.of(0xFF7A5230).size(0.15f), center.x + Math.cos(a) * r, center.y + 0.1,
                    center.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
            if (growth % 20 == 0) {
                level.playSound(null, worldPosition, AutarkiaSounds.of("core_grow_roots"), SoundSource.BLOCKS, 1f, 1f);
            }
        }
        if (growth == 140) {
            level.playSound(null, worldPosition, AutarkiaSounds.of("core_bloom"), SoundSource.BLOCKS, 1f, 1f);
            AnthologyBridge.shake(level, center, 0.3f, 20, 16f);
        }
        growth++;
        c.growth = growth;
        if (growth % 20 == 0 || growth == 60 || growth == 140) {
            syncNow();
        }
        if (growth >= GROWTH_TICKS) {
            bloom(level, c);
        }
    }

    private void bloom(ServerLevel level, Colony c) {
        c.state = Colony.State.ACTIVE;
        active = true;
        Colonies.rebuildIndex();
        Colonies.changed();
        syncNow();
        ColonyEvents.fire(c, "root");
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(c.owner);
        boolean fresh = c.stats.getOrDefault("starter_given", 0L) == 0;
        if (fresh) {
            c.stat("starter_given", 1);
            if (AutarkiaConfig.COLONY_STARTER_PORTER.get()) {
                drop(level, owner, WorkerCapsuleItem.filled(level, dev.autarkia.registry.AutarkiaEntities.PORTER.get()));
            }
            drop(level, owner, new ItemStack(AutarkiaItems.FIELD_MANUAL.get()));
        }
        Autarkia.LOG.info("[colony] {} bloomed at {} (T{} ACTIVE)", c.name, worldPosition.toShortString(), c.tier);
    }

    private void drop(ServerLevel level, @Nullable ServerPlayer owner, ItemStack stack) {
        if (owner != null && owner.distanceToSqr(Vec3.atCenterOf(worldPosition)) < 64 * 64 && owner.getInventory().add(stack)) {
            return;
        }
        net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 3.2, worldPosition.getZ() + 0.5, stack);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        Colony c = colony();
        if (c != null && isCenter() && level instanceof ServerLevel && c.corePos.equals(worldPosition)) {
            c.state = Colony.State.DORMANT;
            Planner.cancelAllJobs(c);
            Colonies.rebuildIndex();
            Colonies.changed();
            Autarkia.LOG.info("[colony] {} went dormant (Core removed at {})", c.name, worldPosition.toShortString());
        }
    }

    @Override
    public String jadeGroup() {
        return "colony";
    }

    @Override
    public void jadeLines(List<Component> out) {
        Colony c = colony();
        if (c == null) {
            out.add(Component.translatable("jade.autarkia.colony.growing", growth * 100 / GROWTH_TICKS));
            return;
        }
        ColonyRuntime rt = ColonyRuntime.of(c);
        long now = level == null ? 0 : level.getGameTime();
        out.add(Component.translatable("jade.autarkia.colony.name", c.name, c.tier));
        out.add(Component.translatable("jade.autarkia.colony.power", Math.round(rt.signal("S01", now, 0)), Math.round(rt.signal("S02", now, 0))));
        long activeCount = c.projects.stream().filter(p -> p.state.active()).count();
        long proposed = c.projects.stream().filter(p -> p.state == Project.State.PROPOSED).count();
        out.add(Component.translatable("jade.autarkia.colony.projects", activeCount, proposed));
        out.add(Component.translatable("jade.autarkia.colony.workers", c.workerCost(), ColonyTiers.workerCap(c.tier)));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ColonyCoreBlockEntity>("growth", 4, test -> {
            ColonyCoreBlockEntity be = test.animatable();
            if (be.active) {
                return test.setAndContinue(ACTIVE);
            }
            if (be.colonyId != null && be.growth >= GROWTH_TICKS) {
                return test.setAndContinue(DORMANT);
            }
            return test.setAndContinue(be.growth < 60 ? ROOTS : be.growth < 140 ? FRAME : BLOOM);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** Return the growth tick (0 to 200). */
    public int growth() {
        return growth;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (colonyId != null) {
            out.store("colony", UUIDUtil.CODEC, colonyId);
        }
        out.putInt("growth", growth);
        out.putBoolean("active", active);
        out.putString("name", name);
        out.putInt("tier", tier);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        colonyId = in.read("colony", UUIDUtil.CODEC).orElse(null);
        growth = in.getIntOr("growth", 0);
        active = in.getBooleanOr("active", false);
        name = in.getStringOr("name", "");
        tier = in.getIntOr("tier", 1);
    }
}

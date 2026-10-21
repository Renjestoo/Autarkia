package dev.autarkia.energy;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.logistics.LinkerRef;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Cryo node, a 1x3x1 superconductor connector with the center at the bottom (02 sections 2.1 and 4.6, decision G8):
 * 4 spans attached on the top block (+0.3), 4 coolant cell slots, and a use of 1 unit per tick plus 1 per attached
 * span with load above 0.5. The node is cold while its cells hold coolant. The player who last opened its menu is its
 * tender: a quench of one of its spans also grants that player {@code power/quench}.
 */
public class CryoNodeBlockEntity extends NodeBlockEntity implements Ticking, GeoBlockEntity, MenuProvider {
    public static final int MAX_SPANS = 4;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.cryo_node.idle");
    private static final RawAnimation WARM = RawAnimation.begin().thenLoop("animation.cryo_node.warm");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final CoolantSlots coolant = new CoolantSlots(4, this::setChanged);
    private final LinkerRef tender = new LinkerRef();
    private int lastUse = 1;
    private boolean wasCold;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> coolant.units();
                case 1 -> coolant.capacity();
                case 2 -> ticksLeft();
                case 3 -> spans().size();
                default -> {
                    List<Span> s = spans();
                    int k = i - 4;
                    yield k < s.size() ? Math.round(s.get(k).temp * 1000) : -1;
                }
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

    public CryoNodeBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.CRYO_NODE.get(), pos, state, CableTier.SUPERCONDUCTOR, MAX_SPANS);
    }

    /** Return the coolant cell slots. */
    public CoolantSlots coolant() {
        return coolant;
    }

    /** Return the player who last opened the menu (gets {@code power/quench} when a span of this node quenches). */
    public LinkerRef tender() {
        return tender;
    }

    /** Return true while the cells hold coolant (02 section 4.6). */
    public boolean cold() {
        return coolant.units() > 0;
    }

    /** Return the units used in the last tick: 1 plus 1 per attached span with load above 0.5. */
    public int lastUse() {
        return lastUse;
    }

    /** Return the ticks the coolant lasts at the last use. */
    public int ticksLeft() {
        return coolant.units() / Math.max(1, lastUse);
    }

    /** Return the spans attached to this node (server side; empty on the client). */
    public List<Span> spans() {
        if (!(level instanceof ServerLevel sl)) {
            return List.of();
        }
        GridSavedData d = Grid.of(sl).data();
        IntArrayList ids = d.at(worldPosition.asLong());
        List<Span> out = new java.util.ArrayList<>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            Span s = d.spans.get(ids.getInt(i));
            if (s != null) {
                out.add(s);
            }
        }
        return out;
    }

    /** Return the highest temperature T of the attached spans (0 without spans). */
    public float temperature() {
        float t = 0;
        for (Span s : spans()) {
            t = Math.max(t, s.temp);
        }
        return t;
    }

    @Override
    public void serverTick() {
        int hot = 0;
        for (Span s : spans()) {
            if (s.load > 0.5f) {
                hot++;
            }
        }
        lastUse = 1 + hot;
        coolant.draw(lastUse);
        boolean cold = cold();
        if (cold != wasCold) {
            wasCold = cold;
            syncNow();
        }
    }

    @Override
    public void jadeLines(List<Component> out) {
        super.jadeLines(out);
        out.add(Component.translatable("jade.autarkia.coolant", ticksLeft() / 20));
        out.add(Component.translatable("jade.autarkia.temperature", Math.round(temperature() * 100)));
    }

    @Override
    public float jadeProgress() {
        return coolant.units() / (float) coolant.capacity();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide() && isCenter()) {
            for (int i = 0; i < coolant.size(); i++) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, coolant.get(i).copy());
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<CryoNodeBlockEntity>("frost", 10, test -> test.setAndContinue(test.animatable().cold() ? IDLE : WARM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.autarkia.cryo_node");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        if (player instanceof ServerPlayer sp) {
            tender.set(sp);
        }
        return new SlotMenu(AutarkiaMenus.CRYO_NODE.get(), id, inv, worldPosition, coolant, data, GuiLayouts.CRYO_NODE);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        coolant.serialize(out.child("coolant"));
        tender.save(out);
        out.putInt("use", lastUse);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("coolant").ifPresent(coolant::deserialize);
        tender.load(in);
        lastUse = in.getIntOr("use", 1);
        wasCold = cold();
    }
}

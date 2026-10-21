package dev.autarkia.processing;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.AutarkiaRecipes;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.SlotMenu;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Fabricator, 2x2x1 (03 sections 4.1 and 4.2): 160 FE/t, recipe time (default 200). The player or the Core picks a
 * target {@code autarkia:fabricating} recipe; the machine pulls its ingredients from the 3 x 3 buffer and stays idle
 * while one is missing. Menu buttons: 0 next target, 1 previous target, 2 no target. Rating (decision G13): a target
 * recipe whose draw ceil(FE / ticks) is above 160 FE/t runs at its own draw, so the buffer holds 200 x the draw and the
 * input (grid and energy capability) is 4 x the draw, at least 32,000 FE and 640 FE/t.
 */
public class FabricatorBlockEntity extends MachineBlockEntity implements GeoBlockEntity, dev.autarkia.registry.MenuButtons {
    private static final RawAnimation WORK = RawAnimation.begin().thenLoop("animation.fabricator.work");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.fabricator.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static final int RATING = 160;
    private @Nullable Identifier target;
    private int maxIn = RATING * 4;

    public FabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.FABRICATOR.get(), pos, state, 9, 1, 160);
    }

    /** Return the fabricating recipes sorted by id (server and client build the same list). */
    public static List<RecipeHolder<MachineRecipe>> targets(RecipeMap map) {
        List<RecipeHolder<MachineRecipe>> list = new ArrayList<>(map.byType(AutarkiaRecipes.type(MachineRecipeKind.FABRICATING)));
        list.sort(Comparator.comparing(h -> h.id().identifier().toString()));
        return list;
    }

    /** Return the target recipe id, or null. */
    public @Nullable Identifier target() {
        return target;
    }

    /** Set the target recipe (null for none); the Core uses this to order components. */
    public void setTarget(@Nullable Identifier id) {
        target = id;
        progress = 0;
        setChanged();
    }

    @Override
    public boolean menuButton(Player player, int button) {
        if (button < 0 || button > 2) {
            return false;
        }
        List<RecipeHolder<MachineRecipe>> list = targets(serverLevel().recipeAccess().recipeMap());
        if (button == 2 || list.isEmpty()) {
            setTarget(null);
            return true;
        }
        int at = target == null ? -1 : indexOf(list, target);
        int next = button == 0 ? at + 1 : at < 0 ? list.size() - 1 : at - 1;
        setTarget(next < 0 || next >= list.size() ? null : list.get(next).id().identifier());
        return true;
    }

    private static int indexOf(List<RecipeHolder<MachineRecipe>> list, Identifier id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().identifier().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void serverTick() {
        int draw = targetDraw(serverLevel());
        if (energy != null) {
            energy.setCapacity(draw * 200);
            energy.setMaxInsert(draw * 4);
        }
        maxIn = draw * 4;
        super.serverTick();
    }

    /** Return the per-tick draw the fabricator is rated for now: the target recipe's draw, at least {@link #RATING}. */
    public int targetDraw(ServerLevel sl) {
        if (target == null) {
            return RATING;
        }
        for (RecipeHolder<MachineRecipe> h : sl.recipeAccess().recipeMap().byType(AutarkiaRecipes.type(MachineRecipeKind.FABRICATING))) {
            if (h.id().identifier().equals(target)) {
                return Math.max(RATING, (int) Math.ceil(h.value().energyOr(defaultEnergy())
                    * dev.autarkia.config.AutarkiaConfig.PROCESSING_ENERGY_MULTIPLIER.get()));
            }
        }
        return RATING;
    }

    @Override
    public int gridMaxIn() {
        return maxIn;
    }

    @Override
    protected @Nullable MachineRecipe selectRecipe(ServerLevel sl, MachineInput input) {
        if (target == null) {
            return null;
        }
        for (RecipeHolder<MachineRecipe> h : sl.recipeAccess().recipeMap().byType(AutarkiaRecipes.type(MachineRecipeKind.FABRICATING))) {
            if (h.id().identifier().equals(target)) {
                return h.value().slotMatch(input) != null ? h.value() : null;
            }
        }
        return null;
    }

    @Override
    protected MachineRecipeKind kind() {
        return MachineRecipeKind.FABRICATING;
    }

    @Override
    protected int defaultTime() {
        return 200;
    }

    @Override
    protected int defaultEnergy() {
        return 160;
    }

    @Override
    protected float workRate() {
        return 1;
    }

    @Override
    protected void onOperation(MachineRecipe recipe) {
        serverLevel().playSound(null, worldPosition, SoundEvents.CRAFTER_CRAFT, SoundSource.BLOCKS, 0.7f, 1f);
    }

    @Override
    protected int extraData(int i) {
        if (!(level instanceof ServerLevel sl) || target == null) {
            return 0;
        }
        return indexOf(targets(sl.recipeAccess().recipeMap()), target) + 1;
    }

    @Override
    protected int extraDataCount() {
        return 1;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.FABRICATOR.get(), id, playerInv, worldPosition, inv, data, GuiLayouts.FABRICATOR);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<FabricatorBlockEntity>("main", 0, test -> test.setAndContinue(test.animatable().running() ? WORK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (target != null) {
            out.putString("target", target.toString());
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        target = in.getString("target").map(Identifier::parse).orElse(null);
    }
}

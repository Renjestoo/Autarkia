package dev.autarkia.processing;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.SlotMenu;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Jaw crusher, 2x2x2 heavy machine (03 sections 4.1 and 4.2): crushing at 192 FE/t, 100 ticks, 1 raw ore to 2 crushed
 * ore, cobblestone to gravel and gravel to sand. Every bottom position needs a foundation below (checked every 20
 * ticks), else NO_CONDITION. Each operation plays a stone break sound with block particles of the input at the jaws.
 */
public class JawCrusherBlockEntity extends MachineBlockEntity implements GeoBlockEntity {
    private static final RawAnimation CRUSH = RawAnimation.begin().thenLoop("animation.jaw_crusher.crush");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.jaw_crusher.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean foundation;
    private boolean foundationKnown;
    private ItemStack crushing = ItemStack.EMPTY;

    public JawCrusherBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.JAW_CRUSHER.get(), pos, state, 1, 2, 192);
    }

    @Override
    protected MachineRecipeKind kind() {
        return MachineRecipeKind.CRUSHING;
    }

    @Override
    protected int defaultTime() {
        return 100;
    }

    @Override
    protected int defaultEnergy() {
        return 192;
    }

    @Override
    protected float workRate() {
        if (level.getGameTime() % 20 == 0 || !foundationKnown) {
            foundation = onFoundation();
            foundationKnown = true;
        }
        crushing = inv.get(0).copyWithCount(1);
        return foundation ? 1 : 0;
    }

    @Override
    protected void onOperation(MachineRecipe recipe) {
        ServerLevel sl = serverLevel();
        ItemStack in = crushing;
        sl.playSound(null, worldPosition, level.getRandom().nextBoolean() ? SoundEvents.STONE_BREAK : SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS,
            0.7f, 0.9f);
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 1.6;
        double z = worldPosition.getZ() + 0.5;
        if (in.getItem() instanceof BlockItem bi) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, bi.getBlock().defaultBlockState()), x, y, z, 12, 0.3, 0.1, 0.3, 0.05);
        } else if (!in.isEmpty()) {
            sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, in.getItem()), x, y, z, 12, 0.3, 0.1, 0.3, 0.05);
        }
    }

    @Override
    public void jadeLines(List<Component> out) {
        super.jadeLines(out);
        if (!foundation) {
            out.add(Component.translatable("jade.autarkia.no_foundation"));
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.JAW_CRUSHER.get(), id, playerInv, worldPosition, inv, data, GuiLayouts.JAW_CRUSHER);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<JawCrusherBlockEntity>("main", 0, test -> test.setAndContinue(test.animatable().running() ? CRUSH : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

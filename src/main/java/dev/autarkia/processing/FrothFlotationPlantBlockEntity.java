package dev.autarkia.processing;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.SlotMenu;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import org.jspecify.annotations.Nullable;

/**
 * Froth flotation plant, 3x3x3 heavy machine (03 section 4): {@code autarkia:flotation} at 768 FE/t, 200 ticks, 1 crushed
 * ore plus the recipe water (250 mB) to 2 concentrate, slag and a secondary nugget. Slots: input 0, water bucket 1
 * (the empty bucket goes back into the same slot and can be extracted), outputs 2 to 4. Water tank 8,000 mB, filled
 * from the bucket slot, through the fluid capability (water only) or, with {@code flotation.pumpFromSource}, at 50 mB
 * per tick while a water source touches a bottom position (the source stays). Without water for the next run or
 * without a foundation under each of the 9 bottom positions (checked every 20 ticks) the status is NO_CONDITION.
 * While it works, bubbles pop at the top and the upward bubble column sound loops.
 */
public class FrothFlotationPlantBlockEntity extends MachineBlockEntity implements GeoBlockEntity {
    /** Tank capacity in mB. */
    public static final int TANK = 8000;
    /** Water pumped per tick from an adjacent source, in mB. */
    public static final int PUMP = 50;
    /** Slot index of the water bucket. */
    public static final int BUCKET_SLOT = 1;
    private static final RawAnimation AGITATE = RawAnimation.begin().thenLoop("animation.froth_flotation_plant.agitate");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.froth_flotation_plant.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final FluidStacksResourceHandler water = new FluidStacksResourceHandler(1, TANK) {
        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.is(Fluids.WATER);
        }

        @Override
        protected void onContentsChanged(int index, net.neoforged.neoforge.fluids.FluidStack previous) {
            setChanged();
        }
    };
    private final ResourceHandler<ItemResource> automation = new Automation();
    private @Nullable MachineRecipe current;
    private boolean foundation;
    private boolean source;
    private boolean surveyed;
    private long waterUsed;

    public FrothFlotationPlantBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.FROTH_FLOTATION_PLANT.get(), pos, state, 2, 3, 768);
    }

    @Override
    protected MachineInventory createInventory(int inputs, int outputs) {
        return new MachineInventory(inputs, outputs, this::acceptsInput, this::setChanged) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return index == BUCKET_SLOT ? resource.is(Items.WATER_BUCKET) : super.isValid(index, resource);
            }

            @Override
            public int queue() {
                return get(0).getCount();
            }
        };
    }

    /** Return the water tank (fluid capability of every face; water only). */
    public FluidStacksResourceHandler water() {
        return water;
    }

    /** Return the item handler other blocks see: input and bucket slot in, outputs and the empty bucket out. */
    public ResourceHandler<ItemResource> automation() {
        return automation;
    }

    /** Return the stored water in mB. */
    public int waterAmount() {
        return (int) water.getAmountAsLong(0);
    }

    /** Return the total water in mB the finished operations used. */
    public long waterUsed() {
        return waterUsed;
    }

    private void setWater(int amount) {
        water.set(0, amount <= 0 ? FluidResource.EMPTY : FluidResource.of(Fluids.WATER), Math.max(0, amount));
    }

    @Override
    protected MachineRecipeKind kind() {
        return MachineRecipeKind.FLOTATION;
    }

    @Override
    protected int defaultTime() {
        return 200;
    }

    @Override
    protected int defaultEnergy() {
        return 768;
    }

    @Override
    protected MachineInput input() {
        return new MachineInput(List.of(inv.get(0)));
    }

    @Override
    protected @Nullable MachineRecipe selectRecipe(ServerLevel sl, MachineInput input) {
        current = super.selectRecipe(sl, input);
        return current;
    }

    @Override
    protected float workRate() {
        return foundation && current != null && waterAmount() >= current.water() ? 1 : 0;
    }

    @Override
    protected void onOperation(MachineRecipe recipe) {
        setWater(waterAmount() - recipe.water());
        waterUsed += recipe.water();
    }

    private void survey() {
        Set<BlockPos> parts = ((BaseMultiblock) getBlockState().getBlock()).getMultiblockShape(level, worldPosition, getBlockState()).getGlobalPositions();
        foundation = onFoundation();
        source = false;
        for (BlockPos p : parts) {
            if (p.getY() != worldPosition.getY()) {
                continue;
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (!parts.contains(n) && level.getFluidState(n).isSourceOfType(Fluids.WATER)) {
                    source = true;
                }
            }
        }
        surveyed = true;
    }

    private void fillWater() {
        if (inv.get(BUCKET_SLOT).is(Items.WATER_BUCKET) && waterAmount() + 1000 <= TANK) {
            setWater(waterAmount() + 1000);
            inv.put(BUCKET_SLOT, new ItemStack(Items.BUCKET));
        }
        if (source && AutarkiaConfig.FLOTATION_PUMP_FROM_SOURCE.get() && waterAmount() < TANK) {
            setWater(Math.min(TANK, waterAmount() + PUMP));
        }
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        long now = sl.getGameTime();
        if (now % 20 == 0 || !surveyed) {
            survey();
        }
        fillWater();
        super.serverTick();
        if (!running()) {
            return;
        }
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 3.05;
        double z = worldPosition.getZ() + 0.5;
        if (now % 4 == 0) {
            sl.sendParticles(ParticleTypes.BUBBLE, x, y, z, 4, 0.7, 0.05, 0.7, 0.05);
            sl.sendParticles(ParticleTypes.BUBBLE_POP, x, y, z, 3, 0.7, 0.05, 0.7, 0.02);
        }
        if (now % 40 == 0) {
            sl.playSound(null, worldPosition.above(2), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.7f, 1f);
        }
    }

    @Override
    public void jadeLines(List<Component> out) {
        super.jadeLines(out);
        out.add(Component.translatable("jade.autarkia.flotation_water", String.format(Locale.ROOT, "%,d", waterAmount()),
            String.format(Locale.ROOT, "%,d", TANK)));
        if (!foundation) {
            out.add(Component.translatable("jade.autarkia.no_foundation"));
        }
    }

    @Override
    protected int extraData(int i) {
        return waterAmount();
    }

    @Override
    protected int extraDataCount() {
        return 1;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.FROTH_FLOTATION_PLANT.get(), id, playerInv, worldPosition, inv, data, GuiLayouts.FROTH_FLOTATION_PLANT);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<FrothFlotationPlantBlockEntity>("main", 4,
            test -> test.setAndContinue(test.animatable().running() ? AGITATE : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        water.serialize(out.child("water"));
        out.putLong("water_used", waterUsed);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("water").ifPresent(water::deserialize);
        waterUsed = in.getLongOr("water_used", 0);
    }

    private final class Automation implements ResourceHandler<ItemResource> {
        @Override
        public int size() {
            return inv.size();
        }

        @Override
        public ItemResource getResource(int index) {
            return inv.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            return inv.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return inv.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return inv.isValid(index, resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
            return inv.automation().insert(index, resource, amount, tx);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
            return index == BUCKET_SLOT && resource.is(Items.BUCKET) ? inv.extract(index, resource, amount, tx)
                : inv.automation().extract(index, resource, amount, tx);
        }
    }
}

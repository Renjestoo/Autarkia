package dev.autarkia.energy;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.integration.AnthologyBridge;
import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.Ticking;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import org.jspecify.annotations.Nullable;

/**
 * Boiler, 2x3x2 (02 section 6.2, W9). Water tank 8,000 mB (bucket slot, fluid capability on every face, +25 mB/t per
 * water source next to the bottom layer). Fire level F (0 to 4, GUI) burns F fuel ticks per tick from the fuel slot
 * (burn time from {@code minecraft:cooking_fuel}). T rises 0.25 F per tick while burning and falls 0.05 per tick when
 * not; with water it stops at 100 C, dry it rises to 250 C. At T >= 100 with water it makes 10 F mB steam per tick.
 * P = steam / 400 bar. Each pressure valve on the top vents 25 mB/t above 9 bar; at 10 bar the gauge ticks and steam
 * leaks, at 11 the camera shakes. It bursts at the burst pressure or after 200 ticks at 200 C or more: the 12
 * positions fall as 4 bricks and 8 gravel, an explosion without terrain damage, a steam cloud. Max 2 steam engines.
 */
public class BoilerBlockEntity extends AutarkiaBlockEntity implements Ticking, GeoBlockEntity, MenuProvider, JadeInfo {
    public static final int TANK = 8000;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.boiler.idle");
    private static final RawAnimation BURNING = RawAnimation.begin().thenLoop("animation.boiler.burning");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final MachineInventory inv = new MachineInventory(2, 1, s -> true, this::setChanged) {
        @Override
        public boolean isValid(int index, net.neoforged.neoforge.transfer.item.ItemResource resource) {
            return index == 0 ? resource.toStack(1).has(DataComponents.COOKING_FUEL) && !resource.is(Items.WATER_BUCKET)
                : index == 1 && resource.is(Items.WATER_BUCKET);
        }
    };
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
    private int fire;
    private int burn;
    private double temp = 20;
    private int steam;
    private int dryTicks;
    private int venting;
    private boolean high;
    private boolean suppressDrops;
    private final Map<BlockPos, Long> engines = new HashMap<>();
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> waterAmount();
                case 1 -> steam;
                case 2 -> (int) Math.round(pressure() * 100);
                case 3 -> (int) Math.round(temp);
                case 4 -> fire;
                case 5 -> burning() ? 1 : 0;
                default -> venting;
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.BOILER.get(), pos, state);
    }

    /** Return the water tank (fluid capability of every face). */
    public FluidStacksResourceHandler water() {
        return water;
    }

    /** Return the slot inventory: fuel 0, water bucket 1, empty bucket 2. */
    public MachineInventory inventory() {
        return inv;
    }

    /** Return the stored water in mB. */
    public int waterAmount() {
        return (int) water.getAmountAsLong(0);
    }

    private void setWater(int amount) {
        water.set(0, amount <= 0 ? FluidResource.EMPTY : FluidResource.of(Fluids.WATER), Math.max(0, amount));
    }

    /** Return the pressure in bar. */
    public double pressure() {
        return steam / 400.0;
    }

    /** Return the temperature in degrees C. */
    public double temperature() {
        return temp;
    }

    /** Return the stored steam in mB. */
    public int steam() {
        return steam;
    }

    /** Return the number of valves that vented in the last tick. */
    public int venting() {
        return venting;
    }

    /** Return the fire level 0 to 4. */
    public int fire() {
        return fire;
    }

    /** Set the fire level (GUI buttons), clamped to 0 to 4. */
    public void setFire(int level) {
        fire = Math.max(0, Math.min(4, level));
        syncNow();
    }

    /** Return true while fuel burns. */
    public boolean burning() {
        return fire > 0 && burn > 0;
    }

    /** Return the blocks above the top layer of a boiler with this center and state (the valve spots), placed or planned. */
    public static List<BlockPos> tops(Level level, BlockPos center, BlockState state) {
        List<BlockPos> all = new ArrayList<>(((BaseMultiblock) state.getBlock()).getMultiblockShape(level, center, state).getGlobalPositions());
        int top = all.stream().mapToInt(BlockPos::getY).max().orElse(center.getY());
        return all.stream().filter(p -> p.getY() == top).map(BlockPos::above).sorted(java.util.Comparator.comparingLong(BlockPos::asLong)).toList();
    }

    /** Return every position of the boiler. */
    public List<BlockPos> positions() {
        return new ArrayList<>(((BaseMultiblock) getBlockState().getBlock()).getMultiblockShape(level, worldPosition, getBlockState()).getGlobalPositions());
    }

    /**
     * Give steam to the engine whose center is at pos: up to max mB per tick, and only to 2 engines (an engine counts
     * while it drew steam in the last 20 ticks).
     */
    public int drawSteam(BlockPos engine, int max) {
        long now = level.getGameTime();
        engines.values().removeIf(t -> now - t > 20);
        if (!engines.containsKey(engine) && engines.size() >= 2) {
            return 0;
        }
        engines.put(engine.immutable(), now);
        int take = Math.min(max, steam);
        steam -= take;
        return take;
    }

    private int valves() {
        int n = 0;
        for (BlockPos p : positions()) {
            if (level.getBlockState(p.above()).is(AutarkiaBlocks.PRESSURE_VALVE.get())) {
                n++;
            }
        }
        return n;
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        long now = sl.getGameTime();
        fillWater();
        if (fire > 0 && burn <= 0) {
            ItemStack fuel = inv.get(0);
            if (fuel.has(DataComponents.COOKING_FUEL)) {
                int t = fuelTicks(sl, fuel);
                if (t > 0) {
                    var remainder = fuel.getCraftingRemainder();
                    ItemStack rest = remainder == null ? ItemStack.EMPTY : remainder.create();
                    inv.shrink(0, 1);
                    if (inv.get(0).isEmpty() && !rest.isEmpty()) {
                        inv.put(0, rest);
                    }
                    burn += t;
                }
            }
        }
        boolean wasBurning = burning();
        if (burning()) {
            burn = Math.max(0, burn - fire);
            temp += 0.25 * fire;
        } else {
            temp = Math.max(20, temp - 0.05);
        }
        int w = waterAmount();
        temp = Math.min(w > 0 ? 100 : 250, temp);
        if (temp >= 100 && fire > 0 && w >= 10 * fire && burning()) {
            steam += 10 * fire;
            setWater(w - 10 * fire);
        }
        int vents = valves();
        venting = 0;
        if (pressure() > 9 && vents > 0) {
            int v = Math.min(steam, 25 * vents);
            steam -= v;
            venting = vents;
            if (now % 20 == 0) {
                sl.playSound(null, worldPosition.above(3), AutarkiaSounds.of("valve.scream"), SoundSource.BLOCKS, 0.9f, 1.9f);
                for (BlockPos p : positions()) {
                    if (sl.getBlockEntity(p.above()) instanceof PressureValveBlockEntity valve) {
                        valve.vent();
                    }
                }
            }
            if (now % 4 == 0) {
                Vec3 top = Vec3.atCenterOf(worldPosition.above(3));
                sl.sendParticles(ParticleTypes.CLOUD, top.x, top.y, top.z, 3, 0.2, 0.1, 0.2, 0.05);
            }
        }
        warnings(sl, now);
        dryTicks = temp >= 200 ? dryTicks + 1 : 0;
        if (AutarkiaConfig.BOILER_BURST_ENABLED.get() && (pressure() >= AutarkiaConfig.BOILER_BURST_PRESSURE.get() || dryTicks >= 200)) {
            burst(sl);
            return;
        }
        if (wasBurning != burning() || now % 10 == 0) {
            syncNow();
        }
    }

    private int fuelTicks(ServerLevel sl, ItemStack fuel) {
        return net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.getFromItem(fuel, DataComponents.COOKING_FUEL,
            f -> f.burnTime(), getLootContext(sl, fuel), 0);
    }

    private net.minecraft.world.level.storage.loot.LootContext getLootContext(ServerLevel sl, ItemStack fuel) {
        net.minecraft.world.level.storage.loot.LootParams params = new net.minecraft.world.level.storage.loot.LootParams.Builder(sl)
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY, this)
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, getBlockState())
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.CONTAINER, new net.minecraft.world.SimpleContainer(fuel))
            .withParameter(net.neoforged.neoforge.common.loot.NeoForgeLootContextParams.QUERIED_STACK, fuel)
            .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CONTAINER_PROCESS);
        return new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(java.util.Optional.empty());
    }

    private void fillWater() {
        ItemStack bucket = inv.get(1);
        if (bucket.is(Items.WATER_BUCKET) && waterAmount() + 1000 <= TANK && (inv.get(2).isEmpty() || inv.get(2).is(Items.BUCKET)
            && inv.get(2).getCount() < inv.get(2).getMaxStackSize())) {
            setWater(waterAmount() + 1000);
            inv.shrink(1, 1);
            inv.put(2, new ItemStack(Items.BUCKET, inv.get(2).getCount() + 1));
        }
        int sources = 0;
        for (BlockPos p : positions()) {
            if (p.getY() != worldPosition.getY()) {
                continue;
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (level.getFluidState(n).is(Fluids.WATER) && level.getFluidState(n).isSource()) {
                    sources++;
                }
            }
        }
        if (sources > 0) {
            setWater(Math.min(TANK, waterAmount() + 25 * sources));
        }
    }

    private void warnings(ServerLevel sl, long now) {
        double p = pressure();
        if (p >= 10) {
            if (now % 4 == 0) {
                sl.playSound(null, worldPosition, AutarkiaSounds.of("boiler.gauge_tick"), SoundSource.BLOCKS, 0.3f, 1.6f);
                Vec3 c = center();
                sl.sendParticles(ParticleTypes.WHITE_SMOKE, c.x, c.y + 0.5, c.z, 2, 0.6, 0.6, 0.6, 0.01);
            }
            if (!high) {
                high = true;
                dev.autarkia.colony.ColonyMemory.at(sl, worldPosition, "boiler_high", worldPosition.toShortString());
            }
        } else if (p < 9) {
            high = false;
        }
        if (p >= 11 && now % 20 == 0) {
            AnthologyBridge.shake(sl, center(), 0.2f, 10, 8f);
        }
    }

    private Vec3 center() {
        List<BlockPos> ps = positions();
        double x = 0;
        double y = 0;
        double z = 0;
        for (BlockPos p : ps) {
            x += p.getX() + 0.5;
            y += p.getY() + 0.5;
            z += p.getZ() + 0.5;
        }
        return new Vec3(x / ps.size(), y / ps.size(), z / ps.size());
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (isCenter() && level != null) {
            for (int i = 0; i < inv.size(); i++) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, inv.get(i).copy());
                inv.put(i, ItemStack.EMPTY);
            }
        }
    }

    /** Return true while a burst removes the boiler (its positions drop nothing). */
    public boolean suppressDrops() {
        return suppressDrops;
    }

    /** Burst the boiler (02 section 6.2 burst sequence). */
    public void burst(ServerLevel sl) {
        Vec3 c = center();
        List<BlockPos> ps = positions();
        int bottom = worldPosition.getY();
        double p = pressure();
        suppressDrops = true;
        for (BlockPos pos : ps) {
            sl.destroyBlock(pos, false);
        }
        var rnd = sl.getRandom();
        for (BlockPos pos : ps) {
            BlockState fall = pos.getY() == bottom ? Blocks.BRICKS.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
            sl.setBlock(pos, fall, net.minecraft.world.level.block.Block.UPDATE_ALL);
            FallingBlockEntity fb = FallingBlockEntity.fall(sl, pos, fall);
            fb.setDeltaMovement(rnd.nextDouble() * 0.7 - 0.35, 0.5 + rnd.nextDouble() * 0.4, rnd.nextDouble() * 0.7 - 0.35);
        }
        sl.explode(null, c.x, c.y, c.z, 2.0F, false, Level.ExplosionInteraction.NONE);
        sl.sendParticles(ParticleTypes.CLOUD, c.x, c.y, c.z, 80, 1.5, 1.5, 1.5, 0.15);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 20, 1.0, 1.0, 1.0, 0.05);
        sl.playSound(null, BlockPos.containing(c), AutarkiaSounds.of("boiler.burst"), SoundSource.BLOCKS, 1.2f, 0.7f);
        dev.autarkia.colony.ColonyMemory.at(sl, worldPosition, "boiler_burst", worldPosition.toShortString());
        dev.autarkia.registry.AutarkiaAdvancements.awardNear(sl, c, "power/boiler_burst");
        dev.autarkia.Autarkia.LOG.info("[boiler] burst at {} with {} bar", worldPosition.toShortString(), String.format(java.util.Locale.ROOT, "%.2f", p));
    }

    @Override
    public String jadeGroup() {
        return "power";
    }

    @Override
    public void jadeLines(List<Component> out) {
        out.add(Component.translatable("jade.autarkia.pressure", String.format(java.util.Locale.ROOT, "%.1f", pressure())));
        out.add(Component.translatable("jade.autarkia.water", waterAmount()));
        out.add(Component.translatable("jade.autarkia.fire_level", fire));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.autarkia.boiler");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new BoilerMenu(id, playerInv, worldPosition, inv, data);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<BoilerBlockEntity>("fire", 4, test -> test.setAndContinue(test.animatable().burning() ? BURNING : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        inv.serialize(out.child("items"));
        water.serialize(out.child("water"));
        out.putInt("fire", fire);
        out.putInt("burn", burn);
        out.putDouble("temp", temp);
        out.putInt("steam", steam);
        out.putInt("dry", dryTicks);
        out.putBoolean("burning", burning());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("items").ifPresent(inv::deserialize);
        in.child("water").ifPresent(water::deserialize);
        fire = in.getIntOr("fire", 0);
        burn = in.getIntOr("burn", 0);
        temp = in.getDoubleOr("temp", 20);
        steam = in.getIntOr("steam", 0);
        dryTicks = in.getIntOr("dry", 0);
    }
}

package dev.autarkia.energy;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import net.nikdo53.tinymultiblocklib.blockentities.IMultiBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Steam turbine, 3x3x2 (02 sections 6.1 and 6.2): its back and side faces take steam from 1 to 4 touching boilers, up to
 * 100 mB per tick split equally, only while its 20,000 FE buffer has 1,000 FE of room. The rotor r spins up by 0.0017
 * per tick while it gets at least 10 mB per tick and down by 0.00083 per tick without; output = r x steam x 10 FE/t
 * (max 1,000), out 1,024 FE/t.
 */
public class SteamTurbineBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, GeoBlockEntity, JadeInfo, MenuProvider {
    public static final int MAX_STEAM = 100;
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.steam_turbine.spin");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.steam_turbine.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer buffer = new EnergyBuffer(20000, 0, 1024, this::setChanged);
    private float rotor;
    private int steamRate;
    private int output;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> Math.round(rotor * 1000);
                case 1 -> 1000;
                case 2 -> buffer.stored();
                case 3 -> buffer.capacity();
                case 4 -> steamRate;
                default -> output;
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

    public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.STEAM_TURBINE.get(), pos, state);
    }

    /** Return the boilers touching the back and side faces (at most 4, nearest first). */
    public List<BoilerBlockEntity> boilers() {
        Direction front = MachineBlock.front(getBlockState());
        List<BoilerBlockEntity> out = new ArrayList<>();
        java.util.Set<BlockPos> parts = ((BaseMultiblock) getBlockState().getBlock()).getMultiblockShape(level, worldPosition, getBlockState())
            .getGlobalPositions();
        for (BlockPos p : parts) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (d == front) {
                    continue;
                }
                BlockPos n = p.relative(d);
                if (parts.contains(n)) {
                    continue;
                }
                BlockPos c = level.getBlockEntity(n) instanceof IMultiBlockEntity m ? m.getCenter() : n;
                if (level.getBlockEntity(c) instanceof BoilerBlockEntity b && !out.contains(b)) {
                    out.add(b);
                }
            }
        }
        out.sort(java.util.Comparator.comparingDouble((BoilerBlockEntity b) -> b.getBlockPos().distSqr(worldPosition))
            .thenComparingLong(b -> b.getBlockPos().asLong()));
        return out.size() > 4 ? out.subList(0, 4) : out;
    }

    /** Return the rotor speed 0..1. */
    public float rotor() {
        return rotor;
    }

    /** Return the steam drawn in the last tick in mB. */
    public int steamRate() {
        return steamRate;
    }

    /** Return the FE made in the last tick. */
    public int output() {
        return output;
    }

    @Override
    public void serverTick() {
        boolean wasSpinning = rotor > 0;
        steamRate = 0;
        List<BoilerBlockEntity> list = boilers();
        if (buffer.room() >= 1000 && !list.isEmpty()) {
            int n = list.size();
            for (int i = 0; i < n; i++) {
                steamRate += list.get(i).drawSteam(worldPosition, MAX_STEAM / n + (i < MAX_STEAM % n ? 1 : 0));
            }
        }
        rotor = steamRate >= 10 ? Math.min(1f, rotor + 0.0017f) : Math.max(0f, rotor - 0.00083f);
        output = Math.round(rotor * steamRate * 10);
        buffer.fill(output);
        if (rotor > 0 && level.getGameTime() % 20 == 0) {
            level.playSound(null, worldPosition, AutarkiaSounds.of("turbine.whine"), SoundSource.BLOCKS, 0.4f, 0.5f + rotor);
        }
        if (wasSpinning != rotor > 0 || level.getGameTime() % 40 == 0) {
            syncThrottled(20);
        }
    }

    /** Return the buffer (extract only). */
    public EnergyBuffer buffer() {
        return buffer;
    }

    @Override
    public Role gridRole() {
        return Role.GENERATOR;
    }

    @Override
    public EnergyBuffer gridBuffer() {
        return buffer;
    }

    @Override
    public int gridMaxOut() {
        return 1024;
    }

    @Override
    public int gridMaxIn() {
        return 0;
    }

    @Override
    public boolean gridValid() {
        return !isRemoved();
    }

    @Override
    public String jadeGroup() {
        return "power";
    }

    @Override
    public void jadeLines(List<Component> out) {
        out.add(Component.translatable("jade.autarkia.output", output));
        out.add(Component.translatable("jade.autarkia.buffer", buffer.stored(), buffer.capacity()));
        out.add(Component.translatable("jade.autarkia.turbine.rotor", Math.round(rotor * 100)));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SteamTurbineBlockEntity>("rotor", 4, test -> {
            SteamTurbineBlockEntity be = test.animatable();
            test.setControllerSpeed(Math.max(0.05f, be.rotor * 2f));
            return test.setAndContinue(be.rotor > 0 ? SPIN : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.autarkia.steam_turbine");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SlotMenu(AutarkiaMenus.STEAM_TURBINE.get(), id, inv, worldPosition, new ItemStacksResourceHandler(0), data, GuiLayouts.STEAM_TURBINE);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        buffer.serialize(out.child("energy"));
        out.putFloat("rotor", rotor);
        out.putInt("rate", steamRate);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(buffer::deserialize);
        rotor = in.getFloatOr("rotor", 0);
        steamRate = in.getIntOr("rate", 0);
    }
}

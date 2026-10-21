package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

/**
 * Fuse box state view (02 section 6.3). The trip state lives in the grid save data ({@link GridSavedData.FuseState}),
 * the grid ticks it; this block entity registers the fuse, shows it (menu, Jade) and resets it (menu button or wrench).
 */
public class FuseBoxBlockEntity extends AutarkiaBlockEntity implements Ticking, JadeInfo, MenuProvider, Wrenchable, dev.autarkia.registry.MenuButtons {
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            GridSavedData.FuseState f = state();
            return switch (i) {
                case 0 -> f == null ? 0 : f.locked() ? 2 : f.open() ? 1 : 0;
                case 1 -> f == null ? 0 : f.recentTrips();
                case 2 -> f == null || !f.open() || f.locked() ? 0 : (int) Math.max(0, (f.resetAt() - level.getGameTime() + 19) / 20);
                default -> level instanceof ServerLevel sl ? Grid.of(sl).data().at(worldPosition.asLong()).size() : 0;
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public FuseBoxBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.FUSE_BOX.get(), pos, state);
    }

    private GridSavedData.@Nullable FuseState state() {
        return level instanceof ServerLevel sl ? Grid.of(sl).data().fuses.get(worldPosition.asLong()) : null;
    }

    /** Return true while the fuse is open. */
    public boolean open() {
        GridSavedData.FuseState f = state();
        return f != null && f.open();
    }

    /** Return true while the fuse is locked open after 3 trips. */
    public boolean locked() {
        GridSavedData.FuseState f = state();
        return f != null && f.locked();
    }

    /** Return the trips counted in the current 1,200-tick window. */
    public int trips() {
        GridSavedData.FuseState f = state();
        return f == null ? 0 : f.recentTrips();
    }

    /** Called by the grid when the fuse opened or closed. */
    public void stateChanged() {
        syncNow();
    }

    @Override
    public void serverTick() {
        GridSavedData d = Grid.of(serverLevel()).data();
        if (!d.fuses.containsKey(worldPosition.asLong())) {
            d.fuses.put(worldPosition.asLong(), GridSavedData.FuseState.closed(worldPosition.asLong()));
            d.setDirty();
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel sl) {
            Grid.of(sl).data().fuses.remove(pos.asLong());
            Grid.of(sl).data().setDirty();
        }
    }

    @Override
    public boolean menuButton(Player player, int id) {
        if (id != 0) {
            return false;
        }
        Grid.of(serverLevel()).resetFuse(worldPosition.asLong());
        return true;
    }

    @Override
    public InteractionResult wrench(ServerPlayer player) {
        Grid.of(serverLevel()).resetFuse(worldPosition.asLong());
        player.sendOverlayMessage(Component.translatable("message.autarkia.fuse.reset"));
        return InteractionResult.SUCCESS;
    }

    @Override
    public String jadeGroup() {
        return "power";
    }

    @Override
    public void jadeLines(List<Component> out) {
        int s = data.get(0);
        out.add(s == 2 ? Component.translatable("jade.autarkia.fuse.locked")
            : s == 1 ? Component.translatable("jade.autarkia.fuse.open", data.get(2)) : Component.translatable("jade.autarkia.fuse.closed"));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.autarkia.fuse_box");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SlotMenu(AutarkiaMenus.FUSE_BOX.get(), id, inv, worldPosition, new ItemStacksResourceHandler(0), data, GuiLayouts.FUSE_BOX);
    }
}

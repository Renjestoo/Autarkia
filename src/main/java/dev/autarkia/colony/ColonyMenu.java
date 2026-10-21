package dev.autarkia.colony;

import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.AutarkiaMenus;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The colony menu without slots (05b section 9.1): the Core screen (all tabs), the {@code project_board} (Projects tab)
 * and the {@code planning_tablet} (Projects and autonomy). Data comes with {@code colony_sync}, actions go through
 * {@code colony_action}.
 */
public class ColonyMenu extends AbstractContainerMenu {
    /** Which block or item opened the menu. */
    public enum Mode { CORE, BOARD, TABLET }

    public final UUID colonyId;
    public final Mode mode;
    public final BlockPos pos;

    public ColonyMenu(int id, Inventory inv, UUID colonyId, Mode mode, BlockPos pos) {
        super(type(mode), id);
        this.colonyId = colonyId;
        this.mode = mode;
        this.pos = pos;
    }

    /** Client constructor: reads the colony id and block position written by {@link #open}. */
    public static ColonyMenu client(Mode mode, int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return new ColonyMenu(id, inv, UUIDUtil.STREAM_CODEC.decode(buf), mode, BlockPos.STREAM_CODEC.decode(buf));
    }

    private static MenuType<?> type(Mode mode) {
        return switch (mode) {
            case CORE -> AutarkiaMenus.COLONY_CORE.get();
            case BOARD -> AutarkiaMenus.PROJECT_BOARD.get();
            case TABLET -> AutarkiaMenus.PLANNING_TABLET.get();
        };
    }

    /** Open the menu for a player and start sending colony data. */
    public static void open(ServerPlayer player, Colony c, Mode mode, BlockPos pos) {
        player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new ColonyMenu(id, inv, c.id, mode, pos),
            net.minecraft.network.chat.Component.translatable("menu.autarkia." + (mode == Mode.CORE ? "colony_core" : mode == Mode.BOARD ? "project_board"
                : "planning_tablet"))), buf -> {
                    UUIDUtil.STREAM_CODEC.encode(buf, c.id);
                    BlockPos.STREAM_CODEC.encode(buf, pos);
                });
        ColonySync.send(player, c, mode);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (mode == Mode.TABLET) {
            return player.getMainHandItem().is(AutarkiaItems.PLANNING_TABLET.get()) || player.getOffhandItem().is(AutarkiaItems.PLANNING_TABLET.get());
        }
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64;
    }
}

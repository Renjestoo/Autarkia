package dev.autarkia.storage;

import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.LedgerStatusPayload;
import dev.autarkia.network.StockViewPayload;
import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * Ledger terminal (04 section 2.5): links to the warehouse controller that owns its position, takes requests from the
 * players at its screen, and holds the input buffer (slots 0-8, insert only; a put-away request moves it into the
 * warehouse), the crate slot (9, empty crates for new stock) and the output buffer (slots 10-27, extract only). Other
 * blocks see the same rules through the item capability.
 */
public class LedgerTerminalBlockEntity extends AutarkiaBlockEntity implements Ticking, JadeInfo, MenuProvider {
    public static final int INPUTS = 9;
    public static final int CRATE_SLOT = 9;
    public static final int OUTPUTS = 18;
    private final MachineInventory inv = new MachineInventory(INPUTS + 1, OUTPUTS, s -> true, this::setChanged) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            ItemStack s = resource.toStack(1);
            return index < INPUTS ? !CrateBlockEntity.isFilledCrate(s) : index == CRATE_SLOT && CrateItems.isCrate(s);
        }
    };
    private String warning = "";
    private long warnedAt;

    public LedgerTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.LEDGER_TERMINAL.get(), pos, state);
    }

    /** Return the handler other blocks see: insert into the input buffer and crate slot, extract from the output buffer. */
    public ResourceHandler<ItemResource> items() {
        return inv.automation();
    }

    /** Return the menu inventory. */
    public MachineInventory inventory() {
        return inv;
    }

    /** Return the controller this terminal is linked to, or null ("No warehouse"). */
    public @Nullable WarehouseControllerBlockEntity controller() {
        return level instanceof ServerLevel sl ? Warehouses.ownerOf(sl, worldPosition) : null;
    }

    /** Return the first non-empty input stack, or empty. */
    public ItemStack firstInput() {
        for (int i = 0; i < INPUTS; i++) {
            if (!inv.get(i).isEmpty()) {
                return inv.get(i);
            }
        }
        return ItemStack.EMPTY;
    }

    /** Return the crate slot stack (live view). */
    public ItemStack crateSlot() {
        return inv.get(CRATE_SLOT);
    }

    /** Take one empty crate out of the crate slot; returns it or empty. */
    public ItemStack takeEmptyCrate() {
        ItemStack s = inv.get(CRATE_SLOT);
        if (s.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack one = s.copyWithCount(1);
        inv.shrink(CRATE_SLOT, 1);
        return one;
    }

    /** Move as much of the input buffer as fits into a crate item's contents. */
    public void fillCrate(ItemStack crate) {
        for (int i = 0; i < INPUTS; i++) {
            ItemStack s = inv.get(i);
            if (!s.isEmpty()) {
                ItemStack rest = CrateItems.put(crate, s.copy());
                inv.put(i, rest);
            }
        }
    }

    /** Put a stack into the output buffer; returns what did not fit. */
    public ItemStack output(ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int pass = 0; pass < 2 && !rest.isEmpty(); pass++) {
            for (int i = INPUTS + 1; i < inv.size() && !rest.isEmpty(); i++) {
                ItemStack cur = inv.get(i);
                if (pass == 0 && !cur.isEmpty() && ItemStack.isSameItemSameComponents(cur, rest)) {
                    int n = Math.min(rest.getCount(), cur.getMaxStackSize() - cur.getCount());
                    inv.put(i, cur.copyWithCount(cur.getCount() + n));
                    rest.shrink(n);
                } else if (pass == 1 && cur.isEmpty()) {
                    inv.put(i, rest.split(rest.getMaxStackSize()));
                }
            }
        }
        return rest;
    }

    /** Show a warning key on the screen for 10 seconds. */
    public void warn(String key) {
        warning = key;
        warnedAt = level == null ? 0 : level.getGameTime();
    }

    /** Submit a player's request to the linked controller; tells the player when it is refused. */
    public void request(ServerPlayer player, ItemStack item, int amount) {
        WarehouseControllerBlockEntity c = controller();
        String refusal = c == null ? "message.autarkia.ledger.no_warehouse" : item.isEmpty() ? "message.autarkia.ledger.none_available"
            : c.submit(player, worldPosition, item, amount);
        if (refusal != null) {
            player.sendSystemMessage(Component.translatable(refusal));
            return;
        }
        serverLevel().playSound(null, worldPosition, AutarkiaSounds.of("ledger_request"), SoundSource.BLOCKS, 0.8f, 1.0f);
    }

    /** Return the stock view of the linked controller (empty without one). */
    public StockViewPayload stockView() {
        List<StockViewPayload.Stock> out = new ArrayList<>();
        WarehouseControllerBlockEntity c = controller();
        if (c != null) {
            for (Map.Entry<ItemResource, Long> e : c.stock().entrySet()) {
                out.add(new StockViewPayload.Stock(e.getKey().toStack(1), c.available(e.getKey()), e.getValue()));
            }
        }
        return new StockViewPayload(worldPosition, out);
    }

    /** Return the current warning key: no_warehouse, no_loader, a recent trip warning, or empty. */
    public String warning() {
        WarehouseControllerBlockEntity c = controller();
        if (c == null) {
            return "no_warehouse";
        }
        if (!warning.isEmpty() && level.getGameTime() - warnedAt < 200) {
            return warning;
        }
        return c.requests(worldPosition).stream().anyMatch(r -> r.open() && "no_loader".equals(r.reason)) ? "no_loader" : "";
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        long now = sl.getGameTime();
        if ((now + worldPosition.asLong()) % 20 != 0) {
            return;
        }
        WarehouseControllerBlockEntity c = controller();
        if (c != null && !firstInput().isEmpty()) {
            c.submitStore(worldPosition);
        }
        List<LedgerRequest.View> queue = c == null ? List.of() : c.requests(worldPosition).stream().filter(r -> !r.store).map(r -> r.view(now))
            .toList();
        List<LedgerRequest.View> shown = queue.subList(Math.max(0, queue.size() - 16), queue.size());
        LedgerStatusPayload status = new LedgerStatusPayload(worldPosition, c != null, warning(), c == null ? 0 : c.loaders(), shown);
        for (ServerPlayer p : sl.players()) {
            if (p.containerMenu instanceof SlotMenu m && m.pos.equals(worldPosition)) {
                AutarkiaNetwork.toPlayer(p, status);
            }
        }
    }

    @Override
    public String jadeGroup() {
        return "logistics";
    }

    @Override
    public void jadeLines(List<Component> out) {
        WarehouseControllerBlockEntity c = controller();
        out.add(Component.translatable(c != null ? "jade.autarkia.ledger.linked" : "jade.autarkia.ledger.unlinked"));
        if (c != null) {
            out.add(Component.translatable("jade.autarkia.ledger.queue", c.requests(worldPosition).stream().filter(LedgerRequest::open).count(), c.loaders()));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.LEDGER_TERMINAL.get(), id, playerInv, worldPosition, inv, new SimpleContainerData(0), GuiLayouts.LEDGER_TERMINAL);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            for (int i = 0; i < inv.size(); i++) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, inv.get(i).copy());
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        inv.serialize(out.child("items"));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("items").ifPresent(inv::deserialize);
    }
}

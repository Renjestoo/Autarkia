package dev.autarkia.colony;

import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaAdvancements;
import dev.autarkia.registry.AutarkiaItems;
import guideme.GuidesCommon;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The {@code field_manual} (10 section 5): opens the GuideME guide {@code autarkia:field_manual} (server side works through GuideME's payload).
 * On a player's first join of a world the player gets {@code guide/root} (the Getting Started advancement tab) and, when
 * {@code colony.starterManual} is on, one manual; the done root marks the join as seen, so the manual comes once per world.
 */
public class FieldManualItem extends Item {
    public static final Identifier GUIDE = Autarkia.id("field_manual");

    public FieldManualItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            GuidesCommon.openGuide(player, GUIDE);
        }
        return InteractionResult.SUCCESS;
    }

    /** Register the first-join listener that starts the Getting Started tab and hands out the starter manual. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) {
                firstJoin(sp);
            }
        });
    }

    private static void firstJoin(ServerPlayer player) {
        AdvancementHolder root = player.level().getServer().getAdvancements().get(Autarkia.id("guide/root"));
        if (root == null || player.getAdvancements().getOrStartProgress(root).isDone()) {
            return;
        }
        AutarkiaAdvancements.award(player, "guide/root");
        if (AutarkiaConfig.COLONY_STARTER_MANUAL.get()) {
            ItemStack manual = new ItemStack(AutarkiaItems.FIELD_MANUAL.get());
            if (!player.getInventory().add(manual)) {
                net.minecraft.world.Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), manual);
            }
        }
    }
}

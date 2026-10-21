package dev.autarkia.client;

import dev.autarkia.config.AutarkiaClientConfig;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.storage.MatterBufferBlockEntity;
import dev.autarkia.storage.MatterValues;
import dev.autarkia.storage.RackBlockEntity;
import java.math.BigDecimal;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * The item tooltip of 04 section 4.3 while Shift is held and {@code showLoadTooltip} is on: "Load: X units, Matter: Y MU"
 * for the hovered stack (rack load classes of 04 section 2.3.2, a crate with its contents and shell; matter values of
 * 03 section 6 at domain tier 5, the matter buffer tier), or "Load: X units" for an item without a matter value.
 */
public final class LoadTooltip {
    private LoadTooltip() {
    }

    /** Add the load and matter line to the tooltip. */
    public static void tooltip(ItemTooltipEvent e) {
        ItemStack st = e.getItemStack();
        if (st.isEmpty() || !AutarkiaClientConfig.SHOW_LOAD_TOOLTIP.get() || !Minecraft.getInstance().hasShiftDown()) {
            return;
        }
        int q = st.is(AutarkiaItems.CRATE.get()) ? RackBlockEntity.crateQ(st) * st.getCount() : RackBlockEntity.unitsQ(st);
        String load = BigDecimal.valueOf(q).divide(BigDecimal.valueOf(4)).stripTrailingZeros().toPlainString();
        long mu = MatterValues.of(st, 5) * st.getCount();
        e.getToolTip().add((mu > 0 ? Component.translatable("tooltip.autarkia.load_matter", load, MatterBufferBlockEntity.fmt(mu))
            : Component.translatable("tooltip.autarkia.load", load)).withStyle(ChatFormatting.GRAY));
    }
}

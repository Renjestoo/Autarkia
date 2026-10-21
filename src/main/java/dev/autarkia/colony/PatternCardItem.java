package dev.autarkia.colony;

import dev.autarkia.registry.AutarkiaComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** The {@code pattern_card} (05 section 5.4): blank, or holding an {@code autarkia:pattern_ref} to a recorded Pattern. */
public class PatternCardItem extends Item {
    public PatternCardItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        Pattern.Ref ref = stack.get(AutarkiaComponents.PATTERN_REF.get());
        if (ref == null) {
            out.accept(Component.translatable("tooltip.autarkia.pattern_card.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        out.accept(Component.translatable("tooltip.autarkia.pattern_card.pattern", ref.name(), ref.sx(), ref.sy(), ref.sz(), ref.blocks())
            .withStyle(ChatFormatting.AQUA));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(AutarkiaComponents.PATTERN_REF.get());
    }
}

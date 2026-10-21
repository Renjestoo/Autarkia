package dev.autarkia.colony;

import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The {@code autarky_seed} (05 section 8.4, 08a section 2.6): made by the {@code seed_matrix} only, it carries
 * {@code autarkia:seed_contents}. Use throws one {@code thrown_autarky_seed} along the look direction at 1.5 (ender pearl
 * arc) with {@code seed_throw}; a seed without contents is not thrown.
 */
public class AutarkySeedItem extends Item {
    public AutarkySeedItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.has(AutarkiaComponents.SEED_CONTENTS.get())) {
            if (player instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.translatable("message.autarkia.seed.empty"));
            }
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel sl) {
            ThrownAutarkySeed seed = new ThrownAutarkySeed(sl, player, stack.copyWithCount(1));
            seed.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, ThrownAutarkySeed.VELOCITY, 1.0F);
            sl.addFreshEntity(seed);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(), AutarkiaSounds.of("seed_throw"), SoundSource.PLAYERS, 1f, 1f);
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        SeedContents s = stack.get(AutarkiaComponents.SEED_CONTENTS.get());
        if (s == null) {
            out.accept(Component.translatable("tooltip.autarkia.autarky_seed.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        out.accept(Component.translatable("tooltip.autarkia.autarky_seed.contents", s.entries().size(), s.size().getX(), s.size().getY(), s.size().getZ(),
            s.blocks()).withStyle(ChatFormatting.AQUA));
        out.accept(Component.translatable("tooltip.autarkia.autarky_seed.crew", s.workers().size(), s.charge()).withStyle(ChatFormatting.GRAY));
    }
}

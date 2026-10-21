package dev.autarkia.colony;

import dev.autarkia.Autarkia;
import guideme.GuidesCommon;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** The {@code field_manual} (10 section 5): opens the GuideME guide {@code autarkia:field_manual} (server side works through GuideME's payload). */
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
}

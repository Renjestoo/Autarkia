package dev.autarkia.colony;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The {@code domain_beacon} block (05 section 1.3): placed with its center inside a T3+ domain by the owner or a trusted
 * player, it adds the beacon square of the tier around itself. It fails (and drops) when the colony has its beacon
 * maximum or the square would overlap another colony's domain.
 */
public class DomainBeaconBlock extends MachineBlock {
    public DomainBeaconBlock(Properties props) {
        super(props, AutarkiaBlockEntities.DOMAIN_BEACON, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel sl) || Colonies.server() == null) {
            return;
        }
        Colony c = Colonies.at(sl, pos);
        String fail = null;
        if (c == null || c.tier < 3) {
            fail = "message.autarkia.beacon.outside";
        } else if (placer instanceof Player p && !c.canManage(p.getUUID())) {
            fail = "message.autarkia.beacon.no_rights";
        } else if (c.beacons.size() >= ColonyTiers.maxBeacons(c.tier)) {
            fail = "message.autarkia.beacon.max";
        } else if (Colonies.overlapsOther(sl, c, pos.getX(), pos.getZ(), ColonyTiers.beaconHalf(c.tier))) {
            fail = "message.autarkia.beacon.overlap";
        }
        if (fail != null) {
            if (placer instanceof Player p) {
                p.sendOverlayMessage(Component.translatable(fail));
            }
            sl.destroyBlock(pos, true);
            return;
        }
        c.beacons.add(new Colony.Beacon(pos.immutable(), false));
        Colonies.rebuildIndex();
        Colonies.changed();
        ColonyRuntime.of(c).log("Domain beacon placed at " + pos.toShortString());
    }
}

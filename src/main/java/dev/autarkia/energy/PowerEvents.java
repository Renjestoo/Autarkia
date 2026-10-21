package dev.autarkia.energy;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.config.AutarkiaConfig;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.ServerExplosion;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Game events of the T6 power part. The explosion hook of 05 section 8.6 runs on {@code ExplosionEvent.Detonate} at
 * priority HIGH, so it changes the affected list before the {@code PatternRepair} and {@code FrontierSignals} listeners
 * (priority NORMAL) read it: first the positions of a Gyro Heart holding at least 1% are removed (decision G2), then the
 * colony is the domain of the center, else of the first affected block (decision C7); at tier 6 with
 * {@code absorb.enabled} and an ONLINE core, {@link EnergyApi#absorbExplosion} pays the gain and the domain positions are
 * removed (no break, no drops, no fire; entities keep their damage). In such a domain a Wither or Ender Dragon cannot
 * destroy blocks ({@code LivingDestroyBlockEvent} cancelled at priority HIGH, decision C8). A survival break of a Gyro
 * Heart at 1% or more is cancelled with "Spin it down first.". The level tick refreshes {@link DomainPowerIndex}.
 */
public final class PowerEvents {
    private PowerEvents() {
    }

    /** Register the listeners (game bus). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, PowerEvents::detonate);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, PowerEvents::bossDestroy);
        NeoForge.EVENT_BUS.addListener(PowerEvents::gyroBreak);
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post e) -> {
            if (e.getLevel() instanceof ServerLevel sl) {
                DomainPowerIndex.tick(sl);
            }
        });
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload e) -> {
            if (e.getLevel() instanceof ServerLevel sl) {
                DomainPowerIndex.clear(sl);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> DomainPowerIndex.clear(null));
    }

    private static void detonate(ExplosionEvent.Detonate e) {
        if (!(e.getLevel() instanceof ServerLevel sl)) {
            return;
        }
        List<BlockPos> affected = e.getAffectedBlocks();
        affected.removeIf(p -> Grid.memberAt(sl, p) instanceof GyroHeartBlockEntity g && g.locked());
        if (Colonies.server() == null || !AutarkiaConfig.ABSORB_ENABLED.get()) {
            return;
        }
        ServerExplosion ex = e.getExplosion();
        Colony c = Colonies.at(sl, BlockPos.containing(ex.center()));
        if (c == null && !affected.isEmpty()) {
            c = Colonies.at(sl, affected.get(0));
        }
        if (c == null || c.tier < 6 || !DomainPowerIndex.coreOnline(sl, c)) {
            return;
        }
        Colony colony = c;
        int inside = (int) affected.stream().filter(p -> Colonies.inside(colony, p)).count();
        if (EnergyApi.absorbExplosion(sl, colony, ex.radius(), inside)) {
            affected.removeIf(p -> Colonies.inside(colony, p));
        }
    }

    private static void bossDestroy(LivingDestroyBlockEvent e) {
        if (!(e.getEntity() instanceof WitherBoss || e.getEntity() instanceof EnderDragon) || !(e.getEntity().level() instanceof ServerLevel sl)
            || Colonies.server() == null || !AutarkiaConfig.ABSORB_ENABLED.get()) {
            return;
        }
        Colony c = Colonies.at(sl, e.getPos());
        if (c != null && c.tier >= 6 && DomainPowerIndex.coreOnline(sl, c)) {
            e.setCanceled(true);
            Autarkia.LOG.debug("[grid] {} could not break {} in the powered domain of {}", e.getEntity().getType().getDescriptionId(),
                e.getPos().toShortString(), c.name);
        }
    }

    private static void gyroBreak(BreakBlockEvent e) {
        if (e.getLevel() instanceof ServerLevel sl && Grid.memberAt(sl, e.getPos()) instanceof GyroHeartBlockEntity g && g.locked()
            && !e.getPlayer().getAbilities().instabuild) {
            e.setCanceled(true);
            e.getPlayer().sendOverlayMessage(Component.translatable("message.autarkia.gyro.spin_down"));
        }
    }
}

package dev.autarkia.equipment;

import com.klikli_dev.magicparticleslib.premade.particle.electricarc.ElectricArcParticleOptions;
import dev.autarkia.registry.AutarkiaSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * {@code shock_baton} (07 section 1.4): 200,000 FE, charged at 2,000 FE/t. A hit with 1,000 FE or more spends 1,000 FE
 * and adds 4 {@code autarkia:shock} damage, Slowness II for 40 ticks and an arc.
 */
public class ShockBatonItem extends PoweredItem {
    public static final int HIT_COST = 1000;

    public ShockBatonItem(Properties props) {
        super(props, 200000, 2000, 0);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel sl) || !spend(stack, HIT_COST)) {
            return;
        }
        target.setInvulnerableTime(0);
        target.hurtServer(sl, sl.damageSources().source(AutarkiaDamage.SHOCK, attacker), 4f);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), attacker);
        Vec3 from = attacker.getEyePosition();
        Vec3 to = target.position().add(0, target.getBbHeight() / 2, 0);
        sl.sendParticles(new ElectricArcParticleOptions(to, 0xFF8FD3FF, 0.12F, sl.getRandom().nextInt(), 6), from.x, from.y - 0.3, from.z, 1, 0, 0, 0, 0);
        sl.playSound(null, target.getX(), target.getY(), target.getZ(), AutarkiaSounds.of("item.shock_baton.hit"), SoundSource.PLAYERS, 0.8f, 1.4f);
    }
}

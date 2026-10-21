package dev.autarkia.integration.jade;

import dev.autarkia.Autarkia;
import dev.autarkia.energy.RollingFlywheelEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Client Jade lines of a {@code rolling_flywheel} (02 section 9.2): {@code Spin: X%} with spin = |v| / 0.6 (the speed
 * of a full wheel, the same scale as the mounted {@code stone_flywheel}) and {@code Energy: X FE} = 40,000 (v / 0.6)^2,
 * both from the synced roll speed.
 */
public enum RollingFlywheelProvider implements IEntityComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (accessor.getEntity() instanceof RollingFlywheelEntity wheel) {
            tooltip.add(Component.translatable("jade.autarkia.spin", Math.round(Math.abs(wheel.speed()) / 0.6 * 100)));
            tooltip.add(Component.translatable("jade.autarkia.gyro.energy", wheel.energy()));
        }
    }

    @Override
    public Identifier getUid() {
        return Autarkia.id("rolling_flywheel");
    }
}

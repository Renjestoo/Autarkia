package dev.autarkia.worker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.tslat.smartbrainlib.api.core.navigation.SmoothFlyingPathNavigation;

/**
 * A worker on the drone rig (06 section 1.1): flies without gravity at its FLYING_SPEED, hovers in place, takes no
 * fall damage, makes no step sounds, never drowns, hums its rotors while running and lands in place when its battery runs out (06 section 1.2).
 */
public class DroneWorkerEntity extends WorkerEntity {
    public DroneWorkerEntity(EntityType<? extends WorkerEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
    }

    /** Return the attribute set of a drone type: the worker set plus FLYING_SPEED and follow range 64. */
    public static AttributeSupplier.Builder droneAttributes(WorkerType t) {
        return attributes(t).add(Attributes.FLYING_SPEED, t.speed()).add(Attributes.FOLLOW_RANGE, 64);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        SmoothFlyingPathNavigation nav = new SmoothFlyingPathNavigation(this, level);
        nav.setCanFloat(true);
        nav.setCanOpenDoors(false);
        return nav;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel) {
            setNoGravity(!runDown());
            if (!runDown() && tickCount % 30 == 0) {
                sound("entity.worker.rotor_loop", 1.0f, 1.0f);
            }
        }
    }
}

package dev.autarkia.worker;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

/**
 * Worker animation controllers (06 section 2.3): {@code move} (idle, walk or fly, rundown), {@code rotors} (drones),
 * {@code pose} (carry) and {@code action} (work, crank, climb and recharge loops from {@code DATA_ACTION}, plus the
 * server-triggered one-shots).
 */
public final class WorkerAnimations {
    private WorkerAnimations() {
    }

    private static RawAnimation loop(String rig, String name) {
        return RawAnimation.begin().thenLoop("animation." + rig + "." + name);
    }

    /** Register the three controllers for a worker. */
    public static void register(WorkerEntity w, AnimatableManager.ControllerRegistrar controllers) {
        String rig = w.workerType.rig();
        RawAnimation idle = loop(rig, "idle");
        RawAnimation walk = loop(rig, "walk");
        RawAnimation rundown = RawAnimation.begin().thenPlayAndHold("animation." + rig + ".rundown");
        RawAnimation carry = loop(rig, "carry");
        RawAnimation work = loop(rig, "work");
        RawAnimation crank = loop(rig, "crank");
        RawAnimation recharge = loop(rig, "recharge");
        boolean drone = rig.equals("drone");
        RawAnimation move = drone ? loop(rig, "fly") : walk;
        controllers.add(new AnimationController<WorkerEntity>("move", 5, test -> {
            WorkerEntity e = test.animatable();
            if (e.runDown()) {
                return test.setAndContinue(rundown);
            }
            return test.setAndContinue(test.isMoving() ? move : idle);
        }));
        if (drone) {
            RawAnimation rotors = loop(rig, "rotor_spin");
            controllers.add(new AnimationController<WorkerEntity>("rotors", 0, test -> test.animatable().runDown() ? PlayState.STOP : test.setAndContinue(rotors)));
        }
        controllers.add(new AnimationController<WorkerEntity>("pose", 4, test -> test.animatable().carrying() ? test.setAndContinue(carry) : PlayState.STOP));
        RawAnimation climb = rig.equals("walker") ? loop(rig, "climb") : work;
        AnimationController<WorkerEntity> action = new AnimationController<>("action", 3, test -> switch (test.animatable().action()) {
            case WorkerEntity.ACTION_WORK -> test.setAndContinue(work);
            case WorkerEntity.ACTION_CRANK -> test.setAndContinue(crank);
            case WorkerEntity.ACTION_CLIMB -> test.setAndContinue(climb);
            case WorkerEntity.ACTION_RECHARGE -> test.setAndContinue(recharge);
            default -> PlayState.STOP;
        });
        String[] triggers = switch (rig) {
            case "crab" -> new String[] {"place", "scoop", "lift", "lower", "unfold"};
            case "drone" -> new String[] {"place", "weld", "string", "scan", "shoot", "unfold"};
            default -> new String[] {"place", "string", "weld", "unfold"};
        };
        for (String one : triggers) {
            action.triggerableAnim(one, RawAnimation.begin().thenPlay("animation." + rig + "." + one));
        }
        controllers.add(action);
    }
}

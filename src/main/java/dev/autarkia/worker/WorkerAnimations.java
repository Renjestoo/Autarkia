package dev.autarkia.worker;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

/**
 * Worker animation controllers (06 section 2.3): {@code move} (idle, walk, rundown), {@code pose} (carry) and
 * {@code action} (work, crank and recharge loops from {@code DATA_ACTION}, plus the server-triggered one-shots).
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
        controllers.add(new AnimationController<WorkerEntity>("move", 5, test -> {
            WorkerEntity e = test.animatable();
            if (e.runDown()) {
                return test.setAndContinue(rundown);
            }
            return test.setAndContinue(test.isMoving() ? walk : idle);
        }));
        controllers.add(new AnimationController<WorkerEntity>("pose", 4, test -> test.animatable().carrying() ? test.setAndContinue(carry) : PlayState.STOP));
        AnimationController<WorkerEntity> action = new AnimationController<>("action", 3, test -> switch (test.animatable().action()) {
            case WorkerEntity.ACTION_WORK -> test.setAndContinue(work);
            case WorkerEntity.ACTION_CRANK -> test.setAndContinue(crank);
            case WorkerEntity.ACTION_RECHARGE -> test.setAndContinue(recharge);
            default -> PlayState.STOP;
        });
        for (String one : new String[] {"place", "string", "weld", "unfold"}) {
            action.triggerableAnim(one, RawAnimation.begin().thenPlay("animation." + rig + "." + one));
        }
        controllers.add(action);
    }
}

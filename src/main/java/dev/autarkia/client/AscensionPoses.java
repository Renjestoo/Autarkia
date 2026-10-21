package dev.autarkia.client;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.enums.PlayState;
import dev.autarkia.Autarkia;
import dev.autarkia.network.AscensionPosePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;

/**
 * Client side of the ascension poses (07 section 3.5): the Player Animation Library layer {@code autarkia:ascension}
 * (priority 1000) on every avatar, and the {@code ascension_pose} handler that triggers or stops
 * {@code autarkia:ascension_fit} and {@code autarkia:ascension_rise} (from {@code player_animations/ascension.json}).
 */
public final class AscensionPoses {
    public static final Identifier LAYER = Autarkia.id("ascension");

    private AscensionPoses() {
    }

    /** Register the animation layer factory (client setup). */
    public static void register() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 1000,
            avatar -> new PlayerAnimationController(avatar, (controller, data, setter) -> PlayState.STOP));
    }

    /** Play or stop the pose of the payload on the player it names, when that player is loaded on this client. */
    public static void handle(AscensionPosePayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(p.playerId()) instanceof Avatar avatar)) {
            return;
        }
        if (PlayerAnimationAccess.getPlayerAnimationLayer(avatar, LAYER) instanceof PlayerAnimationController c) {
            if (p.play()) {
                c.triggerAnimation(p.animation());
            } else {
                c.stopTriggeredAnimation();
            }
        }
    }
}

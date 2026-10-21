package dev.autarkia.client.render;

import dev.autarkia.logistics.CapsuleDeltaPayload;
import dev.autarkia.logistics.CapsuleStationBlockEntity;
import net.minecraft.client.Minecraft;

/** Client handler of capsule network deltas: applies them to the mirror of the owning station (04 section 0.3). */
public final class ClientCapsules {
    private ClientCapsules() {
    }

    /** Apply a delta to the station at its anchor position; ignored when that station is not loaded on this client. */
    public static void handle(CapsuleDeltaPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(p.anchor()) instanceof CapsuleStationBlockEntity st) {
            st.network().apply(p.ops());
        }
    }
}

package dev.autarkia.registry;

import com.mojang.serialization.Codec;
import dev.autarkia.Autarkia;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Data attachments of 08b section 6: per-player state that is saved with the player and kept through a death. */
public final class AutarkiaAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Autarkia.MOD_ID);

    /** Game time from which the Nanite Reform of the player is ready again (07 section 3.2); 0 = ready. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> REFORM_READY_AT = ATTACHMENTS.register("reform_ready_at",
        () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("value")).copyOnDeath().build());

    /** Damage points the Aegis tether paid for the player in total, in thousandths ({@code equipment/paid_in_full} at 1,000 points). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> AEGIS_PAID_MILLI = ATTACHMENTS.register("aegis_paid_milli",
        () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("value")).copyOnDeath().build());

    private AutarkiaAttachments() {
    }

    /** Register the deferred register on the mod bus. */
    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}

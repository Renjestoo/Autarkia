package dev.autarkia.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers every Autarkia payload; client-bound handlers are attached by the client entry point. */
public final class AutarkiaNetwork {
    public static final String VERSION = "3";

    private AutarkiaNetwork() {
    }

    /** Register payload types and codecs (mod bus). */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("autarkia").versioned(VERSION);
        r.playToClient(SpanListPayload.TYPE, SpanListPayload.CODEC);
        r.playToClient(SpanStatePayload.TYPE, SpanStatePayload.CODEC);
        r.playToClient(BeamVisualPayload.TYPE, BeamVisualPayload.CODEC);
        r.playToClient(ProjectPreviewPayload.TYPE, ProjectPreviewPayload.CODEC);
        r.playToClient(MultimeterReadingPayload.TYPE, MultimeterReadingPayload.CODEC);
        r.playToClient(TransitDeltaPayload.TYPE, TransitDeltaPayload.CODEC);
        r.playToServer(EquipmentActionPayload.TYPE, EquipmentActionPayload.CODEC, EquipmentActionPayload::handle);
        r.playToClient(ColonySyncPayload.TYPE, ColonySyncPayload.CODEC);
        r.playToClient(WorkerDebugPayload.TYPE, WorkerDebugPayload.CODEC);
        r.playToServer(ColonyActionPayload.TYPE, ColonyActionPayload.CODEC, ColonyActionPayload::handle);
        r.playToServer(PreviewRequestPayload.TYPE, PreviewRequestPayload.CODEC, PreviewRequestPayload::handle);
    }

    /** Send a payload to every player tracking the chunk at pos. */
    public static void toTracking(ServerLevel level, ChunkPos chunk, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingChunk(level, chunk, payload);
    }

    /** Send a payload to all players within radius of pos in the level. */
    public static void toNear(ServerLevel level, Vec3 pos, double radius, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersNear(level, null, pos.x, pos.y, pos.z, radius, payload);
    }

    /** Send a payload to one player. */
    public static void toPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}

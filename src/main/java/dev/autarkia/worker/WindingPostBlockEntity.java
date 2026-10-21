package dev.autarkia.worker;

import dev.autarkia.colony.ColonySensors;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.EnergyBuffer;
import dev.autarkia.energy.GridMember;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.Ticking;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The {@code winding_post} (06 section 5.7): one docked spring worker at a time is wound at 40 T/t for 80 FE/t from
 * its 4,000 FE buffer (FE per T from config, minimum 2 keeps the anti-loop rule), or at 10 T/t without FE while a player
 * holds use on the post. A ratchet sound plays every 10 ticks while winding.
 */
public class WindingPostBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, JadeInfo, ColonySensors.Member {
    private final EnergyBuffer buffer = new EnergyBuffer(4000, 80, 0, this::setChanged);
    private @Nullable UUID docked;
    private long playerUntil;
    private boolean winding;

    public WindingPostBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.WINDING_POST.get(), pos, state);
    }

    /** Return whether a worker may use the post (free or already docked there). */
    public boolean availableFor(UUID worker) {
        return docked == null || docked.equals(worker) || !(serverLevel() != null && serverLevel().getEntity(docked) instanceof WorkerEntity);
    }

    /** Lock the post to the worker; returns false when another worker holds it. */
    public boolean dock(WorkerEntity w) {
        if (!availableFor(w.getUUID())) {
            return false;
        }
        docked = w.getUUID();
        return true;
    }

    /** Release the lock of the worker. */
    public void undock(WorkerEntity w) {
        if (w.getUUID().equals(docked)) {
            docked = null;
            winding = false;
        }
    }

    /** Wind by hand for this tick and the next few (player holding use). */
    public void handWind() {
        playerUntil = level.getGameTime() + 5;
    }

    @Override
    public void serverTick() {
        WorkerEntity w = docked == null ? null : serverLevel().getEntity(docked) instanceof WorkerEntity we ? we : null;
        winding = false;
        if (w == null || w.position().distanceToSqr(Vec3.atCenterOf(worldPosition)) > 2.4 * 2.4 || w.energy() >= w.workerType.capacity()) {
            if (w == null || w.state() != WorkerEntity.State.RECHARGING) {
                docked = null;
            }
            return;
        }
        int fePerT = Math.max(2, AutarkiaConfig.WINDING_FE_PER_TENSION.get());
        int t = Math.min(40, buffer.stored() / fePerT);
        if (t > 0) {
            buffer.drain(t * fePerT);
        } else if (level.getGameTime() <= playerUntil) {
            t = 10;
        }
        if (t > 0) {
            w.setEnergy(w.energy() + t);
            winding = true;
            if (level.getGameTime() % 10 == 0) {
                w.sound("entity.worker.wind", 0.5f, 1.8f);
            }
        }
    }

    @Override
    public Set<ColonySensors.Kind> colonyKinds() {
        return Set.of(ColonySensors.Kind.CHARGER);
    }

    @Override
    public Role gridRole() {
        return Role.CONSUMER;
    }

    @Override
    public EnergyBuffer gridBuffer() {
        return buffer;
    }

    @Override
    public int gridMaxOut() {
        return 0;
    }

    @Override
    public int gridMaxIn() {
        return buffer.room() > 0 ? 80 : 0;
    }

    @Override
    public int workDraw() {
        return docked != null ? 80 : 0;
    }

    @Override
    public boolean gridValid() {
        return !isRemoved();
    }

    @Override
    public String jadeGroup() {
        return "power";
    }

    @Override
    public void jadeLines(List<Component> out) {
        out.add(Component.translatable("jade.autarkia.buffer", buffer.stored(), buffer.capacity()));
        out.add(Component.translatable(docked == null ? "jade.autarkia.post.free" : winding ? "jade.autarkia.post.winding" : "jade.autarkia.post.docked"));
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        buffer.serialize(out.child("energy"));
        if (docked != null) {
            out.store("docked", UUIDUtil.CODEC, docked);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(buffer::deserialize);
        docked = in.read("docked", UUIDUtil.CODEC).orElse(null);
    }
}

package dev.autarkia.worker;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Debris a Hauler Crab collects (06 section 3.6 CollectDebris): collecting returns the items and discards the entity. */
public interface WorkerCollectable {
    /** Return the debris items and discard the entity. */
    List<ItemStack> collect(ServerLevel level);
}

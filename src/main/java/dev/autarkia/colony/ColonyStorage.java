package dev.autarkia.colony;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * A colony storage member (05 section 2.3 StorageView): a container the sensors count and the workers fetch from and
 * deposit into (crates now; racks and depots join with their milestones).
 */
public interface ColonyStorage {
    /** Return the item handler the colony reads and writes. */
    ResourceHandler<ItemResource> colonyItems();
}

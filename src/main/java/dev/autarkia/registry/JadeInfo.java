package dev.autarkia.registry;

import java.util.List;
import net.minecraft.network.chat.Component;

/** Server-side look-at lines of a block entity; the optional Jade plugin streams them to the client (10 section 4). */
public interface JadeInfo {
    /** Return the Jade provider group: power, machine or logistics. */
    String jadeGroup();

    /** Add the tooltip lines. */
    void jadeLines(List<Component> out);

    /** Return a progress fraction 0..1 to show as a bar, or a negative value for none. */
    default float jadeProgress() {
        return -1;
    }
}

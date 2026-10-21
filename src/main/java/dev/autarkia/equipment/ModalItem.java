package dev.autarkia.equipment;

import java.util.List;

/** An item with a per-item list of tool modes for the mode key (07 section 2.5) and an area mining speed factor. */
public interface ModalItem {
    /** Return the modes the mode key cycles, in order. */
    List<ToolMode> modes();

    /** Return the mining speed factor while an area mode is active (07 section 1, for example x0.35 for 3x3). */
    float areaSpeed(ToolMode mode);

    /** Return the mode after the given one in this item's list. */
    default ToolMode next(ToolMode mode) {
        List<ToolMode> m = modes();
        return m.get((m.indexOf(mode) + 1) % m.size());
    }
}

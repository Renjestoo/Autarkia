package dev.autarkia.registry;

/**
 * Screen and slot layout of one menu (generated into GuiLayouts by tools/gui.py together with the background
 * texture). Positions are GUI pixels from the panel corner; -1 means the widget is absent.
 *
 * @param slots   item slot positions; the first {@code inputs} are inputs, the rest outputs or storage
 * @param data    number of container data values the menu syncs
 * @param invY    y of the player inventory rows (hotbar at invY + 58)
 */
public record GuiLayout(String name, int width, int height, int[][] slots, int inputs, int data, int energyX, int energyY,
                        int arrowX, int arrowY, int flameX, int flameY, int invY) {
}

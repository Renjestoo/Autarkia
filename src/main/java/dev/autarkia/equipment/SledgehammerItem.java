package dev.autarkia.equipment;

import java.util.List;
import net.minecraft.world.item.Item;

/** {@code steel_sledgehammer} (07 section 1.3): modes 1x1 and 3x3; 3x3 mines at x0.35 speed through {@link AreaMining}. */
public class SledgehammerItem extends Item implements ModalItem {
    public SledgehammerItem(Properties props) {
        super(props);
    }

    @Override
    public List<ToolMode> modes() {
        return List.of(ToolMode.SINGLE, ToolMode.AREA3);
    }

    @Override
    public float areaSpeed(ToolMode mode) {
        return mode == ToolMode.AREA3 ? 0.35f : 1f;
    }
}

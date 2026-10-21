package dev.autarkia.client.screen;

import dev.autarkia.logistics.MatterConduitBlockEntity;
import dev.autarkia.registry.SlotMenu;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

/**
 * Matter conduit screen (04 section 4.1): the mode toggle, the output face button, 16 colour channel buttons (two rows
 * of 8, the chosen one framed white), the 9-slot buffer, the energy bar, items per second and "Outside domain" in red.
 */
public class ConduitScreen extends SlotScreen {
    private static final int CH_X = 88;
    private static final int CH_Y = 52;
    private static final int CH = 9;
    private int seenMode = -1;
    private int seenFace = -1;

    public ConduitScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected boolean showsStatus() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        seenMode = menu.value(5);
        seenFace = menu.value(7);
        String mode = seenMode == 1 ? "receive" : "send";
        addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.matter_conduit.mode", Component.translatable(
            "jade.autarkia.matter_conduit.mode." + mode)), b -> click(MatterConduitBlockEntity.MODE_BUTTON)).bounds(leftPos + 86, topPos + 17, 82, 14).build());
        Direction face = Direction.values()[Math.clamp(seenFace, 0, 5)];
        addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.matter_conduit.face", Component.translatable(
            "screen.autarkia.matter_conduit.face." + face.getName().toLowerCase(Locale.ROOT))), b -> click(MatterConduitBlockEntity.FACE_BUTTON))
            .bounds(leftPos + 86, topPos + 33, 82, 14).build());
    }

    private void click(int button) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.value(5) != seenMode || menu.value(7) != seenFace) {
            rebuild();
        }
    }

    private int channelAt(double x, double y) {
        for (int c = 0; c < 16; c++) {
            int cx = CH_X + (c % 8) * (CH + 1);
            int cy = CH_Y + (c / 8) * (CH + 1);
            if (isHovering(cx, cy, CH, CH, x, y)) {
                return c;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        int c = channelAt(e.x(), e.y());
        if (c >= 0) {
            click(MatterConduitBlockEntity.CHANNEL_BUTTON + c);
            return true;
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int chosen = menu.value(6);
        for (int c = 0; c < 16; c++) {
            int x = leftPos + CH_X + (c % 8) * (CH + 1);
            int y = topPos + CH_Y + (c / 8) * (CH + 1);
            g.fill(x - 1, y - 1, x + CH + 1, y + CH + 1, c == chosen ? 0xFFFFFFFF : 0xFF202020);
            g.fill(x, y, x + CH, y + CH, 0xFF000000 | DyeColor.byId(c).getTextureDiffuseColor());
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        Component state = menu.value(9) == 1 ? Component.translatable("jade.autarkia.matter_conduit.outside")
            : Component.translatable("screen.autarkia.matter_conduit.channel", menu.value(6));
        g.text(font, state, imageWidth - 8 - font.width(state), titleLabelY, menu.value(9) == 1 ? 0xFFB01818 : 0xFF404040, false);
        Component rate = Component.translatable("jade.autarkia.matter_conduit.rate", menu.value(8));
        g.text(font, rate, imageWidth - 8 - font.width(rate), inventoryLabelY, 0xFF404040, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int c = channelAt(mouseX, mouseY);
        if (c >= 0) {
            g.setTooltipForNextFrame(font, List.of(Component.translatable("jade.autarkia.matter_conduit.channel", c, MatterConduitBlockEntity.channelName(c))
                .getVisualOrderText()), mouseX, mouseY);
        }
    }
}

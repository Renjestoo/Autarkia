package dev.autarkia.client.screen;

import dev.autarkia.Autarkia;
import dev.autarkia.logistics.CrateLauncherBlockEntity;
import dev.autarkia.registry.SlotMenu;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen of the {@code crate_launcher} (04 section 4.1): input 3 x 9 and the crate slot, the status, the target
 * position, distance and chosen angle, "Last shot: HIT / MISS by X.X", the energy bar, and a Reset button while the
 * launcher is paused after 3 misses. Values come from the menu data; the target position from the synced block entity.
 */
public class CrateLauncherScreen extends AbstractContainerScreen<SlotMenu> {
    private final Identifier background;
    private Button reset;

    public CrateLauncherScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, menu.layout.width(), menu.layout.height());
        this.background = Autarkia.id("textures/gui/" + menu.layout.name() + ".png");
        this.inventoryLabelY = menu.layout.invY() - 11;
    }

    @Override
    protected void init() {
        super.init();
        reset = Button.builder(Component.translatable("screen.autarkia.launcher.reset"),
            b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0)).bounds(leftPos + 114, topPos + 76, 54, 14).build();
        addRenderableWidget(reset);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        reset.visible = menu.value(6) == 1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.blit(RenderPipelines.GUI_TEXTURED, background, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        int w = (int) Math.round(160.0 * menu.value(0) / Math.max(1, menu.value(1)));
        g.fill(leftPos + 8, topPos + 134, leftPos + 168, topPos + 137, 0xFF2A1A12);
        g.fill(leftPos + 8, topPos + 134, leftPos + 8 + w, topPos + 137, 0xFFE0A030);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        CrateLauncherBlockEntity.Status[] all = CrateLauncherBlockEntity.Status.values();
        CrateLauncherBlockEntity.Status st = all[Math.clamp(menu.value(7), 0, all.length - 1)];
        int color = st == CrateLauncherBlockEntity.Status.READY || st == CrateLauncherBlockEntity.Status.WAITING ? 0xFF2E7D32
            : st == CrateLauncherBlockEntity.Status.PAUSED || st == CrateLauncherBlockEntity.Status.NO_ARC ? 0xFFB71C1C : 0xFF8D6E00;
        g.text(font, Component.translatable(st.key()), 8, 96, color, false);
        BlockPos t = minecraft.level != null && minecraft.level.getBlockEntity(menu.pos) instanceof CrateLauncherBlockEntity l ? l.target() : null;
        g.text(font, t == null ? Component.translatable("screen.autarkia.launcher.no_target")
            : Component.translatable("screen.autarkia.launcher.target", t.getX(), t.getY(), t.getZ()), 8, 105, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.launcher.distance", menu.value(2), menu.value(3)), 8, 114, 0xFF404040, false);
        Component shot = switch (menu.value(4)) {
            case 1 -> Component.translatable("screen.autarkia.launcher.hit");
            case 2 -> Component.translatable("screen.autarkia.launcher.miss", String.format(Locale.ROOT, "%.1f", menu.value(5) / 10.0));
            default -> Component.translatable("screen.autarkia.launcher.none");
        };
        g.text(font, shot, 8, 123, menu.value(4) == 2 ? 0xFFB71C1C : 0xFF404040, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (isHovering(8, 133, 160, 5, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, List.of(Component.translatable("tooltip.autarkia.energy", menu.value(0), menu.value(1)).getVisualOrderText()),
                mouseX, mouseY);
        }
    }
}

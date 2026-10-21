package dev.autarkia.client.screen;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.NaniteClaim;
import dev.autarkia.colony.NaniteClaims;
import dev.autarkia.colony.NaniteHiveBlockEntity;
import dev.autarkia.network.ColonyActionPayload;
import dev.autarkia.registry.SlotMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Nanite Hive screen (G5): up to 8 marker pairs to pick from, the depth below ref_y (the total height stays within
 * {@code nanite.maxClaimY}), the terrace toggle, Start and Stop (sent as colony_action {@code claim}, {@code claim_resume}
 * and {@code claim_stop}, checked against the colony rights on the server), the claim state and progress, the swarm
 * status, the FE bar and the paste slot.
 */
public class NaniteHiveScreen extends AbstractContainerScreen<SlotMenu> {
    private static final Identifier WIDGETS = Autarkia.id("textures/gui/widgets.png");
    private static final Identifier BACKGROUND = Autarkia.id("textures/gui/nanite_hive.png");
    private static final int ROW_DATA = 10;
    private final List<Button> rows = new ArrayList<>();
    private int selected = -1;
    private int depth;
    private boolean terrace = true;

    public NaniteHiveScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, menu.layout.width(), menu.layout.height());
        this.inventoryLabelY = menu.layout.invY() - 11;
    }

    @Override
    protected void init() {
        super.init();
        rows.clear();
        for (int i = 0; i < NaniteClaims.MAX_PAIRS; i++) {
            int row = i;
            rows.add(addRenderableWidget(Button.builder(Component.empty(), b -> {
                selected = row;
                depth = Math.min(depth, maxDepth());
            }).bounds(leftPos + 8, topPos + 18 + 10 * i, 140, 10).build()));
        }
        addRenderableWidget(Button.builder(Component.literal("-8"), b -> depth = Math.max(0, depth - 8)).bounds(leftPos + 8, topPos + 101, 16, 12).build());
        addRenderableWidget(Button.builder(Component.literal("-1"), b -> depth = Math.max(0, depth - 1)).bounds(leftPos + 25, topPos + 101, 16, 12).build());
        addRenderableWidget(Button.builder(Component.literal("+1"), b -> depth = Math.min(maxDepth(), depth + 1)).bounds(leftPos + 60, topPos + 101, 16, 12)
            .build());
        addRenderableWidget(Button.builder(Component.literal("+8"), b -> depth = Math.min(maxDepth(), depth + 8)).bounds(leftPos + 77, topPos + 101, 16, 12)
            .build());
        addRenderableWidget(Button.builder(terraceLabel(), b -> {
            terrace = !terrace;
            b.setMessage(terraceLabel());
        }).bounds(leftPos + 96, topPos + 101, 72, 12).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.nanite_hive.start"), b -> start()).bounds(leftPos + 8, topPos + 115, 36, 12)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.nanite_hive.stop"), b -> send("claim_stop", List.of(Long.toString(
            menu.pos.asLong())))).bounds(leftPos + 46, topPos + 115, 36, 12).build());
    }

    private Component terraceLabel() {
        return Component.translatable(terrace ? "screen.autarkia.nanite_hive.terrace_on" : "screen.autarkia.nanite_hive.terrace_off");
    }

    private int pairs() {
        return Math.max(0, Math.min(NaniteClaims.MAX_PAIRS, menu.value(9)));
    }

    private BlockPos rel(int row, int first) {
        int b = ROW_DATA + row * 7 + first;
        return menu.pos.offset(menu.value(b), menu.value(b + 1), menu.value(b + 2));
    }

    private int heightOf(int row, int d) {
        BlockPos a = rel(row, 0);
        BlockPos b = rel(row, 3);
        int top = menu.pos.getY() + menu.value(ROW_DATA + row * 7 + 6);
        return top - (Math.min(a.getY(), b.getY()) - d) + 1;
    }

    private int maxDepth() {
        if (selected < 0 || selected >= pairs()) {
            return 0;
        }
        return Math.max(0, menu.value(ROW_DATA + 7 * NaniteClaims.MAX_PAIRS) - heightOf(selected, 0));
    }

    private UUID colony() {
        return minecraft.level != null && minecraft.level.getBlockEntity(menu.pos) instanceof NaniteHiveBlockEntity h ? h.colonyId() : null;
    }

    private void send(String action, List<String> args) {
        UUID c = colony();
        if (c != null) {
            ClientPacketDistributor.sendToServer(new ColonyActionPayload(c, action, args));
        }
    }

    private void start() {
        if (menu.value(0) == NaniteClaim.State.PAUSED.ordinal() + 1) {
            send("claim_resume", List.of(Long.toString(menu.pos.asLong())));
        } else if (selected >= 0 && selected < pairs()) {
            send("claim", List.of(Long.toString(menu.pos.asLong()), Long.toString(rel(selected, 0).asLong()), Long.toString(rel(selected, 3).asLong()),
                Integer.toString(depth), terrace ? "on" : "off"));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int n = pairs();
        if (selected >= n) {
            selected = -1;
        }
        for (int i = 0; i < rows.size(); i++) {
            Button b = rows.get(i);
            b.visible = i < n;
            if (i < n) {
                BlockPos a = rel(i, 0);
                BlockPos c = rel(i, 3);
                Component text = Component.translatable("screen.autarkia.nanite_hive.pair", Math.abs(a.getX() - c.getX()) + 1, Math.abs(a.getZ() - c.getZ()) + 1,
                    Math.min(a.getX(), c.getX()), Math.min(a.getZ(), c.getZ()), heightOf(i, 0));
                b.setMessage(i == selected ? Component.literal("> ").append(text) : text);
            }
        }
        depth = Math.min(depth, maxDepth());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        int cap = menu.value(5);
        if (menu.layout.energyX() >= 0 && cap > 0) {
            int h = Math.min(52, (int) Math.ceil(52.0 * menu.value(4) / cap));
            g.blit(RenderPipelines.GUI_TEXTURED, WIDGETS, leftPos + menu.layout.energyX(), topPos + menu.layout.energyY() + 52 - h, 24, 52 - h, 12, h, 64, 64);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        g.text(font, Component.literal(Integer.toString(depth)), 44, 103, 0xFF404040, false);
        int flags = menu.value(8);
        Component swarm = (flags & NaniteHiveBlockEntity.FLAG_NO_RECIPE) != 0 ? Component.translatable("screen.autarkia.nanite_hive.no_recipe")
            : (flags & NaniteHiveBlockEntity.FLAG_NEEDS_PASTE) != 0 ? Component.translatable("screen.autarkia.nanite_hive.needs_paste")
            : (flags & NaniteHiveBlockEntity.FLAG_CAP_FULL) != 0 ? Component.translatable("screen.autarkia.nanite_hive.cap_full")
            : menu.value(7) > 0 ? Component.translatable("screen.autarkia.nanite_hive.assembling", menu.value(7))
            : Component.translatable("screen.autarkia.nanite_hive.swarms", menu.value(6));
        g.text(font, swarm, 86, 117, 0xFF404040, false);
        int st = menu.value(0);
        if (st <= 0 || st > NaniteClaim.State.values().length) {
            g.text(font, Component.translatable("screen.autarkia.nanite_hive.no_claim"), 8, 130, 0xFF404040, false);
        } else {
            NaniteClaim.State s = NaniteClaim.State.values()[st - 1];
            g.text(font, Component.translatable("screen.autarkia.nanite_hive.state", Component.translatable("nanite.autarkia.state." + s.getSerializedName()),
                menu.value(1), menu.value(2), menu.value(3)), 8, 130, s == NaniteClaim.State.RUN || s == NaniteClaim.State.DONE ? 0xFF404040 : 0xFFB01818,
                false);
        }
        boolean outside = colony() == null;
        Component info = (flags & NaniteHiveBlockEntity.FLAG_STOPPED) != 0 ? Component.translatable("screen.autarkia.nanite_hive.stopped")
            : outside ? Component.translatable("screen.autarkia.nanite_hive.no_colony")
            : (flags & NaniteHiveBlockEntity.FLAG_NO_BUFFER) != 0 ? Component.translatable("screen.autarkia.nanite_hive.no_buffer")
            : selected >= 0 && selected < pairs() ? Component.translatable("screen.autarkia.nanite_hive.height", heightOf(selected, depth),
                menu.value(ROW_DATA + 7 * NaniteClaims.MAX_PAIRS))
            : Component.translatable("screen.autarkia.nanite_hive.pick");
        g.text(font, info, 8, 140, outside || (flags & (NaniteHiveBlockEntity.FLAG_STOPPED | NaniteHiveBlockEntity.FLAG_NO_BUFFER)) != 0 ? 0xFFB01818
            : 0xFF404040, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (menu.layout.energyX() >= 0 && isHovering(menu.layout.energyX(), menu.layout.energyY(), 12, 52, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, List.of(Component.translatable("tooltip.autarkia.energy", menu.value(4) * 100, menu.value(5) * 100)
                .getVisualOrderText()), mouseX, mouseY);
        }
    }
}

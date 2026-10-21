package dev.autarkia.client.screen;

import dev.autarkia.network.DepotNamePayload;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.storage.DepotBlockEntity;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Depot screen (04 section 4.1): the 6 x 9 slots, the site name field (sent 10 ticks after the last edit and when the
 * screen closes) and the "Linked view" button that opens the read-only {@link SitesScreen}.
 */
public class DepotScreen extends SlotScreen {
    private EditBox name;
    private String sent = "";
    private int delay;

    public DepotScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        String current = minecraft.level != null && minecraft.level.getBlockEntity(menu.pos) instanceof DepotBlockEntity d ? d.siteName() : "";
        sent = current;
        name = new EditBox(font, leftPos + 8, topPos + 17, 88, 12, Component.translatable("screen.autarkia.depot.name"));
        name.setMaxLength(DepotBlockEntity.NAME_LENGTH);
        name.setValue(current);
        name.setHint(Component.translatable("screen.autarkia.depot.name"));
        name.setResponder(s -> delay = 10);
        addRenderableWidget(name);
        addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.depot.linked"),
            b -> minecraft.gui.setScreen(new SitesScreen(this, menu.pos, false))).bounds(leftPos + 100, topPos + 16, 68, 14).build());
    }

    private void send() {
        if (!name.getValue().equals(sent)) {
            sent = name.getValue();
            ClientPacketDistributor.sendToServer(new DepotNamePayload(menu.pos, sent));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (delay > 0 && --delay == 0) {
            send();
        }
    }

    @Override
    public void removed() {
        send();
        super.removed();
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (name != null && name.isFocused() && e.key() != 256) {
            name.keyPressed(e);
            return true;
        }
        return super.keyPressed(e);
    }
}

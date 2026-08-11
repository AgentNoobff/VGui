package me.agent.vgui.core.internal;

import com.github.retrooper.packetevents.protocol.sound.Sound;
import com.velocitypowered.api.proxy.Player;
import me.agent.vgui.api.View;
import me.agent.vgui.api.click.ClickContext;
import me.agent.vgui.api.click.ClickType;
import me.agent.vgui.api.contents.Slot;
import me.agent.vgui.api.contents.ViewContents;
import me.agent.vgui.api.context.ViewContext;

final class ClickContextImpl implements ClickContext {
    private final SessionManager manager;
    private final ViewSession session;
    private final int slot;
    private final ClickType clickType;
    private final int hotbarKey;
    private final int rawButton;
    private final int rawMode;
    private final int stateId;

    ClickContextImpl(SessionManager manager, ViewSession session, int slot, ClickType clickType,
                     int hotbarKey, int rawButton, int rawMode, int stateId) {
        this.manager = manager;
        this.session = session;
        this.slot = slot;
        this.clickType = clickType;
        this.hotbarKey = hotbarKey;
        this.rawButton = rawButton;
        this.rawMode = rawMode;
        this.stateId = stateId;
    }

    @Override
    public Player player() {
        return session.player();
    }

    @Override
    public int slot() {
        return slot;
    }

    @Override
    public ClickType clickType() {
        return clickType;
    }

    @Override
    public int hotbarKey() {
        return hotbarKey;
    }

    @Override
    public Slot slotPos() {
        if (slot < 0 || slot >= session.size()) {
            return null;
        }
        return Slot.fromIndex(slot, session.type().columns());
    }

    @Override
    public boolean isPlayerInventory() {
        return slot >= session.size() && slot < session.size() + 36;
    }

    @Override
    public View view() {
        return session.view();
    }

    @Override
    public ViewContents contents() {
        return session.contents();
    }

    @Override
    public ViewContext context() {
        return session.context();
    }

    @Override
    public int rawButton() {
        return rawButton;
    }

    @Override
    public int rawMode() {
        return rawMode;
    }

    @Override
    public int stateId() {
        return stateId;
    }

    @Override
    public void close() {
        manager.close(session);
    }

    @Override
    public void open(View view) {
        manager.open(session, view, null, true);
    }

    @Override
    public void openReplacing(View view) {
        manager.open(session, view, null, false);
    }

    @Override
    public boolean back() {
        return manager.back(session);
    }

    @Override
    public void playSound(Sound sound) {
        playSound(sound, 1.0f, 1.0f);
    }

    @Override
    public void playSound(Sound sound, float volume, float pitch) {
        manager.playSound(session, sound, volume, pitch);
    }
}

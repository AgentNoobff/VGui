package me.agent.vgui.core.internal;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindowButton;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientNameItem;
import com.velocitypowered.api.proxy.Player;
import me.agent.vgui.api.View;
import me.agent.vgui.api.ViewType;
import me.agent.vgui.api.click.ClickType;
import me.agent.vgui.api.item.ViewItem;
import org.slf4j.Logger;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Translates client-bound packets into view events: clicks, window closes and anvil
 * text input. Packets belonging to a proxy view are cancelled so backend servers
 * never see interactions with windows they did not open.
 */
public final class PacketBridge extends PacketListenerAbstract {
    private final Logger logger;
    private final SessionManager sessionManager;

    public PacketBridge(Logger logger, SessionManager sessionManager) {
        super(PacketListenerPriority.HIGH);
        this.logger = logger;
        this.sessionManager = sessionManager;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        sessionManager.runWithCurrent(uuid, session -> {
            if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
                try {
                    handleClick(event, session, uuid);
                } catch (Throwable t) {
                    event.setCancelled(true);
                    logger.warn("[VGui] Rejected malformed click packet for {}", uuid, t);
                    sessionManager.resync(session);
                }
            } else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW_BUTTON) {
                WrapperPlayClientClickWindowButton button = new WrapperPlayClientClickWindowButton(event);
                if (button.getWindowId() == session.windowId()) {
                    event.setCancelled(true);
                }
            } else if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
                WrapperPlayClientCloseWindow close = new WrapperPlayClientCloseWindow(event);
                if (close.getWindowId() == session.windowId()) {
                    event.setCancelled(true);
                    sessionManager.clientClosed(session);
                }
            } else if (event.getPacketType() == PacketType.Play.Client.NAME_ITEM
                    && session.type() == ViewType.ANVIL) {
                event.setCancelled(true);
                WrapperPlayClientNameItem name = new WrapperPlayClientNameItem(event);
                try {
                    session.view().onAnvilInput(name.getItemName(), session.contents());
                } catch (Throwable t) {
                    logger.warn("[VGui] Exception in onAnvilInput for {}", uuid, t);
                }
            }
        });
    }

    private void handleClick(PacketReceiveEvent event, ViewSession session, UUID uuid) {
        WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);
        if (click.getWindowId() != session.windowId()) {
            return;
        }

        int stateId = click.getStateId().orElse(-1);
        if (!session.acceptsStateId(stateId)) {
            event.setCancelled(true);
            sessionManager.resync(session);
            return;
        }

        int slot = click.getSlot();
        int button = click.getButton();
        WrapperPlayClientClickWindow.WindowClickType mode = click.getWindowClickType();
        Set<Integer> changedSlots = changedSlots(click);
        int totalSlots = session.size() + 36;
        if (!ClickDecoder.isValid(mode, button, slot, totalSlots)
                || changedSlots.stream().anyMatch(changed -> changed < 0 || changed >= totalSlots)) {
            event.setCancelled(true);
            sessionManager.correctClick(session, slot, changedSlots, false, false);
            return;
        }
        ClickType clickType = ClickDecoder.decode(mode, button, slot);
        changedSlots = correctionSlots(session, slot, clickType, button, changedSlots);
        boolean legacyMultiSlot = changedSlots.isEmpty() && (clickType.isShift()
                || clickType == ClickType.DOUBLE_CLICK || clickType == ClickType.DRAG_END);
        boolean correctOffhand = clickType == ClickType.OFFHAND_SWAP;

        View view = session.view();
        boolean cancel = true;
        try {
            cancel = view.cancelClientTransactions();
        } catch (Throwable t) {
            logger.warn("[VGui] Exception in cancelClientTransactions for {}", uuid, t);
        }
        if (cancel) {
            event.setCancelled(true);
        }
        if (!sessionManager.isCurrent(session)) {
            event.setCancelled(true);
            return;
        }

        long cooldown = 0;
        try {
            cooldown = view.clickCooldownMillis();
        } catch (Throwable t) {
            logger.warn("[VGui] Exception in clickCooldownMillis for {}", uuid, t);
        }
        if (!sessionManager.isCurrent(session)) {
            event.setCancelled(true);
            return;
        }
        if (!clickType.isDrag() && !session.tryClick(cooldown)) {
            event.setCancelled(true);
            sessionManager.correctClick(session, slot, changedSlots, legacyMultiSlot, correctOffhand);
            return;
        }

        int hotbarKey = ClickDecoder.hotbarKey(mode, button);

        ClickContextImpl ctx = sessionManager.createClickContext(
                session, slot, clickType, hotbarKey, button, mode.ordinal(), stateId);

        boolean allowed = sessionManager.allowClick(session, ctx);
        if (!sessionManager.isCurrent(session)) {
            event.setCancelled(true);
            return;
        }
        if (!allowed) {
            event.setCancelled(true);
            sessionManager.correctClick(session, slot, changedSlots, legacyMultiSlot, correctOffhand);
            return;
        }

        if (slot >= 0 && slot < session.size()) {
            ViewItem item = session.contents().get(slot);
            if (item != null) {
                try {
                    item.handle(ctx);
                } catch (Throwable t) {
                    logger.warn("[VGui] Exception in ViewItem handler for {} slot {}", uuid, slot, t);
                }
            }
        }
        if (!sessionManager.isCurrent(session)) {
            // An item handler navigated. Do not dispatch the stale click to the old
            // view or to the backend, even for an explicitly pass-through view.
            event.setCancelled(true);
            return;
        }
        try {
            view.onClick(ctx);
        } catch (Throwable t) {
            logger.warn("[VGui] Exception in onClick for {}", uuid, t);
        }
        if (!sessionManager.isCurrent(session)) {
            event.setCancelled(true);
            return;
        }

        // The client predicted the click locally; snap it back to our authoritative
        // state (cursor cleared, all slots restored). Skipped for pass-through views.
        if (cancel) {
            sessionManager.correctClick(session, slot, changedSlots, legacyMultiSlot, correctOffhand);
        }
    }

    private static Set<Integer> changedSlots(WrapperPlayClientClickWindow click) {
        Map<Integer, ?> changed = click.getSlots().orElse(Map.of());
        return changed.isEmpty() ? Set.of() : new LinkedHashSet<>(changed.keySet());
    }

    private static Set<Integer> correctionSlots(ViewSession session, int clickedSlot, ClickType clickType,
                                                int button, Set<Integer> changedSlots) {
        if (clickType != ClickType.NUMBER_KEY) {
            return changedSlots;
        }
        LinkedHashSet<Integer> result = new LinkedHashSet<>(changedSlots);
        result.add(clickedSlot);
        result.add(session.size() + 27 + button);
        return result;
    }
}

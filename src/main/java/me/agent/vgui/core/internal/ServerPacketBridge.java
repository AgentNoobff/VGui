package me.agent.vgui.core.internal;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenHorseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Observes packets flowing from backend servers to the client. Two jobs:
 *
 * <ul>
 *   <li>Cache window-0 (player inventory) contents in the {@link InventoryTracker}
 *       so proxy windows can render and resync the player-inventory half.</li>
 *   <li>Detect a backend server opening or closing a window while a proxy view is
 *       open, and end the proxy session ({@code CloseReason.OVERRIDDEN}) so the two
 *       never fight over the player's screen.</li>
 * </ul>
 *
 * <p>All packets VGui itself sends bypass this listener (sent silently), so anything
 * observed here genuinely comes from a backend server.</p>
 */
public final class ServerPacketBridge extends PacketListenerAbstract {
    private final Logger logger;
    private final SessionManager sessionManager;
    private final InventoryTracker inventoryTracker;

    public ServerPacketBridge(Logger logger, SessionManager sessionManager, InventoryTracker inventoryTracker) {
        super(PacketListenerPriority.HIGHEST);
        this.logger = logger;
        this.sessionManager = sessionManager;
        this.inventoryTracker = inventoryTracker;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems items = new WrapperPlayServerWindowItems(event);
            if (items.getWindowId() == 0) {
                UUID uuid = userId(event);
                if (uuid != null) {
                    long generation = inventoryTracker.generation(uuid);
                    inventoryTracker.windowItems(uuid, generation, items.getItems());
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot setSlot = new WrapperPlayServerSetSlot(event);
            int windowId = setSlot.getWindowId();
            if (windowId == 0 || windowId == -2) {
                UUID uuid = userId(event);
                if (uuid != null) {
                    long generation = inventoryTracker.generation(uuid);
                    inventoryTracker.setSlot(uuid, generation, setSlot.getSlot(), setSlot.getItem());
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            UUID uuid = userId(event);
            if (uuid != null) {
                int windowId = new WrapperPlayServerOpenWindow(event).getContainerId();
                if (windowId != 0 && sessionManager.backendOpened(uuid, windowId)) {
                    event.setCancelled(true);
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Server.OPEN_HORSE_WINDOW) {
            UUID uuid = userId(event);
            if (uuid != null) {
                int windowId = new WrapperPlayServerOpenHorseWindow(event).getWindowId();
                if (windowId != 0 && sessionManager.backendOpened(uuid, windowId)) {
                    event.setCancelled(true);
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Server.CLOSE_WINDOW) {
            UUID uuid = userId(event);
            if (uuid != null) {
                int windowId = new WrapperPlayServerCloseWindow(event).getWindowId();
                if (windowId != 0 && sessionManager.backendClosed(uuid, windowId)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private static UUID userId(PacketSendEvent event) {
        if (event.getUser() == null || event.getUser().getProfile() == null) {
            return null;
        }
        return event.getUser().getProfile().getUUID();
    }
}

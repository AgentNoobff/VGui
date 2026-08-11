package me.agent.vgui.core;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import me.agent.vgui.core.internal.SessionManager;

/**
 * Cleans up sessions, history and inventory caches when a player leaves the proxy.
 */
public final class PlayerDisconnectListener {
    private final SessionManager sessionManager;

    public PlayerDisconnectListener(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        sessionManager.disconnected(event.getPlayer().getUniqueId());
    }
}

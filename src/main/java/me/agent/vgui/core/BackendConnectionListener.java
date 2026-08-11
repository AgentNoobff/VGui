package me.agent.vgui.core;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.agent.vgui.core.internal.SessionManager;

/** Invalidates backend-scoped state around Velocity server transitions. */
public final class BackendConnectionListener {
    private final SessionManager sessionManager;

    public BackendConnectionListener(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Subscribe(priority = 64)
    public void onServerPreConnect(ServerPreConnectEvent event) {
        RegisteredServer target = event.getResult().getServer().orElse(null);
        RegisteredServer previous = event.getPreviousServer();
        if (target != null && !target.equals(previous)) {
            sessionManager.backendSwitchStarted(event.getPlayer().getUniqueId());
        }
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        sessionManager.backendConnected(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        if (event.kickedDuringServerConnect()) {
            sessionManager.backendSwitchAborted(event.getPlayer().getUniqueId());
        }
    }
}

package me.agent.vgui.core;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.EventManager;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.retrooper.packetevents.velocity.factory.VelocityPacketEventsBuilder;
import me.agent.vgui.api.CloseReason;
import me.agent.vgui.api.VGui;
import me.agent.vgui.core.internal.InventoryTracker;
import me.agent.vgui.core.internal.PacketBridge;
import me.agent.vgui.core.internal.ServerPacketBridge;
import me.agent.vgui.core.internal.SessionManager;
import me.agent.vgui.core.internal.UpdateScheduler;
import me.agent.vgui.core.internal.VGuiServiceImpl;
import me.agent.vgui.core.internal.WindowIdAllocator;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Wires VGui into a running proxy. Used two ways:
 *
 * <ul>
 *   <li><b>Standalone plugin</b>: {@code VGuiPlatform} calls this for you. Drop the
 *       jar in the plugins folder and depend on plugin id {@code vgui}.</li>
 *   <li><b>Shaded library</b>: shade VGui into your own plugin, then call
 *       {@link #loadPacketEvents} from your plugin constructor and {@link #init} from
 *       your {@code ProxyInitializeEvent} handler, and {@link #shutdown} on
 *       {@code ProxyShutdownEvent}.</li>
 * </ul>
 *
 * <p>A shaded integration can share PacketEvents when it deliberately uses the same
 * API classes and class loader. The standalone release embeds the runtime it manages.</p>
 */
public final class VGuiBootstrap {
    private static volatile VGuiBootstrap instance;

    private final Logger logger;
    private final SessionManager sessionManager;
    private final boolean managePacketEvents;
    private final ProxyServer server;
    private final Object pluginInstance;
    private final EventManager packetEventManager;
    private final PacketListenerCommon[] packetListeners;
    private final Object[] velocityListeners;
    private volatile boolean shutdown;

    private VGuiBootstrap(Logger logger, SessionManager sessionManager, boolean managePacketEvents,
                          ProxyServer server, Object pluginInstance, EventManager packetEventManager,
                          PacketListenerCommon[] packetListeners, Object[] velocityListeners) {
        this.logger = logger;
        this.sessionManager = sessionManager;
        this.managePacketEvents = managePacketEvents;
        this.server = server;
        this.pluginInstance = pluginInstance;
        this.packetEventManager = packetEventManager;
        this.packetListeners = packetListeners;
        this.velocityListeners = velocityListeners;
    }

    /** The active bootstrap, or {@code null} before {@link #init}. */
    public static VGuiBootstrap instance() {
        return instance;
    }

    /**
     * Builds and loads the PacketEvents API if no other plugin has installed it yet.
     * Call from your plugin constructor (before the proxy initializes). Safe to call
     * when PacketEvents is already present.
     *
     * @return {@code true} if this call installed PacketEvents (VGui then owns its
     *         lifecycle and will terminate it on shutdown)
     */
    public static synchronized boolean loadPacketEvents(ProxyServer server, PluginContainer container,
                                                        Logger logger, Path dataDirectory) {
        if (PacketEvents.getAPI() != null) {
            logger.info("[VGui] Reusing PacketEvents API installed by another plugin.");
            return false;
        }
        PacketEvents.setAPI(VelocityPacketEventsBuilder.build(server, container, logger, dataDirectory));
        PacketEvents.getAPI().load();
        logger.info("[VGui] PacketEvents API loaded.");
        return true;
    }

    /**
     * Initializes VGui: PacketEvents listeners, session manager, disconnect cleanup and
     * the {@link VGui} service singleton. Call once from {@code ProxyInitializeEvent}.
     *
     * @param server             the proxy
     * @param pluginInstance     your plugin main-class instance (used to register
     *                           Velocity events and scheduler tasks)
     * @param logger             logger for VGui diagnostics
     * @param managePacketEvents whether VGui owns the PacketEvents lifecycle (pass the
     *                           result of {@link #loadPacketEvents})
     */
    public static synchronized VGuiBootstrap init(ProxyServer server, Object pluginInstance,
                                                  Logger logger, boolean managePacketEvents) {
        if (instance != null) {
            logger.warn("[VGui] init() called twice; ignoring the second call.");
            return instance;
        }
        if (PacketEvents.getAPI() == null) {
            throw new IllegalStateException("PacketEvents is not loaded. Call VGuiBootstrap.loadPacketEvents(...) "
                    + "from your plugin constructor first (or install the PacketEvents plugin).");
        }
        if (!PacketEvents.getAPI().isInitialized()) {
            PacketEvents.getAPI().init();
            logger.info("[VGui] PacketEvents API initialized.");
        }

        WindowIdAllocator idAllocator = new WindowIdAllocator();
        InventoryTracker inventoryTracker = new InventoryTracker();
        UpdateScheduler updateScheduler = new UpdateScheduler(server, pluginInstance, logger);
        SessionManager sessionManager = new SessionManager(logger, idAllocator, inventoryTracker, updateScheduler);

        EventManager packetEventManager = PacketEvents.getAPI().getEventManager();
        List<PacketListenerCommon> packetListeners = new ArrayList<>(2);
        List<Object> velocityListeners = new ArrayList<>(2);
        try {
            packetListeners.add(packetEventManager.registerListener(new PacketBridge(logger, sessionManager)));
            packetListeners.add(packetEventManager.registerListener(
                    new ServerPacketBridge(logger, sessionManager, inventoryTracker)));

            PlayerDisconnectListener disconnectListener = new PlayerDisconnectListener(sessionManager);
            BackendConnectionListener backendConnectionListener = new BackendConnectionListener(sessionManager);
            server.getEventManager().register(pluginInstance, disconnectListener);
            velocityListeners.add(disconnectListener);
            server.getEventManager().register(pluginInstance, backendConnectionListener);
            velocityListeners.add(backendConnectionListener);
        } catch (RuntimeException | Error t) {
            try {
                if (!packetListeners.isEmpty()) {
                    packetEventManager.unregisterListeners(packetListeners.toArray(PacketListenerCommon[]::new));
                }
            } catch (Throwable cleanupFailure) {
                t.addSuppressed(cleanupFailure);
            }
            for (Object listener : velocityListeners) {
                try {
                    server.getEventManager().unregisterListener(pluginInstance, listener);
                } catch (Throwable cleanupFailure) {
                    t.addSuppressed(cleanupFailure);
                }
            }
            if (managePacketEvents) {
                try {
                    PacketEvents.getAPI().terminate();
                } catch (Throwable cleanupFailure) {
                    t.addSuppressed(cleanupFailure);
                }
            }
            throw t;
        }

        VGui.setInstance(new VGuiServiceImpl(sessionManager, logger));

        VGuiBootstrap bootstrap = new VGuiBootstrap(
                logger, sessionManager, managePacketEvents, server, pluginInstance, packetEventManager,
                packetListeners.toArray(PacketListenerCommon[]::new), velocityListeners.toArray());
        instance = bootstrap;
        logger.info("[VGui] Service initialized. Ready for use.");
        return bootstrap;
    }

    /** The session manager (internal API, exposed for the platform plugin). */
    public SessionManager sessionManager() {
        return sessionManager;
    }

    /** Closes all views and (if VGui owns it) terminates PacketEvents. */
    public synchronized void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        try {
            packetEventManager.unregisterListeners(packetListeners);
        } catch (Throwable t) {
            logger.warn("[VGui] Error while unregistering PacketEvents listeners.", t);
        }
        for (Object listener : velocityListeners) {
            try {
                server.getEventManager().unregisterListener(pluginInstance, listener);
            } catch (Throwable t) {
                logger.warn("[VGui] Error while unregistering a Velocity listener.", t);
            }
        }
        try {
            sessionManager.closeAll(CloseReason.SHUTDOWN);
        } catch (Throwable t) {
            logger.warn("[VGui] Error while closing sessions on shutdown.", t);
        }
        if (managePacketEvents) {
            try {
                PacketEvents.getAPI().terminate();
                logger.info("[VGui] PacketEvents API terminated.");
            } catch (Throwable t) {
                logger.warn("[VGui] Error while terminating PacketEvents API.", t);
            }
        }
        VGui.setInstance(null);
        instance = null;
    }
}

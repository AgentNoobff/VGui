package me.agent.vgui.core;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import me.agent.vgui.core.demo.DemoCommand;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Standalone plugin entry point. Loads the embedded PacketEvents runtime, initializes
 * {@link VGuiBootstrap} and registers the {@code /vgui} demo command.
 */
public final class VGuiPlatform {
    private final ProxyServer server;
    private final Logger logger;
    private final boolean managePacketEvents;
    private VGuiBootstrap bootstrap;

    @Inject
    public VGuiPlatform(ProxyServer server, PluginContainer pluginContainer, Logger logger,
                        @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.managePacketEvents = VGuiBootstrap.loadPacketEvents(server, pluginContainer, logger, dataDirectory);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        bootstrap = VGuiBootstrap.init(server, this, logger, managePacketEvents);

        CommandManager commands = server.getCommandManager();
        CommandMeta meta = commands.metaBuilder("vgui").plugin(this).build();
        commands.register(meta, new DemoCommand());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (bootstrap != null) {
            bootstrap.shutdown();
        }
    }
}

package me.agent.vgui.testutil;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.NettyManager;

/**
 * Installs a minimal PacketEvents API stub so protocol registries (ItemTypes,
 * ItemStack, ...) can class-initialize inside plain unit tests.
 */
public final class TestPacketEvents {

    private TestPacketEvents() {
    }

    public static synchronized void ensureInitialized() {
        if (PacketEvents.getAPI() != null) {
            return;
        }
        PacketEvents.setAPI(new StubApi());
    }

    private static final class StubApi extends PacketEventsAPI<Object> {
        private final ServerManager serverManager = () -> ServerVersion.V_1_21;

        @Override
        public void load() {
        }

        @Override
        public boolean isLoaded() {
            return true;
        }

        @Override
        public void init() {
        }

        @Override
        public boolean isInitialized() {
            return true;
        }

        @Override
        public void terminate() {
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public Object getPlugin() {
            return null;
        }

        @Override
        public ServerManager getServerManager() {
            return serverManager;
        }

        @Override
        public ProtocolManager getProtocolManager() {
            return null;
        }

        @Override
        public PlayerManager getPlayerManager() {
            return null;
        }

        @Override
        public NettyManager getNettyManager() {
            return new io.github.retrooper.packetevents.impl.netty.NettyManagerImpl();
        }

        @Override
        public ChannelInjector getInjector() {
            return null;
        }
    }
}

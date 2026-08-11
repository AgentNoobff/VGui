package me.agent.vgui.core.internal;

import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import me.agent.vgui.api.CloseReason;
import me.agent.vgui.api.View;
import me.agent.vgui.api.ViewType;
import me.agent.vgui.api.click.ClickType;
import me.agent.vgui.api.contents.ViewContents;
import me.agent.vgui.api.event.VGuiListener;
import me.agent.vgui.api.item.ViewItem;
import me.agent.vgui.testutil.TestPacketEvents;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionManagerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionManagerTest.class);

    @BeforeAll
    static void setupPacketEvents() {
        TestPacketEvents.ensureInitialized();
    }

    @Test
    void reentrantCloseTransitionWinsWithoutStaleOpenWork() {
        Fixture fixture = fixture(true);
        AtomicInteger requestedOpen = new AtomicInteger();
        AtomicInteger requestedClose = new AtomicInteger();
        View callbackView = view("callback");
        View requested = new View() {
            @Override
            public Component title() {
                return Component.text("requested");
            }

            @Override
            public ViewType type() {
                return ViewType.CHEST_9X1;
            }

            @Override
            public void onOpen(ViewContents contents) {
                requestedOpen.incrementAndGet();
            }

            @Override
            public void onClose(Player player, CloseReason reason,
                                me.agent.vgui.api.context.ViewContext context) {
                requestedClose.incrementAndGet();
            }
        };
        View first = new View() {
            @Override
            public Component title() {
                return Component.text("first");
            }

            @Override
            public ViewType type() {
                return ViewType.CHEST_9X1;
            }

            @Override
            public void onClose(Player player, CloseReason reason,
                                me.agent.vgui.api.context.ViewContext context) {
                if (reason == CloseReason.SWITCHED) {
                    fixture.manager.open(player, callbackView, null, false);
                }
            }
        };

        assertTrue(fixture.manager.tryOpen(fixture.player, first, null, false));
        assertFalse(fixture.manager.tryOpen(fixture.player, requested, null, true));

        assertSame(callbackView, fixture.manager.getSession(fixture.uuid).view());
        assertEquals(0, requestedOpen.get());
        assertEquals(1, requestedClose.get());
    }

    @Test
    void staleContentsCannotAffectNewerSession() {
        Fixture fixture = fixture(true);
        View first = view("first");
        View second = view("second");

        assertTrue(fixture.manager.tryOpen(fixture.player, first, null, false));
        ViewContents stale = fixture.manager.getSession(fixture.uuid).contents();
        assertTrue(fixture.manager.tryOpen(fixture.player, second, null, true));

        stale.close();
        stale.open(view("third"));
        stale.openReplacing(view("fourth"));
        stale.set(0, ViewItem.of(com.github.retrooper.packetevents.protocol.item.ItemStack.EMPTY));
        assertFalse(stale.back());

        assertSame(second, fixture.manager.getSession(fixture.uuid).view());
        assertNull(stale.get(0));
    }

    @Test
    void promptCompletesWhenReplacedOrOpenFails() {
        Fixture fixture = fixture(true);
        VGuiServiceImpl service = new VGuiServiceImpl(fixture.manager, LOGGER);

        CompletableFuture<String> replaced = service.prompt(
                fixture.player, Component.text("input"), "start");
        assertFalse(replaced.isDone());
        assertTrue(fixture.manager.tryOpen(fixture.player, view("replacement"), null, false));
        assertTrue(replaced.isDone());
        assertNull(replaced.join());

        Fixture missingUser = fixture(false);
        CompletableFuture<String> failed = new VGuiServiceImpl(missingUser.manager, LOGGER)
                .prompt(missingUser.player, Component.text("input"));
        assertTrue(failed.isDone());
        assertNull(failed.join());
    }

    @Test
    void shutdownCallbacksCannotReopenSessions() {
        Fixture fixture = fixture(true);
        AtomicReference<Boolean> reopened = new AtomicReference<>();
        View view = new View() {
            @Override
            public Component title() {
                return Component.text("shutdown");
            }

            @Override
            public ViewType type() {
                return ViewType.CHEST_9X1;
            }

            @Override
            public void onClose(Player player, CloseReason reason,
                                me.agent.vgui.api.context.ViewContext context) {
                reopened.set(fixture.manager.tryOpen(player, SessionManagerTest.view("reopened"), null, false));
            }
        };

        assertTrue(fixture.manager.tryOpen(fixture.player, view, null, false));
        fixture.manager.closeAll(CloseReason.SHUTDOWN);

        assertEquals(Boolean.FALSE, reopened.get());
        assertNull(fixture.manager.getSession(fixture.uuid));
        assertFalse(fixture.manager.tryOpen(fixture.player, view("after"), null, false));
    }

    @Test
    void listenerExceptionsDenyClicks() {
        Fixture fixture = fixture(true);
        assertTrue(fixture.manager.tryOpen(fixture.player, view("click"), null, false));
        ViewSession session = fixture.manager.getSession(fixture.uuid);
        fixture.manager.addListener(new VGuiListener() {
            @Override
            public boolean onClick(me.agent.vgui.api.click.ClickContext click) {
                throw new IllegalStateException("deny by failure");
            }
        });
        ClickContextImpl context = fixture.manager.createClickContext(
                session, 0, ClickType.LEFT, -1, 0, 0, 1);

        assertFalse(fixture.manager.allowClick(session, context));
    }

    @Test
    void stateIdsRejectStaleClicksButAllowLegacyPackets() {
        Fixture fixture = fixture(true);
        assertTrue(fixture.manager.tryOpen(fixture.player, view("state"), null, false));
        ViewSession session = fixture.manager.getSession(fixture.uuid);

        assertTrue(session.acceptsStateId(1));
        assertFalse(session.acceptsStateId(0));
        assertTrue(session.acceptsStateId(-1));

        assertTrue(fixture.manager.sendFull(session));
        assertTrue(session.acceptsStateId(2));
        assertFalse(session.acceptsStateId(1));
    }

    @Test
    void changedTitlesSurviveHistoryNavigation() {
        Fixture fixture = fixture(true);
        View first = view("first");
        Component changed = Component.text("changed");

        assertTrue(fixture.manager.tryOpen(fixture.player, first, null, false));
        fixture.manager.getSession(fixture.uuid).contents().title(changed);
        assertTrue(fixture.manager.tryOpen(fixture.player, view("second"), null, true));
        assertTrue(fixture.manager.back(fixture.player));

        ViewSession restored = fixture.manager.getSession(fixture.uuid);
        assertSame(first, restored.view());
        assertEquals(changed, restored.title());
    }

    @Test
    void failedBackKeepsHistoryEntryForRetry() {
        Fixture fixture = fixture(true);
        AtomicReference<Boolean> fail = new AtomicReference<>(false);
        View first = new View() {
            @Override
            public Component title() {
                return Component.text("first");
            }

            @Override
            public ViewType type() {
                if (fail.get()) {
                    throw new IllegalStateException("temporary failure");
                }
                return ViewType.CHEST_9X1;
            }
        };

        assertTrue(fixture.manager.tryOpen(fixture.player, first, null, false));
        View second = view("second");
        assertTrue(fixture.manager.tryOpen(fixture.player, second, null, true));
        fail.set(true);
        assertFalse(fixture.manager.back(fixture.player));
        assertSame(second, fixture.manager.getSession(fixture.uuid).view());

        fail.set(false);
        assertTrue(fixture.manager.back(fixture.player));
        assertSame(first, fixture.manager.getSession(fixture.uuid).view());
    }

    @Test
    void liveSendFailureRetiresTheSession() {
        Fixture fixture = fixture(true);
        assertTrue(fixture.manager.tryOpen(fixture.player, view("send"), null, false));
        ViewSession session = fixture.manager.getSession(fixture.uuid);

        fixture.user.failSends = true;
        assertFalse(fixture.manager.sendFull(session));
        assertNull(fixture.manager.getSession(fixture.uuid));
        assertFalse(session.isOpen());
    }

    @Test
    void onOpenFailureCancelsTasksScheduledBeforeTheFailure() {
        UUID uuid = UUID.randomUUID();
        Player player = player(uuid);
        RecordingUser user = new RecordingUser(uuid);
        AtomicBoolean taskCancelled = new AtomicBoolean();
        ScheduledTask scheduledTask = (ScheduledTask) Proxy.newProxyInstance(
                ScheduledTask.class.getClassLoader(), new Class<?>[]{ScheduledTask.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("cancel")) {
                        taskCancelled.set(true);
                    }
                    return defaultValue(method.getReturnType());
                });
        AtomicReference<Scheduler.TaskBuilder> builderRef = new AtomicReference<>();
        Scheduler.TaskBuilder taskBuilder = (Scheduler.TaskBuilder) Proxy.newProxyInstance(
                Scheduler.TaskBuilder.class.getClassLoader(), new Class<?>[]{Scheduler.TaskBuilder.class},
                (proxy, method, args) -> method.getName().equals("schedule") ? scheduledTask : builderRef.get());
        builderRef.set(taskBuilder);
        Scheduler scheduler = (Scheduler) Proxy.newProxyInstance(
                Scheduler.class.getClassLoader(), new Class<?>[]{Scheduler.class},
                (proxy, method, args) -> method.getName().equals("buildTask")
                        ? taskBuilder : defaultValue(method.getReturnType()));
        ProxyServer server = (ProxyServer) Proxy.newProxyInstance(
                ProxyServer.class.getClassLoader(), new Class<?>[]{ProxyServer.class},
                (proxy, method, args) -> method.getName().equals("getScheduler")
                        ? scheduler : defaultValue(method.getReturnType()));
        SessionManager manager = new SessionManager(
                LOGGER, new WindowIdAllocator(), new InventoryTracker(),
                new UpdateScheduler(server, new Object(), LOGGER), ignored -> user);
        View failing = new View() {
            @Override
            public Component title() {
                return Component.text("failing");
            }

            @Override
            public ViewType type() {
                return ViewType.CHEST_9X1;
            }

            @Override
            public void onOpen(ViewContents contents) {
                contents.schedule(Duration.ofSeconds(1), ignored -> { });
                throw new IllegalStateException("after scheduling");
            }
        };

        assertFalse(manager.tryOpen(player, failing, null, false));
        assertTrue(taskCancelled.get());
        assertNull(manager.getSession(uuid));
    }

    @Test
    void openingRelinquishesTrackedBackendWindow() {
        Fixture fixture = fixture(true);
        fixture.manager.backendOpened(fixture.uuid, 7);

        assertTrue(fixture.manager.tryOpen(fixture.player, view("proxy"), null, false));
        assertEquals(1, fixture.user.receivedPackets.size());
        WrapperPlayClientCloseWindow close =
                (WrapperPlayClientCloseWindow) fixture.user.receivedPackets.get(0);
        assertEquals(7, close.getWindowId());

        assertTrue(fixture.manager.backendClosed(fixture.uuid, 7));
        assertTrue(fixture.manager.getSession(fixture.uuid).isOpen());
    }

    @Test
    void backendSwitchBlocksOpeningUntilConnected() {
        Fixture fixture = fixture(true);
        fixture.manager.backendSwitchStarted(fixture.uuid);
        assertFalse(fixture.manager.tryOpen(fixture.player, view("switching"), null, false));

        fixture.manager.backendConnected(fixture.uuid);
        assertTrue(fixture.manager.tryOpen(fixture.player, view("connected"), null, false));
    }

    @Test
    void completedBackendSwitchClosesOldViewAndBlocksCallbackReopen() {
        Fixture fixture = fixture(true);
        AtomicReference<CloseReason> reason = new AtomicReference<>();
        AtomicReference<Boolean> reopened = new AtomicReference<>();
        View oldView = new View() {
            @Override
            public Component title() {
                return Component.text("old-backend");
            }

            @Override
            public ViewType type() {
                return ViewType.CHEST_9X1;
            }

            @Override
            public void onClose(Player player, CloseReason closeReason,
                                me.agent.vgui.api.context.ViewContext context) {
                reason.set(closeReason);
                reopened.set(fixture.manager.tryOpen(player, view("too-early"), null, false));
            }
        };

        assertTrue(fixture.manager.tryOpen(fixture.player, oldView, null, false));
        fixture.manager.backendSwitchStarted(fixture.uuid);
        fixture.manager.backendConnected(fixture.uuid);

        assertEquals(CloseReason.OVERRIDDEN, reason.get());
        assertEquals(Boolean.FALSE, reopened.get());
        assertNull(fixture.manager.getSession(fixture.uuid));
    }

    @Test
    void failedBackendSwitchReleasesGateWithoutRestoringStaleInventory() {
        Fixture fixture = fixture(true);
        fixture.manager.backendSwitchStarted(fixture.uuid);
        fixture.manager.backendSwitchAborted(fixture.uuid);

        assertTrue(fixture.manager.tryOpen(fixture.player, view("old-backend-safe-empty"), null, false));
    }

    @Test
    void reentrantOpenReclaimsPendingBackendWindow() {
        Fixture fixture = fixture(true);
        View replacement = view("replacement");
        View first = new View() {
            @Override
            public Component title() {
                return Component.text("first");
            }

            @Override
            public ViewType type() {
                return ViewType.CHEST_9X1;
            }

            @Override
            public void onClose(Player player, CloseReason reason,
                                me.agent.vgui.api.context.ViewContext context) {
                if (reason == CloseReason.OVERRIDDEN) {
                    fixture.manager.tryOpen(player, replacement, null, false);
                }
            }
        };

        assertTrue(fixture.manager.tryOpen(fixture.player, first, null, false));
        assertTrue(fixture.manager.backendOpened(fixture.uuid, 11));
        assertSame(replacement, fixture.manager.getSession(fixture.uuid).view());
        WrapperPlayClientCloseWindow close = (WrapperPlayClientCloseWindow)
                fixture.user.receivedPackets.get(fixture.user.receivedPackets.size() - 1);
        assertEquals(11, close.getWindowId());
    }

    @Test
    void coordinateAndPaginationValidationDoNotAliasSlots() {
        Fixture fixture = fixture(true);
        assertTrue(fixture.manager.tryOpen(fixture.player, view("geometry"), null, false));
        ViewContents contents = fixture.manager.getSession(fixture.uuid).contents();
        ViewItem item = ViewItem.of(com.github.retrooper.packetevents.protocol.item.ItemStack.EMPTY);

        assertThrows(IllegalArgumentException.class, () -> contents.set(0, 9, item));
        assertThrows(IllegalArgumentException.class, () -> contents.set(1, 0, item));
        assertThrows(IllegalArgumentException.class, () -> contents.pagination().slots(0, 0));
    }

    private static Fixture fixture(boolean withUser) {
        UUID uuid = UUID.randomUUID();
        Player player = player(uuid);
        RecordingUser user = new RecordingUser(uuid);
        SessionManager manager = new SessionManager(
                LOGGER,
                new WindowIdAllocator(),
                new InventoryTracker(),
                new UpdateScheduler(null, new Object(), LOGGER),
                ignored -> withUser ? user : null);
        return new Fixture(uuid, player, user, manager);
    }

    private static View view(String title) {
        return new View() {
            @Override
            public Component title() {
                return Component.text(title);
            }

            @Override
            public ViewType type() {
                return ViewType.CHEST_9X1;
            }
        };
    }

    private static Player player(UUID uuid) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> uuid;
                    case "getUsername" -> "test-player";
                    case "isActive" -> true;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "TestPlayer[" + uuid + "]";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }

    private record Fixture(UUID uuid, Player player, RecordingUser user, SessionManager manager) {
    }

    private static final class RecordingUser extends User {
        private final List<PacketWrapper<?>> packets = new ArrayList<>();
        private final List<PacketWrapper<?>> receivedPackets = new ArrayList<>();
        private volatile boolean failSends;

        private RecordingUser(UUID uuid) {
            super(new Object(), ConnectionState.PLAY, ClientVersion.V_1_21, new UserProfile(uuid, "test-player"));
        }

        @Override
        public void sendPacketSilently(PacketWrapper<?> packet) {
            if (failSends) {
                throw new IllegalStateException("simulated send failure");
            }
            packets.add(packet);
        }

        @Override
        public void receivePacketSilently(PacketWrapper<?> packet) {
            receivedPackets.add(packet);
        }
    }
}

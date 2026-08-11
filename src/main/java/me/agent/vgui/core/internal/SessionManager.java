package me.agent.vgui.core.internal;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.sound.Sound;
import com.github.retrooper.packetevents.protocol.sound.SoundCategory;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntitySoundEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import com.velocitypowered.api.proxy.Player;
import me.agent.vgui.api.CloseReason;
import me.agent.vgui.api.View;
import me.agent.vgui.api.ViewType;
import me.agent.vgui.api.context.ViewContext;
import me.agent.vgui.api.contents.UpdateTask;
import me.agent.vgui.api.contents.ViewContents;
import me.agent.vgui.api.event.VGuiListener;
import me.agent.vgui.api.item.ViewItem;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Owns all view sessions: opening, closing, navigation history, packet pushes and
 * global listener dispatch. Operations for one player are serialized, including
 * lifecycle, click and scheduled callbacks. Reentrant callbacks may transition to a
 * different view; the outer operation then observes that it is stale and stops.
 */
public final class SessionManager {
    private static final int HISTORY_LIMIT = 10;
    private static final int PLAYER_INV_SLOTS = 36;
    private static final int MAX_TARGETED_CORRECTIONS = 12;
    private static final int PLAYER_LOCK_COUNT = 64;

    private final Logger logger;
    private final WindowIdAllocator idAllocator;
    private final InventoryTracker inventoryTracker;
    private final UpdateScheduler updateScheduler;
    private final Function<Player, User> userProvider;
    private final Map<UUID, ViewSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<HistoryEntry>> histories = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> backendWindows = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> claimedBackendWindows = new ConcurrentHashMap<>();
    private final List<VGuiListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<UUID, Integer> blockedPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> activeOpens = new ConcurrentHashMap<>();
    private final Object[] playerLocks = new Object[PLAYER_LOCK_COUNT];
    private final Object closeAllLock = new Object();
    private final AtomicInteger openBlockers = new AtomicInteger();
    private volatile boolean shutdown;

    private record HistoryEntry(View view, ViewContext context, Component title) {
    }

    public SessionManager(Logger logger, WindowIdAllocator idAllocator,
                          InventoryTracker inventoryTracker, UpdateScheduler updateScheduler) {
        this(logger, idAllocator, inventoryTracker, updateScheduler, player -> {
            if (PacketEvents.getAPI() == null || PacketEvents.getAPI().getPlayerManager() == null) {
                return null;
            }
            return PacketEvents.getAPI().getPlayerManager().getUser(player);
        });
    }

    SessionManager(Logger logger, WindowIdAllocator idAllocator, InventoryTracker inventoryTracker,
                   UpdateScheduler updateScheduler, Function<Player, User> userProvider) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.idAllocator = Objects.requireNonNull(idAllocator, "idAllocator");
        this.inventoryTracker = Objects.requireNonNull(inventoryTracker, "inventoryTracker");
        this.updateScheduler = Objects.requireNonNull(updateScheduler, "updateScheduler");
        this.userProvider = Objects.requireNonNull(userProvider, "userProvider");
        for (int i = 0; i < playerLocks.length; i++) {
            playerLocks[i] = new Object();
        }
    }

    /* ------------------------------------------------------------------ open/close */

    /**
     * Opens a view. Replaces any current session without close-window flicker; when
     * {@code pushHistory} is set the replaced view goes onto the player's back-stack.
     */
    public void open(Player player, View view, ViewContext context, boolean pushHistory) {
        tryOpen(player, view, context, pushHistory);
    }

    /**
     * Same transition as {@link #open(Player, View, ViewContext, boolean)}, with a
     * result for internal callers.
     *
     * @return whether this view remained current through all open callbacks and sends
     */
    boolean tryOpen(Player player, View view, ViewContext context, boolean pushHistory) {
        if (player == null || view == null) {
            throw new IllegalArgumentException("player and view must not be null");
        }
        UUID uuid = player.getUniqueId();
        increment(activeOpens, uuid);
        try {
            synchronized (lockFor(uuid)) {
                if (!canOpen(player, uuid)) {
                    return false;
                }
                User user = findUser(player);
                if (user == null) {
                    logger.warn("[VGui] Cannot open view for {}: no PacketEvents user (player not fully connected?)",
                            player.getUsername());
                    return false;
                }
                return openLocked(player, user, view, context, pushHistory, null);
            }
        } finally {
            decrement(activeOpens, uuid);
        }
    }

    /** Opens from a live session; stale click/content callbacks are ignored. */
    boolean open(ViewSession source, View view, ViewContext context, boolean pushHistory) {
        Objects.requireNonNull(view, "view");
        synchronized (lockFor(source.playerId())) {
            if (!isCurrentLocked(source) || !canOpen(source.player(), source.playerId())) {
                return false;
            }
            return openLocked(source.player(), source.user(), view, context, pushHistory, null);
        }
    }

    private boolean openLocked(Player player, User user, View view, ViewContext context,
                               boolean pushHistory, Component restoredTitle) {
        UUID uuid = player.getUniqueId();
        final ViewType type;
        final Component title;
        try {
            type = Objects.requireNonNull(view.type(), "view.type()");
            title = restoredTitle != null
                    ? restoredTitle
                    : Objects.requireNonNull(view.title(), "view.title()");
        } catch (Throwable t) {
            logger.warn("[VGui] Failed to inspect view {} for {}", view.getClass().getName(), uuid, t);
            return false;
        }

        if (!claimBackendWindowLocked(uuid, user)) {
            return false;
        }

        int windowId = idAllocator.nextId(uuid);
        ViewContext ctx = context != null ? context : new DefaultViewContext();
        ViewSession session = new ViewSession(player, user, windowId, view, type, ctx, title);
        DefaultViewContents contents = new DefaultViewContents(this, session);
        session.attachContents(contents);

        // Install the replacement before closing the old view. If an old close callback
        // navigates again, that newer transition wins and this open stops below.
        ViewSession previous = sessions.put(uuid, session);
        if (previous != null && previous.isOpen()) {
            if (pushHistory) {
                pushHistoryLocked(uuid, new HistoryEntry(previous.view(), previous.context(), previous.title()));
            }
            retireSession(previous, CloseReason.SWITCHED, false, false);
        }
        if (!isCurrentLocked(session)) {
            return false;
        }

        try {
            WindowTypeResolver.openWindow(user, windowId, type, title);
        } catch (Throwable t) {
            logger.warn("[VGui] Failed to open window for {}", uuid, t);
            return abortOpenLocked(session, true);
        }
        if (!isCurrentLocked(session)) {
            return false;
        }

        try {
            contents.populate(view);
        } catch (Throwable t) {
            logger.warn("[VGui] Exception in onOpen of {} for {}", view.getClass().getName(), uuid, t);
            return abortOpenLocked(session, true);
        }
        if (!isCurrentLocked(session)) {
            return false;
        }
        if (!sendFullLocked(session)) {
            return abortOpenLocked(session, true);
        }

        for (VGuiListener listener : listeners) {
            if (!isCurrentLocked(session)) {
                return false;
            }
            try {
                listener.onOpen(player, view, contents);
            } catch (Throwable t) {
                logger.warn("[VGui] Exception in VGuiListener.onOpen", t);
            }
        }
        return isCurrentLocked(session);
    }

    private boolean abortOpenLocked(ViewSession session, boolean sendClosePacket) {
        if (sessions.remove(session.playerId(), session)) {
            retireSession(session, CloseReason.SERVER, sendClosePacket, false);
        }
        return false;
    }

    private void failCurrentSessionLocked(ViewSession session) {
        if (sessions.remove(session.playerId(), session)) {
            retireSession(session, CloseReason.SERVER, false, true);
        }
    }

    /** Closes the player's session (server-initiated), sending a close packet. */
    public void close(Player player) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        synchronized (lockFor(uuid)) {
            ViewSession session = sessions.remove(uuid);
            if (session != null) {
                retireSession(session, CloseReason.SERVER, true, true);
            }
        }
    }

    /** Closes only the originating session; stale callbacks cannot close a newer view. */
    boolean close(ViewSession expected) {
        synchronized (lockFor(expected.playerId())) {
            if (!sessions.remove(expected.playerId(), expected)) {
                return false;
            }
            retireSession(expected, CloseReason.SERVER, true, true);
            return true;
        }
    }

    /** The client told us it closed this exact window. */
    public void clientClosed(ViewSession expected) {
        synchronized (lockFor(expected.playerId())) {
            if (sessions.remove(expected.playerId(), expected)) {
                retireSession(expected, CloseReason.CLIENT, false, true);
            }
        }
    }

    /** Compatibility overload that closes whichever session is current at dispatch. */
    public void clientClosed(UUID uuid) {
        synchronized (lockFor(uuid)) {
            ViewSession session = sessions.remove(uuid);
            if (session != null) {
                retireSession(session, CloseReason.CLIENT, false, true);
            }
        }
    }

    /** A backend server took over the player's screen. */
    public void overridden(ViewSession expected) {
        synchronized (lockFor(expected.playerId())) {
            if (sessions.remove(expected.playerId(), expected)) {
                retireSession(expected, CloseReason.OVERRIDDEN, false, true);
            }
        }
    }

    /** Atomically lets a backend window take ownership from the current proxy view. */
    public void overridden(UUID uuid) {
        synchronized (lockFor(uuid)) {
            ViewSession session = sessions.remove(uuid);
            if (session != null) {
                retireSession(session, CloseReason.OVERRIDDEN, false, true);
            }
        }
    }

    /** The player disconnected from the proxy. */
    public void disconnected(UUID uuid) {
        increment(blockedPlayers, uuid);
        try {
            synchronized (lockFor(uuid)) {
                ViewSession session = sessions.remove(uuid);
                if (session != null) {
                    retireSession(session, CloseReason.DISCONNECT, false, true);
                }
                histories.remove(uuid);
                backendWindows.remove(uuid);
                claimedBackendWindows.remove(uuid);
                inventoryTracker.evict(uuid);
                idAllocator.forget(uuid);
            }
        } finally {
            decrement(blockedPlayers, uuid);
        }
    }

    /** Closes every session. Shutdown permanently rejects any callback-driven reopen. */
    public void closeAll(CloseReason reason) {
        synchronized (closeAllLock) {
            openBlockers.incrementAndGet();
            if (reason == CloseReason.SHUTDOWN) {
                shutdown = true;
            }
            try {
                HashSet<UUID> affectedPlayers = new HashSet<>(sessions.keySet());
                affectedPlayers.addAll(activeOpens.keySet());
                for (UUID uuid : affectedPlayers) {
                    synchronized (lockFor(uuid)) {
                        ViewSession session = sessions.remove(uuid);
                        if (session != null) {
                            retireSession(session, reason, true, true);
                        }
                    }
                }
                histories.clear();
                backendWindows.clear();
                claimedBackendWindows.clear();
                if (reason == CloseReason.SHUTDOWN) {
                    inventoryTracker.clear();
                    idAllocator.clear();
                    listeners.clear();
                    blockedPlayers.clear();
                    activeOpens.clear();
                }
            } finally {
                openBlockers.decrementAndGet();
            }
        }
    }

    private void retireSession(ViewSession session, CloseReason reason,
                               boolean sendClosePacket, boolean clearHistory) {
        if (!session.markClosed()) {
            return;
        }
        session.cancelTasks();
        if (sendClosePacket) {
            try {
                session.user().sendPacketSilently(new WrapperPlayServerCloseWindow(session.windowId()));
            } catch (Throwable t) {
                logger.warn("[VGui] Failed to send close-window packet to {}", session.playerId(), t);
            }
        }
        if (clearHistory) {
            histories.remove(session.playerId());
        }
        fireClose(session, reason);
    }

    private void fireClose(ViewSession session, CloseReason reason) {
        try {
            session.view().onClose(session.player(), reason, session.context());
        } catch (Throwable t) {
            logger.warn("[VGui] Exception in onClose ({}) for {}", reason, session.playerId(), t);
        }
        for (VGuiListener listener : listeners) {
            try {
                listener.onClose(session.player(), session.view(), reason);
            } catch (Throwable t) {
                logger.warn("[VGui] Exception in VGuiListener.onClose", t);
            }
        }
    }

    /* ------------------------------------------------------------------ history */

    private void pushHistoryLocked(UUID uuid, HistoryEntry entry) {
        Deque<HistoryEntry> history = histories.computeIfAbsent(uuid, ignored -> new ArrayDeque<>());
        history.push(entry);
        while (history.size() > HISTORY_LIMIT) {
            history.removeLast();
        }
    }

    /** Reopens the previous view in the player's history, if any. */
    public boolean back(Player player) {
        if (player == null) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        synchronized (lockFor(uuid)) {
            ViewSession current = sessions.get(uuid);
            if (current == null || !isCurrentLocked(current) || !canOpen(player, uuid)) {
                return false;
            }
            return backLocked(current);
        }
    }

    /** Navigates back only if the originating session is still current. */
    boolean back(ViewSession expected) {
        synchronized (lockFor(expected.playerId())) {
            if (!isCurrentLocked(expected) || !canOpen(expected.player(), expected.playerId())) {
                return false;
            }
            return backLocked(expected);
        }
    }

    private boolean backLocked(ViewSession current) {
        Deque<HistoryEntry> history = histories.get(current.playerId());
        HistoryEntry previous = history != null ? history.peek() : null;
        if (previous == null) {
            return false;
        }
        if (!openLocked(current.player(), current.user(), previous.view(), previous.context(), false,
                previous.title())) {
            return false;
        }
        removeHistoryEntryLocked(current.playerId(), history, previous);
        return true;
    }

    private void removeHistoryEntryLocked(UUID uuid, Deque<HistoryEntry> history, HistoryEntry entry) {
        for (Iterator<HistoryEntry> iterator = history.iterator(); iterator.hasNext();) {
            if (iterator.next() == entry) {
                iterator.remove();
                break;
            }
        }
        if (history.isEmpty()) {
            histories.remove(uuid, history);
        }
    }

    /* --------------------------------------------------------- backend ownership */

    /** Invalidates backend-scoped state before a connection attempt. */
    public void backendSwitchStarted(UUID uuid) {
        synchronized (lockFor(uuid)) {
            inventoryTracker.beginBackendSwitch(uuid);
        }
    }

    /** Activates the latest backend generation after Velocity finishes connecting. */
    public void backendConnected(UUID uuid) {
        synchronized (lockFor(uuid)) {
            if (!inventoryTracker.isSwitching(uuid)) {
                inventoryTracker.beginBackendSwitch(uuid);
            }
            backendWindows.remove(uuid);
            claimedBackendWindows.remove(uuid);
            ViewSession session = sessions.remove(uuid);
            if (session != null) {
                // Keep the switch gate closed through callbacks so a closing prompt or
                // view cannot immediately reopen against an inventory with no baseline.
                retireSession(session, CloseReason.OVERRIDDEN, true, true);
            }
            inventoryTracker.backendConnected(uuid);
        }
    }

    /** Releases the open gate after a failed backend connection attempt. */
    public void backendSwitchAborted(UUID uuid) {
        synchronized (lockFor(uuid)) {
            inventoryTracker.backendSwitchAborted(uuid);
        }
    }

    /** Records a backend container and lets it take ownership from a proxy view. */
    boolean backendOpened(UUID uuid, int windowId) {
        synchronized (lockFor(uuid)) {
            backendWindows.put(uuid, windowId);
            claimedBackendWindows.remove(uuid);
            ViewSession session = sessions.remove(uuid);
            if (session != null) {
                retireSession(session, CloseReason.OVERRIDDEN, false, true);
            }
            // A close callback may have synchronously reclaimed ownership. The
            // still-pending backend packet must not overwrite that newer view.
            return Objects.equals(claimedBackendWindows.get(uuid), windowId);
        }
    }

    /**
     * Records a backend close. Returns true when this is the expected close for a
     * container VGui already relinquished, so the stale client-bound packet must be
     * suppressed instead of closing the proxy window that replaced it.
     */
    boolean backendClosed(UUID uuid, int windowId) {
        synchronized (lockFor(uuid)) {
            if (claimedBackendWindows.remove(uuid, windowId)) {
                return true;
            }
            backendWindows.remove(uuid, windowId);
            ViewSession session = sessions.remove(uuid);
            if (session != null) {
                retireSession(session, CloseReason.OVERRIDDEN, false, true);
            }
            return sessions.containsKey(uuid);
        }
    }

    private boolean claimBackendWindowLocked(UUID uuid, User user) {
        Integer backendWindow = backendWindows.remove(uuid);
        if (backendWindow == null || backendWindow == 0) {
            return true;
        }
        try {
            user.receivePacketSilently(new WrapperPlayClientCloseWindow(backendWindow));
            claimedBackendWindows.put(uuid, backendWindow);
            return true;
        } catch (Throwable t) {
            backendWindows.put(uuid, backendWindow);
            logger.warn("[VGui] Failed to close backend window {} before opening a proxy view for {}",
                    backendWindow, uuid, t);
            return false;
        }
    }

    /* ------------------------------------------------------------------ packets */

    void sendSlot(ViewSession session, int slot, ViewItem item) {
        synchronized (lockFor(session.playerId())) {
            if (!isCurrentLocked(session)) {
                return;
            }
            ItemStack stack = item != null ? item.item() : ItemStack.EMPTY;
            try {
                session.user().sendPacketSilently(
                        new WrapperPlayServerSetSlot(session.windowId(), session.nextStateId(), slot, stack));
            } catch (Throwable t) {
                logger.warn("[VGui] Failed to send slot {} to {}", slot, session.playerId(), t);
                failCurrentSessionLocked(session);
            }
        }
    }

    /** Pushes the full window: GUI contents plus the player's tracked inventory. */
    boolean sendFull(ViewSession session) {
        synchronized (lockFor(session.playerId())) {
            if (!isCurrentLocked(session)) {
                return false;
            }
            if (sendFullLocked(session)) {
                return true;
            }
            failCurrentSessionLocked(session);
            return false;
        }
    }

    private boolean sendFullLocked(ViewSession session) {
        DefaultViewContents contents = session.contents();
        if (contents == null) {
            return false;
        }
        ViewItem[] gui = contents.snapshot();
        ItemStack[] playerInv = inventoryTracker.mainAndHotbar(session.playerId());
        List<ItemStack> all = new ArrayList<>(gui.length + PLAYER_INV_SLOTS);
        for (ViewItem item : gui) {
            all.add(item != null ? item.item() : ItemStack.EMPTY);
        }
        all.addAll(List.of(playerInv));
        try {
            session.user().sendPacketSilently(
                    new WrapperPlayServerWindowItems(session.windowId(), session.nextStateId(), all, ItemStack.EMPTY));
            return true;
        } catch (Throwable t) {
            logger.warn("[VGui] Failed to send window contents to {}", session.playerId(), t);
            return false;
        }
    }

    /** Full resync after a rejected click: window contents and an empty cursor. */
    void resync(ViewSession session) {
        sendFull(session);
    }

    /**
     * Corrects only slots the client predicted for a cancelled click, plus the
     * cursor. Large multi-slot changes fall back to one full-window packet.
     */
    void correctClick(ViewSession session, int clickedSlot, Collection<Integer> changedSlots,
                      boolean forceFull, boolean correctOffhand) {
        synchronized (lockFor(session.playerId())) {
            if (!isCurrentLocked(session)) {
                return;
            }
            int totalSlots = session.size() + PLAYER_INV_SLOTS;
            LinkedHashSet<Integer> corrections = new LinkedHashSet<>();
            if (changedSlots != null) {
                for (Integer slot : changedSlots) {
                    if (slot != null && slot >= 0 && slot < totalSlots) {
                        corrections.add(slot);
                    }
                }
            }
            if (corrections.isEmpty() && clickedSlot >= 0 && clickedSlot < totalSlots) {
                corrections.add(clickedSlot);
            }

            boolean sent;
            if (forceFull || corrections.size() > MAX_TARGETED_CORRECTIONS) {
                sent = sendFullLocked(session);
                if (sent && correctOffhand) {
                    sent = sendOffhandLocked(session);
                }
            } else {
                sent = sendCorrectionsLocked(session, corrections, correctOffhand);
            }
            if (!sent) {
                failCurrentSessionLocked(session);
            }
        }
    }

    private boolean sendCorrectionsLocked(ViewSession session, Collection<Integer> slots,
                                          boolean correctOffhand) {
        ItemStack[] playerInventory = null;
        try {
            for (int slot : slots) {
                ItemStack stack;
                if (slot < session.size()) {
                    ViewItem item = session.contents().get(slot);
                    stack = item != null ? item.item() : ItemStack.EMPTY;
                } else {
                    if (playerInventory == null) {
                        playerInventory = inventoryTracker.mainAndHotbar(session.playerId());
                    }
                    stack = playerInventory[slot - session.size()];
                }
                session.user().sendPacketSilently(new WrapperPlayServerSetSlot(
                        session.windowId(), session.nextStateId(), slot, stack));
            }
            // Window/slot -1 is the carried cursor stack on all supported versions.
            session.user().sendPacketSilently(
                    new WrapperPlayServerSetSlot(-1, session.stateId(), -1, ItemStack.EMPTY));
            return !correctOffhand || sendOffhandLocked(session);
        } catch (Throwable t) {
            logger.warn("[VGui] Failed to correct cancelled click for {}", session.playerId(), t);
            return false;
        }
    }

    private boolean sendOffhandLocked(ViewSession session) {
        try {
            session.user().sendPacketSilently(new WrapperPlayServerSetSlot(
                    -2, session.stateId(), 45, inventoryTracker.windowSlot(session.playerId(), 45)));
            return true;
        } catch (Throwable t) {
            logger.warn("[VGui] Failed to correct offhand slot for {}", session.playerId(), t);
            return false;
        }
    }

    void updateTitle(ViewSession session, Component title) {
        synchronized (lockFor(session.playerId())) {
            if (!isCurrentLocked(session)) {
                return;
            }
            try {
                WindowTypeResolver.openWindow(session.user(), session.windowId(), session.type(), title);
                if (!sendFullLocked(session)) {
                    failCurrentSessionLocked(session);
                    return;
                }
                session.title(title);
            } catch (Throwable t) {
                logger.warn("[VGui] Failed to update window title for {}", session.playerId(), t);
                failCurrentSessionLocked(session);
            }
        }
    }

    void playSound(ViewSession session, Sound sound, float volume, float pitch) {
        if (sound == null) {
            return;
        }
        runIfCurrent(session, () -> {
            try {
                session.user().sendPacketSilently(new WrapperPlayServerEntitySoundEffect(
                        sound, SoundCategory.MASTER, session.user().getEntityId(), volume, pitch));
            } catch (Throwable t) {
                logger.warn("[VGui] Failed to play sound for {}", session.playerId(), t);
            }
        });
    }

    /* ------------------------------------------------------------------ misc */

    UpdateTask schedule(ViewSession session, Duration interval, Consumer<ViewContents> task) {
        synchronized (lockFor(session.playerId())) {
            if (!isCurrentLocked(session)) {
                return CancelledUpdateTask.INSTANCE;
            }
            return updateScheduler.schedule(session, interval,
                    contents -> runIfCurrent(session, () -> task.accept(contents)));
        }
    }

    ClickContextImpl createClickContext(ViewSession session, int slot, me.agent.vgui.api.click.ClickType clickType,
                                        int hotbarKey, int rawButton, int rawMode, int stateId) {
        return new ClickContextImpl(this, session, slot, clickType, hotbarKey, rawButton, rawMode, stateId);
    }

    /** Dispatches global click gates. Listener failures veto the click. */
    boolean allowClick(ViewSession session, ClickContextImpl context) {
        synchronized (lockFor(session.playerId())) {
            boolean allowed = true;
            for (VGuiListener listener : listeners) {
                if (!isCurrentLocked(session)) {
                    return false;
                }
                try {
                    if (!listener.onClick(context)) {
                        allowed = false;
                    }
                } catch (Throwable t) {
                    allowed = false;
                    logger.warn("[VGui] Exception in VGuiListener.onClick; click denied", t);
                }
            }
            return allowed;
        }
    }

    /** Runs a packet/task callback only while its exact session remains current. */
    boolean runIfCurrent(ViewSession session, Runnable callback) {
        synchronized (lockFor(session.playerId())) {
            if (!isCurrentLocked(session)) {
                return false;
            }
            callback.run();
            return true;
        }
    }

    /** Looks up and dispatches the current session in one serialized operation. */
    boolean runWithCurrent(UUID uuid, Consumer<ViewSession> callback) {
        synchronized (lockFor(uuid)) {
            ViewSession session = sessions.get(uuid);
            if (session == null || !session.isOpen()) {
                return false;
            }
            callback.accept(session);
            return true;
        }
    }

    boolean isCurrent(ViewSession session) {
        return session != null && sessions.get(session.playerId()) == session && session.isOpen();
    }

    public ViewSession getSession(UUID uuid) {
        synchronized (lockFor(uuid)) {
            ViewSession session = sessions.get(uuid);
            return session != null && session.isOpen() ? session : null;
        }
    }

    public void addListener(VGuiListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(VGuiListener listener) {
        listeners.remove(listener);
    }

    Logger logger() {
        return logger;
    }

    private boolean isCurrentLocked(ViewSession session) {
        return sessions.get(session.playerId()) == session && session.isOpen();
    }

    private boolean canOpen(Player player, UUID uuid) {
        return !shutdown && openBlockers.get() == 0 && !blockedPlayers.containsKey(uuid)
                && !inventoryTracker.isSwitching(uuid) && player.isActive();
    }

    private User findUser(Player player) {
        try {
            return userProvider.apply(player);
        } catch (Throwable t) {
            logger.warn("[VGui] Failed to resolve PacketEvents user for {}", player.getUniqueId(), t);
            return null;
        }
    }

    private Object lockFor(UUID uuid) {
        int index = (uuid.hashCode() & Integer.MAX_VALUE) % playerLocks.length;
        return playerLocks[index];
    }

    private static void increment(Map<UUID, Integer> counts, UUID uuid) {
        counts.merge(uuid, 1, Integer::sum);
    }

    private static void decrement(Map<UUID, Integer> counts, UUID uuid) {
        counts.computeIfPresent(uuid, (ignored, count) -> count == 1 ? null : count - 1);
    }

    private enum CancelledUpdateTask implements UpdateTask {
        INSTANCE;

        @Override
        public void cancel() {
            // already cancelled
        }

        @Override
        public boolean isCancelled() {
            return true;
        }
    }
}

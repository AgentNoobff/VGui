package me.agent.vgui.core.internal;

import com.github.retrooper.packetevents.protocol.player.User;
import com.velocitypowered.api.proxy.Player;
import me.agent.vgui.api.View;
import me.agent.vgui.api.ViewType;
import me.agent.vgui.api.context.ViewContext;
import me.agent.vgui.api.contents.UpdateTask;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * One player's active view: window id, live contents, state id counter, cooldown
 * state and the update tasks tied to the view's lifetime.
 */
public final class ViewSession {
    private final Player player;
    private final User user;
    private final int windowId;
    private final View view;
    private final ViewType type;
    private final ViewContext context;
    private volatile Component title;
    private final AtomicInteger stateId = new AtomicInteger();
    private final AtomicLong lastClickAt = new AtomicLong();
    private final List<UpdateTask> tasks = new CopyOnWriteArrayList<>();
    private volatile DefaultViewContents contents;
    private final AtomicBoolean open = new AtomicBoolean(true);

    ViewSession(Player player, User user, int windowId, View view, ViewType type,
                ViewContext context, Component title) {
        this.player = player;
        this.user = user;
        this.windowId = windowId;
        this.view = view;
        this.type = type;
        this.context = context;
        this.title = title;
    }

    void attachContents(DefaultViewContents contents) {
        this.contents = contents;
    }

    public Player player() {
        return player;
    }

    public User user() {
        return user;
    }

    public UUID playerId() {
        return player.getUniqueId();
    }

    public int windowId() {
        return windowId;
    }

    public View view() {
        return view;
    }

    public ViewType type() {
        return type;
    }

    public int size() {
        return type.slots();
    }

    public ViewContext context() {
        return context;
    }

    Component title() {
        return title;
    }

    void title(Component title) {
        this.title = title;
    }

    public DefaultViewContents contents() {
        return contents;
    }

    public boolean isOpen() {
        return open.get();
    }

    /** Marks the session closed; further contents mutations become no-ops. */
    boolean markClosed() {
        return open.compareAndSet(true, false);
    }

    /** Next window state id for a server push. */
    int nextStateId() {
        return stateId.incrementAndGet();
    }

    int stateId() {
        return stateId.get();
    }

    /** Whether a client state id describes the last state sent for this window. */
    boolean acceptsStateId(int clientStateId) {
        return clientStateId < 0 || clientStateId == stateId.get();
    }

    /**
     * Records a click attempt; returns {@code false} when it arrives within the
     * view's click cooldown and should be dropped.
     */
    boolean tryClick(long cooldownMillis) {
        if (cooldownMillis <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        long last = lastClickAt.get();
        if (now - last < cooldownMillis) {
            return false;
        }
        return lastClickAt.compareAndSet(last, now);
    }

    void addTask(UpdateTask task) {
        if (!isOpen()) {
            task.cancel();
            return;
        }
        tasks.add(task);
        if (!isOpen() && tasks.remove(task)) {
            task.cancel();
        }
    }

    void cancelTasks() {
        for (UpdateTask task : tasks) {
            try {
                task.cancel();
            } catch (Throwable ignored) {
                // best effort
            }
        }
        tasks.clear();
    }
}

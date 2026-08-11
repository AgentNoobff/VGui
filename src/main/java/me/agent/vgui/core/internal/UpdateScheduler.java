package me.agent.vgui.core.internal;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import me.agent.vgui.api.contents.UpdateTask;
import me.agent.vgui.api.contents.ViewContents;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Runs repeating view-update tasks on the Velocity scheduler, tied to a session's
 * lifetime.
 */
public final class UpdateScheduler {
    private static final Duration MIN_INTERVAL = Duration.ofMillis(50);

    private final ProxyServer server;
    private final Object pluginInstance;
    private final Logger logger;

    public UpdateScheduler(ProxyServer server, Object pluginInstance, Logger logger) {
        this.server = server;
        this.pluginInstance = pluginInstance;
        this.logger = logger;
    }

    UpdateTask schedule(ViewSession session, Duration interval, Consumer<ViewContents> task) {
        Duration period = interval.compareTo(MIN_INTERVAL) < 0 ? MIN_INTERVAL : interval;
        Handle handle = new Handle();
        ScheduledTask scheduled = server.getScheduler()
                .buildTask(pluginInstance, () -> {
                    if (handle.isCancelled()) {
                        return;
                    }
                    if (!session.isOpen()) {
                        handle.cancel();
                        return;
                    }
                    try {
                        task.accept(session.contents());
                    } catch (Throwable t) {
                        logger.warn("[VGui] Exception in scheduled view update for {}", session.playerId(), t);
                    }
                })
                .delay(period)
                .repeat(period)
                .schedule();
        handle.attach(scheduled);
        session.addTask(handle);
        return handle;
    }

    private static final class Handle implements UpdateTask {
        private volatile ScheduledTask task;
        private volatile boolean cancelled;

        void attach(ScheduledTask task) {
            this.task = task;
            if (cancelled) {
                task.cancel();
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            ScheduledTask current = task;
            if (current != null) {
                current.cancel();
            }
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }
}

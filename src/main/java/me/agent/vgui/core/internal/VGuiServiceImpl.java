package me.agent.vgui.core.internal;

import com.velocitypowered.api.proxy.Player;
import me.agent.vgui.api.CloseReason;
import me.agent.vgui.api.VGuiService;
import me.agent.vgui.api.View;
import me.agent.vgui.api.contents.ViewContents;
import me.agent.vgui.api.context.ViewContext;
import me.agent.vgui.api.event.VGuiListener;
import me.agent.vgui.api.input.AnvilInputView;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class VGuiServiceImpl implements VGuiService {
    private final SessionManager sessionManager;
    private final Logger logger;

    public VGuiServiceImpl(SessionManager sessionManager, Logger logger) {
        this.sessionManager = sessionManager;
        this.logger = logger;
    }

    @Override
    public void open(Player player, View view) {
        sessionManager.open(player, view, null, true);
    }

    @Override
    public void open(Player player, View view, ViewContext context) {
        sessionManager.open(player, view, context, true);
    }

    @Override
    public void open(Player player, View view, Consumer<ViewContext> contextBuilder) {
        ViewContext context = new DefaultViewContext();
        if (contextBuilder != null) {
            contextBuilder.accept(context);
        }
        sessionManager.open(player, view, context, true);
    }

    @Override
    public void openReplacing(Player player, View view) {
        sessionManager.open(player, view, null, false);
    }

    @Override
    public void openReplacing(Player player, View view, ViewContext context) {
        sessionManager.open(player, view, context, false);
    }

    @Override
    public boolean back(Player player) {
        return sessionManager.back(player);
    }

    @Override
    public void close(Player player) {
        sessionManager.close(player);
    }

    @Override
    public void closeAll() {
        sessionManager.closeAll(CloseReason.SERVER);
    }

    @Override
    public View currentView(Player player) {
        ViewSession session = session(player);
        return session != null ? session.view() : null;
    }

    @Override
    public ViewContext currentContext(Player player) {
        ViewSession session = session(player);
        return session != null ? session.context() : null;
    }

    @Override
    public ViewContents currentContents(Player player) {
        ViewSession session = session(player);
        return session != null ? session.contents() : null;
    }

    @Override
    public boolean isViewing(Player player) {
        return session(player) != null;
    }

    @Override
    public CompletableFuture<String> prompt(Player player, Component title) {
        return prompt(player, title, "");
    }

    @Override
    public CompletableFuture<String> prompt(Player player, Component title, String initialText) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(initialText, "initialText");
        CompletableFuture<String> future = new CompletableFuture<>();
        AnvilInputView view = AnvilInputView.builder()
                .title(title)
                .initialText(initialText)
                .onConfirm((p, text) -> future.complete(text))
                .onCancel(p -> future.complete(null))
                .build();
        if (!sessionManager.tryOpen(player, view, null, true)) {
            future.complete(null);
            logger.warn("[VGui] Anvil prompt could not be opened for {}; completed as cancelled.",
                    player.getUniqueId());
        }
        return future;
    }

    @Override
    public void addListener(VGuiListener listener) {
        sessionManager.addListener(listener);
    }

    @Override
    public void removeListener(VGuiListener listener) {
        sessionManager.removeListener(listener);
    }

    private ViewSession session(Player player) {
        return player != null ? sessionManager.getSession(player.getUniqueId()) : null;
    }
}

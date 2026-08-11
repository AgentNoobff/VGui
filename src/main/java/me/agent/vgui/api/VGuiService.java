package me.agent.vgui.api;

import com.velocitypowered.api.proxy.Player;
import me.agent.vgui.api.contents.ViewContents;
import me.agent.vgui.api.context.ViewContext;
import me.agent.vgui.api.event.VGuiListener;
import net.kyori.adventure.text.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * The VGui service: open/close views, query sessions, register global listeners.
 * Obtain via {@link VGui#get()}.
 */
public interface VGuiService {

    /* ------------------------------------------------------------------ open/close */

    /**
     * Opens a view. If another proxy view is open it is pushed onto the player's
     * back-history (see {@link #back}).
     */
    void open(Player player, View view);

    /** Opens a view with a pre-populated context. */
    void open(Player player, View view, ViewContext context);

    /** Opens a view, configuring a fresh context first. */
    void open(Player player, View view, Consumer<ViewContext> contextBuilder);

    /** Opens a view without pushing the currently open view onto the back-history. */
    void openReplacing(Player player, View view);

    /** Opens a view without pushing history, with a pre-populated context. */
    void openReplacing(Player player, View view, ViewContext context);

    /**
     * Returns to the player's previous view, if any.
     *
     * @return {@code true} if a previous view was reopened
     */
    boolean back(Player player);

    /** Closes the player's current view (fires {@link CloseReason#SERVER}). */
    void close(Player player);

    /** Closes every open view on the proxy. */
    void closeAll();

    /* ------------------------------------------------------------------ queries */

    /** The player's open view, or {@code null}. */
    View currentView(Player player);

    /** The player's current view context, or {@code null}. */
    ViewContext currentContext(Player player);

    /** The live contents of the player's open view, or {@code null}. */
    ViewContents currentContents(Player player);

    /** Whether the player has a proxy view open. */
    boolean isViewing(Player player);

    /* ------------------------------------------------------------------ input */

    /**
     * Opens an anvil text prompt. The future completes with the entered text when the
     * player confirms, or with {@code null} if the prompt closes, is replaced, or
     * cannot be opened.
     */
    CompletableFuture<String> prompt(Player player, Component title);

    /** Opens an anvil text prompt with pre-filled initial text. */
    CompletableFuture<String> prompt(Player player, Component title, String initialText);

    /* ------------------------------------------------------------------ listeners */

    /** Registers a global listener observing all views. */
    void addListener(VGuiListener listener);

    /** Removes a previously registered global listener. */
    void removeListener(VGuiListener listener);
}

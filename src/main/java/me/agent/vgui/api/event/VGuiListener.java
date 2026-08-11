package me.agent.vgui.api.event;

import com.velocitypowered.api.proxy.Player;
import me.agent.vgui.api.CloseReason;
import me.agent.vgui.api.View;
import me.agent.vgui.api.click.ClickContext;
import me.agent.vgui.api.contents.ViewContents;

/**
 * Global hook for every view on the proxy. Use it for metrics, logging, permission
 * gates, or menu sounds shared across plugins. Register it through
 * {@link me.agent.vgui.api.VGuiService#addListener}.
 *
 * <p>Callbacks run on the player's netty thread; keep them fast.</p>
 */
public interface VGuiListener {

    /** Called after a view opened for a player. */
    default void onOpen(Player player, View view, ViewContents contents) {
        // no-op
    }

    /**
     * Called before a click is dispatched to item and view handlers.
     *
     * @return {@code false} to veto the click (handlers are skipped; the window is
     *         still resynced)
     */
    default boolean onClick(ClickContext click) {
        return true;
    }

    /** Called after a view closed for a player. */
    default void onClose(Player player, View view, CloseReason reason) {
        // no-op
    }
}

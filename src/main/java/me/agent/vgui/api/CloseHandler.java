package me.agent.vgui.api;

import com.velocitypowered.api.proxy.Player;
import me.agent.vgui.api.context.ViewContext;

/**
 * Callback invoked when a view closes; see {@link ViewBuilder#onClose(CloseHandler)}.
 */
@FunctionalInterface
public interface CloseHandler {

    /**
     * Handles the close.
     *
     * @param player  the player the view was open for
     * @param reason  why it closed
     * @param context the per-open context
     */
    void onClose(Player player, CloseReason reason, ViewContext context);
}

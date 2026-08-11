package me.agent.vgui.api;

import me.agent.vgui.api.click.ClickContext;
import me.agent.vgui.api.contents.ViewContents;
import me.agent.vgui.api.context.ViewContext;
import net.kyori.adventure.text.Component;

/**
 * A server-controlled inventory screen rendered entirely at the proxy.
 *
 * <p>Implementations should be stateless and reusable across players: per-player state
 * lives in the {@link ViewContents} passed to {@link #onOpen} and the
 * {@link ViewContext} attached to it. Prefer building views with
 * {@link VGui#chest(int)} / {@link ViewBuilder} unless you need a class.</p>
 */
public interface View {

    /** Title shown on the container. */
    Component title();

    /** The window type (determines slot count and shape). */
    ViewType type();

    /**
     * Called right after the window opens (and again if the view is re-opened via
     * back-navigation). Populate {@code contents} here; mutations made inside this
     * callback are sent as one batch.
     */
    default void onOpen(ViewContents contents) {
        // no-op
    }

    /**
     * Called for every click inside the window (GUI half and player-inventory half).
     * Item-level handlers registered via
     * {@link me.agent.vgui.api.item.ViewItem#clickable} run before this.
     */
    default void onClick(ClickContext click) {
        // no-op
    }

    /**
     * Called exactly once when the view stops being shown, with the reason and the
     * per-open context.
     */
    default void onClose(com.velocitypowered.api.proxy.Player player, CloseReason reason, ViewContext context) {
        // no-op
    }

    /**
     * Called when the player types in an anvil rename field (only for
     * {@link ViewType#ANVIL} views), once per keystroke with the full current text.
     */
    default void onAnvilInput(String text, ViewContents contents) {
        // no-op
    }

    /**
     * Whether client click transactions are cancelled so items can never be taken out
     * of (or put into) the GUI. Default {@code true}; return {@code false} only if you
     * know what you are doing. The proxy cannot apply inventory changes for you.
     */
    default boolean cancelClientTransactions() {
        return true;
    }

    /**
     * Minimum milliseconds between processed non-drag clicks per player. Clicks
     * arriving faster are cancelled and corrected without invoking handlers. Drag
     * phases are never throttled so a configured cooldown cannot break quick-craft
     * sequences. Return {@code 0} to disable (the default).
     */
    default long clickCooldownMillis() {
        return 0;
    }
}

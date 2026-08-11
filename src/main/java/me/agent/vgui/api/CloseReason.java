package me.agent.vgui.api;

/**
 * Why a {@link View} was closed.
 */
public enum CloseReason {
    /** The player closed the window themselves (e.g. pressed Esc). */
    CLIENT,
    /** A plugin closed the view via {@link VGuiService#close}. */
    SERVER,
    /** The view was replaced by another proxy view opening for the same player. */
    SWITCHED,
    /** A backend server opened or closed a window, taking over the player's screen. */
    OVERRIDDEN,
    /** The player disconnected from the proxy. */
    DISCONNECT,
    /** The proxy is shutting down. */
    SHUTDOWN
}

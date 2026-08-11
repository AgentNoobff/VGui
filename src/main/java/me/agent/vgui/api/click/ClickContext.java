package me.agent.vgui.api.click;

import com.github.retrooper.packetevents.protocol.sound.Sound;
import com.velocitypowered.api.proxy.Player;
import me.agent.vgui.api.View;
import me.agent.vgui.api.contents.Slot;
import me.agent.vgui.api.contents.ViewContents;
import me.agent.vgui.api.context.ViewContext;

/**
 * Everything about one inventory click inside an open view, plus convenient actions
 * to react to it (mutate contents, navigate, play sounds).
 *
 * <p>Handlers run on the player's Netty thread. Keep them fast and do not block. For
 * expensive work, use the Velocity scheduler and mutate {@link #contents()} when it
 * completes.</p>
 */
public interface ClickContext {

    /* ------------------------------------------------------------------ info */

    /** The player who clicked. */
    Player player();

    /** The clicked slot, or {@code -999} for clicks outside the window. */
    int slot();

    /** The decoded click type. */
    ClickType clickType();

    /**
     * The pressed hotbar key in [0, 8] when {@link #clickType()} is
     * {@link ClickType#NUMBER_KEY}, otherwise {@code -1}.
     */
    int hotbarKey();

    /** The row/column of the clicked slot, or {@code null} for outside/player-inventory clicks. */
    Slot slotPos();

    /** True when the click landed in the player-inventory half of the window. */
    boolean isPlayerInventory();

    /** True when the click landed in the top (GUI) half of the window. */
    default boolean isGui() {
        return !isPlayerInventory() && slot() >= 0;
    }

    /** Shortcut for {@code clickType().isShift()}. */
    default boolean isShiftClick() {
        return clickType().isShift();
    }

    /** The view being clicked. */
    View view();

    /** The live contents of the open view. Handlers may mutate it directly. */
    ViewContents contents();

    /** The per-open key/value context. */
    ViewContext context();

    /** The raw button id from the click packet. */
    int rawButton();

    /** The raw click mode ordinal from the click packet (pickup, quick-move, swap, ...). */
    int rawMode();

    /** The client's window state id from the click packet, or {@code -1} if absent. */
    int stateId();

    /* ------------------------------------------------------------------ actions */

    /** Closes the view. */
    void close();

    /** Opens another view, pushing the current one onto the back-history. */
    void open(View view);

    /** Opens another view without pushing history. */
    void openReplacing(View view);

    /** Returns to the previous view, if any. */
    boolean back();

    /** Plays a sound only this player hears (volume 1, pitch 1). */
    void playSound(Sound sound);

    /** Plays a sound only this player hears. */
    void playSound(Sound sound, float volume, float pitch);
}

package me.agent.vgui.api.contents;

import com.velocitypowered.api.proxy.Player;
import me.agent.vgui.api.View;
import me.agent.vgui.api.ViewType;
import me.agent.vgui.api.context.ViewContext;
import me.agent.vgui.api.item.ViewItem;
import net.kyori.adventure.text.Component;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * The live, mutable contents of one player's open view. Created when a view opens and
 * discarded when it closes. Every mutation is pushed to the client immediately as a
 * single-slot update or a full window refresh. Wrap bulk changes in
 * {@link #batch(Consumer)} to send one refresh instead.
 *
 * <p>All methods are safe to call from click handlers and update tasks. If the view has
 * already closed, mutations are silently ignored.</p>
 */
public interface ViewContents {

    /* ------------------------------------------------------------------ info */

    /** The player viewing this GUI. */
    Player player();

    /** The view definition these contents belong to. */
    View view();

    /** The window type. */
    ViewType type();

    /** Number of slots in the top (GUI) inventory. */
    int size();

    /** The per-open key/value context (survives view switches via history). */
    ViewContext context();

    /** Whether this view is still open for the player. */
    boolean isOpen();

    /* ------------------------------------------------------------------ slots */

    /** Sets or replaces the item at a flat slot index. {@code null} clears the slot. */
    void set(int slot, ViewItem item);

    /** Sets or replaces the item at a row/column position. {@code null} clears the slot. */
    void set(int row, int column, ViewItem item);

    /** Sets or replaces the item at a {@link Slot} position. */
    void set(Slot slot, ViewItem item);

    /** The item currently at a flat slot index, or {@code null} if empty. */
    ViewItem get(int slot);

    /** The item currently at a row/column position, or {@code null} if empty. */
    ViewItem get(int row, int column);

    /** Clears one slot. */
    void remove(int slot);

    /* ------------------------------------------------------------------ fills */

    /** Fills every slot with the item. */
    void fill(ViewItem item);

    /** Fills only slots that are currently empty. */
    void fillEmpty(ViewItem item);

    /** Fills one row (zero-based). */
    void fillRow(int row, ViewItem item);

    /** Fills one column (zero-based). */
    void fillColumn(int column, ViewItem item);

    /** Fills the outer border (first/last row and first/last column). */
    void fillBorder(ViewItem item);

    /**
     * Fills the rectangle from (rowFrom, columnFrom) to (rowTo, columnTo), both inclusive.
     */
    void fillRect(int rowFrom, int columnFrom, int rowTo, int columnTo, ViewItem item);

    /** Applies a {@link Layout}: every mapped character is written to its slots. */
    void applyLayout(Layout layout);

    /** Clears every slot. */
    void clear();

    /* ------------------------------------------------------------------ updates */

    /**
     * Runs bulk mutations without sending per-slot packets, then pushes one full
     * refresh at the end.
     */
    void batch(Consumer<ViewContents> mutations);

    /** Re-sends the full window contents to the client. */
    void refresh();

    /** Changes the window title in place (re-opens the same window id client-side). */
    void title(Component title);

    /**
     * Schedules a repeating task on the proxy scheduler, cancelled automatically when
     * this view closes or the player switches views.
     *
     * @param interval time between runs (minimum 50ms)
     * @param task     receives these contents on every run
     * @return a handle to cancel the task early
     */
    UpdateTask schedule(Duration interval, Consumer<ViewContents> task);

    /* ------------------------------------------------------------------ features */

    /** The pagination helper for this view (lazily created, one per open view). */
    Pagination pagination();

    /* ------------------------------------------------------------------ navigation */

    /** Closes this view. */
    void close();

    /** Opens another view, pushing the current one onto the player's back-history. */
    void open(View view);

    /** Opens another view without pushing the current one onto the back-history. */
    void openReplacing(View view);

    /** Returns to the previous view in the player's history, if any. */
    boolean back();
}

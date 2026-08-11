package me.agent.vgui.api.contents;

import me.agent.vgui.api.item.ViewItem;

import java.util.List;

/**
 * Paginates a list of items across a region of slots. Obtain via
 * {@link ViewContents#pagination()}, configure the region and items, then navigate;
 * every change re-renders the region automatically.
 *
 * <pre>{@code
 * contents.pagination()
 *     .slots(layout, 'p')
 *     .items(shopEntries);
 * }</pre>
 */
public interface Pagination {

    /**
     * Sets the unique slots the items flow into, in the given order. Resets to page 0.
     *
     * @throws IllegalArgumentException if a slot is out of range or duplicated
     */
    Pagination slots(int... slots);

    /** Sets the region to all slots marked with {@code character} in {@code layout}. */
    Pagination slots(Layout layout, char character);

    /** Sets (or replaces) the items to paginate. Re-renders and clamps the current page. */
    Pagination items(List<ViewItem> items);

    /** Current zero-based page. */
    int page();

    /** Total number of pages (at least 1, even with no items). */
    int pageCount();

    /** Number of slots per page (the size of the configured region). */
    int itemsPerPage();

    /** True when on the first page. */
    boolean isFirst();

    /** True when on the last page. */
    boolean isLast();

    /** Jumps to a page (clamped to the valid range) and re-renders. */
    Pagination open(int page);

    /** Advances one page if possible. */
    Pagination next();

    /** Goes back one page if possible. */
    Pagination previous();

    /** Jumps to the first page. */
    Pagination first();

    /** Jumps to the last page. */
    Pagination last();

    /** Re-renders the current page into the region (e.g. after external changes). */
    void render();
}

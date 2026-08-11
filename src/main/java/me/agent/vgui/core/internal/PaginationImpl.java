package me.agent.vgui.core.internal;

import me.agent.vgui.api.contents.Layout;
import me.agent.vgui.api.contents.Pagination;
import me.agent.vgui.api.item.ViewItem;

import java.util.List;
import java.util.Objects;

/**
 * Pagination bound to a region of a {@link DefaultViewContents}. All navigation
 * re-renders the region.
 */
final class PaginationImpl implements Pagination {
    private final DefaultViewContents contents;
    private int[] region = new int[0];
    private List<ViewItem> items = List.of();
    private int page;

    PaginationImpl(DefaultViewContents contents) {
        this.contents = contents;
    }

    /* ------------------------------------------------------------------ pure math */

    static int pageCount(int itemCount, int perPage) {
        if (perPage <= 0 || itemCount <= 0) {
            return 1;
        }
        return (itemCount + perPage - 1) / perPage;
    }

    static int clampPage(int page, int pageCount) {
        return Math.max(0, Math.min(pageCount - 1, page));
    }

    /* ------------------------------------------------------------------ config */

    @Override
    public Pagination slots(int... slots) {
        Objects.requireNonNull(slots, "slots");
        boolean[] used = new boolean[contents.size()];
        for (int slot : slots) {
            if (slot < 0 || slot >= contents.size()) {
                throw new IllegalArgumentException("Pagination slot " + slot + " out of bounds [0, " + contents.size() + ")");
            }
            if (used[slot]) {
                throw new IllegalArgumentException("Duplicate pagination slot " + slot);
            }
            used[slot] = true;
        }
        synchronized (this) {
            this.region = slots.clone();
            this.page = 0;
        }
        render();
        return this;
    }

    @Override
    public Pagination slots(Layout layout, char character) {
        Objects.requireNonNull(layout, "layout");
        return slots(layout.slotsOf(character));
    }

    @Override
    public Pagination items(List<ViewItem> items) {
        Objects.requireNonNull(items, "items");
        synchronized (this) {
            this.items = List.copyOf(items);
            this.page = clampPage(page, pageCount());
        }
        render();
        return this;
    }

    /* ------------------------------------------------------------------ queries */

    @Override
    public synchronized int page() {
        return page;
    }

    @Override
    public synchronized int pageCount() {
        return pageCount(items.size(), region.length);
    }

    @Override
    public synchronized int itemsPerPage() {
        return region.length;
    }

    @Override
    public synchronized boolean isFirst() {
        return page == 0;
    }

    @Override
    public synchronized boolean isLast() {
        return page >= pageCount() - 1;
    }

    /* ------------------------------------------------------------------ navigation */

    @Override
    public Pagination open(int page) {
        synchronized (this) {
            this.page = clampPage(page, pageCount());
        }
        render();
        return this;
    }

    @Override
    public Pagination next() {
        return open(page() + 1);
    }

    @Override
    public Pagination previous() {
        return open(page() - 1);
    }

    @Override
    public Pagination first() {
        return open(0);
    }

    @Override
    public Pagination last() {
        return open(pageCount() - 1);
    }

    @Override
    public void render() {
        final int[] renderedRegion;
        final List<ViewItem> renderedItems;
        final int offset;
        synchronized (this) {
            if (region.length == 0) {
                return;
            }
            renderedRegion = region.clone();
            renderedItems = items;
            offset = page * renderedRegion.length;
        }
        contents.batch(c -> {
            for (int i = 0; i < renderedRegion.length; i++) {
                int itemIndex = offset + i;
                c.set(renderedRegion[i], itemIndex < renderedItems.size() ? renderedItems.get(itemIndex) : null);
            }
        });
    }
}

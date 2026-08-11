package me.agent.vgui.core.internal;

import com.velocitypowered.api.proxy.Player;
import me.agent.vgui.api.View;
import me.agent.vgui.api.ViewType;
import me.agent.vgui.api.contents.Layout;
import me.agent.vgui.api.contents.Pagination;
import me.agent.vgui.api.contents.Slot;
import me.agent.vgui.api.contents.UpdateTask;
import me.agent.vgui.api.contents.ViewContents;
import me.agent.vgui.api.context.ViewContext;
import me.agent.vgui.api.item.ViewItem;
import net.kyori.adventure.text.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Live contents backing one {@link ViewSession}. Mutations send minimal packets
 * unless batching, and become no-ops once the session closes.
 */
public final class DefaultViewContents implements ViewContents {
    private final SessionManager manager;
    private final ViewSession session;
    private final ViewItem[] items;
    private final Object lock = new Object();
    private PaginationImpl pagination;
    private boolean batching;

    DefaultViewContents(SessionManager manager, ViewSession session) {
        this.manager = manager;
        this.session = session;
        this.items = new ViewItem[session.size()];
    }

    /* ------------------------------------------------------------------ info */

    @Override
    public Player player() {
        return session.player();
    }

    @Override
    public View view() {
        return session.view();
    }

    @Override
    public ViewType type() {
        return session.type();
    }

    @Override
    public int size() {
        return items.length;
    }

    @Override
    public ViewContext context() {
        return session.context();
    }

    @Override
    public boolean isOpen() {
        return session.isOpen();
    }

    /* ------------------------------------------------------------------ slots */

    @Override
    public void set(int slot, ViewItem item) {
        checkSlot(slot);
        manager.runIfCurrent(session, () -> {
            synchronized (lock) {
                items[slot] = item;
                if (!batching) {
                    manager.sendSlot(session, slot, item);
                }
            }
        });
    }

    @Override
    public void set(int row, int column, ViewItem item) {
        set(toIndex(row, column), item);
    }

    @Override
    public void set(Slot slot, ViewItem item) {
        set(slot.index(type().columns()), item);
    }

    @Override
    public ViewItem get(int slot) {
        checkSlot(slot);
        synchronized (lock) {
            return items[slot];
        }
    }

    @Override
    public ViewItem get(int row, int column) {
        return get(toIndex(row, column));
    }

    @Override
    public void remove(int slot) {
        set(slot, null);
    }

    /* ------------------------------------------------------------------ fills */

    @Override
    public void fill(ViewItem item) {
        batch(contents -> {
            for (int slot = 0; slot < items.length; slot++) {
                items[slot] = item;
            }
        });
    }

    @Override
    public void fillEmpty(ViewItem item) {
        batch(contents -> {
            for (int slot = 0; slot < items.length; slot++) {
                if (items[slot] == null) {
                    items[slot] = item;
                }
            }
        });
    }

    @Override
    public void fillRow(int row, ViewItem item) {
        int columns = type().columns();
        if (row < 0 || row >= type().rows()) {
            throw new IllegalArgumentException("Row " + row + " out of bounds for " + type());
        }
        batch(contents -> {
            for (int column = 0; column < columns; column++) {
                items[row * columns + column] = item;
            }
        });
    }

    @Override
    public void fillColumn(int column, ViewItem item) {
        int columns = type().columns();
        if (column < 0 || column >= columns) {
            throw new IllegalArgumentException("Column " + column + " out of bounds for " + type());
        }
        batch(contents -> {
            for (int row = 0; row < type().rows(); row++) {
                items[row * columns + column] = item;
            }
        });
    }

    @Override
    public void fillBorder(ViewItem item) {
        int rows = type().rows();
        int columns = type().columns();
        batch(contents -> {
            for (int slot = 0; slot < items.length; slot++) {
                int row = slot / columns;
                int column = slot % columns;
                if (row == 0 || row == rows - 1 || column == 0 || column == columns - 1) {
                    items[slot] = item;
                }
            }
        });
    }

    @Override
    public void fillRect(int rowFrom, int columnFrom, int rowTo, int columnTo, ViewItem item) {
        int columns = type().columns();
        int r1 = Math.min(rowFrom, rowTo);
        int r2 = Math.max(rowFrom, rowTo);
        int c1 = Math.min(columnFrom, columnTo);
        int c2 = Math.max(columnFrom, columnTo);
        checkSlot(toIndex(r1, c1));
        checkSlot(toIndex(r2, c2));
        batch(contents -> {
            for (int row = r1; row <= r2; row++) {
                for (int column = c1; column <= c2; column++) {
                    items[row * columns + column] = item;
                }
            }
        });
    }

    @Override
    public void applyLayout(Layout layout) {
        Objects.requireNonNull(layout, "layout");
        if (layout.size() != size() || layout.columns() != type().columns()) {
            throw new IllegalArgumentException(
                    "Layout is " + layout.rows() + "x" + layout.columns() + " but " + type() + " needs "
                            + type().rows() + "x" + type().columns());
        }
        batch(contents -> layout.resolve().forEach((slot, item) -> items[slot] = item));
    }

    @Override
    public void clear() {
        fill(null);
    }

    /* ------------------------------------------------------------------ updates */

    @Override
    public void batch(Consumer<ViewContents> mutations) {
        Objects.requireNonNull(mutations, "mutations");
        manager.runIfCurrent(session, () -> {
            boolean send;
            synchronized (lock) {
                boolean wasBatching = batching;
                batching = true;
                try {
                    mutations.accept(this);
                } finally {
                    batching = wasBatching;
                }
                send = !wasBatching;
            }
            if (send) {
                manager.sendFull(session);
            }
        });
    }

    /** Populates initial contents from the view without sending anything. */
    void populate(View view) {
        synchronized (lock) {
            boolean wasBatching = batching;
            batching = true;
            try {
                view.onOpen(this);
            } finally {
                batching = wasBatching;
            }
        }
    }

    @Override
    public void refresh() {
        if (session.isOpen()) {
            manager.sendFull(session);
        }
    }

    @Override
    public void title(Component title) {
        Objects.requireNonNull(title, "title");
        if (session.isOpen()) {
            manager.updateTitle(session, title);
        }
    }

    @Override
    public UpdateTask schedule(Duration interval, Consumer<ViewContents> task) {
        Objects.requireNonNull(interval, "interval");
        Objects.requireNonNull(task, "task");
        return manager.schedule(session, interval, task);
    }

    /* ------------------------------------------------------------------ features */

    @Override
    public Pagination pagination() {
        synchronized (lock) {
            if (pagination == null) {
                pagination = new PaginationImpl(this);
            }
            return pagination;
        }
    }

    /* ------------------------------------------------------------------ navigation */

    @Override
    public void close() {
        manager.close(session);
    }

    @Override
    public void open(View view) {
        manager.open(session, view, null, true);
    }

    @Override
    public void openReplacing(View view) {
        manager.open(session, view, null, false);
    }

    @Override
    public boolean back() {
        return manager.back(session);
    }

    /* ------------------------------------------------------------------ internal */

    ViewSession session() {
        return session;
    }

    /** Snapshot of the raw item array for packet building. */
    ViewItem[] snapshot() {
        synchronized (lock) {
            return items.clone();
        }
    }

    private int toIndex(int row, int column) {
        int columns = type().columns();
        if (row < 0 || row >= type().rows()) {
            throw new IllegalArgumentException("Row " + row + " out of bounds for " + type());
        }
        if (column < 0 || column >= columns) {
            throw new IllegalArgumentException("Column " + column + " out of bounds for " + type());
        }
        return row * columns + column;
    }

    private void checkSlot(int slot) {
        if (slot < 0 || slot >= items.length) {
            throw new IllegalArgumentException("Slot " + slot + " out of bounds for " + type() + " [0, " + items.length + ")");
        }
    }
}

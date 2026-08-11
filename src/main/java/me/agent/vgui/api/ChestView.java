package me.agent.vgui.api;

import me.agent.vgui.api.contents.ViewContents;
import me.agent.vgui.api.item.ViewItem;
import net.kyori.adventure.text.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Convenience base class for static 9xN chest menus: call {@link #setItem} in your
 * constructor and the items are applied every time the view opens. For dynamic menus
 * override {@link #onOpen(ViewContents)} (call {@code super.onOpen(contents)} to keep
 * the static items) or use {@link VGui#chest(int)}.
 */
public abstract class ChestView implements View {
    private final ViewType type;
    private final Component title;
    private final Map<Integer, ViewItem> items = new LinkedHashMap<>();

    protected ChestView(int rows, Component title) {
        this.type = ViewType.chestRows(rows);
        this.title = Objects.requireNonNull(title, "title");
    }

    /** Number of chest rows (1-6). */
    public final int rows() {
        return type.rows();
    }

    /** Total slot count ({@code rows() * 9}). */
    public final int size() {
        return type.slots();
    }

    @Override
    public final Component title() {
        return title;
    }

    @Override
    public final ViewType type() {
        return type;
    }

    /** Sets or replaces a static item; {@code null} removes it. Ignores out-of-range slots. */
    protected final void setItem(int slot, ViewItem item) {
        if (slot >= 0 && slot < size()) {
            if (item == null) {
                items.remove(slot);
            } else {
                items.put(slot, item);
            }
        }
    }

    /**
     * Sets or replaces a static item at a row/column position.
     *
     * @throws IllegalArgumentException if the coordinate is outside this chest
     */
    protected final void setItem(int row, int column, ViewItem item) {
        if (row < 0 || row >= rows()) {
            throw new IllegalArgumentException("Row " + row + " out of bounds [0, " + rows() + ")");
        }
        if (column < 0 || column >= 9) {
            throw new IllegalArgumentException("Column " + column + " out of bounds [0, 9)");
        }
        setItem(row * 9 + column, item);
    }

    @Override
    public void onOpen(ViewContents contents) {
        items.forEach(contents::set);
    }
}

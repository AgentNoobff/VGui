package me.agent.vgui.api.contents;

import me.agent.vgui.api.item.ViewItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A character-mask layout for a view. Each string is one row; each character is one slot.
 * Map characters to items with {@link #where(char, ViewItem)}, then apply the layout via
 * {@link ViewContents#applyLayout(Layout)} or a
 * {@link me.agent.vgui.api.ViewBuilder#layout(String...) ViewBuilder}.
 *
 * <p>The characters {@code '.'} and {@code ' '} always mean "leave the slot empty" and
 * cannot be mapped. Unmapped characters are also left empty, which makes them useful as
 * named regions for {@link Pagination#slots(Layout, char)}.</p>
 *
 * <pre>{@code
 * Layout layout = Layout.of(
 *         "#########",
 *         "#ppppppp#",
 *         "#ppppppp#",
 *         "#<..c..>#")
 *     .where('#', ViewItem.of(filler))
 *     .where('<', ViewItem.pagePrevious(prevArrow))
 *     .where('>', ViewItem.pageNext(nextArrow))
 *     .where('c', ViewItem.closeButton(barrier));
 * }</pre>
 */
public final class Layout {
    private final List<String> rows;
    private final int columns;
    private final Map<Character, ViewItem> mappings = new HashMap<>();

    private Layout(List<String> rows) {
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Layout needs at least one row");
        }
        int columns = rows.get(0).length();
        if (columns == 0) {
            throw new IllegalArgumentException("Layout rows must not be empty strings");
        }
        for (String row : rows) {
            Objects.requireNonNull(row, "layout row");
            if (row.length() != columns) {
                throw new IllegalArgumentException(
                        "All layout rows must have the same length; first row has " + columns
                                + " but found row of length " + row.length() + ": \"" + row + "\"");
            }
        }
        this.rows = List.copyOf(rows);
        this.columns = columns;
    }

    /** Creates a layout from row strings. All rows must have the same length. */
    public static Layout of(String... rows) {
        return new Layout(List.of(rows));
    }

    /** Creates a layout from row strings. All rows must have the same length. */
    public static Layout of(List<String> rows) {
        return new Layout(new ArrayList<>(rows));
    }

    /**
     * Maps a character to an item. Passing {@code null} removes the mapping.
     *
     * @throws IllegalArgumentException for {@code '.'} or {@code ' '} (always empty)
     */
    public Layout where(char character, ViewItem item) {
        if (character == '.' || character == ' ') {
            throw new IllegalArgumentException("'" + character + "' always means an empty slot and cannot be mapped");
        }
        if (item == null) {
            mappings.remove(character);
        } else {
            mappings.put(character, item);
        }
        return this;
    }

    /** Number of rows in this layout. */
    public int rows() {
        return rows.size();
    }

    /** Number of columns (length of each row string). */
    public int columns() {
        return columns;
    }

    /** Total number of slots covered by this layout. */
    public int size() {
        return rows.size() * columns;
    }

    /** The character at a flat slot index. */
    public char charAt(int slot) {
        if (slot < 0 || slot >= size()) {
            throw new IndexOutOfBoundsException("Slot " + slot + " out of layout bounds [0, " + size() + ")");
        }
        return rows.get(slot / columns).charAt(slot % columns);
    }

    /** All flat slot indexes whose layout character equals {@code character}, in reading order. */
    public int[] slotsOf(char character) {
        List<Integer> result = new ArrayList<>();
        for (int slot = 0; slot < size(); slot++) {
            if (charAt(slot) == character) {
                result.add(slot);
            }
        }
        int[] slots = new int[result.size()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = result.get(i);
        }
        return slots;
    }

    /**
     * Resolves the layout to a map of flat slot index to item, in reading order.
     * Slots whose character is unmapped, {@code '.'} or {@code ' '} are absent.
     */
    public Map<Integer, ViewItem> resolve() {
        Map<Integer, ViewItem> result = new LinkedHashMap<>();
        for (int slot = 0; slot < size(); slot++) {
            ViewItem item = mappings.get(charAt(slot));
            if (item != null) {
                result.put(slot, item);
            }
        }
        return Collections.unmodifiableMap(result);
    }
}

package me.agent.vgui.api.contents;

/**
 * A row/column position inside a view's top inventory. Rows and columns are zero-based;
 * the width of a row depends on the {@link me.agent.vgui.api.ViewType}.
 *
 * @param row    zero-based row
 * @param column zero-based column
 */
public record Slot(int row, int column) {

    public Slot {
        if (row < 0 || column < 0) {
            throw new IllegalArgumentException("Slot row/column must be >= 0, got (" + row + ", " + column + ")");
        }
    }

    /** Creates a slot position. */
    public static Slot of(int row, int column) {
        return new Slot(row, column);
    }

    /** Converts this position to a flat slot index for a view that is {@code columns} wide. */
    public int index(int columns) {
        if (columns <= 0) {
            throw new IllegalArgumentException("columns must be > 0, got " + columns);
        }
        if (column >= columns) {
            throw new IllegalArgumentException("Column " + column + " out of bounds [0, " + columns + ")");
        }
        try {
            return Math.addExact(Math.multiplyExact(row, columns), column);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("Slot index overflows int for (" + row + ", " + column
                    + ") at width " + columns, overflow);
        }
    }

    /** Converts a flat slot index back to a position for a view that is {@code columns} wide. */
    public static Slot fromIndex(int index, int columns) {
        if (index < 0 || columns <= 0) {
            throw new IllegalArgumentException("index must be >= 0 and columns > 0");
        }
        return new Slot(index / columns, index % columns);
    }
}

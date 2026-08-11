package me.agent.vgui.api;

/**
 * The kind of container window a {@link View} renders as.
 *
 * <p>Each type carries its fixed top-inventory slot count and column width used for
 * row/column math in layouts and fill helpers. The player inventory (36 slots) is
 * always appended after the top inventory by the client.</p>
 */
public enum ViewType {
    /** Generic chest with 1 row (9 slots). */
    CHEST_9X1(9, 9),
    /** Generic chest with 2 rows (18 slots). */
    CHEST_9X2(18, 9),
    /** Generic chest with 3 rows (27 slots). */
    CHEST_9X3(27, 9),
    /** Generic chest with 4 rows (36 slots). */
    CHEST_9X4(36, 9),
    /** Generic chest with 5 rows (45 slots). */
    CHEST_9X5(45, 9),
    /** Generic chest with 6 rows (54 slots). */
    CHEST_9X6(54, 9),
    /** Hopper window (5 slots in one row). */
    HOPPER(5, 5),
    /** 3x3 dispenser-style grid (9 slots). */
    DISPENSER(9, 3),
    /**
     * Anvil window (3 slots: two inputs, one output). Supports text input via the
     * rename field. See {@link me.agent.vgui.api.input.AnvilInputView}.
     */
    ANVIL(3, 3);

    private final int slots;
    private final int columns;

    ViewType(int slots, int columns) {
        this.slots = slots;
        this.columns = columns;
    }

    /** Number of slots in the top (GUI) inventory. */
    public int slots() {
        return slots;
    }

    /** Number of columns per row (9 for chests, 5 for hopper, 3 for dispenser/anvil). */
    public int columns() {
        return columns;
    }

    /** Number of rows ({@code slots() / columns()}). */
    public int rows() {
        return slots / columns;
    }

    /** True for any of the generic 9xN chest types. */
    public boolean isChest() {
        return ordinal() <= CHEST_9X6.ordinal();
    }

    /**
     * Chest type for the given row count.
     *
     * @param rows rows in [1, 6]
     * @return the matching CHEST_9xN type
     * @throws IllegalArgumentException if rows is out of range
     */
    public static ViewType chestRows(int rows) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Chest rows must be between 1 and 6, got " + rows);
        }
        return values()[rows - 1];
    }

    /**
     * Smallest chest type that fits {@code size} slots (clamped to [9, 54], rounded up
     * to a full row).
     */
    public static ViewType chestForSize(int size) {
        int rows;
        if (size <= 9) {
            rows = 1;
        } else if (size >= 54) {
            rows = 6;
        } else {
            rows = (size + 8) / 9;
        }
        return chestRows(rows);
    }
}

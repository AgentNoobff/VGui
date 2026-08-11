package me.agent.vgui.api.click;

/**
 * High-level classification of an inventory click, decoded from the raw
 * click mode + button combination the client sends.
 */
public enum ClickType {
    /** Plain left click on a slot. */
    LEFT,
    /** Plain right click on a slot. */
    RIGHT,
    /** Middle click (creative clone). */
    MIDDLE,
    /** Shift + left click. */
    SHIFT_LEFT,
    /** Shift + right click. */
    SHIFT_RIGHT,
    /** Hotbar number key (1-9); see {@link ClickContext#hotbarKey()}. */
    NUMBER_KEY,
    /** F key swaps with the offhand. */
    OFFHAND_SWAP,
    /** Q drops one item. */
    DROP,
    /** Ctrl+Q drops the whole stack. */
    CTRL_DROP,
    /** Left click outside the window (would drop the cursor stack). */
    LEFT_OUTSIDE,
    /** Right click outside the window (would drop one from the cursor stack). */
    RIGHT_OUTSIDE,
    /** Double click collects matching items to the cursor. */
    DOUBLE_CLICK,
    /** Started a click-drag ("paint") operation. */
    DRAG_START,
    /** Added a slot to an ongoing click-drag operation. */
    DRAG_ADD,
    /** Finished a click-drag operation. */
    DRAG_END,
    /** Anything this library does not recognize. */
    UNKNOWN;

    /** True for {@link #SHIFT_LEFT} and {@link #SHIFT_RIGHT}. */
    public boolean isShift() {
        return this == SHIFT_LEFT || this == SHIFT_RIGHT;
    }

    /** True for {@link #LEFT}, {@link #SHIFT_LEFT} and {@link #LEFT_OUTSIDE}. */
    public boolean isLeft() {
        return this == LEFT || this == SHIFT_LEFT || this == LEFT_OUTSIDE;
    }

    /** True for {@link #RIGHT}, {@link #SHIFT_RIGHT} and {@link #RIGHT_OUTSIDE}. */
    public boolean isRight() {
        return this == RIGHT || this == SHIFT_RIGHT || this == RIGHT_OUTSIDE;
    }

    /** True for the three drag phases. */
    public boolean isDrag() {
        return this == DRAG_START || this == DRAG_ADD || this == DRAG_END;
    }

    /** True when the click happened outside the window bounds. */
    public boolean isOutside() {
        return this == LEFT_OUTSIDE || this == RIGHT_OUTSIDE;
    }
}

package me.agent.vgui.core.internal;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow.WindowClickType;
import me.agent.vgui.api.click.ClickType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickDecoderTest {

    @Test
    void pickupClicks() {
        assertEquals(ClickType.LEFT, ClickDecoder.decode(WindowClickType.PICKUP, 0, 10));
        assertEquals(ClickType.RIGHT, ClickDecoder.decode(WindowClickType.PICKUP, 1, 10));
        assertEquals(ClickType.LEFT_OUTSIDE, ClickDecoder.decode(WindowClickType.PICKUP, 0, -999));
        assertEquals(ClickType.RIGHT_OUTSIDE, ClickDecoder.decode(WindowClickType.PICKUP, 1, -999));
    }

    @Test
    void shiftClicks() {
        assertEquals(ClickType.SHIFT_LEFT, ClickDecoder.decode(WindowClickType.QUICK_MOVE, 0, 3));
        assertEquals(ClickType.SHIFT_RIGHT, ClickDecoder.decode(WindowClickType.QUICK_MOVE, 1, 3));
    }

    @Test
    void swaps() {
        assertEquals(ClickType.NUMBER_KEY, ClickDecoder.decode(WindowClickType.SWAP, 0, 3));
        assertEquals(ClickType.NUMBER_KEY, ClickDecoder.decode(WindowClickType.SWAP, 8, 3));
        assertEquals(ClickType.OFFHAND_SWAP, ClickDecoder.decode(WindowClickType.SWAP, 40, 3));
        assertEquals(4, ClickDecoder.hotbarKey(WindowClickType.SWAP, 4));
        assertEquals(-1, ClickDecoder.hotbarKey(WindowClickType.SWAP, 40));
        assertEquals(-1, ClickDecoder.hotbarKey(WindowClickType.PICKUP, 0));
    }

    @Test
    void middleAndDrops() {
        assertEquals(ClickType.MIDDLE, ClickDecoder.decode(WindowClickType.CLONE, 2, 3));
        assertEquals(ClickType.DROP, ClickDecoder.decode(WindowClickType.THROW, 0, 3));
        assertEquals(ClickType.CTRL_DROP, ClickDecoder.decode(WindowClickType.THROW, 1, 3));
    }

    @Test
    void drags() {
        assertEquals(ClickType.DRAG_START, ClickDecoder.decode(WindowClickType.QUICK_CRAFT, 0, -999));
        assertEquals(ClickType.DRAG_START, ClickDecoder.decode(WindowClickType.QUICK_CRAFT, 4, -999));
        assertEquals(ClickType.DRAG_START, ClickDecoder.decode(WindowClickType.QUICK_CRAFT, 8, -999));
        assertEquals(ClickType.DRAG_ADD, ClickDecoder.decode(WindowClickType.QUICK_CRAFT, 1, 5));
        assertEquals(ClickType.DRAG_ADD, ClickDecoder.decode(WindowClickType.QUICK_CRAFT, 5, 5));
        assertEquals(ClickType.DRAG_END, ClickDecoder.decode(WindowClickType.QUICK_CRAFT, 2, -999));
        assertEquals(ClickType.DRAG_END, ClickDecoder.decode(WindowClickType.QUICK_CRAFT, 10, -999));
        assertEquals(ClickType.UNKNOWN, ClickDecoder.decode(WindowClickType.QUICK_CRAFT, 3, -999));
    }

    @Test
    void doubleClick() {
        assertEquals(ClickType.DOUBLE_CLICK, ClickDecoder.decode(WindowClickType.PICKUP_ALL, 0, 3));
    }

    @Test
    void rejectsMalformedButtonsAndSlots() {
        assertEquals(ClickType.UNKNOWN, ClickDecoder.decode(WindowClickType.PICKUP, 7, 3));
        assertEquals(ClickType.UNKNOWN, ClickDecoder.decode(WindowClickType.SWAP, 9, 3));
        assertEquals(ClickType.UNKNOWN, ClickDecoder.decode(WindowClickType.CLONE, 0, 3));
        assertEquals(ClickType.UNKNOWN, ClickDecoder.decode(WindowClickType.PICKUP_ALL, 1, 3));

        assertTrue(ClickDecoder.isValid(WindowClickType.PICKUP, 0, 44, 45));
        assertFalse(ClickDecoder.isValid(WindowClickType.PICKUP, 0, 45, 45));
        assertFalse(ClickDecoder.isValid(WindowClickType.PICKUP, 0, -1, 45));
        assertFalse(ClickDecoder.isValid(WindowClickType.QUICK_CRAFT, 0, 4, 45));
        assertFalse(ClickDecoder.isValid(WindowClickType.QUICK_CRAFT, 1, -999, 45));
        assertTrue(ClickDecoder.isValid(WindowClickType.QUICK_CRAFT, 1, 4, 45));
    }
}

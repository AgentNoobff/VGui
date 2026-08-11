package me.agent.vgui.api.contents;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SlotTest {

    @Test
    void indexRoundTrip() {
        Slot slot = Slot.of(2, 4);
        assertEquals(22, slot.index(9));
        assertEquals(slot, Slot.fromIndex(22, 9));
    }

    @Test
    void hopperWidthMath() {
        assertEquals(3, Slot.of(0, 3).index(5));
        assertEquals(Slot.of(0, 4), Slot.fromIndex(4, 5));
    }

    @Test
    void rejectsNegativePositions() {
        assertThrows(IllegalArgumentException.class, () -> Slot.of(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> Slot.of(0, -1));
        assertThrows(IllegalArgumentException.class, () -> Slot.fromIndex(-1, 9));
        assertThrows(IllegalArgumentException.class, () -> Slot.fromIndex(0, 0));
        assertThrows(IllegalArgumentException.class, () -> Slot.of(0, 5).index(5));
        assertThrows(IllegalArgumentException.class, () -> Slot.of(0, 0).index(0));
        assertThrows(IllegalArgumentException.class,
                () -> Slot.of(Integer.MAX_VALUE, 0).index(9));
    }
}

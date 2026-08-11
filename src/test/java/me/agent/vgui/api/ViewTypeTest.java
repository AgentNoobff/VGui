package me.agent.vgui.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewTypeTest {

    @Test
    void chestRows() {
        assertEquals(ViewType.CHEST_9X1, ViewType.chestRows(1));
        assertEquals(ViewType.CHEST_9X6, ViewType.chestRows(6));
        assertThrows(IllegalArgumentException.class, () -> ViewType.chestRows(0));
        assertThrows(IllegalArgumentException.class, () -> ViewType.chestRows(7));
    }

    @Test
    void chestForSizeRoundsUp() {
        assertEquals(ViewType.CHEST_9X1, ViewType.chestForSize(1));
        assertEquals(ViewType.CHEST_9X1, ViewType.chestForSize(9));
        assertEquals(ViewType.CHEST_9X2, ViewType.chestForSize(10));
        assertEquals(ViewType.CHEST_9X6, ViewType.chestForSize(54));
        assertEquals(ViewType.CHEST_9X6, ViewType.chestForSize(999));
        assertEquals(ViewType.CHEST_9X6, ViewType.chestForSize(Integer.MAX_VALUE));
        assertEquals(ViewType.CHEST_9X1, ViewType.chestForSize(Integer.MIN_VALUE));
    }

    @Test
    void geometry() {
        assertEquals(6, ViewType.CHEST_9X6.rows());
        assertEquals(9, ViewType.CHEST_9X6.columns());
        assertEquals(54, ViewType.CHEST_9X6.slots());
        assertEquals(1, ViewType.HOPPER.rows());
        assertEquals(5, ViewType.HOPPER.slots());
        assertEquals(3, ViewType.DISPENSER.rows());
        assertEquals(3, ViewType.DISPENSER.columns());
        assertEquals(1, ViewType.ANVIL.rows());
        assertTrue(ViewType.CHEST_9X1.isChest());
        assertFalse(ViewType.HOPPER.isChest());
        assertFalse(ViewType.ANVIL.isChest());
    }
}

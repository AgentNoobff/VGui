package me.agent.vgui.api;

import me.agent.vgui.api.item.ViewItem;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ChestViewTest {
    @Test
    void coordinateOverloadRejectsAliasingPositions() {
        TestChest chest = new TestChest();
        assertThrows(IllegalArgumentException.class, () -> chest.place(0, 9, null));
        assertThrows(IllegalArgumentException.class, () -> chest.place(2, 0, null));
    }

    private static final class TestChest extends ChestView {
        private TestChest() {
            super(2, Component.text("test"));
        }

        private void place(int row, int column, ViewItem item) {
            setItem(row, column, item);
        }
    }
}

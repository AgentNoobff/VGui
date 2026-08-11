package me.agent.vgui.api.contents;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import me.agent.vgui.api.item.ViewItem;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutTest {
    static {
        me.agent.vgui.testutil.TestPacketEvents.ensureInitialized();
    }

    private static final ViewItem ITEM = ViewItem.of(ItemStack.EMPTY);
    private static final ViewItem OTHER = ViewItem.of(ItemStack.EMPTY);

    @Test
    void dimensionsAndCharAt() {
        Layout layout = Layout.of(
                "#########",
                "#a.....b#",
                "#########");
        assertEquals(3, layout.rows());
        assertEquals(9, layout.columns());
        assertEquals(27, layout.size());
        assertEquals('#', layout.charAt(0));
        assertEquals('a', layout.charAt(10));
        assertEquals('b', layout.charAt(16));
        assertEquals('.', layout.charAt(11));
    }

    @Test
    void rejectsMismatchedRowLengths() {
        assertThrows(IllegalArgumentException.class, () -> Layout.of("#########", "####"));
    }

    @Test
    void rejectsEmptyLayouts() {
        assertThrows(IllegalArgumentException.class, Layout::of);
        assertThrows(IllegalArgumentException.class, () -> Layout.of(""));
    }

    @Test
    void rejectsMappingReservedCharacters() {
        Layout layout = Layout.of("...");
        assertThrows(IllegalArgumentException.class, () -> layout.where('.', ITEM));
        assertThrows(IllegalArgumentException.class, () -> layout.where(' ', ITEM));
    }

    @Test
    void slotsOfReturnsReadingOrder() {
        Layout layout = Layout.of(
                "x.x",
                ".x.",
                "x.x");
        assertArrayEquals(new int[]{0, 2, 4, 6, 8}, layout.slotsOf('x'));
        assertArrayEquals(new int[]{}, layout.slotsOf('z'));
    }

    @Test
    void resolveMapsOnlyMappedCharacters() {
        Layout layout = Layout.of(
                "aab",
                "..c")
                .where('a', ITEM)
                .where('b', OTHER);
        Map<Integer, ViewItem> resolved = layout.resolve();
        assertEquals(3, resolved.size());
        assertSame(ITEM, resolved.get(0));
        assertSame(ITEM, resolved.get(1));
        assertSame(OTHER, resolved.get(2));
        // 'c' is unmapped, '.' is reserved: both absent
        assertTrue(!resolved.containsKey(5) && !resolved.containsKey(3));
    }

    @Test
    void charAtBoundsChecked() {
        Layout layout = Layout.of("ab");
        assertThrows(IndexOutOfBoundsException.class, () -> layout.charAt(2));
        assertThrows(IndexOutOfBoundsException.class, () -> layout.charAt(-1));
    }
}

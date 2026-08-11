package me.agent.vgui.api;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import me.agent.vgui.api.item.ViewItem;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ViewBuilderTest {
    static {
        me.agent.vgui.testutil.TestPacketEvents.ensureInitialized();
    }

    private static final ViewItem ITEM = ViewItem.of(ItemStack.EMPTY);

    @Test
    void buildsViewWithConfiguredProperties() {
        Component title = Component.text("Shop");
        View view = VGui.chest(3)
                .title(title)
                .cancelClientTransactions(false)
                .clickCooldown(Duration.ofMillis(200))
                .build();
        assertEquals(title, view.title());
        assertEquals(ViewType.CHEST_9X3, view.type());
        assertFalse(view.cancelClientTransactions());
        assertEquals(200, view.clickCooldownMillis());
    }

    @Test
    void defaultsAreSane() {
        View view = VGui.view(ViewType.HOPPER).build();
        assertEquals(ViewType.HOPPER, view.type());
        assertEquals(0, view.clickCooldownMillis());
    }

    @Test
    void rejectsLayoutDimensionMismatch() {
        ViewBuilder builder = VGui.chest(3).layout("###", "###");
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void rejectsMappingsWithoutLayout() {
        ViewBuilder builder = VGui.chest(1).map('#', ITEM);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void rejectsOutOfBoundsItems() {
        assertThrows(IllegalArgumentException.class, () -> VGui.chest(1).item(9, ITEM));
        assertThrows(IllegalArgumentException.class, () -> VGui.chest(1).item(-1, ITEM));
        assertThrows(IllegalArgumentException.class, () -> VGui.chest(2).item(0, 9, ITEM));
        assertThrows(IllegalArgumentException.class, () -> VGui.chest(2).item(2, 0, ITEM));
        assertThrows(IllegalArgumentException.class,
                () -> VGui.chest(1).clickCooldown(Duration.ofMillis(-1)));
        assertThrows(IllegalArgumentException.class,
                () -> VGui.chest(1).clickCooldown(Duration.ofNanos(-1)));
    }

    @Test
    void acceptsMatchingLayout() {
        View view = VGui.chest(2)
                .layout("#########",
                        "#.......#")
                .map('#', ITEM)
                .build();
        assertEquals(ViewType.CHEST_9X2, view.type());
    }
}

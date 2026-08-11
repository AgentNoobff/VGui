package me.agent.vgui.core.internal;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import me.agent.vgui.api.item.ItemBuilder;
import me.agent.vgui.testutil.TestPacketEvents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryTrackerTest {
    @BeforeAll
    static void setupPacketEvents() {
        TestPacketEvents.ensureInitialized();
    }

    @Test
    void backendGenerationRejectsOldAndPreBaselineUpdates() {
        InventoryTracker tracker = new InventoryTracker();
        UUID uuid = UUID.randomUUID();
        ItemStack oldItem = ItemBuilder.of(ItemTypes.STONE).build();
        ItemStack newItem = ItemBuilder.of(ItemTypes.DIAMOND).build();

        tracker.windowItems(uuid, inventory(oldItem));
        assertEquals(ItemTypes.STONE, tracker.mainAndHotbar(uuid)[0].getType());
        long oldGeneration = tracker.generation(uuid);

        long newGeneration = tracker.beginBackendSwitch(uuid);
        assertTrue(tracker.isSwitching(uuid));
        tracker.windowItems(uuid, oldGeneration, inventory(oldItem));
        assertSame(ItemStack.EMPTY, tracker.mainAndHotbar(uuid)[0]);

        assertEquals(newGeneration, tracker.backendConnected(uuid));
        tracker.setSlot(uuid, newGeneration, 9, oldItem);
        assertSame(ItemStack.EMPTY, tracker.mainAndHotbar(uuid)[0]);

        tracker.windowItems(uuid, newGeneration, inventory(newItem));
        tracker.setSlot(uuid, oldGeneration, 9, oldItem);
        assertEquals(ItemTypes.DIAMOND, tracker.mainAndHotbar(uuid)[0].getType());
    }

    private static List<ItemStack> inventory(ItemStack mainSlot) {
        List<ItemStack> items = new ArrayList<>(Collections.nCopies(46, ItemStack.EMPTY));
        items.set(9, mainSlot);
        return items;
    }
}

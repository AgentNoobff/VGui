package me.agent.vgui.core.internal;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import me.agent.vgui.api.ViewType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WindowTypeResolverTest {

    @Test
    void chestIdsStableAcrossVersions() {
        for (int rows = 1; rows <= 6; rows++) {
            ViewType type = ViewType.chestRows(rows);
            assertEquals(rows - 1, WindowTypeResolver.modernTypeId(type, ClientVersion.V_1_14));
            assertEquals(rows - 1, WindowTypeResolver.modernTypeId(type, ClientVersion.V_1_21));
        }
    }

    @Test
    void crafterRegistryShiftIn1_20_3() {
        assertEquals(7, WindowTypeResolver.modernTypeId(ViewType.ANVIL, ClientVersion.V_1_20_2));
        assertEquals(8, WindowTypeResolver.modernTypeId(ViewType.ANVIL, ClientVersion.V_1_20_3));
        assertEquals(15, WindowTypeResolver.modernTypeId(ViewType.HOPPER, ClientVersion.V_1_20_2));
        assertEquals(16, WindowTypeResolver.modernTypeId(ViewType.HOPPER, ClientVersion.V_1_20_3));
        assertEquals(6, WindowTypeResolver.modernTypeId(ViewType.DISPENSER, ClientVersion.V_1_20_2));
        assertEquals(6, WindowTypeResolver.modernTypeId(ViewType.DISPENSER, ClientVersion.V_1_20_3));
    }

    @Test
    void legacyTypes() {
        assertEquals("minecraft:container", WindowTypeResolver.legacyType(ViewType.CHEST_9X3));
        assertEquals("minecraft:hopper", WindowTypeResolver.legacyType(ViewType.HOPPER));
        assertEquals("minecraft:dispenser", WindowTypeResolver.legacyType(ViewType.DISPENSER));
        assertEquals("minecraft:anvil", WindowTypeResolver.legacyType(ViewType.ANVIL));
    }

    @Test
    void legacySlotsOnlyForChests() {
        assertEquals(27, WindowTypeResolver.legacySlots(ViewType.CHEST_9X3));
        assertEquals(54, WindowTypeResolver.legacySlots(ViewType.CHEST_9X6));
        assertEquals(0, WindowTypeResolver.legacySlots(ViewType.ANVIL));
        assertEquals(0, WindowTypeResolver.legacySlots(ViewType.HOPPER));
    }
}

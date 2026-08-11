package me.agent.vgui.core.internal;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowIdAllocatorTest {

    @Test
    void staysAboveVanillaRangeAndWraps() {
        WindowIdAllocator allocator = new WindowIdAllocator();
        UUID uuid = UUID.randomUUID();
        int first = allocator.nextId(uuid);
        assertEquals(WindowIdAllocator.MIN_ID, first);
        int previous = first;
        for (int i = 0; i < 100; i++) {
            int id = allocator.nextId(uuid);
            assertTrue(id >= WindowIdAllocator.MIN_ID && id <= WindowIdAllocator.MAX_ID,
                    "id out of range: " + id);
            if (previous == WindowIdAllocator.MAX_ID) {
                assertEquals(WindowIdAllocator.MIN_ID, id);
            } else {
                assertEquals(previous + 1, id);
            }
            previous = id;
        }
    }

    @Test
    void independentPerPlayer() {
        WindowIdAllocator allocator = new WindowIdAllocator();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        allocator.nextId(a);
        allocator.nextId(a);
        assertEquals(WindowIdAllocator.MIN_ID, allocator.nextId(b));
    }

    @Test
    void forgetResets() {
        WindowIdAllocator allocator = new WindowIdAllocator();
        UUID uuid = UUID.randomUUID();
        allocator.nextId(uuid);
        allocator.nextId(uuid);
        allocator.forget(uuid);
        assertEquals(WindowIdAllocator.MIN_ID, allocator.nextId(uuid));
    }
}

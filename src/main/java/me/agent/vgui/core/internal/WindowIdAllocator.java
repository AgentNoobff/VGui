package me.agent.vgui.core.internal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Allocates container window ids per player. Vanilla servers cycle ids 1..99, so we
 * cycle 101..119 to never collide with a window a backend server opens.
 */
public final class WindowIdAllocator {
    static final int MIN_ID = 101;
    static final int MAX_ID = 119;

    private final Map<UUID, AtomicInteger> counters = new ConcurrentHashMap<>();

    public int nextId(UUID uuid) {
        AtomicInteger counter = counters.computeIfAbsent(uuid, u -> new AtomicInteger(MIN_ID));
        return counter.getAndUpdate(id -> id >= MAX_ID ? MIN_ID : id + 1);
    }

    public void forget(UUID uuid) {
        counters.remove(uuid);
    }

    public void clear() {
        counters.clear();
    }
}

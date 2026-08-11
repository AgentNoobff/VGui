package me.agent.vgui.core.internal;

import com.github.retrooper.packetevents.protocol.item.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches each player's backend-owned inventory by observing window-0 packets.
 * Every backend connection has a generation: switching invalidates the old cache,
 * packets captured for an older generation are rejected, and SET_SLOT updates are
 * ignored until the new backend supplies a complete WINDOW_ITEMS baseline.
 *
 * <p>Window 0 layout: 0 = craft result, 1-4 = craft grid, 5-8 = armor, 9-35 = main
 * inventory, 36-44 = hotbar, 45 = offhand.</p>
 */
public final class InventoryTracker {
    private static final int WINDOW_0_SIZE = 46;
    private static final int MAIN_START = 9;
    private static final int MAIN_AND_HOTBAR = 36;

    private final Map<UUID, Cache> inventories = new ConcurrentHashMap<>();

    /** Invalidates the current cache before a backend connection attempt. */
    public long beginBackendSwitch(UUID uuid) {
        Cache cache = cache(uuid);
        synchronized (cache) {
            cache.generation++;
            cache.switching = true;
            clear(cache);
            return cache.generation;
        }
    }

    /** Marks the latest backend generation connected and ready to accept a baseline. */
    public long backendConnected(UUID uuid) {
        Cache cache = cache(uuid);
        synchronized (cache) {
            if (!cache.switching) {
                cache.generation++;
                clear(cache);
            }
            cache.switching = false;
            return cache.generation;
        }
    }

    /** Ends a failed connection attempt without reviving the invalidated old cache. */
    public void backendSwitchAborted(UUID uuid) {
        Cache cache = inventories.get(uuid);
        if (cache == null) {
            return;
        }
        synchronized (cache) {
            cache.switching = false;
        }
    }

    /** Current backend generation, captured by packet listeners before an update. */
    public long generation(UUID uuid) {
        Cache cache = cache(uuid);
        synchronized (cache) {
            return cache.generation;
        }
    }

    /** Whether a backend transition is in progress; proxy views should not open yet. */
    public boolean isSwitching(UUID uuid) {
        Cache cache = inventories.get(uuid);
        if (cache == null) {
            return false;
        }
        synchronized (cache) {
            return cache.switching;
        }
    }

    /** Records a full window-0 contents packet for the current generation. */
    public void windowItems(UUID uuid, List<ItemStack> items) {
        windowItems(uuid, generation(uuid), items);
    }

    /** Records a full window-0 packet only if its captured generation is still live. */
    public void windowItems(UUID uuid, long generation, List<ItemStack> items) {
        Cache cache = cache(uuid);
        synchronized (cache) {
            if (cache.switching || cache.generation != generation) {
                return;
            }
            Arrays.fill(cache.items, ItemStack.EMPTY);
            int count = Math.min(items.size(), WINDOW_0_SIZE);
            for (int i = 0; i < count; i++) {
                cache.items[i] = orEmpty(items.get(i));
            }
            cache.baseline = true;
        }
    }

    /** Records a single window-0 slot update for the current generation. */
    public void setSlot(UUID uuid, int slot, ItemStack item) {
        setSlot(uuid, generation(uuid), slot, item);
    }

    /**
     * Records a slot update only for a live generation with a full baseline. This
     * prevents late old-backend deltas from seeding a newly connected cache.
     */
    public void setSlot(UUID uuid, long generation, int slot, ItemStack item) {
        if (slot < 0 || slot >= WINDOW_0_SIZE) {
            return;
        }
        Cache cache = cache(uuid);
        synchronized (cache) {
            if (cache.switching || !cache.baseline || cache.generation != generation) {
                return;
            }
            cache.items[slot] = orEmpty(item);
        }
    }

    /**
     * The 36 main-inventory + hotbar items in container-window order. An empty
     * snapshot is returned until the current backend provides a complete baseline.
     */
    public ItemStack[] mainAndHotbar(UUID uuid) {
        ItemStack[] result = new ItemStack[MAIN_AND_HOTBAR];
        Arrays.fill(result, ItemStack.EMPTY);
        Cache cache = inventories.get(uuid);
        if (cache == null) {
            return result;
        }
        synchronized (cache) {
            if (!cache.switching && cache.baseline) {
                System.arraycopy(cache.items, MAIN_START, result, 0, MAIN_AND_HOTBAR);
            }
        }
        return result;
    }

    /** One raw window-0 slot, or empty while no current baseline is available. */
    ItemStack windowSlot(UUID uuid, int slot) {
        if (slot < 0 || slot >= WINDOW_0_SIZE) {
            return ItemStack.EMPTY;
        }
        Cache cache = inventories.get(uuid);
        if (cache == null) {
            return ItemStack.EMPTY;
        }
        synchronized (cache) {
            return !cache.switching && cache.baseline ? cache.items[slot] : ItemStack.EMPTY;
        }
    }

    /** Drops all connection and inventory state for a disconnected player. */
    public void evict(UUID uuid) {
        inventories.remove(uuid);
    }

    /** Drops every cached player, used when the owning bootstrap shuts down. */
    public void clear() {
        inventories.clear();
    }

    private Cache cache(UUID uuid) {
        return inventories.computeIfAbsent(uuid, ignored -> new Cache());
    }

    private static void clear(Cache cache) {
        Arrays.fill(cache.items, ItemStack.EMPTY);
        cache.baseline = false;
    }

    private static ItemStack orEmpty(ItemStack item) {
        return item == null ? ItemStack.EMPTY : item;
    }

    private static final class Cache {
        private final ItemStack[] items = new ItemStack[WINDOW_0_SIZE];
        private long generation;
        private boolean switching;
        private boolean baseline;

        private Cache() {
            Arrays.fill(items, ItemStack.EMPTY);
        }
    }
}

package me.agent.vgui.api.context;

import java.util.Map;

/**
 * Context associated with a single player's active view (menu).
 * Supports both typed keys and simple string keys.
 */
public interface ViewContext {
    // Typed keys
    <T> void set(ViewKey<T> key, T value);
    <T> T get(ViewKey<T> key);

    // String keys
    void set(String key, Object value);
    Object get(String key);
    <T> T get(String key, Class<T> type);
    boolean contains(String key);

    /**
     * Read-only snapshot of simple string-backed values.
     * Typed entries are NOT included here.
     */
    Map<String, Object> asUnmodifiableMap();
}
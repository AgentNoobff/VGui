package me.agent.vgui.core.internal;

import me.agent.vgui.api.context.ViewContext;
import me.agent.vgui.api.context.ViewKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class DefaultViewContext implements ViewContext {
    private final Map<ViewKey<?>, Object> typed = new HashMap<>();
    private final Map<String, Object> plain = new HashMap<>();

    @Override
    public synchronized <T> void set(ViewKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        if (value == null) {
            typed.remove(key);
        } else if (!key.type().isInstance(value)) {
            throw new IllegalArgumentException("Value for key " + key + " must be of type " + key.type().getName());
        } else {
            typed.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <T> T get(ViewKey<T> key) {
        Objects.requireNonNull(key, "key");
        Object v = typed.get(key);
        if (v == null) return null;
        if (!key.type().isInstance(v)) {
            // Should not happen due to guard in set
            throw new IllegalStateException("Stored value type mismatch for key " + key);
        }
        return (T) v;
    }

    @Override
    public synchronized void set(String key, Object value) {
        Objects.requireNonNull(key, "key");
        if (value == null) plain.remove(key);
        else plain.put(key, value);
    }

    @Override
    public synchronized Object get(String key) {
        Objects.requireNonNull(key, "key");
        return plain.get(key);
    }

    @Override
    public synchronized <T> T get(String key, Class<T> type) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(type, "type");
        Object v = plain.get(key);
        if (v == null) return null;
        if (!type.isInstance(v)) {
            throw new ClassCastException("Value for key '" + key + "' is not of type " + type.getName());
        }
        return type.cast(v);
    }

    @Override
    public synchronized boolean contains(String key) {
        return plain.containsKey(key);
    }

    @Override
    public synchronized Map<String, Object> asUnmodifiableMap() {
        return Map.copyOf(plain);
    }
}
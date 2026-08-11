package me.agent.vgui.api.context;

import java.util.Objects;

/**
 * Strongly typed key for ViewContext storage.
 */
public final class ViewKey<T> {
    private final String name;
    private final Class<T> type;

    private ViewKey(String name, Class<T> type) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
    }

    public static <T> ViewKey<T> of(String name, Class<T> type) {
        return new ViewKey<>(name, type);
    }

    public String name() {
        return name;
    }

    public Class<T> type() {
        return type;
    }

    @Override
    public String toString() {
        return "ViewKey[" + name + ":" + type.getSimpleName() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ViewKey<?> other)) return false;
        return name.equals(other.name) && type.equals(other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
}
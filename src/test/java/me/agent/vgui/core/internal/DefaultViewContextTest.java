package me.agent.vgui.core.internal;

import me.agent.vgui.api.context.ViewContext;
import me.agent.vgui.api.context.ViewKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultViewContextTest {

    @Test
    void typedKeys() {
        ViewContext context = new DefaultViewContext();
        ViewKey<Integer> page = ViewKey.of("page", Integer.class);
        assertNull(context.get(page));
        context.set(page, 3);
        assertEquals(3, context.get(page));
        context.set(page, null); // null removes
        assertNull(context.get(page));
    }

    @Test
    void typedKeysDistinguishType() {
        ViewContext context = new DefaultViewContext();
        ViewKey<Integer> asInt = ViewKey.of("value", Integer.class);
        ViewKey<String> asString = ViewKey.of("value", String.class);
        context.set(asInt, 1);
        context.set(asString, "one");
        assertEquals(1, context.get(asInt));
        assertEquals("one", context.get(asString));
    }

    @Test
    void plainKeys() {
        ViewContext context = new DefaultViewContext();
        context.set("shop", "weapons");
        assertTrue(context.contains("shop"));
        assertEquals("weapons", context.get("shop"));
        assertEquals("weapons", context.get("shop", String.class));
        assertThrows(ClassCastException.class, () -> context.get("shop", Integer.class));
        context.set("shop", null);
        assertFalse(context.contains("shop"));
    }

    @Test
    void snapshotIsUnmodifiableAndDetached() {
        ViewContext context = new DefaultViewContext();
        context.set("a", 1);
        var snapshot = context.asUnmodifiableMap();
        context.set("b", 2);
        assertEquals(1, snapshot.size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put("c", 3));
    }
}

# Context and State

`ViewContext` stores state for one player's open view. It travels with a view when that view enters back history.

## Typed keys

Create shared static keys:

```java
public static final ViewKey<String> CATEGORY =
    ViewKey.of("shop:category", String.class);

public static final ViewKey<Integer> PAGE_SIZE =
    ViewKey.of("shop:page_size", Integer.class);
```

Read and write them through the context:

```java
contents.context().set(CATEGORY, "blocks");
String category = contents.context().get(CATEGORY);
```

Setting null removes a typed entry. Setting a value that does not match the key class throws `IllegalArgumentException`.

`ViewKey` equality uses both name and type. `ViewKey.of("id", String.class)` is different from `ViewKey.of("id", UUID.class)`.

Use namespaced names to avoid accidental collisions across integrations.

## Plain string keys

```java
context.set("selected", playerId);
Object raw = context.get("selected");
UUID selected = context.get("selected", UUID.class);
boolean present = context.contains("selected");
```

The typed string getter returns null for a missing value and throws `ClassCastException` for a wrong type. Setting null removes the key.

`asUnmodifiableMap()` returns a snapshot of plain string entries only. Typed `ViewKey` entries are intentionally absent.

## Opening with initial context

Use a builder callback:

```java
VGui.open(player, shopView, context -> {
    context.set(CATEGORY, "featured");
    context.set(PAGE_SIZE, 21);
});
```

Or pass a `ViewContext` instance through `VGuiService.open`. The public API does not expose a concrete context constructor, so the callback overload is usually easiest for external plugins.

## Context during callbacks

```java
.onOpen(contents -> {
    String category = contents.context().get(CATEGORY);
    renderCategory(contents, category);
})
.onClick(click -> {
    click.context().set(CATEGORY, nextCategory());
})
.onClose((player, reason, context) -> {
    saveLastCategory(player, context.get(CATEGORY));
})
```

## History behavior

When `open` replaces a current view, VGui stores the view and its context in a history entry. `back()` removes that entry and reopens the view with the same context object. `onOpen` runs again and should render from context.

The previous `ViewContents` does not survive. Never store live contents inside context as a way to restore it.

## State design

Good context values are small identifiers and immutable snapshots:

- selected category id
- page filter
- sort order
- request generation number
- target player UUID
- permission snapshot when explicitly desired

Avoid storing large caches, database connections, plugin instances, network clients, or objects with a lifecycle longer than the view.

## Prevent stale asynchronous updates

```java
public static final ViewKey<Long> REQUEST =
    ViewKey.of("search:request", Long.class);

long request = System.nanoTime();
context.set(REQUEST, request);

loadAsync(query).thenAccept(results -> scheduler.execute(() -> {
    if (!contents.isOpen()) {
        return;
    }
    if (!Long.valueOf(request).equals(context.get(REQUEST))) {
        return;
    }
    contents.batch(c -> render(c, results));
}));
```

This check protects against a slow earlier query replacing a newer result.

## Thread safety

The built-in context implementation synchronizes access. That protects its maps from corruption. It does not make a mutable object stored inside the context thread-safe.

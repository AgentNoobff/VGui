# Contents and Updates

`ViewContents` is the live top inventory for one player and one open view. Mutations are synchronized and become no-ops for packet output after the session closes.

## Session information

```java
Player player = contents.player();
View view = contents.view();
ViewType type = contents.type();
int slots = contents.size();
ViewContext context = contents.context();
boolean active = contents.isOpen();
```

`size()` covers only the proxy-owned top inventory.

## Read and write slots

```java
contents.set(13, item);
contents.set(1, 4, item);
contents.set(Slot.of(1, 4), item);

ViewItem first = contents.get(13);
ViewItem second = contents.get(1, 4);

contents.remove(13);
```

Passing null to `set` clears the slot. Invalid positions throw `IllegalArgumentException`.

A single `set` sends one set-slot packet when the session is open and not batching.

## Fill operations

```java
contents.fill(background);
contents.fillEmpty(background);
contents.fillRow(0, border);
contents.fillColumn(0, border);
contents.fillBorder(border);
contents.fillRect(1, 2, 3, 6, panel);
contents.clear();
```

Fill methods use batching internally, so each operation sends one full refresh.

`fillRect` treats both corners as inclusive and normalizes their order. Bounds are validated after normalization.

## Batching

Use `batch` for several related mutations:

```java
contents.batch(c -> {
    c.clear();
    c.fillBorder(border);
    c.set(13, mainAction);
    c.set(22, closeButton);
});
```

Nested batches are supported. Only the outer batch sends the final full refresh. An exception from the mutation callback restores the previous batching state, then propagates to the caller.

The initial `View.onOpen` population is automatically batched and followed by one full window packet.

## Refresh

`refresh()` resends the complete GUI, tracked player inventory, and empty cursor without changing stored items. It is useful after a low-level item object changed or when an external packet interaction requires a correction.

Normal `set`, fill, pagination, and title operations already send the required packets.

## Live titles

```java
contents.title(Component.text("Page " + pageNumber));
```

The client window is opened again with the same id and type, then the full contents are resent. The live title is retained if this view enters back history. Use title changes sparingly because they cost more than a single slot update.

## Repeating tasks

```java
UpdateTask task = contents.schedule(Duration.ofSeconds(1), c -> {
    int value = readCurrentValue();
    c.set(4, counterItem(value));
});
```

Intervals have an effective minimum of 50 milliseconds. Tasks run on the Velocity scheduler and are cancelled automatically when the session closes or switches.

The returned handle supports:

```java
task.cancel();
boolean stopped = task.isCancelled();
```

Cancellation is idempotent.

## Builder updates

`ViewBuilder.updateEvery` registers a task definition on the immutable view. A separate task starts for each open session:

```java
View clock = VGui.chest(1)
    .updateEvery(Duration.ofSeconds(1), c ->
        c.set(4, clockItem(Instant.now())))
    .build();
```

Use `contents.schedule` when the task should be created conditionally during `onOpen` or in response to a click.

## Navigation shortcuts

Contents can control their own session:

- `close()` closes with `CloseReason.SERVER`.
- `open(view)` pushes the current view and context into history.
- `openReplacing(view)` switches without pushing history.
- `back()` reopens the last history entry and returns whether one existed.

## Asynchronous result example

```java
UUID expectedPlayer = contents.player().getUniqueId();

databaseExecutor.execute(() -> {
    List<Record> records = loadRecords(expectedPlayer);
    proxyServer.getScheduler()
        .buildTask(plugin, () -> {
            if (!contents.isOpen()) {
                return;
            }
            contents.batch(c -> renderRecords(c, records));
        })
        .schedule();
});
```

For changing filters or pages, also capture a request token in `ViewContext` so an old result cannot overwrite a newer request.

## Packet cost model

- One `set` or `remove`: one slot packet.
- A fill or `batch`: one full window packet.
- `refresh`: one full window packet.
- `title`: open-window plus one full window packet.
- A cancelled click: targeted slot/cursor corrections; large or legacy multi-slot changes use one full resync.

Group related mutations, avoid cosmetic updates faster than players can perceive, and do not call `refresh` after methods that already refresh.

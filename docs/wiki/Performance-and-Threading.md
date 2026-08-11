# Performance and Threading

VGui is packet-driven. Correct thread use and update frequency matter more than the number of immutable view definitions.

## Network-thread callbacks

The following run in client packet handling:

- `ViewItem` click handlers
- `View.onClick`
- `View.onAnvilInput`
- `VGuiListener.onClick`
- close handling triggered by client packets

Keep them short. Blocking the player's Netty thread delays packet processing and can cause latency, timeouts, or disconnected clients.

Do not perform:

- database queries;
- HTTP calls;
- file reads or writes;
- process execution;
- long serialization;
- waits on futures or locks controlled by other threads.

## Scheduler callbacks

Repeating updates run through the Velocity scheduler. They should still remain efficient because one slow task can consume scheduler capacity.

Fetch external data on a dedicated executor and render a cached result from the scheduled callback.

## Safe asynchronous pattern

```java
UUID playerId = click.player().getUniqueId();
ViewContents contents = click.contents();

CompletableFuture
    .supplyAsync(() -> repository.load(playerId), databaseExecutor)
    .thenAccept(result -> proxy.getScheduler()
        .buildTask(plugin, () -> {
            if (!contents.isOpen()) {
                return;
            }
            contents.batch(c -> render(c, result));
        })
        .schedule());
```

Add a context request id when more than one load may overlap.

## Packet volume

One slot mutation sends a set-slot packet. Bulk mutations send the full window, including the tracked lower inventory. Cancelled clicks normally correct only client-predicted slots plus the cursor; large or legacy multi-slot operations use a full refresh.

Choose the update form based on the number of changed slots:

- one isolated change: `set`;
- several coordinated changes: `batch`;
- complete redraw: `batch`, a fill, or pagination;
- no data change but forced correction: `refresh`.

Do not call `refresh` immediately after `batch`, fill, pagination, or title updates.

## Update intervals

The scheduler enforces a minimum interval of 50 milliseconds. That does not make 20 updates per second appropriate for ordinary menus.

- clocks and status summaries usually need one-second updates;
- queue positions might update a few times per second;
- animations should use the lowest visually acceptable rate;
- data that changes rarely should update on events rather than polling.

## Memory model

Reusable views and items are small shared objects. Per connected player, VGui may retain:

- an active session;
- top-inventory item array;
- context values;
- up to ten navigation entries;
- task handles;
- a 36-slot player inventory cache;
- a small window id and state counter.

Memory grows primarily with connected players and retained context data. Do not place large result sets in context or history.

## Content synchronization

The default contents and context implementations synchronize their internal state. This prevents collection corruption across callbacks and scheduler tasks.

It does not make external objects safe. If context contains a mutable list or custom object, its own synchronization rules still apply.

## Handler allocation

Prefer reusable `ViewItem` instances for static buttons and fillers. Avoid rebuilding identical lore-heavy items every tick. Rebuild only values that actually changed.

## Measurement

VGui does not publish universal benchmark numbers. Packet cost depends on client protocol, menu size, update rate, network compression, player count, PacketEvents, and proxy hardware.

Measure with the intended player count and menu behavior. Monitor event-loop delay, scheduler backlog, packet throughput, allocation rate, and garbage collection rather than only average CPU.

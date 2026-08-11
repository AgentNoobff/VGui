# Architecture

VGui has a public definition layer, a platform bootstrap, session state, packet bridges, and small protocol utilities.

## Bootstrap

`VGuiPlatform` is the standalone Velocity entry point. `VGuiBootstrap` is shared by standalone and shaded deployments.

Bootstrap responsibilities:

- create or reuse PacketEvents;
- initialize PacketEvents if necessary;
- create the session manager, inventory tracker, scheduler, and window-id allocator;
- register client and server packet listeners;
- register disconnect and backend-connection cleanup;
- install the `VGuiService` singleton.

Shutdown unregisters VGui's exact PacketEvents and Velocity listener instances before a shared PacketEvents API can be reused or VGui can be initialized again.

## Public API

The `me.agent.vgui.api` packages contain reusable definitions and session-facing interfaces. They do not expose packet wrapper details to normal menu code.

The supported integration boundary is the API package plus `VGuiBootstrap` for shaded integrations. The `core.internal` package is not a compatibility contract.

## SessionManager

`SessionManager` owns active sessions by player UUID and navigation histories by player UUID.

Opening a view:

1. validate the replacement and create its context, session, and contents;
2. install the replacement atomically for the player;
3. optionally push the previous view, context, and current live title to history;
4. retire the previous session and run its close callbacks;
5. stop if a reentrant callback performed a newer transition;
6. send the open-window packet and populate initial contents;
7. send a complete window packet;
8. notify global listeners.

Closing marks the session inactive, cancels update tasks, optionally sends a close packet, clears history when appropriate, and fires close callbacks.

## ViewSession

A session holds player and PacketEvents user references, window id, view, type, context, contents, state id, click timing, open state, and task handles.

It is short-lived and belongs to one open view.

## Client PacketBridge

The client packet listener runs at high priority and handles:

- window clicks;
- anvil window buttons;
- client close-window packets;
- anvil rename packets.

Packets for other window ids are ignored. VGui-originated packets are sent silently so its listeners do not process their own output.

## ServerPacketBridge

The server bridge observes backend inventory and window behavior. It keeps the player inventory cache current and ends a proxy session when a backend takes over the screen. If VGui opens over a tracked backend container, it first sends that backend the matching client close packet. Reentrant ownership changes are serialized and stale backend open/close packets are suppressed when the proxy reclaimed the screen.

## InventoryTracker

The tracker stores the 36 main-inventory and hotbar slots required when composing a complete VGui window refresh. A backend-switch generation invalidates the old cache, ignores old-generation updates, and requires a new full window-0 baseline before deltas are accepted. Proxy opens are gated while a connection attempt is in progress, and a completed switch closes any old proxy view before activating the new generation. Entries are removed on disconnect.

It does not replace a backend inventory API.

## Window ids

VGui allocates ids from a proxy-reserved range of 101 through 119, cycling per player. The allocator forgets a player on disconnect.

Only clicks matching the active session window id are treated as VGui interactions.

## State ids

Modern inventory protocols include a state id. VGui increments the session state id
for slot and full-window packets, rejects and resynchronizes mismatched client ids, and
records an accepted id in `ClickContext`. Legacy packets without an id remain valid.

## Full refresh composition

A full packet contains:

1. every top-inventory VGui item, or an empty stack;
2. 36 tracked player inventory and hotbar stacks;
3. an empty cursor stack.

This preserves the lower inventory while making the proxy state authoritative.

## Scheduling

`UpdateScheduler` uses the Velocity scheduler. Each scheduled task is attached to its session, observes a minimum interval, checks session state, catches callback errors, and can be cancelled through `UpdateTask`.

## Error boundaries

Callback and packet-send errors are logged around major extension points. Failed open and live window sends retire the affected session and cancel its tasks. Packet protocol changes and incompatible dependencies can still prevent correct operation, which is why live validation remains required.

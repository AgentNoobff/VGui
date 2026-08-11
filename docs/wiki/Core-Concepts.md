# Core Concepts

VGui separates reusable menu definitions from one player's live session. Understanding that split prevents shared-state bugs.

## View

A `View` describes a screen:

- title
- container type
- open, click, close, and anvil-input callbacks
- click cooldown
- transaction cancellation policy

A built view should be treated as immutable and stateless. Store it in a field and open it for many players.

## ViewContents

`ViewContents` belongs to one player and one open view. It contains the current `ViewItem` at each top-inventory slot and provides mutations such as `set`, `fill`, `batch`, `title`, `schedule`, and `pagination`.

Contents stop being active when the view closes. Calls made after closure are ignored where a packet would otherwise be sent. Use `isOpen()` before applying results from asynchronous work.

## ViewContext

`ViewContext` is the per-open key/value store. It is the correct place for a selected category, account id, filter, permission snapshot, or other state used by the menu.

Typed `ViewKey<T>` values are safer than strings. Plain string keys remain available for simple integrations.

When a view is placed in back history, its context is stored with it. Returning through `back()` reopens the view with that context and runs `onOpen` again.

## ViewItem

A `ViewItem` combines a PacketEvents `ItemStack` with an optional `Consumer<ClickContext>`. It is immutable and can be reused in multiple slots and views.

Factory methods cover display items, clickable items, close buttons, back buttons, page controls, and links to another view.

## ClickContext

`ClickContext` is created for one decoded client click. It exposes:

- player, slot, row and column, click type, hotbar key, and raw packet fields
- current view, contents, and context
- close, open, replace, back, and sound actions

Handlers run on the player's Netty thread. They must not wait for a database, HTTP request, file operation, or long computation.

## VGui and VGuiService

`VGui` is the convenient static facade. `VGui.get()` returns the active `VGuiService` installed during proxy initialization.

Use static calls for normal integrations:

```java
VGui.open(player, view);
ViewContents contents = VGui.currentContents(player);
boolean open = VGui.isViewing(player);
```

Use the service directly when dependency injection or explicit ownership is preferable.

## Session lifecycle

Opening a view creates a session, assigns a proxy window id, runs `onOpen`, sends the complete contents, and notifies global listeners.

Opening a second view closes the previous view with `CloseReason.SWITCHED`. Normal `open` also pushes the previous view and context into history. `openReplacing` does not.

Closing cancels scheduled tasks and calls the close hooks. A client close, plugin close, backend override, disconnect, and proxy shutdown have distinct close reasons.

## Authoritative transactions

By default, VGui cancels every click packet for its window. It runs the appropriate handlers and then sends a full window refresh with an empty cursor. This removes the client's predicted movement and restores the proxy-owned state.

The default behavior means buttons are safe display items. It does not mean VGui can transfer real inventory items. A view returning `false` from `cancelClientTransactions()` opts out of this protection and must supply its own transaction design.

## Player inventory tracking

The top inventory belongs to VGui. The 36 player inventory and hotbar slots shown below it belong to the backend. VGui observes backend inventory packets and caches those slots so a full proxy-window refresh can preserve what the player sees.

That cache is visual state for packet composition. It is not a general inventory API and should not be used as authoritative gameplay data.

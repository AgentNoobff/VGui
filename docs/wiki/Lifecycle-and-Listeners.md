# Lifecycle and Listeners

Views have local callbacks. `VGuiListener` observes all views on the proxy.

## View callbacks

### `onOpen(ViewContents)`

Runs after the window packet is sent and before the initial full contents packet. Initial mutations are automatically batched.

For builder views, layout items, explicit items, and repeating update tasks are installed before the configured `onOpen` callback.

`onOpen` runs again when a view is restored through back navigation.

### `onClick(ClickContext)`

Runs for processed clicks after global listener approval and after the top-slot item's handler. It also receives clicks in the player-inventory section and outside the window.

### `onAnvilInput(String, ViewContents)`

Runs for every rename packet while an anvil view is active.

### `onClose(Player, CloseReason, ViewContext)`

Runs once when the session stops being shown.

## Close reasons

| Reason | Cause |
| --- | --- |
| `CLIENT` | Player closed the window, such as with Escape |
| `SERVER` | Plugin called a VGui close method |
| `SWITCHED` | Another proxy view replaced this one |
| `OVERRIDDEN` | Backend opened or closed a window and took control |
| `DISCONNECT` | Player left the proxy |
| `SHUTDOWN` | Proxy is stopping |

Use the reason to avoid treating navigation as cancellation:

```java
.onClose((player, reason, context) -> {
    if (reason == CloseReason.CLIENT) {
        recordUserDismissal(player);
    }
})
```

## Global listeners

```java
VGuiListener listener = new VGuiListener() {
    @Override
    public void onOpen(Player player, View view, ViewContents contents) {
        metrics.increment("vgui.opens");
    }

    @Override
    public boolean onClick(ClickContext click) {
        return hasMenuPermission(click.player(), click.view());
    }

    @Override
    public void onClose(Player player, View view, CloseReason reason) {
        metrics.increment("vgui.closes." + reason.name().toLowerCase());
    }
};

VGui.addListener(listener);
```

Remove it when the owning integration stops:

```java
VGui.removeListener(listener);
```

Listeners are held in a copy-on-write list. Register stable listener instances rather than creating one per player.

## Click veto

Every global listener receives the click while its originating session remains current.
If any returns false, item and view handlers are skipped. Other listeners still run
because VGui collects the final allowed result rather than stopping at the first veto.

A veto always cancels and resynchronizes the click, including for a view that otherwise
allows transaction pass-through. An exception from a click listener is logged and
treated as a veto so permission and audit gates fail closed.

## Error isolation

VGui catches exceptions from view and listener callbacks and logs them. A failed
`onOpen` aborts that session and fires its close lifecycle; ordinary open/close
listener failures remain isolated. Do not rely on exceptions as normal control flow.

## Task cleanup

All scheduled update tasks attached to the session are cancelled before close callbacks run. The contents are marked closed before callbacks, so further mutations do not send session packets.

## Shutdown

On proxy shutdown, VGui rejects new opens, closes all sessions with `SHUTDOWN`, and
then terminates the PacketEvents API owned by the standalone bootstrap.

## Listener uses

Appropriate uses include metrics, audit logging with privacy limits, permission gates, shared click sounds, and diagnostics. Avoid database writes or external requests directly in callbacks because they run on the player's network thread.

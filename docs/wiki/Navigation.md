# Navigation

VGui keeps a bounded, per-player back history for proxy views.

## Open and push history

```java
click.open(detailsView);
contents.open(detailsView);
VGui.open(player, detailsView);
```

If another VGui view is open, these calls close it with `CloseReason.SWITCHED`, store its view and context in history, and open the new view.

## Replace without history

```java
click.openReplacing(statusView);
contents.openReplacing(statusView);
VGui.openReplacing(player, statusView);
```

Use replacement for transient state, a refresh represented by another view, or a redirect the user should not return to.

## Go back

```java
if (!click.back()) {
    click.close();
}
```

`back()` returns true when an entry was reopened. The entry is consumed only after reopening succeeds. Reopening does not push the current view back into history, so moving backward consumes the stack normally.

`ViewItem.backButton(item)` implements this exact behavior: back if possible, otherwise close.

## History capacity

Each player history stores at most ten entries. When an eleventh entry is pushed, the oldest entry is removed. This bounds memory and prevents unbounded navigation chains.

## Context restoration

History stores the previous `View`, `ViewContext`, and current live title. It does not store `ViewContents`.

When the player returns:

1. the old view is opened with a new window id and new contents;
2. the saved context is attached;
3. `onOpen` runs again;
4. the view should rebuild its items from context.

## When history is cleared

History is cleared when a session fully ends through a client close, server close, backend override, disconnect, or proxy shutdown. Switching between VGui views preserves it.

A disconnect also clears inventory tracking and window-id allocation state.

## Backend overrides

If a backend connection completes or a backend server opens or closes a window while a proxy view is active, VGui ends the proxy session with `CloseReason.OVERRIDDEN`. The backend takes control, and VGui clears the navigation history to avoid reopening stale proxy state unexpectedly.

## Lazy submenus

Use a supplier when building the next view is expensive or depends on the moment of the click:

```java
ViewItem button = ViewItem.opens(icon, () -> createDetailsView(currentConfig()));
```

The supplier runs on the click thread, so it must remain fast.

## Navigation graph design

- Use `open` for user-chosen drill-down navigation.
- Use `openReplacing` for redirects and completed steps.
- Put back buttons in a consistent slot.
- Render restored state from context.
- Avoid building a circular navigation loop that continually pushes equivalent views.
- Close after terminal actions such as confirmation when no return path is useful.

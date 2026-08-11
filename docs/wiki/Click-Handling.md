# Click Handling

VGui decodes client inventory packets into `ClickContext`, applies global vetoes, dispatches item and view handlers, and restores the authoritative screen when transactions are cancelled.

## Dispatch order

For a click targeting the current VGui window:

1. The state id and raw mode, button, slot, and changed-slot indexes are validated.
2. VGui reads the view's transaction cancellation policy.
3. A configured per-player cooldown is checked for non-drag clicks.
4. Every global `VGuiListener.onClick` runs.
5. If all listeners return true, the clicked top-slot `ViewItem` handler runs.
6. The view-level `onClick` handler runs.
7. If cancellation is enabled and the same session remains open, VGui corrects the predicted slots and cursor.

A malformed or unknown click is cancelled before callbacks. A global listener returning false skips both the item and view handlers and corrects the client even for a pass-through view.

## ClickType values

| Value | Meaning |
| --- | --- |
| `LEFT` | Normal left click |
| `RIGHT` | Normal right click |
| `MIDDLE` | Creative clone click |
| `SHIFT_LEFT` | Shift and left click |
| `SHIFT_RIGHT` | Shift and right click |
| `NUMBER_KEY` | Number key 1 through 9 |
| `OFFHAND_SWAP` | F key swap |
| `DROP` | Q drop-one action |
| `CTRL_DROP` | Control and Q drop-stack action |
| `LEFT_OUTSIDE` | Left click outside the window |
| `RIGHT_OUTSIDE` | Right click outside the window |
| `DOUBLE_CLICK` | Collect matching items |
| `DRAG_START` | Start click-drag |
| `DRAG_ADD` | Add a slot to click-drag |
| `DRAG_END` | Finish click-drag |
| `UNKNOWN` | Unrecognized mode and button combination |

Convenience predicates include `isShift`, `isLeft`, `isRight`, `isDrag`, and `isOutside`.

## ClickContext information

- `player()` returns the Velocity player.
- `slot()` is the raw window slot, or `-999` for outside clicks.
- `slotPos()` returns row and column only for top-inventory slots.
- `isGui()` identifies the top inventory.
- `isPlayerInventory()` identifies the player-inventory section.
- `clickType()` returns the decoded classification.
- `hotbarKey()` returns 0 through 8 for `NUMBER_KEY`, otherwise `-1`.
- `view()`, `contents()`, and `context()` expose the current session.
- `rawButton()`, `rawMode()`, and `stateId()` expose packet-level values for advanced diagnostics.

Do not assume every non-GUI click is a normal player-inventory slot. Outside clicks have negative slots.

## Actions

```java
click.close();
click.open(detailsView);
click.openReplacing(statusView);
boolean returned = click.back();
click.playSound(Sounds.UI_BUTTON_CLICK);
click.playSound(sound, 0.7f, 1.2f);
```

Opening or closing inside a handler changes the active session. VGui notices that change and does not resync the closed session afterward.

## Item handler example

```java
ViewItem toggle = ItemBuilder.of(ItemTypes.LEVER)
    .name(Component.text("Toggle alerts"))
    .onClick(click -> {
        boolean enabled = Boolean.TRUE.equals(click.context().get(ALERTS));
        click.context().set(ALERTS, !enabled);
        click.contents().set(click.slot(), alertsItem(!enabled));
        click.playSound(Sounds.UI_BUTTON_CLICK);
    });
```

## View-level handler

Use `onClick` for behavior that is not tied to one item, such as diagnostics or clicks in the player inventory.

```java
.onClick(click -> {
    if (click.isPlayerInventory()) {
        auditPlayerInventoryClick(click.player(), click.slot(), click.clickType());
    }
})
```

## Transaction cancellation

Cancellation defaults to true. The client may visually predict item movement before receiving the correction, so VGui restores the changed slots and clears the cursor after a processed click. Large or legacy multi-slot operations fall back to a complete window refresh.

Returning false from `cancelClientTransactions()` allows packets to continue toward the backend. The backend did not open the proxy window and may not understand those transactions. This option is only for an integration with a complete packet and inventory strategy.

## Cooldown

The cooldown is disabled by default. If configured, a click arriving sooner does not invoke handlers and is still corrected. Drag start/add/end packets bypass the cooldown so quick-craft sequences remain coherent.

```java
.clickCooldown(Duration.ofMillis(150))
```

Use zero to disable. The builder rejects negative durations.

## Threading

Click and listener callbacks run on the player's Netty thread. Never block it.

For external work:

1. Copy the values needed from the click.
2. Submit the slow work to an executor or Velocity scheduler.
3. Before updating, confirm `contents.isOpen()` and that the result still belongs to the current view.
4. Apply several slot changes in `batch`.

See [Performance and Threading](Performance-and-Threading.md) for safe patterns.

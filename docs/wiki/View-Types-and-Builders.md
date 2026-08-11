# View Types and Builders

`ViewType` selects the client container shape. `ViewBuilder` is the main way to create a view.

## Available view types

| Constant | Top slots | Columns | Rows | Notes |
| --- | ---: | ---: | ---: | --- |
| `CHEST_9X1` | 9 | 9 | 1 | One-row generic chest |
| `CHEST_9X2` | 18 | 9 | 2 | Two-row generic chest |
| `CHEST_9X3` | 27 | 9 | 3 | Three-row generic chest |
| `CHEST_9X4` | 36 | 9 | 4 | Four-row generic chest |
| `CHEST_9X5` | 45 | 9 | 5 | Five-row generic chest |
| `CHEST_9X6` | 54 | 9 | 6 | Six-row generic chest |
| `HOPPER` | 5 | 5 | 1 | Hopper window |
| `DISPENSER` | 9 | 3 | 3 | Dispenser-style grid |
| `ANVIL` | 3 | 3 | 1 | Anvil slots and rename input |

The player inventory is appended by the client and is not included in these slot counts.

## Type helpers

- `ViewType.chestRows(rows)` accepts 1 through 6 and returns the matching chest type.
- `ViewType.chestForSize(size)` rounds up to the smallest chest that fits, clamped to 1 through 6 rows.
- `slots()`, `columns()`, and `rows()` expose dimensions.
- `isChest()` identifies any generic chest type.

## Starting a builder

```java
ViewBuilder chest = VGui.chest(4);
ViewBuilder hopper = VGui.view(ViewType.HOPPER);
```

`VGui.chest(rows)` validates the row count immediately. `VGui.view(type)` works with every type.

## Builder operations

### `title(Component)`

Sets the initial title. The default is `Menu`. Use Adventure components, not legacy text strings.

### `layout(String...)`

Sets a character mask. Its row count and width must exactly match the chosen type. See [Layouts and Slots](Layouts-and-Slots.md).

### `map(char, ViewItem)`

Maps a layout character. Calling `map` without `layout` causes `build()` to fail. Dot and space remain reserved empty characters.

### `item(int, ViewItem)`

Places an item by flat slot. The builder rejects out-of-range slots.

### `item(int row, int column, ViewItem)`

Places by zero-based coordinates. Rows and columns are validated independently, so an invalid column cannot alias the next row. Explicit items are applied after the layout, so they override mapped items at the same slot.

### Lifecycle callbacks

- `onOpen(Consumer<ViewContents>)` runs after layouts, explicit items, and registered repeating updates are applied.
- `onClick(Consumer<ClickContext>)` runs after an item handler when global listeners allow the click.
- `onClose(CloseHandler)` receives the player, reason, and context.
- `onAnvilInput(BiConsumer<String, ViewContents>)` receives every anvil rename update.

### `updateEvery(Duration, Consumer<ViewContents>)`

Registers a repeating task that starts with each open session and is cancelled when that session ends.

### `cancelClientTransactions(boolean)`

Defaults to `true`. Keep it enabled for button-style GUIs.

### `clickCooldown(Duration)`

Disabled by default. A configured cooldown ignores and corrects faster non-drag clicks; drag phases are never throttled. A zero duration disables the cooldown, and negative durations are rejected.

### `build()`

Validates the layout and creates an immutable reusable `View`.

## Implementing View directly

Implement `View` when a named class makes the design clearer or when behavior is easier to express with methods:

```java
public final class ProfileView implements View {
    @Override
    public Component title() {
        return Component.text("Profile");
    }

    @Override
    public ViewType type() {
        return ViewType.CHEST_9X3;
    }

    @Override
    public void onOpen(ViewContents contents) {
        contents.set(13, buildProfileItem(contents.player()));
    }
}
```

Do not store `ViewContents` or the current player in instance fields. The same view can be open for several players at once.

## Extending ChestView

`ChestView` is a convenience base class for static chest menus. Its constructor accepts rows and title. Call protected `setItem` methods in the subclass constructor.

```java
public final class HelpView extends ChestView {
    public HelpView() {
        super(3, Component.text("Help"));
        setItem(13, ItemBuilder.of(ItemTypes.BOOK)
            .name(Component.text("Documentation"))
            .asItem());
    }
}
```

`rows()` and `size()` expose the chest dimensions. The flat `setItem(slot, item)` overload keeps its documented ignore-on-out-of-range behavior; the row/column overload rejects invalid coordinates so they cannot alias another slot.

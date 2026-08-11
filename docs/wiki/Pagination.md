# Pagination

`Pagination` renders a list of `ViewItem` values into an ordered region of slots. It belongs to one `ViewContents` session.

## Basic setup

```java
Layout layout = Layout.of(
    "#########",
    "#ppppppp#",
    "#ppppppp#",
    "#ppppppp#",
    "<###b###>");

View view = VGui.chest(5)
    .layout(
        "#########",
        "#ppppppp#",
        "#ppppppp#",
        "#ppppppp#",
        "<###b###>")
    .map('#', filler)
    .map('<', ViewItem.pagePrevious(previousArrow))
    .map('>', ViewItem.pageNext(nextArrow))
    .map('b', ViewItem.backButton(backItem))
    .onOpen(contents -> contents.pagination()
        .slots(layout, 'p')
        .items(entries))
    .build();
```

The `p` character stays unmapped in the view layout. `slots(layout, 'p')` extracts its indexes in reading order.

## Configure slots directly

```java
contents.pagination().slots(10, 11, 12, 13, 14, 15, 16);
```

Every slot is validated against the top inventory and duplicate indexes are rejected. The array is copied. Setting slots resets the page to zero and renders.

## Set items

```java
pagination.items(List.copyOf(items));
```

The list is copied. Replacing the list clamps the current page to the new valid range and renders immediately.

## Queries

- `page()` returns the zero-based page.
- `pageCount()` returns at least one, even with no items or slots.
- `itemsPerPage()` returns the number of region slots.
- `isFirst()` and `isLast()` describe the current position.

## Navigation

- `open(page)` clamps to the valid range.
- `next()` and `previous()` stay at the edge when necessary.
- `first()` and `last()` jump directly.
- `render()` redraws the current page.

Every navigation method re-renders the region in one batch.

## Empty slots on the last page

Pagination clears region slots that do not have an item for the current page. This prevents leftover items from a fuller previous page.

## Dynamic controls

The built-in previous and next buttons always remain clickable. If you want disabled edge buttons, update them after navigation:

```java
ViewItem next = ViewItem.clickable(nextArrow, click -> {
    Pagination pagination = click.contents().pagination();
    pagination.next();
    renderControls(click.contents(), pagination);
});
```

`renderControls` can display a disabled item when `isLast()` is true and change the title to show the page number.

## Updating data

Call `items(newItems)` after filters or live data change. The helper keeps its configured region, clamps the page, and re-renders.

For mutable external collections, always pass a snapshot. The pagination helper intentionally copies the list so concurrent mutations cannot change rendering halfway through.

## Common mistakes

- Mapping the region character to a filler, which fills the slots before pagination but is harmless because pagination replaces them.
- Using a layout with dimensions different from the view.
- Creating a new layout whose region does not match the builder layout.
- Assuming `pageCount()` can be zero.
- Manually calling `refresh()` after page navigation. Pagination already batches and refreshes.

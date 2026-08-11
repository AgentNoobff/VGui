# Layouts and Slots

Layouts turn a visual character grid into slot assignments. They make menu structure easier to review than a long list of numeric indexes.

## Character masks

Each string is one row and each character is one slot:

```java
Layout layout = Layout.of(
    "#########",
    "#ppppppp#",
    "#ppppppp#",
    "#<..c..>#")
    .where('#', filler)
    .where('<', previous)
    .where('>', next)
    .where('c', close);
```

All rows must be nonempty and have the same width. A view only accepts a layout whose total slots and column count match its `ViewType`.

## Reserved and unmapped characters

`.` and space always mean empty. Attempting to map either throws `IllegalArgumentException`.

Any other unmapped character also remains empty. This is useful for named regions. In the example, `p` marks pagination slots without assigning a fixed item.

## Applying layouts

There are two equivalent styles.

Builder style:

```java
View view = VGui.chest(3)
    .layout(
        "#########",
        "#.......#",
        "#########")
    .map('#', filler)
    .build();
```

Contents style:

```java
contents.applyLayout(layout);
```

Application only writes mapped characters. It does not clear slots represented by empty or unmapped characters. Call `clear()` first if replacing an existing layout completely.

## Layout inspection

- `rows()` returns the row count.
- `columns()` returns the width.
- `size()` returns rows multiplied by columns.
- `charAt(slot)` reads a character by flat index and checks bounds.
- `slotsOf(character)` returns matching flat indexes in reading order.
- `resolve()` returns an unmodifiable ordered map of mapped slot indexes to items.

## Flat slot indexes

Slots are zero-based and increase left to right, then top to bottom. In a nine-column chest:

```text
row 0:  0  1  2  3  4  5  6  7  8
row 1:  9 10 11 12 13 14 15 16 17
row 2: 18 19 20 21 22 23 24 25 26
```

The center of a three-row chest is row 1, column 4, flat slot 13.

## Slot values

`Slot` is a record containing zero-based `row` and `column`.

```java
Slot center = Slot.of(1, 4);
int index = center.index(9);       // 13
Slot same = Slot.fromIndex(13, 9); // row 1, column 4
```

The constructor rejects negative coordinates. `fromIndex` rejects negative indexes and nonpositive column counts.

## Coordinate-based contents methods

```java
contents.set(1, 4, item);
contents.set(Slot.of(1, 4), item);
ViewItem current = contents.get(1, 4);
```

Coordinates are checked against the view width and size. A hopper has five columns. A dispenser and anvil have three. Do not use nine-column chest math for every type.

## Regions and fills

`ViewContents` includes common geometric operations:

- `fill` replaces every slot.
- `fillEmpty` writes only null slots.
- `fillRow` and `fillColumn` use zero-based positions.
- `fillBorder` writes the outer rows and columns.
- `fillRect` fills an inclusive rectangle and accepts corners in either order.

Each fill performs one full refresh rather than a packet per slot.

## Layout design tips

1. Use one character per semantic role, such as border, entries, previous, next, and close.
2. Keep interactive characters visibly distinct in the source grid.
3. Reserve an unmapped character for pagination regions.
4. Use explicit `item` calls only for deliberate overrides.
5. Test dimensions when changing the view type.

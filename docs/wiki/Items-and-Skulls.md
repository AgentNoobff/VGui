# Items and Skulls

VGui displays PacketEvents `ItemStack` values wrapped in `ViewItem`. `ItemBuilder` provides the usual menu metadata while writing both modern item components and legacy NBT.

## ItemBuilder basics

```java
ItemStack item = ItemBuilder.of(ItemTypes.DIAMOND_SWORD)
    .name(Component.text("Excalibur"))
    .lore(
        Component.text("Legendary blade"),
        Component.text("Click to inspect"))
    .glow()
    .build();
```

`of(type)` starts with amount 1. `of(type, amount)` accepts an initial amount. Amounts are clamped to 1 through 99, including later `amount` calls.

## Names and lore

`name(Component)` sets the modern custom-name component and legacy display name.

`lore(Component...)` and `lore(List<Component>)` set modern lore and serialize legacy lore lines. Use Adventure components for colors, decoration, and translatable content.

## Glint

`glow()` enables an enchantment glint without exposing a normal enchantment. `glow(boolean)` can explicitly set the modern override. When enabled, legacy enchantment tags and hide flags are also written.

## Other metadata

- `customModelData(int)` writes legacy custom model data used by resource packs.
- `unbreakable()` writes the legacy unbreakable tag.
- `legacyData(int)` sets pre-1.13 durability or metadata.
- `edit(Consumer<ItemStack.Builder>)` exposes the underlying PacketEvents builder for unsupported components.

The escape hatch can make an item version-specific. Verify it against every supported client protocol.

## Converting to ViewItem

```java
ViewItem display = ItemBuilder.of(ItemTypes.PAPER)
    .name(Component.text("Information"))
    .asItem();

ViewItem button = ItemBuilder.of(ItemTypes.EMERALD)
    .name(Component.text("Confirm"))
    .onClick(click -> confirm(click.player()));
```

`asItem()` is shorthand for `ViewItem.of(build())`. `onClick` is shorthand for `ViewItem.clickable(build(), handler)`.

## ViewItem factories

| Method | Behavior |
| --- | --- |
| `of(ItemStack)` | Display-only item |
| `of(ItemBuilder)` | Builds a display-only item |
| `clickable` | Runs a click handler |
| `closeButton` | Closes the current view |
| `backButton` | Goes back, or closes if history is empty |
| `pageNext` | Advances the current pagination helper |
| `pagePrevious` | Moves to the previous page |
| `opens(ItemStack, View)` | Opens a submenu and pushes history |
| `opens(ItemStack, Supplier<View>)` | Lazily creates and opens a submenu |

`withClick` returns a copy with another handler. Passing null removes the handler. `withItem` returns a copy with another stack while preserving the handler.

`item()`, `hasHandler()`, and `handle()` expose low-level state. Most integrations do not call `handle()` directly because VGui dispatches it.

## Player skulls

Use a base64 texture property:

```java
ItemStack head = Skulls.texturedHead(base64Texture);
```

Use a builder to add a name or lore:

```java
ViewItem head = Skulls.builder(base64Texture)
    .name(Component.text("Profile"))
    .lore(Component.text("Click to open"))
    .onClick(click -> click.open(profileView));
```

Use a premium player name:

```java
ItemStack head = Skulls.ownedHead("PlayerName");
```

`Skulls.textureFromUrl(url)` converts a `textures.minecraft.net` skin URL into the base64 JSON value used by `skullTexture`.

## Cross-version behavior

Modern clients read PacketEvents item components. Older clients read the legacy NBT written alongside them. A feature may still be unavailable on old protocols. For example, custom model data has no effect before its client version existed, and very old clients may show a plain head when profile data cannot be represented.

## Reuse and mutation

`ViewItem` is immutable. PacketEvents `ItemStack` should also be treated as a value after building. Create a new item when a label, amount, or lore must change, then replace the slot through `ViewContents.set`.

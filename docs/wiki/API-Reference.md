# API Reference

This page is a map of every public VGui API type. The [generated Javadocs](https://agentnoobff.github.io/VGui/) contain complete signatures and linked source-level documentation.

## `me.agent.vgui.api`

### `VGui`

Static service access, chest and generic builders, open and replace overloads, back and close, current session queries, anvil prompts, and global listener registration.

### `VGuiService`

Instance form of session operations. It additionally exposes `closeAll` and replacement overloads that accept an explicit context.

### `View`

Contract for title, type, lifecycle callbacks, transaction cancellation, and click cooldown.

### `ViewBuilder`

Fluent immutable-view builder covering title, layout mappings, explicit items, callbacks, repeating updates, cancellation, and cooldown.

### `ViewType`

Chest sizes, hopper, dispenser, and anvil dimensions plus chest-selection helpers.

### `ChestView`

Base class for static chest views with protected item placement.

### `CloseHandler`

Functional interface receiving player, `CloseReason`, and `ViewContext`.

### `CloseReason`

Client, server, switched, overridden, disconnect, and shutdown reasons.

## `me.agent.vgui.api.click`

### `ClickContext`

Decoded click data, raw packet fields, current session access, navigation, close, back, and player-only sounds.

### `ClickType`

High-level click categories and classification helpers.

## `me.agent.vgui.api.contents`

### `ViewContents`

Per-session slots, fills, layouts, batching, refresh, title, schedules, pagination, and navigation.

### `Layout`

Character-mask dimensions, mappings, slot lookup, and resolution.

### `Slot`

Zero-based row and column value with flat-index conversion.

### `Pagination`

Region and item configuration, page queries, navigation, and rendering.

### `UpdateTask`

Cancellation handle for a scheduled repeating update.

## `me.agent.vgui.api.context`

### `ViewContext`

Typed and string key storage plus a snapshot of string entries.

### `ViewKey<T>`

Named typed key with value-based equality on name and class.

## `me.agent.vgui.api.event`

### `VGuiListener`

Global open, vetoable click, and close callbacks.

## `me.agent.vgui.api.input`

### `AnvilInputView`

Ready-made anvil prompt with current-text context key.

### `AnvilInputView.Builder`

Title, initial text, item type, typing, confirm, cancel, build, and open operations.

## `me.agent.vgui.api.item`

### `ViewItem`

Immutable item and optional handler plus factories for common navigation controls.

### `ItemBuilder`

PacketEvents item builder for name, lore, amount, glint, model data, unbreakable state, skull profiles, legacy data, low-level edits, and `ViewItem` conversion.

### `Skulls`

Textured and owner-name player-head shortcuts plus skin-URL encoding.

## Bootstrap API

### `me.agent.vgui.core.VGuiBootstrap`

Advanced shaded integration API. `loadPacketEvents` runs before proxy initialization, `init` runs during initialization, `instance` reports current state, and `shutdown` performs cleanup.

`sessionManager()` exposes an internal implementation type and should not be treated as stable public integration surface.

## Package stability

Types under `me.agent.vgui.api` are the supported integration API. The `me.agent.vgui.core.internal` package is implementation detail and may change without source compatibility guarantees.

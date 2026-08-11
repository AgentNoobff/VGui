# VGui Wiki

VGui is a Velocity library and standalone plugin for proxy-owned inventory screens. It opens container windows directly from the proxy, receives client clicks through PacketEvents, and keeps backend servers out of the menu lifecycle.

This Wiki documents the complete public API, deployment options, packet behavior, threading rules, build process, and common integration patterns.

## Start here

| Goal | Page |
| --- | --- |
| Install VGui and open a first menu | [Getting Started](Getting-Started.md) |
| Add VGui to Maven or Gradle | [Installation](Installation.md) |
| Understand views, contents, and context | [Core Concepts](Core-Concepts.md) |
| Build menus with every available view type | [View Types and Builders](View-Types-and-Builders.md) |
| Design character-mask layouts | [Layouts and Slots](Layouts-and-Slots.md) |
| Build items, skulls, buttons, and actions | [Items and Skulls](Items-and-Skulls.md) |
| Handle every click type | [Click Handling](Click-Handling.md) |
| Change open menus and schedule updates | [Contents and Updates](Contents-and-Updates.md) |
| Store per-player state | [Context and State](Context-and-State.md) |
| Open submenus and go back | [Navigation](Navigation.md) |
| Build paged menus | [Pagination](Pagination.md) |
| Collect text with an anvil | [Anvil Input](Anvil-Input.md) |
| Use lifecycle callbacks and global listeners | [Lifecycle and Listeners](Lifecycle-and-Listeners.md) |
| Copy complete examples | [Examples and Recipes](Examples-and-Recipes.md) |
| Diagnose a failure | [Troubleshooting](Troubleshooting.md) |

## What VGui controls

VGui owns the top inventory of its proxy window. It assigns a proxy window id, sends the open-window packet, renders GUI items, decodes clicks, and restores the authoritative contents after cancelled client transactions. It also appends the tracked player inventory to full refresh packets so a GUI update does not visually clear the hotbar or main inventory.

The backend still owns the real player inventory. A default VGui view is a display-and-click interface, not a second inventory implementation. VGui cancels transactions so a client cannot move items into or out of the proxy GUI. If an integration disables cancellation, that integration is responsible for the resulting inventory behavior.

## Public entry points

- `VGui` provides static builders and delegates to the active service.
- `VGuiService` opens, replaces, closes, and queries views.
- `View` describes a reusable screen and its lifecycle.
- `ViewBuilder` creates immutable `View` instances without subclassing.
- `ViewContents` is the mutable state of one player viewing one screen.
- `ViewContext` stores state associated with that open screen.
- `ViewItem` combines a PacketEvents item stack with an optional click handler.
- `ItemBuilder` writes modern item components and legacy NBT.
- `ClickContext` describes one click and exposes navigation and sound actions.

The generated [API Javadocs](https://agentnoobff.github.io/VGui/) provide signature-level documentation. This Wiki focuses on how the pieces work together.

## Deployment choices

Use the standalone VGui plugin for most networks. A dependent plugin declares the `vgui` plugin dependency and compiles against VGui with `provided` or `compileOnly` scope.

Shading is available for plugins that must be self-contained. Shaded integrations must relocate the VGui package and call the bootstrap lifecycle methods themselves. See [Shading VGui](Shading-VGui.md).

## Current support baseline

The current source builds with Java 25 and Velocity 4.0.0. PacketEvents, Velocity, test, Maven plugin, and GitHub Actions versions are centralized and monitored by Dependabot. See [Compatibility](Compatibility.md) for the difference between a compiled path and a live-tested combination.

## Project links

- [Repository](https://github.com/AgentNoobff/VGui)
- [Latest release](https://github.com/AgentNoobff/VGui/releases/latest)
- [API Javadocs](https://agentnoobff.github.io/VGui/)
- [Issues](https://github.com/AgentNoobff/VGui/issues)
- [Discussions](https://github.com/AgentNoobff/VGui/discussions)
- [Security reports](https://github.com/AgentNoobff/VGui/security/advisories/new)

<details>
<summary aria-label="Documentation note">&#8203;</summary>
<small><sub>Some documentation was AI-assisted and may contain minor inaccuracies; please verify details before relying on it.</sub></small>
</details>

# Getting Started

This guide installs VGui as a standalone Velocity plugin, adds the API to another plugin, and opens a first menu.

## Requirements

- A Velocity 4 proxy running Java 25 or newer
- A plugin project that targets Java 25
- Maven or Gradle for the dependent plugin
- VGui on the proxy at runtime

PacketEvents does not have to be installed separately. The standalone VGui release
contains the PacketEvents runtime modules it needs and manages that API for its own
lifecycle. Do not add a separate PacketEvents plugin solely for VGui.

## 1. Install VGui

Download the [latest stable jar](https://github.com/AgentNoobff/VGui/releases/latest/download/vgui.jar), place it in the Velocity `plugins` directory, and restart the proxy.

The stable asset is always named `vgui.jar`. The URL follows the latest GitHub release and does not contain a release number.

On startup, look for messages confirming that PacketEvents and the VGui service initialized.

## 2. Add the compile dependency

Add JitPack and the VGui dependency. Use `main-SNAPSHOT` while developing against the newest main branch, or replace it with a `vX.Y.Z` release tag for a reproducible production build.

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.AgentNoobff</groupId>
    <artifactId>VGUI</artifactId>
    <version>main-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

JitPack can fetch the project directly from the repository. See [Installation](Installation.md) for Gradle and dependency details.

## 3. Declare the runtime plugin dependency

Make Velocity load VGui before your plugin:

```java
@Plugin(
    id = "myplugin",
    dependencies = @Dependency(id = "vgui")
)
public final class MyPlugin {
}
```

Without this dependency, your plugin may call `VGui.get()` before initialization and receive an `IllegalStateException`.

## 4. Build a reusable view

```java
private final View selector = VGui.chest(3)
    .title(Component.text("Choose a server"))
    .layout(
        "#########",
        "#...s...#",
        "####c####")
    .map('#', ItemBuilder.of(ItemTypes.GRAY_STAINED_GLASS_PANE)
        .name(Component.text(" "))
        .asItem())
    .map('s', ItemBuilder.of(ItemTypes.COMPASS)
        .name(Component.text("Survival"))
        .lore(Component.text("Click to connect"))
        .onClick(click -> connectToSurvival(click.player())))
    .map('c', ViewItem.closeButton(ItemBuilder.of(ItemTypes.BARRIER)
        .name(Component.text("Close"))
        .build()))
    .build();
```

The result is immutable and can be stored once. Per-player state is created when the view opens.

## 5. Open it

Call `VGui.open` from a command, event, or other plugin action:

```java
VGui.open(player, selector);
```

The proxy sends the window immediately. VGui calls the view's open handler, sends its initial contents, and registers the session for clicks and cleanup.

## 6. Try the built-in demo

Grant `vgui.demo` and run `/vgui` as a player. The demo includes layouts, pagination, anvil input, live updates, navigation, and sounds. The source is also a useful integration example.

## Important first rules

1. Keep click handlers fast. They run on the player's network thread.
2. Use `ViewContext` for per-player values. Do not put mutable player state in a reusable `View` instance.
3. Use `ViewContents.batch` when changing several slots at once.
4. Leave client transaction cancellation enabled unless you implement the full inventory behavior yourself.
5. Pin a release tag before shipping a production plugin.

Continue with [Core Concepts](Core-Concepts.md), then use [Examples and Recipes](Examples-and-Recipes.md) for larger menus.

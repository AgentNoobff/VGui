# VGui

Proxy-side inventory screens for [Velocity](https://papermc.io/software/velocity), powered by [PacketEvents](https://github.com/retrooper/packetevents).

[![Build](https://github.com/AgentNoobff/VGui/actions/workflows/build.yml/badge.svg)](https://github.com/AgentNoobff/VGui/actions/workflows/build.yml)
[![Latest release](https://img.shields.io/github/v/release/AgentNoobff/VGui?display_name=tag&sort=semver)](https://github.com/AgentNoobff/VGui/releases/latest)
[![Javadocs](https://img.shields.io/badge/API-Javadocs-2f81f7)](https://agentnoobff.github.io/VGui/)
[![Java 25](https://img.shields.io/badge/Java-25-e76f00)](https://adoptium.net/temurin/releases/?version=25)
[![License](https://img.shields.io/github/license/AgentNoobff/VGui)](LICENSE)

VGui lets a Velocity plugin open container menus without installing a matching plugin on every backend server. The proxy owns the window, receives clicks, preserves the player inventory during refreshes, and prevents client-side item transactions by default.

Use it for server selectors, network shops, queues, moderation tools, player settings, text prompts, and any menu that should remain under proxy control.

## Documentation

- [Complete GitHub Wiki](https://github.com/AgentNoobff/VGui/wiki)
- [Getting started](https://github.com/AgentNoobff/VGui/wiki/Getting-Started)
- [Installation and dependency setup](https://github.com/AgentNoobff/VGui/wiki/Installation)
- [Examples and recipes](https://github.com/AgentNoobff/VGui/wiki/Examples-and-Recipes)
- [API Javadocs](https://agentnoobff.github.io/VGui/)
- [Troubleshooting](https://github.com/AgentNoobff/VGui/wiki/Troubleshooting)

## Highlights

- Chest, hopper, dispenser, and anvil views
- Reusable immutable view definitions with per-player contents and context
- Character layouts, fills, rectangles, batching, and live title changes
- Item, view, and global click handlers
- Pagination and bounded back navigation
- Scheduled counters, animations, and live data
- Anvil text prompts with callback and `CompletableFuture` APIs
- Modern item components plus legacy NBT for cross-version rendering
- Standalone plugin and shaded-library deployment modes

## Install the standalone plugin

1. Download the stable [latest VGui jar](https://github.com/AgentNoobff/VGui/releases/latest/download/vgui.jar).
2. Put `vgui.jar` in the Velocity `plugins` directory.
3. Restart the proxy.
4. Declare VGui as a dependency of the plugin that uses its API.

The release jar embeds the PacketEvents API, Velocity adapter, and common runtime
classes needed by VGui. It deliberately does not embed Netty, which Velocity supplies,
and PacketEvents does not need to be installed separately.

```java
@Plugin(id = "myplugin", dependencies = @Dependency(id = "vgui"))
public final class MyPlugin {
}
```

The release asset has a stable filename, so the link always follows the newest release. The `/vgui` command opens the built-in demo for players with the `vgui.demo` permission.

## Use VGui as a dependency

VGui is available through [JitPack](https://jitpack.io). You do not need to download or install the jar manually.

### Maven

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

### Gradle Kotlin DSL

```kotlin
repositories {
    maven("https://jitpack.io") {
        content { includeGroup("com.github.AgentNoobff") }
    }
}

dependencies {
    compileOnly("com.github.AgentNoobff:VGUI:main-SNAPSHOT")
}
```

`main-SNAPSHOT` follows the current `main` branch. Production plugins should replace it with a release tag from the [latest release](https://github.com/AgentNoobff/VGui/releases/latest), using the `vX.Y.Z` form, for reproducible builds.

Keep the dependency in `provided` or `compileOnly` scope when the standalone VGui plugin supplies it at runtime. See the [shading guide](https://github.com/AgentNoobff/VGui/wiki/Shading-VGui) if you need to embed and relocate VGui instead.

## Small example

```java
View menu = VGui.chest(3)
    .title(Component.text("Proxy menu"))
    .layout(
        "#########",
        "#...i...#",
        "####c####")
    .map('#', ItemBuilder.of(ItemTypes.GRAY_STAINED_GLASS_PANE)
        .name(Component.text(" "))
        .asItem())
    .map('i', ItemBuilder.of(ItemTypes.EMERALD)
        .name(Component.text("Say hello"))
        .onClick(click -> click.player().sendMessage(Component.text("Hello from the proxy."))))
    .map('c', ViewItem.closeButton(ItemBuilder.of(ItemTypes.BARRIER)
        .name(Component.text("Close"))
        .build()))
    .build();

VGui.open(player, menu);
```

Views are reusable. Each open session receives its own `ViewContents` and `ViewContext`.

## Compatibility

VGui builds for Java 25 and Velocity 4.0.0. Dependency versions are centralized in [`pom.xml`](pom.xml) and monitored through Dependabot. The packet layer contains compatibility paths for Minecraft 1.8 through current clients, but packet changes still require live testing against the exact client, Velocity, PacketEvents, and backend combination used in production.

## Build

Use the included Maven Wrapper. No system Maven installation is required.

```bash
./mvnw clean verify
```

On Windows:

```powershell
.\mvnw.cmd clean verify
```

The build runs focused unit tests and creates the main, source, and Javadoc jars in `target/`. CI also checks the plugin descriptor, embedded PacketEvents bootstrap, absence of bundled Netty, public API class, generated Javadocs, and uploaded artifacts.

## Contributing and security

Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request. Participation is governed by the [Code of Conduct](CODE_OF_CONDUCT.md). Use [GitHub private vulnerability reporting](https://github.com/AgentNoobff/VGui/security/advisories/new) for security reports and [SUPPORT.md](SUPPORT.md) for support questions.

VGui is licensed under the [MIT License](LICENSE).

<details>
<summary aria-label="Documentation note">&#8203;</summary>
<small><sub>Some documentation was AI-assisted and may contain minor inaccuracies; please verify details before relying on it.</sub></small>
</details>

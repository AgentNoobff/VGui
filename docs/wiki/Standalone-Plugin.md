# Standalone Plugin

The standalone VGui jar is the recommended runtime model.

## Startup

Velocity reads the filtered `velocity-plugin.json` from the jar. The descriptor identifies `me.agent.vgui.core.VGuiPlatform` as the entry point and records the Maven project version automatically.

The constructor receives:

- `ProxyServer`
- VGui's `PluginContainer`
- SLF4J `Logger`
- plugin data directory

During construction, VGui loads PacketEvents if no API singleton exists. During `ProxyInitializeEvent`, it initializes VGui and registers `/vgui`. During `ProxyShutdownEvent`, it closes sessions and performs owned PacketEvents cleanup.

The standalone artifact embeds the PacketEvents API, Velocity adapter, and common
runtime modules that this bootstrap calls. PacketEvents' own plugin descriptor and
Netty classes are excluded, leaving exactly one Velocity entry point and using the
Netty version supplied by the proxy.

## Plugin id

The runtime id is `vgui`. Dependent plugins must declare it:

```java
@Plugin(id = "consumer", dependencies = @Dependency(id = "vgui"))
```

## Demo command

`/vgui` opens a demonstration menu. The permission is `vgui.demo`.

The demo covers:

- character layouts
- item handlers
- pagination
- back navigation
- scheduled updates
- click sounds
- future-based anvil input

The command is intended for administrators and developers, not as an end-user interface.

## PacketEvents ownership

The standalone jar builds the embedded PacketEvents API using VGui's plugin container
and data directory, and owns that API's lifecycle. Reuse in a shaded integration is
possible only when both integrations intentionally share the same API classes and
class loader. A separately loaded, unrelocated PacketEvents copy is not assumed to be
the same singleton; test any such coexistence before production.

## Data directory

The data directory is passed to PacketEvents when VGui creates the API. VGui itself currently has no user configuration file or persistent menu data.

## Updating

1. Download [the latest stable vgui.jar](https://github.com/AgentNoobff/VGui/releases/latest/download/vgui.jar).
2. Stop the proxy.
3. Replace the old jar.
4. Start the proxy and inspect startup logs.
5. Test menus with the client and backend versions used by the network.

Do not hot-reload proxy plugins. Plugin lifecycle, PacketEvents listeners, and network state require a normal restart.

## Removing

Stop the proxy, remove `vgui.jar`, and remove or update plugins that declare the `vgui` dependency. A dependent plugin will not load successfully without its required runtime dependency.

## Health checks

After startup:

- confirm VGui initialized in logs;
- confirm dependent plugins loaded after VGui;
- run `/vgui` with permission;
- open and close the demo;
- confirm backend inventory remains visually intact;
- test a server switch while a view is open;
- check logs for callback or packet errors.

## Release artifact identity

The download name stays `vgui.jar`. Inside the jar, `velocity-plugin.json` and `META-INF/MANIFEST.MF` contain the actual project version. This allows dynamic latest-release links without losing runtime version diagnostics.

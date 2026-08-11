# Shading VGui

Shading embeds VGui inside another Velocity plugin. Use it only when a shared standalone installation is unsuitable.

## Responsibilities

A shaded integration must:

1. include VGui and its required runtime classes;
2. relocate `me.agent.vgui` to avoid collisions;
3. decide how PacketEvents is supplied;
4. call `VGuiBootstrap.loadPacketEvents` from the plugin constructor;
5. call `VGuiBootstrap.init` during `ProxyInitializeEvent`;
6. call `shutdown` during `ProxyShutdownEvent`;
7. avoid also depending on the standalone `vgui` plugin unless the copies are deliberately isolated.

## Maven dependency

Use normal compile scope when shading:

```xml
<dependency>
    <groupId>com.github.AgentNoobff</groupId>
    <artifactId>VGUI</artifactId>
    <version>main-SNAPSHOT</version>
</dependency>
```

Pin a release tag for production.

## Maven Shade example

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>YOUR_CURRENT_VERSION</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <relocations>
                    <relocation>
                        <pattern>me.agent.vgui</pattern>
                        <shadedPattern>your.plugin.libs.vgui</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Select the current stable Shade plugin release in your own build. The placeholder avoids copying a stale plugin version from documentation.

## Constructor bootstrap

```java
private final boolean managePacketEvents;

@Inject
public MyPlugin(
    ProxyServer server,
    PluginContainer container,
    Logger logger,
    @DataDirectory Path dataDirectory
) {
    this.server = server;
    this.logger = logger;
    this.managePacketEvents = VGuiBootstrap.loadPacketEvents(
        server,
        container,
        logger,
        dataDirectory.resolve("packetevents")
    );
}
```

The constructor timing matters because PacketEvents must load before proxy initialization.

## Initialize and shut down

```java
private VGuiBootstrap vgui;

@Subscribe
public void onInitialize(ProxyInitializeEvent event) {
    vgui = VGuiBootstrap.init(server, this, logger, managePacketEvents);
}

@Subscribe
public void onShutdown(ProxyShutdownEvent event) {
    if (vgui != null) {
        vgui.shutdown();
    }
}
```

Pass the result of `loadPacketEvents` to `init`. It records whether this integration owns the PacketEvents lifecycle.

## PacketEvents choices

The published VGui artifact already contains the PacketEvents API, Velocity adapter,
and common runtime modules, so shading it as-is also embeds those classes. Do not add a
second copy. If an integration deliberately filters them out to use another provider,
it must make the compatible classes visible before `VGuiBootstrap` loads.

Relocating PacketEvents is a separate design decision. A relocated copy cannot share
the ordinary singleton with other plugins, and relocation also changes PacketEvents
types present in VGui's public signatures. Test ownership, linkage, and class loading
carefully.

## Descriptor conflict

When shading VGui, exclude its `velocity-plugin.json` from the final jar so Velocity sees only your plugin descriptor. Configure the shade resource filters accordingly:

```xml
<filters>
    <filter>
        <artifact>com.github.AgentNoobff:VGUI</artifact>
        <excludes>
            <exclude>velocity-plugin.json</exclude>
        </excludes>
    </filter>
</filters>
```

JitPack may expose resolved coordinates differently inside the local dependency graph. Inspect `mvn dependency:tree` and the final jar to confirm the filter matches.

## Verify the shaded jar

- exactly one `velocity-plugin.json` exists;
- VGui classes appear only under the relocated package;
- the dependent plugin loads on a clean Velocity proxy;
- PacketEvents initializes once;
- shutdown removes sessions and listeners;
- no standalone VGui jar is needed;
- a backend switch closes the old menu with `OVERRIDDEN`, and menus reopen cleanly afterward.

Shaded deployment has more failure modes than the standalone plugin. Prefer standalone unless self-containment is a real requirement.

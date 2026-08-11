# Installation

VGui supports a standalone plugin model and a shaded-library model. The standalone model is recommended because one VGui installation can serve multiple Velocity plugins.

## Runtime installation

Download [vgui.jar from the latest release](https://github.com/AgentNoobff/VGui/releases/latest/download/vgui.jar) and place it in `plugins/` on the Velocity proxy.

This jar is self-contained for VGui's PacketEvents usage: it embeds PacketEvents' API,
Velocity adapter, and common runtime modules while relying on Velocity for Netty. A
separate PacketEvents plugin is optional rather than required.

The release workflow publishes three stable asset names:

- `vgui.jar` for the runtime plugin
- `vgui-sources.jar` for source attachment
- `vgui-javadoc.jar` for offline API documentation

The files do not contain a version in their asset name. The plugin descriptor and manifest still record the actual build version.

## Maven dependency

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.AgentNoobff</groupId>
        <artifactId>VGUI</artifactId>
        <version>main-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

For a release build, replace `main-SNAPSHOT` with the tag shown by the [latest release](https://github.com/AgentNoobff/VGui/releases/latest), using `vX.Y.Z` syntax.

`provided` is correct when `vgui.jar` supplies the API at runtime. It prevents Maven from embedding VGui into your plugin jar.

## Gradle Kotlin DSL

```kotlin
repositories {
    maven("https://jitpack.io") {
        content {
            includeGroup("com.github.AgentNoobff")
        }
    }
}

dependencies {
    compileOnly("com.github.AgentNoobff:VGUI:main-SNAPSHOT")
}
```

## Gradle Groovy DSL

```groovy
repositories {
    maven {
        url = uri('https://jitpack.io')
        content {
            includeGroup 'com.github.AgentNoobff'
        }
    }
}

dependencies {
    compileOnly 'com.github.AgentNoobff:VGUI:main-SNAPSHOT'
}
```

Repository content filters prevent Gradle from asking JitPack for unrelated dependencies.

## Dynamic and pinned versions

`main-SNAPSHOT` follows the latest commit on `main`. It is convenient for integration testing and updates when JitPack refreshes the snapshot. It is not a stable contract.

A release tag is immutable and reproducible. Production plugins should pin a release tag, then update it deliberately. Dependabot or Renovate can automate those update pull requests in consumer repositories.

## Velocity dependency declaration

The compile dependency does not control Velocity load order. Declare VGui in the dependent plugin's annotation:

```java
@Plugin(
    id = "myplugin",
    name = "My Plugin",
    dependencies = @Dependency(id = "vgui")
)
public final class MyPlugin {
}
```

## PacketEvents ownership

The standalone release embeds the PacketEvents runtime modules, creates the API with
VGui's plugin container, and terminates it during normal shutdown. The bootstrap can
reuse an API only when a shaded integration deliberately shares the same PacketEvents
classes and class loader. Do not assume that a separately loaded plugin exposes the
same singleton; conflicting unrelocated copies require explicit live validation.

## Offline build

Clone the repository and use the included Maven Wrapper:

```bash
./mvnw clean verify
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
```

The main jar is written to `target/`. Local installation into the Maven cache is available with `./mvnw install`.

## Javadocs and domains

A separate domain is not required. GitHub Pages hosts the generated API documentation at [agentnoobff.github.io/VGui](https://agentnoobff.github.io/VGui/). Pages is configured for this repository with HTTPS, and the workflow publishes from `main`.

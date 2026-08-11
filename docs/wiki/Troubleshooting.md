# Troubleshooting

Start with the first relevant error in the proxy or build log. Remove credentials, private addresses, and unrelated player data before sharing logs.

## VGui is not initialized

Error: `VGui is not initialized yet`.

Cause: the consumer plugin called the API before VGui installed its service.

Fix:

```java
@Plugin(id = "consumer", dependencies = @Dependency(id = "vgui"))
```

For a shaded integration, call `VGuiBootstrap.loadPacketEvents` in the constructor and `VGuiBootstrap.init` during `ProxyInitializeEvent`.

## Plugin does not load

Check that:

- Java 25 or newer runs the proxy;
- the jar contains `velocity-plugin.json`;
- the descriptor `main` points to `VGuiPlatform`;
- the descriptor version is not an unresolved Maven placeholder;
- only one conflicting VGui copy is present;
- PacketEvents dependencies can load.

Inspect locally:

```bash
jar tf vgui.jar
unzip -p vgui.jar velocity-plugin.json
```

## No PacketEvents user

VGui may log that it cannot open a view because no PacketEvents user exists. The player may not be fully connected, PacketEvents may not be initialized, or another plugin may have created an incompatible API state.

Open the view after the player connection lifecycle is ready. Check PacketEvents initialization logs and remove duplicate copies.

## Dependency cannot be resolved

For JitPack:

- add `https://jitpack.io` as a repository;
- use `com.github.AgentNoobff:VGUI` with `main-SNAPSHOT` or a real `vX.Y.Z` tag;
- ensure the tag build succeeds on JitPack;
- refresh Maven or Gradle snapshot caches when using `main-SNAPSHOT`.

## Maven uses the wrong Java

Run:

```bash
./mvnw --version
```

The Java runtime must be 25 or newer. Set `JAVA_HOME` to a JDK 25 or newer installation, not a JRE.

## Build reports an unsupported class-file version

Velocity 4 and VGui target Java 25. Confirm both `JAVA_HOME` and `./mvnw --version` report JDK 25 or newer; changing only the shell's `java` command may leave Maven on an older JDK.

## Tests fail while PacketEvents registries initialize

The tests require the PacketEvents stub plus test-scoped Netty and Adventure NBT dependencies. Do not remove `TestPacketEvents`, `netty-buffer`, or `adventure-nbt` without replacing the registry test bootstrap.

## Click handler does not run

Check:

- the click is inside the active VGui window id;
- the slot contains a `ViewItem` with a handler;
- a global listener is not returning false;
- the click is not inside the cooldown;
- the session was not replaced before dispatch;
- the handler did not throw, as shown in logs.

Item handlers run only for top-inventory slots. Use view-level `onClick` for the player inventory.

## Clicked item snaps back

That is expected with default transaction cancellation. The client predicted a move, then VGui restored its authoritative menu state and cleared the cursor.

Use click handlers to implement actions. Do not treat the GUI as a real movable inventory.

## Player inventory appears wrong after refresh

Check backend inventory packets, server switching, protocol translation, and PacketEvents version. VGui composes the lower inventory from tracked backend state. Capture the exact packet sequence and client version for a reproducible issue.

## Backend menu closes the VGui menu

Expected. A backend window takes ownership and closes the proxy session with `OVERRIDDEN`.

## Back button closes instead of returning

History is empty. Causes include opening with `openReplacing`, closing the earlier session, a backend override, disconnect, or reaching the oldest of the ten retained entries.

## Pagination is empty

Confirm that slots are configured before items, the region character exists in the layout, indexes belong to the top inventory, and the item list is not empty.

## Anvil cancel callback is unexpected

`onCancel` runs for every unconfirmed closure, including `SWITCHED`. Confirmation sets
an internal marker before closing. If custom code closes or replaces the prompt
without using its confirm item, cancellation is expected.

## Javadocs link is unavailable

A separate domain is not required. GitHub Pages is configured for this repository and publishes from the Javadocs workflow. Check the Pages environment and workflow logs if the site is unavailable.

## Workflow remains queued or skipped

- Build runs on pushes to main, pull requests, and manual dispatch.
- Dependency review runs on pull requests.
- Release runs only for tags beginning with `v`.

Check repository Actions permissions, Pages configuration, branch rules, and any environment protection rules.

## Reporting a bug

Include VGui tag or commit, Velocity, Java, PacketEvents, client, and backend versions, deployment model, smallest reproduction, expected result, actual result, and relevant sanitized logs.

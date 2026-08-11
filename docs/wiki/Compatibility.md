# Compatibility

Compatibility has three different meanings: compile compatibility, protocol code paths, and verified runtime behavior. They should not be confused.

## Java

VGui compiles with `--release 25` and requires Java 25 or newer at runtime.

The Maven Enforcer plugin rejects older Java and Maven environments. The GitHub build workflow uses Temurin 25.

## Velocity

The project compiles against Velocity 4.0.0. Velocity 4 requires the Java 25 toolchain, so this baseline no longer runs on Java 21. The exact API version is centralized in `pom.xml`.

Compile success against Velocity does not prove every proxy build behaves identically. Test the exact Velocity release used in production.

## PacketEvents

The exact stable PacketEvents dependency is centralized in `pom.xml` and updated
through Dependabot. The release jar shades the API, Velocity adapter, and common
runtime modules, but excludes Netty and PacketEvents' own Velocity descriptor.

PacketEvents updates can change wrappers, registries, components, and protocol behavior. A successful build is the first check, not the last.

## Minecraft clients

The implementation contains protocol mapping paths for clients from 1.8 through current versions. In particular, it resolves container types by client version and writes modern item components plus legacy NBT.

That statement describes source paths. It is not a claim that every client, backend, and proxy combination has been live-tested.

## Backend servers

VGui does not require a matching plugin on backend servers. It observes backend window and inventory packets at the proxy.

Backend software can still affect packet order, inventory updates, server switching, and window ownership. Validate Paper, Folia, modded, or custom backends separately when they differ from the tested setup.

## Feature limits by protocol

- Old clients ignore modern-only item components.
- Custom model data only works on clients that support it.
- Profile and skull representation differs across protocol eras.
- Modern window state ids do not exist on old protocols.
- Anvil rename behavior and menu identifiers vary by client version.
- Entity-attached sound packets may behave differently across protocol translations.

## Supported deployment combinations

| Combination | Status |
| --- | --- |
| Java 25 and Velocity 4.0.0 | Build target |
| Standalone VGui with internally managed PacketEvents | Supported design |
| Standalone VGui beside a separately loaded PacketEvents plugin | Integration-dependent; class-loader and listener behavior require live validation |
| Relocated shaded VGui with explicit bootstrap | Supported design, integration-owned testing required |
| Unrelocated standalone and shaded copies together | Unsupported due to singleton and class collision risk |
| Transaction pass-through without custom inventory logic | Unsafe and unsupported as a complete inventory solution |

## Required live test matrix

For a release affecting packet logic, check at minimum:

1. oldest claimed client protocol;
2. newest claimed client protocol;
3. current Velocity target;
4. the deployed PacketEvents version;
5. normal backend connection;
6. backend server switch while a view is open;
7. client close and plugin close;
8. shift, number-key, offhand, drop, outside, double, and drag clicks;
9. player inventory preservation during full refresh;
10. anvil typing, confirm, and cancel.

Record the actual combinations tested in release notes. Do not turn compile coverage into a runtime claim.

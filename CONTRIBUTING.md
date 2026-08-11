# Contributing to VGui

Thanks for taking the time to improve VGui.

## Before you start

For bugs and feature requests, check the existing issues first. Packet behavior is version-sensitive, so include the Velocity version, Java version, client version, PacketEvents version, and a short reproduction when reporting a problem.

## Local workflow

1. Fork the repository and create a focused branch.
2. Make the smallest change that addresses the issue.
3. Run `./mvnw clean verify`, or `.\mvnw.cmd clean verify` on Windows.
4. Update public documentation when behavior or compatibility changes.
5. Open a pull request that explains the behavior change and the validation you performed.

Unit tests are preferred for layout, pagination, decoding, and other logic that does not need a live proxy. Packet interception changes also need a manual client and backend check before they are considered complete.

The build requires Java 25. The Maven Wrapper downloads the supported Maven version automatically.

## Code guidelines

- Keep public API changes deliberate. Explain additions, removals, and compatibility impact in the pull request.
- Keep packet and Netty callbacks short. Move blocking work to the Velocity scheduler or an application executor.
- Preserve the existing package structure and Java 25 source level unless the project deliberately changes its support policy.
- Update the relevant Wiki page and public Javadocs when an API or user-visible behavior changes.
- Do not commit `target/`, IDE metadata, credentials, or local proxy configuration.
- Use clear names and comments that describe the current behavior rather than repeating the code.

## Pull requests

Pull requests should have one purpose, a useful title, and a description that covers:

- what changed;
- why it changed;
- how it was checked;
- any client, backend, or Velocity versions that were tested;
- any follow-up work that remains.

## Documentation

The canonical Wiki source is kept in `docs/wiki` in this repository and published to the GitHub Wiki. Keep page links relative so they work in both locations. Public types and methods also need useful Javadoc because the Javadocs site is published from the source on `main`.

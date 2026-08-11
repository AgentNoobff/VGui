# Building and Contributing

## Toolchain

- Java 25
- included Maven Wrapper
- Git

No system Maven installation is required.

## Clone and verify

```bash
git clone https://github.com/AgentNoobff/VGUI.git
cd VGUI
./mvnw clean verify
```

Windows:

```powershell
git clone https://github.com/AgentNoobff/VGUI.git
Set-Location VGUI
.\mvnw.cmd clean verify
```

## Build outputs

The build creates:

- main plugin jar;
- source jar;
- Javadoc jar;
- generated Javadocs under `target/reports/apidocs`;
- Surefire reports under `target/surefire-reports`.

Artifact filenames use the Maven project version. GitHub releases rename copies to stable public asset names.

## Verification performed by CI

The Build workflow:

1. checks out the exact commit;
2. installs Temurin 25 with Maven caching;
3. runs Maven Wrapper with batch mode, transfer suppression, and strict checksums;
4. runs clean compilation, 45 focused tests, packaging, source jar, and Javadoc jar generation;
5. verifies `velocity-plugin.json` and the main public API class exist in the runtime jar;
6. verifies generated Javadocs have an index;
7. uploads all jars as a short-retention workflow artifact;
8. reviews dependency changes on public-repository pull requests.

Jobs have explicit timeouts and redundant runs for the same branch are cancelled.

## Test scope

Focused unit tests cover:

- view types and builders;
- layouts and slot math;
- item builder metadata;
- click decoding;
- context storage;
- pagination math;
- window type resolution;
- window id allocation.

Packet and lifecycle changes still require a live Velocity, backend, and client test. Unit tests cannot prove real protocol behavior.

## Dependency updates

Versions are centralized as POM properties. Dependabot checks Maven and GitHub Actions weekly and groups routine updates.

Stable major dependency updates are reviewed deliberately because they can change the Java baseline or runtime contract. Pre-release dependency lines remain excluded from routine automated updates.

## Documentation source

Wiki pages are stored in `docs/wiki`. The GitHub Wiki repository receives the same files. Edit the canonical source and publish it with the rest of a documentation change.

Public APIs require Javadoc. Behavior changes require an update to the relevant guide, example, compatibility note, or troubleshooting entry.

## Pull request checklist

- keep the change focused;
- explain user-visible behavior;
- run `clean verify`;
- add a regression test where practical;
- update Javadocs and Wiki pages;
- document compatibility impact;
- report live client and backend combinations tested;
- avoid committing generated `target` files, IDE files, or credentials.

## Packet changes

Packet changes need extra review:

- confirm receive and send direction;
- verify cancellation behavior;
- verify old and modern protocol fields;
- test the lower player inventory during resync;
- test backend ownership transitions;
- test connection and shutdown cleanup;
- avoid blocking packet callbacks.

## Local Javadocs

```bash
./mvnw javadoc:javadoc
```

Open `target/reports/apidocs/index.html`.

## Community files

- `CONTRIBUTING.md` contains contribution policy.
- `SECURITY.md` routes vulnerabilities to private reporting.
- `SUPPORT.md` lists information required for help.
- issue and pull request templates collect reproducible context.
- `CODEOWNERS` assigns review ownership.

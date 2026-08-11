# Security

## Report privately

Use [GitHub private vulnerability reporting](https://github.com/AgentNoobff/VGui/security/advisories/new). Do not open a public issue for a vulnerability.

Include:

- affected release or commit;
- impact and realistic attacker capability;
- exact Velocity, Java, PacketEvents, client, and backend versions;
- minimal reproduction;
- packet capture or logs when relevant, with private data removed;
- proposed mitigation if available.

## Security boundaries

VGui controls a client-side container representation at the proxy. It is not an authentication system, secure text-entry system, or authoritative backend inventory store.

Default transaction cancellation is an important safety boundary. It prevents menu clicks from becoming ordinary backend inventory transactions and restores the proxy-owned state.

Disabling cancellation transfers responsibility to the integration. A secure implementation must account for click modes, cursor state, hotbar swaps, offhand swaps, drag phases, state ids, replay, packet order, and backend ownership.

## Handler trust

VGui runs plugin-provided callbacks. A handler can access the player and plugin services and can block a network thread. Install only trusted plugins and review global listeners that can observe or veto all VGui clicks.

## Input handling

Anvil text is untrusted player input. Validate length, characters, authorization, and application state before using it in commands, database queries, filenames, logs, or external APIs.

Use parameterized database queries. Do not treat client-side validation as authoritative.

Never collect passwords, API keys, one-time codes, or other secrets through a Minecraft inventory interface.

## Dependency security

Dependencies and GitHub Actions are monitored by Dependabot. Public pull requests also receive dependency review for moderate or higher known vulnerabilities.

Updates still require compatibility review. Automatically selecting an incompatible major version can be less secure than applying a supported fix carefully.

## Data and privacy

VGui stores in-memory player UUID session keys, view context values chosen by consuming plugins, and a visual cache of 36 inventory slots. Core VGui does not persist player profiles or send analytics.

Consumer plugins remain responsible for what they place in context, log from listeners, or send to external systems. Store only the state required for the menu and remove sensitive values promptly.

## Denial of service considerations

- keep packet callbacks nonblocking;
- use click cooldowns;
- bound result lists and context size;
- avoid very fast repeating updates;
- cancel obsolete asynchronous work;
- validate item text and external data sizes;
- monitor callback exceptions and scheduler backlog.

## Release response

Security fixes are applied to the current release line. Disclosure timing depends on severity, reproducibility, protocol compatibility, and the time required for downstream users to update.

# Architecture

## Invariants
1. Memory first: visible screens own transient resources; no unbounded caches.
2. Local first: Find renders local results independently of network state.
3. Private by default: AI context sharing is opt-in; query-only is the future default.
4. Minimal surface: one app module, eight production Kotlin files, two runtime dependencies.
5. No background work: no services, receivers, polling, or extra processes.
6. User-owned infrastructure: no bundled keys, telemetry, accounts, or hosted backend.
7. Confirmed actions: only four whitelisted action types; user confirmation precedes execution.

## Runtime flow

`MainActivity -> HomeScreen -> Find`

Find discovers launchable activities using `PackageManager`, matches locally, and ranks exact/prefix/word-prefix/substring matches. Usage counts are local preferences and only influence ranking.

Think is not connected in this phase. `AiClient` is an interface placeholder. `AiActions` contains preliminary typed validation, not a complete JSON parser or dispatcher.

## Lifecycle

`onStart`: refresh launchable app index and render current query.

`onStop`: clear screen-owned result collections and the activity's app index.

No background services, receivers, providers, or worker jobs are declared.

## Manifest

The manifest declares INTERNET, a MAIN+LAUNCHER queries entry, and one HOME/DEFAULT/MAIN activity. INTERNET is reserved for the future Think feature. No QUERY_ALL_PACKAGES permission is used.

## Source map

- MainActivity.kt: HOME activity and app launch
- HomeScreen.kt: native UI and RecyclerView
- Find.kt: discovery and ranking
- Think.kt: future Think boundary
- AiClient.kt: future network boundary
- AiActions.kt: action allowlist and preliminary validation
- Settings.kt: local learned usage counts
- Memory.kt: memory-management boundary

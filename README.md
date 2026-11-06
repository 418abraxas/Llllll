# Find / Think Launcher

A minimal, local-first Android launcher. Find searches installed apps without a network dependency. Think is an optional, user-configured AI interface planned for Phase 3.

## Current state

Phase 0/1 starter implementation:
- HOME activity and minimal native Views interface
- Local launcher-app discovery
- Local matching and learned launch-count ranking
- App launch hand-off
- Initial scorer and action-validation unit tests

Think networking, streaming, payload inspection, widgets, and final memory measurements are not implemented yet.

## Build

Open this directory in Android Studio, allow Gradle sync, then run `app`.

Or, with a compatible Gradle installation and Android SDK:

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

The project uses JDK 17, compile SDK 35, and min SDK 26.

## Important

The PSS targets are acceptance criteria, not measured results. This starter has not been device-tested. The API key and AI endpoint features are not implemented.

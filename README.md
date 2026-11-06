# Find / Think

Minimal Android launcher: one input line, local app search (Find), optional AI chat with four confirmed actions (Think).

## Build in Codespaces

```bash
bash setup.sh
./gradlew testDebugUnitTest assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Download it, copy to your phone, install, then Settings > Apps > Default apps > Home app.

Release: `./gradlew assembleRelease` (R8 full mode; signed with the debug key for personal testing only).

## Files (8)

MainActivity, HomeScreen (UI + widgets), Find (index, scorer, list), Think (chat), AiClient (HTTP/SSE), AiActions (parse + dispatch), Settings (prefs + Keystore vault), Memory (icon cache).

## Known deviations and unverified items

- Manifest adds `com.android.alarm.permission.SET_ALARM`, required by the timer intent.
- Default payload = query + a fixed instruction (so the model knows the action format). No user data beyond the query.
- Widget picking/binding and long-press removal are untested on any device.
- No memory (PSS) numbers measured. Never compiled by the author of this scaffold: expect to fix small compile errors on first build.

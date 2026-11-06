# Implementation status

## Phase 0 — Foundation
- [x] Single app module
- [x] HOME activity
- [x] Native Views
- [x] One input field
- [ ] Device verification
- [ ] Baseline PSS measurement

## Phase 1 — Find
- [x] PackageManager launcher discovery
- [x] Local matching
- [x] Learned launch-count ranking
- [x] Application launch
- [x] Play Store search hand-off (market URI with HTTPS fallback)
- [ ] Low-end device latency benchmark
- [ ] OEM compatibility verification

## Required next work
1. Build and run ScorerTest and AiParseTest.
2. Install on a stated device and verify HOME selection.
3. Measure home idle and Find-open PSS with `dumpsys meminfo`.
4. Add Play Store search hand-off.
5. Review lifecycle cleanup with Activity recreation and app switching.

## Not yet implemented
Think streaming, BYOK settings, exact payload inspector, complete JSON action parser, confirmation/dispatch UI, widgets, release R8 verification, and device measurements.

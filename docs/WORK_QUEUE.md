# WORK QUEUE — ZIMT Lab 1 Garten & Küche
Single canonical queue; inspect latest source/CI before marking VERIFIED.

| ID | Goal / acceptance criterion | Priority / dependencies | Status | Verification | Evidence / blocker / next action |
|---|---|---|---|---|---|
| L1-001 | AC-L1-001: changing contour never silently deletes pigment; user can preview and reject | P0 / none | IMPLEMENTED | UNVERIFIED | Build 12 reopens a pending contour preview for a matching saved draft. Snapshot regression compared working strokes and restored pigment after rejection in CI 45497143381 at 850605d. The APK manifest and v2 signature were checked for de.naehe.app.gartenkueche.lab12. Build 13 adds Android instrumentation for activity recreation, acceptance and rejection with on-disk readback. Check emulator results and real-device rotation. |
| L1-002 | AC-L1-002: undo/redo remains correct after brush, fill, erase and contour changes | P1 / L1-001 | IMPLEMENTED | UNVERIFIED | Mixed pen, fill, erase and contour history now restores serialized pixel and stroke state at every undo/redo step. Local regression reproduced the lost point count before the fix. Verify the same sequence across the actual editor modes on device. |
| L1-003 | AC-L1-003: interrupted editor and process restart preserve latest draft | P0 / persistence | IMPLEMENTED | UNVERIFIED | Local Java round-trip retained pending review and pigment; a truncated GZIP raised a load error. Verify rotation, process recreation, and replacement confirmation with an existing Lab 10 drawing on Android. |
| L1-004 | AC-L1-004: multi-touch zoom/pan does not corrupt drawing or consume unintended input | P1 / none | READY | UNVERIFIED | Add touch cancellation/slop tests and physical checks |
| L1-005 | AC-L1-005: independently installable API23+ build and portable editor logic | P2 / CI | IMPLEMENTED | CI_VERIFIED | CI 45285575943 at 5f65771 passed Java regressions, Android 6+ debug build and package check; build 13 binds launcher and home labels to the build version; verify manifest and side-by-side install. Portable integration remains open |

## Coverage
All remaining ACTIVE_SCOPE criteria and discovered defects must map to a queue item or justified BLOCKED/DEFERRED; continue after each slice.

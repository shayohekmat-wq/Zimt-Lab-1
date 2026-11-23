# WORK QUEUE — ZIMT Lab 1 Garten & Küche
Single canonical queue; inspect latest source/CI before marking VERIFIED.

| ID | Goal / acceptance criterion | Priority / dependencies | Status | Verification | Evidence / blocker / next action |
|---|---|---|---|---|---|
| L1-001 | AC-L1-001: changing contour never silently deletes pigment; user can preview and reject | P0 / none | READY | UNVERIFIED | Inspect contour-fill pipeline and add regression tests |
| L1-002 | AC-L1-002: undo/redo remains correct after brush, fill, erase and contour changes | P1 / L1-001 | READY | UNVERIFIED | Test sparse pixel diffs and mixed-operation history |
| L1-003 | AC-L1-003: interrupted editor and process restart preserve latest draft | P0 / persistence | READY | UNVERIFIED | Inspect AtomicFile/GZIP snapshot and lifecycle failure tests |
| L1-004 | AC-L1-004: multi-touch zoom/pan does not corrupt drawing or consume unintended input | P1 / none | READY | UNVERIFIED | Add touch cancellation/slop tests and physical checks |
| L1-005 | AC-L1-005: independently installable API23+ build and portable editor logic | P2 / CI | READY | UNVERIFIED | Verify current Gradle, build and test evidence, no Compose rewrite |

## Coverage
All remaining ACTIVE_SCOPE criteria and discovered defects must map to a queue item or justified BLOCKED/DEFERRED; continue after each slice.

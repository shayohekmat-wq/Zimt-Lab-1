# Lab 1 architecture — existing implementation and safety boundary

## Current source of truth
The current `README.md` describes an Android API 23+ Java/Canvas standalone atelier. This document records the intended responsibilities, not an assertion that all boundaries are already enforced in code.

## State and data
- **Input:** touch gestures and tool selection translate to one authoritative editing operation. Pan/pinch, stroke, fill and contour confirmation must not simultaneously own the same pointer stream.
- **Domain:** per-stage pixels, outlines, pigment, computed fill masks, evolution-stage metadata and history are separate concepts. A recalculated mask must never erase original pigment.
- **Transactions:** fill/contour changes are previewable and either accepted as one change or rolled back; a failed recompute leaves the last valid image intact.
- **Persistence:** existing AtomicFile/GZIP snapshots save the latest draft; current session undo history is not guaranteed after process restart. Preserve snapshot schema and test recovery before changing serialization.
- **UI:** Canvas renders from editor state; visual animation and haptic effects must not determine whether a draft is durably saved.
- **Portability:** extract stable stage/drawing models and versioned export/import only as integration becomes concrete. A separate app ID cannot access private files of another app ID.

## Verification boundaries
Unit tests for pixel-diff undo/redo, fill rollback and contour mask; device checks for touch arbitration, process interruption and memory/latency; signed update checks for installed drawing preservation. Record PASS/FAIL/UNVERIFIED in QUALITY_EVIDENCE.md.

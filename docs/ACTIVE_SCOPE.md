# ACTIVE SCOPE — ZIMT Lab 1: Garten & Küche

## Authority
Latest explicit user requirements override this file. Current source and verified evidence determine implementation status; `README.md` and `BUILD_PLAN_0.0.9_ATELIER_NONDESTRUCTIVE.md` document the current editor and historical design. Do not mistake old feature descriptions for unimplemented work.

## Current product
Standalone Android 6.0+ (API 23) Java/Canvas drawing and plant-evolution atelier, build 0.0.16, app ID `de.naehe.app.gartenkueche.lab16`. It must remain independently installable and later portable into ZIMT without requiring a Compose rewrite.

## Invariants
- Preserve each evolution-stage drawing, outline, pigment, mask and stage metadata; keep stable identity and atomic snapshot persistence.
- Undo/redo must not silently discard earlier painting when changing outline, layer or stage. Existing session-level undo history and process-restart snapshot behavior must be described accurately; cross-process undo is NOT yet guaranteed.
- Treat fill/recompute/outline confirmation as an atomic decision. If a new contour could obscure paint, keep prior data and offer a reversible choice.
- Preserve accessibility, two-finger pan/pinch, small displays, selected-tool clarity, and responsiveness on Moto G6; profile before broad optimization.
- No automatic data import from earlier separate app IDs is currently implemented. Future integration needs a tested versioned export/import path.

## Next risk-based acceptance gates
1. **Safety critical:** reproducible fill, contour, stage-switch, interruption, and persistence/restore tests, including a rejected contour that must not destroy prior pigment.
2. **Feature:** visual/haptic editor acceptance on Moto G6 and Nothing Phone 3a; verify scroll/pan/draw conflicts and undo/redo.
3. **Release:** exact-commit build and tests, signed package identity and upgrade-with-existing-drawing evidence before claiming safe update.
4. **Portability:** separate drawing domain/state from Activity/Canvas adapters only where real integration requires it.

No global feature freeze. New design and interaction improvements may proceed independently, with their own relevant gates.

## Doctrine 7.0 — Scope und Akzeptanzabdeckung
Die Priorisierung begrenzt nicht die Anzahl der autorisierten Editor-Funktionen und Folgearbeiten. `docs/WORK_QUEUE.md` bildet die vorläufigen Kriterien AC-L1-001 bis AC-L1-005 ab; weitere unerfüllte Kriterien und Bugs bei Code-/Testprüfung ergänzen. Keine pauschale Feature-Sperre oder vorzeitige Fertigmeldung nach einem Slice.

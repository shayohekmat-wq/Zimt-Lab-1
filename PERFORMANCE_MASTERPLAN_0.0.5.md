# Garten & Küche — Performance-Offensive (Plan für 0.0.5)

**Stand:** 20.11.2026 · **Status:** Code-Audit und geplanter Umbau, kein bereits implementierter Fix.  
**Ziele:** ANR-Ursachen beseitigen, flüssige Touch-/Zeicheninteraktion, langlebige Pflanzendaten, Android 6+ (minSdk 23), ohne Änderungen an ZIMT.

## Ausgangslage

Der Nutzer meldet starkes Ruckeln und ein Android-Systemdialog „Garten & Küche 0.0.4 reagiert nicht“ im Beet nach erfolgreicher Pflanzenerstellung (ANR). Ein Screenshot allein zeigt **keinen ANR-Stacktrace**; die tatsächliche Blockade muss am Gerät oder mit Traces verifiziert werden. Die Code-Analyse ergibt mehrere konkrete, vorrangige Verdachtsmomente.

## Verifizierte Code-Funde und Priorität

### P0: Main-Thread-Blockaden und zu viel Schreibarbeit

- \`BeetActivity.onResume() -> reload() -> PlantStore.loadPlants()\`: synchroner GZIP-Read/Parse sämtlicher Pflanzen und sämtlicher Stufen auf dem UI-Thread.
- \`PlantArt.read()\`: ruft \`validate()\` pro geladenem Konturbild auf, unter anderem beim Öffnen einer Pflanze. \`validate()\` führt mehrfache 300×300-Raster-Scans aus.
- \`BeetActivity.reload()\` ruft \`CareEngine.evaluate()\` auf und speichert bei \`changed\` sofort mit \`save(plant)\`. In \`CareEngine.evaluate()\` wird \`lastEvaluatedAtUtc\` bei abweichender aktueller Zeit überschrieben und damit \`changed = true\`, auch wenn weder Stufe noch Pflegefenster gewechselt sind. Somit wird oft bei **jedem Resume** die komplette Pflanze geschrieben.
- \`BeetActivity.save()\` ruft \`PlantStore.savePlant()\` synchron auf dem UI-Thread auf (u. a. Drag-Ende und 5s-Gießaktion). \`savePlant()\` serialisiert nicht nur die kleinen Statuswerte, sondern **alle Stufen + Todesgrafik**, per \`DataOutputStream\` pixelweise und mit GZIP-Kompression.
- \`PlantStore.write()\` hat eine riskante Fallback-Sequenz: bei Rename-Fehler erst das bestehende Ziel löschen, dann temporäre Datei umbenennen. Bei Abbruch nach dem Löschen droht Datenverlust. Ersetzen durch Android \`AtomicFile\` (API-kompatibel) oder verlässlichen Journaling-/SQLite-Transaktionspfad; niemals Original löschen, bevor ein konsistenter neuer Zustand abgesichert ist.

### P0/P1: Editor zeichnet und kopiert zu viel

- \`EditorActivity.PixelCanvas.onDraw()\` bei \`dirty\`: \`PlantArt.bitmap()\` scannt **alle 90.000 Pixel**, erzeugt ein neues \`int[90000]\` und ein neues ARGB-Bitmap, anschließend wird das alte Bitmap \`recycle()\`d. Bei jeder Bewegung entsteht unnötiger CPU-, Speicher- und GC-Druck.
- \`EditorActivity.history()\` kopiert bei jedem Strich eine komplette \`PlantArt\` (Kontur + Farben + Innenmaske) und hält bis 12 Undo-Snapshots.
- \`EditorActivity.persistDraft()\` schreibt nach Touch-Release debounced 750 ms, aber **synchron auf dem UI-Thread**, und serialisiert bei mehrstufigen Pflanzen jedes Mal alle vorherigen fertiggestellten Zeichnungen neu.
- \`ContourDetector.detect()\`: versuchsweise Verbindungen von Endpunkten; im ungünstigen Fall O(T² × 90.000) wegen erneuter Flood-Fill-Prüfung pro Endpunktpaar, trotz T<=160. Auf großen Strichsammlungen kann das sehr lange dauern.
- \`EditorActivity.buildScreen()\` erstellt beim Wechsel mehrfach die komplette Oberfläche und Bitmaps neu; Dialog-/Navigationswechsel vereinfachen und nicht mehr bei jeder Kleinigkeit den Baum neu aufbauen.

### P1: Beet rendert Arbeit, die sich gar nicht ändert

- \`BeetCanvas.onDraw()\` zeichnet bei jedem Input das gesamte 800×600-Feld, ca. 70 Rasterlinien sowie alle Pflanzensprites. Raster und Boden sind dabei statisch.
- Jede Pflanze wird mit einem ungekürzten transparenten 300×300-Sprite gerendert; bei kleinen Pflanzen sind große Flächen unnötig transparent.
- Sprite-Cache enthält verschiedene Stufen gleichzeitig ohne definiertes Speicherbudget. Alte/unsichtbare Bilder werden nicht gezielt entfernt. Ein Spritesheet muss nicht verwendet werden: sinnvoll sind **stufenspezifische, begrenzte LRU-Caches und Vorberechnung bei Bedarf**.
- Das \`postInvalidateDelayed(80)\` während eines Gießens redrew zusätzlich das komplette Beet für einen kleinen Fortschrittsbalken.
- \`onDraw()\` verwendet befristete \`Paint\`-/Zeichenobjekte, aber das Hauptproblem ist nicht ein einzelner \`drawLine\`, sondern I/O, Bitmap-Rekonstruktion und große invalidierte Bereiche.

### P1: Pflege-Engine und Historie entkoppeln

- \`CareEngine.evaluate()\` ist synchron bei Resume; bis zu 2048 Fenster pro Pflanze, Event-Strings, bei vielen Pflanzen potenziell belastend.
- Gießereignisse sind im vollständigen \`Plant\` gespeichert; jeder Speichervorgang serialisiert die vollständige Historie mit. Historie stattdessen append-only, indiziert und ggf. archiviert.
- \`lastEvaluatedAtUtc\` nur bei tatsächlicher Änderung der Pflegezustände als persistierbarer Wert behandeln, nicht als Anlass für bilddatenintensive Full-Write.

## Mehrschichtige Zielarchitektur

### Schicht A — UI-Thread zu 99 % reine Eingabe/Darstellung

- **Single-Thread-IO-Executor** für Ladevorgänge/Serialisierung/DB-Writes. Main Thread nur kleine, bereits vorbereitete Zustände, Canvas, Touch-Events.
- \`BeetActivity\` sofort zeichnen mit kleinem Skeleton/Progress; definierte Datensätze asynchron laden; batched UI-Anzeige via Main-Looper. Auf Resume keine komplette Datei neu laden, wenn In-Memory-Status noch gültig.
- Pflege-Simulation beim Laden/offline-Nachholen auf eigenem Worker; danach kompaktes Ergebnis atomar als Statusänderung anwenden. Simulationsreihenfolge und idempotente Event-IDs beibehalten. Synchronisation und Kopien so gestalten, dass während Zeichnen keine Objektgraphen mutiert werden.
- Kein \`runOnUiThread\` für teure Arbeiten; keine \`Thread.sleep\`-Quickfixes, keine pauschale Anhebung von Thread-Prioritäten.
- Ein dedizierter \`PlantRepository\` mit \`loadMetadata\`, \`loadArtwork(definitionId, stage)\`, \`saveInstanceState\`, \`appendWaterEvent\`, \`saveDraftDelta\`. Belastungsprofil über Zeitmessungen.

### Schicht B — Status und Bilddaten trennen (größter struktureller Gewinn)

- Eine immutable \`PlantDefinition\`: Palette, Stufen, Konturen, Todesgrafik. **Einmal speichern**, bei Gießen und Verschieben nicht erneut komprimieren.
- Pro platzierter Pflanze eine schlanke \`PlantInstance\`: UUID, Pflanzen-ID, Beet-Koordinate, Stufe, Pflegefenster, Strafpunkte, Zeitstempel. Ein Positionswechsel schreibt nur diese wenigen Bytes.
- \`WaterEvent\` in eigener append-only-Tabelle/Datei statt immer größerer GZIP-Vollkopie. Eindeutige Action-IDs verhindern Doppelzählung.
- SQLite \`SQLiteOpenHelper\` (API23) mit Transaktionen/WAL als langfristige persistente Quelle für Instanz/Events. Artwork als komprimierte, versionierte immutable Dateien oder BLOBs; Palette 40 -> Indexpixel 1 Byte je Pixel, Kontur als Bitset -> **11.250 Byte** je 300×300 Maske vor Komprimierung. Eine alte unkomprimierte 300×300-ARGB-Bitmap benötigt **360.000 Byte**.
- **Keine Datenmigration durch Löschen/Neuinstallation**: alte \`plants_v2\`-GZIPs zunächst read-only erkennen und einmalig im IO-Worker migrieren, Validität/Checksummen prüfen, erst dann „Migration erfolgreich“ markieren. Save-Backup bewahren.
- Vorsicht: Jede Lab-Version besitzt für parallele Installation eine neue Android-\`applicationId\` und daher **andere Sandbox**. Daraus lassen sich Daten nicht automatisch übernehmen; für zukünftige Releases expliziten Export/Import oder Android-eigenen Transferpfad vorsehen. Bestehende 0.0.4-Daten nicht durch den neuen Code unsichtbar machen.

### Schicht C — Renderer: weniger Arbeit pro Frame

- 800×600-Logikfeld unverändert. Boden/Raster einmal als statische 800×600-Textur erzeugen, im normalen \`onDraw\` nur \`drawBitmap\`.
- Pflanzenbilder der jeweils aktiven Evolutionsstufe einmalig rasterisieren, bis Stufe/Artwork sich ändert. Nearest-neighbor und keine permanenten Re-Farbfilter.
- Transparente Ränder per Bounding-Rect/Bounding-Box trimmen; Sprite-Anker/Beet-Koordinate und Shape-Hitbox erhalten. Bei vielen Pflanzen zunächst nur sichtbare Sprites zeichnen (Viewport-Culling).
- Gießkanne/Fortschrittsbalken auf getrennte Overlay-View oder schmale invalidierte Region auslagern; nicht 800×600 pro Timer-Tick komplett neu rendern.
- UI-Updates an Choreographer/Framebegrenzung koppeln: maximal ein Draw pro Display-Frame, nicht pro Move-Sample; Zeit fortlaufend monotonic für 5s-Gießen. Drosselung nicht auf Kosten exakter Touch-Koordinaten.
- \`Paint\`, \`Path\`, \`Rect\` und Raster-Texturen vorhalten; keine kurzlebigen großen Objekte in \`onDraw\`.
- Ein **LRU-Bitmap-Cache nach Bytebudget**, mit sauberem Besitz-/Evictionmodell und keinem Recycling von Bitmaps, die möglicherweise gerade noch in Renderbefehlen benutzt werden. Auf \`onTrimMemory\` Cache verkleinern.

### Schicht D — Editor: tatsächliche Pixeländerung statt Komplett-Neuerstellung

- Eine persistente 300×300-ARGB-Bitmap je aktivem Layer; Striche direkt in die zugehörigen Indexpixel/Pixelregion schreiben. Nur „dirty rectangles“ via \`setPixels\`-Teilbereich/Bitmap-Canvas aktualisieren.
- Während des Zeichnens **keine komplette neue Bitmap anlegen oder alte recyceln**. Temporäre Stroke-Buffer und Shadow-Diffs verwenden.
- Undo/Redo als Pixel-Diff je Stroke (\`index, previous, new\`) statt 90k-Vollkopie; begrenztes Bytebudget und ggf. sporadische Checkpoints.
- Autosave inkrementell und asynchron, debounce 1–2s nach Änderung plus Save-Queue. Beim App-Verlassen möglichst kleinen Zustand sofort in Queue schreiben; kritische Entwürfe mit WAL/journalbasiertem Durable-Commit absichern.
- Konturerkennung nur nach „Weiter“, nie pro Frame. Direkter 300×300-Floodfill; kleine Lücken algorithmisch gezielt verbinden (nur nächste Kandidaten, räumliche Hash-/Raster-Nachbarschaft), **nicht quadratisch dutzende vollständige Floodfills**.
- Prüfergebnis visualisieren und Vorschau anbieten, wenn Linien bei Erkennung abgeschnitten/geheilt werden. Vorläufige Vorschau statt stiller Datenänderung.
- Vollständig fertige Evolutionsstufen als immutable, gecachte Bilder; Overlay im Editor 20 % per Paint Alpha 51, nicht in Pixelset kopieren.

### Schicht E — Regel-Engine, Zeit und Zuverlässigkeit

- \`CareEngine\` rein Java und ZIMT-kompatibel lassen. Snapshot + kleine Append-Events; schon evaluierte Fenster nicht neu berechnen.
- Keine Timer/Services für die Langzeitentwicklung; anhand persistierter UTC-Timestamps beim Öffnen und Ereignisverarbeitung nachvollziehen.
- Offline-Nachholen nur auf Worker, mit Fortschrittskontrolle und Abbruch/Konsistenzcheck; noch laufende UI weiter bedienbar.
- Gießevent nach 5.000ms genau einmal registrieren, UI quittiert vor/parallel zur kurzen dauerhaften Speicherung, aber bei Save-Fehler eindeutige erneute Zustellung statt falscher dauerhafter Erfolgsmeldung.
- Im Fehlerfall **nicht still Pflanzendatei überschreiben oder Pflanze löschen**. Recovery-Dateien, CRC/Checksummen, lesbarer Fehler- und Exportpfad.

### Schicht F — Android-6-/Moto-G6-Fallback

- Java/Android-View/Canvas unverändert, minSdk23, keine neue 3D-Engine/Compose-Abhängigkeit.
- Runtime-Profile **Standard** und **sparsam** anhand tatsächlicher Geräteleistung/Frametime, nicht bloß Android-Version. Standard: Ziel 60fps bei aktiver Interaktion; schwächeres Gerät: stabile 30fps, geringere Zusatzanimationen/kleinerer Sprite-Cache, inaktiver Bildschirm fast 0 Updates.
- Die logischen **300×300 und 800×600** bleiben in beiden Profilen exakt gleich: Sparmodus darf niemals Pixeldaten/Spielregeln verändern.
- Keine Netzwerkberechtigungen oder neuen Servervoraussetzungen. Grafik möglichst hardwarebeschleunigt; Umschalten auf Software nur für belegte Spezialfälle nach Messung.

### Schicht G — Diagnose, Messwerte, Tests

**Nicht raten**: 
- In Debug-Builds Android \`StrictMode\` für Disk I/O auf Main Thread, \`Trace.beginSection()\` around \`loadPlants\`, \`evaluate\`, \`savePlant\`, \`validate\`, \`onDraw\`, \`bitmap\`.
- Auf Android10+ Perfetto/Android-Studio-System-Trace inkl. UI/Main, RenderThread, I/O und Garbage Collector; Android6–9: \`adb shell dumpsys gfxinfo <package> framestats\`, Android CPU Profiler, Logcat, ANR-Traces. Analysiere **exakten** Hauptthread-Stack.
- Benchmark 1/5/20/50 Pflanzen und 1/5/10 Stufen, 300×300-Editor mit 100 kontinuierlichen Strichen, wiederholte Gießbewegung, mehrfaches Öffnen, 30 Tage Offline-Catch-up, 5-Minuten-Drag, 20× Undo/Redo, 50 Wiederaufnahmen.
- Automatisierte Regressionstests für alle vier Pflegemodi, eindeutige Event-IDs, Formatversionen, pixelgenauen JSON/Art-Rundtrip und Stufe1-Tod.
- Nach jeder Verbesserung Frametime p50/p95/p99, GC, App-Speicher (Java/native/GPU), ANR-Zahl, Speicherdauer und Warm-/Cold-Start im Vorher/Nachher-Vergleich dokumentieren.

## Performance-Abnahmeziele (Ziele, KEINE bisher gemessenen Werte)

- **Null ANRs** in einem definierten Langzeittest mit großer Pflanzenbibliothek und mehrfacher Resume-/Gießbelastung.
- Nach Warm-Start ist Beet ohne blockierendes Nachladen bedienbar; Ziel < 300 ms auf aktuellen Geräten, als Geräte-SLO kein Versprechen.
- **Keine GZIP-/Bilddaten-Lese- oder Schreibvorgänge auf dem UI-Thread**.
- Editor und Beet: auf ausreichend leistungsfähigen Geräten 60fps-Ziel (16,7ms/Frame), auf Moto G6 30fps-Ziel (33,3ms/Frame). p95/P99 Frametimes mit realen Tests ermitteln.
- Gießen genau einmal bei >=5000ms, auch wenn UI-Framerate abfällt; kein Doppelzählen bei Wiederaufnahme.
- Bei unveränderter Pflanzengrafik kein erneutes Rendern/Komprimieren ihrer Evolutionsbitmaps beim Verschieben/Gießen/Resume.
- Pflanzen und Entwürfe nach Process Kill und APK-Update unverändert wiederherstellbar.
- Keine Regression des Farb-/Kontur-/Overlay-Workflows aus 0.0.4.

## Reihenfolge der Umsetzung

**Phase 0: Sofortige Sicherung** – aktuelle Daten unverändert lassen, Version 0.0.4 als reproduzierbare Referenz; Backups/Formatkonverter vorbereiten. Bei künftigen parallelen APKs ist der Transfer nicht automatisch möglich.

**Phase 1: ANR-Hotfix** – alle GZIP-Lade-/Schreiboperationen aus Main Thread verlagern; kein \`savePlant\` bei bloßer \`lastEvaluatedAtUtc\`-Änderung; \`PlantArt.read\` nicht jedes Mal vollständig validieren; konturbezogene Worst-Case-Arbeit vom Main Thread entkoppeln. Erst wenn dieses Gate grün ist, weiter.

**Phase 2: Datenstruktur** – Definition/Instanz trennen, inkrementelle Zustandsupdates, Eventjournal, atomare Sicherung, v3→v4-Migration mit Altformat-Leser und reversibler Sicherung.

**Phase 3: Renderer/Editor** – dauerhafte Bitmap statt Bitmap-Neubau je Frame; statische Beet-Hintergrundtextur, Dirty-Rects, Pixel-Diff-Undo, bounded LRU-Caches, Konturdetektor optimieren.

**Phase 4: Performance-QA** – CPU/Heap/Frametimes messen und im CI vergleichen; Android6/API23, Moto G6/API26/28 und Nothing Phone aktuelles Android. Reale Geräte erforderlich für belastbare Framerate-Aussagen.

**Phase 5: Release** – **Garten & Küche 0.0.5** mit eindeutiger \`applicationId de.naehe.app.gartenkueche.lab5\`, neuer App-Name, direkt herunterladbare \`.apk\` in Chat (kein ZIP als einziger Benutzerweg). Erfolgreicher CI-Build ist Mindestvoraussetzung, echte UI-/Gerätetests bleiben zusätzlicher Schritt.

## Was wir bewusst NICHT tun

- Kein Wechsel zu Unity, Flutter, Compose oder Kotlin allein als vermeintliche Beschleunigung.
- Nicht das 300×300- oder 800×600-Logikraster verkleinern.
- Kein pauschales Herabsetzen auf 15fps, um einen UI-Thread-Stall zu verbergen.
- Kein Löschen und Neuinstallieren als „Fix“, das Pflanzen und Entwürfe zerstört.
- Kein Einsatz von Firebase oder Hintergrundservices zum Ausweichen vor lokalen Performancefehlern.
- Keine Behauptung, die genaue ANR-Ursache sei ohne Trace bereits zweifelsfrei bewiesen.

## Verifizierte Quellen

- GitHub: \`app/src/main/java/de/naehe/gartenkueche/{BeetActivity,EditorActivity,PlantStore,PlantArt,ContourDetector,CareEngine}.java\`.
- Android Entwicklerdokumentation: https://developer.android.com/reference/android/os/StrictMode
- https://developer.android.com/develop/ui/views/layout/custom-views/optimizing-view
- https://developer.android.com/topic/performance/anrs/diagnose-and-fix-anrs
- https://developer.android.com/topic/performance/tracing

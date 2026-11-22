# Garten & Küche – Master-Build-Plan 0.0.9 „Atelier“
**Stand:** 22. November 2026 · **Status:** verbindlicher Design-/Technikplan, noch kein implementierter Release · **Repo:** shayohekmat-wq/Zimt-Lab-1  
**Primärziel:** Freude am Zeichnen, zuverlässige nicht-destruktive Abläufe, ästhetische Bedienung, stabile Performance auf Android 6+.

> Nutzerentscheidung: **Version 0.0.9 ist wieder separat installierbar.** Keine Update-/Import-/Migrationsfunktion zwischen bisherigen Lab-Versionen in diesem Build. Die alte Installation und ihre Daten bleiben unberührt. Für die neue App \`de.naehe.app.gartenkueche.lab9\`, eigener Name „Garten & Küche 0.0.9“. Die 1–80×-Zeitrafferfunktion, die 5 echten Sekunden Gießdauer, das 800×600-Beet, 1–10 Evolutionsstufen, Graustufen-/eigene Todesform und das Pixel-Cropping müssen bestehen bleiben.

## A. UX-Leitprinzipien – was wir erreichen wollen

1. **Nichts verschwindet stillschweigend:** Kontur- und Farbebene behalten auch bei Änderungen ihren ursprünglichen Inhalt. Die Anzeige kann Farben verdecken, der gespeicherte Pixelwert geht dabei nicht verloren.
2. **Echte Bewegungsfreiheit:** Navigation (Zurück/Weiter/zu Stufe X) und Verlauf (Rückgängig/Wiederholen) sind zwei getrennte Funktionen. Werkzeuge oder Modi wechseln ohne Verlust der bereits gemalten Ebenen und ohne unerwartetes Zurücksetzen des Undo-Verlaufs.
3. **Keine Modalfalle bei kleinen Fehlern:** Potenziell verlustbehaftete Konturänderungen als Vorschau direkt unter dem *neu berechneten* Bild mit „Neue Kontur behalten?“ und Aktionen anbieten. Weiterzeichnen ist nach einer bewussten Entscheidung möglich. Ein harmloser Strich erfordert keinen Bestätigungsdialog.
4. **Klares Feedback an jedem Punkt:** Immer sichtbar: aktives Werkzeug, Werkzeuggröße, Modus (Kontur/Farbe), ausgewählte Farbe, aktive Evolutionsstufe, Autosave-Status, ggf. ausstehende Konturprüfung.
5. **Pixel bleiben Pixel:** Ein logisches 300×300-Editorraster mit nearest-neighbor; tatsächlich belegte Pixel werden 1:1, nicht auf 300×300 gestreckt, ins 800×600-Beet importiert. Mehrere geschlossene und offene Striche bilden genau eine Pflanze.
6. **Kleine Displays zuerst:** Oberste Priorität liegt auf der möglichst großen nutzbaren Zeichenfläche. Werkzeug-/Farbpaletten dürfen bei Bedarf kompakt eingeklappt werden; keine riesigen Designcontainer auf Kosten des Canvases.
7. **Schönheit ohne Lag:** Aufwändig wirkendes Design über begrenzte, vorgerenderte 9-Patch-/Bitmap-Oberflächen, solide Schriften und wiederverwendete Paints; keine teuren Echtzeit-Blur-, Partikel- oder Shader-Effekte.

## B. Gestalterische Leitidee „Das kleine Atelier“

**Referenz:** Nutzers Screenshot eines dunklen Holz-/Gold-Pixelkunst-Editors. Nur für Stimmung/Materialität und Werkzeuganordnung, **keine 1:1-Reproduktion**.

- **Grundmaterial:** dunkles, warmes Nussbaum-/Kirschholz; flache, edle Paneele mit leichter, unregelmäßiger Maserung; dünne Messing-/Kupferrahmen; wenige subtile Lichtakzente. Nicht grell orange, keine dekorativen Sternenschauer während des Malens.
- **Kreativraum:** matte, optisch neutrale Zeichenfläche, fein gezeichnetes Raster. Zwei umschaltbare Hintergründe: neutral-hell und neutral-dunkel/Schachbrett, damit sowohl dunkelviolette Konturen als auch helle Pixel auf Anhieb sichtbar sind. Der Hintergrund wird **nicht** mitexportiert.
- **Optische Hierarchie:** Canvas ist das größte Element. Materialeffekte führen nur am Rand; auf dem Pixelbild keine Glanzreflexe, die die Farbentscheidung verfälschen.
- **Ausgewählter Zustand eindeutig:** (a) deutlich hellerer Messing-/Amethyst-Rahmen, (b) **sichtbar eingedrückte** Werkzeugoberfläche, (c) kleines aktives Unterlabel (z. B. „Stift · 1 px“), (d) leicht erhöhte Kontraststufe. **Nicht nur Glow oder Farbe**; barrierefrei.
- **Taktile Qualität:** kurzer Haptik-Impuls beim Toolwechsel, anderer dezenter Impuls bei Bestätigung; kurze 80–140-ms-Press-Animation, kein fortlaufendes Atmen/Glitzern. Bei Android-Geräten ohne brauchbare Haptik bleibt das visuelle Feedback vollständig.
- **Typografie:** sehr gut lesbar, maximal eine dekorative Serifenschrift für große Abschnittstitel, für Werkzeuge und Status eine klare UI-Schrift; keine kursiven Mini-Labels.
- **Zielkontrast und Touch:** Werkzeugflächen idealerweise 48dp oder größer, lesbare Labels, TalkBack-Beschreibungen, ausgewählte Farben mit Doppelkontur/Marker, Option für weniger Animationen.

### Informationsarchitektur / Bildschirm

1. Kopfzeile: kompakter Zurückpfeil, „Pflanzen-Atelier“, Evolutionsstufe \`2/5\`, kleines Speichersymbol (✓ gespeichert / … sichert / ! Fehler).
2. **Großes, frei skalierbares 300×300-Canvas** mit dünnem Messingrahmen und Pan/Zoom. Pinch-to-zoom oder ± Zoom / „Alles zeigen“ als sichtbare Fallbacks.
3. **Modusschalter „Kontur | Farbe“ dauerhaft sichtbar**, jederzeit wechselbar; unter dem Canvas erscheint bei riskanten geometrischen Änderungen die nicht-blockierende Bestätigungskarte.
4. Werkzeugreihe: **dünner Stift (1px)**, **dicker Pinsel (zunächst 3px)**, **Radierer**, **Pipette**, **Eimer**. Icon + zugänglicher Name; exakt ein Werkzeug ist aktiv. Radierer wirkt standardmäßig auf die *aktuelle Ebene*; Eimer im Konturmodus deaktiviert und erläutert, im Farbmodus auf geschlossene Innenflächen beschränkt.
5. Aktuelle Farbe als große **aktive Farbprobe**, direkt daneben horizontale/paginierte Farbleiste (links/rechts, swipe) sowie **Farbkreis**.
6. Letzte Farbseite: **„Häufig verwendet“** (lokal gesichert, inklusive optional angehefteter Favoriten); bei Bedarf ein kleines „Zuletzt benutzt“-Band zur schnellen Wiederwahl.
7. Kleine Werkzeugeinstellungen: Dicke (3px-Pinsel perspektivisch 3/5px), Pixelperfekt-Hilfe (standardmäßig aus; manuell aktivierbar), Raster ein/aus, Undo/Redo, „Ausschnitt anpassen“.
8. Schwierigkeit Pflegeleicht/Medium/Profi/Diva als 4 klar voneinander getrennte, tastbare Optionen mit aktiver Stellung; gespeicherte Auswahl pro Evolutionsstufe.
9. Fußleiste: „Zurück“ und „Weiter“ mit konkretem Ziel („Zu Farbe“, „Stufe 3“, „Todesform“), gesondert vom Undo/Redo. Bei Layoutengpässen bleiben die zwei Navigationsaktionen fest unten und die dekorativen Panels klappen ein.

## C. Die fünf Werkzeuge – präzise Regeln

| Werkzeug | Konturmodus | Farbmodus | Bestätigung/Feedback |
|---|---|---|---|
| Dünner Stift | 1 logischer Pixel, lückenlose Linien (Bresenham) | 1px Farbe nur in aktueller gültiger Füllmaske | Aktives Tool und 1px anzeigen |
| Dicker Pinsel | Standard 3px Pixel-Brush, keine Glättung | 3px-Malpinsel, auf gültige Innensegmente maskiert | Größe sichtbar, runde oder quadratische Pixelkappe als Option |
| Radierer | Entfernt nur Konturpixel; ändert Geometrie → bei betroffenen Farben Vorschau | Entfernt Farben im Inneren, nicht die Kontur | Ebenenzuordnung stets genannt |
| Pipette | Nimmt sichtbare Farbe aus dem zusammengeführten Bild auf; kein Strich wird erzeugt | Ebenso, vorzugsweise sichtbare Pixel; optional „nur Farbebene“ | Probe neben Finger + direkt aktualisierte Farbprobe |
| Eimer | In Konturmodus inaktiv mit kurzem erklärenden Hinweis | Floodfill einer zusammenhängenden zulässigen Farbregion, ohne offene Flächen zu übermalen | Ein Tap = ein Undo-Schritt; Rechenarbeit bei Bedarf Worker |

**Farbige Konturen:** Standard weiterhin dunkelviolett. Da der Nutzer eine freie Palette wünscht, speichern wir Geometrie (binäre Konturmaske) **getrennt** von Kontur-Farbwerten; in diesem Build kann das gewählte Farbfeld bei aktiven Zeichenwerkzeugen im Konturmodus die Linienfarbe steuern, während die Farbfüllung weiterhin auf geometrischer Maske basiert. Neue \`outlineColor[]\`-Pixelebene, Alt-Darstellung bei fehlender Linienfarbe dunkelviolett. Umstellungen an der Konturfarbe dürfen die Geschlossenheit nicht ändern.

**Pipette**: Farbwahl erfolgt, ohne einen Pinselstrich zu erzeugen; danach kann auf das vorherige Zeichenwerkzeug zurückgeschaltet werden (sichtbare Option, nicht überraschend heimlicher Wechsel). Pipette auf transparentem Bereich wählt nicht automatisch „transparent“, sondern zeigt „Keine Farbe hier“.

**Pinselgefühl:** optionaler pixelperfekter Modus für weniger hässliche Treppen-Ecken (nicht blind Pixel löschen). Gesteninformationen aus \`MotionEvent.getHistoricalX/Y\` nutzen, um schnelle Fingerbewegungen lückenlos zu zeichnen; bei Zoom nur Pixelrasterkoordinaten verwenden. Optionalen kleinen Fadenkreuz-Cursor oberhalb der Fingerkuppe für sehr präzise Pixel setzen (Finger verdeckt Zielpixel).

## D. Das wichtigste Kernstück: nicht-destruktiver Rückweg Kontur ↔ Farbe

### Vier getrennte Schichten pro Evolutionsstufe

- \`outlineMask[300*300]\` – enthält **jeden** gezeichneten Linienpixel, auch offene Stiele und Zweige.
- \`outlineRGBA[300*300]\` – sichtbare Linienfarbe pro Pixel; die Konturgeometrie hängt niemals von der Farbe ab.
- \`paintRGBA[300*300]\` – Originalpigment der Malebene. **Wird bei Geometrieänderungen nicht gelöscht oder auf 0 gesetzt.**
- \`fillMask[300*300]\` + \`regionIds\` – ausschließlich **abgeleitete** Innenmasken aus der aktuellen Kontur; können ohne Farbverlust neu berechnet werden.

**Renderreihenfolge:** Hintergrundvorschau → zulässiges Pigment \`paintRGBA ∩ fillMask\` → \`outlineRGBA ∩ outlineMask\` → ggf. Overlay/Editorhilfen (nicht exportiert). Alte Pigmentpixel außerhalb der neuen Maske bleiben intern gespeichert („schlafende Farben“), sind im normalen Bild unsichtbar und können nach einer weiteren Geometrieänderung wieder sichtbar werden.

### Durchgängiger Nutzerfluss

- Nutzer malt im **Konturmodus** mehrere geschlossene Blätter und einen offenen Stiel.
- \`Zu Farbe\` erkennt vorhandene Innenflächen, Farbbereich öffnet sich; Farbpigment wird persistiert.
- Nutzer drückt **Kontur**, ergänzt eine Linie oder löscht einen Umriss. Schon bestehende Farbebene **bleibt vollständig bestehen**.
- Beim Rückwechsel zu \`Farbe\` wird die neue Füllmaske auf einer **unveränderlichen Snapshot-Kopie** asynchron berechnet; vergleichen:
  - \`lostVisibleColors\`: Pixel mit Farbe in vorheriger, aber nicht mehr gültiger Innenmaske;
  - \`newlyAvailablePixels\`: neue ausmalbare Regionen;
  - \`coveredByOutline\`: bisherige Farbpixel, jetzt von einer Linie überdeckt;
  - \`unchangedColors\`: unverändert sichtbare Pigmentpixel.
- **Wenn keine bislang sichtbare Farbe bedroht ist**, neue Kontur automatisch übernehmen und eine kurze dezente Bestätigung zeigen. Keine unnötigen Dialoge.
- **Wenn Farben optisch verschwinden würden**, den neu berechneten Stand als temporäres Vorschau-Bild anzeigen; unter dem Bild (nicht als störender System-Popup!) eine kleine hochwertige Karte:

  **„Neue Kontur behalten?“**  
  „12 bisher bemalte Pixel wären vorübergehend verdeckt. Deine Farben bleiben gespeichert.“  
  Aktionen: **„Behalten & weiter“**, **„Alte Kontur behalten“**, optional **„Unterschied zeigen“** (Bernsteinmarkierung im Bild).

- Das ist eine **Transaktion**: bis zur Entscheidung ist die vorherige Kontur/Fill-Mask als gültiger Commit erhalten. Entscheidung \`commit\` oder \`restore\` atomar, mit Undo-Eintrag. Auswahl \`Behalten\` blendet die alte Farbe nur aus, **löscht sie aber nicht**. Die Farbe wird nach wiederhergestellter Fläche automatisch erneut sichtbar, sofern nicht durch neue Malaktionen überschrieben.
- Beim Ignorieren der Karte oder Verlassen des Bildschirms: **kein stillschweigendes Verwerfen**. Status \`PENDING_TOPOLOGY_CHANGE\` persistieren und beim Zurückkehren weiter anbieten. Währenddessen Zoom/Pan und Vergleich erlauben; weitere destruktive Aktionen erst nach Entscheidung oder explizitem Abbrechen.
- Bei akutem Speicherfehler bleibt der alte Commit vollständig nutzbar; rot markierter Status mit **„Erneut sichern“**, niemals behaupten, gespeichert zu haben.

### Drei Schutzebenen

1. Kurzfristig: echte **Undo-/Redo-Befehle** (Strich, Füllen, Radieren, Kontur übernehmen), jeweils als reversible Pixel-Diffs; history pro Stufe, über Moduswechsel hinweg.
2. Mittel: **Checkpoint pro bearbeiteter Evolutionsstufe und vor jedem Topologie-Commit**, sodass Zurück/Weiter die vorherigen Farben/Stufen/Schwierigkeiten nicht löscht. Undo/Redo bei Stagewechsel nicht global \`clear()\`.
3. Langfristig: **Autorecovery** über append-only Änderungsjournal und periodische AtomicFile-Snapshots auf \`AppIo\`-Worker; Entwürfe nach Prozessende, Android-Kill oder Absturz wiederherstellbar. Kein vollständiges GZIP-Schreiben für jeden Touch-Move.

## E. Navigation: zwei unterschiedliche Bedeutungen von „Zurück“

- **↶ / ↷:** Undo/Redo des letzten Zeichen- oder Füllereignisses, einschließlich Änderungen an der Kontur und Bestätigungen. Bleibt über Moduswechsel erhalten; verfügbare Historie sichtbar/erklärbar.
- **← Zurück / Weiter →:** im Bearbeitungsablauf bewegen: Kontur A ↔ Farbe B ↔ Schwierigkeit/Stufenabschluss ↔ nächste Evolutionsstufe; auch alte \`StageDraft\`s wieder besuchen und nachbearbeiten, ohne die späteren Stufen zu vernichten.
- Die Stageübersicht (1...N plus Todesform) zeigt **✓ fertig / ● in Arbeit / ○ offen**. Aus jeder Stufe kann zu einer bereits angelegten zurückgesprungen werden.
- Nachbearbeitung einer älteren Evolutionsstufe aktualisiert bei Bedarf die 20%-Vorlage der nächstfolgenden Stufe, aber **niemals deren eigentliche Zeichnung**. Die gespeicherte Todesform-Entscheidung bleibt separat und wird nur nach bewusster Bestätigung angepasst.
- System-Back-Button: zurück zum letzten Editor-Schritt, nicht sofort die Activity beenden. Langer Wechsel/Schließen zeigt nur bei ungesicherten Änderungen einen klaren Status (im Hintergrund sichern), keine unnötige Bestätigungsdialog-Flut.
- \`Weiter\` hängt nicht vom zuletzt gewählten Werkzeug ab. Auch nach Pipette oder Radierer ist der Schrittwechsel möglich.
- \`Ausmalen\` erfordert weiterhin mindestens eine geschlossene Innenregion. Offene Linien bleiben als Pflanzenteile sichtbar. Kein Löschen ungeschlossener Details.
- **Fortschrittsanzeige**: z. B. „Evolution 2/5 · Farbe“, „Zuletzt gespeichert 05:42“, beim Rückweg „Deine bisherigen Farben bleiben erhalten“.

## F. Farbwelt, Palette und Farbkreis

- Ursprüngliche 40 Farben als kuratierte Basispalette beibehalten, **auf mehrere horizontale Seiten verteilen** (z. B. 8–10 Farben pro Seite), per sichtbare Pfeile + Swipe, Seitenindikatoren 1/5 etc.
- Freier Farbkreis mit HSV-Auswahl und eigenem **Helligkeits- und Sättigungsregler** (und optional HEX-Feld). Vorschau „alt/neu“, „Übernehmen/Abbrechen“; Wunschfarbe ist exakter ARGB-Pixelwert, keine zwangsweise Quantisierung auf 40 Farben.
- **Letzte Farbseite „Häufig verwendet“**: Ranking nach tatsächlich erfolgreich ausgeführten Strich-/Füllaktionen (nicht bloß Antippen eines Farbfelds), stabil über App-Neustart lokal gespeichert; bei gleicher Häufigkeit jüngere Nutzung zuerst. Benutzer kann Lieblingsfarben zusätzlich **anheften**. Max. z. B. 16 Einträge; niemals die 40 Standardfarben löschen.
- „Zuletzt verwendet“ als kleine gesonderte Zeile mit z. B. 6 Farben, damit häufige und kürzlich gewählte Farben nicht verwechselt werden.
- Gewählte Farbe bleibt beim Wechsel Kontur↔Farbe und beim Wiederaufnehmen des Entwurfs erhalten. Unterschiedliche Tooltypen können die zuletzt verwendete Farbe teilen; wenn sich die Konturfarbe vom Pigment unterscheiden soll, separate zuletzt gewählte Farbe pro Modus.
- Farbfelder zeigen einen **sichtbaren Rahmen und Häkchen/Punkt für aktive Farbe**, auch wenn hellweiß oder nahezu schwarz; zuverlässige \`contentDescription\` via Name/HEX.

## G. Spaß-/Anti-Frust-Details: kleines Quality-of-Life-Labor

**P0 – sofort:**
- Direkte, immer sichtbare Zustandsanzeige unter Werkzeugleiste: „✎ Dünner Stift · 1px · Farbe #54BCB1 · Ebene: Farbe“. Bei Pinsel/Auswahlwechsel aktualisiert; haptischer Klick nur bei echtem Zustandswechsel.
- Stiftvorschau bei Fingerberührung: kleines vergrößertes Pixelziel **oberhalb** der Fingerkuppe; optional ausblendbar. Bei 300×300 auf Telefon ist das praktisch wichtiger als noch mehr Werkzeuge.
- Pinch-to-zoom **um die Fingermitte** und Zwei-Finger-Pan; Zoom „Alles zeigen“ + gespeicherter Zoom pro Stage. Ein zweiter Finger verwirft keinen fertigen Pinselstrich.
- Wahlweise **Raster sichtbar/verstecken** ohne Pixelwerte zu verändern; feinere Gitterlinien nur bei hohem Zoom; neutraler Hell-/Dunkelmodus des Zeichenhintergrunds.
- **„Vorher/Nachher halten“** auf dem Vorschaubild (Touch-Hold) und klarer Mini-Hinweis für verdeckte Farben; kein Flashing oder Zwinkern.
- **„Alles füllen“** bleibt als optionale sekundäre Aktion erreichbar, nicht als leicht versehentlich gedrückter primärer Knopf; Undo jederzeit möglich.
- Keine Löschfunktion ohne sofortigen Undo, **keine Ebene beim Zurücknavigieren leeren**.

**P1 – gezielte Verbesserungen, wenn P0 stabil:**
- Pixelperfekt-Hilfsmodus gegen ungewollte Verdickungen einzelner Linien; **standardmäßig aus**, per Toggle. Kein automatisch „intelligentes“ Radieren der Originalstriche.
- Temporäre Pipette per langem Druck auf aktuelle Farbprobe; reguläre Pipette weiterhin klarer Button.
- „Kleine Lücke entdeckt“ als dezenter Hinweis nach Konturprüfung. Vorschau zeigt zusätzlich generierte Verschlusspixel als gepunktete Linie; per „Übernehmen“ bestätigen, Originalpixeldaten bleiben erhalten.
- **Kleine Miniaturvorschau** der Pflanze wie sie in echter Beetgröße wirkt (1:1 logische Pixel), ohne künstliche Skalierung. Optionaler „Beet-Vorschau“-Schalter für die aktuelle Stage.
- Zarte Abschlussrückmeldung beim erfolgreichen Evolutionsschritt (kurzes Messing-Leuchten, sanfter Klick). Keine ständigen Stern-/Partikel-Animationen.
- Option „Linkshänder“-Anordnung für Werkzeugleiste / Undo, und Schalter „Animationen reduzieren“. Beide rein visuelle Settings, keine Gameplayänderung.

**P2 – nicht blockierend / nur bei freiem Budget:** Werkzeug-Favoriten oder anpassbare Quick-Tools, einfache Spiegelung oder Gerade-Linienhilfe; **nicht** im Kern-Scope 0.0.9, wenn dadurch Undo, Performance oder Speicherqualität leiden.

## H. Technik und Datenformat (Android 6+, ZIMT-integrierbar)

### Kernmodelle
\`\`\`text
PlantDraft (schemaVersion, plantUuid, stageCount, activeStageId, activeMode, savedAt)
  StageDraft[1..10] (stageUuid, difficulty, stageStatus,
    outlineMask[90000], outlineRGBA[90000], paintRGBA[90000],
    computedFillMask[90000], regionMap, committedContourRevision,
    pendingContourPreview?, paletteSelection, zoomPan, undoCheckpointId)
  DeathArtworkDraft (same layer model, may default to gray copy)
PaletteState (standardPages, customColors, recentColors, frequencyCounter, pinnedColors)
EditCommand (id, stageUuid, layer, indexDiffs, modeBeforeAfter, timestamp)
TopologyTransaction (oldMaskHash, candidateMaskHash,
  changedPixelCounts, oldCommitId, proposedSnapshotId, status)
\`\`\`

- **Source of truth** sind \`outlineMask\`, \`outlineRGBA\`, \`paintRGBA\` und die nachvollziehbare Editorhistorie; \`fillMask\`, Thumbnails, Caches und Export-Crop sind **abgeleitet** und bei Bedarf neu berechenbar.
- Pixel-Diff-Protokoll statt bei jedem Strich Kopien von 90.000 Pixeln. Checkpoints nach abgeschlossenen Schritten und zeitgesteuert auf Hintergrund-I/O-Thread.
- \`StageDraft\`-Instanzen persistieren auch **nach** Klick auf „Weiter“ und bleiben bearbeitbar. Finale \`PlantDefinition\` wird erst aus dem vollständigen Wizard erzeugt; dabei weiterhin offene Stiele plus alle Konturen in einer Bounding Box, unverzerrter 1:1-Beet-Import.
- Statusdiagramm: \`EDIT_OUTLINE | EDIT_COLOR | CHECKING_OUTLINE | PREVIEW_TOPOLOGY | COMMITTING | SAVED\`; Modus- und Zustandswechsel mit klaren Übergängen, kein implizites \`clear()\` auf Schrittwechsel.
- **Asynchrone Arbeit:** Füllmaskenberechnung/Gap-Healing/Floodfill-Analyse, Kompression, Autosaves und Thumbnail-Erzeugung laufen auf einem Worker. Main Thread nur Eingabe, kleine Diffs, vorhandene Bitmap, Anzeigen. **Nie** Android-\`View\`/Bitmap aus Worker mutieren, während gerendert wird. Immutable Snapshots und Main-Thread-Commit.
- **Transaktionssicherheit:** \`AtomicFile\` oder SQLite-Transaktionen mit monotoner Revision; bei Crash nach „Neue Kontur behalten?“ kann die App entweder vorherigen vollständigen Commit oder neuen vollständigen Commit restaurieren, niemals eine halb passende Fill-Mask.
- Bestehende Code-Funde: \`EditorActivity.buildScreen()\` baut UI neu; \`history()\` speichert nur acht vollständige \`PlantArt\`-Kopien; \`freshUndo()\` wird beim Modus-/Stufenwechsel aufgerufen; \`StageSetupActivity\` kennt nur einen aktuellen Wizardfortschritt. **Diese Stellen sind ausdrücklich zu ersetzen**, nicht nur optisch anzupassen.
- Renderstrategie für Design: Rahmen-/Holzelemente pre-baked 9-Patch/Drawable; Canvas/Pigmentbitmap nearest-neighbor und gecacht; \`onDraw\` ohne neue Objekte; \`invalidate\` nur betroffene Regionen/Frames; große History-Listen nicht als unendlich wachsende Java-Objekte laden.
- Getrennte App-ID \`de.naehe.app.gartenkueche.lab9\`. **Keine Pflicht, Pflanzen aus lab8/lab7 zu importieren**; dortige Daten bleiben jeweils in eigener Sandbox. Interne Datenversionierung der *neuen* App trotzdem ab Anfang migrationsfähig vorbereiten.
- Keine neuen Netzwerk-, Cloud-, Firebase- oder Abo-/Kaufabhängigkeiten; vorhandene Pflege-Engine, Beet-Drag, 5s-Gießen, Stagegrößen, Offline-Modus und Zeitraffer bleiben funktional.

## I. Konkrete Build-Milestones und Gates

**M0 – Baseline & Sicherheitsnetz:** Teststand 0.0.8 verifizieren, Version 0.0.9 + App-ID \`lab9\` vorbereiten, regressionsfähige Testfixtures für 1/3/20 Konturen, dünnen Stiel, 10 Stufen, 80× anlegen. **Gate:** alter Lifecycle weiterhin grün.

**M1 – Nicht-destruktive Editor-Engine:** \`StageDraft\` und separate Outline-/Color-Layer, Masken-Neuberechnung ohne Überschreiben des Pigments, Back/Forward, Undo/Redo persistiert. **Gate:** Kontur nach Farbe korrigierbar, 100 gemalte Striche + alte Stufen bleiben erhalten.

**M2 – Topologie-Vorschau:** alten vs. neuen sichtbaren Farbraum prüfen, ohne Konflikt automatisch übernehmen; bei Verlust von sichtbarer Farbe Preview unter neu berechnetem Bild, Buttons „Behalten & weiter / Alte Kontur behalten / Unterschied zeigen“, reversible Transaktion. **Gate:** gelöschte/unterbrochene Kontur blendet Farben nur vorübergehend aus, Wiederherstellung macht sie identisch sichtbar.

**M3 – Werkzeuge und Eingabe:** 1px-Stift, 3px-Pinsel, ebenenbezogener Radierer, Pipette, zusammenhängender Eimer, klarer aktiver Zustand, Touch-History, Cursor-Offset, Zoom/Pan. **Gate:** jede Eingabe erzeugt genau einen wiederherstellbaren History-Eintrag; Toolwechsel nie Verlust des Progress.

**M4 – Farben:** Originalpalette auf blätterbare Seiten, HSV-Farbkreis mit Helligkeit/Sättigung, persistierte häufigste Farben und Pinning, konsistente aktive Farbe. **Gate:** exakte ARGB-Werte auf allen Stufen nach Prozesskill; Häufigkeit nur bei echten Malaktionen.

**M5 – Premium-Design:** charmanter eigenständiger warmes-Holz/Messing-Look mit klarer Bildfläche, 9-Patch-Assets, adaptive obere/untere Leisten, aktive Toolhervorhebung, Schwierigkeit, Back/Weiter, Feedback. **Gate:** ohne Überdeckung oder unsichtbare Controls auf kleinen Smartphones; Farbbild nicht durch Glasglanz verfälscht.

**M6 – Alle Evolutionsstufen, Todesform, Beet:** Stage-Map mit Rückweg, Fortschrittscheckpoint, 20%-Overlay der vorherigen Stufe, separate Todesform, enge Crops, 800×600-Beet, Drag, Pflege-Engine und 1–80×-Zeitraffer. **Gate:** Änderung einer frühen Stage zerstört weder spätere Stufen noch deren Farben.

**M7 – Performance & Datenrettung:** Speicher-Journal, Atomic Recovery, Geräte-Resumption und Low-RAM-Profil. **Gate:** keine synchronen großen Dateioperationen auf UI-Thread, keine bei jedem Touch neu angelegten 300×300-Bitmaps; Test auch bei Android 6/API23.

**M8 – QA & Distribution:** Java-/Android-Tests (inkl. Crash-Recovery), \`assembleDebug\`, APK-ID \`lab9\` prüfen, installierbare Debug-APK als **direkten Chat-Download**, nicht nur ZIP. Reale Android-Geräte-Tests bleiben separate Abnahme.

## J. Abnahmetests – konkrete Nutzererlebnisse

1. **Drei Blätter + offener Stiel:** drei geschlossene Füllflächen, ein einzelner offener dunkelvioletter Stiel, alles bleibt sichtbar und wird als eine Pflanze exportiert.
2. **Kontur → Farbe → Kontur:** 20px in zwei Blättern färben; eine Kontur verändern; Vorschau zeigt exakt betroffene Farben, „Behalten“ verdeckt sie nur, „Alte Kontur“ stellt die ursprüngliche Grafik einschließlich aller Farben wieder her.
3. **Reversibilität:** Kontur wiederherstellen bringt alte verdeckte Farben ohne Neumalen zurück, sogar nach App-Neustart.
4. **Harmloser Strich:** neue Linie außerhalb aller kolorierten Innenregionen löst keine unnötige Rückfrage aus.
5. **Modi und Stufen:** von Farbmodus Stufe 4 bis Konturmodus Stufe 1 zurückspringen, weiter zu Stufe 4, alle vier Zeichnungen, Farbwerte und Schwierigkeiten bleiben unverändert.
6. **Undo/Redo:** ein Pinselstrich, Werkzeugwechsel, Pipette und Moduswechsel, anschließend Undo und Redo; der richtige Strich wird rückgängig gemacht, nicht der gesamte Stagefortschritt.
7. **Farbseiten:** Standardseiten, HSV-Farbkreis und zuletzt/häufig verwendete Farben bleiben nach App-Neustart korrekt; ohne versehentliches Umfärben durch die Pipette.
8. **Falsche Bedienung:** unbeabsichtigtes Tippen auf einen falschen Modus, Radierer oder „Zurück“ darf keine fertige Evolutionsstufe dauerhaft leeren.
9. **Fingerpräzision:** schnelles diagonales Zeichnen hat keine Löcher, Pixel mit Offsetcursor gezielt treffbar, Pinch und Pan funktionieren ohne zufällige Zeichenspuren.
10. **Recovery:** Prozesskill während gemalter Kontur; Prozesskill während ausstehender neuer Kontur; Prozesskill beim Speichern der dritten von zehn Stufen. Beim Neustart entweder letzter gültiger Stand oder explizit wiederaufnehmbare Vorschau – nie korrupte Mischstände.
11. **Grafikqualität:** Sprites auf originaler Bounding Box, unterschiedliche Crop-Größen pro Stage mit stabilem Ursprung, keine geglätteten Pixel, keine plötzlich verschwindenden Konturen.
12. **Spielmechanik:** 1×, 20×, 80× Zeit, reale 5000ms-Gießaktion, Offline-Kare-Engine, Regression und Todesform unverändert.
13. **Android:** Pixelgrafik und Interaktion auf Android 6+, kleinen und großen Bildschirmen; 48dp-Touchflächen und TalkBack-Bezeichnungen. APK installierbar neben Lab 0.0.8 und ZIMT.

## K. Inspirations-Recherche – was verifiziert ist, was unsere eigene Lösung ist

Es wurden **offizielle Handbücher / Entwicklerdokumente** untersucht:

- **Aseprite:** Ebenen, Undo/Redo-History, Pipette, Pixelzoom und Layer-Persistenz: https://aseprite.org/docs/basics/ und https://aseprite.org/docs/edit-menu/ und https://aseprite.org/docs/eyedropper/ . Inspiration für differenzierte Ebenen und wiedererreichbare Befehle, nicht ein fertiger unverändert kopierter Workflow.
- **Krita:** Fill-Tool/Contiguous-Regions, Pixel-Brush-Smoothing und getrennte Line-Art-/Farb-Layer: https://docs.krita.org/en/reference_manual/tools/fill.html und https://docs.krita.org/en/reference_manual/tools/freehand_brush.html und https://docs.krita.org/en/user_manual/getting_started/basic_concepts.html .
- **Procreate:** QuickMenu, sichtbare Farbauswahl/-historie, Gesten für Undo/Redo: https://help.procreate.com/procreate/handbook/interface-gestures/quickmenu und https://help.procreate.com/procreate/handbook/colors/colors-interface .
- **Android:** keine Allokationen in \`View.onDraw\`, UI-Thread freihalten: https://developer.android.com/develop/ui/views/layout/custom-views/optimizing-view .

**Eigene projektspezifische Entwurfsentscheidungen:** Die unterhalb des *neu berechneten Bilds* erscheinende „Neue Kontur behalten?“-Karte, Schichten mit „schlafenden“ Farbe-Pixeln, persistierter Topologie-Transaktionszustand, Stage-Rückweg ohne Artworkverlust und alle abgestimmten Gameplay-Interaktionen sind unser **eigenes Design** für Garten & Küche, nicht behauptete Funktionen der genannten Programme.

---

## Definition of Done

**0.0.9 ist erst fertig**, wenn ein Nutzer einen Setzling mit mehreren Konturen und offenem Stiel zeichnet, ihn farbig gestaltet, zurück zur Kontur wechselt, sicher eine problematische Änderung vorprüft und wieder rückgängig macht, beliebig zwischen begonnenen Evolutionsstufen hin- und hergeht, mit fünf klar markierten Werkzeugen und frei wählbaren Farben weiterarbeitet, und alles nach Neustart samt Beet/Lebenszyklus erhalten bleibt – mit angenehm flüssiger Bedienung und einem eigenständigen Holz-/Messing-Design. Ein grüner GitHub-Workflow ist notwendig, ersetzt aber keinen echten Gerätetest.

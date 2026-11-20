# Garten & Küche — Buildplan 0.0.6
**Status:** Geplanter Build (nicht implementiert) · **Basis:** Android Build 0.0.5 · **ZIMT-Kompatibilität:** native Java/Views/Canvas, minSdk 23

## Ziele

1. Im 300×300-Editor **beliebig viele separate geschlossene Konturen** in einer Evolutionsstufe behalten. Nicht die zuletzt gemalte Kontur bevorzugen.
2. Alle erkannten Innenflächen individuell bemalbar, Außenflächen geschützt; die Gruppe bleibt immer **ein Pflanzenobjekt**.
3. Für Beet/Transfer alle Pixel auf die nötige **Bounding Box** zuschneiden. Ursprüngliche Rasterkoordinaten und stabile Anker behalten; keine automatische Größenänderung.
4. Unter dem 800×600-Beet einen ästhetischen **1×–20×-Zeitraffer-Slider** sowie die Anzeige „Tag N seit Erstellung“ für die ausgewählte Pflanze ergänzen.
5. Die Zeitsimulation an die Pflege-Engine anbinden, **ohne** reale 5-s-Gießgesten zu beschleunigen; reproduzierbar, persistent, ohne UI-Blockaden.
6. Eigene Anwendung-ID \`de.naehe.app.gartenkueche.lab6\`, neben 0.0.5 und ZIMT installierbar; Android 6+, reiner Java-Core, keine Serverpflicht.

## 1. Mehrkonturen-Editor

**Ist-Stand:** \`PlantArt.outline\` + \`PlantArt.pigments\` + \`PlantArt.inside\`, \`ContourDetector.detect()\` erkennt abgeschlossene Innenregionen, kann aber die Konturrandlinie bereinigen. Wir müssen die gesamte gültige Geometrie erhalten und alle Formen auswählbar machen.

- Jede Maus-/Touchgeste produziert Pixel auf einem gemeinsamen unveränderlich dimensionierten Zeichenraster; es gibt keine globale Start-/Endbedingung mehr.
- Nach \`Kontur prüfen\` eine Kopie der vollständigen Kontur erstellen und per linearer 4-Nachbar-Floodfill-Analyse vom Außenrand aus alle eingeschlossenen Regionen erkennen. 8-Nachbar-Analyse für zusammenhängende Pinselstriche nur, wenn relevant; niemals darauf bestehen, dass alle Konturstriche untereinander verbunden sind.
- Alle erkannten Innenflächen werden in einer **gemeinsamen union-Innenmaske** und optional in einzelnen \`regionIds\` erfasst. Jede Region bleibt bemalbar, entweder per Stift, Eimer je Region oder „alle füllen“ über die Gesamtheit.
- Mehrere Formen, Kreuzungen und berührende Formen sind zulässig. Werden Innenflächen durch Linien geteilt, bleiben alle ausreichend großen Innenregionen verfügbar. Bei ineinandergeschachtelten Konturen die tatsächlich geschlossenen Teilflächen nachvollziehbar freigeben, statt eine „letzte Kontur“ vorzuziehen.
- Zusätzliche offene Striche dürfen fertige gültige Innenflächen **nicht löschen**. Vor dem Bestätigen eine sichtbare Vorschau der erkannten Konturen präsentieren. Nur wirklich außerhalb jeder gültigen geschlossenen Region liegende unvollständige Hilfslinien getrennt halten; standardmäßig nicht ungefragt endgültig aus dem Entwurf löschen.
- Kurze Lücken bis vier logische Pixel optional verbinden, aber höchstens so, dass echte, sinnvolle Innenflächen entstehen. Lückenschluss als getrenntes Resultat und ohne teure wiederholte Vollscans auf dem UI-Thread. Aufwendige Erkennung im Hintergrund.
- Mindestens eine sinnvoll große geschlossene Innenfläche ist erforderlich, ansonsten Fehlermeldung; es gibt **kein Maximum an Konturen** außer dem Platz im 300×300-Raster.
- Kontur, Innenmaske, Farbbitmap, Region-ID oder abgeleitete Maske und reversible Endbearbeitung bleiben für jede Evolutionsstufe getrennt persistiert. Einheitliche UX für 1–10 Stufen, Overlay bei 20 % Deckkraft und manuell gezeichnete Todesform.
- Testfälle: 1/2/5/20 getrennte Formen; mehrere Stiftansätze je Form; fertige Form + spätere zweite Form; zusätzliche offene Linie; kreuzende/berührende/verschachtelte Konturen; Undo/Redo; Zwischenspeichern und Fortsetzen; alle Flächen kolorierbar.

## 2. Crop/Bounding-Box-Export und Beet-Darstellung

**Wichtig:** 300×300 ist *nur der Arbeitsbereich*. Die tatsächliche Pflanzengröße ist die Vereinigung aller sichtbaren Kontur-/Farbpixel einer Evolutionsstufe.

- \`bbox = [minX, minY, maxX, maxY]\` über **alle** zugehörigen Konturen, Füllpixel und transparent getrennten Inseln. Kein künstlicher Puffer in den exportierten Bilddaten.
- \`width=maxX-minX+1\`, \`height=maxY-minY+1\`; leere Bilder zurückweisen. Beispielsweise 35×60 belegte Pixel ergeben ein 35×60-Sprite, **nicht** ein auf 300×300 gestrecktes Sprite.
- Original \`300×300\` + Crop-Offsets/Anker in der **PlantDefinition** behalten. Für den Speicher- und Anzeigeexport tatsächlich auf die Crop-Größe zuschneiden. Mehrere Formen bleiben in **einer** Bounding Box und behalten exakt ihre relativen Abstände.
- Bei jeder Evolution eine eigene \`bbox\`, aber denselben logischen Ursprung / Pflanzenanker verwenden, sodass Blatt- und Stängelpositionen nicht springen. \`destX = plantOriginX + stageCropMinX - anchorX\`, entsprechend Y. Alternative: explizite Pivots pro Evolution, falls das Pflanzenwachstum anders ausgerichtet sein soll. Kein \`drawBitmap\` auf ein künstliches fixes 300×300-Zielrechteck.
- Für Hit-Testing und Auswahl die aktive sichtbare Pixelmaske oder eine maßvolle Mindest-Touchfläche verwenden, **niemals** alle 300×300 Pixel als Hitbox. Das Bewegen der Pflanze verschiebt das gesamte zusammengesetzte Objekt, unabhängig von der Anzahl der Konturen.
- Für das künftige \`.zimtplant\`-Paket \`stage_bbox\`, ursprüngliche logicalCanvasSize=300, anchor, cropOffset, croppedArt, OutlineMask und difficulty speichern. Späteres Wiederöffnen im Editor muss alle Originalpositionen wiederherstellen können.
- Performance: kleine getrimmte ARGB-Sprites statt 300×300-Bitmaps, Bytebudget-LRU weiterverwenden, nearest-neighbor, keine Neurasterisierung pro Dragframe.
- Testfälle: belegte Pixel 8×12, 35×60, 150×280, mehrere Inseln weit auseinander; pixelidentische Größe in Beet, Pixelpositionen beim Stufenwechsel stabil, Todesgrafik folgt denselben Regeln, keinen Teil abgeschnitten.

## 3. Zeitraffer-Testmodul

### Oberfläche und Platzierung
Unmittelbar **unter dem 800×600-Beet** eine ruhige, kompakte Karte, z. B. dunkle anthrazitfarbene Fläche, etwas violette Leuchtkante, matte Beschriftung. Im bestehenden Lab-UI ggf. warmes Beige/Amethyst-Design als harmonische Alternative; keine schweren Effekte. Oben Uhr-/Sanduhr-Icon + „Zeitraffer“, rechts dezente **N×**-Anzeige. Darunter ein Slider mit 1,5,10,15,20 als Marken und 1er-Schritten. Darunter „Pflanzenalter · Tag N seit Erstellung“ und optional „Ein Pflanzentag: X in Echtzeit“. Wenn mehrere Pflanzen im Beet: die angetippte ausgewählte Pflanze hat ihren **eigenen** Tag-N-Zähler; keine Auswahl → verständlichen Hinweis anzeigen.

### Zeitsemantik
- Bei **1×** vergeht virtuelle Zeit in normaler Geschwindigkeit. Bei **20×** laufen Zeitfristen zwanzigfach schneller. Beispiel: \`1 Pflanzentag = 24h / speed\`, bei 20× = 72 echte Minuten; 10× = 2h24min. Keine irreführende Zusage, zehn Evolutionsstufen in Minuten testen zu können.
- Die 5.000-ms-Haltedauer der Gießkanne bleibt immer **5 echte Sekunden aus \`SystemClock.elapsedRealtime()\`**, unabhängig vom Slider. Ein Halt = ein Gießvorgang; seine Spielzeit erhält den aktuellen \`simulationNow\`-Zeitstempel.
- \`TestClock\` separat von \`CareEngine\` definieren, \`Clock.nowUtc()\` injizieren. \`CareEngine.evaluate(plant, simulatedNow)\` und \`CareEngine.water(plant, simulatedNow)\` mit **derselben** virtuellen Zeitbasis aufrufen. Pflegefenster, rollierende 24h-Limits und Evolution nutzen diese Zeit; sonst inkonsistente Gießfristen.
- \`VirtualClock\` mit \`simulationAnchorUtc\`, \`anchorElapsedRealtime\`, \`speedFactor=1..20\`. \`nowSim=anchorSim+(elapsedRealtime-anchorElapsed)*speed\`. Bei Sliderwechsel den aktuellen simulierten Zeitpunkt zuerst sichern und danach neuen Faktor setzen: **keine Zeit- oder Tagessprünge**.
- Unterbrechen/Wiederaufnehmen: bei Testtempo >1 vorgeschlagen **Simulation pausiert außerhalb des geöffneten Beets**, damit die Testpflanze nicht während der Abwesenheit überraschend stirbt; beim Zurückkehren fortsetzen. 1× kann wie bisher echte Zeit zwischen App-Starts berücksichtigen. Verhalten explizit anzeigen, persistent machen und via Tests gegen Zeitsprünge prüfen. Keine Zeitmanipulation an der Systemuhr.
- \`createdAtSimUtc\` getrennt von realem Erstellungsdatum halten; \`plantAgeDays=floor((simNow-createdAtSimUtc)/DAY_MS)\`, angezeigter \`Tag 1+max(0,plantAgeDays)\`. Bei verschiedenen Pflanzen dürfen die angezeigten Tage unterschiedlich sein.
- Der Testmodus ist **lokal** und für spätere Geschenk-/ZIMT-Synchronisation **nicht maßgeblich**. \`testOnly\` und Datenformatversion kennzeichnen, damit keine künstlich beschleunigten Lebensdaten als echte übertragen werden.
- Simulationsengine soll während geöffnetem Beet tatsächlich mitlaufen, auch wenn niemand gießt: kostenarme ereignisbasierte Scheduler-Berechnung aus nächster relevanter Frist/Taggrenze; keine dauernden 60-FPS-Neuberechnungen der Pflege. Worker wertet abgelaufene Fenster seriell aus und liefert nur Zustandsänderungen an UI.
- Optional nach separater Zustimmung: „+1 Testtag“, weil selbst 20× noch 72 Minuten pro Spieltag benötigt. **Nicht zwingender 0.0.6-Scope**.

## 4. Daten-/Performance- und Android-Schutz

- Android 6.0 minSdk23, Java17, Native Canvas, gleicher Android/Gradle-Stack wie ZIMT, keine neue Engine.
- Die 0.0.5-Arbeit (asynchrones Einlesen/Speichern, \`AtomicFile\`, StateSnapshot, statische Raster und Bitmap-Caching) **bewahren**, nicht zurückbauen.
- Kontur-Analyse und Crop nur bei Bestätigen/Änderung, nicht während jeder Stiftbewegung.
- Ausgewählte Pflanze und Tag N bei Bedarf aktualisieren (Taggrenze), slider-Anzeige bei Änderung, Pflege-Engine zu relevanten Ereignissen; kein neuer Timer mit ständiger teurer Vollsimulation.
- Getrennte Änderungen: Artwork-Definitionsdaten unverändert und verlustfrei; Status und \`TestClock\` getrennt atomar speichern. Versionierte Migration für ältere Datenformate und Tests auf bestehendem Labor-Build.
- Eigene Android Application-ID je Lab-Build (\`lab6\`). Wegen Sandbox-Isolation sind bestehende Pflanzen aus 0.0.5 **nicht automatisch** in 0.0.6 verfügbar. Das ausdrücklich in README/Downloadhinweis erklären.
- Beim Import später geschenkter Pflanzen in ZIMT sind Zeitraffer-/Testdaten standardmäßig nicht übertragbar.

## 5. Umsetzung in 6 Milestones

**M0: Branch/Backup + Tests.** Bestehenden 0.0.5-Stand dokumentieren; Kontur-Regressionstests für 1–20 Formen und Crop-Testfälle hinzufügen.

**M1: Region-Aggregation.** \`ContourDetector\` und \`PlantArt\` auf alle gültigen Regionen ausweiten, Vorschau für mehrere Innenflächen, ohne alte Formen zu löschen. Test: Erst eine geschlossene Form, dann zweite, dann dritte; alle bleiben ausmalbar.

**M2: Tight sprites.** Bounding-Box-Funktion, Bitmap-Crop, Crop-Metadaten, konsistente Anker über Evolutionsstufen, Multi-Shape-Hitbox und gemeinsames Dragging. Pixelgröße 1:1 im Beet.

**M3: UI.** Ruhige Zeitraffer-Karte *direkt unter dem Beet*: Slider 1..20, numerischer N×-Wert, responsive Markierungen, Pflanzenalter Tag N, Anzeige der Umrechnung zur Echtzeit; für ausgewählte Pflanze.

**M4: TestClock/Simulation.** Virtuelle Zeitbasis mit persistiertem Anker/Geschwindigkeit; bei Sliderwechsel fließender Übergang; 1× Realzeit, >1× Testtempo; Gießkanne exakt 5 reale Sekunden; Spielzeit-Stempel und Pflegefristen einheitlich simuliert. Lifecycle/Background-Verhalten verifizieren.

**M5: QA und APK.** Android-6-Java-Tests, UI-Performance, 10 Evolutionsstufen, 20 Shapes, wiederholte Farbfüllung, Gießen, Regression/Tod, Testtempo-Wechsel und App-Neustarts. GitHub-Actions-APK mit \`de.naehe.app.gartenkueche.lab6\`; direkte APK-Datei im Chat anbieten, nicht bloß den ZIP-Workflow.

## 6. Kritische Abnahmetests

1. Zwei weit voneinander entfernte komplett gültige Konturen, dritte Kontur zuletzt: **alle drei** sichtbar und einzeln bemalbar.
2. Eine gültige Kontur plus eine offene Linie: erste Kontur bleibt nutzbar; unnötige Linien nicht stillschweigend endgültig löschen.
3. Drei Formen -> ein Objekt im Beet; Verschieben bewegt alle Teile mit gleichen Abständen.
4. 20×30 belegte Pixel exportieren als 20×30-Bitmap und erscheinen mit **1:1 Logikpixelmaßstab** im 800×600-Beet.
5. Drei Evolutionsstufen mit verschiedenen Crops bleiben am selben Pflanzenanker, kein Sprung bei Wachstum oder Rückentwicklung.
6. Bei 1× und 20× und gleichem simulierten Zeitverlauf gilt dasselbe Regelwerk. Reale 5s-Gießschwelle unverändert.
7. Sliderwechsel 20×→5×→1× löst weder Rücksprung noch zusätzlichen Gießvorgang aus.
8. Aktuelle Pflanzenalter-Anzeige: neu erstellt Tag1, nach genau 24 simulierten Stunden Tag2; jede Pflanze separat.
9. 20×-Simulationsfenster prüfen auch Fristverstöße und Tod, ohne bei jedem Frame den ganzen Pflanzenbestand neu zu speichern.
10. Nach App-Neustart/Screen-Lock stabile Simulation und persistierte Pflanzen; alter Build 0.0.5 bleibt unabhängig installiert.

**Wichtig:** Dieses Dokument ist der technische Plan, **noch kein implementierter Build oder geprüfte APK**.

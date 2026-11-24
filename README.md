# Garten & Küche — ZIMT Lab 1

## Build 0.0.13 — Version der installierten App

Launcher und Startseite zeigen die Version des installierten Builds an. Das Atelier bündelt die Pixeldaten beim Speichern und Laden seiner Entwürfe. Diese Ausgabe hat die eigene Paketkennung `de.naehe.app.gartenkueche.lab13`.

## Build 0.0.12 — Konturvorschau nach Drehung

Eine offene Konturentscheidung wird beim erneuten Erstellen des Editors aus dem gespeicherten Entwurf wieder aufgebaut. So kannst du die Änderung nach der Unterbrechung weiterhin annehmen oder verwerfen. Eigenständiges Android-Paket `de.naehe.app.gartenkueche.lab12`.

## Build 0.0.11 — Zeichenverlauf

Beim Rückgängigmachen und Wiederholen von Konturstrichen und Radierungen werden auch die Punktzahl und die Strichendpunkte wiederhergestellt. Das gilt auch, wenn zwischen den Strichen gefüllt oder mit Farbe gezeichnet wurde. Separat installierbare APK für Android 6.0+: `de.naehe.app.gartenkueche.lab11`.

## Build 0.0.10 — Entwürfe nach Unterbrechungen wieder öffnen

Der Editor lädt nach einer Activity-Neuerstellung den gespeicherten Entwurf. Ein Lesefehler führt zurück zur Auswahl. Dort lässt sich die Wiederherstellung wiederholen oder das Ersetzen ausdrücklich bestätigen. Die separate APK verwendet `de.naehe.app.gartenkueche.lab10` (Android 6.0+). Zeichnungen aus Lab 9 bleiben in dessen App-Speicher.

## Build 0.0.9 — Pflanzen-Atelier (nicht-destruktiver Editor)

**Neue eigenständige Android-Test-App:** Paketkennung \`de.naehe.app.gartenkueche.lab9\`, Android 6.0+; die älteren Lab-Versionen und ZIMT bleiben installiert und unverändert. **Kein Update-/Importmechanismus** zwischen älteren Lab-Apps in dieser Version.

### Design
- Eigenständige, warme dunkle Holz-/Messingoptik mit subtilen Farbverläufen, deutlichen aktiven Werkzeugzuständen und neutralem Editor-Canvas. An der Designreferenz orientiert, nicht eins zu eins reproduziert.
- Eigenständiger neuer Einstieg: Evolutionsstufen 1–10 auswählen und bei vorhandenem Entwurf fortsetzen.
- Größtmögliche Zeichenfläche, kleine Smartphone-Displays und Android 6.0+ bleiben berücksichtigt; keine kostenintensiven Daueranimationen.

### Werkzeuge, Farben und Bedienung
- **Fünf Werkzeuge:** 1-Pixel-Stift, 3-Pixel-Pinsel, Radierer, Pipette und Fülleimer.
- Immer sichtbar: aktuelles Werkzeug, Größe, Ebene und Hex-Farbe. Ausgewählte Werkzeuge erhalten gut unterscheidbare Messing-/Holzrahmen.
- Die 40 Grundfarben liegen auf fünf über Pfeile steuerbaren Seiten. Die fünfte Seite zeigt **häufig tatsächlich gemalte Farben**; die Statistik bleibt lokal erhalten.
- HSV-Farbkreis mit Helligkeit, freier Farbwahl und Farbprobe. Pigment und Konturlinie speichern eigene ARGB-Pixelwerte.
- Undo/Redo verwenden eine **sparsame Pixel-Differenzliste**, die beim Wechsel zwischen Kontur und Farbe sowie beim Navigieren zu einer anderen Evolutionsstufe nicht gelöscht wird. Der Verlauf gilt für die aktuelle App-Sitzung; nach einem kompletten Prozessneustart wird der letzte atomar gespeicherte **Entwurfsstand**, nicht der vollständige Undo-Verlauf, wiederhergestellt.
- Fingerhilfe beim Zeichnen, Zwei-Finger-Pan und Pinchzoom, Raster ein/aus und Zoomtasten.

### Schutz vor verlorenem Fortschritt
- **Separate Konturlinien, Konturfarben, Farbwerte und berechnete Füllmaske.** Auch verdeckte alte Farbwerte bleiben gespeichert.
- **Kontur ↔ Farbe ist jederzeit möglich.** Beim Übergang wird die neue Innenmaske asynchron geprüft. Könnten bereits sichtbare Farben vorübergehend verdeckt werden, zeigt die App direkt **unter dem neu berechneten Bild** eine Entscheidung: „Neue Kontur behalten?“ → „Behalten & weiter“, „Alte Kontur“, „Unterschied anzeigen“.
- Kleine Konturänderungen ohne gefährdete Farben werden ohne Unterbrechung übernommen; offene Stiele und mehrere geschlossene Bereiche bleiben erhalten.
- Unterbrochene Konturarbeiten bleiben mit **„noch zu prüfen“** über Stagewechsel und App-Neustart markiert.
- Alle angefangenen Evolutionsstufen können über die Stage-Leiste erneut geöffnet und verändert werden, ohne die anderen Stufen oder Farben zu löschen.
- Entwürfe werden mit Android \`AtomicFile\` und GZIP auf dem I/O-Worker gesichert. Die App erstellt vollständige Sicherheits-Snapshots nach Bearbeitungspausen und beim Verlassen der Activity; ein vollständiges transaktionales Änderungsjournal ist eine spätere Erweiterung.

### Weiterhin enthalten
800×600-Beet, offene Stiele, alle Mehrkonturen als ein Objekt, pixelgenauer Zuschnitt, 20%-Vorlage aus vorheriger Evolutionsstufe, 1–80×-Zeitraffer, echte 5-Sekunden-Gießaktion, Schwierigkeitsgrade, Evolution/Regression/Tod, Standard- oder eigene Todesform.

### Hinweise zu Reife und offenen Punkten
- Ein grüner GitHub-Build bestätigt Compilation und automatisierte Tests, nicht echte Geräteleistung. Android-6-/Small-Display-Tests sowie Absturz-Recovery sind weiter zu überprüfen.
- Der Farbkreis wird frei nach HSV ausgewählt; angeheftete Lieblingsfarben und ein HEX-Eingabefeld sind noch nicht verfügbar.
- Der **über Prozessneustarts hinweg wiederherstellbare Undo-Verlauf** sowie ein append-only Journal sind noch nicht umgesetzt; der fertige Entwurf selbst wird aber gesichert.
- Keine automatische Datenübernahme von lab8 oder anderen separat installierten APKs: Android isoliert deren private Dateien.

**Vollständige Spezifikation:** [BUILD_PLAN_0.0.9_ATELIER_NONDESTRUCTIVE.md](BUILD_PLAN_0.0.9_ATELIER_NONDESTRUCTIVE.md).  
**APK:** Über [GitHub Actions](https://github.com/shayohekmat-wq/Zimt-Lab-1/actions), Artifakt \`Garten-und-Kueche-v0.0.9-Atelier-APK\`; zusätzlich als direkter APK-Download im Chat.

---


Eigenständige Android-Test-App für spätere Integration in ZIMT. **Java + Android Canvas**, Android **6.0+ (API 23)**, offline-first und separat installierbar. ZIMT bleibt unverändert.

## Build 0.0.8 — Stiele und offene Linien bleiben erhalten

**Fehlerkorrektur nach einem echten Smartphone-Test:** Bei einem selbst gezeichneten Setzling waren drei geschlossene Konturen erkannt, ein frei gezeichneter einpixeliger Stiel jedoch nach der Konturprüfung verschwunden.

- Die Zeichenpixel bleiben jetzt **unabhängig** von der Maske für die ausmalbaren Innenflächen erhalten.
- **Stiele, Zweige, Blattadern und offene Einzellinien** bleiben sichtbar; weiterhin sind nur die geschlossenen Bereiche bemalbar.
- Eine spätere Linie oder Kontur löscht keine vorherigen Striche. Beim Schließen winziger Lücken werden ausschließlich Pixel hinzugefügt.
- Die **enge Bounding Box** umfasst sämtliche sichtbaren Linien und Füllfarben. Dadurch werden auch lange dünne Stiele im Beet als Teil derselben Pixelpflanze exportiert und gemeinsam bewegt.
- Dieselbe Logik gilt für alle Evolutionsstufen und die Todesform. Kein verändertes Pflanzen-Speicherformat.
- Neue automatische Tests prüfen das ursprünglich gemalte Stielende, Innenmasken, Crop, die gespeicherten Konturpixel, Farben und Graustufen-Todesgrafik.
- Unverändert: **1×–80× Zeitraffer**, Gießdauer fünf echte Sekunden, Android 6+ und die Performance-Optimierungen aus 0.0.5.

**Installieren:** Die App `Garten & Küche 0.0.8` hat die neue Paketkennung
`de.naehe.app.gartenkueche.lab8`. Sie ist parallel zu 0.0.7 und ZIMT installierbar.
Die App-Daten sind aufgrund der getrennten Android-Sandboxes nicht automatisch übertragbar.
Die direkte APK-Datei wird im Chat bereitgestellt; der zugehörige
[GitHub-Actions-Workflow](https://github.com/shayohekmat-wq/Zimt-Lab-1/actions)
enthält das Artefakt `Garten-und-Kueche-v0.0.8-Stiel-Fix-APK`.

Technische Spezifikation: [BUILD_PLAN_0.0.8_OFFENE_LINIEN.md](BUILD_PLAN_0.0.8_OFFENE_LINIEN.md).

---

## Vorheriger Build 0.0.7 — Mehrkonturen & Zeitraffer bis 80×

Diese Version baut auf der Performance-Optimierung 0.0.5 auf.

### Funktionen aus 0.0.7
- **Mehrere Konturen pro Pflanze:** Eine Evolutionsstufe darf viele getrennte Formen enthalten. Der Kontur-Detektor sucht alle tatsächlichen abgeschlossenen Innenbereiche; eine spätere zweite Kontur ersetzt nicht die erste. Nach der Prüfung zeigt der Farbeditor die Anzahl erkannter Innenregionen an.
- **Mehrere Formen = eine Pflanze:** Alle erkannten, bemalten Pixel aus separaten Konturen werden innerhalb derselben 300×300-Stage gespeichert. Im Beet ist es ein einziges gemeinsames Objekt, das sich zusammen verschieben lässt.
- **Kleine Lücken:** Kurz geschlossene Lücken (bis vier logische Rasterpixel) können auch dann noch erkannt werden, wenn schon eine andere gültige Kontur existiert.
- **Exakter enger Zuschnitt:** Im Beet werden Sprites je Evolutionsstufe auf die tatsächlich belegten Pixel zugeschnitten. Eine 8×12-Pixel-Zeichnung erscheint mit 8×12 logischen Pixeln — nicht auf 300×300 vergrößert. Die Positionen bleiben durch die aufbewahrten Originalkoordinaten über Evolutionsstufen stabil. Auch die Todesform wird so behandelt.
- **Zeitraffer-Karte unterhalb des Beets:** Dunkle, violett akzentuierte Oberfläche, Slider **1×–80×** mit Geschwindigkeit und Anzeige, wie lange ein Spieltag in Echtzeit dauert.
- **Pflanzenalter:** Tippe im Beet auf eine Pflanze. Unter dem Regler wird ihr persönlicher Lebenstag als **„Tag N“** angezeigt. Tag 1 beginnt bei der Aufnahme ins Beet; jeder weitere Tag beginnt nach vollen 24 *simulierten* Stunden.
- **Virtuelle Testuhr:** Slider beschleunigt Pflegefristen, Wachstum, Rückentwicklung und Todesereignisse. Gießen erfordert **weiterhin fünf echte Sekunden** ununterbrochener Berührung.
- **Zeituhr ohne Sprünge:** Der simulierte Zeitpunkt bleibt bei einem Geschwindigkeitswechsel konsistent. Im Testtempo >1× pausiert die Simulation, wenn das Beet nicht geöffnet ist; bei 1× läuft die normale Echtzeit weiter.
- **Lokale Versionierung:** Geschwindigkeit und Testzeit separat in SharedPreferences, individueller simulierter Erstellungszeitpunkt je Pflanze im kompakten v2-Zustandsformat (v1 bleibt lesbar).

### Bestehende Funktionen
Startseite Beet/Küche/Garten, Stufenauswahl 1–10, 300×300-Pixelstift, 40 Farben, Lückenerkennung, Füllen, Undo/Redo, 20%-Overlay der vorherigen Stufe, Graustufen-/eigene Todesform, 800×600-Beet, Long-Press-Verschieben, Gießkanne, vier Pflege-Schwierigkeitsgrade, offline gespeicherte Fristen und Lebenszyklus.

### Installation
1. [GitHub Actions](https://github.com/shayohekmat-wq/Zimt-Lab-1/actions) → neuesten erfolgreichen Android-Build.
2. Artifakt \`Garten-und-Kueche-v0.0.7-Mehrkonturen-Zeitraffer-APK\` (ZIP) laden, APK darin installieren. Im Chat wird die **einzelne APK direkt verlinkt**.
3. **Parallele Installation**: eigene Paketkennung \`de.naehe.app.gartenkueche.lab7\` und eigener App-Name \`Garten & Küche 0.0.7\`.

**Android-Sandbox:** Pflanzen und Entwürfe aus 0.0.5 erscheinen nicht automatisch in 0.0.7. Bitte vorherige Test-App nicht löschen. Export/Import zwischen Lab-Versionen und Partnergeräten kommt später. Spielzeitraffer ist **Testdatenzeit** und wird bei späteren ZIMT-Geschenken nicht als echte Zeit übertragen.

### Qualitätssicherung
GitHub Actions: Java-Tests für sämtliche vier Pflege-Schwierigkeitsgrade, Evolution/Regression/Tod, separate Konturregionen (inklusive 20 Konturen), unabhängige kleine Lücken, Bounding-Box-Geometrie, 1×–80×-Zeitberechnung und Taggrenzen; danach Android-APK-Build und Anwendung-ID-Prüfung. Ein grüner Workflow ersetzt keine echten Gerätetests auf Android 6 bzw. deinem Android-Handy.

### Weitere Entwicklungsarbeit
- Stabiler \`.zimtplant\`-Import/Export, Definition-/Instanz-Migration und Geschenkefunktion.
- Performance- und ANR-Profile auf echten Geräten, insbesondere alte Android-Versionen.
- Optionaler „+1 Testtag“-Knopf: bei 80× entspricht ein simulierter Tag **18 echten Minuten**.
- Verbesserte Vorschau erkannter Konturen vor dem Ausmalen und differenziertere 2-Finger-Gesten.
- Küche/Garten sind weiterhin Platzhalter.

**Referenzen:** [Build-Plan 0.0.7](BUILD_PLAN_0.0.7_MULTI_CONTOUR_TIMELAPSE.md) · [Performance-Plan 0.0.5](PERFORMANCE_MASTERPLAN_0.0.5.md) · [Masterplan 0.1.0](BUILD_PLAN_0.1.0.md).

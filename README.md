# Garten & Küche — ZIMT Lab 1

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

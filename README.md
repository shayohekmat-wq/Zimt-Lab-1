# Garten & Küche — ZIMT Lab 1

Eigenständige Android-Test-App für spätere Integration in ZIMT. **Java + Android Canvas**, Android **6.0+ (API 23)**, offline-first und separat installierbar. ZIMT bleibt unverändert.

## Build 0.0.6 — Mehrkonturen & Zeitraffer

Diese Version baut auf der Performance-Optimierung 0.0.5 auf.

### Neu in 0.0.6
- **Mehrere Konturen pro Pflanze:** Eine Evolutionsstufe darf viele getrennte Formen enthalten. Der Kontur-Detektor sucht alle tatsächlichen abgeschlossenen Innenbereiche; eine spätere zweite Kontur ersetzt nicht die erste. Nach der Prüfung zeigt der Farbeditor die Anzahl erkannter Innenregionen an.
- **Mehrere Formen = eine Pflanze:** Alle erkannten, bemalten Pixel aus separaten Konturen werden innerhalb derselben 300×300-Stage gespeichert. Im Beet ist es ein einziges gemeinsames Objekt, das sich zusammen verschieben lässt.
- **Kleine Lücken:** Kurz geschlossene Lücken (bis vier logische Rasterpixel) können auch dann noch erkannt werden, wenn schon eine andere gültige Kontur existiert.
- **Exakter enger Zuschnitt:** Im Beet werden Sprites je Evolutionsstufe auf die tatsächlich belegten Pixel zugeschnitten. Eine 8×12-Pixel-Zeichnung erscheint mit 8×12 logischen Pixeln — nicht auf 300×300 vergrößert. Die Positionen bleiben durch die aufbewahrten Originalkoordinaten über Evolutionsstufen stabil. Auch die Todesform wird so behandelt.
- **Zeitraffer-Karte unterhalb des Beets:** Dunkle, violett akzentuierte Oberfläche, Slider **1×–20×** mit Geschwindigkeit und Anzeige, wie lange ein Spieltag in Echtzeit dauert.
- **Pflanzenalter:** Tippe im Beet auf eine Pflanze. Unter dem Regler wird ihr persönlicher Lebenstag als **„Tag N“** angezeigt. Tag 1 beginnt bei der Aufnahme ins Beet; jeder weitere Tag beginnt nach vollen 24 *simulierten* Stunden.
- **Virtuelle Testuhr:** Slider beschleunigt Pflegefristen, Wachstum, Rückentwicklung und Todesereignisse. Gießen erfordert **weiterhin fünf echte Sekunden** ununterbrochener Berührung.
- **Zeituhr ohne Sprünge:** Der simulierte Zeitpunkt bleibt bei einem Geschwindigkeitswechsel konsistent. Im Testtempo >1× pausiert die Simulation, wenn das Beet nicht geöffnet ist; bei 1× läuft die normale Echtzeit weiter.
- **Lokale Versionierung:** Geschwindigkeit und Testzeit separat in SharedPreferences, individueller simulierter Erstellungszeitpunkt je Pflanze im kompakten v2-Zustandsformat (v1 bleibt lesbar).

### Bestehende Funktionen
Startseite Beet/Küche/Garten, Stufenauswahl 1–10, 300×300-Pixelstift, 40 Farben, Lückenerkennung, Füllen, Undo/Redo, 20%-Overlay der vorherigen Stufe, Graustufen-/eigene Todesform, 800×600-Beet, Long-Press-Verschieben, Gießkanne, vier Pflege-Schwierigkeitsgrade, offline gespeicherte Fristen und Lebenszyklus.

### Installation
1. [GitHub Actions](https://github.com/shayohekmat-wq/Zimt-Lab-1/actions) → neuesten erfolgreichen Android-Build.
2. Artifakt \`Garten-und-Kueche-v0.0.6-Mehrkonturen-Zeitraffer-APK\` (ZIP) laden, APK darin installieren. Im Chat wird die **einzelne APK direkt verlinkt**.
3. **Parallele Installation**: eigene Paketkennung \`de.naehe.app.gartenkueche.lab6\` und eigener App-Name \`Garten & Küche 0.0.6\`.

**Android-Sandbox:** Pflanzen und Entwürfe aus 0.0.5 erscheinen nicht automatisch in 0.0.6. Bitte vorherige Test-App nicht löschen. Export/Import zwischen Lab-Versionen und Partnergeräten kommt später. Spielzeitraffer ist **Testdatenzeit** und wird bei späteren ZIMT-Geschenken nicht als echte Zeit übertragen.

### Qualitätssicherung
GitHub Actions: Java-Tests für sämtliche vier Pflege-Schwierigkeitsgrade, Evolution/Regression/Tod, separate Konturregionen (inklusive 20 Konturen), unabhängige kleine Lücken, Bounding-Box-Geometrie, 1×–20×-Zeitberechnung und Taggrenzen; danach Android-APK-Build und Anwendung-ID-Prüfung. Ein grüner Workflow ersetzt keine echten Gerätetests auf Android 6 bzw. deinem Android-Handy.

### Weitere Entwicklungsarbeit
- Stabiler \`.zimtplant\`-Import/Export, Definition-/Instanz-Migration und Geschenkefunktion.
- Performance- und ANR-Profile auf echten Geräten, insbesondere alte Android-Versionen.
- Optionaler „+1 Testtag“-Knopf: bei 20× entspricht ein simulierter Tag immer noch **72 echte Minuten**.
- Verbesserte Vorschau erkannter Konturen vor dem Ausmalen und differenziertere 2-Finger-Gesten.
- Küche/Garten sind weiterhin Platzhalter.

**Referenzen:** [Build-Plan 0.0.6](BUILD_PLAN_0.0.6_MULTI_CONTOUR_TIMELAPSE.md) · [Performance-Plan 0.0.5](PERFORMANCE_MASTERPLAN_0.0.5.md) · [Masterplan 0.1.0](BUILD_PLAN_0.1.0.md).

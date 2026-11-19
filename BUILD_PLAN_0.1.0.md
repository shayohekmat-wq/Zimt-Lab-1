# Garten & Küche — Master-Build-Plan 0.1.0

**Stand:** 19. November 2026 · **Status:** Spezifikation, keine fertige APK  
**GitHub:** \`shayohekmat-wq/Zimt-Lab-1\` · **ZIMT-Hauptprojekt:** \`shayohekmat-wq/Test\`

## 1. Grundsätze und verifizierte technische Basis

Im bestehenden ZIMT-Projekt nachgesehen: native Java-Android-App, klassische Views/Canvas, Gradle Kotlin DSL, Android-Gradle-Plugin 8.7.3, Gradle 8.9 im CI, Java 17, minSdk 23, compileSdk 35, targetSdk 34. Bereits vorhanden: Firebase/Auth/Firestore in ZIMT, aber der Lab-Build darf davon **nicht abhängen**.

- Ziel für das Lab: **Android 6.0 / API 23** und neuer; performant auch auf Moto G6 und anderen älteren Geräten.
- Separat installierbare App \`de.naehe.app.gartenkueche.lab1\`; ZIMT bleibt unverändert.
- Kein Unity, Flutter, Compose, WebView oder 3D-Framework: **Java + Android Canvas** für Pixel-Art, kleine Dependency-Oberfläche.
- Modulare Engine ohne Android-Klassen für spätere ZIMT-Integration.
- Offline-first, keine Konten, kein Internet oder Backend für v0.1.0.
- Eigenständiger GitHub-Actions-Debug-APK-Build, automatisierte Unit- und Speichertests.
- UI-Theme später unabhängig auf ZIMT/charmingX4 anpassbar; nicht in Pflanzendaten einbacken.

## 2. Umfang 0.1.0 / Scope

| Funktion | Abnahme |
|---|---|
| Start | Drei verschieden große Kacheln **Beet**, **Küche**, **Garten**; nur Beet aktiv, andere mit Platzhalter |
| Wizard | Vorab Auswahl **1–10 Evolutionsstufen** |
| Kontur-Editor A | Exaktes **300×300**-Logikpixelraster, ein dunkelvioletter Pixelstift, geschlossene Kontur verpflichtend |
| Farbeditor B | Nur Pixel im abgeschlossenen Inneren bemalbar; **40 feste Farben**, Fülleimer/Radierer, Undo |
| Weitere Stufen | Nach jeder fertigen Stufe neue A/B-Zeichenfolge; vorheriges Farbbild erscheint **20 % transparent** als nicht-editierbare Vorlage |
| Todesform | Erstes lebendiges Bild automatisch in Graustufen; Wahl **Passt** oder **Selber zeichnen** (separate A/B-Folge) |
| Stufenregeln | Schwierigkeit pro Stufe: Pflegeleicht, Medium, Profi, Diva; Varianzwerte werden einmal pro jeweiligem Pflegefenster gezogen und gespeichert |
| Beet | **800 px breit × 600 px hoch**, automatischer Import der ersten Stufe, Pflanzen per Long-Press frei repositionierbar, Koordinaten dauerhaft speichern |
| Gießen | Gießkanne auf Pflanzen-Hitbox halten: **>= 5.000 ms** ununterbrochen → exakt 1 Gießvorgang; längeres Halten zählt nicht mehrfach |
| Leben | Erfolgreiche Pflege + Zeit → Stufenaufstieg; Verletzung → eine Stufe zurück; auf Stufe 1 bleibt sie, nach zwei dortigen Verstößen dauerhaft tot |
| Speicherung | Alle Grafiken, Konturen, Regeln, Zufallsparameter, Fortschritte, Gießereignisse und Beetpositionen lokal und dauerhaft |

**Nicht enthalten:** Partner-Sync/Verschenken, Cloud, Notifications, tatsächliche Funktionen in Küche/Garten. Transferformat bereits vorsehen.

## 3. Erstellungsfluss

Home → Beet → „Pflanze erstellen“ → Stufenzahl 1..10 → für jede Stufe:
1. **A:** auf 300×300 geschlossene Kontur zeichnen;
2. **B:** Kontur beibehalten, nur Innenpixel mit 40 Farben gestalten;
3. **Schwierigkeit:** Kategorie pro Stufe wählen;
4. Falls weitere Stufen: vorherige vollständig gemalte Stufe als 20%-Overlay zeigen; Raster darüber ist wieder leer; A und B wiederholen.

Danach Todesform von Stufe 1 als Graustufen-Vorschlag anzeigen; „Passt“ / „Selber zeichnen“. Ergebnis prüfen und als **PlantDefinition** speichern, **PlantInstance** in Beet erzeugen.

Alle Zwischenstände per Autosave vor Verlust durch Prozessende schützen; kein Löschen durch Displaydrehung.

## 4. Pixelgrafik-Engine

- Logische Rasterkoordinaten x/y jeweils 0..299; klare Trennung von Bildschirmpixeln und Rasterpixeln; Zoom/Pan mit inverser Transformationsmatrix.
- Android \`Bitmap\`/\`Canvas\`, nearest-neighbor, \`Paint.setFilterBitmap(false)\`; bei hoher Vergrößerung Zelllinien rendern, bei kleineren Zoomstufen adaptive grobe Linien; kein Glätten der Pixelart.
- Beim Zeichnen Punkte lückenlos durch Bresenham-Linien zwischen Touch-Samples verbinden, nur einen Pixelbreite im logischen Raster (visuell größer darstellbar).
- Konturlayer getrennt von Farblayer. Der Start und das Ende müssen verbunden sein; zusätzliche Validierung mittels 4-Nachbar-Floodfill: genau ein eindeutiger Innenbereich, keine offene Verbindung zum äußeren Bildrand, keine Selbstkreuzungen/zusätzlichen Inseln in v0.1.0. Fehlerbereich visuell markieren, Weiter-Knopf blockieren.
- Konturfarbe dunkelviolett als Editor-UI-Token; Konturmaske farbunabhängig speichern.
- B-Modus: Malen und Eimer nur innerhalb der von der Kontur eingeschlossenen Pixel; Kontur ist geschützt. 40 konstante ARGB-Paletteneinträge. Undo/Redo als kompakte Stiftoperationen/Diffs.
- Overlay aus der **fertigen vorherigen Stufe** (Kontur + Farbe), Deckkraft 0.20; niemals Bestandteil der neuen Bilddaten.
- Bilder als verlustfreie PNG oder indizierte Palettenbitmap, Konturmaske separat; keine JPEG-Artefakte. 300×300×4 = 360.000 Bytes pro unkomprimierter ARGB-Layer.
- Feste Ursprungsanker am Pflanzenfuß, damit unterschiedliche Pflanzenhöhen nicht „springen“.

## 5. Pflege- und Evolutionswerte

Alle **±**-Werte stehen für eine gleichverteilte ganzzahlige Zufallsauslosung im genannten Bereich, außer der unten erklärten Diva-Korrektur. Ausgeloste Werte für eine aktive Phase/Frist werden **persistiert** und niemals beim Zeichnen oder App-Neustart neu bestimmt.

| Modus | Mindestmenge | Gießfrist / Fenster | Höchstmenge | Evolution |
|---|---|---|---|---|
| Pflegeleicht | 1× | 14 ± 2 = **12–16 Tage** | 10 ± 1 = **9–11 pro Tag** | 4 ± 1 = **3–5 Tage** |
| Medium | 1× | 9 ± 2 = **7–11 Tage** | 7 ± 2 = **5–9 pro Tag** | 6 ± 2 = **4–8 Tage** |
| Profi | 2 ± 1 = **1–3×** | 6 ± 1 = **5–7 Tage** | **5 im Fenster** | 8 ± 3 = **5–11 Tage** |
| Diva | 1× | 4 ± 5 = **-1 bis 9 Tage mathematisch** | **5 im Fenster** | 11 ± 7 = **4–18 Tage** |

**Sicherheitsregel für Diva:** 0 oder negative Tage als Frist sind ungültig. Vorschlag: originalen Zufallswert erneut ziehen, bis 1..9 Tage entsteht. Diese **Rejection-Sampling**-Regel vermeidet Übergewicht von 1 Tag durch Clamping. Noch vom Nutzer zu bestätigen.

**Offene Regelentscheidungen (nicht stillschweigend als fix betrachten):**
1. **Was bedeutet „Schwierigkeit einer Stufe erreichen“?** Modell-Vorschlag: Pflegekategorie der **aktiven Stufe** regelt ihre Erhaltung und den Aufstieg *aus* ihr; auf der maximalen Stufe regelt sie nur die Erhaltung. Alternative: Regelsatz der Zielstufe. Entscheidung vor finalem Gameplay.
2. **Tägliches Limit:** für Pflegeleicht/Medium rollierende 24 Stunden statt Kalendertage, um Zeitzonen-Tricks zu vermeiden. Zu bestätigen.
3. **Profi/Diva-Limit:** Höchstmenge 5 **pro mehrtägigem Pflegefenster**, nicht pro Kalendertag. Zu bestätigen.
4. **Mindestfrist:** aneinandergereihte, beim Start jedes Fensters neu ausgeloste Fristen. Unterbewässerung nur einmal pro abgelaufenem Fenster prüfen. Zu bestätigen, ob stattdessen rollierend seit letztem Gießen.
5. **Stufe 1 und Todeszähler:** Vorschlag zwei Verstöße auch bei einer **frisch erstellten** Stufe 1; Strikes bei erneutem Aufstieg/Rückschritt zurück auf 0. Alternative: Strafkonto erst nach einer Regression aktiv.
6. **Evolutionszeit vs. Mindestgießen:** Aufstieg nach zufälliger Zeit **nur wenn** die nötige Pflege bereits erfüllt und keine Frist verletzt ist; sonst wartet die Stufe, ohne allein deswegen zu sterben, bis eine echte Pflegefrist verletzt ist.
7. **Überwässerung:** jede neue separat abgeschlossene 5-s-Gießaktion über Maximalwert als einzelner Verstoß; das fortgesetzte Halten derselben Aktion nie doppelt zählen.
8. **Nach Regression:** Fortschritt und aktuell gültige Pflegefristen der neuen aktiven Stufe beginnen neu; alte Historie bleibt nachvollziehbar.

**Wichtig:** Die Evolutionszeiten (3–18 Tage) können kürzer als die Pflegefristen sein. Das Gameplay muss sowohl eine Mindestanzahl an Gießvorgängen als auch die Evolutionszeit für einen Aufstieg prüfen; nicht einfach nach Frist automatisch entwickeln.

## 6. PlantEngine: deterministische Zeit- und Zustandsmaschine

Zustände: DRAFT → ALIVE(stage 1..N) → DEAD. DEAD ist endgültig und rendert das Todesbild.

- \`advance(nowUtc)\`: seit letzter Prüfung abgelaufene Fenster **chronologisch** aufarbeiten (auch nach Wochen App-Pause). Jedes Fenster hat eine eigene ID und \`evaluatedAt\`, damit wiederholtes Prüfen nie doppelt bestraft.
- Unterwässerung: ein vollständig verstrichenes Pflegefenster enthält zu wenige gültige Gießereignisse → 1 Regelverstoß.
- Überwässerung: ein neuer Gieß-Event führt zur Überschreitung der für diese Kategorie geltenden Höchstmenge → 1 Regelverstoß für **dieses** Ereignis, nie bei Wiederaufnahme erneut.
- Stufe>1 bei einem Regelverstoß: aktuelle Stufe um 1 reduzieren, neue Zeit-/Pflegephase starten, alte Historie behalten. Stufe1 beim Verstoß: 1 Strike; bei 2 Strikes → DEAD. Stufe nie <1.
- Aufstieg nur wenn \`stage < maxStage\`, Evolutionsdeadline erreicht, Mindestpflege erfüllt, keine offenen Verstöße. N=1 bleibt N=1.
- Höchste Evolutionsstufe bleibt pflegebedürftig; Regression/Tod weiterhin möglich.
- Gültige Gieß-Events mit Timestamp **<= Deadline** zuerst verarbeiten, dann Fristversäumnis und Entwicklung prüfen, um Grenzwert-Rennen auszuschließen.
- Alle Pflege- und Evolutionswerte gehören zur **Instanz**, nicht bloß zur Definition. Bei Übertragung der Definition beginnt neuer Lebenszyklus mit frischen Auslosungen.
- Zeiten als UTC-Epoch-Millis; „Tag“ als 24h statt lokaler Kalendertag. Bei App-Pause keine ständig laufende Hintergrundservice-Abhängigkeit; Auswertung beim Öffnen/Interaktion.
- Manipulation der lokalen Uhr ist offline nicht vollständig vermeidbar: in der laufenden Sitzung für Gießdauer \`SystemClock.elapsedRealtime()\` verwenden und für Weltzeit \`System.currentTimeMillis()\`; größere Rücksprünge protokollieren, niemals negative Fristen erzeugen. Verlässliche gemeinsame Zeitbasis erst beim späteren Partner-Sync.

## 7. Gießkanne und Beet

- Beet exakt **800 × 600 logische Pixel**, skaliert mit letterbox/pan bei anderer Displayform; 800 ist Breite, 600 Höhe.
- Pflanzen als Sprite mit x/y, bottom-center-Anker, z-Reihenfolge, Alpha-/Touchhitbox, UUID. Beim Erstellen automatisch an freie Standardposition setzen.
- Long-Press ca. 450 ms (UI-Parameter) → Drag-Modus; mit Finger verschieben, Position innerhalb zulässiger Grenzen und nach Loslassen persistent speichern.
- Gießkanne: draggable; erst nach **5.000 ms ununterbrochener Position innerhalb der Hitbox derselben Pflanze** ein einziges Gießereignis. Halten 5/20/60 s = je **ein** Ereignis; Verlassen/Zurücksetzen vor 5 s zählt nicht; für einen weiteren Vorgang Ziel zunächst verlassen und erneut betreten. Gießen und Pflanzenziehen über getrennte Modi.
- Action-ID (UUID) und Zustand IDLE → HOVERING → COUNTED → RESET sichern, damit Touch-Move oder Timer-Ticks das Ereignis nicht mehrfach anlegen.
- Bei App-Wechsel, Touch-Abbruch oder Display-Sperre unvollständige 5-s-Gießaktion verwerfen.
- Frame- und Bitmap-Caching für ältere Geräte; keine 90.000 separaten Views im Raster.

## 8. Datenformat, Dauerhaftigkeit und Geschenke

Zentrale klare Trennung:
- \`PlantDefinition\`: UUID, formatVersion, creation UTC, 1..10 \`PlantStage\` mit Pixelbild, Konturmaske, Schwierigkeit/Regelschema, zusätzliche DeathArtwork.
- \`PlantInstance\`: UUID, definitionId, aktueller Status, Stufe, Beet-x/y, Anker, Zeitmarken, Zufallsauslosungen, CareWindow-Liste, gezählte WaterEvents, Strike-Zahl, Revision.
- \`CareWindow\`: UUID, Stufe, Fensterstart/-ende, konkrete Zufallswerte, versionierte Regel und \`evaluated\`-Status.
- \`WaterEvent\`: UUID + inputGestureId, plantInstanceId, UTC-Zeit, duration>=5s, Fenster-ID.
- \`PlantEvent\`: UUID, Typ (watered, evolved, regressed, died, underwatered, overwatered), Auslöser, vorher/nachher, UTC-Zeit.

Implementierung: Android \`SQLiteOpenHelper\` mit Transaktionen, versionierter DB und Upgrade-Migrationen; PNG/Bitmasken in Blob oder atomar gespeicherten lokalen Dateien mit DB-Referenzen. Entwürfe nach Stiftaktionen autospeichern. Alle Events idempotent per UNIQUE-Index auf inputGestureId bzw. Window-ID + Auswertungstyp.

**Geschenk-/Export-Vertrag bereits ab v0.1.0 definieren:** \`.zimtplant\` als ZIP mit \`manifest.json\` (formatVersion, definitionId, Hashes, Schutzlimits), \`stages/01..10/{outline,artwork}\`, \`death/{outline,artwork}\`, \`rules.json\`. Optional später \`instance.json\` und \`history.json\` für bewusstes Übertragen eines laufenden Lebewesens. Validierung auf Dateigrößen, unzulässige Pfade und beschädigte Dateien. Die **UI zum Verschenken/Importieren** ist nicht im 0.1.0-Build, nur Datenvertrag und Codecs/Rundtriptests.

Im späteren ZIMT-Integrationsschritt erst aktivieren: Besitzzuordnung, Sender-/Empfänger-Protokoll, doppelte Geschenk-IDs, Zeitbasis, Bestätigung und Synchronisationskonflikte. Keine Android-Paket-Sandbox direkt auslesen.

## 9. Umsetzung als überprüfbare Milestones

1. **M0 Projektbasis:** Android-Studio/Gradle-Struktur Java 17 + minSdk23, separate App-ID, GitHub Actions APK, Launcher, drei Kacheln, Beet-Placeholder.
2. **M1 Pixel-Editor:** 300×300 Canvas, präzises Touchmapping, Konturvalidierung, 40 Farben, Innenmasken, Undo/Redo, Zoom/Pan.
3. **M2 Multi-Stage-Wizard:** Auswahl 1..10, Schritt A/B je Stufe, 20%-Overlay, Todesform/Graustufen und freies Zeichnen, Autosave.
4. **M3 Modell und Speicherung:** klare Definition/Instanz-Trennung, SQLite, Rules, Zufallsziehung, Datenmigration, PNG-Roundtrip.
5. **M4 Beet-Interaktion:** 800×600 Canvas, Auto-Import, mehrere Pflanzen, Long-Press/Drag, Gießkanne >=5s, deduplizierte Events.
6. **M5 Lebenszyklus:** Echtzeit/Offline-Catch-up, 4 Schwierigkeitsgrade, Evolution, Regression, Stufe1-Strikes, Tod.
7. **M6 QA und Release-Kandidat:** API23/API26/aktuelle Android-Versionen, Geräteperformance, persistente Daten, Codec-Prototyp, APK und Dokumentation.

**Gates:** jeder Milestone muss auf GitHub Actions durchbauen; M2 nur bei pixelgenauen Stufendaten; M3 nur bei testbarer RNG-/DB-Idempotenz; M5 nur bei vollständigen Zustandsmaschinen-Tests; M6 nur ohne Datenverlust.

## 10. Konkrete Tests / Akzeptanzfälle

- Kontur offen / berührt Anfang / selbstkreuzend / mehrfach getrennt / Rand offen / diagonale Verbindung; Innenmaske korrekt, jede Rasterzelle maximal 299.
- 40 Palettefarben, Farbe bleibt nach Neustart bitgenau; 10 Stufen und DeathArtwork persistieren; 20%-Overlay wird nicht ins Bild gebrannt.
- 5.000 ms exakt = 1 Gießen, 4.999 ms = 0, 60.000 ms = weiterhin 1, erneutes Betreten = weiterer Gießvorgang. Wechsel der Pflanze vor Ablauf setzt Timer zurück.
- Zufall: Pflegeleicht 12..16/9..11/3..5, Medium 7..11/5..9/4..8, Profi 1..3, 5..7 Tage, max5, 5..11 Evolution, Diva 1..9 Tage, max5, 4..18 Evolution; Werte bleiben nach Neustart gleich.
- Zweite Bewässerung bei max1 in derselben relevanten 24h erzeugt genau einen Verstoß; mehrfacher Renderer-Aufruf keinen weiteren.
- Von Stufe 4 Regelverstoß →3; von Stufe 1 zweimal Verstoß → DEAD; keine Stufe 0; maxStage=1 und 10 korrekt.
- 30 Tage offline → alle **tatsächlich** verpassten Intervalle einmalig ausgewertet, kein Mehrfachabzug durch wiederholtes Öffnen.
- Prozesskill beim Zeichnen, Stromunterbrechung beim Speichern, SQLite-Migration, Geräteuhr zurückgestellt, Zeitzonenwechsel, Sommer-/Winterzeit.
- Android 6/API23, Moto G6/API26+ wenn verfügbar, Nothing Phone/aktuell; niedrigere RAM-/CPU-Last und stabile Touchbedienung.
- Export-Manifest und Bitmap-Roundtrip rein programmatisch testbar, auch ohne sichtbare Exportoberfläche.

## 11. ZIMT-Integrationsschutz

Hauptrepository unverändert lassen. \`plant-core\` ohne Firebase-/Android-UI-Abhängigkeiten halten, Interfaces für Store/Clock/Random/Transfer, unabhängig themebare Lab-Oberfläche. Später entweder gemeinsame Gradle-Library oder Source-Transfer mit Tests; bestehende ZIMT-Features nicht duplizieren. Die Java-/Android-6-Kompatibilität ist ein **Designkriterium jeder neuen Abhängigkeit**, nicht eine nachträgliche Portierung.

## 12. Definition of Done für 0.1.0

Eine unabhängig installierbare Android-6+-APK ermöglicht vollständige manuelle Erstellung einer 1–10-stufigen Pixelpflanze, separate Todesform, pro Stufe Schwierigkeit, lokalen Autoimport in 800×600-Beet, positionsgenaues Verschieben, Gießen mit 5-s-Schwelle, deterministisches Wachstum/Regression/Tod, verlustfreie lokale Speicherung und geprüfte APK-CI-Tests. Spätere Geschenke durch versioniertes Datenformat vorbereitet; keine Networking- oder ZIMT-Abhängigkeiten.

**Offene Regeln sind in Abschnitt 5 ausdrücklich als vorläufig dokumentiert.** Erst wenn diese bestätigt sind, gelten sie als verbindliche Gameplay-Semantik.

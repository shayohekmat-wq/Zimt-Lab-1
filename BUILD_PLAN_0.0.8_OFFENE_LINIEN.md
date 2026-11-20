# Garten & Küche — Build-Plan 0.0.8: Stiele und offene Linien erhalten

**Status:** Umsetzung für Android-Lab 0.0.8 · **Basis:** 0.0.7 (Mehrkonturen, Crop, 1–80× Zeitraffer).
**Auslöser:** Reale Geräteaufnahme zeigt einen Setzling mit drei erkannten geschlossenen Regionen, aber ein frei gezeichneter einpixeliger Stiel verschwindet nach „Kontur prüfen“.

## Reproduzierter Fehler

Im bisherigen \`ContourDetector.detectEnclosure()\` wurde ein \`boundary\`-Array nur für solche Konturpixel angelegt, die direkt neben einer erkannten Innenregion lagen. Das Ergebnis ersetzte in \`PlantArt.validate()\` die vollständige Zeichnung via \`System.arraycopy(result.outline, ...)\`. Somit gingen alle Striche verloren, die **keine Grenze einer gefüllten Region** waren, z. B. Stängel, Blattadern, lose Verbindungslinien, verzweigte Zweige und frei stehende Akzente.

## Verbindliche neue Regeln

1. **Sichtbares Strich-Layer bleibt vollständig erhalten.** Jeder gesetzte Konturpixel bleibt Bestandteil der Pflanzengrafik – auch offene Linien, dünne Stiele, Äste oder einzelne nicht geschlossene Konturen. Keine automatische Entfernung vorhandener Pixel bei der Konturprüfung.
2. **Separate Füllmaske:** Nur tatsächlich geschlossene Innenregionen dürfen per Stift oder Fülleimer koloriert werden; auf den Linien bleiben die dunkelvioletten Konturpixel sichtbar. Offene Formen erzeugen ohne geschlossene Fläche keine zusätzliche Füllregion.
3. **Mehrkonturen bleiben erhalten:** Ein, drei oder zwanzig geschlossene Formen plus beliebig viele offene Stiele zählen insgesamt als **eine Pflanze**. Die Anzeige „N geschlossene Bereiche erkannt“ zählt nur die befüllbaren Regionen, nicht die Zahl aller sichtbaren Linien.
4. **Mindestens eine geschlossene Innenregion** für den Übergang zu Schritt B. Rein offene Zeichnungen werden weiterhin nicht fälschlich als ausmalbare Pflanze akzeptiert.
5. **Kurze Lücken** in einer noch nicht geschlossenen Kontur dürfen weiterhin im Prüfmodus bis zu vier Rasterpixel repariert werden. Dabei werden **ergänzend** Verbindungspixel hinzugefügt, niemals bereits gezeichnete Stiele oder Äste gelöscht.
6. **Export/Bounding Box:** Pixelgenaue Bounding Box basiert auf der Vereinigung **sämtlicher sichtbaren Konturpixel und aller ausgemalten Innenpixel**, einschließlich einzelner dünner Stiele außerhalb geschlossener Bereiche. Rendering in 1:1 Größe im Beet, ohne künstliches Hochskalieren.
7. **Zusammengehörigkeit:** Sämtliche Striche und gefüllten Regionen werden beim Platzieren, Gießen, Verschenken (später) und Verschieben als ein einziges Pflanzenobjekt behandelt; die Abstände der Linien bleiben unverändert.
8. **Alle Evolutionsstufen und Todesform:** Genau dieselbe Logik für jede der 1–10 Stufen sowie selbst gezeichnete und graue Todesgrafik. Entwürfe, Undo/Redo, 20%-Overlay und lokales GZIP-/State-Datenformat bleiben kompatibel.
9. **Performance:** Konturprüfung weiterhin asynchron auf dem existierenden IO-Executor; keine teure Neuberechnung auf jedem Frame, keine neue Bitmap pro Stiftbewegung.
10. **Installation:** Release \`0.0.8\` mit einzigartiger Android-Paketkennung \`de.naehe.app.gartenkueche.lab8\`, kompatibel mit Android API 23+, parallel zu ZIMT und bisherigen Lab-APKs.

## Implementierungsaufgaben

**M1 — Ursache korrigieren.** In \`ContourDetector\` die vollständige Strichmaske als \`Result.outline\` zurückgeben. Bei optionalem Lückenschluss eine Obermenge der ursprünglichen Striche zurückgeben; die Außen-Floodfill-Füllmaske bleibt davon getrennt.

**M2 — Pipeline verifizieren.** \`PlantArt.validate\`, \`bitmap()\`, \`croppedBitmap()\`, \`grayCopy()\`, \`write()/read()\` dürfen Originalstriche nicht verlieren. Nach der Konturprüfung werden lediglich erkannte Lückenschlusspixel ergänzt. Schutz gegen Verfärbung beim „Alle füllen“ und gegen ungewolltes Bemalen offener Außenbereiche.

**M3 — Regressionstests.** Eine geschlossene Blattkontur mit offenem Stiel, mehrere geschlossene Blätter plus frei stehender Stiel, externe Kanten und ein zusätzlich gezeichneter Zierstrich; alle nach Detektion sichtbar, Innenmaske weiterhin ausschließlich in den geschlossenen Regionen. Der Crop muss den längsten Stiel einschließen. Tests für klassische mehrteilige Konturen, kleine Lücken und reine offene Zeichnungen erhalten.

**M4 — UI-Texte.** Im Kontur- und Farbeditor verständlich erklären: „Offene Linien wie Stiele bleiben sichtbar; ausmalbar sind nur geschlossene Innenbereiche.“ Die Regionszählung weiterhin getrennt anzeigen.

**M5 — Build & Distribution.** Java-Regressionstests, Android-Build, APK-Paketkennung im CI prüfen. Debug-APK **als einzelne Datei direkt im Chat** verlinken (kein GitHub-ZIP als einziger Download). Neue App ist getrennt von 0.0.7, und hat daher eine eigene Datensandbox.

## Akzeptanzkriterien

- Drei geschlossene Formen werden erkannt, **plus ein einzelner frei stehender einpixeliger Stiel bleibt dunkelviolett sichtbar**.
- Fülleimer färbt drei Innenbereiche, nicht den Stiel und nicht den Hintergrund.
- Nach Speichern/Neuöffnen bleiben Stiel + alle Formen sichtbar.
- Crop und Beet enthalten den Stiel vollständig und das gesamte Arrangement ist ein verschiebbares Objekt.
- Automatischer Lückenschluss entfernt keine bereits gezeichneten Striche.
- Version 0.0.8 kompiliert, Test-Suite und Paket-ID-Prüfung sind grün; echter Smartphone-Performance- und Anzeige-Test folgt.

**Nicht in diesem Bugfix:** automatische Konturlinienkolorierung, SVG/Vektor-Export, Datenmigration zwischen verschiedenen Android-Lab-Installationen, größere Gameplay-Änderungen und die Änderung des Zeitraffers von 1–80×.

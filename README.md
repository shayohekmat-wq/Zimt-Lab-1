# Garten & Küche — ZIMT Lab 1

**Native Android-App als eigenständiges Labor für spätere Integration in ZIMT.** Java/Canvas, Android **6.0+ / API23**, offline-first, installierbar neben dem Hauptprojekt. Das bestehende ZIMT-Repository \`shayohekmat-wq/Test\` bleibt unverändert.

## Entwicklungsstand: 0.0.3 (parallel installierbarer Fix nach 0.0.2) (Funktionsprototyp; kein abgeschlossener 0.1.0-Release)

### Bereits implementiert
- **Startseite:** Kacheln Beet (aktiv), Küche und Garten (Platzhalter).
- **Stufenwizard:** 1–10 individuelle Evolutionsstufen auswählen, Kontur + Farbbild pro Stufe, Schwierigkeitsgrad separat wählen.
- **Pixel-Art-Editor:** 300×300 logische Pixel, dunkelviolette Kontur, automatische Prüfung auf verbundenen geschlossenen Umriss und eindeutige Innenfläche, 40 Farbwerte, Einzelpixelstift, Fülleimer, alles füllen, Radierer, Undo/Redo, Zoom + Zwei-Finger-Pan.
- **Overlay:** Vorherige vollständige Stufe erscheint beim Kontur-/Farbeditor der Folgestufe mit **20 % Deckkraft**, ohne in neue Bilddaten einzufließen.
- **Todesform:** Graustufen-Vorschlag aus Stufe 1 inkl. Kontur, oder eigens zeichnen und ausmalen.
- **Entwürfe:** Automatische Sicherung im privaten App-Speicher; gespeicherten Entwurf fortsetzen; Bestätigungsdialog beim Ersetzen.
- **Beet:** 800×600 logisches Raster, fertiggestellte Pflanzen erscheinen automatisch, lange drücken und ziehen zum Verschieben; Koordinaten werden gespeichert.
- **Gießkanne:** Aktivieren, über Pflanze bewegen und mindestens fünf Sekunden halten → einmaliger Gießvorgang; weiter halten zählt **nicht mehrfach**; neue Berührung nach Verlassen zählt erneut. Unterbrochene Gießaktionen werden bei App-Pause abgebrochen.
- **Pflege-Engine:** Pflegeleicht, Medium, Profi, Diva, reproduzierbare ganzzahlige Zufallswerte je Zeitfenster, fest gespeicherte Regeln, lokal nachgeholte Fristen nach App-Pausen, Stufenaufstieg/-rückentwicklung, zwei Regelverstöße auf Stufe 1 → endgültiger Tod. Verfügbare Regeln werden im Beet angezeigt.
- **Speicherung:** Versionierte lokale, GZIP-komprimierte Pflanzen-/Entwurfsdaten samt Konturmasken, Artwork, Schwierigkeiten, Pflegefenstern, Gießhistorie, Stufenstand und Beetposition. v2→v3-Platz für spätere Migration.
- **CI:** Java-Regel-/Spielsimulations-Tests und Android-Gradle-Build mit automatisch herunterladbarer Debug-APK.

### Explizit noch nicht Teil der fertigen v0.1.0
- Gemeinsame \`plant-core\`-Gradle-Bibliothek und stabiles \`.zimtplant\`-Import-/Exportformat (für spätere Partnergeschenke als Schnittstelle geplant).
- SQLite-Datenbank und Transaktionsereignisse: derzeit versionierte private lokale Dateien als Prototyp.
- Umfassende echte Gerätemessungen, insbesondere Android 6, Moto G6, Nothing Phone; Performance/Touch/Anzeige noch prüfen.
- Fertige Gestaltung und Animationen, Warnhinweise vor Fristablauf, endgültige Spielregelabnahme.
- Küche/Garten und direkter Partnertransfer.

## APK installieren

1. [GitHub Actions](https://github.com/shayohekmat-wq/Zimt-Lab-1/actions) öffnen.
2. **Neuesten erfolgreichen** Build „Garten und Kueche - Android Debug APK“ wählen.
3. Unter **Artifacts** \`Garten-und-Kueche-Lab-0.0.3-Parallel-APK\` herunterladen.
4. ZIP entpacken und \`app-debug.apk\` auf dem Android-Smartphone installieren.

**Ab 0.0.3 hat jede neue Lab-Version eine eigene Paketkennung**: \`de.naehe.app.gartenkueche.lab3\` für 0.0.3 (v0.0.2 verwendete \`de.naehe.app.gartenkueche.lab1\`). Dadurch lässt sich 0.0.3 parallel zu 0.0.2 und zur ZIMT-Hauptapp installieren. Wichtig: Die App-Daten sind durch das Android-Sandbox-System getrennt; die alte Pflanze wird nicht automatisch mitgenommen. Für eine zukünftige separate Version muss \`labBuild\` in app/build.gradle.kts erhöht werden. Die Android-Laufzeit muss bei erster Installation eventuell „aus dieser Quelle installieren“ erlauben.

## Vorläufige Spielregelinterpretation

- Die Schwierigkeitsstufe **der aktuell aktiven Evolutionsstufe** legt Pflege und Entwicklung von ihr aus fest.
- Pflegeleicht/Medium: Maximalmenge pro **rollierenden 24 h**. Profi/Diva: maximal fünf Gießvorgänge **pro aktuellem mehrtägigem Pflegefenster**.
- Die **Mindestmenge** zählt pro in sich abgeschlossenem, beim Start ausgewürfelten Pflegefenster.
- Gültige Diva-Frist ist zufällig **1–9 Tage**; die ungültigen Werte 0 und -1 aus 4±5 Tagen werden neu gezogen.
- Alle Pflegefristen und Evolutionsdauern sind in festen **24-Stunden-Tagen** angegeben, nicht in lokalen Kalendertagen.
- Nach einem Regelverstoß geht die Pflanze um genau eine Stufe zurück; auf Stufe 1 führt der zweite Regelverstoß zum Tod. Die neue Phase startet beim Verstoß. Eine neue Stufe bekommt neue, persistierte Parameter.
- Offline wird beim erneuten Öffnen chronologisch nachgerechnet; keine Hintergrunddienste und keine Netzabhängigkeit. Vorsätzliche Manipulation der Systemzeit ist offline nicht vollständig verhinderbar.

**Diese Details sind Vorschläge** und können noch geändert werden. Der komplette Masterplan steht in [BUILD_PLAN_0.1.0.md](BUILD_PLAN_0.1.0.md).

## Technologie und ZIMT-Kompatibilität

Gradle 8.9 / AGP 8.7.3, Java 17, minSdk 23, compileSdk 35, targetSdk 34 für die Test-APK. Keine benötigten Internet-/Kamera-/Speicherberechtigungen. Verwendete Standard-Android-Views, Canvas und eigene Java-Simulation lassen sich später gezielt in das bestehende Java-basierte ZIMT-Projekt übertragen. Beide Projekte bleiben vorerst vollständig getrennt.

## Abnahmegrenzen

Ein grüner GitHub-Workflow bestätigt **Kompilierung und die automatisierten Regeln-Tests**, nicht die Funktionsfähigkeit auf jedem konkreten Android-Gerät. Vor 0.1.0 sind besonders echte Tests auf Android 6/8, Speichern bei App-Abbruch, große 10-Stufen-Pflanzen, schnelle Touchbewegungen und Zeitwechsel nötig.

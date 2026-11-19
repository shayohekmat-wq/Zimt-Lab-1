# Garten & Küche — ZIMT Lab 1

Eigenständige native Android-App als modulares Labor für spätere ZIMT-Integration. Android **6.0+ (API 23)**, Java + Canvas, offline ohne Anmeldung/Firebase. Das ZIMT-Hauptrepository wird nicht verändert.

## Version 0.0.5 — Performance-Optimierung

Nach einem gemeldeten Android-Dialog **„Garten & Küche 0.0.4 reagiert nicht“** wurden folgende Engpässe im Code umgebaut:

- **Beet-Datenladen auf einem eigenen Hintergrund-Thread:** Alte GZIP-Pflanzendateien und längere Offline-Pflegesimulationen blockieren nicht mehr die Benutzeroberfläche.
- **Pflanzenbilder vom Pflegezustand getrennt:** Die unveränderlichen Evolutionszeichnungen werden einmal als Basis archiviert; bei Gießen und Verschieben wird künftig nur ein kleiner, atomar geschriebener Zustandsdatensatz aktualisiert. Die bisherigen Pflanzendateien können weiterhin gelesen werden.
- **Atomare Speicherung:** Android \`AtomicFile\` schützt Pflanzendaten und kompakte Zustandsdateien vor halbfertigen Schreibvorgängen.
- **Kein unnötiges Speichern bei App-Resume:** Bloßes Fortschreiten der letzten Prüfzeit zwingt nicht zu einem neuen Dateischreibvorgang.
- **Editor mit wiederverwendbarer Bitmap:** Ein Zeichenstrich setzt direkt die betroffenen Pixel, statt bei jeder Bewegung das komplette 300×300-Bild neu anzulegen und die vorige Bitmap zu verwerfen.
- **Draft-Autosave, Laden und Pflanzenabschluss auf einem I/O-Worker:** Lange Dateivorgänge laufen im Hintergrund; es gibt weiterhin Entwurfs-Autosave und Fortsetzen.
- **Konturprüfung im Hintergrund:** Auch komplizierte mehrstrichige Konturen dürfen die UI nicht einfrieren. Die kurze Lückenerkennung prüft jetzt höchstens 24 sortierte Kandidaten statt möglicherweise tausende Vollraster-Varianten.
- **Beet-Hintergrund einmalig gerendert:** Ein RGB_565-Bitmap für Boden und Raster wird wiederverwendet, Pflanzensprites entstehen außerhalb des UI-Threads und landen in einem begrenzten Cache.
- **Debug-Messpunkt:** Dauer von Laden und Pflegeprüfung wird als \`GartenKuechePerf\` in Logcat gemeldet.

### Bereits vorhandene Funktionen

Startseite mit Beet/Küche/Garten, Evolutionen 1–10, 300×300-Pixelkonturen und 40 Farben, 20%-Vorlage der vorherigen Stufe, Todesform grau oder selbst gezeichnet, automatisch ins 800×600-Beet, Long-Press-Verschieben, 5-Sekunden-Gießkanne, vier Pflege-Schwierigkeitsgrade, langfristige Pflege, Evolution, Regression und Tod.

### Installation und wichtige Einschränkung

1. Neuesten erfolgreichen [GitHub-Actions-Build](https://github.com/shayohekmat-wq/Zimt-Lab-1/actions) öffnen.
2. Das Artifact **Garten-und-Kueche-v0.0.5-Performance-APK** herunterladen und entpacken. In ChatGPT wird die eigentliche APK direkt als Download angeboten.
3. \`app-debug.apk\` installieren.

**0.0.5 hat die eindeutige Android-Paketkennung \`de.naehe.app.gartenkueche.lab5\`.** Die neue Version lässt sich *parallel* zu 0.0.4 und zur ZIMT-App installieren. **Wichtig:** Android trennt dabei die Datensandboxes. Pflanzen aus 0.0.4 erscheinen nicht automatisch in 0.0.5. Die alte Installation bitte nicht entfernen, bevor wir einen expliziten Export-/Importpfad bereitgestellt haben. Bestehende Daten im gleichen Paket/Format werden rückwärtskompatibel gelesen.

### Qualitätssicherung

GitHub Actions überprüft pure-Java-Pflege- und Kontur-Tests, Android 6.0+ minSdk und die APK-Paketkennung. **Ein grüner Build ist noch kein echter Gerätetest und beweist nicht, dass alle ANRs beseitigt sind.** Der Screenshot zeigt eine ANR; ohne Android-ANR-Stacktrace ist die exakte ursprüngliche Ursache nicht vollständig bestätigt. Ein ausführlicher Performance-Plan liegt in [PERFORMANCE_MASTERPLAN_0.0.5.md](PERFORMANCE_MASTERPLAN_0.0.5.md). Weitere Schritte: echte Profiling-Messungen, 10-Stufen-Belastungstests, inkrementelles Undo/Redo und Datenexport/-import.

### Offene Spielregeln und Architektur

Die aktuellen Pflegeinterpretationen sind weiterhin vorläufig, beschrieben im [Build-Plan 0.1.0](BUILD_PLAN_0.1.0.md). Java, klassische Android Views und minSdk23 bleiben unverändert; Kernfunktionen sind für eine spätere Übernahme nach ZIMT vorbereitet.

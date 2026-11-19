# Garten & Küche — ZIMT Lab 1

Eigenständiger Android-Prototyp zur späteren Integration in ZIMT. **Android 6.0+ (minSdk 23)**; native **Java/Canvas**-Implementierung; lokale Nutzung ohne Anmeldung/Firebase. Das Hauptprojekt \`shayohekmat-wq/Test\` bleibt unverändert.

## Aktueller Stand

**0.0.1 / M0–Editor-Vorschau** – erste lauffähige Grundlage (nicht der vollständige 0.1.0-Build):
- Startseite mit drei unterschiedlich großen Kacheln: **Beet** aktiv; **Küche** und **Garten** Platzhalter.
- Beet zeigt ein logisch **800 × 600 Pixel** großes Feld. Noch keine gespeicherten Pflanzen.
- Anlegen einer Pflanze: Auswahl **1–10** Evolutionsstufen.
- Erste Zeichenansicht: **300 × 300**-Pixel-Kontur mit dunkelviolettem Pixelstift, stufenweisem Zoom und Zwei-Finger-Verschieben; vereinfachte geschlossene-Kontur-Prüfung.
- Experimenteller Innenfarbmodus mit 40 Farben. Noch **keine Speicherung**, Todesform, vollständiger Evolutionseditor, Pflege-Engine oder Gießmechanik.
- Eigene Paketkennung \`de.naehe.app.gartenkueche.lab1\` für parallele Installation neben ZIMT.

**Wichtig:** Dies ist ein früher Editor-Prototyp. Die vollumfängliche Definition für den späteren **Build 0.1.0** steht in [BUILD_PLAN_0.1.0.md](BUILD_PLAN_0.1.0.md). Der erste APK-Build kann erst als geprüft gelten, wenn GitHub Actions \`assembleDebug\` erfolgreich abgeschlossen hat.

## APK selbst installieren

1. [GitHub Actions öffnen](https://github.com/shayohekmat-wq/Zimt-Lab-1/actions).
2. Den **neuesten erfolgreichen** Workflow „Garten und Kueche - Android Debug APK“ auswählen.
3. Unter **Artifacts** \`Garten-und-Kueche-Lab-0.0.1-Debug-APK\` laden.
4. ZIP entpacken, \`app-debug.apk\` auf Android installieren. Bei Bedarf Installation aus dieser Quelle einmalig erlauben.

## Architektur / Workflow

- Native Android API 23+, Java 17, Android Gradle Plugin 8.7.3, Gradle 8.9.
- \`app/\`: UI-Prototyp mit \`MainActivity\`, \`BeetActivity\`, \`StageSetupActivity\` und \`EditorActivity\`.
- \`.github/workflows/android-apk.yml\`: Gradle-Build auf GitHub, Artefakt-Upload.
- Künftiger Core wird als vom Android-UI getrennter \`plant-core\` aufgebaut: PlantDefinition, PlantInstance, CareWindow und deterministischer PlantEngine.
- ZIMT kann später den Core und die UI über definierte Schnittstellen übernehmen. Noch **kein** Cloud-Transfer oder Dateiexport.

## Nächste Schritte

M1: robuste Konturprüfung, alle Editorwerkzeuge, Rückgängig, Farbregionen und Canvas-Zoom. M2: vollständiger 1–10-Stufen-Wizard mit 20%-Overlay sowie Todesform. M3–M6: Persistenz, Beet-Interaktionen, Pflege/Entwicklung, alte Android-Geräte und QA.

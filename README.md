# Fitnessapp – Workout Begleiter

Android-App, die dich durch den 10-Minuten-Workoutplan führt:

- **3 Durchgänge** mit je 4 Übungen à **45 Sekunden**
  1. Bodyweight Squats (Kniebeugen)
  2. Push-Ups (Liegestütze)
  3. Reverse Lunges (Rückwärts-Ausfallschritte)
  4. Plank to Shoulder Tap (Liegestützposition mit Schultertippen)
- **30 Sekunden Pause** nach jedem Durchgang (außer dem letzten)
- Gesamtdauer **10:00 Minuten** (plus 10 Sekunden „Mach dich bereit“ zu Beginn)

## Funktionen

- Großer Countdown-Ring mit Farbe der jeweiligen Übung
- Sprachansagen auf Deutsch (Übung, „Noch 10 Sekunden“, was als nächstes kommt)
- Pieptöne in den letzten 3 Sekunden und Signal + Vibration beim Wechsel
- Technik-Tipps zu jeder Übung („Saubere Technik hat immer Vorrang vor Tempo“)
- Pausieren, Übung überspringen oder zurückspringen
- Bildschirm bleibt während des Trainings an
- Pieptöne, Sprache und Vibration einzeln abschaltbar

## APK herunterladen

Jeder Push baut die App per GitHub Actions. Unter **Actions → Android Build → letzter Lauf**
das Artefakt `WorkoutBegleiter-apk` herunterladen, entpacken und `app-debug.apk` auf dem
Handy installieren (Installation aus unbekannten Quellen erlauben).

## Selbst bauen

Mit Android Studio öffnen oder per Kommandozeile (JDK 17 + Android SDK nötig):

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Tests der Timer-Logik: `./gradlew testDebugUnitTest`

Voraussetzung: Android 8.0 (API 26) oder neuer. Für Sprachansagen muss eine deutsche
Text-to-Speech-Stimme installiert sein (bei den meisten Geräten vorinstalliert).

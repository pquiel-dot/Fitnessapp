# Fitnessapp – Workout Begleiter

Android-App mit zwei Programmen, die oben auf der Startseite ausgewählt werden.

## Workoutplan

Der 10-Minuten-Workoutplan:

- **3 Durchgänge** mit je 4 Übungen à **45 Sekunden**
  1. Bodyweight Squats (Kniebeugen)
  2. Push-Ups (Liegestütze)
  3. Reverse Lunges (Rückwärts-Ausfallschritte)
  4. Plank to Shoulder Tap (Liegestützposition mit Schultertippen)
- **30 Sekunden Pause** nach jedem Durchgang (außer dem letzten)
- Gesamtdauer **10:00 Minuten** (plus 10 Sekunden „Mach dich bereit“ zu Beginn)

## Running-Mobilisation

„6 Moves, um deinen Laufkörper zu mobilisieren“ (Runner's World 11/2026) – am besten vor einem
lockeren Lauf. Benötigt ein Widerstandsband und eine Stütze.

| Nr. | Übung | Ziel | Zeit |
| --- | --- | --- | --- |
| 1 | Banded Arm Circle (Armkreisen mit Band) | 20 Wiederholungen | 60 s |
| 2 | Twisted Warrior (Gedrehter Krieger) | 5 Drehungen pro Seite | 2 × 45 s |
| 3 | Side-Lying Hip Circle (Hüftkreisen in Seitenlage) | 3–5 Wiederholungen pro Seite | 2 × 60 s |
| 4 | Deep Squat Rock (Gewicht kreisen in tiefer Hocke) | 10 Kreise pro Richtung | 2 × 45 s |
| 5 | Pigeon Hip Extension (Hüftstreckung aus der Taube) | 20 Wiederholungen pro Seite | 2 × 60 s |
| 6 | Directional Leg Swing (Beinschwingen in alle Richtungen) | 4–8 Schwünge pro Richtung | 2 × 60 s |

Zwischen den Übungen bleiben 15 Sekunden zum Umpositionieren, beim Seitenwechsel 10 Sekunden.
Gesamtdauer **12:05 Minuten** (plus 10 Sekunden „Mach dich bereit“). Die App zeigt für jede
Übung Zweck, Wiederholungsziel, aktuelle Seite und die Schritt-für-Schritt-Anleitung und sagt
Wechsel und Seiten an. Eine Übung zählt im Verlauf als absolviert, wenn alle ihre Seiten
vollständig durchlaufen wurden.

## Funktionen

- Großer Countdown-Ring mit Farbe der jeweiligen Übung
- Sprachansagen auf Deutsch (Übung, „Noch 10 Sekunden“, was als nächstes kommt)
- Pieptöne in den letzten 3 Sekunden und Signal + Vibration beim Wechsel
- Technik-Tipps zu jeder Übung („Saubere Technik hat immer Vorrang vor Tempo“)
- Pausieren, Übung überspringen oder zurückspringen
- Bildschirm bleibt während des Trainings an
- Pieptöne, Sprache und Vibration einzeln abschaltbar
- **Verlauf**: Jedes abgeschlossene Training wird mit Programm, Datum und Uhrzeit (Start und Ende),
  tatsächlich trainierter Zeit und den vollständig absolvierten Übungen gespeichert
  (übersprungene Übungen zählen nicht). Die Übersicht zeigt Gesamtwerte pro Übung und lässt
  sich nach Datum, Dauer oder Anzahl Übungen sortieren; einzelne Einträge lassen sich löschen.
- **Strava-Export**: Trainings auf Wunsch automatisch nach jedem Abschluss zu Strava hochladen (siehe unten)

## Strava-Export

Abgeschlossene Trainings können automatisch als Aktivität zu Strava hochgeladen werden
(Workoutplan als „Krafttraining“, Running-Mobilisation als „Training“, mit Startzeit,
trainierter Dauer und den absolvierten Übungen in der Beschreibung).

Einmalige Einrichtung:

1. Unter [strava.com/settings/api](https://www.strava.com/settings/api) eine eigene API-Anwendung
   anlegen. Name und Website sind frei wählbar; als **Autorisierungs-Callback-Domain** `localhost`
   eintragen.
2. In der App auf der Startseite im Bereich **Strava** die angezeigte **Client-ID** und das
   **Client-Secret** eintragen und auf **Mit Strava verbinden** tippen.
3. Bei Strava anmelden und das Hochladen von Aktivitäten erlauben – danach geht es automatisch zurück
   in die App.

Ab dann wird jedes gespeicherte Training direkt nach dem Abschluss hochgeladen (abschaltbar).
Ohne Internet wird der Upload automatisch nachgeholt, sobald wieder eine Verbindung besteht.
Im Verlauf zeigt jedes Training seinen Strava-Stand; ältere oder fehlgeschlagene Trainings lassen
sich dort per **Zu Strava hochladen** nachträglich exportieren. Die Sichtbarkeit der Aktivität
richtet sich nach den Privatsphäre-Einstellungen im Strava-Konto.

Client-ID, Client-Secret und Zugangstoken liegen nur im privaten App-Speicher des Handys, nicht
im Quelltext oder APK.

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

Tests der Timer- und Verlaufs-Logik: `./gradlew testDebugUnitTest`

Voraussetzung: Android 8.0 (API 26) oder neuer. Für Sprachansagen muss eine deutsche
Text-to-Speech-Stimme installiert sein (bei den meisten Geräten vorinstalliert).

# Startseite für den Philips 65PUS8545

Eigene, werbefreie Startseite für den Fernseher – mit selbst gewählten Kacheln,
umbelegbaren Fernbedienungstasten und Aufwach-Logik. Eine einzige App:
**Launcher + Bedienungshilfe-Dienst** (Tasten, Standby, Home-Umleitung).

Ein eigener Neubau statt CleanYourTV-Fork: Laut Planung kommt das Design komplett
selbst, CleanYourTV hätte nur Backup/Restore/Umschalten geliefert – das erledigen
hier die kleinen Skripte in [`adb/`](adb/).

## Was die App kann

| Teil | Funktion |
|---|---|
| Startseite | Uhr + Datum, Reihe **Favoriten**, Reihe **Eingänge** (HDMI), Raster **Alle Apps** |
| Kacheln | OK = öffnen · OK lange / Menü-Taste = zu Favoriten, verschieben, entfernen |
| Einstellungen (⚙ oben rechts) | Dienst-Status, Aufwachen an/aus, Original-Startseite ersetzen an/aus, Tastenbelegungen |
| Tasten-Test | Tasten-Inventar ohne Rechner: zeigt Code jeder Taste und ob sie umbelegbar ist |
| Tastenbelegung | Pro Taste **kurz / lang / doppelt** → App, HDMI-Eingang, Startseite, Zurück oder sperren |
| Aufwachen | Nach dem Standby erscheint sofort die eigene Startseite |
| Original ersetzen | Taucht die Google-/Philips-Startseite auf (Kaltstart, Home-Taste), springt der Dienst zur eigenen |

## Installation – nur mit Handy und Fernbedienung (Weg A)

Jeder Push baut automatisch eine neue APK (GitHub Actions) und veröffentlicht sie
unter **Releases** als `Startseite.apk`.

Am TV in der App **„Downloader“** diesen Link eingeben (Repo muss dafür **öffentlich** sein):

`https://github.com/theodorhannusch/Fernseher/releases/latest/download/Startseite.apk`

Der Link zeigt immer auf die neueste Version – für Updates einfach erneut eingeben.

Einmalig am TV:

1. Dem Downloader „Unbekannte Quellen“ erlauben
   (Einstellungen → Geräteeinstellungen → Sicherheit & Einschränkungen).
2. APK installieren, **Startseite** öffnen.
3. ⚙ Einstellungen → „Dienst ist AUS – hier einschalten“ → **Startseite – Tasten & Aufwachen** aktivieren.
4. Fragt der TV bei der Home-Taste, welche Startseite: **Startseite → Immer**.

**Updates:** Neue APK genauso drüberinstallieren – Favoriten und Belegungen bleiben erhalten.

## Erste Schritte nach der Installation

1. **Tasten-Test** (⚙ → Tasten-Test öffnen): jede Taste einmal drücken, Bildschirm abfotografieren
   und mir schicken. Tasten, die gar nicht erscheinen (meist Power, Ambilight), fängt Philips ab.
2. **Favoriten** festlegen (noch offen laut Plan): auf einer App OK lange drücken → „Zu Favoriten“.
3. **Tasten belegen**: ⚙ → „Neue Tastenbelegung“ → Taste drücken → kurz/lang/doppelt → Ziel.

### Grenzen ohne ADB

- Beim Kaltstart blitzt die Original-Startseite kurz auf, bevor die eigene erscheint.
- Werbung/Vorinstalliertes bleibt aktiv. → Beides löst [Weg B](adb/) (einmalig, ca. 2 Std.,
  braucht einmalig einen Mac/PC, mit iPhone nicht möglich).
- Ist nur „lang“ oder „doppelt“ belegt, verliert der kurze Druck seine Originalfunktion
  (außer Zurück/Home) – ein Dienst kann Tasten nicht „weiterreichen“. Daher am besten auch „kurz“ belegen.
- HDMI-Kacheln öffnen den Eingang über den Philips-TV-Player. Das Paket dafür darf beim Entrümpeln nicht weg
  (steht in der Schutzliste von `adb/gemeinsam.sh`).

## Notfall

Siehe [docs/NOTFALL.md](docs/NOTFALL.md).

## Entwicklung

- Kotlin, keine Bibliotheken außer dem Android-Framework; minSdk 26, targetSdk 34.
- Code: `app/src/main/java/de/fernseher/startseite/`
  - `HomeActivity.kt` – Startseite · `EinstellungenActivity.kt` · `TastenTestActivity.kt`
  - `TastenService.kt` – Tasten, Aufwachen, Original ersetzen · `Daten.kt` – Einstellungen, Apps, Eingänge
- Build lokal: `./gradlew assembleRelease` (Android SDK nötig). In CI automatisch.
- Signierschlüssel: `signing/startseite.jks` (Passwort `startseite`) liegt bewusst im Repo,
  damit Updates über die alte Version installiert werden können. Bei öffentlichem Repo könnte damit
  theoretisch jeder eine „Update“-APK signieren – relevant nur, wenn man eine APK aus fremder Quelle
  installiert. Darum nur über den Release-Link oben installieren.

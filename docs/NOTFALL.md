# Notfall-Spickzettel

Kaputtmachen kann man praktisch nichts – alles ist umkehrbar.

| Problem | Lösung ohne Rechner | Lösung mit ADB (`adb/`) |
|---|---|---|
| Startseite springt ständig / nervt | Einstellungen → Geräteeinstellungen → Bedienungshilfen → Dienst „Startseite“ aus | – |
| Startseite stürzt ab | Dienst aus (s. o.), dann Einstellungen → Apps → Startseite → Deinstallieren | `./restore.sh` |
| Tastenbelegung blockiert etwas | ⚙ → Belegung anklicken → Löschen · oder Dienst aus | – |
| Nach Firmware-Update alte Startseite wieder da | Nichts tun – der Dienst springt automatisch um (falls Dienst aus: wieder einschalten) | `./startseite-setzen.sh` |
| Funktion fehlt nach Entrümpeln (z. B. Ambilight) | – | `adb shell pm enable <paket>` oder `./restore.sh` |
| Kein Startbildschirm mehr (Launcher deaktiviert, App weg) | Werksreset (letzter Ausweg) | `adb shell pm enable com.google.android.tvlauncher` |
| ADB-Verbindung weg | – | Feste IP prüfen, Entwickleroptionen → USB-Debugging neu bestätigen |

Der Dienst pausiert sich selbst 5 Minuten, wenn er mehr als 6-mal in 30 Sekunden
umspringen müsste (Schutz gegen Endlosschleifen).

**Letzter Ausweg:** Werksreset am TV. Danach ist alles wie neu (Senderliste neu einrichten).

# ADB-Skripte (Weg B)

Nur nötig für das, was ohne ADB nicht geht: Werbung/Vorinstalliertes abschalten
und die Original-Startseite deaktivieren. Alles ist umkehrbar (`pm disable-user`,
kein Deinstallieren, kein Root).

Läuft auf einem Mac/PC **oder auf einem Android-Handy mit Termux**:

```sh
pkg install android-tools git
git clone https://github.com/theodorhannusch/Fernseher && cd Fernseher/adb
export TV=192.168.178.50    # feste IP des Fernsehers
./verbinden.sh              # am TV „Immer zulassen“ bestätigen
./backup.sh                 # Phase 1: Bestand sichern -> backup/<datum>/
./entruempeln.sh            # Phase 2: Pakete aus entruempeln-liste.txt deaktivieren
./startseite-setzen.sh      # Phase 5: Original-Launcher aus, eigene Startseite als Standard
./restore.sh                # Alles, was diese Skripte deaktiviert haben, wieder an
```

Jedes deaktivierte Paket wird in `backup/deaktiviert.txt` protokolliert –
das ist die Grundlage für `restore.sh`. Den Ordner `backup/` nach dem Lauf
committen oder anders sichern (er ist in `.gitignore`, weil er Gerätedaten enthält;
bei Bedarf `git add -f backup/`).

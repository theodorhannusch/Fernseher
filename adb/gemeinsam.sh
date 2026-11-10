#!/usr/bin/env bash
# Gemeinsame Einstellungen für alle Skripte.
set -euo pipefail
HIER="$(cd "$(dirname "$0")" && pwd)"
BACKUP="$HIER/../backup"
PROTOKOLL="$BACKUP/deaktiviert.txt"
mkdir -p "$BACKUP"

if [ -z "${TV:-}" ]; then
  echo "Bitte zuerst die TV-Adresse setzen:  export TV=192.168.x.y" >&2
  exit 1
fi
ZIEL="$TV"
case "$ZIEL" in *:*) ;; *) ZIEL="$ZIEL:5555" ;; esac
A() { adb -s "$ZIEL" "$@"; }

# Positivliste: diese Pakete werden NIE deaktiviert (Muster, Groß/Klein egal).
SCHUTZ='ambilight|org\.droidtv\.(tvserver|tvsystemui|settings|euinstallertc|playtv|hdmi|eventlogger|channels)|com\.android\.tv\.settings|com\.android\.providers\.tv|tvinput|hdmi|cec|com\.google\.android\.tv\.remote|com\.google\.android\.katniss|com\.google\.android\.gms$|com\.android\.vending|de\.fernseher\.startseite|inputmethod|packageinstaller|com\.android\.systemui|com\.android\.shell'
geschuetzt() { echo "$1" | grep -Eiq "$SCHUTZ"; }

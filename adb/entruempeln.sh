#!/usr/bin/env bash
# Phase 2: Pakete aus entruempeln-liste.txt deaktivieren (umkehrbar mit restore.sh).
source "$(dirname "$0")/gemeinsam.sh"
LISTE="$HIER/entruempeln-liste.txt"
grep -Ev '^\s*(#|$)' "$LISTE" | sed 's/#.*//; s/[[:space:]]//g' | while read -r p; do
  [ -z "$p" ] && continue
  if geschuetzt "$p"; then echo "GESCHÜTZT, übersprungen: $p"; continue; fi
  if A shell pm disable-user --user 0 "$p" >/dev/null 2>&1; then
    echo "$p" >> "$PROTOKOLL"; echo "deaktiviert: $p"
  else
    echo "nicht gefunden/fehlgeschlagen: $p"
  fi
done
sort -u -o "$PROTOKOLL" "$PROTOKOLL" 2>/dev/null || true
echo "Jetzt testen: Ambilight, HDMI-Wechsel, Netflix/YouTube, Sprachsteuerung."

#!/usr/bin/env bash
# Macht alles rückgängig, was entruempeln.sh / startseite-setzen.sh deaktiviert haben.
source "$(dirname "$0")/gemeinsam.sh"
if [ ! -s "$PROTOKOLL" ]; then echo "Nichts zu tun – $PROTOKOLL ist leer."; exit 0; fi
while read -r p; do
  [ -z "$p" ] && continue
  A shell pm enable "$p" >/dev/null 2>&1 && echo "wieder an: $p" || echo "fehlgeschlagen: $p"
done < "$PROTOKOLL"
echo "Wenn alles wieder läuft, kann $PROTOKOLL geleert werden."

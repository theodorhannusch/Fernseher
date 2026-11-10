#!/usr/bin/env bash
# Phase 1: Bestandsaufnahme. Ändert nichts am TV.
source "$(dirname "$0")/gemeinsam.sh"
ORDNER="$BACKUP/$(date +%Y-%m-%d_%H%M)"
mkdir -p "$ORDNER"
A shell getprop > "$ORDNER/getprop.txt"
A shell pm list packages -f > "$ORDNER/pakete-alle.txt"
A shell pm list packages -d > "$ORDNER/pakete-deaktiviert.txt"
A shell pm list packages -3 > "$ORDNER/pakete-nachinstalliert.txt"
for ns in global secure system; do A shell settings list "$ns" > "$ORDNER/settings-$ns.txt"; done
A shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME > "$ORDNER/home-launcher.txt" || true
echo "Modell:   $(grep -m1 'ro.product.model' "$ORDNER/getprop.txt")"
echo "Android:  $(grep -m1 'ro.build.version.release\]' "$ORDNER/getprop.txt")"
echo "Firmware: $(grep -m1 'ro.build.display.id' "$ORDNER/getprop.txt")"
echo "Gesichert in $ORDNER"

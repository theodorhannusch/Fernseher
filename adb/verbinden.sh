#!/usr/bin/env bash
source "$(dirname "$0")/gemeinsam.sh"
adb connect "$ZIEL"
echo "Falls am TV eine Abfrage erscheint: „Immer zulassen“ wählen, dann erneut ausführen."
A shell getprop ro.product.model

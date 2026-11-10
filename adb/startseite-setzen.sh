#!/usr/bin/env bash
# Phase 5: eigene Startseite als Standard, Original-Launcher deaktivieren.
# Nach jedem Philips-Firmware-Update einfach erneut ausführen.
source "$(dirname "$0")/gemeinsam.sh"
EIGENE="de.fernseher.startseite/.HomeActivity"
if ! A shell pm list packages | grep -q "de.fernseher.startseite"; then
  echo "Die Startseite-App ist noch nicht installiert." >&2; exit 1
fi
A shell cmd package set-home-activity "$EIGENE" || echo "(set-home-activity nicht unterstützt – Deaktivieren reicht meist)"
for p in com.google.android.tvlauncher com.google.android.apps.tv.launcherx com.google.android.leanbacklauncher; do
  if A shell pm list packages | grep -qx "package:$p"; then
    A shell pm disable-user --user 0 "$p" && echo "$p" >> "$PROTOKOLL" && echo "deaktiviert: $p"
  fi
done
sort -u -o "$PROTOKOLL" "$PROTOKOLL"
echo "Fertig. Test: 5x Kaltstart, 5x aus dem Standby."

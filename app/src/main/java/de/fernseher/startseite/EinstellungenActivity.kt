package de.fernseher.startseite

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast

class EinstellungenActivity : Activity() {

    private lateinit var liste: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rand = dp(48)
        liste = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(rand * 3, rand / 2, rand * 3, rand)
            clipChildren = false
            clipToPadding = false
        }
        setContentView(ScrollView(this).apply {
            background = verlauf()
            clipChildren = false
            addView(liste)
        })
    }

    override fun onResume() {
        super.onResume()
        aufbauen()
    }

    private fun eintrag(text: CharSequence, beiKlick: () -> Unit) {
        liste.addView(knopf(text, beiKlick), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(10) })
    }

    private fun abschnitt(text: String) {
        liste.addView(text(text, 18f, Farben.TEXT_LEISE, fett = true).apply { setPadding(0, dp(20), 0, dp(8)) })
    }

    private fun anAus(b: Boolean) = if (b) "An" else "Aus"

    private fun aufbauen() {
        liste.removeAllViews()
        liste.addView(text("Einstellungen", 30f, fett = true).apply { setPadding(0, 0, 0, dp(8)) })

        abschnitt("Bedienungshilfe-Dienst")
        val aktiv = TastenService.istAktiv(this)
        eintrag(if (aktiv) "✓  Dienst ist aktiv" else "⚠  Dienst ist AUS – hier einschalten") {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
        eintrag("Nach Standby Startseite zeigen: ${anAus(Prefs.aufwachen(this))}") {
            Prefs.setzeAufwachen(this, !Prefs.aufwachen(this)); aufbauen()
        }
        eintrag("Original-Startseite ersetzen: ${anAus(Prefs.ersetzen(this))}") {
            Prefs.setzeErsetzen(this, !Prefs.ersetzen(this)); aufbauen()
        }

        abschnitt("Tasten")
        eintrag("Tasten-Test öffnen (Inventar)") {
            startActivity(Intent(this, TastenTestActivity::class.java))
        }
        eintrag("+  Neue Tastenbelegung") { tasteAufnehmen() }
        val belegungen = Prefs.belegungen(this).sortedWith(compareBy({ it.keyCode }, { Art.ALLE.indexOf(it.art) }))
        if (belegungen.isEmpty()) {
            liste.addView(text("Noch keine Belegungen.", 16f, Farben.TEXT_LEISE).apply { setPadding(dp(8), 0, 0, dp(8)) })
        }
        for (b in belegungen) {
            eintrag("${tastenName(b.keyCode)} · ${Art.name(b.art)}  →  ${Katalog.name(this, b.ziel)}") {
                AlertDialog.Builder(this)
                    .setTitle("Belegung löschen?")
                    .setMessage("${tastenName(b.keyCode)} · ${Art.name(b.art)} → ${Katalog.name(this, b.ziel)}")
                    .setPositiveButton("Löschen") { _, _ ->
                        Prefs.setzeBelegungen(this, Prefs.belegungen(this) - b); aufbauen()
                    }
                    .setNegativeButton("Abbrechen", null)
                    .show()
            }
        }

        abschnitt("Info")
        liste.addView(text("Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", 15f, Farben.TEXT_LEISE))
    }

    // ---------- Neue Belegung: Taste → Art → Ziel ----------

    private val navigation = setOf(
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
    )

    private fun tasteAufnehmen() {
        var erledigt = false
        lateinit var dialog: AlertDialog
        val hoerer: (KeyLog.Eintrag) -> Unit = { e ->
            if (!erledigt && e.aktion == KeyEvent.ACTION_DOWN && e.keyCode !in navigation) {
                erledigt = true
                dialog.dismiss()
                artWaehlen(e.keyCode)
            }
        }
        dialog = AlertDialog.Builder(this)
            .setTitle("Taste drücken")
            .setMessage("Drücke jetzt die Taste, die du belegen willst.\n(Pfeiltasten und OK lassen sich nicht belegen.)")
            .setNegativeButton("Abbrechen", null)
            .create()
        dialog.setOnKeyListener { _, code, ev ->
            if (code in navigation) return@setOnKeyListener false
            if (ev.action == KeyEvent.ACTION_DOWN && !erledigt) {
                erledigt = true
                dialog.dismiss()
                artWaehlen(code)
            }
            true
        }
        dialog.setOnDismissListener {
            KeyLog.abmelden(hoerer)
            KeyLog.durchlassen = false
        }
        KeyLog.durchlassen = true
        KeyLog.anmelden(hoerer)
        dialog.show()
    }

    private fun artWaehlen(keyCode: Int) {
        val namen = arrayOf("Kurz drücken", "Lang drücken", "Doppelt drücken")
        AlertDialog.Builder(this)
            .setTitle("Taste ${tastenName(keyCode)}")
            .setItems(namen) { _, i -> zielWaehlen(keyCode, Art.ALLE[i]) }
            .show()
    }

    private fun zielWaehlen(keyCode: Int, art: String) {
        val ziele = mutableListOf(
            "Startseite" to Ziel(Ziel.STARTSEITE),
            "Taste sperren (nichts tun)" to Ziel(Ziel.SPERREN),
            "Zurück" to Ziel(Ziel.ZURUECK),
        )
        Katalog.eingaenge(this).forEach { ziele += "Eingang: ${it.name}" to it.ziel }
        Katalog.apps(this).forEach { ziele += it.name to it.ziel }
        AlertDialog.Builder(this)
            .setTitle("${tastenName(keyCode)} · ${Art.name(art)} →")
            .setItems(ziele.map { it.first }.toTypedArray()) { _, i ->
                val neu = Belegung(keyCode, art, ziele[i].second)
                val rest = Prefs.belegungen(this).filterNot { it.keyCode == keyCode && it.art == art }
                Prefs.setzeBelegungen(this, rest + neu)
                val hatKurz = (rest + neu).any { it.keyCode == keyCode && it.art == Art.KURZ }
                if (art != Art.KURZ && !hatKurz &&
                    keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_HOME
                ) {
                    Toast.makeText(
                        this,
                        "Hinweis: Kurz drücken macht bei dieser Taste jetzt nichts mehr – am besten auch „kurz“ belegen.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                if (!TastenService.istAktiv(this)) {
                    Toast.makeText(this, "Gespeichert – wirkt erst, wenn der Dienst aktiv ist.", Toast.LENGTH_LONG).show()
                }
                aufbauen()
            }
            .show()
    }
}

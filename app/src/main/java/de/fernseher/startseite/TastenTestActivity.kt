package de.fernseher.startseite

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tasten-Inventar ohne Rechner: zeigt jede gedrückte Taste mit Code und
 * ob sie beim Dienst (= umbelegbar) und/oder bei der App ankommt.
 * Tasten, die hier gar nicht erscheinen, fängt die Philips-Firmware vorher ab.
 */
class TastenTestActivity : Activity() {

    private class Info(val keyCode: Int, var scanCode: Int, var dienst: Boolean = false, var app: Boolean = false)

    private val gesehen = LinkedHashMap<Int, Info>()
    private val verlauf = ArrayDeque<String>()
    private val tabellen = ArrayList<TextView>()
    private lateinit var fehlend: TextView
    private lateinit var protokoll: TextView
    private lateinit var status: TextView
    private val zeit = SimpleDateFormat("HH:mm:ss", Locale.GERMANY)

    private val hoerer: (KeyLog.Eintrag) -> Unit = { eintragen(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rand = dp(40)
        val wurzel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(rand, rand / 2, rand, rand / 2)
            background = verlauf()
        }
        wurzel.addView(text("Tasten-Test", 30f, fett = true))
        wurzel.addView(text(
            "Drücke jede Taste der Fernbedienung einmal kurz. Zum Verlassen: Zurück lange gedrückt halten.\n" +
                "Dienst ✓ = lässt sich umbelegen · App ✓ = kommt normal bei Apps an · Taste fehlt = Philips fängt sie ab.",
            15f, Farben.TEXT_LEISE,
        ))
        status = text("", 15f).apply { setPadding(0, dp(8), 0, dp(8)) }
        wurzel.addView(status)

        val spalten = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fehlend = text("", 15f, Farben.WARNUNG).apply { setPadding(0, 0, 0, dp(8)) }
        wurzel.addView(fehlend)
        repeat(SPALTEN) {
            val t = text("", 13f).apply { typeface = android.graphics.Typeface.MONOSPACE }
            tabellen += t
            spalten.addView(t, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        }
        protokoll = text("", 12f, Farben.TEXT_LEISE).apply { typeface = android.graphics.Typeface.MONOSPACE }
        spalten.addView(ScrollView(this).apply { addView(protokoll) }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.8f))
        wurzel.addView(spalten, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(wurzel)
        anzeigen()
    }

    override fun onResume() {
        super.onResume()
        KeyLog.durchlassen = true
        KeyLog.anmelden(hoerer)
        if (TastenService.istAktiv(this)) {
            status.text = "Bedienungshilfe-Dienst: aktiv"
            status.setTextColor(Farben.OK)
        } else {
            status.text = "Bedienungshilfe-Dienst: AUS – nur die Spalte „App“ ist aussagekräftig. Einschalten unter Einstellungen → Bedienungshilfen."
            status.setTextColor(Farben.WARNUNG)
        }
    }

    override fun onPause() {
        KeyLog.abmelden(hoerer)
        KeyLog.durchlassen = false
        super.onPause()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        eintragen(KeyLog.Eintrag(event.keyCode, event.scanCode, event.action, event.repeatCount, KeyLog.QUELLE_APP, System.currentTimeMillis()))
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN && event.repeatCount >= 3) {
            finish()
        }
        return true // alles abfangen, damit z. B. Zurück den Test nicht beendet
    }

    private fun eintragen(e: KeyLog.Eintrag) {
        if (e.aktion == KeyEvent.ACTION_DOWN && e.wiederholung == 0) {
            val info = gesehen.getOrPut(e.keyCode) { Info(e.keyCode, e.scanCode) }
            if (e.scanCode != 0) info.scanCode = e.scanCode
            if (e.quelle == KeyLog.QUELLE_DIENST) info.dienst = true else info.app = true
            verlauf.addFirst("${zeit.format(Date(e.zeit))}  ${e.quelle.padEnd(6)} ${tastenName(e.keyCode)}")
            while (verlauf.size > 30) verlauf.removeLast()
            anzeigen()
        }
    }

    private fun anzeigen() {
        // Alle Tasten in mehreren Spalten, damit ein Foto genügt.
        val zeilen = gesehen.values.map { i ->
            tastenName(i.keyCode).take(16).padEnd(17) +
                i.keyCode.toString().padStart(4) + "  " +
                (if (i.dienst) "✓" else "–") + " " + (if (i.app) "✓" else "–")
        }
        val kopf = "Taste            Code  D A\n"
        val proSpalte = maxOf(1, (zeilen.size + SPALTEN - 1) / SPALTEN)
        tabellen.forEachIndexed { n, t ->
            val teil = zeilen.drop(n * proSpalte).take(proSpalte)
            t.text = if (teil.isEmpty() && n > 0) "" else kopf + teil.joinToString("\n").ifEmpty { "(noch keine Taste)" }
        }
        val fehlt = ERWARTET.filter { (codes, _) -> codes.none { it in gesehen } }.map { it.second }
        fehlend.text = if (fehlt.isEmpty()) "Alle typischen Tasten wurden erkannt." else
            "Nicht erkannt (drücken – erscheint sie nicht, fängt Philips sie ab): " + fehlt.joinToString(", ")
        protokoll.text = verlauf.take(20).joinToString("\n")
    }

    companion object {
        private const val SPALTEN = 3

        /** Tasten, die fast jede Fernbedienung hat – zum Abhaken. */
        private val ERWARTET = listOf(
            listOf(KeyEvent.KEYCODE_HOME) to "Home",
            listOf(KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_TV_POWER) to "Power",
            listOf(KeyEvent.KEYCODE_VOLUME_UP) to "Lauter",
            listOf(KeyEvent.KEYCODE_VOLUME_DOWN) to "Leiser",
            listOf(KeyEvent.KEYCODE_TV_INPUT) to "Quelle/Source",
            listOf(KeyEvent.KEYCODE_SETTINGS) to "Einstellungen",
            listOf(KeyEvent.KEYCODE_INFO) to "Info",
            listOf(KeyEvent.KEYCODE_SEARCH, KeyEvent.KEYCODE_ASSIST, KeyEvent.KEYCODE_VOICE_ASSIST) to "Mikrofon",
        )
    }
}

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
    private lateinit var tabelle: TextView
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
        tabelle = text("", 16f).apply { typeface = android.graphics.Typeface.MONOSPACE }
        protokoll = text("", 14f, Farben.TEXT_LEISE).apply { typeface = android.graphics.Typeface.MONOSPACE }
        spalten.addView(ScrollView(this).apply { addView(tabelle) }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 3f))
        spalten.addView(ScrollView(this).apply { addView(protokoll) }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 2f))
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
        val sb = StringBuilder()
        sb.append("Taste                     Code  Scan  Dienst  App\n")
        sb.append("────────────────────────────────────────────────\n")
        for (i in gesehen.values) {
            sb.append(tastenName(i.keyCode).take(24).padEnd(25))
            sb.append(i.keyCode.toString().padStart(5))
            sb.append(i.scanCode.toString().padStart(6))
            sb.append((if (i.dienst) "✓" else "–").padStart(6))
            sb.append((if (i.app) "✓" else "–").padStart(6))
            sb.append('\n')
        }
        if (gesehen.isEmpty()) sb.append("(noch keine Taste gedrückt)\n")
        tabelle.text = sb
        protokoll.text = verlauf.joinToString("\n")
    }
}

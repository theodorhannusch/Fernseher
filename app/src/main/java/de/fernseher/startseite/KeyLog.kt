package de.fernseher.startseite

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Verteilt Tastendrücke an den Tasten-Test und die Tasten-Aufnahme.
 * Solange [durchlassen] aktiv ist, belegt der Dienst keine Tasten um.
 */
object KeyLog {
    @Volatile
    var durchlassen = false

    data class Eintrag(
        val keyCode: Int,
        val scanCode: Int,
        val aktion: Int,
        val wiederholung: Int,
        val quelle: String,
        val zeit: Long,
    )

    const val QUELLE_DIENST = "Dienst"
    const val QUELLE_APP = "App"

    private val hoerer = CopyOnWriteArrayList<(Eintrag) -> Unit>()
    private val main = Handler(Looper.getMainLooper())

    fun anmelden(h: (Eintrag) -> Unit) { hoerer.add(h) }
    fun abmelden(h: (Eintrag) -> Unit) { hoerer.remove(h) }

    fun melden(e: KeyEvent, quelle: String) {
        if (hoerer.isEmpty()) return
        val eintrag = Eintrag(e.keyCode, e.scanCode, e.action, e.repeatCount, quelle, System.currentTimeMillis())
        main.post { hoerer.forEach { it(eintrag) } }
    }
}

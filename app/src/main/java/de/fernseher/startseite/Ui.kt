package de.fernseher.startseite

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.TextView

object Farben {
    const val HINTERGRUND_OBEN = 0xFF14223A.toInt()
    const val HINTERGRUND_UNTEN = 0xFF05070C.toInt()
    const val KACHEL = 0xFF1C2638.toInt()
    const val KACHEL_FOKUS = 0xFF26344D.toInt()
    const val AKZENT = 0xFF4FA3FF.toInt()
    const val TEXT = 0xFFF2F5FA.toInt()
    const val TEXT_LEISE = 0xFF9AA7BD.toInt()
    const val WARNUNG = 0xFFFFB454.toInt()
    const val OK = 0xFF6BD68A.toInt()
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun Context.text(s: CharSequence, sp: Float, farbe: Int = Farben.TEXT, fett: Boolean = false): TextView =
    TextView(this).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        setTextColor(farbe)
        if (fett) typeface = Typeface.DEFAULT_BOLD
    }

fun rund(farbe: Int, radius: Float, rahmen: Int = 0, rahmenFarbe: Int = Color.TRANSPARENT): GradientDrawable =
    GradientDrawable().apply {
        setColor(farbe)
        cornerRadius = radius
        if (rahmen > 0) setStroke(rahmen, rahmenFarbe)
    }

fun verlauf(): GradientDrawable = GradientDrawable(
    GradientDrawable.Orientation.TOP_BOTTOM,
    intArrayOf(Farben.HINTERGRUND_OBEN, Farben.HINTERGRUND_UNTEN),
)

/** Vergrößert eine Ansicht leicht und zeichnet einen Rahmen, solange sie den Fokus hat. */
fun View.fokusEffekt(radius: Float, skalierung: Float = 1.08f) {
    val rahmen = rund(Color.TRANSPARENT, radius, (3 * resources.displayMetrics.density).toInt(), Farben.AKZENT)
    setOnFocusChangeListener { v, hatFokus ->
        val s = if (hatFokus) skalierung else 1f
        v.animate().scaleX(s).scaleY(s).translationZ(if (hatFokus) 12f else 0f).setDuration(120).start()
        v.foreground = if (hatFokus) rahmen else null
    }
}

/** Ein fokussierbarer Knopf für Einstellungs-Listen. */
fun Context.knopf(beschriftung: CharSequence, beiKlick: () -> Unit): TextView =
    text(beschriftung, 18f).apply {
        val r = dp(12).toFloat()
        background = rund(Farben.KACHEL, r)
        setPadding(dp(24), dp(14), dp(24), dp(14))
        gravity = Gravity.CENTER_VERTICAL
        isFocusable = true
        isClickable = true
        setOnClickListener { beiKlick() }
        fokusEffekt(r, 1.02f)
    }

package de.fernseher.startseite

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock

/** Die eigene Startseite: Uhr, Favoriten, Eingänge, alle Apps. */
class HomeActivity : Activity() {

    private lateinit var scroll: ScrollView
    private lateinit var inhalt: LinearLayout

    private var kachelBreite = 0
    private var kachelHoehe = 0
    private var abstand = 0
    private var rand = 0
    private val spalten = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val breite = resources.displayMetrics.widthPixels
        rand = (breite * 0.05f).toInt()
        abstand = (breite * 0.015f).toInt()
        kachelBreite = (breite - 2 * rand - (spalten - 1) * abstand) / spalten
        kachelHoehe = kachelBreite * 9 / 16

        inhalt = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(rand, rand / 2, rand, rand)
            clipChildren = false
            clipToPadding = false
        }
        scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            clipChildren = false
            isFillViewport = true
            background = verlauf()
            addView(inhalt)
        }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        aufbauen(currentFocus?.tag as? String)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Home erneut gedrückt: nach oben und auf die erste Kachel.
        scroll.post { scroll.smoothScrollTo(0, 0) }
        aufbauen(null)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Zurück auf der Startseite macht nichts.
        if (keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.onKeyDown(keyCode, event)
    }

    private fun aufbauen(fokusId: String?) {
        inhalt.removeAllViews()
        val apps = Katalog.apps(this)
        val eingaenge = Katalog.eingaenge(this)
        val nachId = HashMap<String, View>()

        inhalt.addView(kopfzeile())

        // Favoriten
        val favoriten = Prefs.favoriten(this).mapNotNull { id ->
            val ziel = Ziel.ausId(id)
            when (ziel.typ) {
                Ziel.APP -> apps.firstOrNull { it.paket == ziel.wert }?.let { appKachel(it, "fav:$id") }
                Ziel.EINGANG -> eingaenge.firstOrNull { it.inputId == ziel.wert }?.let { eingangKachel(it, "fav:$id") }
                else -> null
            }
        }
        if (favoriten.isEmpty()) {
            inhalt.addView(reihe("Favoriten", listOf(hinweisKachel())))
        } else {
            inhalt.addView(reihe("Favoriten", favoriten))
        }

        if (eingaenge.isNotEmpty()) {
            inhalt.addView(reihe("Eingänge", eingaenge.map { eingangKachel(it, it.ziel.id) }))
        }

        // Alle Apps als Raster
        inhalt.addView(titel("Alle Apps"))
        apps.chunked(spalten).forEach { zeile ->
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                clipChildren = false
                clipToPadding = false
                setPadding(0, abstand / 2, 0, abstand / 2)
            }
            zeile.forEach { box.addView(appKachel(it, it.ziel.id)) }
            inhalt.addView(box)
        }

        sammeln(inhalt, nachId)
        val ziel = fokusId?.let { nachId[it] } ?: nachId.entries.firstOrNull { it.key.startsWith("fav:") }?.value
            ?: nachId.values.firstOrNull()
        ziel?.post { ziel.requestFocus() }
    }

    private fun sammeln(v: View, karte: HashMap<String, View>) {
        (v.tag as? String)?.let { if (v.isFocusable) karte.putIfAbsent(it, v) }
        if (v is ViewGroup) for (i in 0 until v.childCount) sammeln(v.getChildAt(i), karte)
    }

    private fun kopfzeile(): View {
        val zeile = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, abstand)
        }
        val uhrBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        uhrBox.addView(TextClock(this).apply {
            format24Hour = "HH:mm"
            format12Hour = "HH:mm"
            textSize = 54f
            setTextColor(Farben.TEXT)
        })
        uhrBox.addView(TextClock(this).apply {
            format24Hour = "EEEE, d. MMMM"
            format12Hour = "EEEE, d. MMMM"
            textSize = 18f
            setTextColor(Farben.TEXT_LEISE)
        })
        zeile.addView(uhrBox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        zeile.addView(knopf("⚙  Einstellungen") {
            startActivity(Intent(this, EinstellungenActivity::class.java))
        }.apply { tag = "einstellungen" })
        return zeile
    }

    private fun titel(s: String) = text(s, 20f, Farben.TEXT_LEISE, fett = true).apply {
        setPadding(0, abstand, 0, abstand / 2)
    }

    private fun reihe(ueberschrift: String, kacheln: List<View>): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
        }
        box.addView(titel(ueberschrift))
        val zeile = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }
        kacheln.forEach { zeile.addView(it) }
        val hs = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipChildren = false
            clipToPadding = false
            setPadding(0, abstand / 2, rand, abstand / 2)
            addView(zeile)
        }
        box.addView(hs)
        return box
    }

    private fun appKachel(app: AppEintrag, tag: String) =
        kachel(tag, app.ziel, app.name, app.banner, app.icon)

    private fun eingangKachel(e: Eingang, tag: String) =
        kachel(tag, e.ziel, e.name, null, null, untertitel = "Eingang")

    private fun kachel(
        tag: String,
        ziel: Ziel,
        name: String,
        banner: Drawable?,
        icon: Drawable?,
        untertitel: String? = null,
    ): View {
        val radius = dp(14).toFloat()
        val frame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(kachelBreite, kachelHoehe).apply { marginEnd = abstand }
            isFocusable = true
            isClickable = true
            this.tag = tag
            background = rund(Farben.KACHEL, radius)
            clipToOutline = true
            contentDescription = name
        }
        if (banner != null) {
            frame.addView(ImageView(this).apply {
                setImageDrawable(banner)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        } else {
            val spalte = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(8), dp(8), dp(8))
            }
            if (icon != null) {
                spalte.addView(ImageView(this).apply { setImageDrawable(icon) },
                    LinearLayout.LayoutParams(kachelHoehe / 3, kachelHoehe / 3))
            }
            if (untertitel != null) spalte.addView(text(untertitel, 13f, Farben.TEXT_LEISE))
            spalte.addView(text(name, 17f, fett = true).apply {
                gravity = Gravity.CENTER
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
            })
            frame.addView(spalte, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        frame.fokusEffekt(radius)
        frame.setOnClickListener { Starter.starten(this, ziel) }
        frame.setOnLongClickListener { optionen(ziel, name, tag); true }
        frame.setOnKeyListener { _, code, ev ->
            if (code == KeyEvent.KEYCODE_MENU && ev.action == KeyEvent.ACTION_UP) {
                optionen(ziel, name, tag); true
            } else false
        }
        return frame
    }

    private fun hinweisKachel(): View {
        val radius = dp(14).toFloat()
        return FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(kachelBreite * 2 + abstand, kachelHoehe)
            isFocusable = true
            tag = "hinweis"
            background = rund(Farben.KACHEL, radius)
            addView(text("Noch keine Favoriten.\nOK lange drücken (oder Menü-Taste) auf einer App → „Zu Favoriten“.", 16f, Farben.TEXT_LEISE).apply {
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(16), dp(16), dp(16))
            }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            fokusEffekt(radius, 1.03f)
        }
    }

    private fun optionen(ziel: Ziel, name: String, tag: String) {
        val fav = Prefs.favoriten(this).toMutableList()
        val pos = fav.indexOf(ziel.id)
        val punkte = mutableListOf<Pair<String, () -> String?>>()
        if (pos >= 0) {
            if (pos > 0) punkte += "◀  Nach links" to {
                fav.removeAt(pos); fav.add(pos - 1, ziel.id); Prefs.setzeFavoriten(this, fav); "fav:${ziel.id}"
            }
            if (pos < fav.size - 1) punkte += "Nach rechts  ▶" to {
                fav.removeAt(pos); fav.add(pos + 1, ziel.id); Prefs.setzeFavoriten(this, fav); "fav:${ziel.id}"
            }
            punkte += "Aus Favoriten entfernen" to {
                fav.removeAt(pos); Prefs.setzeFavoriten(this, fav); ziel.id
            }
        } else {
            punkte += "★  Zu Favoriten hinzufügen" to {
                fav.add(ziel.id); Prefs.setzeFavoriten(this, fav); "fav:${ziel.id}"
            }
        }
        if (ziel.typ == Ziel.APP) {
            punkte += "App-Info" to {
                try {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ziel.wert, null)))
                } catch (e: Exception) { /* nicht verfügbar */ }
                tag
            }
        }
        AlertDialog.Builder(this)
            .setTitle(name)
            .setItems(punkte.map { it.first }.toTypedArray()) { _, i ->
                val neuerFokus = punkte[i].second()
                aufbauen(neuerFokus)
            }
            .show()
    }
}

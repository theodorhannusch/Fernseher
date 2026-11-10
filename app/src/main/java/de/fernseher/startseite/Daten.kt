package de.fernseher.startseite

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.view.KeyEvent
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

/** Wohin eine Kachel oder Taste führt. */
data class Ziel(val typ: String, val wert: String = "") {
    /** Eindeutige Kennung, z. B. "app:com.netflix.ninja" oder "input:...HW5". */
    val id: String get() = if (wert.isEmpty()) typ else "$typ:$wert"

    companion object {
        const val APP = "app"
        const val EINGANG = "input"
        const val STARTSEITE = "home"
        const val ZURUECK = "back"
        const val SPERREN = "sperren"

        fun ausId(id: String): Ziel {
            val i = id.indexOf(':')
            return if (i < 0) Ziel(id) else Ziel(id.substring(0, i), id.substring(i + 1))
        }
    }
}

/** Art des Tastendrucks. */
object Art {
    const val KURZ = "kurz"
    const val LANG = "lang"
    const val DOPPELT = "doppelt"
    val ALLE = listOf(KURZ, LANG, DOPPELT)
    fun name(art: String) = when (art) {
        KURZ -> "kurz"
        LANG -> "lang"
        DOPPELT -> "doppelt"
        else -> art
    }
}

data class Belegung(val keyCode: Int, val art: String, val ziel: Ziel)

fun tastenName(keyCode: Int): String =
    KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")

/** Alle Einstellungen der App, gespeichert in SharedPreferences. */
object Prefs {
    fun sp(c: Context): SharedPreferences =
        c.getSharedPreferences("startseite", Context.MODE_PRIVATE)

    private val VORSCHLAEGE = listOf(
        "com.netflix.ninja",
        "com.google.android.youtube.tv",
        "com.amazon.amazonvideo.livingroom",
        "com.disney.disneyplus",
        "com.spotify.tv.android",
        "com.apple.atve.androidtv.appletv",
    )

    fun favoriten(c: Context): List<String> {
        val sp = sp(c)
        if (!sp.contains("favoriten")) {
            // Erster Start: bekannte Streaming-Apps vorschlagen, sofern installiert.
            val pm = c.packageManager
            val start = VORSCHLAEGE.filter { p ->
                try { pm.getApplicationInfo(p, 0); true } catch (e: Exception) { false }
            }.map { Ziel(Ziel.APP, it).id }
            setzeFavoriten(c, start)
            return start
        }
        return sp.getString("favoriten", "")!!.split("\n").filter { it.isNotBlank() }
    }

    fun setzeFavoriten(c: Context, liste: List<String>) {
        sp(c).edit().putString("favoriten", liste.joinToString("\n")).apply()
    }

    fun aufwachen(c: Context) = sp(c).getBoolean("aufwachen", true)
    fun setzeAufwachen(c: Context, an: Boolean) = sp(c).edit().putBoolean("aufwachen", an).apply()

    fun ersetzen(c: Context) = sp(c).getBoolean("ersetzen", true)
    fun setzeErsetzen(c: Context, an: Boolean) = sp(c).edit().putBoolean("ersetzen", an).apply()

    fun belegungen(c: Context): List<Belegung> {
        val roh = sp(c).getString("belegungen", "[]") ?: "[]"
        return try {
            val arr = JSONArray(roh)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Belegung(o.getInt("taste"), o.getString("art"), Ziel.ausId(o.getString("ziel")))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setzeBelegungen(c: Context, liste: List<Belegung>) {
        val arr = JSONArray()
        liste.forEach {
            arr.put(JSONObject().put("taste", it.keyCode).put("art", it.art).put("ziel", it.ziel.id))
        }
        sp(c).edit().putString("belegungen", arr.toString()).apply()
    }
}

data class AppEintrag(val paket: String, val name: String, val banner: Drawable?, val icon: Drawable?) {
    val ziel get() = Ziel(Ziel.APP, paket)
}

data class Eingang(val inputId: String, val name: String) {
    val ziel get() = Ziel(Ziel.EINGANG, inputId)
}

/** Liest installierte Apps und HDMI-Eingänge aus. */
object Katalog {
    fun apps(c: Context): List<AppEintrag> {
        val pm = c.packageManager
        val gefunden = LinkedHashMap<String, android.content.pm.ResolveInfo>()
        for (kategorie in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val infos = try {
                pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(kategorie), 0)
            } catch (e: Exception) {
                emptyList()
            }
            for (ri in infos) {
                val p = ri.activityInfo.packageName
                if (p != c.packageName && p !in gefunden) gefunden[p] = ri
            }
        }
        return gefunden.map { (p, ri) ->
            val ai = ri.activityInfo
            AppEintrag(
                paket = p,
                name = ri.loadLabel(pm).toString(),
                banner = ai.loadBanner(pm) ?: ai.applicationInfo.loadBanner(pm),
                icon = ri.loadIcon(pm),
            )
        }.sortedBy { it.name.lowercase() }
    }

    fun eingaenge(c: Context): List<Eingang> {
        val tim = c.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return emptyList()
        return try {
            tim.tvInputList
                .filter { it.type == TvInputInfo.TYPE_HDMI && it.parentId == null && !it.isHidden(c) }
                .map { info ->
                    val name = info.loadCustomLabel(c)?.toString()?.takeIf { it.isNotBlank() }
                        ?: info.loadLabel(c).toString()
                    Eingang(info.id, name)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun name(c: Context, ziel: Ziel): String = when (ziel.typ) {
        Ziel.APP -> try {
            val pm = c.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(ziel.wert, 0)).toString()
        } catch (e: Exception) {
            ziel.wert
        }
        Ziel.EINGANG -> eingaenge(c).firstOrNull { it.inputId == ziel.wert }?.name ?: "Eingang"
        Ziel.STARTSEITE -> "Startseite"
        Ziel.ZURUECK -> "Zurück"
        Ziel.SPERREN -> "Taste gesperrt"
        else -> ziel.id
    }
}

/** Öffnet Apps, Eingänge und die Startseite. */
object Starter {
    fun intentFuer(c: Context, ziel: Ziel): Intent? {
        val intent = when (ziel.typ) {
            Ziel.APP -> c.packageManager.getLeanbackLaunchIntentForPackage(ziel.wert)
                ?: c.packageManager.getLaunchIntentForPackage(ziel.wert)
            Ziel.EINGANG -> Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(ziel.wert))
            Ziel.STARTSEITE -> Intent(c, HomeActivity::class.java)
            else -> null
        }
        return intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun starten(c: Context, ziel: Ziel): Boolean {
        return try {
            val intent = intentFuer(c, ziel)
            if (intent == null) {
                if (ziel.typ == Ziel.APP || ziel.typ == Ziel.EINGANG) {
                    Toast.makeText(c, "Nicht gefunden: ${Katalog.name(c, ziel)}", Toast.LENGTH_SHORT).show()
                }
                false
            } else {
                c.startActivity(intent)
                true
            }
        } catch (e: Exception) {
            Toast.makeText(c, "Konnte ${Katalog.name(c, ziel)} nicht öffnen", Toast.LENGTH_SHORT).show()
            false
        }
    }
}

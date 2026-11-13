package de.fernseher.startseite

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Bedienungshilfe-Dienst:
 *  1. belegt Fernbedienungstasten um (kurz / lang / doppelt),
 *  2. holt nach dem Standby die eigene Startseite nach vorn,
 *  3. ersetzt die Original-Startseite, sobald sie erscheint (z. B. nach Kaltstart oder Home-Taste).
 */
class TastenService : AccessibilityService() {

    companion object {
        @Volatile
        var instanz: TastenService? = null
            private set

        private const val LANG_MS = 600L
        private const val DOPPEL_MS = 350L

        /** Bekannte Original-Startseiten auf Android TV / Google TV. */
        private val BEKANNTE_LAUNCHER = setOf(
            "com.google.android.tvlauncher",
            "com.google.android.apps.tv.launcherx",
            "com.google.android.leanbacklauncher",
        )

        fun istAktiv(c: Context): Boolean {
            if (instanz != null) return true
            val eigener = ComponentName(c, TastenService::class.java).flattenToString()
            val liste = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            return liste.split(':').any { it.equals(eigener, ignoreCase = true) }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var belegungen: Map<Int, Map<String, Ziel>> = emptyMap()
    private var fremdeLauncher: Set<String> = emptySet()

    private val langRunnables = HashMap<Int, Runnable>()
    private val einzelRunnables = HashMap<Int, Runnable>()
    private val langGefeuert = HashSet<Int>()
    private val gedrueckt = HashSet<Int>()

    // Schutz gegen Endlosschleifen, falls die Startseite abstürzt.
    private val spruenge = ArrayDeque<Long>()
    private var pausiertBis = 0L

    private val prefHoerer = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> laden() }

    private val bildschirmAn = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON && Prefs.aufwachen(this@TastenService)) {
                handler.postDelayed({ zurStartseite() }, 400)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanz = this
        laden()
        fremdeLauncher = launcherErmitteln()
        Prefs.sp(this).registerOnSharedPreferenceChangeListener(prefHoerer)
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(bildschirmAn, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(bildschirmAn, filter)
        }
    }

    override fun onDestroy() {
        instanz = null
        try { unregisterReceiver(bildschirmAn) } catch (e: Exception) { }
        Prefs.sp(this).unregisterOnSharedPreferenceChangeListener(prefHoerer)
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onInterrupt() {}

    private fun laden() {
        belegungen = Prefs.belegungen(this)
            .groupBy { it.keyCode }
            .mapValues { (_, liste) -> liste.associate { it.art to it.ziel } }
    }

    private fun launcherErmitteln(): Set<String> {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val gefunden = try {
            packageManager.queryIntentActivities(home, 0).map { it.activityInfo.packageName }
        } catch (e: Exception) {
            emptyList()
        }
        // "FallbackHome" der Einstellungen-App beim Booten nicht anfassen.
        return (gefunden + BEKANNTE_LAUNCHER)
            .filter { it != packageName && !it.contains("settings", ignoreCase = true) }
            .toSet()
    }

    // ---------- Original-Startseite ersetzen ----------

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val paket = event.packageName?.toString() ?: return
        if (paket in fremdeLauncher && Prefs.ersetzen(this)) zurStartseite()
    }

    fun zurStartseite() {
        val jetzt = System.currentTimeMillis()
        if (jetzt < pausiertBis) return
        if (spruenge.isNotEmpty() && jetzt - spruenge.last() < 1500) return
        spruenge.addLast(jetzt)
        while (spruenge.isNotEmpty() && jetzt - spruenge.first() > 30_000) spruenge.removeFirst()
        if (spruenge.size > 6) {
            // Zu viele Sprünge in kurzer Zeit: 5 Minuten Pause, damit der TV bedienbar bleibt.
            pausiertBis = jetzt + 5 * 60_000
            spruenge.clear()
            return
        }
        try {
            startActivity(
                Intent(this, HomeActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
        } catch (e: Exception) { }
    }

    // ---------- Tasten ----------

    override fun onKeyEvent(event: KeyEvent): Boolean {
        KeyLog.melden(event, KeyLog.QUELLE_DIENST)
        if (KeyLog.durchlassen) {
            // Im Tasten-Test/bei der Aufnahme Home festhalten, damit der Test nicht verlassen wird.
            return event.keyCode == KeyEvent.KEYCODE_HOME
        }
        val karte = belegungen[event.keyCode] ?: return false
        return verarbeiten(event, karte)
    }

    private fun verarbeiten(e: KeyEvent, karte: Map<String, Ziel>): Boolean {
        val k = e.keyCode
        when (e.action) {
            KeyEvent.ACTION_DOWN -> {
                if (e.repeatCount == 0) {
                    gedrueckt.add(k)
                    langGefeuert.remove(k)
                    karte[Art.LANG]?.let { ziel ->
                        val r = Runnable {
                            langRunnables.remove(k)
                            langGefeuert.add(k)
                            ausfuehren(ziel)
                        }
                        langRunnables[k] = r
                        handler.postDelayed(r, LANG_MS)
                    }
                }
            }
            KeyEvent.ACTION_UP -> {
                // Loslassen ohne vorheriges Drücken (z. B. nach der Tasten-Aufnahme) ignorieren.
                if (!gedrueckt.remove(k)) return true
                langRunnables.remove(k)?.let { handler.removeCallbacks(it) }
                if (langGefeuert.remove(k)) return true
                val doppelt = karte[Art.DOPPELT]
                if (doppelt != null) {
                    val wartend = einzelRunnables.remove(k)
                    if (wartend != null) {
                        handler.removeCallbacks(wartend)
                        ausfuehren(doppelt)
                    } else {
                        val r = Runnable {
                            einzelRunnables.remove(k)
                            kurz(k, karte)
                        }
                        einzelRunnables[k] = r
                        handler.postDelayed(r, DOPPEL_MS)
                    }
                } else {
                    kurz(k, karte)
                }
            }
        }
        return true
    }

    private fun kurz(keyCode: Int, karte: Map<String, Ziel>) {
        val ziel = karte[Art.KURZ]
        if (ziel != null) {
            ausfuehren(ziel)
            return
        }
        // Kein Kurz-Ziel: Originalfunktion nachbilden, soweit ein Dienst das kann.
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            KeyEvent.KEYCODE_HOME -> zurStartseite()
        }
    }

    private fun ausfuehren(ziel: Ziel) {
        when (ziel.typ) {
            Ziel.SPERREN -> {}
            Ziel.ZURUECK -> performGlobalAction(GLOBAL_ACTION_BACK)
            Ziel.STARTSEITE -> {
                spruenge.clear() // bewusster Wunsch, nicht drosseln
                zurStartseite()
            }
            else -> Starter.starten(this, ziel)
        }
    }
}

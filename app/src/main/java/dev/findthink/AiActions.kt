package dev.findthink

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.widget.Toast
import org.json.JSONObject
import java.net.URI

sealed class Action {
    data class OpenApp(val name: String) : Action()
    data class SearchStore(val query: String) : Action()
    data class SetTimer(val seconds: Int, val label: String) : Action()
    data class OpenUrl(val url: String) : Action()
}

object AiActions {
    private const val PREFIX = "ACTION:"
    private const val MAX_ACTIONS = 3

    fun parse(text: String): List<Action> =
        text.lineSequence().map { it.trim() }
            .filter { it.startsWith(PREFIX) }
            .mapNotNull { parseOne(it.removePrefix(PREFIX).trim()) }
            .take(MAX_ACTIONS).toList()

    fun stripActions(text: String): String =
        text.lineSequence().filterNot { it.trim().startsWith(PREFIX) }.joinToString("\n").trim()

    fun parseOne(json: String): Action? {
        val o = try { JSONObject(json) } catch (e: Exception) { return null }
        fun s(k: String, max: Int): String? =
            (o.opt(k) as? String)?.trim()?.takeIf { it.isNotEmpty() && it.length <= max }
        return when (o.opt("action")) {
            "open_app" -> s("app", 100)?.let { Action.OpenApp(it) }
            "search_store" -> s("query", 100)?.let { Action.SearchStore(it) }
            "set_timer" -> {
                val sec = o.opt("seconds") as? Int ?: return null
                if (sec !in 1..86400) return null
                val label = o.opt("label")
                if (label != null && label !is String) return null
                Action.SetTimer(sec, ((label as? String) ?: "").take(60))
            }
            "open_url" -> s("url", 2000)?.takeIf { validUrl(it) }?.let { Action.OpenUrl(it) }
            else -> null
        }
    }

    fun validUrl(s: String): Boolean = try {
        val u = URI(s)
        u.scheme?.equals("https", ignoreCase = true) == true && !u.host.isNullOrBlank() && u.userInfo == null
    } catch (e: Exception) {
        false
    }

    fun openApp(a: Activity, pkg: String) {
        a.packageManager.getLaunchIntentForPackage(pkg)?.let { a.startActivity(it) }
    }

    fun searchStore(a: Activity, q: String) {
        val e = Uri.encode(q)
        try {
            a.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$e&c=apps")))
        } catch (x: ActivityNotFoundException) {
            safeView(a, "https://play.google.com/store/search?q=$e&c=apps")
        }
    }

    fun setTimer(a: Activity, seconds: Int, label: String) {
        val i = Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            .putExtra(AlarmClock.EXTRA_MESSAGE, label)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        try { a.startActivity(i) } catch (x: Exception) { toast(a, "No timer app available") }
    }

    fun openUrl(a: Activity, url: String) {
        if (validUrl(url)) safeView(a, url)
    }

    private fun safeView(a: Activity, url: String) {
        try { a.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (x: Exception) { toast(a, "Nothing can open this") }
    }

    private fun toast(a: Activity, m: String) = Toast.makeText(a, m, Toast.LENGTH_SHORT).show()
}

package dev.findthink

import android.app.Activity
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class ThinkView(
    private val a: Activity,
    private val box: LinearLayout,
    private val scroll: ScrollView,
) {
    private var call: AiClient.Call? = null
    private var alive = true
    private val main = Handler(Looper.getMainLooper())

    fun send(q: String) {
        val query = q.trim()
        if (query.isEmpty()) return
        call?.cancel()
        addText("\u203a $query", 0xFFAAD4FF.toInt())
        val reply = addText("\u2026", Color.WHITE)
        val sb = StringBuilder()
        call = AiClient.stream(
            a, query,
            onDelta = { d -> main.post { if (alive) { sb.append(d); reply.text = AiActions.stripActions(sb.toString()); scroll.fullScroll(View.FOCUS_DOWN) } } },
            onDone = { main.post { if (alive) finish(sb.toString(), reply) } },
            onError = { m -> main.post { if (alive) reply.text = m } },
        )
    }

    private fun finish(full: String, reply: TextView) {
        reply.text = AiActions.stripActions(full).ifEmpty { "(no text)" }
        val acts = AiActions.parse(full)
        if (acts.isEmpty()) return
        val index = Index.load(a.packageManager, a.packageName)
        for (act in acts) proposal(act, index)
    }

    private fun proposal(act: Action, index: List<AppEntry>) {
        val pair: Pair<String, (() -> Unit)?> = when (act) {
            is Action.OpenApp -> {
                val e = Index.resolve(index, act.name)
                if (e == null) "App not found: ${act.name}" to null
                else "Open ${e.label}" to { AiActions.openApp(a, e.pkg) }
            }
            is Action.SearchStore -> "Search Play Store: ${act.query}" to { AiActions.searchStore(a, act.query) }
            is Action.SetTimer -> "Set timer: ${act.seconds}s ${act.label}".trim() to { AiActions.setTimer(a, act.seconds, act.label) }
            is Action.OpenUrl -> "Open ${act.url}" to { AiActions.openUrl(a, act.url) }
        }
        val (label, run) = pair
        val b = Button(a)
        b.text = label
        b.isAllCaps = false
        b.isEnabled = run != null
        b.setOnClickListener { run?.invoke() }
        box.addView(b, LinearLayout.LayoutParams(-1, -2))
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun addText(t: CharSequence, color: Int): TextView {
        val tv = TextView(a)
        tv.text = t
        tv.textSize = 16f
        tv.setTextColor(color)
        tv.setBackgroundColor(0x66000000)
        tv.setPadding(a.dp(14), a.dp(8), a.dp(14), a.dp(8))
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.topMargin = a.dp(4)
        box.addView(tv, lp)
        return tv
    }

    fun stop() {
        alive = false
        call?.cancel(); call = null
        main.removeCallbacksAndMessages(null)
        box.removeAllViews()
    }
}

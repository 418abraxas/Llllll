package dev.findthink

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppEntry(val label: String, val pkg: String) {
    val lower: String = label.lowercase()
}

data class Row(val label: String, val pkg: String?)

object Scorer {
    fun match(lower: String, q: String): Int {
        if (q.isEmpty()) return 0
        if (lower == q) return 100
        if (lower.startsWith(q)) return 80
        if (lower.split(' ', '-', '.', '_').any { it.startsWith(q) }) return 60
        if (lower.contains(q)) return 40
        var i = 0
        for (ch in lower) if (i < q.length && ch == q[i]) i++
        return if (i == q.length) 20 else 0
    }

    fun rank(apps: List<AppEntry>, query: String, count: (String) -> Int, limit: Int = 50): List<AppEntry> {
        val q = query.trim().lowercase()
        val scored = ArrayList<Pair<AppEntry, Int>>()
        for (app in apps) {
            val boost = minOf(count(app.pkg), 20)
            val s = if (q.isEmpty()) boost else {
                val m = match(app.lower, q)
                if (m == 0) 0 else m * 10 + boost
            }
            if (s > 0) scored.add(app to s)
        }
        scored.sortWith(
            compareByDescending<Pair<AppEntry, Int>> { it.second }
                .thenBy { it.first.lower }
                .thenBy { it.first.pkg }
        )
        return scored.take(if (q.isEmpty()) minOf(limit, 8) else limit).map { it.first }
    }
}

object Index {
    fun load(pm: PackageManager, self: String): List<AppEntry> {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(i, 0)
            .filter { it.activityInfo.packageName != self }
            .map { AppEntry(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .distinctBy { it.pkg }
    }

    fun resolve(apps: List<AppEntry>, name: String): AppEntry? {
        val q = name.trim().lowercase()
        if (q.isEmpty()) return null
        return apps.firstOrNull { it.lower == q }
            ?: apps.filter { it.lower.startsWith(q) }.minByOrNull { it.lower }
    }
}

class FindView(private val a: Activity, private val list: RecyclerView) {
    private var apps: List<AppEntry> = emptyList()
    private var counts: Map<String, Int> = emptyMap()
    private var query = ""
    private var alive = true
    private val main = Handler(Looper.getMainLooper())
    private val prefs = a.getSharedPreferences("rank", 0)
    private val adapter = RowAdapter(a) { launch(it) }

    init {
        list.layoutManager = LinearLayoutManager(a)
        list.adapter = adapter
        val pm = a.packageManager
        val self = a.packageName
        Thread {
            val idx = Index.load(pm, self)
            val c = HashMap<String, Int>()
            for ((k, v) in prefs.all) if (v is Int) c[k] = v
            main.post {
                if (alive) { apps = idx; counts = c; update(query) }
            }
        }.start()
    }

    fun update(q: String) {
        query = q
        var rows = Scorer.rank(apps, q, { counts[it] ?: 0 }).map { Row(it.label, it.pkg) }
        if (rows.isEmpty() && q.isNotBlank() && apps.isNotEmpty()) {
            rows = listOf(Row("Search Play Store for \u201c${q.trim()}\u201d", null))
        }
        adapter.set(rows)
    }

    fun launchTop() {
        adapter.items.firstOrNull()?.let { launch(it) }
    }

    private fun launch(r: Row) {
        val pkg = r.pkg
        if (pkg == null) { AiActions.searchStore(a, query.trim()); return }
        prefs.edit().putInt(pkg, minOf(prefs.getInt(pkg, 0) + 1, 1000)).apply()
        a.packageManager.getLaunchIntentForPackage(pkg)?.let { a.startActivity(it) }
    }

    fun stop() {
        alive = false
        main.removeCallbacksAndMessages(null)
        adapter.set(emptyList())
        list.adapter = null
        apps = emptyList()
        counts = emptyMap()
    }
}

class RowAdapter(private val a: Activity, private val onClick: (Row) -> Unit) :
    RecyclerView.Adapter<RowAdapter.VH>() {

    var items: List<Row> = emptyList()
        private set

    class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)

    fun set(r: List<Row>) { items = r; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = TextView(a).apply {
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(0x66000000)
            gravity = Gravity.CENTER_VERTICAL
            compoundDrawablePadding = a.dp(12)
            setPadding(a.dp(16), a.dp(8), a.dp(16), a.dp(8))
            layoutParams = RecyclerView.LayoutParams(MATCH, WRAP)
        }
        return VH(tv)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val r = items[position]
        h.tv.text = r.label
        val bmp = r.pkg?.let { Memory.icon(a.packageManager, it) }
        val d = bmp?.let { BitmapDrawable(a.resources, it).apply { setBounds(0, 0, a.dp(40), a.dp(40)) } }
        h.tv.setCompoundDrawablesRelative(d, null, null, null)
        h.tv.setOnClickListener { onClick(r) }
    }

    override fun getItemCount() = items.size
}

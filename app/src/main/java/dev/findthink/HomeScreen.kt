package dev.findthink

import android.app.Activity
import android.app.Dialog
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

fun Context.dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

class Ui(private val a: Activity) {
    val root = LinearLayout(a)
    private val widgetBox = LinearLayout(a)
    private val content = FrameLayout(a)
    private val input = EditText(a)
    private var mode = 0
    private var find: FindView? = null
    private var think: ThinkView? = null
    private var widgets: Widgets? = null
    private var dialog: Dialog? = null
    private val findBtn = bar("Find") { setMode(0) }
    private val thinkBtn = bar("Think") { setMode(1) }

    init {
        root.orientation = LinearLayout.VERTICAL
        root.fitsSystemWindows = true
        widgetBox.orientation = LinearLayout.VERTICAL
        root.addView(widgetBox, LinearLayout.LayoutParams(MATCH, WRAP))
        root.addView(content, LinearLayout.LayoutParams(MATCH, 0, 1f))

        val row = LinearLayout(a)
        row.orientation = LinearLayout.HORIZONTAL
        val lp = { LinearLayout.LayoutParams(0, WRAP, 1f) }
        row.addView(findBtn, lp())
        row.addView(thinkBtn, lp())
        row.addView(bar("+ Widget") { widgets?.add() }, lp())
        row.addView(bar("Settings") { dialog = Settings.show(a) }, lp())
        root.addView(row, LinearLayout.LayoutParams(MATCH, WRAP))

        input.setSingleLine(true)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.imeOptions = EditorInfo.IME_ACTION_GO
        input.textSize = 18f
        input.setTextColor(Color.WHITE)
        input.setHintTextColor(0x99FFFFFF.toInt())
        input.setBackgroundColor(0x99000000.toInt())
        input.setPadding(a.dp(16), a.dp(14), a.dp(16), a.dp(14))
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, af: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (mode == 0) find?.update(s?.toString() ?: "")
            }
        })
        input.setOnEditorActionListener { _, _, _ ->
            if (mode == 0) {
                find?.launchTop()
            } else {
                think?.send(input.text.toString())
                input.setText("")
            }
            true
        }
        root.addView(input, LinearLayout.LayoutParams(MATCH, WRAP))
    }

    private fun bar(t: String, click: () -> Unit) = TextView(a).apply {
        text = t
        textSize = 15f
        gravity = android.view.Gravity.CENTER
        setTextColor(Color.WHITE)
        setPadding(a.dp(8), a.dp(10), a.dp(8), a.dp(10))
        setBackgroundColor(0x66000000)
        setOnClickListener { click() }
    }

    fun start() {
        widgets = Widgets(a).also { it.start(widgetBox) }
        setMode(mode)
    }

    private fun setMode(m: Int) {
        mode = m
        find?.stop(); think?.stop()
        find = null; think = null
        content.removeAllViews()
        if (m == 0) {
            val rv = RecyclerView(a)
            content.addView(rv, FrameLayout.LayoutParams(MATCH, MATCH))
            find = FindView(a, rv).also { it.update(input.text.toString()) }
            input.hint = "Find an app"
        } else {
            val sv = ScrollView(a)
            val box = LinearLayout(a)
            box.orientation = LinearLayout.VERTICAL
            sv.addView(box, FrameLayout.LayoutParams(MATCH, WRAP))
            content.addView(sv, FrameLayout.LayoutParams(MATCH, MATCH))
            think = ThinkView(a, box, sv)
            input.hint = "Ask anything, press Go"
        }
        findBtn.alpha = if (m == 0) 1f else 0.5f
        thinkBtn.alpha = if (m == 1) 1f else 0.5f
    }

    fun back() {
        if (input.text.isNotEmpty()) input.setText("") else if (mode == 1) setMode(0)
    }

    fun stop() {
        dialog?.dismiss(); dialog = null
        find?.stop(); find = null
        think?.stop(); think = null
        widgets?.stop(widgetBox); widgets = null
        content.removeAllViews()
        Memory.trim()
    }
}

private const val HOST_ID = 1024
private const val REQ_PICK = 71
private const val REQ_CONF = 72
private const val MAX_WIDGETS = 2

class Widgets(private val a: Activity) {
    private val host = AppWidgetHost(a, HOST_ID)
    private val mgr = AppWidgetManager.getInstance(a)

    fun start(box: LinearLayout) {
        host.startListening()
        for (id in WidgetStore.ids(a)) {
            val info = mgr.getAppWidgetInfo(id) ?: continue
            val v = host.createView(a, id, info)
            v.setOnLongClickListener {
                WidgetStore.remove(a, id)
                host.deleteAppWidgetId(id)
                box.removeView(v)
                true
            }
            box.addView(v, LinearLayout.LayoutParams(MATCH, maxOf(info.minHeight, a.dp(80))))
        }
    }

    fun stop(box: LinearLayout) {
        try { host.stopListening() } catch (e: Exception) { }
        box.removeAllViews()
    }

    fun add() {
        if (WidgetStore.ids(a).size >= MAX_WIDGETS) {
            Toast.makeText(a, "Max $MAX_WIDGETS widgets. Long-press one to remove.", Toast.LENGTH_SHORT).show()
            return
        }
        val id = host.allocateAppWidgetId()
        WidgetStore.setPending(a, id)
        val i = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, ArrayList())
            .putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, ArrayList())
        a.startActivityForResult(i, REQ_PICK)
    }
}

object WidgetStore {
    private fun p(c: Context) = c.getSharedPreferences("widgets", 0)

    fun ids(c: Context): List<Int> =
        (p(c).getString("ids", "") ?: "").split(',').mapNotNull { it.toIntOrNull() }

    private fun put(c: Context, l: List<Int>) = p(c).edit().putString("ids", l.joinToString(",")).apply()
    fun remove(c: Context, id: Int) = put(c, ids(c) - id)
    fun setPending(c: Context, id: Int) = p(c).edit().putInt("pending", id).apply()
    private fun pending(c: Context) = p(c).getInt("pending", -1)

    fun onResult(a: Activity, req: Int, res: Int, data: Intent?) {
        if (req != REQ_PICK && req != REQ_CONF) return
        val id = pending(a)
        if (id < 0) return
        val host = AppWidgetHost(a, HOST_ID)
        val mgr = AppWidgetManager.getInstance(a)
        if (res != Activity.RESULT_OK) { host.deleteAppWidgetId(id); return }
        val info = mgr.getAppWidgetInfo(id)
        if (info == null) {
            host.deleteAppWidgetId(id)
            Toast.makeText(a, "Widget was not bound (unverified on this device).", Toast.LENGTH_LONG).show()
            return
        }
        if (req == REQ_PICK && info.configure != null) {
            host.startAppWidgetConfigureActivityForResult(a, id, 0, REQ_CONF, null)
            return
        }
        put(a, ids(a) + id)
    }
}

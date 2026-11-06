package dev.findthink

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager

class MainActivity : Activity() {
    private var ui: Ui? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onStart() {
        super.onStart()
        val u = Ui(this)
        ui = u
        setContentView(u.root)
        u.start()
    }

    override fun onStop() {
        ui?.stop()
        ui = null
        setContentView(View(this))
        super.onStop()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Memory.trim()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        ui?.back()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        WidgetStore.onResult(this, requestCode, resultCode, data)
    }
}

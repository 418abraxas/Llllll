package dev.findthink

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache

object Memory {
    private const val CAP_BYTES = 1_048_576
    private const val PX = 96

    private val icons = object : LruCache<String, Bitmap>(CAP_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun icon(pm: PackageManager, pkg: String): Bitmap? {
        icons.get(pkg)?.let { return it }
        return try {
            val d = pm.getApplicationIcon(pkg)
            val bmp = Bitmap.createBitmap(PX, PX, Bitmap.Config.ARGB_8888)
            d.setBounds(0, 0, PX, PX)
            d.draw(Canvas(bmp))
            icons.put(pkg, bmp)
            bmp
        } catch (e: Exception) {
            null
        }
    }

    fun trim() = icons.evictAll()
}

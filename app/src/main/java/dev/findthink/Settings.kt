package dev.findthink

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.text.InputType
import android.util.Base64
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object Vault {
    private const val ALIAS = "findthink_key"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        g.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return g.generateKey()
    }

    fun seal(plain: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key())
        return Base64.encodeToString(c.iv + c.doFinal(plain.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    fun open(sealed: String): String? = try {
        val b = Base64.decode(sealed, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, b, 0, 12))
        String(c.doFinal(b, 12, b.size - 12), Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }
}

object Settings {
    private fun p(c: Context) = c.getSharedPreferences("settings", 0)

    fun endpoint(c: Context): String = p(c).getString("endpoint", "https://api.openai.com/v1/chat/completions")!!
    fun model(c: Context): String = p(c).getString("model", "gpt-4o-mini")!!
    fun shareLabels(c: Context): Boolean = p(c).getBoolean("share", false)
    fun apiKey(c: Context): String? =
        p(c).getString("key", null)?.let { Vault.open(it) }?.takeIf { it.isNotEmpty() }

    fun saveLastPayload(c: Context, s: String) = File(c.filesDir, "last_payload.json").writeText(s)
    fun lastPayload(c: Context): String =
        File(c.filesDir, "last_payload.json").takeIf { it.exists() }?.readText() ?: "(nothing sent yet)"

    fun show(a: Activity): AlertDialog {
        fun field(hint: String, value: String, pw: Boolean = false) = EditText(a).apply {
            this.hint = hint
            setText(value)
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or
                (if (pw) InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_TEXT_VARIATION_URI)
        }
        val ep = field("Endpoint (https only)", endpoint(a))
        val model = field("Model", model(a))
        val key = field(if (apiKey(a) != null) "API key (saved; blank keeps it)" else "API key", "", pw = true)
        val share = CheckBox(a).apply {
            text = "Also send my app labels with each request"
            isChecked = shareLabels(a)
        }
        val note = TextView(a).apply {
            text = "Default payload: your query plus a fixed instruction. Nothing else leaves the device."
        }
        val col = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(a.dp(20), a.dp(8), a.dp(20), 0)
            addView(ep); addView(model); addView(key); addView(share); addView(note)
        }
        return AlertDialog.Builder(a)
            .setTitle("Settings")
            .setView(ScrollView(a).apply { addView(col) })
            .setPositiveButton("Save") { _, _ ->
                val e = ep.text.toString().trim()
                if (!e.startsWith("https://")) {
                    Toast.makeText(a, "Endpoint must start with https://", Toast.LENGTH_LONG).show()
                } else {
                    val ed = p(a).edit()
                        .putString("endpoint", e)
                        .putString("model", model.text.toString().trim())
                        .putBoolean("share", share.isChecked)
                    val k = key.text.toString().trim()
                    if (k.isNotEmpty()) ed.putString("key", Vault.seal(k))
                    ed.apply()
                }
            }
            .setNeutralButton("Last payload") { _, _ ->
                AlertDialog.Builder(a).setTitle("Last payload sent")
                    .setMessage(lastPayload(a)).setPositiveButton("Close", null).show()
            }
            .setNegativeButton("Cancel", null)
            .create().also { it.show() }
    }
}

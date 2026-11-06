package dev.findthink

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object AiClient {
    const val SYSTEM = "You are a concise assistant inside a phone launcher. Answer briefly. " +
        "To propose an action, end your reply with one line per action, formatted exactly as ACTION: followed by JSON. " +
        "Allowed JSON: {\"action\":\"open_app\",\"app\":\"<name>\"} ; {\"action\":\"search_store\",\"query\":\"<text>\"} ; " +
        "{\"action\":\"set_timer\",\"seconds\":<integer>,\"label\":\"<text>\"} ; {\"action\":\"open_url\",\"url\":\"<https url>\"}. " +
        "Never claim an action has already been done."

    class Call {
        @Volatile var cancelled = false
        @Volatile var conn: HttpURLConnection? = null
        fun cancel() { cancelled = true; conn?.disconnect() }
    }

    fun buildPayload(model: String, query: String, labels: List<String>?): String {
        val sys = if (labels == null) SYSTEM else SYSTEM + "\nInstalled apps: " + labels.joinToString(", ")
        val msgs = JSONArray()
            .put(JSONObject().put("role", "system").put("content", sys))
            .put(JSONObject().put("role", "user").put("content", query))
        return JSONObject().put("model", model).put("stream", true).put("messages", msgs).toString()
    }

    fun parseDelta(json: String): String = try {
        val d = JSONObject(json).getJSONArray("choices").getJSONObject(0).optJSONObject("delta")
        if (d == null || d.isNull("content")) "" else d.optString("content", "")
    } catch (e: Exception) {
        ""
    }

    fun stream(
        ctx: Context,
        query: String,
        onDelta: (String) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ): Call {
        val call = Call()
        val key = Settings.apiKey(ctx)
        val ep = Settings.endpoint(ctx)
        if (key == null || !ep.startsWith("https://")) {
            onError("Open Settings and set an https endpoint and an API key.")
            return call
        }
        val labels = if (Settings.shareLabels(ctx)) Index.load(ctx.packageManager, ctx.packageName).map { it.label } else null
        val payload = buildPayload(Settings.model(ctx), query, labels)
        Settings.saveLastPayload(ctx, payload)
        val bytes = payload.toByteArray(Charsets.UTF_8)

        Thread {
            var c: HttpURLConnection? = null
            try {
                c = URL(ep).openConnection() as HttpURLConnection
                call.conn = c
                c.requestMethod = "POST"
                c.connectTimeout = 15000
                c.readTimeout = 60000
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.setRequestProperty("Accept", "text/event-stream")
                c.setRequestProperty("Authorization", "Bearer $key")
                c.outputStream.use { it.write(bytes) }
                if (c.responseCode !in 200..299) {
                    onError("HTTP ${c.responseCode}")
                    return@Thread
                }
                BufferedReader(InputStreamReader(c.inputStream, Charsets.UTF_8)).use { r ->
                    while (!call.cancelled) {
                        val line = r.readLine() ?: break
                        if (!line.startsWith("data:")) continue
                        val d = line.substring(5).trim()
                        if (d == "[DONE]") break
                        val delta = parseDelta(d)
                        if (delta.isNotEmpty()) onDelta(delta)
                    }
                }
                if (!call.cancelled) onDone()
            } catch (e: Exception) {
                if (!call.cancelled) onError("Network error: ${e.javaClass.simpleName}")
            } finally {
                c?.disconnect()
            }
        }.start()
        return call
    }
}

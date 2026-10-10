package com.jehad.miniwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.widget.RemoteViews
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 4x2 "Today" widget: fetches a day's check-ins/check-outs JSON
 * (mithaqstay's /api/v1/movements/day) every 30 min or on tap of ⟳.
 */
class TodayWidget : BaseWidget() {
    override fun onUpdate(c: Context, mgr: AppWidgetManager, ids: IntArray) {
        super.onUpdate(c, mgr, ids) // draw the cached answer right away
        Today.fetchAsync(c, ids, goAsync())
    }

    override fun onReceive(c: Context, intent: Intent) {
        super.onReceive(c, intent)
        if (intent.action == Today.ACTION_REFRESH) {
            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                Today.fetchAsync(c, intArrayOf(id), goAsync())
            }
        }
    }
}

object Today {
    const val ACTION_REFRESH = "com.jehad.miniwidgets.REFRESH_TODAY"
    private const val PREFS = "mini_widgets_today"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Result of one fetch: the parsed body, or an error to show. */
    data class Result(val body: JSONObject?, val error: String)

    fun fetchAsync(c: Context, ids: IntArray, pending: BroadcastReceiver.PendingResult?) {
        val app = c.applicationContext
        Thread {
            try {
                val mgr = AppWidgetManager.getInstance(app)
                ids.forEach { id ->
                    Store.defFor(app, id, "today")?.takeIf { it.type == "today" }?.let { fetch(app, it) }
                    Render.update(app, mgr, id)
                }
            } finally {
                pending?.finish()
            }
        }.start()
    }

    /** Blocking network call; never call on the main thread. */
    fun fetch(c: Context, d: WidgetDef): Result {
        val result = try {
            val conn = URL(d.url).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.setRequestProperty("Accept", "application/json")
                if (d.key.isNotBlank()) conn.setRequestProperty("X-Api-Key", d.key)
                when (val code = conn.responseCode) {
                    200 -> Result(JSONObject(conn.inputStream.bufferedReader().use { it.readText() }), "")
                    401 -> Result(null, "Key rejected")
                    else -> Result(null, "Error $code")
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: IOException) {
            Result(null, "Offline")
        } catch (e: Exception) {
            Result(null, "Bad response")
        }
        val edit = prefs(c).edit()
        if (result.body != null) {
            edit.putString("body_${d.id}", result.body.toString())
            edit.putLong("at_${d.id}", System.currentTimeMillis())
        }
        edit.putString("err_${d.id}", result.error).apply()
        return result
    }

    private fun cached(c: Context, d: WidgetDef): JSONObject? =
        prefs(c).getString("body_${d.id}", null)?.let {
            try {
                JSONObject(it)
            } catch (e: Exception) {
                null
            }
        }

    fun summary(body: JSONObject): String {
        val ins = body.optJSONArray("checkins")?.length() ?: 0
        val outs = body.optJSONArray("checkouts")?.length() ?: 0
        return "$ins in · $outs out"
    }

    /** Fills widget_today.xml for one widget from the cached answer. */
    fun fill(c: Context, v: RemoteViews, d: WidgetDef, widgetId: Int) {
        val body = cached(c, d)
        val p = prefs(c)
        val err = p.getString("err_${d.id}", "") ?: ""
        val at = p.getLong("at_${d.id}", 0L)

        v.setTextViewText(R.id.title, "${d.glyph}  ${d.label}".trim())
        v.setTextColor(R.id.title, d.accent)
        val time = if (at > 0) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(at)) else ""
        v.setTextViewText(R.id.status, listOf(err, time).filter { it.isNotBlank() }.joinToString(" · "))
        v.setTextColor(R.id.status, d.text)
        v.setTextColor(R.id.refresh, d.text)
        v.setTextColor(R.id.ins, d.text)
        v.setTextColor(R.id.outs, d.text)

        if (body == null) {
            v.setTextViewText(R.id.day, "")
            v.setTextViewText(R.id.ins, if (err.isBlank()) "Loading…" else "Tap ⟳ to retry")
            v.setTextViewText(R.id.outs, "")
        } else {
            v.setTextViewText(R.id.day, dayLabel(body))
            v.setTextColor(R.id.day, d.text)
            v.setTextViewText(R.id.ins, line("↓ In", body.optJSONArray("checkins")))
            v.setTextViewText(R.id.outs, line("↑ Out", body.optJSONArray("checkouts")))
        }

        // Tap the card → the dashboard on that day; tap ⟳ → fetch now.
        val open = body?.optString("dashboard_url").orEmpty().ifBlank { d.url }
        v.setOnClickPendingIntent(
            R.id.root,
            PendingIntent.getActivity(
                c, widgetId,
                Intent(Intent.ACTION_VIEW, Uri.parse(open)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        val refresh = Intent(c, TodayWidget::class.java)
            .setAction(ACTION_REFRESH)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        v.setOnClickPendingIntent(
            R.id.refresh,
            PendingIntent.getBroadcast(
                c, widgetId, refresh,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }

    private fun dayLabel(body: JSONObject): String {
        val raw = body.optString("date")
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw) ?: return raw
            val label = SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(date)
            if (body.optBoolean("is_today")) "Today · $label" else label
        } catch (e: Exception) {
            raw
        }
    }

    /** "↓ In (3)  103 · 105 · 112" — same-day turnovers in bold. */
    private fun line(prefix: String, rows: JSONArray?): CharSequence {
        val n = rows?.length() ?: 0
        val sb = SpannableStringBuilder("$prefix ($n)  ")
        if (rows == null || n == 0) {
            sb.append("—")
            return sb
        }
        for (i in 0 until n) {
            val row = rows.optJSONObject(i) ?: continue
            if (i > 0) sb.append(" · ")
            val start = sb.length
            sb.append(row.optString("unit", "?"))
            if (row.optBoolean("turnover")) {
                sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return sb
    }
}

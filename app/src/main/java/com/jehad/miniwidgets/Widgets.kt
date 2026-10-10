package com.jehad.miniwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.widget.RemoteViews

open class BaseWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { Render.update(c, mgr, it) }
    }

    override fun onDeleted(c: Context, ids: IntArray) {
        ids.forEach { Store.unassign(c, it) }
    }
}

/** 4x1 search-bar style widget. */
class BarWidget : BaseWidget()

/** 1x1 round button widget. */
class IconWidget : BaseWidget()

object Render {
    private val PROVIDERS = listOf(BarWidget::class.java, IconWidget::class.java)

    fun typeOf(mgr: AppWidgetManager, widgetId: Int): String =
        if (mgr.getAppWidgetInfo(widgetId)?.provider?.className == IconWidget::class.java.name) "icon"
        else "bar"

    fun update(c: Context, mgr: AppWidgetManager, widgetId: Int) {
        val type = typeOf(mgr, widgetId)
        val layout = if (type == "icon") R.layout.widget_icon else R.layout.widget_bar
        val v = RemoteViews(c.packageName, layout)
        val d = Store.defFor(c, widgetId, type)
        if (d != null) {
            // Tint the shape with the opaque color, then apply its alpha separately.
            v.setInt(R.id.bg, "setColorFilter", d.background or 0xFF000000.toInt())
            v.setInt(R.id.bg, "setImageAlpha", Color.alpha(d.background))
            v.setTextViewText(R.id.glyph, d.glyph)
            v.setTextColor(R.id.glyph, d.accent)
            if (type == "bar") {
                v.setTextViewText(R.id.label, d.label)
                v.setTextColor(R.id.label, d.text)
            }
            v.setContentDescription(R.id.root, d.label)
            v.setOnClickPendingIntent(R.id.root, pending(c, widgetId, d))
        }
        mgr.updateAppWidget(widgetId, v)
    }

    fun refreshAll(c: Context) {
        val mgr = AppWidgetManager.getInstance(c)
        PROVIDERS.forEach { cls ->
            mgr.getAppWidgetIds(ComponentName(c, cls)).forEach { update(c, mgr, it) }
        }
    }

    /** What tapping the widget opens. Null means nothing usable was found. */
    fun target(c: Context, d: WidgetDef): Intent? {
        val pm = c.packageManager
        if (d.url.startsWith("app:")) {
            return pm.getLaunchIntentForPackage(d.url.removePrefix("app:").trim())
        }
        if (d.url.isBlank()) {
            return if (d.pkg.isNotBlank()) pm.getLaunchIntentForPackage(d.pkg) else null
        }
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(d.url))
        if (d.pkg.isNotBlank()) {
            val inApp = Intent(view).setPackage(d.pkg)
            if (inApp.resolveActivity(pm) != null) return inApp
        }
        return view
    }

    private fun pending(c: Context, widgetId: Int, d: WidgetDef): PendingIntent {
        val intent = target(c, d) ?: Intent(c, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            c, widgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

package com.jehad.miniwidgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle

/** Shown when a widget is placed (or reconfigured): pick which design it uses. */
class ConfigureActivity : Activity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val mgr = AppWidgetManager.getInstance(this)
        val type = Render.typeOf(mgr, widgetId)
        val all = Store.all(this)
        // A Today widget needs a Today design (it carries the feed URL and key);
        // the other two can fall back to any design.
        val defs = all.filter { it.type == type }.ifEmpty { if (type == "today") emptyList() else all }

        val ui = Ui(this)
        val col = ui.column()
        col.addView(ui.title("Choose a design"))
        col.addView(
            ui.hint(
                if (defs.isEmpty()) "No Today design yet. Open Mini Widgets, paste or import one, then add this widget again."
                else "Add more from the Mini Widgets app with Import.",
            ),
        )
        defs.forEach { d -> col.addView(ui.defRow(d, onClick = { pick(mgr, d) })) }
        setContentView(ui.scroll(col))
    }

    private fun pick(mgr: AppWidgetManager, d: WidgetDef) {
        Store.assign(this, widgetId, d.id)
        Render.update(this, mgr, widgetId)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}

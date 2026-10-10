package com.jehad.miniwidgets

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Keeps widget designs and which design each placed widget uses. */
object Store {
    private const val PREFS = "mini_widgets"
    private const val KEY_DEFS = "defs"

    private val DEFAULTS = """
        {"widgets":[
          {"id":"ask-claude","type":"bar","label":"Ask Claude…","glyph":"✻",
           "url":"https://claude.ai/new","package":"com.anthropic.claude",
           "background":"#F2FFFFFF","text":"#5F6368","accent":"#D97757"},
          {"id":"ask-claude-dark","type":"bar","label":"Ask Claude…","glyph":"✻",
           "url":"https://claude.ai/new","package":"com.anthropic.claude",
           "background":"#F23B3B3D","text":"#C4C7C5","accent":"#D97757"},
          {"id":"claude-icon","type":"icon","label":"Claude","glyph":"✻",
           "url":"https://claude.ai/new","package":"com.anthropic.claude",
           "background":"#FFD97757","text":"#FFFFFFFF","accent":"#FFFFFFFF"}
        ]}
    """.trimIndent()

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun all(c: Context): List<WidgetDef> {
        val raw = prefs(c).getString(KEY_DEFS, null) ?: DEFAULTS
        return try {
            WidgetDef.parseMany(raw)
        } catch (e: Exception) {
            WidgetDef.parseMany(DEFAULTS)
        }
    }

    private fun toJson(defs: List<WidgetDef>): JSONObject {
        val arr = JSONArray()
        defs.forEach { arr.put(it.toJson()) }
        return JSONObject().put("widgets", arr)
    }

    private fun save(c: Context, defs: List<WidgetDef>) {
        prefs(c).edit().putString(KEY_DEFS, toJson(defs).toString()).apply()
    }

    /** Adds new designs; a design with an existing id replaces the old one. */
    fun merge(c: Context, incoming: List<WidgetDef>): Int {
        val byId = LinkedHashMap<String, WidgetDef>()
        all(c).forEach { byId[it.id] = it }
        incoming.forEach { byId[it.id] = it }
        save(c, byId.values.toList())
        return incoming.size
    }

    fun remove(c: Context, id: String) = save(c, all(c).filter { it.id != id })

    fun reset(c: Context) {
        prefs(c).edit().remove(KEY_DEFS).apply()
    }

    fun exportJson(c: Context): String = toJson(all(c)).toString(2)

    fun assign(c: Context, widgetId: Int, defId: String) {
        prefs(c).edit().putString("w_$widgetId", defId).apply()
    }

    fun unassign(c: Context, widgetId: Int) {
        prefs(c).edit().remove("w_$widgetId").apply()
    }

    fun defFor(c: Context, widgetId: Int, type: String): WidgetDef? {
        val defs = all(c)
        val id = prefs(c).getString("w_$widgetId", null)
        return defs.firstOrNull { it.id == id }
            ?: defs.firstOrNull { it.type == type }
            ?: defs.firstOrNull()
    }
}

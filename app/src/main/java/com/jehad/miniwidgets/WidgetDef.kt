package com.jehad.miniwidgets

import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/**
 * One widget design. Imported from / exported to JSON like:
 *
 * {"id":"ask-claude","type":"bar","label":"Ask Claude…","glyph":"✻",
 *  "url":"https://claude.ai/new","package":"com.anthropic.claude",
 *  "background":"#F2FFFFFF","text":"#5F6368","accent":"#D97757"}
 *
 * type    "bar" (search-bar style) or "icon" (round 1x1 button)
 * url     link to open, or "app:<package.name>" to just launch an app
 * package optional: prefer opening the link inside this app
 */
data class WidgetDef(
    val id: String,
    val type: String,
    val label: String,
    val glyph: String,
    val url: String,
    val pkg: String,
    val background: Int,
    val text: Int,
    val accent: Int,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("type", type)
        .put("label", label)
        .put("glyph", glyph)
        .put("url", url)
        .put("package", pkg)
        .put("background", hex(background))
        .put("text", hex(text))
        .put("accent", hex(accent))

    companion object {
        private fun hex(c: Int) = String.format("#%08X", c)

        private fun str(o: JSONObject, key: String, def: String): String =
            if (o.has(key) && !o.isNull(key)) o.getString(key) else def

        private fun color(s: String, def: Int): Int = try {
            if (s.isBlank()) def else Color.parseColor(s.trim())
        } catch (e: IllegalArgumentException) {
            def
        }

        private fun slug(s: String) =
            s.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "widget" }

        fun fromJson(o: JSONObject): WidgetDef {
            val type = if (str(o, "type", "bar") == "icon") "icon" else "bar"
            val label = str(o, "label", "Widget")
            return WidgetDef(
                id = str(o, "id", "").ifBlank { slug(label) + "-" + type },
                type = type,
                label = label,
                glyph = str(o, "glyph", "✻"),
                url = str(o, "url", ""),
                pkg = str(o, "package", ""),
                background = color(str(o, "background", ""), 0xF2FFFFFF.toInt()),
                text = color(str(o, "text", ""), 0xFF5F6368.toInt()),
                accent = color(str(o, "accent", ""), 0xFFD97757.toInt()),
            )
        }

        /** Accepts a single object, an array, or {"widgets":[...]}. */
        fun parseMany(raw: String): List<WidgetDef> {
            val arr = when (val v = JSONTokener(raw.trim()).nextValue()) {
                is JSONArray -> v
                is JSONObject -> v.optJSONArray("widgets") ?: JSONArray().put(v)
                else -> throw IllegalArgumentException("Not a JSON object or array")
            }
            return (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        }
    }
}

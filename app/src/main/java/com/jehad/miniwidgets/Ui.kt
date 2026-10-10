package com.jehad.miniwidgets

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Tiny view helpers so the app needs no UI libraries. */
class Ui(private val a: Activity) {
    private val dark =
        (a.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    val bg = if (dark) 0xFF121212.toInt() else 0xFFF7F7F5.toInt()
    val card = if (dark) 0xFF1E1E1E.toInt() else 0xFFFFFFFF.toInt()
    val fg = if (dark) 0xFFECECEC.toInt() else 0xFF1F1F1F.toInt()
    val muted = if (dark) 0xFF9AA0A6.toInt() else 0xFF6B6B6B.toInt()
    private val line = if (dark) 0x33FFFFFF else 0x1F000000

    fun dp(v: Int) = (v * a.resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radiusDp: Int, stroke: Boolean = false) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
        if (stroke) setStroke(dp(1), line)
    }

    private fun lp(w: Int, h: Int, top: Int = 0, end: Int = 0) =
        LinearLayout.LayoutParams(w, h).apply { topMargin = dp(top); marginEnd = dp(end) }

    fun column() = LinearLayout(a).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(28), dp(20), dp(28))
        setBackgroundColor(bg)
    }

    fun scroll(child: View) = ScrollView(a).apply {
        setBackgroundColor(bg)
        isFillViewport = true
        addView(child)
    }

    fun text(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(a).apply {
        text = t
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    fun title(t: String) = text(t, 26f, fg, bold = true)

    fun hint(t: String) = text(t, 14f, muted).apply { setPadding(0, dp(6), 0, dp(16)) }

    fun section(t: String) = text(t, 13f, muted, bold = true).apply {
        setPadding(0, dp(20), 0, dp(4))
        setAllCaps(true)
    }

    fun buttons(vararg items: Pair<String, () -> Unit>): View {
        val row = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL }
        items.forEach { (label, action) ->
            row.addView(text(label, 15f, fg).apply {
                background = rounded(card, 22, stroke = true)
                setPadding(dp(18), dp(10), dp(18), dp(10))
                setOnClickListener { action() }
            }, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, end = 8))
        }
        return HorizontalScrollView(a).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }
    }

    /** A card with a live-looking preview of the widget plus where it goes. */
    fun defRow(d: WidgetDef, onClick: () -> Unit, onLongClick: (() -> Unit)? = null): View {
        val preview: View = if (d.type == "icon") {
            FrameLayout(a).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(d.background)
                    setStroke(dp(1), line)
                }
                addView(text(d.glyph, 22f, d.accent).apply { gravity = Gravity.CENTER },
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                layoutParams = lp(dp(52), dp(52))
            }
        } else {
            LinearLayout(a).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(d.background, 26, stroke = true)
                setPadding(dp(16), 0, dp(16), 0)
                addView(text(d.glyph, 20f, d.accent))
                addView(text(d.label, 16f, d.text).apply {
                    setSingleLine()
                    ellipsize = TextUtils.TruncateAt.END
                    setPadding(dp(12), 0, 0, 0)
                })
                layoutParams = lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(52))
            }
        }

        val where = if (d.url.isBlank()) d.pkg else d.url
        val caption = text("${d.id}  ·  ${d.type}\n$where", 12f, muted).apply {
            setPadding(0, dp(8), 0, 0)
        }

        return LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(card, 18, stroke = true)
            setPadding(dp(14), dp(14), dp(14), dp(12))
            addView(preview)
            addView(caption)
            layoutParams = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, top = 10)
            isClickable = true
            setOnClickListener { onClick() }
            if (onLongClick != null) setOnLongClickListener { onLongClick(); true }
        }
    }
}

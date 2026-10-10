package com.jehad.miniwidgets

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var ui: Ui

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = Ui(this)
        handleIncoming(intent)
        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncoming(intent)
        render()
    }

    /** A .json file opened with this app. */
    private fun handleIncoming(i: Intent?) {
        if (i?.action == Intent.ACTION_VIEW) i.data?.let { importFrom(it) }
    }

    private fun render() {
        val col = ui.column()
        col.addView(ui.title("Mini Widgets"))
        col.addView(ui.hint("Long-press your home screen → Widgets → Mini Widgets.\nTap a design to test it, long-press to delete."))
        col.addView(ui.buttons(
            "Import file" to { pickFile() },
            "Paste JSON" to { importClipboard() },
            "Export" to { exportFile() },
            "Reset" to { confirmReset() },
        ))

        val defs = Store.all(this)
        listOf("bar" to "Search bars", "icon" to "Round buttons").forEach { (type, heading) ->
            val group = defs.filter { it.type == type }
            if (group.isNotEmpty()) {
                col.addView(ui.section(heading))
                group.forEach { d ->
                    col.addView(ui.defRow(d, onClick = { test(d) }, onLongClick = { confirmDelete(d) }))
                }
            }
        }
        setContentView(ui.scroll(col))
    }

    private fun test(d: WidgetDef) {
        val target = Render.target(this, d)
        if (target == null) {
            toast("Nothing installed can open this")
            return
        }
        try {
            startActivity(target)
        } catch (e: ActivityNotFoundException) {
            toast("Nothing installed can open this")
        }
    }

    // --- Import / export -------------------------------------------------

    private fun pickFile() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
        startActivityForResult(i, REQ_IMPORT)
    }

    private fun exportFile() {
        val i = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/json")
            .putExtra(Intent.EXTRA_TITLE, "mini-widgets.json")
        startActivityForResult(i, REQ_EXPORT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (resultCode != RESULT_OK || uri == null) return
        when (requestCode) {
            REQ_IMPORT -> importFrom(uri)
            REQ_EXPORT -> try {
                contentResolver.openOutputStream(uri, "wt")?.use {
                    it.write(Store.exportJson(this).toByteArray())
                }
                toast("Exported")
            } catch (e: Exception) {
                toast("Export failed: ${e.message}")
            }
        }
    }

    private fun importFrom(uri: Uri) {
        try {
            val raw = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalStateException("Can't read that file")
            importText(raw)
        } catch (e: Exception) {
            toast("Import failed: ${e.message}")
        }
        render()
    }

    private fun importClipboard() {
        val cm = getSystemService(ClipboardManager::class.java)
        val raw = cm?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
        if (raw.isNullOrBlank()) {
            toast("Clipboard is empty")
            return
        }
        try {
            importText(raw)
        } catch (e: Exception) {
            toast("Import failed: ${e.message}")
        }
        render()
    }

    private fun importText(raw: String) {
        val n = Store.merge(this, WidgetDef.parseMany(raw))
        Render.refreshAll(this)
        toast(if (n == 1) "Imported 1 design" else "Imported $n designs")
    }

    // --- Delete / reset --------------------------------------------------

    private fun confirmDelete(d: WidgetDef) {
        AlertDialog.Builder(this)
            .setTitle("Delete “${d.label}”?")
            .setMessage("Widgets using it will switch to another design.")
            .setPositiveButton("Delete") { _, _ ->
                Store.remove(this, d.id)
                Render.refreshAll(this)
                render()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle("Reset to defaults?")
            .setMessage("Removes every imported design.")
            .setPositiveButton("Reset") { _, _ ->
                Store.reset(this)
                Render.refreshAll(this)
                render()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        private const val REQ_IMPORT = 1
        private const val REQ_EXPORT = 2
    }
}

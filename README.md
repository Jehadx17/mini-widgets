# Mini Widgets

A tiny Android app (no libraries, plain Kotlin) that gives you two home-screen widgets:

- **Search bar** (4×1): a Google-style pill, e.g. "✻ Ask Claude…"
- **Round button** (1×1): a circle with a glyph

Each placed widget uses a *design*. Designs are imported as JSON (file, "Open with", or paste from clipboard), tested by tapping, deleted by long-press, and can be exported.

## Design format

```json
{"widgets":[
  {"id":"ask-claude","type":"bar","label":"Ask Claude…","glyph":"✻",
   "url":"https://claude.ai/new","package":"com.anthropic.claude",
   "background":"#F2FFFFFF","text":"#5F6368","accent":"#D97757"}
]}
```

| field | meaning |
|---|---|
| `id` | unique name; importing the same id replaces it |
| `type` | `bar` or `icon` |
| `label` | text on the bar |
| `glyph` | symbol or emoji on the left (bar) or center (icon) |
| `url` | link to open, or `app:<package>` to just launch an app |
| `package` | optional: open the link inside this app when it can handle it |
| `background`, `text`, `accent` | `#RRGGBB` or `#AARRGGBB` |

A single object, a plain array, or `{"widgets":[...]}` are all accepted.

## Build

**GitHub (no computer needed):** push this folder to a GitHub repo. The *Build APK* action runs automatically; open the run under **Actions** and download `MiniWidgets-apk`.

**Android Studio:** open the folder, let it sync, then *Build → Build APK(s)*.

The APK is a debug build signed with a debug key, so Android will ask you to allow installs from your browser or file manager.

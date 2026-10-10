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

### Today card (`"type": "today"`)

A 4×2 card that fetches `url` every 30 minutes (or on ⟳) with `key` in the
`X-Api-Key` header, and shows the day's arrivals and departures. Same-day
turnovers are bold; tapping the card opens the response's `dashboard_url`.
Expected response (mithaqstay's `/api/v1/movements/day`):

```json
{"date":"2026-10-11","is_today":true,
 "checkins":[{"unit":"103","platform":"airbnb","time":"16:00","turnover":false}],
 "checkouts":[{"unit":"207","platform":"gathern","time":"12:00","turnover":false}],
 "dashboard_url":"https://mithaqstay.com/?day=2026-10-11"}
```

See `examples/mithaqstay-today.json`. Tap the design in the app to test the key.

## Build

**GitHub (no computer needed):** push this folder to a GitHub repo. The *Build APK* action runs automatically; open the run under **Actions** and download `MiniWidgets-apk`.

**Android Studio:** open the folder, let it sync, then *Build → Build APK(s)*.

The APK is a debug build signed with a debug key, so Android will ask you to allow installs from your browser or file manager.

# Custom Countdown for Pixel

A custom Android countdown-timer widget, built from scratch for a Pixel 7a.
Pick any date and time, give it a label, drop it on your home screen, and
it counts down live in `days:hrs:min` — with your own font, background, and
colors.

## Features

- **Any date, any year** — a normal Android date/time picker, no artificial
  range limits.
- **Custom label** per timer (e.g. "MAXIMUM EFFORT").
- **Home screen widget**, tap it any time to edit the timer.
- **`d:HH:mm` countdown** with `DAYS` / `HRS` / `MIN` captions lined up
  precisely under each number group.
- **Styling, all live-previewed before you save:**
  - 7 bundled fonts (Oswald, Anton, Orbitron, JetBrains Mono, Playfair
    Display, Caveat) plus the system default
  - 8 background presets — solid colors, gradients, or fully transparent —
    each with an adjustable opacity/fade slider
  - 11 text-color choices, or "Auto" to contrast against whatever
    background you picked
  - independent size sliders for the label and the countdown value
- **Expiry alert** — a notification fires the exact moment a timer hits
  zero, saying `<label> — Timer expired`.
- **Ongoing lock-screen notification** — see [Known limitation](#known-limitation-no-real-lock-screen-widget) below.

## Why a Bitmap, not a TextView

`RemoteViews` (what widgets are built from) cannot set a custom
`Typeface` on a `TextView` — it's stuck with the system font, permanently.
So the whole widget face — background card, label, number, unit captions —
is drawn by hand onto a `Canvas`/`Bitmap` in [`WidgetRenderer.kt`](app/src/main/java/com/skn/countdown/WidgetRenderer.kt),
and the widget just displays that bitmap in an `ImageView`. That's also
what makes custom fonts, gradients, and per-segment caption alignment
possible at all.

The renderer reads the widget's *actual current pixel size* from
`AppWidgetManager` (not a fixed guess), so the card fills the widget
exactly — no letterboxing — and repaints itself live if you drag-resize
the widget on your home screen.

Text sizing is solved directly rather than by trial-and-error shrinking:
for a fixed string and typeface, both `Paint.measureText` (width) and
`Paint.getTextBounds` (tight glyph height) scale *exactly* linearly with
`textSize`. Measuring once at a reference size gives an exact size for any
target width or height via a single ratio — no iteration, no overshoot.
Height uses `getTextBounds` rather than `Paint.getFontMetrics`, because
font metrics reserve room for the tallest/lowest glyph *anywhere in the
typeface* (accents, descenders like "g" or "y") — space a string of only
digits and a colon never uses.

## Known limitation: no real lock-screen widget

The original goal was a widget visible on the lock screen too. On this
Pixel 7a (Android 16), the lock screen's swipe-left panel is a fixed,
Google-curated surface (weather / Gemini / stocks widgets only) that is
still in beta and has no slot for third-party widgets. That's a platform
limitation, not something an app can opt into.

As the closest available substitute, the widget also posts a silent,
persistent notification that mirrors its current countdown text and
updates every minute — notifications do show on the lock screen by
default, so this is the "always visible without unlocking" behavior in
practice.

## Architecture

| Component | Role |
|---|---|
| `ConfigureActivity` | Date/time picker, label input, style controls, live preview |
| `CountdownWidgetProvider` | The `AppWidgetProvider`; renders and updates the widget |
| `WidgetRenderer` | Draws the widget face to a `Bitmap` |
| `WidgetPrefs` | Per-widget settings, keyed by the widget's own `AppWidgetManager` id |
| `AlarmScheduler` | One shared per-minute "tick" alarm (repaints every widget) + one exact per-widget "expiry" alarm |
| `TickReceiver` / `ExpiryReceiver` / `BootReceiver` | Alarm callbacks; `BootReceiver` re-arms everything after a reboot, since alarms don't survive one |
| `NotificationHelper` | The ongoing countdown notification + the one-shot expiry alert |
| `FontCatalog` / `BackgroundPreset` / `TextColorChoice` | Style option catalogs |

Each home screen widget instance already has its own unique id from
Android, so `WidgetPrefs` stores settings per id — one timer today, many
timers later, with no extra data model needed.

## Requirements

- JDK 17
- Android SDK: platform 36 & 37, build-tools 36.1/37, platform-tools
- Gradle 9.x (a wrapper isn't checked in; use a local Gradle install)

## Building

```bash
gradle assembleDebug
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## Installing

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then, on the phone: long-press the home screen → Widgets → **Countdown** →
drag it onto your home screen. It opens straight into the config screen.

## Permissions

- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` — for the exact-instant expiry
  alert and the per-minute tick.
- `POST_NOTIFICATIONS` — for the ongoing countdown notification and the
  expiry alert.
- `RECEIVE_BOOT_COMPLETED` — alarms don't survive a reboot, so this
  re-arms them.

None of this data leaves the device — there's no network permission at
all.

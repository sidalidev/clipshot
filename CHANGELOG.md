# Changelog

## 1.3.0

- About: our principles, the installed version and what's new, the date of the last security review, the signing certificate of the installed app (compare it with SECURITY.md), and links to the source code, the privacy policy and private security reporting.
- What leaves your device: nothing. The screen says so, and says what the optional Capture service cannot do.
- Erase everything: resets Clipshot to its first launch (settings, counters, the last three copies, gallery removal turned off). Type ERASE to confirm.
- Security: threat model and a dated internal review in `docs/`. CI now checks the permissions against a fixed list.

## 1.2.0

- Capture: a Quick Settings tile and the floating accessibility button take a screenshot in one tap, straight to the clipboard. It works through an accessibility service that cannot read your screen; it only presses the screenshot button. Optional, off by default, not in the Google Play version.
- Your phone's colors: Material You, from your wallpaper, with a matching palette on phones without it. Themed icon.
- No more white flash at launch, predictive back, springy buttons and haptic feedback.
- English only.

## 1.1.1

- Google Play build (same app, without the "remove from gallery" mode, which Play does not allow). Nothing changes for GitHub, Obtainium and F-Droid users.

## 1.1.0

- Remove from gallery (optional): the screenshot is deleted from your photos one minute after the copy.
- Quick Settings tile to pause and resume.
- Only screenshots saved by the system screenshot app are copied; downloads or synced files named "Screenshot…" are ignored.
- More reliable while the screenshot is still being written.
- Android 11 and 12 supported (storage permission).
- App data excluded from backups and device transfers. Reproducible builds.

## 1.0.0

- First release: every screenshot goes straight to the clipboard. No internet permission.

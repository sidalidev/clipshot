# Contributing to Clipshot

Thanks for helping. Clipshot does one thing; contributions that keep it small, private and reliable are the most welcome.

## Ground rules

- **No network, ever.** A pull request that adds the INTERNET permission, directly or through a dependency, will not be merged. CI fails if the merged manifest contains it.
- **No new dependency** without a strong reason. Today the app uses only AndroidX Compose and Activity.
- **One permission by default.** Anything more must be optional, asked at the moment it is needed, and explained in the UI.
- Keep it simple. If a feature needs a settings screen of its own, it probably belongs in another app.

## Getting started

1. JDK 21, Android SDK with platform 36.
2. `./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleDebug`
3. Test on a device or emulator: take a screenshot (`adb shell input keyevent 120`), then run `pastecheck` to check that the image pastes into another app.

## Pull requests

- One topic per pull request, with a short description of what changes for the user.
- Add or update unit tests in `app/src/test` for any change to `Rules.kt`.
- Strings go in `values/strings.xml` (English) and `values-fr/strings.xml`. New languages are welcome.
- Update `CHANGELOG.md` under "Unreleased".

## Reporting bugs

Open an issue with your phone model, Android version (and ROM, e.g. GrapheneOS), and what you expected. For a screenshot that was not copied, say which app took it: some OEMs save screenshots with their own app, and `Rules.SCREENSHOT_OWNERS` may need it.

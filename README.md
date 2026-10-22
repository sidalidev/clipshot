# Clipshot

Press the two buttons. Your screenshot is already on the clipboard.

Clipshot copies every new screenshot to the clipboard the moment Android saves it, so you can paste it straight into a chat, a mail or a note. Nothing else.

- **No internet permission.** Check the manifest: the app cannot open a single network connection. No account, no analytics, no ads.
- **No background service.** Android wakes Clipshot only when the photo library changes (a JobScheduler content trigger). No persistent notification, no battery cost while idle.
- **One permission:** read images, to see new screenshots. It only looks at images added in the last 30 seconds whose path or name contains "Screenshot".
- **Private copies.** The last three screenshots are copied into the app's private storage and shared read-only with the app you paste into.

Android 11+ (tested on Android 16). Built for GrapheneOS, works on any Android.

## Install and updates

Clipshot cannot update itself, by design: it has no internet access. Install it with [Obtainium](https://github.com/ImranR98/Obtainium) from this repository's releases and Obtainium keeps it up to date. Each APK is signed with the same key; its SHA-256 is published with every release.

## Build

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew :app:assembleRelease
```

Release signing reads `~/.clipshot/keystore.properties` (never committed). `pastecheck/` is a tiny test app that reads the clipboard like any messaging app would.

## License

Apache-2.0

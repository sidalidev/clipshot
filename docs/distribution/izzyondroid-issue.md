Title: Clipshot – every screenshot straight to the clipboard (dev.sidali.clipshot)

Hi! I'd like to request inclusion of my app Clipshot.

* Source: https://github.com/sidalidev/clipshot
* Releases with signed APK and SHA-256: https://github.com/sidalidev/clipshot/releases
* License: Apache-2.0
* Package: dev.sidali.clipshot, 1.6 MB, minSdk 30
* Signing certificate SHA-256: b2d10abfec2170c29d4c6f957bbb353608f9cf5591d8b3cd01b4694cc8d8c2d3
* Fastlane metadata (en-US, fr-FR) with screenshots: `fastlane/metadata/android/`

What it does: copies every new system screenshot to the clipboard. No INTERNET permission, no trackers, no proprietary libraries (AndroidX Compose only), no background service. One permission (read images); optional MANAGE_EXTERNAL_STORAGE only for the "remove from gallery" mode, requested when the user turns it on.

Builds are reproducible: a clean build of each tag gives the same APK as the release (checked on macOS and Ubuntu CI).

Thanks for running the repo!

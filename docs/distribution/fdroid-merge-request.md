New app: Clipshot (dev.sidali.clipshot)

Clipshot copies every new system screenshot to the clipboard, so it can be pasted straight into another app. No INTERNET permission, no background service (JobScheduler content trigger on MediaStore), one permission (read images). Optional "remove from gallery" mode uses MANAGE_EXTERNAL_STORAGE, requested only when the user turns it on.

* [x] The app complies with the inclusion criteria: Apache-2.0, no proprietary dependencies (AndroidX Compose and Activity only), no tracking, no network access.
* [x] The original app author has been notified (I am the author).
* [x] All related fdroiddata and RFP issues have been referenced: none.
* [x] Builds with `fdroid build` and all pipelines pass.
* [x] Reproducible build: the signed release APK matches a clean build of the tag, checked on macOS and in GitHub Actions on Ubuntu (identical unsigned APK SHA-256). AllowedAPKSigningKeys is set so F-Droid can ship the developer-signed APK.
* [x] Fastlane metadata (en-US, fr-FR) with screenshots is in the repository.

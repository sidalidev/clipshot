# IzzyOnDroid: App Inclusion Request (to paste at https://codeberg.org/IzzyOnDroid/repodata/issues/new, template "App Inclusion Request")

Title: Clipshot: every screenshot straight to the clipboard (dev.sidali.clipshot)

Guidelines: tick all four (I am the developer; complies with the policy; not already listed; Fastlane folder available).

Link to the source code: https://github.com/sidalidev/clipshot

Link to app in another app store: https://gitlab.com/fdroid/fdroiddata/-/merge_requests/53719 (F-Droid inclusion pending)

License used: Apache-2.0

Categories: System

Summary: Copies every new system screenshot to the clipboard, ready to paste. No internet permission.

Description:
Clipshot copies every new system screenshot to the clipboard the moment Android saves it: press the two buttons, switch app, paste. No INTERNET permission, no trackers, no proprietary libraries (AndroidX Compose only), no background service (JobScheduler content trigger on MediaStore). One permission (read images). Optional "remove from gallery" mode uses MANAGE_EXTERNAL_STORAGE, requested only when the user turns it on, and deletes only files owned by the system screenshot app. Quick Settings tile to pause. Optional Capture tile and accessibility button: an accessibility service that receives no events and cannot retrieve window content (canRetrieveWindowContent=false), whose only action is GLOBAL_ACTION_TAKE_SCREENSHOT. English.

Build instructions:
JDK 21 and the Android SDK (platform 36), then:
./gradlew :app:assembleFullRelease -Punsigned
The result is app/build/outputs/apk/full/release/app-full-release-unsigned.apk. Builds are reproducible: the release APKs attached to GitHub releases match a clean build of their tag apart from the signature (checked on macOS and Ubuntu CI).

APK: attached to each GitHub release as clipshot-<version>.apk, with a .sha256 file. Signing certificate SHA-256: b2d10abfec2170c29d4c6f957bbb353608f9cf5591d8b3cd01b4694cc8d8c2d3

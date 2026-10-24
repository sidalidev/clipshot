# Google Play: what to fill in (Clipshot, flavor `play`)

Build: `./gradlew :app:bundlePlayRelease` gives `app/build/outputs/bundle/playRelease/app-play-release.aab`, signed with the Clipshot key. Same package name as GitHub/F-Droid (`dev.sidali.clipshot`). Free, open source.

## 1. Create the app
Play Console > Create app: name **Clipshot**, default language **English (United States)**, **App**, **Free**. Accept the declarations.

## 2. App signing: keep the same key everywhere
So users can switch between Play, F-Droid and Obtainium without reinstalling, Play must sign with the Clipshot key, not a Google-generated one.
Test and release > App integrity > App signing > "Use a different key" > **Export and upload a key from Java keystore**. Download `pepk.jar` and the encryption public key it offers, then run (the passwords are in `~/.clipshot/keystore.properties`):

```bash
java -jar pepk.jar --keystore="$HOME/.clipshot/release.jks" --alias=clipshot --output=clipshot-key.zip --include-cert --rsa-aes-encryption --encryption-key-path=encryption_public_key.pem
```

Upload `clipshot-key.zip`. The upload key is the same key; nothing else to do.

## 3. Store listing (Grow > Store presence > Main store listing)
- Short description (80 max): `Every screenshot straight to the clipboard. No internet permission.`
- Full description: `fastlane/metadata/android/en-US/full_description.txt`. Play does not render `<b>`: remove the tags, and remove the Capture lines and the "About the Capture service" section (Capture is not in the play flavor).
- App icon 512×512: `fastlane/metadata/android/en-US/images/icon.png`
- Feature graphic 1024×500: `fastlane/metadata/android/en-US/images/featureGraphic.png`
- Phone screenshots: `fastlane/metadata/android/en-US/images/phoneScreenshots/1.png` to `3.png` (not `4.png`: it shows Capture, which the play flavor does not have). They show the full flavor's Capture row in the options card; retake them from the play build if Play asks.
- Category: **Tools**. Contact email: `contact@sidali.dev`. Website: `https://github.com/sidalidev/clipshot`

## 4. App content (Policy > App content)
- Privacy policy: `https://github.com/sidalidev/clipshot/blob/HEAD/PRIVACY.md`
- Ads: **No ads**.
- App access: **All functionality is available without special access** (no login).
- Content rating: category **Utility, productivity, communication or other**; answer **No** to every question. Expected: Everyone / PEGI 3.
- Target audience: **13 and over** (not designed for children).
- Data safety: **No** data collected, **No** data shared. Data encrypted in transit: not applicable (no network). Users can request deletion: uninstalling removes everything.
- News app: No. Government app: No. Financial features: None. Health: None.
- **Photo and video permissions declaration** (READ_MEDIA_IMAGES): core use. Text to paste:

> Clipshot's only feature is to copy every new screenshot to the clipboard automatically, right after the user takes it with the system buttons. It needs to see screenshots as soon as the system saves them, without the user picking anything, so the Android photo picker cannot provide this: picking the image would defeat the purpose of the app. Clipshot only looks at images added in the last 30 seconds that were saved by the system screenshot app, never reads any other image, and has no internet permission, so nothing ever leaves the device.

## 5. Release
- If your developer account is a **personal account created after 13 November 2023**, Play requires a closed test with at least 12 testers for 14 days before production. Otherwise go straight to Production.
- Upload `app-play-release.aab`, release name `1.2.0`. Release notes: the colors and polish lines of `fastlane/metadata/android/en-US/changelogs/4.txt`, without the Capture line.
- Countries: all.

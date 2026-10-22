# Security policy

## Reporting a vulnerability

Please do not open a public issue. Use GitHub's private vulnerability reporting ("Report a vulnerability" in the Security tab). You will get an answer within a week.

Include what an attacker can do, the steps to reproduce, and the version or commit.

## Scope

In scope: anything that lets another app or a remote party read Clipshot's copies or settings, make it copy or delete an image it should not, or make it use the network.

Out of scope: the clipboard itself. Android lets the focused app and the keyboard read the clipboard; that is inherent to copying anything.

## Design notes

- No INTERNET permission. CI checks the merged manifest.
- `ShotProvider` is not exported. It serves only names matching `shot-<digits>.<png|jpg|webp>` from the app's no-backup directory, read-only.
- Gallery removal (optional, needs All files access) deletes only rows owned by a system screenshot app, and re-checks the row just before deleting.
- Backups and device-to-device transfers of app data are disabled.

## Release signing

APKs are signed with a single key. Its certificate SHA-256 is:

`b2d10abfec2170c29d4c6f957bbb353608f9cf5591d8b3cd01b4694cc8d8c2d3`

Obtainium and Android refuse an update signed with any other key.

# Diplomat

Android app built with hexagonal architecture (Ports & Adapters), Kotlin, Jetpack Compose, and Room.

## Modules

| Module | Responsibility |
|--------|----------------|
| `:domain` | Entities (`WhitelistedContact`, `PhoneNumber`), outbound ports (`ContactRepositoryPort`, `SystemContactsPort`) |
| `:usecase` | Application interactors orchestrating domain logic |
| `:infrastructure` | Room adapters, Android Contacts adapter, `WhitelistViewModel` with `StateFlow` |
| `:presentation` | Jetpack Compose UI consuming ViewModel state |
| `:app` | Application entry point and dependency wiring |

## Features

- Contact whitelist management (add, edit, delete)
- Manual phone number entry or import from system contacts
- Room persistence behind the domain repository port

## Build

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## CI

| Workflow | Trigger | Output |
|----------|---------|--------|
| `build_apk.yml` | push to `main` | Debug APK artifact |
| `release.yml` | push to `main` | Semver git tag (`v{versionName}`) + GitHub Release with APK |
| `release.yml` | manual (**Actions → Release → Run workflow**) | Same, with user-supplied semver (and optional `versionCode`) |

Bump **both** `versionCode` and `versionName` in `app/build.gradle.kts` before merging to `main` to publish a new release on push. Or run **Release** manually and enter a semver (e.g. `1.3.0`); `versionCode` defaults to current + 1 for OTA. The release workflow skips if the tag already exists. In-app OTA requires a higher `versionCode` than the installed build.

### Shared signing (required for OTA)

CI must sign every shippable APK with the **same** keystore so updates install over the existing app (data and privileges kept).

1. Generate once (do not commit the `.jks`):

```bash
keytool -genkeypair -v -keystore diplomat-ci.jks -alias diplomat \
  -keyalg RSA -keysize 2048 -validity 10000
base64 < diplomat-ci.jks | tr -d '\n' > diplomat-ci.jks.b64
```

2. Add GitHub Actions secrets:

| Secret | Value |
|--------|--------|
| `DIPLOMAT_KEYSTORE_BASE64` | contents of `diplomat-ci.jks.b64` |
| `DIPLOMAT_KEYSTORE_PASSWORD` | keystore password |
| `DIPLOMAT_KEY_ALIAS` | key alias (e.g. `diplomat`) |
| `DIPLOMAT_KEY_PASSWORD` | key password |

3. After the first CI build signed with this keystore: **uninstall once** and install that APK. Later OTA updates keep data.

Local `./gradlew assembleDebug` still uses the machine debug keystore unless those `DIPLOMAT_*` env vars are set.

## In-app OTA

On the dashboard footer, paste a URL to a ZIP (Actions artifact) or APK (GitHub Release asset) and tap **Update**. The app downloads, extracts if needed, and opens the system installer.

## Engineering principles

This project follows Clean Code, idiomatic Kotlin, and hexagonal architecture. Agent skills: `AGENTS.md` and `.cursor/skills/` (ponytail / deslop / thermo-nuclear).

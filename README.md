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

Pushes to `main` trigger `.github/workflows/build_apk.yml`, which builds the debug APK and uploads it as a GitHub Actions artifact.

## Engineering principles

This project follows Clean Code, idiomatic Kotlin, and hexagonal architecture. Development guidance is captured in `AGENTS.md` (ponytail-inspired minimalism).

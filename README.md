# Diplomat

Native Android app (Kotlin + Jetpack Compose) that captures SMS and WhatsApp
notifications, analyzes their tone via an external API, and helps the user craft
measured, de-escalating replies (the "Grey Rock" method).

> This is a project skeleton: the structure, build configuration, core
> background service, data/network layers, and the main Compose screens are in
> place. The tone-analysis backend is reached through a placeholder Cloudflare
> Tunnel URL (`BuildConfig.ANALYSIS_BASE_URL`).

## Tech stack

- **Language / UI:** Kotlin, Jetpack Compose (Material 3)
- **Architecture:** MVVM + Repository pattern
- **Local storage:** Room
- **Networking:** Ktor client + kotlinx.serialization (JSON)
- **Async:** Kotlin Coroutines + Flow
- **Build:** Gradle (wrapper), AGP 8.13, Kotlin 2.3, KSP

## Requirements

- JDK 17+
- Android SDK with platform `android-36` and build-tools `36.0.0`
  (`compileSdk = 36`, `minSdk = 26`)
- No local Gradle install needed — use `./gradlew`

Create a `local.properties` with your SDK path (not committed):

```
sdk.dir=/path/to/Android/sdk
```

## Build & test

| Task | Command |
| --- | --- |
| Assemble debug APK | `./gradlew :app:assembleDebug` |
| Run JVM unit tests | `./gradlew :app:testDebugUnitTest` |
| Lint | `./gradlew :app:lintDebug` |

## Module layout (`app/src/main/java/com/diplomat`)

- `service/` — `DiplomatNotificationListenerService` (captures notifications),
  `NotificationRelayForegroundService` (keeps the process alive),
  `NotificationChannels`
- `data/local/` — Room entities, DAO, database, mappers
- `data/remote/` — Ktor client, `DiplomatApi`, request/response DTOs
- `data/repository/` — `MessageRepository` + default implementation
- `domain/model/` — `InterceptedMessage`, `ConversationThread`, `MessageStatus`, `MessageSource`
- `ui/dashboard/` — conversation list (Dashboard) + ViewModel
- `ui/decision/` — decision & drafting screen + ViewModel
- `ui/theme/`, `ui/navigation/` — Compose theme and navigation graph
- `util/` — permission helpers and outgoing-message intents
- `core/` — manual DI container and the contact whitelist

## Permissions

Diplomat needs **Notification access**
(`BIND_NOTIFICATION_LISTENER_SERVICE`), granted by the user in system settings
(the dashboard shows a banner + shortcut), plus an optional **battery
optimization** exemption so the listener is not killed in the background.

## Backend contract

`POST <ANALYSIS_BASE_URL>/analyze`

Request:

```json
{ "incoming_message": "…", "user_agreement": true, "user_reasoning": "…" }
```

Response:

```json
{ "tone_analysis": "…", "requires_response": true, "draft_response": "…" }
```

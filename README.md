# diplomat

A small Java command-line application built with Gradle.

## Requirements

- JDK 21 (the Gradle build enforces a Java 21 toolchain)
- No local Gradle install needed — use the bundled Gradle wrapper (`./gradlew`)

## Common commands

| Task | Command |
| --- | --- |
| Build everything | `./gradlew build` |
| Run the tests | `./gradlew test` |
| Run the application | `./gradlew run` |
| Run with arguments | `./gradlew run --args="France Spain"` |

## Project layout

- `app/` — the application subproject
  - `src/main/java/com/diplomat/App.java` — entry point
  - `src/test/java/com/diplomat/AppTest.java` — unit tests
- `gradle/libs.versions.toml` — dependency version catalog

# Projektüberblick

## Plugin
- **Name:** Backup (Paper-Plugin für Minecraft Java Edition)
- **Zweck:** laut Name ein Backup-Plugin; der genaue Funktionsumfang wird über Issues definiert.
- **Aktueller Feature-Stand (2026-10-03):** leeres Grundgerüst aus dem IntelliJ-Minecraft-Template.
  `Backup.java` enthält nur leere `onEnable`/`onDisable`. Keine `config.yml`, keine Commands, keine Listener.

## Umgebung (Stand 2026-10-03)
- Paper-API `26.2.build.+`, `api-version: '26.2'`, `load: STARTUP` in `plugin.yml`.
- Java-Toolchain 25. Gradle-Wrapper-Properties auf Gradle 9.4.0.
- Gradle-Plugin `xyz.jpenilla.run-paper` ist eingebunden – `runServer` darf laut Agentenregeln **nie** ausgeführt
  werden.
- **Bekannte Umgebungs-Lücken (2026-10-03):**
  - Es existieren **keine** Wrapper-Skripte (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) – nur die
    `gradle-wrapper.properties`.
  - Kein `gradle` im PATH der Agenten-Shell; auf dem PATH liegt nur JDK 11 (Toolchain verlangt 25, kein
    Foojay-Resolver in `settings.gradle.kts`).
  - GitHub-CLI `gh` ist nicht installiert; das Repo hat noch **kein** Git-Remote.
  → Vor dem ersten Build/Issue klären (siehe Regel 5 in `CLAUDE.md`). Diesen Abschnitt aktualisieren, sobald
    behoben.

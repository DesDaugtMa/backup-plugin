# Projektüberblick

## Plugin
- **Name:** Backup (Paper-Plugin für Minecraft Java Edition)
- **Zweck:** laut Name ein Backup-Plugin; der genaue Funktionsumfang wird über Issues definiert.
- **Aktueller Feature-Stand (2026-10-03):** leeres Grundgerüst aus dem IntelliJ-Minecraft-Template.
  `Backup.java` enthält nur leere `onEnable`/`onDisable`. Keine `config.yml`, keine Commands, keine Listener.

## Umgebung (Stand 2026-10-03)
- Paper-API `26.2.build.+`, `api-version: '26.2'`, `load: STARTUP` in `plugin.yml`.
- Java-Toolchain 25. Gradle-Wrapper-Properties auf Gradle 9.8.0. JDK 25 liegt unter
  `C:\Users\Alex\.jdks\ms-25.0.4.1` (von IntelliJ heruntergeladen).
- Fallstrick: `run-paper 3.1.0` verlangt mindestens Gradle 9.7 – bei Gradle 9.4 schlug die Konfiguration mit
  „No matching variant of xyz.jpenilla:run-task:3.1.0“ fehl (behoben 2026-10-03 durch Wrapper-Update auf 9.8.0).
  Beim Aktualisieren von Gradle-Plugins immer die Mindest-Gradle-Version prüfen.
- Gradle-Plugin `xyz.jpenilla.run-paper` ist eingebunden – `runServer` darf laut Agentenregeln **nie** ausgeführt
  werden.
- **Bekannte Umgebungs-Lücken (2026-10-03):**
  - Es existieren **keine** Wrapper-Skripte (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) – nur die
    `gradle-wrapper.properties`.
  - Gradle 9 braucht zum Starten selbst mindestens Java 17. Ist kein passendes `JAVA_HOME` gesetzt, den Build in
    Bash so aufrufen: `JAVA_HOME="/c/Users/Alex/.jdks/ms-25.0.4.1" ./gradlew build`.
  - Kein `gradle` im PATH der Agenten-Shell; auf dem PATH liegt nur JDK 11 (Toolchain verlangt 25, kein
    Foojay-Resolver in `settings.gradle.kts`).
  → Vor dem ersten Build klären (siehe Regel 5 in `CLAUDE.md`). Diesen Abschnitt aktualisieren, sobald
    behoben.

## GitHub (Stand 2026-10-03)
- Remote `origin`: `https://github.com/DesDaugtMa/backup-plugin` (öffentlich), Default-Branch `main`.
- GitHub-CLI `gh` ist installiert (`C:\Program Files\GitHub CLI\gh.exe`) und als `DesDaugtMa` per HTTPS
  angemeldet; Git nutzt den `gh`-Login. Issues können direkt mit `gh issue create` angelegt werden.
- Fallstrick: Findet die Agenten-Shell `gh` nicht (PATH aus einer Sitzung vor der Installation), in Bash
  `export PATH="$PATH:/c/Program Files/GitHub CLI"` voranstellen. Interaktive `gh`-Prompts funktionieren in
  Git Bash (MinTTY) nicht – immer alle Parameter als Flags übergeben.

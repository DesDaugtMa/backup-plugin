# Projektüberblick

## Plugin
- **Name:** Backup (Paper-Plugin für Minecraft Java Edition)
- **Zweck:** laut Name ein Backup-Plugin; der genaue Funktionsumfang wird über Issues definiert.
- **Aktueller Feature-Stand (2026-10-30):** Konfigurations-Grundlage aus Issue #1: `config.yml` + `messages.yml`
  (Validierung, Ergänzen mit `.bak`, Migrationsgerüst), `PluginLogger` mit Debug-Modus, `/backup` (Hilfe) und
  `/backup reload`. Noch keine Backup-Fachfunktion (nächstes Issue: #2).

## Umgebung (Stand 2026-10-03)
- Paper-API `26.2.build.+`, `api-version: '26.2'`, `load: STARTUP` in `plugin.yml`.
- Java-Toolchain 25. Gradle-Wrapper-Properties auf Gradle 9.8.0. JDK 25 liegt unter
  `C:\Users\Alex\.jdks\ms-25.0.4.1` (von IntelliJ heruntergeladen).
- Fallstrick: `run-paper 3.1.0` verlangt mindestens Gradle 9.7 – bei Gradle 9.4 schlug die Konfiguration mit
  „No matching variant of xyz.jpenilla:run-task:3.1.0“ fehl (behoben 2026-10-03 durch Wrapper-Update auf 9.8.0).
  Beim Aktualisieren von Gradle-Plugins immer die Mindest-Gradle-Version prüfen.
- Gradle-Plugin `xyz.jpenilla.run-paper` ist eingebunden – `runServer` darf laut Agentenregeln **nie** ausgeführt
  werden.
- **Build (Stand 2026-10-30):** Der Gradle-Wrapper 9.8.0 liegt im Repo (`gradlew`, `gradlew.bat`,
  `gradle-wrapper.jar`; Nutzerentscheidung 2026-10-30). Auf dem PATH liegt nur JDK 11, Gradle 9 braucht aber
  mindestens Java 17. Deshalb in Bash immer: `JAVA_HOME="/c/Users/Alex/.jdks/ms-25.0.4.1" ./gradlew build`. Die
  Toolchain 25 wird ohne Foojay-Resolver lokal gefunden. Python ist in der Agenten-Shell nicht verfügbar.

## GitHub (Stand 2026-10-03)
- Remote `origin`: `https://github.com/DesDaugtMa/backup-plugin` (öffentlich), Default-Branch `main`.
- GitHub-CLI `gh` ist installiert (`C:\Program Files\GitHub CLI\gh.exe`) und als `DesDaugtMa` per HTTPS
  angemeldet; Git nutzt den `gh`-Login. Issues können direkt mit `gh issue create` angelegt werden.
- Fallstrick: Findet die Agenten-Shell `gh` nicht (PATH aus einer Sitzung vor der Installation), in Bash
  `export PATH="$PATH:/c/Program Files/GitHub CLI"` voranstellen. Interaktive `gh`-Prompts funktionieren in
  Git Bash (MinTTY) nicht – immer alle Parameter als Flags übergeben.

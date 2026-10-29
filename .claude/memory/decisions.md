# Entscheidungen, Vorlieben & Konventionen

Format je Eintrag: **Thema** – Entscheidung. *Warum.* (Quelle, Datum)

## Festgelegt

- **Versionsschema** – `[Major].[Minor].[Patch]`; Major bleibt immer `1`; Minor für Features, Changes und
  Fixes (Patch wird auf 0 gesetzt); Patch für Bugfixes. Abgrenzung „Fix" vs. „Bugfix": siehe
  `.claude/skills/changelog/SKILL.md`. Einzige Versionsquelle: `gradle.properties`.
  (Nutzervorgabe, 2026-10-03)
- **Tests** – Keine Laufzeittests, keine Unit-Tests, kein `runServer`. Einzige Verifikation: fehlerfreier Build.
  (Nutzervorgabe, 2026-10-03)
- **Fragen** – Agenten fragen interaktiv (AskUserQuestion), so lange, bis alles klar ist. (Nutzervorgabe, 2026-10-03)
- **Changelog** – Nach jeder Codeänderung pflegt der Changelog-Agent Version + `CHANGELOG.md`; Einträge sind
  kurze Zusammenfassungen. (Nutzervorgabe, 2026-10-03)
- **Package-Name** – `me.DesDaugtMa.backup` bleibt trotz Großbuchstaben unverändert. *Umbenennung würde nur
  Churn erzeugen.* (Agenten-Setup, 2026-10-03)
- **Sprache Code-Kommentare/Javadoc** – Englisch. *Passt zu englischen Bezeichnern, öffentliches Repo.*
  (Nutzerentscheidung 2026-10-29, Issue #1)
- **Sprache `config.yml`-Kommentare und Spieler-Nachrichten** – Deutsch (keine Mehrsprachigkeit vorerst).
  (Nutzerentscheidung 2026-10-29, Issue #1)
- **Ablage der Nachrichten** – eigene `messages.yml`, MiniMessage, Präfix-Key `prefix` (Default
  `<dark_aqua>Backup »</dark_aqua> `). Leerer String = Nachricht deaktiviert. Ungültiges MiniMessage → eingebauter
  Default + WARN. (Nutzerentscheidung 2026-10-29, Issue #1)
- **Command-API** – Paper-Brigadier über Lifecycle-Manager. Haupt-Command `/backup`, **kein** Alias. `/backup`
  ohne Subcommand = Hilfe, gefiltert nach Permissions. (Nutzerentscheidung 2026-10-29, Issue #1)
- **Permission-Schema** – Präfix `backup.`; Admin-Aktionen als `backup.admin.<aktion>` (Default `op`) mit
  Elternknoten `backup.admin`. Unberechtigte bekommen die Plugin-Meldung `no-permission` statt eines versteckten
  Subcommands (Subcommand nur in Tab-Completion ausgeblendet). (Nutzerentscheidung 2026-10-29, Issue #1)
- **Logger-Basis** – `getComponentLogger()`. (Nutzerentscheidung 2026-10-29, Issue #1)
- **Config-Dateien – Grundsätze (gelten für alle künftigen Dateien/Schlüssel)** (Nutzerentscheidung 2026-10-29, Issue #1):
  - Jede Datei hat einen eigenen `config-version`-Key und wird unabhängig migriert.
  - Fehlende Schlüssel werden in die Datei ergänzt, vorher `<datei>.bak` schreiben; Datei nur anfassen, wenn
    nötig. Schlägt die `.bak` fehl → Original nicht anfassen, Default nur im Speicher.
  - YAML-Syntaxfehler beim Start → Defaults, Datei **nie** überschreiben, ERROR.
  - Reload ist alles-oder-nichts über alle Dateien; bei Fehler bleibt der bisher aktive Stand.
  - Unbekannte Schlüssel → WARN, bleiben in der Datei. Downgrade (höhere `config-version`) → laden, Datei nicht
    verändern, WARN.
  - Reload-Feedback im Chat: Erfolg + Anzahl Warnungen; Details nur in der Konsole. Reload-Log (INFO) nennt
    Auslöser, Dauer, Warnungen.
- **Threading Config-Laden** – Initiales Laden in `onEnable` darf synchron laufen (keine Spieler online);
  Reload liest/schreibt asynchron und übernimmt den Snapshot auf dem Main-Thread. (Nutzerentscheidung 2026-10-29, Issue #1)
- **GitHub-Labels** – GitHub-Standardlabels (`enhancement`, `bug`, `documentation` …), keine eigenen
  Bereichslabels. (Nutzerentscheidung 2026-10-29)

## Offen – beim ersten relevanten Anlass interaktiv klären und hierher verschieben

- [ ] Folia-Unterstützung ja/nein (in Issue #1 bewusst Out of Scope)
- [ ] Git-Workflow: Commit-Message-Konvention (z. B. Conventional Commits), Branch pro Issue ja/nein, Git-Tags pro Version ja/nein
- [ ] Startversion: Wird der erste Feature-Stand als `1.0.0` veröffentlicht oder direkt hochgezählt?

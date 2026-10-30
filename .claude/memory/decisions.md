# Entscheidungen, Vorlieben & Konventionen

Format je Eintrag: **Thema** – Entscheidung. *Warum.* (Quelle, Datum)

## Festgelegt

- **Versionsschema** – `[Major].[Minor].[Patch]`; Major bleibt immer `1`; Minor für Features, Changes und
  Fixes (Patch wird auf 0 gesetzt); Patch für Bugfixes. Abgrenzung „Fix" vs. „Bugfix": siehe
  `.claude/skills/changelog/SKILL.md`. Einzige Versionsquelle: `gradle.properties`.
  (Nutzervorgabe, 2026-10-03)
- **Startversion** – Der erste Funktionsstand (Issue #1) ist `1.0.0`; das leere Template hat keinen eigenen
  Eintrag. Ab dann wird nach Schema gezählt. (Nutzerentscheidung 2026-10-30)
- **Git-Workflow** – Commits direkt auf `main` (kein Branch pro Issue). Commit-Messages: kurzer deutscher Betreff im
  Freitext, optional Stichpunkt-Body, bei Issues am Ende `Closes #<nr>`. Nach jedem Versions-Commit ein Tag
  `v<Version>` (z. B. `v1.0.0`), mit dem Commit gepusht – nur mit Zustimmung zum Commit. (Nutzerentscheidung 2026-10-30)
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
  Elternknoten `backup.admin`. (Nutzerentscheidung 2026-10-29, Issue #1)
- **Subcommand-Rechte über Brigadier-`requires()`** – Subcommands sind per `requires(permission)` abgesichert:
  Unberechtigte sehen sie nicht in der Tab-Completion und bekommen beim Ausführen die Vanilla-Meldung „Unbekannter
  oder unvollständiger Befehl". Nur `/backup` ohne Subcommand (Hilfe) meldet `no-permission`, wenn der Auslöser
  keine einzige Backup-Permission hat. *Brigadier kann Literale nicht gleichzeitig verstecken und für Unberechtigte
  ausführbar halten; typisierte Argumente (z. B. `/backup <Sekunden>` aus #2) bleiben so sauber möglich.*
  Ersetzt die frühere Vorgabe „Plugin-Meldung statt verstecktem Subcommand". (Nutzerentscheidung 2026-10-30, Issue #1)
- **MiniMessage-Validierung** – Ungültig (→ eingebauter Default + WARN) sind unbekannte Tags (Tippfehler), Tags mit
  ungültigen Argumenten und Platzhalter, die es in der jeweiligen Nachricht nicht gibt. Nicht geschlossene Tags und
  `<reset>` sind erlaubt (kein Strict-Mode). (Nutzerentscheidung 2026-10-30, Issue #1)
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
- **Server leer während Backups** – Während ein Backup läuft, ist ausnahmslos niemand online (auch keine OPs,
  **keine** Bypass-Permission). Der Lockdown existiert **nur** als Teil eines Backup-Vorgangs, nie eigenständig
  (Nutzer hat einen separaten `/backup lockdown`-Command ausdrücklich abgelehnt). *Konsistente Vollbackups.*
  (Nutzerentscheidung 2026-10-29, Issue #2)
- **Server-Stop hat Vorrang vor Backups** – `onDisable` wartet nie auf ein laufendes Backup; ein Neustart
  unterbricht es. Kein Timeout für hängende Backups, kein persistierter Sperrzustand. (Nutzerentscheidung 2026-10-29, Issue #2)
- **Command-Syntax Backup** – `/backup <Sekunden>` (Pflichtzahl 0–3600) startet ein Backup mit Countdown;
  `/backup` ohne Argument bleibt die Hilfe. Permissions je Aktion getrennt (`backup.admin.create|cancel|status`).
  (Nutzerentscheidung 2026-10-29, Issue #2)
- **Ausnahme „leer = deaktiviert"** – Gilt nicht für Texte, die zwingend angezeigt werden müssen (Kick-Screen,
  Join-Fail-Screen): leer → eingebauter Standardtext + WARN. (Nutzerentscheidung 2026-10-29, Issue #2)
- **GitHub-Labels** – GitHub-Standardlabels (`enhancement`, `bug`, `documentation` …), keine eigenen
  Bereichslabels. (Nutzerentscheidung 2026-10-29)

## Offen – beim ersten relevanten Anlass interaktiv klären und hierher verschieben

- [ ] Folia-Unterstützung ja/nein (in Issue #1 bewusst Out of Scope)

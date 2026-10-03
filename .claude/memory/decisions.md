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

## Offen – beim ersten relevanten Anlass interaktiv klären und hierher verschieben

- [ ] Sprache der Code-Kommentare/Javadoc (Deutsch/Englisch)
- [ ] Sprache der Spieler-Nachrichten und der `config.yml`-Kommentare (Deutsch/Englisch/mehrsprachig)
- [ ] Ablage der Spieler-Nachrichten (`config.yml`, eigene `messages.yml`, fest im Code)
- [ ] Command-API (Paper-Brigadier über Lifecycle-Manager vs. klassisch über `plugin.yml`) und Haupt-Command-Name/Aliase
- [ ] Permission-Präfix (Vorschlag: `backup.`)
- [ ] Folia-Unterstützung ja/nein
- [ ] Logger-Basis (`getLogger()` vs. `getComponentLogger()`)
- [ ] Git-Workflow: Commit-Message-Konvention (z. B. Conventional Commits), Branch pro Issue ja/nein, Git-Tags pro Version ja/nein
- [ ] GitHub-Labels für Issues (welche Labels verwendet werden)
- [ ] Startversion: Wird der erste Feature-Stand als `1.0.0` veröffentlicht oder direkt hochgezählt?

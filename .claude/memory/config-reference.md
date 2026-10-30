# Config-Referenz (`config.yml`, `messages.yml`)

Vollständige Liste aller Schlüssel. Bei jeder Änderung an einer Config-Datei aktualisieren.

**Aktuelle `config-version`:** `config.yml` = 1, `messages.yml` = 1 (Stand 2026-10-30, Issue #1)

Grundsätze (gelten für alle Dateien): Defaults, bekannte Schlüssel und Kommentare kommen **ausschließlich** aus den
mitgelieferten Vorlagen in `src/main/resources/`. Ein Default in der Vorlage ändern = Default im Code ändern.

## `config.yml`

| Schlüssel | Typ | Default | Erlaubte Werte | Bedeutung | Seit Version |
|---|---|---|---|---|---|
| `config-version` | int | `1` | ≥ 1 | Interne Dateiversion; fehlt/ungültig → 1 + WARN; höher → Downgrade (laden, nicht schreiben) | 1.0.0 |
| `logging.debug` | boolean | `false` | `true`/`false` | Debug-Ausgaben `[DEBUG]` auf INFO; greift sofort per Reload | 1.0.0 |

## `messages.yml`

Alle Werte: String (MiniMessage), `""` = Nachricht deaktiviert, ungültig → eingebauter Default + WARN.

| Schlüssel | Platzhalter | Bedeutung | Seit Version |
|---|---|---|---|
| `config-version` | – | wie oben (int) | 1.0.0 |
| `prefix` | – | Präfix vor jeder Nachricht (nicht vor Hilfe-Zeilen `help.entry`); `""` = kein Präfix | 1.0.0 |
| `no-permission` | – | `/backup` ohne jede Backup-Permission | 1.0.0 |
| `help.header` | – | Überschrift der Hilfe (mit Präfix) | 1.0.0 |
| `help.entry` | `<command>`, `<description>` | Eine Zeile je erlaubtem Subcommand (ohne Präfix) | 1.0.0 |
| `help.reload-description` | – | Beschreibung `/backup reload`; wird als MiniMessage geparst und als Komponente eingesetzt | 1.0.0 |
| `reload.success` | – | Reload ohne Warnungen | 1.0.0 |
| `reload.success-with-warnings` | `<warnings>` | Reload mit Warnungen | 1.0.0 |
| `reload.failed` | `<file>` | Reload abgebrochen | 1.0.0 |
| `reload.already-running` | – | Paralleler Reload abgelehnt | 1.0.0 |

## Migrationshistorie

| Datei | von → nach | Plugin-Version | Was wurde migriert |
|---|---|---|---|
| – | – | – | noch keine (Gerüst: `ConfigFile.migrations`) |

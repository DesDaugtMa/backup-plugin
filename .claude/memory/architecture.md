# Architektur

Zielbild und Regeln: `.claude/shared/plugin-standards.md`. Hier steht der **tatsächliche** Ist-Stand (2026-10-30).

## Pakete & Klassen

| Klasse | Paket | Verantwortung |
|---|---|---|
| `Backup` | `me.DesDaugtMa.backup` | Main-Klasse, nur Verdrahtung. Felder `pluginLogger`/`reloadService` nullable (onDisable nach gescheitertem onEnable) |
| `PluginLogger` | `logging` | Kapselt `getComponentLogger()`, Debug als `[DEBUG]` auf INFO, Flag `volatile` |
| `ConfigManager` | `config` | **Einzige** Stelle, die YAML liest/schreibt: Anlegen aus Vorlage, Lesen (UTF-8), Migration, Ergänzen (+`.bak`, Temp-Datei + atomarer Move), unbekannte Schlüssel, Validierung, hält aktiven `ConfigSnapshot` (`AtomicReference`). `apply()` setzt auch `logging.debug` |
| `ConfigFile` | `config` | Beschreibt eine Datei (Name = Ressourcenname, unterstützte Version, Migrationskette; Lücken → Exception) |
| `ConfigMigration` | `config` | Ein Schritt n → n+1; setzt `config-version` **nicht** selbst, ergänzt keine neuen Keys (macht das Template-Ergänzen) |
| `ConfigSnapshot` | `config` | `PluginConfig` + `Messages` als Einheit → atomarer Tausch beider Dateien |
| `PluginConfig` | `config` | Snapshot `config.yml` |
| `LoadResult` | `config` | sealed: `Success(snapshot, warnings)` / `Failure(fileName)` |
| `MessageKey` | `message` | Enum aller Nachrichten-Pfade **inkl. erlaubter Platzhalter** (Teil der Validierung) |
| `Messages` | `message` | Snapshot `messages.yml` (Template-Strings, leer = deaktiviert) |
| `MiniMessageValidator` | `message` | Eigene Prüfung über einen `TagResolver`, der alle Tag-Namen „beansprucht" und unauflösbare protokolliert |
| `MessageService` | `message` | Rendern + Senden; Präfix via `Component.textOfChildren` (kein Style-Leak) |
| `ReloadService` | `service` | Asynchroner Reload, Guard, Worker, Shutdown |
| `BackupCommand` | `command` | Brigadier-Baum `/backup` + Hilfe-Liste (`HELP_ENTRIES`: Usage, Permission, Beschreibungs-Key) |

**Neue Config-Datei hinzufügen:** `ConfigFile`-Konstante + Ressource anlegen, in `ConfigManager` in `templates`,
`loadInitial`, `reload` und `buildSnapshot` aufnehmen, `ConfigSnapshot` erweitern.
**Neuen Subcommand hinzufügen:** Literal mit `requires(permission)` in `buildTree`, Eintrag in `HELP_ENTRIES`,
Beschreibungs-Key in `MessageKey` + `messages.yml`, Permission in `plugin.yml`.

## Commands

| Command | Subcommands | Permission | Beschreibung |
|---|---|---|---|
| `/backup` | – | keine (Hilfe nach Rechten gefiltert; ohne jede Backup-Permission → `no-permission`) | Hilfe |
| `/backup reload` | – | `backup.admin.reload` (über `requires()`) | Lädt `config.yml` + `messages.yml` neu |

## Permissions

| Permission | Default | Beschreibung |
|---|---|---|
| `backup.admin` | op | Elternknoten, enthält `backup.admin.reload` |
| `backup.admin.reload` | op | `/backup reload` |

## Listener

_keine_

## Geplante/wiederkehrende Tasks

_keine_

## Threading-Modell

- **Start:** Config wird in `onEnable` synchron geladen (Nutzerentscheidung).
- **Reload:** Main (Guard `AtomicBoolean`) → Executor `Backup-Reload-Worker` (Single-Thread, Daemon): beide Dateien
  lesen/migrieren/validieren, **erst wenn beide ok** Ergänzungen schreiben → `runTask` → Main: `apply`, INFO,
  Rückmeldung (nicht an offline Spieler), Guard frei.
- Vor `runTask` wird `isEnabled()` geprüft; `IllegalPluginAccessException` wird abgefangen (Ergebnis verworfen).
- `onDisable`: `ReloadService.shutdown()` wartet max. 5 s, dann `shutdownNow` + WARN.
- Der Reload-Worker ist bewusst **nicht** für Backups gedacht (#2 braucht einen eigenen Executor, der in `onDisable`
  nicht abgewartet wird – siehe decisions.md „Server-Stop hat Vorrang").

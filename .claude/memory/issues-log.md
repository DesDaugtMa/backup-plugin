# Issue-Log

Alle vom Issue-Writer verfassten Issues und ihr Umsetzungsstatus.

| Nr./Datei | Titel | Erstellt | Status | Umgesetzt in Version | Notiz |
|---|---|---|---|---|---|
| [#1](https://github.com/DesDaugtMa/backup-plugin/issues/1) | Konfigurations-Grundlage: config.yml, messages.yml, Debug-Logging und /backup reload | 2026-10-29 | umgesetzt | 1.0.0 | Umgesetzt 2026-10-30 (Abweichung: Subcommands per `requires()` versteckt, Unberechtigte bekommen die Vanilla-Meldung). Grundlage für alle Features (Prio hoch): ConfigManager, PluginLogger, messages.yml, erster Command. Folge-Issue vorgemerkt: `/backup debug <on\|off>` |
| [#2](https://github.com/DesDaugtMa/backup-plugin/issues/2) | Backup-Ablauf mit Countdown und Server-Sperre (Kick, Join-Sperre, MOTD) | 2026-10-29 | offen | – | Setzt #1 voraus (Prio hoch, direkt danach). `/backup <Sekunden>` → Countdown → Kick → Sperre → Backup-Schritt (nur Schnittstelle `BackupStep`, leer) → Freigabe. Folge-Issue nötig: eigentliche Backup-Erstellung |

Status-Werte: `offen`, `in Arbeit`, `umgesetzt`, `verworfen`.

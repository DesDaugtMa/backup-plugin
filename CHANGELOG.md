# Changelog

Alle Änderungen am Plugin **Backup**. Versionsschema: `1.[Minor].[Patch]` – Minor für Features und Änderungen,
Patch für Bugfixes.

## [1.0.0] – 2026-10-30
Konfigurations-Grundlage: einstellbare Dateien, Debug-Modus und Neuladen ohne Neustart.
- **Neu:** `config.yml` und `messages.yml` (deutsch kommentiert) werden beim ersten Start angelegt; fehlende Einträge werden automatisch ergänzt (vorher Sicherung als `.bak`), Fehlkonfigurationen führen nur zu Warnungen und Standardwerten (#1)
- **Neu:** Befehl `/backup` (Hilfe) und `/backup reload` lädt beide Dateien ohne Neustart neu (#1)
- **Neu:** Permissions `backup.admin` und `backup.admin.reload` (Standard: OP)
- **Config:** `logging.debug` (Debug-Ausgaben in der Konsole); alle Nachrichten inkl. `prefix` in `messages.yml` (MiniMessage, leerer Text = Nachricht aus)

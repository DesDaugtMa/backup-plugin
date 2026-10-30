# Bekannte Probleme, Fallstricke & behobene Bugs

## Behobene Bugs

Format: **Datum – Version – Kurztitel**
- Symptom: …
- Root-Cause: …
- Fix: …
- Lehre/Vorbeugung: … (was künftige Agenten beachten müssen)

_noch keine_

## Fallstricke (projektspezifisch)

- **Brigadier `requires()`** wirkt doppelt: Knoten wird nicht an den Client geschickt **und** beim Ausführen
  abgelehnt (Vanilla-Fehler). „Versteckt, aber ausführbar mit Plugin-Meldung" ist mit Literalen nicht machbar
  (`CommandNode` hat keine Remove-API; Literale haben Vorrang vor Argumenten). (2026-10-30, Issue #1)
- **MiniMessage ist nachsichtig:** Unbekannte Tags und Tags mit ungültigen Argumenten werden still als Text
  ausgegeben – auch im Strict-Mode (der prüft nur Schließen/`<reset>`). Deshalb `MiniMessageValidator`. Neue
  Platzhalter müssen in `MessageKey` deklariert werden, sonst gilt die Nachricht als ungültig. (2026-10-30)
- **`YamlConfiguration.getString()` konvertiert** Zahlen/Booleans zu Strings → Typprüfung immer mit
  `isString`/`isBoolean`/`isInt`. (2026-10-30)
- **Kommentar-Parsing** kann serverweit per `-DPaper.parseYamlCommentsByDefault=false` abgeschaltet werden → der
  `ConfigManager` setzt `parseComments(true)` explizit. (2026-10-30)
- Beim Zurückschreiben (nur bei Ergänzung/Migration) serialisiert Bukkit die Datei neu: Kommentare bleiben,
  Anführungszeichen/Formatierung können sich leicht ändern. (2026-10-30)

## Bekannte Einschränkungen (bewusst nicht gelöst)

- Debug-Ausgaben, die **während** des ersten Ladens in `onEnable` entstehen, erscheinen nicht (Logger startet mit
  Debug aus); die Ausgabe aller geladenen Werte nach der Übernahme erscheint. (2026-10-30, Issue #1)
- Ein Skalar an Stelle eines erwarteten Abschnitts (z. B. `logging: true`) wird nicht überschrieben: WARN, Defaults
  darunter nur im Speicher. (2026-10-30, Issue #1)
- Literaler Text, der wie ein Tag aussieht (z. B. `<3`), gilt als unbekannter Tag → muss mit `\<` escaped werden.

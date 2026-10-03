# Gemeinsames Agenten-Memory – Index

Dies ist das gemeinsame Langzeitgedächtnis aller Agenten (Implementierung, Issue-Writer, Bugfix, Changelog).
**Jeder Agent liest zu Beginn diese Datei und ALLE unten verlinkten Dateien vollständig.**

## Dateien

| Datei | Inhalt | Wer schreibt typischerweise |
|---|---|---|
| [project-overview.md](project-overview.md) | Was das Plugin ist/kann, aktueller Feature-Stand, Umgebung | alle |
| [architecture.md](architecture.md) | Pakete, Klassen & Verantwortlichkeiten, Commands, Permissions, Tasks, Listener | Implementierung, Bugfix |
| [decisions.md](decisions.md) | Grundsatzentscheidungen & Vorlieben des Nutzers, Konventionen, offene Punkte | alle |
| [config-reference.md](config-reference.md) | Vollständige Referenz aller `config.yml`-Schlüssel | Implementierung, Bugfix |
| [known-issues.md](known-issues.md) | Behobene Bugs (Root-Cause + Fix), Fallstricke, bekannte Einschränkungen | Bugfix, Implementierung |
| [issues-log.md](issues-log.md) | Geschriebene Issues mit Status und Kurzinhalt | Issue-Writer, Implementierung |

## Pflegeregeln (verbindlich)

1. **Aktualisieren statt duplizieren.** Vor dem Schreiben prüfen, ob es schon einen Eintrag zum Thema gibt, und
   diesen aktualisieren.
2. **Veraltetes korrigieren.** Stimmt ein Eintrag nicht mehr mit dem Code überein, wird er korrigiert oder
   gelöscht – das Memory muss den **aktuellen** Stand beschreiben.
3. **Absolute Datumsangaben** (`YYYY-MM-DD`), nie „heute"/„letzte Woche".
4. **Nur Nicht-Offensichtliches.** Keine Code-Kopien, keine Inhalte, die man in 5 Sekunden im Code sieht. Das
   *Warum* hinter Entscheidungen ist wertvoller als das *Was*.
5. **Quelle angeben**, wenn sinnvoll: z. B. „(Nutzerentscheidung 2026-10-03, Issue #4)".
6. **Keine Geheimnisse** (Passwörter, Tokens, private URLs).
7. **Knapp und strukturiert.** Stichpunkte, Tabellen, kurze Absätze.
8. Neue Themen, die in keine Datei passen, bekommen eine neue Datei, die hier im Index verlinkt wird.

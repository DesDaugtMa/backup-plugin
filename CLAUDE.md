# Backup – Minecraft-Java-Plugin (Paper)

Dieses Repository enthält das Minecraft-Server-Plugin **Backup**. Es wird ausschließlich agentisch über die
unten beschriebenen Skills weiterentwickelt. Diese Datei gilt für **jede** Arbeit in diesem Repository –
egal, ob ein Skill aktiv ist oder nicht.

## Projekt-Eckdaten

| Thema | Wert | Quelle |
|---|---|---|
| Plattform | Paper (Bukkit-kompatibel) | `build.gradle.kts` |
| Paper-API | `26.2.build.+` (compileOnly) | `build.gradle.kts` |
| `api-version` | `26.2` | `src/main/resources/plugin.yml` |
| Java | 25 (Gradle-Toolchain) | `build.gradle.kts` |
| Build-System | Gradle (Kotlin DSL), Wrapper 9.4.0 | `gradle/wrapper/gradle-wrapper.properties` |
| Basis-Package | `me.DesDaugtMa.backup` | `src/main/java/...` |
| Main-Klasse | `me.DesDaugtMa.backup.Backup` | `plugin.yml` |
| Plugin-Version | **einzige Quelle:** `version=` in `gradle.properties` (wird per `processResources` in `plugin.yml` expandiert) | `gradle.properties` |
| Changelog | `CHANGELOG.md` im Repo-Root | – |
| Gemeinsames Agenten-Memory | `.claude/memory/` | – |
| Coding-Standards | `.claude/shared/plugin-standards.md` | – |

> Die Werte oben können veralten. Maßgeblich ist immer der tatsächliche Inhalt der genannten Dateien.

## Die Agenten (Skills)

| Skill | Aufruf | Zweck |
|---|---|---|
| Implementierungs-Agent | `/implement <Issue-Nr. \| Pfad \| Beschreibung>` | Setzt ein Issue nach Best Practices im Plugin um. |
| Issue-Writer-Agent | `/issue-writer <grobe Idee>` | Interviewt den Nutzer, analysiert Edge-Cases und schreibt ein ausführliches GitHub-Issue. |
| Bugfix-Agent | `/bugfix <Stacktrace \| Fehlerbeschreibung>` | Analysiert Exceptions/Fehler, findet die Root-Cause und behebt sie behutsam. |
| Changelog-Agent | `/changelog <Zusammenfassung der Änderung>` | Setzt die Version korrekt und pflegt `CHANGELOG.md`. Wird nach **jeder** Codeänderung aufgerufen. |

Die Skill-Definitionen liegen unter `.claude/skills/<name>/SKILL.md`.

---

## Verbindliche Regeln für ALLE Agenten

Diese Regeln sind nicht verhandelbar. Jeder Skill baut auf ihnen auf und darf sie nur verschärfen, nie lockern.

### Regel 1 – Immer vollständigen Kontext herstellen, bevor gehandelt wird

Bevor ein Agent eine Frage stellt, einen Plan macht oder Code ändert, verschafft er sich ein vollständiges Bild:

1. **Diese Datei** (`CLAUDE.md`) lesen.
2. **Das gesamte gemeinsame Memory** lesen: zuerst `.claude/memory/MEMORY.md`, danach **jede** dort verlinkte Datei.
3. **`.claude/shared/plugin-standards.md`** lesen (Implementierungs- und Bugfix-Agent zwingend, die anderen bei Bedarf).
4. **Build- und Plugin-Konfiguration** lesen: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`,
   `src/main/resources/plugin.yml`, `src/main/resources/config.yml` (falls vorhanden) und alle weiteren Dateien
   unter `src/main/resources/`.
5. **Den gesamten Java-Quellcode** unter `src/main/java/` erfassen. Solange das Projekt überschaubar ist
   (Richtwert: < 60 Klassen), wird **jede** Klasse gelesen. Ist es größer, wird zuerst die komplette
   Paketstruktur aufgelistet, dann werden alle Klassen gelesen, die mit der Aufgabe direkt oder indirekt
   zusammenhängen (Aufrufer, Aufgerufene, gemeinsam genutzte Services, Config-Zugriffe, Listener, Commands).
6. **`CHANGELOG.md`** lesen (falls vorhanden), um die Entwicklungsgeschichte zu kennen.
7. **Git-Zustand** prüfen: `git status`, `git log --oneline -20`, ggf. `git diff`. Uncommittete fremde Änderungen
   werden **nie** verworfen oder überschrieben – bei Unklarheit fragen.

Ein Agent trifft **keine Annahmen** über Code, den er nicht gelesen hat.

### Regel 2 – Gemeinsames Memory lesen UND pflegen

Alle Agenten teilen sich das Memory unter `.claude/memory/`. Es ist das Langzeitgedächtnis des Projekts und
wird mit eingecheckt.

- **Lesen:** zu Beginn jeder Aufgabe vollständig (siehe Regel 1).
- **Schreiben:** am Ende jeder Aufgabe – und zwar alles, was ein künftiger Agent wissen muss und was **nicht**
  ohnehin offensichtlich aus dem Code hervorgeht: Grundsatzentscheidungen des Nutzers, Vorlieben, Architektur-
  änderungen, neue Config-Schlüssel, Fallstricke, Root-Causes behobener Bugs, offene Punkte.
- Die Pflegeregeln stehen in `.claude/memory/MEMORY.md` und sind einzuhalten (aktualisieren statt duplizieren,
  absolute Datumsangaben, veraltete Einträge korrigieren oder entfernen, keine Geheimnisse).
- Das Memory dieses Repositories ist **die** gemeinsame Wissensbasis der Agenten. Es hat Vorrang vor
  eventuellen privaten/benutzerweiten Notizen, wenn es um Projektwissen geht.

### Regel 3 – Fragen werden IMMER interaktiv gestellt – so lange, bis alles klar ist

- Fragen werden **ausschließlich über das Tool `AskUserQuestion`** gestellt – nicht als Fließtext-Liste am Ende
  einer Antwort.
- Es wird **so lange in weiteren Runden gefragt, bis die Umsetzung exakt den Wünschen und Vorstellungen des
  Nutzers entspricht.** Lieber eine Runde mehr als eine falsche Annahme.
- Pro `AskUserQuestion`-Aufruf maximal 4 Fragen; thematisch zusammengehörige Fragen bündeln, danach die nächste
  Runde stellen. Antworten werden vor der nächsten Runde ausgewertet – neue Unklarheiten führen zu neuen Fragen.
- Jede Frage bietet konkrete, sinnvolle Antwortoptionen mit kurzer Erklärung der Konsequenzen. Hat der Agent eine
  fachlich begründete Empfehlung, steht sie als erste Option mit dem Zusatz „(Empfohlen)".
- Der Nutzer kann immer „Other" wählen und frei antworten – freie Antworten werden ernst genommen und bei Bedarf
  mit Rückfragen präzisiert.
- **Nicht gefragt** wird nach Dingen, die sich eindeutig aus Code, Memory oder früheren Antworten ergeben.
  Bereits im Memory dokumentierte Entscheidungen werden nicht erneut zur Diskussion gestellt (es sei denn, die
  aktuelle Aufgabe widerspricht ihnen – dann wird der Konflikt explizit benannt und gefragt).
- Vor größeren Umsetzungen wird der **Plan** zur Freigabe vorgelegt (siehe jeweiliger Skill).

### Regel 4 – NIEMALS Tests zur Laufzeit

- Es wird **kein** Server gestartet (`runServer`, `runPaper`, manuelles Starten eines Paper-Jars o. Ä. sind verboten).
- Es werden **keine** Unit-, Integrations- oder sonstigen Tests geschrieben oder ausgeführt.
- Es wird **keine** Laufzeit-Verifikation simuliert (keine Mock-Server, keine Test-Harnesses).
- Die **einzige** Verifikation ist: **Das Plugin baut fehlerfrei** (siehe Regel 5).
- Korrektheit wird stattdessen durch sorgfältiges Lesen, sauberes Design und gedankliches Durchspielen der
  Abläufe (inkl. Edge-Cases) sichergestellt.

### Regel 5 – Build-Verifikation

Nach jeder Codeänderung (und nach jeder Versionsänderung) wird gebaut:

1. Existiert ein Gradle-Wrapper, wird er verwendet:
   - Bash: `./gradlew build`
   - PowerShell: `.\gradlew.bat build`
2. Existiert **kein** Wrapper (`gradlew`/`gradlew.bat` fehlen), wird geprüft, ob `gradle` im `PATH` ist, und dann
   `gradle build` verwendet.
3. Ist beides nicht möglich, oder scheitert der Build an der Umgebung (z. B. fehlendes JDK 25 für die Toolchain),
   wird **nicht** improvisiert. Der Agent erklärt das Problem präzise und fragt den Nutzer interaktiv, wie
   verfahren werden soll (z. B. Wrapper über IntelliJ/`gradle wrapper` erzeugen, JDK installieren,
   Foojay-Toolchain-Resolver ergänzen). Umgebungsänderungen erfolgen nur mit Zustimmung.
4. Erfolgskriterium: `BUILD SUCCESSFUL` **und** keine neuen Compiler-Fehler. Neue Compiler-Warnungen
   (insbesondere Deprecations), die durch die eigene Änderung entstanden sind, werden behoben.
5. Schlägt der Build wegen des eigenen Codes fehl, wird die Ursache analysiert und behoben – dann erneut gebaut,
   bis der Build grün ist. Fehler werden nie „weggeschaltet" (kein Deaktivieren von Tasks, kein
   `-x compileJava`, keine `@SuppressWarnings` als Abkürzung).

### Regel 6 – Nach JEDER Codeänderung: Changelog-Agent

Sobald ein Agent Code, Ressourcen (`config.yml`, `plugin.yml`, …) oder das Build-Skript geändert hat und der
Build grün ist, ruft er als **letzten** Arbeitsschritt den Changelog-Agenten auf
(Skill-Tool, Skill `changelog`) und übergibt ihm eine strukturierte Zusammenfassung:

```
Art: Feature | Change | Fix | Bugfix
Issue: #<nr> oder "keins"
Zusammenfassung: <1–3 Sätze, was sich für Serverbetreiber/Spieler ändert>
Details:
- <Änderung 1>
- <Änderung 2>
Config-Änderungen: <neue/geänderte/entfernte Schlüssel oder "keine">
Permissions/Commands: <neu/geändert oder "keine">
```

Reine Änderungen an Agenten-Dateien (`CLAUDE.md`, `.claude/**`) oder am Memory lösen **keinen**
Changelog-Eintrag aus.

### Regel 7 – Git

- Es wird **nicht** ohne ausdrückliche Zustimmung committet, gepusht, gebrancht, getaggt oder ein Issue
  geschlossen. Am Ende einer Aufgabe fragt der Agent interaktiv, ob committet werden soll, und schlägt eine
  Commit-Message vor (Konvention: siehe `.claude/memory/decisions.md`).
- Destruktive Git-Befehle (`reset --hard`, `checkout -- .`, `clean -fd`, Force-Push) sind tabu, außer der Nutzer
  verlangt sie ausdrücklich.

### Regel 8 – Sprache

- Kommunikation mit dem Nutzer: **Deutsch**.
- Bezeichner im Code (Klassen, Methoden, Variablen, Config-Schlüssel, Permissions): **Englisch**.
- Sprache von Code-Kommentaren/Javadoc, Spieler-Nachrichten und Kommentaren in `config.yml`: siehe
  `.claude/memory/decisions.md` – ist dort noch nichts festgelegt, wird beim ersten relevanten Anlass gefragt
  und die Antwort dort dokumentiert.

### Regel 9 – Abschlussbericht

Jeder Agent beendet seine Arbeit mit einem kurzen Bericht auf Deutsch:
was getan wurde, welche Dateien betroffen sind, Build-Ergebnis, neue Version (falls vergeben), was ins Memory
geschrieben wurde und welche Punkte offen sind.

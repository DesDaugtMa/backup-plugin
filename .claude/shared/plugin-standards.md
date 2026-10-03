# Plugin-Standards (Architektur, Konfiguration, Logging, Code-Qualität)

Verbindliche Standards für den **Implementierungs-Agenten** und den **Bugfix-Agenten**. Der Issue-Writer nutzt
dieses Dokument, um realistische technische Hinweise ins Issue zu schreiben.

Wenn eine Entscheidung im Projekt bewusst von diesem Dokument abweicht, ist sie in
`.claude/memory/decisions.md` dokumentiert – dort Dokumentiertes hat Vorrang.

---

## 1. Grundprinzipien

1. **Klare Verantwortlichkeiten (Single Responsibility).** Jede Klasse hat genau eine Aufgabe. Die Main-Klasse
   ist nur Bootstrap/Verdrahtung, keine Fachlogik.
2. **Abhängigkeiten werden injiziert, nicht global geholt.** Services bekommen ihre Abhängigkeiten über den
   Konstruktor. Kein statischer `getInstance()`-Zugriff auf die Plugin-Klasse aus Fachlogik heraus.
3. **Fachlogik ist von der Bukkit-Schicht getrennt.** Commands und Listener sind dünn: Sie validieren Eingaben,
   delegieren an Services und geben Rückmeldung. Die eigentliche Logik steckt in Services.
4. **Nichts Hardcodiertes, was ein Serverbetreiber ändern wollen könnte** → `config.yml` (siehe Abschnitt 4).
5. **Nachvollziehbarkeit** → konsequentes Logging mit abschaltbarem Debug-Modus (siehe Abschnitt 5).
6. **Den Main-Thread nie blockieren.** Datei-I/O, Kompression, Netzwerk und lange Berechnungen laufen asynchron
   (siehe Abschnitt 6). Für ein Backup-Plugin ist das besonders kritisch.
7. **Robust gegen Fehlbedienung und Fehlkonfiguration.** Ungültige Werte führen zu einer klaren Warnung und
   einem sicheren Fallback – nicht zu einem Absturz beim Enable.
8. **Minimal-invasiv.** Bestehender Code wird nur angefasst, wenn es für die Aufgabe nötig ist. Gelegenheits-
   Refactorings werden nicht nebenbei gemacht, sondern dem Nutzer als Vorschlag genannt.
9. **Konsistenz vor persönlichem Geschmack.** Neuer Code sieht aus wie bestehender Code (Benennung, Struktur,
   Kommentardichte, Idiome).

---

## 2. Paketstruktur (Zielbild)

Basis-Package: `me.DesDaugtMa.backup` (die Großschreibung im Package ist historisch und wird **nicht** geändert).

```
me.DesDaugtMa.backup
├── Backup.java                 # JavaPlugin: onEnable/onDisable, Verdrahtung, sonst nichts
├── config/                     # Laden, Validieren, Migrieren der Konfiguration; typisierte Config-Objekte
│   ├── ConfigManager.java
│   └── PluginConfig.java       # unveränderlicher, typisierter Snapshot (z. B. Java record)
├── logging/
│   └── PluginLogger.java       # zentraler Logger inkl. Debug-Modus
├── command/                    # Command-Registrierung, (Sub-)Commands, Tab-Completion
├── listener/                   # Event-Listener (dünn, delegieren an Services)
├── service/                    # Fachlogik (z. B. BackupService, RetentionService, …)
├── task/                       # geplante/wiederkehrende Tasks (Scheduler-Anbindung)
├── model/                      # Datenklassen/Records/Enums der Domäne
├── message/                    # Spieler-/Konsolen-Nachrichten (MiniMessage-Templates)
└── util/                       # kleine, zustandslose Helfer (sparsam einsetzen!)
```

Regeln:
- Pakete werden erst angelegt, wenn sie gebraucht werden – keine leeren Pakete auf Vorrat.
- `util` ist kein Mülleimer: Gehört eine Methode fachlich zu einem Service, kommt sie dorthin.
- Klassen sind standardmäßig `final`, Felder `private final`, wo immer möglich.
- Ein neues Paket oder eine neue Schicht wird in `.claude/memory/architecture.md` dokumentiert.

### Lebenszyklus in der Main-Klasse

```java
@Override
public void onEnable() {
    // 1. Konfiguration laden (saveDefaultConfig, validieren, migrieren)
    // 2. Logger mit Debug-Flag initialisieren
    // 3. Services erzeugen (Konstruktor-Injektion)
    // 4. Listener, Commands, Tasks registrieren
    // 5. Eine INFO-Zeile: Plugin aktiviert (Version, Debug-Status, Dauer)
}

@Override
public void onDisable() {
    // 1. Tasks stoppen, laufende Operationen sauber beenden oder abbrechen (mit Log)
    // 2. Ressourcen freigeben (Executor shutdown, Dateien schließen)
    // 3. Eine INFO-Zeile: Plugin deaktiviert
}
```

- `onDisable` muss auch funktionieren, wenn `onEnable` mittendrin gescheitert ist (Null-Checks auf Services).
- Schlägt ein **kritischer** Schritt im `onEnable` fehl (z. B. nicht behebbare Config), wird der Fehler mit
  Ursache geloggt und das Plugin kontrolliert deaktiviert (`getServer().getPluginManager().disablePlugin(this)`),
  statt halb initialisiert weiterzulaufen.

---

## 3. Paper-API-Nutzung

- Gebaut wird gegen die Paper-API in der Version aus `build.gradle.kts`. Vor Nutzung einer API wird geprüft,
  dass sie in **dieser** Version existiert und nicht `@Deprecated` ist. Im Zweifel: Javadoc der Paper-API
  (`https://jd.papermc.io/paper/`) für die passende Version konsultieren.
- **Text:** Adventure `Component`s und **MiniMessage** statt Legacy-Farbcodes (`§`, `ChatColor`).
- **Commands:** Bevorzugt die moderne Paper-Command-API (Brigadier über den Lifecycle-Manager), sofern in
  `decisions.md` nicht anders festgelegt. Jeder Command hat Permission-Prüfung, Tab-Completion, saubere
  Usage-Meldungen und unterscheidet Spieler/Konsole, wo relevant.
- **Permissions:** Schema `backup.<bereich>[.<aktion>]` (z. B. `backup.create`, `backup.admin.reload`), sofern
  `decisions.md` nichts anderes sagt. Jede Permission wird mit Default (op/true/false) und Beschreibung
  deklariert und in `architecture.md` gelistet.
- **Kein NMS / keine CraftBukkit-Interna / keine Reflection auf Server-Interna**, außer der Nutzer verlangt es
  ausdrücklich.
- **Keine neuen Abhängigkeiten** ohne Rückfrage. Werden welche nötig, wird mit dem Nutzer geklärt, ob sie
  geshadet (inkl. Relocation) oder über `libraries:` in der `plugin.yml` geladen werden.
- `plugin.yml` wird gepflegt: `version` bleibt **immer** `'${version}'` (wird aus `gradle.properties` befüllt).

---

## 4. Konfiguration (`config.yml`)

### Was gehört in die Config?

Alles, was ein Serverbetreiber sinnvollerweise anpassen möchte: Intervalle, Pfade, Limits, Aufbewahrungsregeln,
Feature-Schalter, Formate, Nachrichten (falls in `decisions.md` so entschieden), Debug-Logging.
**Nicht** in die Config: interne Konstanten ohne Nutzen für Betreiber, Werte, deren Änderung das Plugin
zuverlässig kaputt macht.

### Aufbau

```yaml
# ------------------------------------------------------------
#  Backup – Konfiguration
# ------------------------------------------------------------

# Interne Version der Konfigurationsdatei. NICHT manuell ändern.
config-version: 1

logging:
  # Aktiviert ausführliche Debug-Ausgaben in der Konsole.
  # Nur zur Fehlersuche einschalten – erzeugt deutlich mehr Log-Ausgaben.
  debug: false

# weitere Bereiche thematisch gruppiert, z. B.:
# backup:
#   ...
```

Regeln:
- **Jeder** Schlüssel hat einen Kommentar: Bedeutung, erlaubte Werte/Einheit, Default.
- Schlüssel in `kebab-case`, thematisch verschachtelt.
- Einheiten im Schlüssel oder Kommentar eindeutig machen (`interval-minutes`, nicht `interval`).
- **Zentraler Zugriff:** Kein Streuen von `getConfig().getString("...")` im Code. Der `ConfigManager` liest die
  Datei einmal, validiert und erzeugt ein typisiertes, unveränderliches `PluginConfig`-Objekt. Services
  bekommen dieses Objekt (bzw. den Teil, den sie brauchen).
- **Validierung beim Laden:** Ungültige/fehlende Werte → WARN mit Schlüssel, ungültigem Wert und verwendetem
  Fallback. Kein Crash.
- **Migration:** Wird die Struktur geändert, wird `config-version` erhöht und eine Migration implementiert, die
  bestehende Nutzerwerte übernimmt. Neue Schlüssel werden ergänzt, **ohne** vorhandene Nutzerwerte zu
  überschreiben. Jede Migration wird geloggt (INFO).
- **Reload:** Falls ein Reload-Command existiert, ersetzt er den Config-Snapshot atomar und informiert die
  betroffenen Services. Was ohne Neustart nicht übernommen werden kann, wird beim Reload als WARN gemeldet.
- **Dokumentation:** Jeder neue/geänderte/entfernte Schlüssel wird in `.claude/memory/config-reference.md`
  eingetragen und dem Changelog-Agenten gemeldet.

---

## 5. Logging

Ausreichendes, gut dosiertes Logging ist Pflicht. Ein Serverbetreiber muss anhand der Logs nachvollziehen
können, was das Plugin tut und warum etwas schiefging – ohne den Code zu kennen.

### Zentraler Logger

Es gibt **eine** zentrale Logging-Klasse (`logging/PluginLogger`), die alle Klassen injiziert bekommen. Sie
kapselt den Plugin-Logger (`getLogger()` bzw. Paper `getComponentLogger()`, je nach Entscheidung in
`decisions.md`) und bietet mindestens:

```java
void info(String message);
void warn(String message);
void warn(String message, Throwable throwable);
void error(String message, Throwable throwable);
void debug(String message);
void debug(Supplier<String> messageSupplier);   // teure Nachrichten nur bauen, wenn Debug aktiv
boolean isDebugEnabled();
void setDebugEnabled(boolean enabled);          // wird aus der Config (logging.debug) gesetzt, auch bei Reload
```

**Debug-Ausgaben** werden – wenn `logging.debug: true` – auf INFO-Level mit dem Präfix `[DEBUG]` ausgegeben,
weil die Server-Konsole FINE/DEBUG-Level standardmäßig herausfiltert. Ist Debug aus, wird nichts ausgegeben und
(bei Supplier-Variante) nichts berechnet.

### Was wird auf welchem Level geloggt?

| Level | Wann | Beispiele |
|---|---|---|
| **ERROR/SEVERE** | Eine Operation ist fehlgeschlagen und konnte nicht abgeschlossen werden. Immer mit Exception. | Backup fehlgeschlagen, Datei nicht schreibbar, unerwartete Exception im Task |
| **WARN** | Etwas ist unerwartet, das Plugin arbeitet aber mit Fallback weiter. | Ungültiger Config-Wert → Default, Zielordner fehlt → wird angelegt, Operation übersprungen, weil bereits eine läuft |
| **INFO** | Wichtige Ereignisse im normalen Betrieb – sparsam! | Plugin aktiviert/deaktiviert (inkl. Version), Start/Ende großer Operationen inkl. Dauer und Ergebnis, Config neu geladen, Config migriert |
| **DEBUG** | Alles, was bei der Fehlersuche hilft. | Geladene Config-Werte, Entscheidungen („überspringe Welt X, weil …"), Zwischenschritte, Parameter, Zeitmessungen, Thread-Wechsel, Anzahl verarbeiteter Dateien |

Regeln:
- Log-Nachrichten sind **konkret**: Was ist passiert, welches Objekt (Welt, Datei, Spieler, Schlüssel), welcher
  Wert, was ist die Folge. Schlecht: `"Error!"`. Gut: `"Backup der Welt 'world_nether' fehlgeschlagen: Zielordner
  '/backups' ist nicht beschreibbar. Backup wird übersprungen."`
- Exceptions werden **nie verschluckt**. Jeder `catch` loggt (mindestens DEBUG mit Begründung, warum das okay
  ist) oder wirft weiter. Leere `catch`-Blöcke sind verboten.
- Kein Logging in Schleifen pro Tick/pro Datei auf INFO. Pro-Element-Logging nur auf DEBUG und bei sehr großen
  Mengen zusammengefasst.
- Keine Geheimnisse (Passwörter, Tokens, Webhook-URLs) in Logs – auch nicht im Debug-Modus.
- Langlaufende Operationen loggen Start und Ende mit Dauer (`System.nanoTime()` bzw. `Duration`).
- Asynchrone Tasks fangen am äußersten Rand **alle** Exceptions und loggen sie als ERROR – sonst verschwinden sie
  stillschweigend.

---

## 6. Threading & Scheduler

- **Bukkit-API-Zugriffe** (Welten, Entities, Spieler, Blöcke, Inventare) nur auf dem Main-Thread (bzw. dem
  korrekten Region-Thread, falls Folia unterstützt wird – siehe `decisions.md`).
- **Blockierende Arbeit** (Datei-I/O, Kompression, Netzwerk, große Berechnungen) **nie** auf dem Main-Thread.
- Muster: Daten, die von der Bukkit-API benötigt werden, auf dem Main-Thread einsammeln → schwere Arbeit asynchron
  → Ergebnis/Rückmeldung wieder auf dem Main-Thread an Spieler senden.
- Eigene `ExecutorService`s bekommen benannte Threads (z. B. `Backup-Worker-1`) und werden in `onDisable`
  heruntergefahren (mit Timeout und Log, falls Tasks noch laufen).
- Geteilter Zustand zwischen Threads: unveränderliche Objekte bevorzugen; sonst `java.util.concurrent`
  (Atomic*, ConcurrentHashMap) statt händischem `synchronized`-Wildwuchs.
- Parallele Ausführung derselben Operation verhindern, wo sie keinen Sinn ergibt (z. B. zwei Backups
  gleichzeitig) – mit klarer Rückmeldung/WARN.
- Keine Tasks registrieren, während das Plugin deaktiviert wird/ist.
- Welt-Daten konsistent sichern: Vor dem Kopieren von Weltdateien den Speicherzustand berücksichtigen (z. B.
  Welten speichern und Auto-Save während des Kopierens pausieren/wieder aktivieren) – im Fehlerfall den
  ursprünglichen Zustand **immer** wiederherstellen (`finally`).

---

## 7. Fehlerbehandlung

- Erwartbare Fehler (Datei fehlt, ungültige Eingabe, fehlende Permission) werden behandelt und dem Nutzer
  verständlich gemeldet – keine Stacktraces im Chat.
- Unerwartete Fehler werden mit vollem Stacktrace als ERROR geloggt; der auslösende Spieler bekommt eine knappe,
  verständliche Fehlermeldung.
- Ressourcen mit `try-with-resources` schließen.
- Zustände, die vor einer Operation verändert werden (z. B. Auto-Save aus), werden im `finally` zurückgesetzt.
- Eingaben von außen (Command-Argumente, Config-Werte, Dateinamen) werden validiert. Pfade werden gegen Path
  Traversal geschützt (normalisieren, prüfen, dass sie im erlaubten Basisordner liegen).
- `null` vermeiden: `Optional` für „kann fehlen"-Rückgaben; Parameter-Validierung mit `Objects.requireNonNull`
  an öffentlichen Service-Grenzen.

---

## 8. Code-Stil

- Modernes Java gemäß Toolchain (Java 25): `record`s für Datenträger, `switch`-Ausdrücke, `var` nur wo der Typ
  offensichtlich ist, Pattern Matching, `java.nio.file` statt `java.io.File`, `java.time` für Zeit.
- Sprechende Namen, keine Abkürzungen außer etablierten (`id`, `uuid`, `io`).
- Methoden kurz und fokussiert. Keine Magic Numbers – benannte Konstanten oder Config-Werte.
- Javadoc für öffentliche Klassen und nicht-triviale öffentliche Methoden; Kommentare erklären das **Warum**,
  nicht das Was. Sprache der Kommentare: siehe `decisions.md`.
- Kein toter Code, keine auskommentierten Blöcke, keine `System.out.println`.
- Keine `TODO`s ohne Erklärung; offene Punkte gehören ins Memory (`decisions.md` → offene Punkte) und in den
  Abschlussbericht.

---

## 9. Spieler-Nachrichten

- Nachrichten werden zentral verwaltet (Paket `message`), nicht als String-Literale in Commands verstreut.
- Format: MiniMessage. Platzhalter über `TagResolver`/`Placeholder` – niemals Nutzereingaben ungefiltert als
  MiniMessage parsen (Injection), sondern als `Placeholder.unparsed(...)`.
- Ob Nachrichten in `config.yml`, einer eigenen `messages.yml` oder im Code liegen und in welcher Sprache: siehe
  `decisions.md`.
- Einheitliches Präfix für Plugin-Nachrichten (konfigurierbar, falls Nachrichten konfigurierbar sind).

---

## 10. Checkliste vor dem Build

- [ ] Jede neue Klasse liegt im richtigen Paket und hat genau eine Verantwortung.
- [ ] Keine Fachlogik in Main-Klasse, Commands oder Listenern.
- [ ] Neue einstellbare Werte liegen in der `config.yml` (kommentiert, validiert, mit Default, dokumentiert).
- [ ] Logging: Start/Ende/Fehler auf passenden Levels, Debug-Ausgaben für Entscheidungen, keine verschluckten
      Exceptions, keine Geheimnisse.
- [ ] Kein blockierendes I/O auf dem Main-Thread; keine Bukkit-API-Aufrufe aus Async-Threads.
- [ ] Ressourcen werden geschlossen, Zustände in `finally` zurückgesetzt, Executor in `onDisable` gestoppt.
- [ ] Commands: Permission, Tab-Completion, Usage, Konsole/Spieler berücksichtigt; in `plugin.yml`/Registrierung
      und Memory eingetragen.
- [ ] Keine Deprecated-API, kein NMS, keine neuen Abhängigkeiten ohne Zustimmung.
- [ ] Alle im Issue genannten Akzeptanzkriterien und Edge-Cases sind abgedeckt.

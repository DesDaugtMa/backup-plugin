package me.DesDaugtMa.backup.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import me.DesDaugtMa.backup.logging.PluginLogger;
import me.DesDaugtMa.backup.message.MessageKey;
import me.DesDaugtMa.backup.message.Messages;
import me.DesDaugtMa.backup.message.MiniMessageValidator;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.Nullable;

/**
 * Creates, reads, migrates, validates and completes {@code config.yml} and {@code messages.yml} and holds the
 * active {@link ConfigSnapshot}. No other class reads YAML directly.
 *
 * <p>The bundled templates are the single source of defaults, known keys and comments. Loading never throws for
 * user errors: invalid values fall back to the template value and are counted as warnings.</p>
 *
 * <p>Threading: {@link #loadInitial()} runs on the main thread during enable, {@link #reload()} on the reload
 * worker. Both only do file I/O and build a new snapshot; {@link #apply(ConfigSnapshot)} swaps it on the main
 * thread.</p>
 */
public final class ConfigManager {

    public static final ConfigFile CONFIG_FILE = new ConfigFile("config.yml", 1, List.of());
    public static final ConfigFile MESSAGES_FILE = new ConfigFile("messages.yml", 1, List.of());

    private static final String VERSION_KEY = "config-version";
    private static final String DEBUG_KEY = "logging.debug";
    private static final int ASSUMED_VERSION = 1;
    private static final String BACKUP_SUFFIX = ".bak";
    private static final String TEMP_SUFFIX = ".tmp";
    private static final int DEBUG_VALUE_MAX_LENGTH = 80;

    private final Path dataFolder;
    private final PluginLogger logger;
    private final MiniMessageValidator validator;
    private final Map<ConfigFile, Template> templates;
    private final AtomicReference<ConfigSnapshot> active = new AtomicReference<>();

    /**
     * @param dataFolder     the plugin data folder; only files inside it are read or written
     * @param resourceLoader opens a bundled resource by name, returns {@code null} if it does not exist
     * @throws IllegalStateException if a bundled template is missing or broken (packaging error)
     */
    public ConfigManager(Path dataFolder, Function<String, @Nullable InputStream> resourceLoader,
                         PluginLogger logger, MiniMessageValidator validator) {
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.templates = Map.of(
                CONFIG_FILE, Template.load(CONFIG_FILE, resourceLoader),
                MESSAGES_FILE, Template.load(MESSAGES_FILE, resourceLoader));
    }

    /**
     * Loads both files during enable. A file that cannot be created, read or migrated is replaced by the built-in
     * defaults for this run and left untouched on disk.
     */
    public ConfigSnapshot loadInitial() {
        Warnings warnings = new Warnings(logger);
        FileState config = readOrDefaults(CONFIG_FILE, warnings);
        FileState messages = readOrDefaults(MESSAGES_FILE, warnings);
        writePending(config, warnings);
        writePending(messages, warnings);
        return buildSnapshot(config, messages, warnings);
    }

    /**
     * Reloads both files, all or nothing: if one file cannot be read or migrated, nothing is written and a
     * {@link LoadResult.Failure} is returned. The active snapshot is not changed here.
     */
    public LoadResult reload() {
        Warnings warnings = new Warnings(logger);
        Optional<FileState> config = readForReload(CONFIG_FILE, warnings);
        if (config.isEmpty()) {
            return new LoadResult.Failure(CONFIG_FILE.fileName());
        }
        Optional<FileState> messages = readForReload(MESSAGES_FILE, warnings);
        if (messages.isEmpty()) {
            return new LoadResult.Failure(MESSAGES_FILE.fileName());
        }
        // Only write once both files are known to be readable, so a failed reload leaves the disk untouched.
        writePending(config.get(), warnings);
        writePending(messages.get(), warnings);
        ConfigSnapshot snapshot = buildSnapshot(config.get(), messages.get(), warnings);
        return new LoadResult.Success(snapshot, warnings.count());
    }

    /**
     * Makes the snapshot active and applies {@code logging.debug}. Main thread only.
     */
    public void apply(ConfigSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        ConfigSnapshot previous = active.getAndSet(snapshot);
        boolean debug = snapshot.config().debug();
        if (previous != null && previous.config().debug() != debug) {
            logger.info("Debug-Modus " + (debug ? "aktiviert" : "deaktiviert") + ".");
        }
        logger.setDebugEnabled(debug);
        logValues(snapshot);
    }

    /** The active snapshot. Only valid after the first {@link #apply(ConfigSnapshot)}. */
    public ConfigSnapshot current() {
        ConfigSnapshot snapshot = active.get();
        if (snapshot == null) {
            throw new IllegalStateException("Configuration has not been loaded yet");
        }
        return snapshot;
    }

    // ------------------------------------------------------------------ reading

    private FileState readOrDefaults(ConfigFile file, Warnings warnings) {
        Path path = pathOf(file);
        if (!ensureExists(file, path)) {
            logger.error("Für " + file.fileName() + " werden die eingebauten Standardwerte verwendet.");
            return FileState.defaults(file, templateOf(file));
        }
        try {
            return read(file, path, warnings);
        } catch (IOException | InvalidConfigurationException exception) {
            logger.error(file.fileName() + " konnte nicht gelesen werden: " + exception.getMessage()
                    + " – Standardwerte aktiv, Datei wurde nicht verändert.");
        } catch (RuntimeException exception) {
            logger.error("Migration/Verarbeitung von " + file.fileName() + " fehlgeschlagen – Standardwerte aktiv, Datei wurde"
                    + " nicht verändert.", exception);
        }
        return FileState.defaults(file, templateOf(file));
    }

    private Optional<FileState> readForReload(ConfigFile file, Warnings warnings) {
        Path path = pathOf(file);
        if (!ensureExists(file, path)) {
            logger.error("Neuladen abgebrochen, weil " + file.fileName()
                    + " nicht angelegt werden konnte – bisherige Einstellungen bleiben aktiv.");
            return Optional.empty();
        }
        try {
            return Optional.of(read(file, path, warnings));
        } catch (IOException | InvalidConfigurationException exception) {
            logger.error(file.fileName() + " konnte nicht gelesen werden: " + exception.getMessage()
                    + " – bisherige Einstellungen bleiben aktiv.");
        } catch (RuntimeException exception) {
            logger.error("Migration/Verarbeitung von " + file.fileName() + " fehlgeschlagen – bisherige Einstellungen bleiben"
                    + " aktiv.", exception);
        }
        return Optional.empty();
    }

    /** Creates the file from the bundled template if it does not exist. Returns false if that failed. */
    private boolean ensureExists(ConfigFile file, Path path) {
        if (Files.exists(path)) {
            return true;
        }
        try {
            Files.createDirectories(dataFolder);
            Files.write(path, templateOf(file).bytes());
            logger.info("Standard-Datei " + file.fileName() + " angelegt.");
            return true;
        } catch (IOException exception) {
            logger.error("Standard-Datei konnte nicht angelegt werden: " + path, exception);
            return false;
        }
    }

    /**
     * Reads, migrates and completes one file in memory. Throws for syntax/IO errors and for failing migrations;
     * every other problem becomes a warning.
     */
    private FileState read(ConfigFile file, Path path, Warnings warnings)
            throws IOException, InvalidConfigurationException {
        long start = System.nanoTime();
        YamlConfiguration yaml = newYaml();
        yaml.loadFromString(Files.readString(path, StandardCharsets.UTF_8));
        Template template = templateOf(file);

        int version = readVersion(file, yaml, warnings);
        logger.debug(() -> file.fileName() + ": gefundene " + VERSION_KEY + " = " + version);

        if (version > file.currentVersion()) {
            warnings.warn(file.fileName() + " stammt von einer neueren Plugin-Version (" + VERSION_KEY + " "
                    + version + ", unterstützt: " + file.currentVersion() + "). Bekannte Schlüssel werden geladen,"
                    + " die Datei wird nicht verändert.");
            logger.debug(() -> file.fileName() + ": Downgrade – Migration und Ergänzen übersprungen.");
            reportUnknownKeys(file, yaml, template, warnings);
            return new FileState(file, yaml, path, List.of(), Optional.empty());
        }

        Optional<Integer> migratedFrom = migrate(file, yaml, version);
        List<String> addedKeys = complete(file, yaml, template, warnings);
        reportUnknownKeys(file, yaml, template, warnings);
        logger.debug(() -> file.fileName() + ": gelesen in " + elapsedMillis(start) + " ms");
        return new FileState(file, yaml, path, addedKeys, migratedFrom);
    }

    private int readVersion(ConfigFile file, YamlConfiguration yaml, Warnings warnings) {
        Object raw = yaml.get(VERSION_KEY);
        if (yaml.isInt(VERSION_KEY) && yaml.getInt(VERSION_KEY) >= ASSUMED_VERSION) {
            return yaml.getInt(VERSION_KEY);
        }
        String found = raw == null ? "fehlt" : "ungültiger Wert '" + raw + "'";
        warnings.warn(file.fileName() + ": " + VERSION_KEY + " " + found + " – Version " + ASSUMED_VERSION
                + " wird angenommen.");
        return ASSUMED_VERSION;
    }

    /** Runs the migration chain in memory. Returns the start version if at least one step ran. */
    private Optional<Integer> migrate(ConfigFile file, YamlConfiguration yaml, int version) {
        if (version == file.currentVersion()) {
            logger.debug(() -> file.fileName() + ": keine Migration nötig.");
            return Optional.empty();
        }
        for (int step = version; step < file.currentVersion(); step++) {
            int from = step;
            logger.debug(() -> file.fileName() + ": Migrationsschritt " + from + " → " + (from + 1));
            file.migrationFrom(step).migrate(yaml);
        }
        yaml.set(VERSION_KEY, file.currentVersion());
        return Optional.of(version);
    }

    /**
     * Adds every template key missing from the user file, including its comments. Existing user values are never
     * replaced – if a scalar sits where the template expects a section, the keys below it are not added.
     */
    private List<String> complete(ConfigFile file, YamlConfiguration yaml, Template template, Warnings warnings) {
        List<String> added = new ArrayList<>();
        List<String> blockedParents = new ArrayList<>();
        for (String path : template.yaml().getKeys(true)) {
            if (yaml.contains(path, true)) {
                continue;
            }
            Optional<String> blockingParent = findScalarParent(yaml, path);
            if (blockingParent.isPresent()) {
                String parent = blockingParent.get();
                if (!blockedParents.contains(parent)) {
                    blockedParents.add(parent);
                    warnings.warn(file.fileName() + ": ungültiger Wert bei '" + parent + "' ('" + yaml.get(parent)
                            + "', erwartet: Abschnitt) – Standardwerte darunter gelten nur im Speicher.");
                }
                continue;
            }
            if (template.yaml().isConfigurationSection(path)) {
                yaml.createSection(path);
            } else {
                yaml.set(path, template.yaml().get(path));
                added.add(path);
            }
            yaml.setComments(path, template.yaml().getComments(path));
            yaml.setInlineComments(path, template.yaml().getInlineComments(path));
        }
        if (added.isEmpty()) {
            logger.debug(() -> file.fileName() + ": keine fehlenden Schlüssel.");
        }
        return List.copyOf(added);
    }

    private static Optional<String> findScalarParent(YamlConfiguration yaml, String path) {
        int separator = path.indexOf('.');
        while (separator >= 0) {
            String parent = path.substring(0, separator);
            if (yaml.contains(parent, true) && !yaml.isConfigurationSection(parent)) {
                return Optional.of(parent);
            }
            separator = path.indexOf('.', separator + 1);
        }
        return Optional.empty();
    }

    /** Warns once per unknown key; children of an unknown section are not reported separately. */
    private void reportUnknownKeys(ConfigFile file, YamlConfiguration yaml, Template template, Warnings warnings) {
        List<String> reported = new ArrayList<>();
        for (String path : yaml.getKeys(true)) {
            if (template.yaml().contains(path, true) || reported.stream().anyMatch(r -> path.startsWith(r + "."))) {
                continue;
            }
            reported.add(path);
            warnings.warn(file.fileName() + ": unbekannter Schlüssel '" + path + "' wird ignoriert.");
        }
    }

    // ------------------------------------------------------------------ writing

    /**
     * Writes completed keys/migrations back. A {@code .bak} copy is written first; if that fails, the original is
     * left alone and the defaults only exist in memory. The file is replaced via a temporary file so that a
     * failing write never leaves a half-written original behind.
     */
    private void writePending(FileState state, Warnings warnings) {
        if (state.path() == null) {
            return;
        }
        if (state.addedKeys().isEmpty() && state.migratedFrom().isEmpty()) {
            logger.debug(() -> state.file().fileName() + ": Datei wird nicht neu geschrieben, weil nichts fehlt.");
            return;
        }
        String fileName = state.file().fileName();
        Path path = state.path();
        Path backup = path.resolveSibling(fileName + BACKUP_SUFFIX);
        try {
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            warnings.warn("Sicherung " + backup + " konnte nicht geschrieben werden (" + exception
                    + ") – " + fileName + " bleibt unverändert, Defaults gelten nur im Speicher.");
            return;
        }
        Path temp = path.resolveSibling(fileName + TEMP_SUFFIX);
        try {
            Files.writeString(temp, state.yaml().saveToString(), StandardCharsets.UTF_8);
            moveReplacing(temp, path);
        } catch (IOException exception) {
            warnings.warn(path + " konnte nicht geschrieben werden (" + exception
                    + ") – Datei bleibt unverändert, Defaults gelten nur im Speicher.");
            deleteQuietly(temp);
            return;
        }
        state.migratedFrom().ifPresent(from -> logger.info(fileName + " von Version " + from + " auf "
                + state.file().currentVersion() + " migriert (Sicherung: " + backup.getFileName() + ")."));
        if (!state.addedKeys().isEmpty()) {
            logger.info("Fehlende Schlüssel in " + fileName + " ergänzt: " + String.join(", ", state.addedKeys())
                    + " (Sicherung: " + backup.getFileName() + ").");
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            logger.debug(() -> "Temporäre Datei " + path + " konnte nicht gelöscht werden: " + exception);
        }
    }

    // ------------------------------------------------------------------ validation

    private ConfigSnapshot buildSnapshot(FileState config, FileState messages, Warnings warnings) {
        return new ConfigSnapshot(buildPluginConfig(config, warnings), buildMessages(messages, warnings));
    }

    private PluginConfig buildPluginConfig(FileState state, Warnings warnings) {
        YamlConfiguration defaults = templateOf(CONFIG_FILE).yaml();
        boolean fallback = defaults.getBoolean(DEBUG_KEY);
        boolean debug = fallback;
        if (state.yaml().isBoolean(DEBUG_KEY)) {
            debug = state.yaml().getBoolean(DEBUG_KEY);
        } else if (state.yaml().contains(DEBUG_KEY, true)) {
            warnings.warn(CONFIG_FILE.fileName() + ": ungültiger Wert bei '" + DEBUG_KEY + "': '"
                    + state.yaml().get(DEBUG_KEY) + "' (erlaubt: true/false) – verwende " + fallback + ".");
        }
        return new PluginConfig(debug);
    }

    private Messages buildMessages(FileState state, Warnings warnings) {
        YamlConfiguration defaults = templateOf(MESSAGES_FILE).yaml();
        Map<MessageKey, String> templates = new EnumMap<>(MessageKey.class);
        for (MessageKey key : MessageKey.values()) {
            templates.put(key, readMessage(state.yaml(), defaults, key, warnings));
        }
        return new Messages(templates);
    }

    private String readMessage(YamlConfiguration yaml, YamlConfiguration defaults, MessageKey key,
                               Warnings warnings) {
        String fallback = Objects.requireNonNull(defaults.getString(key.path()), key.path());
        String fileName = MESSAGES_FILE.fileName();
        if (!yaml.isString(key.path())) {
            if (yaml.contains(key.path(), true)) {
                warnings.warn(fileName + ": ungültiger Wert bei '" + key.path() + "': '" + yaml.get(key.path())
                        + "' (erwartet: Text) – eingebauter Standardtext wird verwendet.");
            }
            return fallback;
        }
        String value = Objects.requireNonNull(yaml.getString(key.path()));
        if (value.isEmpty()) {
            logger.debug(() -> fileName + ": Nachricht '" + key.path() + "' ist leer und deaktiviert.");
            return value;
        }
        Optional<String> problem = validator.findProblem(value, key.placeholders());
        if (problem.isPresent()) {
            warnings.warn(fileName + ": ungültiges MiniMessage bei '" + key.path() + "' (" + problem.get()
                    + ") – eingebauter Standardtext wird verwendet.");
            return fallback;
        }
        return value;
    }

    private void logValues(ConfigSnapshot snapshot) {
        logger.debug(() -> CONFIG_FILE.fileName() + ": " + DEBUG_KEY + " = " + snapshot.config().debug());
        for (MessageKey key : MessageKey.values()) {
            logger.debug(() -> MESSAGES_FILE.fileName() + ": " + key.path() + " = '"
                    + abbreviate(snapshot.messages().template(key)) + "'");
        }
    }

    // ------------------------------------------------------------------ helpers

    private Path pathOf(ConfigFile file) {
        return dataFolder.resolve(file.fileName());
    }

    private Template templateOf(ConfigFile file) {
        return templates.get(file);
    }

    private static String abbreviate(String value) {
        return value.length() <= DEBUG_VALUE_MAX_LENGTH ? value : value.substring(0, DEBUG_VALUE_MAX_LENGTH) + "…";
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    /** Comment parsing can be disabled server-wide by a system property; completing keys relies on it. */
    private static YamlConfiguration newYaml() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().parseComments(true);
        return yaml;
    }

    /**
     * One file in memory.
     *
     * @param path         the file on disk, or {@code null} if built-in defaults are used and nothing may be written
     * @param addedKeys    keys added from the template that still need to be written
     * @param migratedFrom original version if migrations ran and the file needs to be written
     */
    private record FileState(ConfigFile file, YamlConfiguration yaml, @Nullable Path path, List<String> addedKeys,
                             Optional<Integer> migratedFrom) {

        static FileState defaults(ConfigFile file, Template template) {
            return new FileState(file, template.copy(), null, List.of(), Optional.empty());
        }
    }

    /** A bundled template: raw bytes for creating the file plus the parsed defaults. */
    private record Template(byte[] bytes, YamlConfiguration yaml) {

        static Template load(ConfigFile file, Function<String, @Nullable InputStream> resourceLoader) {
            try (InputStream in = resourceLoader.apply(file.fileName())) {
                if (in == null) {
                    throw new IllegalStateException("Bundled template " + file.fileName() + " is missing");
                }
                byte[] bytes = in.readAllBytes();
                YamlConfiguration yaml = newYaml();
                yaml.loadFromString(new String(bytes, StandardCharsets.UTF_8));
                return new Template(bytes, yaml);
            } catch (IOException | InvalidConfigurationException exception) {
                throw new IllegalStateException("Bundled template " + file.fileName() + " is unreadable", exception);
            }
        }

        YamlConfiguration copy() {
            YamlConfiguration copy = newYaml();
            try {
                copy.loadFromString(yaml.saveToString());
            } catch (InvalidConfigurationException exception) {
                throw new IllegalStateException("Bundled template could not be copied", exception);
            }
            return copy;
        }
    }

    /** Logs every warning immediately and counts it for the reload feedback. */
    private static final class Warnings {

        private final PluginLogger logger;
        private int count;

        Warnings(PluginLogger logger) {
            this.logger = logger;
        }

        void warn(String message) {
            logger.warn(message);
            count++;
        }

        int count() {
            return count;
        }
    }
}

package me.DesDaugtMa.backup.config;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * One migration step of a configuration file from {@link #fromVersion()} to {@code fromVersion() + 1}.
 *
 * <p>A migration only moves or converts existing user values. Keys that are new in the target version do not need
 * to be added here – they are filled in from the bundled template after all migrations ran. A migration must not
 * set {@code config-version}; the {@link ConfigManager} does that.</p>
 */
public interface ConfigMigration {

    int fromVersion();

    void migrate(YamlConfiguration configuration);
}

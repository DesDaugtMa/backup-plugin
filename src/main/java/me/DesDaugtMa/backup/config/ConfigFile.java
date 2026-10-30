package me.DesDaugtMa.backup.config;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Describes one configuration file in the plugin data folder: its name (which is also the name of the bundled
 * template), the {@code config-version} this plugin build supports and the migration chain leading up to it.
 *
 * @param fileName       file name, e.g. {@code config.yml}
 * @param currentVersion highest supported {@code config-version}
 * @param migrations     exactly one step for every version from 1 up to {@code currentVersion - 1}
 */
public record ConfigFile(String fileName, int currentVersion, List<ConfigMigration> migrations) {

    public ConfigFile {
        Objects.requireNonNull(fileName, "fileName");
        if (currentVersion < 1) {
            throw new IllegalArgumentException("currentVersion must be at least 1: " + currentVersion);
        }
        migrations = migrations.stream().sorted(Comparator.comparingInt(ConfigMigration::fromVersion)).toList();
        // A gap in the chain would silently skip user data, so it is rejected when the plugin is built/started.
        for (int index = 0; index < migrations.size(); index++) {
            if (migrations.get(index).fromVersion() != index + 1) {
                throw new IllegalArgumentException("Incomplete migration chain for " + fileName);
            }
        }
        if (migrations.size() != currentVersion - 1) {
            throw new IllegalArgumentException("Incomplete migration chain for " + fileName);
        }
    }

    /** Returns the migration that upgrades the file from the given version. */
    public ConfigMigration migrationFrom(int version) {
        return migrations.get(version - 1);
    }
}

package me.DesDaugtMa.backup.config;

/**
 * Immutable, validated snapshot of {@code config.yml}.
 *
 * @param debug {@code logging.debug}
 */
public record PluginConfig(boolean debug) {
}

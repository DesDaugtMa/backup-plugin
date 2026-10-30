package me.DesDaugtMa.backup.config;

import java.util.Objects;

import me.DesDaugtMa.backup.message.Messages;

/**
 * All configuration files of the plugin as one immutable unit, so that a reload swaps them atomically.
 */
public record ConfigSnapshot(PluginConfig config, Messages messages) {

    public ConfigSnapshot {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(messages, "messages");
    }
}

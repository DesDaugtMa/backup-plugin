package me.DesDaugtMa.backup.message;

import java.util.Set;

/**
 * All keys of {@code messages.yml} together with the placeholders each message may use.
 *
 * <p>The placeholder list is part of the validation: a placeholder that a message does not provide is treated
 * like an unknown tag.</p>
 */
public enum MessageKey {

    PREFIX("prefix"),
    NO_PERMISSION("no-permission"),
    HELP_HEADER("help.header"),
    HELP_ENTRY("help.entry", "command", "description"),
    HELP_RELOAD_DESCRIPTION("help.reload-description"),
    RELOAD_SUCCESS("reload.success"),
    RELOAD_SUCCESS_WITH_WARNINGS("reload.success-with-warnings", "warnings"),
    RELOAD_FAILED("reload.failed", "file"),
    RELOAD_ALREADY_RUNNING("reload.already-running");

    private final String path;
    private final Set<String> placeholders;

    MessageKey(String path, String... placeholders) {
        this.path = path;
        this.placeholders = Set.of(placeholders);
    }

    /** YAML path of the message in {@code messages.yml}. */
    public String path() {
        return path;
    }

    public Set<String> placeholders() {
        return placeholders;
    }
}

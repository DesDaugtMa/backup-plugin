package me.DesDaugtMa.backup.message;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Immutable, validated snapshot of {@code messages.yml}: one MiniMessage template per {@link MessageKey}.
 * An empty template means the message is disabled.
 */
public record Messages(Map<MessageKey, String> templates) {

    public Messages {
        templates = Map.copyOf(templates);
        Set<MessageKey> missing = EnumSet.allOf(MessageKey.class);
        missing.removeAll(templates.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing message templates: " + missing);
        }
    }

    public String template(MessageKey key) {
        return templates.get(key);
    }
}

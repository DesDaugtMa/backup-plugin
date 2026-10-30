package me.DesDaugtMa.backup.message;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

/**
 * Renders messages from the active {@link Messages} snapshot and sends them.
 *
 * <p>Runtime values must be passed as {@code Placeholder.unparsed(...)} so that they cannot inject MiniMessage
 * tags. Disabled (empty) messages are never sent. Must be used on the main thread when sending.</p>
 */
public final class MessageService {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Supplier<Messages> messages;

    /**
     * @param messages supplies the currently active snapshot; read once per message so a concurrent reload never
     *                 mixes prefix and text from different snapshots
     */
    public MessageService(Supplier<Messages> messages) {
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    /** Sends the message with the configured prefix, unless it is disabled. */
    public void send(CommandSender recipient, MessageKey key, TagResolver... placeholders) {
        Messages snapshot = messages.get();
        render(snapshot, key, placeholders).ifPresent(body -> {
            String prefix = snapshot.template(MessageKey.PREFIX);
            recipient.sendMessage(prefix.isEmpty()
                    ? body
                    // textOfChildren keeps the prefix style from leaking into the message.
                    : Component.textOfChildren(miniMessage.deserialize(prefix), body));
        });
    }

    /** Sends the message without prefix (e.g. follow-up lines of a list), unless it is disabled. */
    public void sendWithoutPrefix(CommandSender recipient, MessageKey key, TagResolver... placeholders) {
        render(messages.get(), key, placeholders).ifPresent(recipient::sendMessage);
    }

    /** Renders a message as a component without prefix; empty if the message is disabled. */
    public Optional<Component> render(MessageKey key, TagResolver... placeholders) {
        return render(messages.get(), key, placeholders);
    }

    private Optional<Component> render(Messages snapshot, MessageKey key, TagResolver... placeholders) {
        String template = snapshot.template(key);
        if (template.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(miniMessage.deserialize(template, placeholders));
    }
}

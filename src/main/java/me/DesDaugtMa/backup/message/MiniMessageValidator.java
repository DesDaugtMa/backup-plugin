package me.DesDaugtMa.backup.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.jspecify.annotations.Nullable;

/**
 * Checks MiniMessage templates from {@code messages.yml}.
 *
 * <p>MiniMessage itself never rejects input: unknown tags and tags with invalid arguments are silently rendered as
 * plain text. To detect typos, the template is parsed with a resolver that accepts every tag name and records the
 * ones that the standard tags and the allowed placeholders cannot resolve. Unclosed tags and {@code <reset>} are
 * allowed (no strict mode).</p>
 */
public final class MiniMessageValidator {

    private final MiniMessage parser = MiniMessage.builder().tags(TagResolver.empty()).build();

    /**
     * Validates a template.
     *
     * @param template     the MiniMessage text
     * @param placeholders names of the placeholders the message is rendered with
     * @return a human-readable description of the first problem, or empty if the template is valid
     */
    public Optional<String> findProblem(String template, Set<String> placeholders) {
        List<TagResolver> known = new ArrayList<>();
        known.add(StandardTags.defaults());
        // The validator only needs to know that the placeholder exists; the value is irrelevant.
        placeholders.forEach(name -> known.add(Placeholder.unparsed(name, "")));
        RecordingResolver recorder = new RecordingResolver(TagResolver.resolver(known));
        try {
            parser.deserialize(template, recorder);
        } catch (ParsingException exception) {
            return Optional.of("nicht lesbar (" + exception.getMessage() + ")");
        }
        return recorder.problems.stream().findFirst();
    }

    /** Claims every tag name so that unresolvable tags reach {@link #resolve} instead of becoming plain text. */
    private static final class RecordingResolver implements TagResolver {

        private final TagResolver delegate;
        private final List<String> problems = new ArrayList<>();

        private RecordingResolver(TagResolver delegate) {
            this.delegate = delegate;
        }

        @Override
        public @Nullable Tag resolve(String name, ArgumentQueue arguments, Context ctx) throws ParsingException {
            if (!delegate.has(name)) {
                problems.add("unbekannter Tag <" + name + ">");
                return null;
            }
            try {
                Tag tag = delegate.resolve(name, arguments, ctx);
                if (tag == null) {
                    problems.add("ungültige Argumente für <" + name + ">");
                }
                return tag;
            } catch (ParsingException exception) {
                problems.add("ungültige Argumente für <" + name + ">");
                throw exception;
            }
        }

        @Override
        public boolean has(String name) {
            return true;
        }
    }
}

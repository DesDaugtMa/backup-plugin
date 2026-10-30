package me.DesDaugtMa.backup.command;

import java.util.List;
import java.util.Objects;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.DesDaugtMa.backup.message.MessageKey;
import me.DesDaugtMa.backup.message.MessageService;
import me.DesDaugtMa.backup.service.ReloadService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 * The {@code /backup} command (Paper Brigadier API).
 *
 * <p>{@code /backup} shows the help, filtered by permission. Subcommands are protected with {@code requires(...)}:
 * players without the permission neither see them in tab completion nor can execute them (vanilla error).</p>
 */
public final class BackupCommand {

    public static final String RELOAD_PERMISSION = "backup.admin.reload";

    private static final String NAME = "backup";
    private static final String DESCRIPTION = "Befehle des Backup-Plugins";

    /** A subcommand as listed in the help. */
    private record HelpEntry(String usage, String permission, MessageKey description) {
    }

    private static final List<HelpEntry> HELP_ENTRIES = List.of(
            new HelpEntry("/backup reload", RELOAD_PERMISSION, MessageKey.HELP_RELOAD_DESCRIPTION));

    private final MessageService messageService;
    private final ReloadService reloadService;

    public BackupCommand(MessageService messageService, ReloadService reloadService) {
        this.messageService = Objects.requireNonNull(messageService, "messageService");
        this.reloadService = Objects.requireNonNull(reloadService, "reloadService");
    }

    public void register(LifecycleEventManager<Plugin> lifecycleManager) {
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS,
                event -> event.registrar().register(buildTree(), DESCRIPTION));
    }

    private LiteralCommandNode<CommandSourceStack> buildTree() {
        return Commands.literal(NAME)
                .executes(context -> {
                    showHelp(context.getSource().getSender());
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission(RELOAD_PERMISSION))
                        .executes(context -> {
                            reloadService.reload(context.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    private void showHelp(CommandSender sender) {
        List<HelpEntry> allowed = HELP_ENTRIES.stream()
                .filter(entry -> sender.hasPermission(entry.permission()))
                .toList();
        if (allowed.isEmpty()) {
            messageService.send(sender, MessageKey.NO_PERMISSION);
            return;
        }
        messageService.send(sender, MessageKey.HELP_HEADER);
        for (HelpEntry entry : allowed) {
            Component description = messageService.render(entry.description()).orElse(Component.empty());
            messageService.sendWithoutPrefix(sender, MessageKey.HELP_ENTRY,
                    Placeholder.unparsed("command", entry.usage()),
                    Placeholder.component("description", description));
        }
    }
}

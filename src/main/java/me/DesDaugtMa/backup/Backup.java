package me.DesDaugtMa.backup;

import me.DesDaugtMa.backup.command.BackupCommand;
import me.DesDaugtMa.backup.config.ConfigManager;
import me.DesDaugtMa.backup.logging.PluginLogger;
import me.DesDaugtMa.backup.message.MessageService;
import me.DesDaugtMa.backup.message.MiniMessageValidator;
import me.DesDaugtMa.backup.service.ReloadService;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

/**
 * Plugin entry point: wires the services together, contains no logic of its own.
 */
public final class Backup extends JavaPlugin {

    // Nullable because onDisable must also work after a failed onEnable.
    private @Nullable PluginLogger pluginLogger;
    private @Nullable ReloadService reloadService;

    @Override
    public void onEnable() {
        long start = System.nanoTime();
        PluginLogger logger = new PluginLogger(getComponentLogger());
        pluginLogger = logger;

        ConfigManager configManager;
        try {
            configManager = new ConfigManager(getDataPath(), this::getResource, logger, new MiniMessageValidator());
        } catch (IllegalStateException exception) {
            logger.error("Plugin kann nicht starten, die mitgelieferten Konfigurationsvorlagen sind defekt.",
                    exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        configManager.apply(configManager.loadInitial());

        MessageService messageService = new MessageService(() -> configManager.current().messages());
        reloadService = new ReloadService(this, configManager, messageService, logger);
        new BackupCommand(messageService, reloadService).register(getLifecycleManager());

        logger.info("Backup " + getPluginMeta().getVersion() + " aktiviert (Debug: "
                + (logger.isDebugEnabled() ? "an" : "aus") + ") in " + (System.nanoTime() - start) / 1_000_000
                + " ms.");
    }

    @Override
    public void onDisable() {
        if (reloadService != null) {
            reloadService.shutdown();
            reloadService = null;
        }
        if (pluginLogger != null) {
            pluginLogger.info("Backup deaktiviert.");
        }
    }
}

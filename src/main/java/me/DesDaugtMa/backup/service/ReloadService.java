package me.DesDaugtMa.backup.service;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import me.DesDaugtMa.backup.config.ConfigManager;
import me.DesDaugtMa.backup.config.LoadResult;
import me.DesDaugtMa.backup.logging.PluginLogger;
import me.DesDaugtMa.backup.message.MessageKey;
import me.DesDaugtMa.backup.message.MessageService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;

/**
 * Reloads {@code config.yml} and {@code messages.yml} without blocking the main thread.
 *
 * <p>Flow: main thread (guard) → reload worker (file I/O, validation) → main thread (swap snapshot, feedback).
 * At most one reload runs at a time. If the plugin is disabled meanwhile, the result is discarded.</p>
 */
public final class ReloadService {

    private static final String WORKER_THREAD_NAME = "Backup-Reload-Worker";
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(5);
    private static final String ALL_FILES = ConfigManager.CONFIG_FILE.fileName() + ", "
            + ConfigManager.MESSAGES_FILE.fileName();

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final MessageService messageService;
    private final PluginLogger logger;
    private final ExecutorService worker;
    private final AtomicBoolean running = new AtomicBoolean();

    public ReloadService(Plugin plugin, ConfigManager configManager, MessageService messageService,
                         PluginLogger logger) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.messageService = Objects.requireNonNull(messageService, "messageService");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.worker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, WORKER_THREAD_NAME);
            // Never keep the JVM alive because of a reload; onDisable already waits with a timeout.
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Starts an asynchronous reload on behalf of the sender. Main thread only.
     */
    public void reload(CommandSender sender) {
        if (!running.compareAndSet(false, true)) {
            logger.debug(() -> "Reload von " + sender.getName() + " abgelehnt – es läuft bereits einer.");
            messageService.send(sender, MessageKey.RELOAD_ALREADY_RUNNING);
            return;
        }
        long start = System.nanoTime();
        String initiator = sender.getName();
        logger.debug(() -> "Reload von " + initiator + " gestartet – wechsle vom Main-Thread auf "
                + WORKER_THREAD_NAME + ".");
        try {
            worker.execute(() -> runOnWorker(sender, initiator, start));
        } catch (RejectedExecutionException exception) {
            running.set(false);
            logger.warn("Reload von " + initiator + " nicht gestartet, weil das Plugin gerade deaktiviert wird.",
                    exception);
        }
    }

    private void runOnWorker(CommandSender sender, String initiator, long start) {
        boolean handedOver = false;
        try {
            LoadResult result;
            try {
                result = configManager.reload();
            } catch (RuntimeException exception) {
                logger.error("Unerwarteter Fehler beim Neuladen der Konfiguration – bisherige Einstellungen bleiben"
                        + " aktiv.", exception);
                result = new LoadResult.Failure(ALL_FILES);
            }
            logger.debug(() -> "Dateien verarbeitet nach " + elapsedMillis(start) + " ms – wechsle zurück auf den"
                    + " Main-Thread.");
            handedOver = handOver(sender, initiator, start, result);
        } catch (RuntimeException exception) {
            logger.error("Unerwarteter Fehler im Reload-Worker.", exception);
        } finally {
            if (!handedOver) {
                running.set(false);
            }
        }
    }

    /** Schedules the completion on the main thread. Returns false if the plugin is (being) disabled. */
    private boolean handOver(CommandSender sender, String initiator, long start, LoadResult result) {
        if (!plugin.isEnabled()) {
            logger.debug("Plugin wird deaktiviert – Reload-Ergebnis wird verworfen.");
            return false;
        }
        try {
            plugin.getServer().getScheduler().runTask(plugin, () -> complete(sender, initiator, start, result));
            return true;
        } catch (IllegalPluginAccessException exception) {
            logger.debug(() -> "Plugin wurde während des Reloads deaktiviert – Ergebnis wird verworfen ("
                    + exception.getMessage() + ").");
            return false;
        }
    }

    private void complete(CommandSender sender, String initiator, long start, LoadResult result) {
        try {
            switch (result) {
                case LoadResult.Success success -> {
                    configManager.apply(success.snapshot());
                    logger.info("Konfiguration neu geladen von " + initiator + " in " + elapsedMillis(start)
                            + " ms (" + success.warnings() + " Warnungen).");
                    if (success.warnings() == 0) {
                        sendIfOnline(sender, MessageKey.RELOAD_SUCCESS);
                    } else {
                        sendIfOnline(sender, MessageKey.RELOAD_SUCCESS_WITH_WARNINGS,
                                Placeholder.unparsed("warnings", Integer.toString(success.warnings())));
                    }
                }
                case LoadResult.Failure failure -> {
                    logger.debug(() -> "Reload von " + initiator + " fehlgeschlagen (" + failure.fileName()
                            + ") nach " + elapsedMillis(start) + " ms.");
                    sendIfOnline(sender, MessageKey.RELOAD_FAILED, Placeholder.unparsed("file", failure.fileName()));
                }
            }
        } finally {
            running.set(false);
        }
    }

    private void sendIfOnline(CommandSender sender, MessageKey key,
                              TagResolver... placeholders) {
        if (sender instanceof Player player && !player.isOnline()) {
            logger.debug(() -> "Auslöser " + player.getName() + " ist offline – keine Rückmeldung.");
            return;
        }
        messageService.send(sender, key, placeholders);
    }

    /**
     * Stops the worker. Waits up to {@link #SHUTDOWN_TIMEOUT} for a running reload, then interrupts it.
     */
    public void shutdown() {
        worker.shutdown();
        try {
            if (!worker.awaitTermination(SHUTDOWN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                logger.warn("Reload-Worker hat sich nicht innerhalb von " + SHUTDOWN_TIMEOUT.toSeconds()
                        + " s beendet und wird abgebrochen.");
                worker.shutdownNow();
            }
        } catch (InterruptedException exception) {
            logger.warn("Warten auf den Reload-Worker wurde unterbrochen – Worker wird abgebrochen.", exception);
            worker.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}

package me.DesDaugtMa.backup.logging;

import java.util.Objects;
import java.util.function.Supplier;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

/**
 * Central logger of the plugin. Every class receives this instance instead of using the Bukkit logger directly.
 *
 * <p>Debug output is written on INFO level with a {@code [DEBUG]} prefix, because the server console filters
 * DEBUG/FINE levels by default. Without debug mode, debug messages are neither logged nor built.</p>
 */
public final class PluginLogger {

    private static final String DEBUG_PREFIX = "[DEBUG] ";

    private final ComponentLogger logger;
    // Written on the main thread (config apply), read by the reload worker.
    private volatile boolean debugEnabled;

    public PluginLogger(ComponentLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void info(String message) {
        logger.info(message);
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void warn(String message, Throwable throwable) {
        logger.warn(message, throwable);
    }

    public void error(String message) {
        logger.error(message);
    }

    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    public void debug(String message) {
        if (debugEnabled) {
            logger.info(DEBUG_PREFIX + message);
        }
    }

    /**
     * Logs a debug message that is only built when debug mode is active.
     *
     * @param messageSupplier builds the message; not called when debug mode is off
     */
    public void debug(Supplier<String> messageSupplier) {
        if (debugEnabled) {
            logger.info(DEBUG_PREFIX + messageSupplier.get());
        }
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;
    }
}

package me.DesDaugtMa.backup.config;

/**
 * Outcome of reading all configuration files.
 */
public sealed interface LoadResult {

    /**
     * All files were read; problems were fixed with fallbacks.
     *
     * @param snapshot the new snapshot, not yet applied
     * @param warnings number of problems that were replaced by a fallback
     */
    record Success(ConfigSnapshot snapshot, int warnings) implements LoadResult {
    }

    /**
     * A file could not be read or migrated; nothing was written and the active snapshot stays in place.
     *
     * @param fileName the file that caused the failure
     */
    record Failure(String fileName) implements LoadResult {
    }
}

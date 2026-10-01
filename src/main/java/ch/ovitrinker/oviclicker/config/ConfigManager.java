package ch.ovitrinker.oviclicker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Loads and saves the settings as JSON in {@code config/oviclicker.json}.
 *
 * <p>Writes are always atomic: first into a {@code .tmp} file, then the previous file is
 * backed up as {@code .bak} and the temporary file is moved into place. That way a valid file
 * always remains if the game crashes while writing.</p>
 *
 * <p>Missing or broken fields never cause a crash: GSON leaves unknown fields at their
 * default value, and an unreadable file is discarded.</p>
 */
public final class ConfigManager {

    /** The mod's logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger("oviclicker");

    /** GSON instance with readable formatting. */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** File name of the config in the {@code config} folder. */
    private static final String FILE_NAME = "oviclicker.json";

    /** The currently valid settings. */
    private static OviClickerConfig config = new OviClickerConfig();

    private ConfigManager() {
    }

    /**
     * Returns the active config.
     *
     * @return the settings, never {@code null}
     */
    public static OviClickerConfig get() {
        return config;
    }

    /**
     * Returns the path of the config file.
     *
     * @return absolute path to {@code config/oviclicker.json}
     */
    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    /**
     * Loads the config from disk. If no file exists, the defaults are used and written
     * immediately. If the file is broken, it is ignored and the mod starts with the defaults.
     */
    public static void load() {
        Path path = getConfigPath();

        if (!Files.exists(path)) {
            config = new OviClickerConfig();
            save();
            return;
        }

        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            OviClickerConfig loaded = GSON.fromJson(json, OviClickerConfig.class);

            if (loaded == null) {
                LOGGER.warn("oviclicker.json is empty, using the defaults.");
                loaded = new OviClickerConfig();
            }

            loaded.clamp();
            config = loaded;
        } catch (Exception exception) {
            // A broken file must not prevent the client from starting
            LOGGER.error("Could not read oviclicker.json, using the defaults.", exception);
            config = new OviClickerConfig();
        }
    }

    /**
     * Writes the current config to disk atomically.
     *
     * <p>Called after every change, so mode and master switch survive a server switch or a
     * client restart.</p>
     */
    public static void save() {
        config.clamp();

        Path path = getConfigPath();
        Path tempPath = path.resolveSibling(FILE_NAME + ".tmp");
        Path backupPath = path.resolveSibling(FILE_NAME + ".bak");

        try {
            Files.createDirectories(path.getParent());
            Files.writeString(tempPath, GSON.toJson(config), StandardCharsets.UTF_8);

            // Back up the previous version before replacing it
            if (Files.exists(path)) {
                Files.copy(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
            }

            try {
                Files.move(tempPath, path,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                // Not every file system supports atomic moves
                Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not write oviclicker.json.", exception);
        }
    }

    /**
     * Replaces the active config and saves it immediately.
     *
     * @param newConfig the new settings, {@code null} is ignored
     */
    public static void replaceAndSave(OviClickerConfig newConfig) {
        if (newConfig == null) return;
        config.copyFrom(newConfig);
        save();
    }
}

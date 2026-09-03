package ch.andrinzwicky.oviclicker.config;

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
 * Laedt und speichert die Einstellungen als JSON unter {@code config/oviclicker.json}.
 *
 * <p>Geschrieben wird immer atomar: zuerst in eine {@code .tmp}-Datei, danach wird die
 * bisherige Datei als {@code .bak} gesichert und die temporaere Datei an ihre Stelle
 * verschoben. Damit bleibt bei einem Absturz waehrend des Schreibens immer eine
 * gueltige Datei uebrig.</p>
 *
 * <p>Fehlende oder defekte Felder fuehren nie zu einem Absturz: GSON laesst unbekannte
 * Felder auf ihrem Standardwert stehen, und eine unlesbare Datei wird verworfen.</p>
 */
public final class ConfigManager {

    /** Logger des Mods. */
    private static final Logger LOGGER = LoggerFactory.getLogger("oviclicker");

    /** GSON-Instanz mit lesbarer Formatierung. */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Dateiname der Konfiguration im Ordner {@code config}. */
    private static final String FILE_NAME = "oviclicker.json";

    /** Die aktuell gueltigen Einstellungen. */
    private static OviClickerConfig config = new OviClickerConfig();

    private ConfigManager() {
    }

    /**
     * Gibt die aktive Konfiguration zurueck.
     *
     * @return die Einstellungen, nie {@code null}
     */
    public static OviClickerConfig get() {
        return config;
    }

    /**
     * Gibt den Pfad der Konfigurationsdatei zurueck.
     *
     * @return absoluter Pfad auf {@code config/oviclicker.json}
     */
    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    /**
     * Laedt die Konfiguration von der Festplatte. Existiert keine Datei, werden die
     * Standardwerte verwendet und sofort geschrieben. Ist die Datei defekt, wird sie
     * ignoriert und der Mod startet mit den Standardwerten.
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
                LOGGER.warn("oviclicker.json ist leer, es werden die Standardwerte verwendet.");
                loaded = new OviClickerConfig();
            }

            loaded.clamp();
            config = loaded;
        } catch (Exception exception) {
            // Defekte Datei darf den Client nicht am Start hindern
            LOGGER.error("oviclicker.json konnte nicht gelesen werden, es gelten die Standardwerte.", exception);
            config = new OviClickerConfig();
        }
    }

    /**
     * Schreibt die aktuelle Konfiguration atomar auf die Festplatte.
     *
     * <p>Diese Methode wird nach jeder Aenderung aufgerufen, damit Modus und Master-Schalter
     * auch nach einem Serverwechsel oder einem Client-Neustart erhalten bleiben.</p>
     */
    public static void save() {
        config.clamp();

        Path path = getConfigPath();
        Path tempPath = path.resolveSibling(FILE_NAME + ".tmp");
        Path backupPath = path.resolveSibling(FILE_NAME + ".bak");

        try {
            Files.createDirectories(path.getParent());
            Files.writeString(tempPath, GSON.toJson(config), StandardCharsets.UTF_8);

            // Bisherige Fassung sichern, bevor sie ersetzt wird
            if (Files.exists(path)) {
                Files.copy(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
            }

            try {
                Files.move(tempPath, path,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                // Nicht jedes Dateisystem kann atomar verschieben
                Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.error("oviclicker.json konnte nicht geschrieben werden.", exception);
        }
    }

    /**
     * Ersetzt die aktive Konfiguration und speichert sie sofort.
     *
     * @param newConfig die neuen Einstellungen, {@code null} wird ignoriert
     */
    public static void replaceAndSave(OviClickerConfig newConfig) {
        if (newConfig == null) return;
        config.copyFrom(newConfig);
        save();
    }
}

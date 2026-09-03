package ch.andrinzwicky.oviclicker;

import ch.andrinzwicky.oviclicker.config.ConfigManager;
import ch.andrinzwicky.oviclicker.feature.OviClickerEngine;
import ch.andrinzwicky.oviclicker.feature.AutoEatHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Einstiegspunkt des Mods auf der Client-Seite.
 *
 * <p>Der Mod ist rein client-seitig: er simuliert ausschliesslich lokale Eingaben,
 * faelscht keine Netzwerkpakete und umgeht keine Serverlogik.</p>
 */
public class OviClickerClient implements ClientModInitializer {

    /** Modkennung, wie sie in der {@code fabric.mod.json} steht. */
    public static final String MOD_ID = "oviclicker";

    /** Logger des Mods. */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Version des Mods, wird von Stonecutter eingesetzt. */
    public static final String VERSION = /*$ mod_version*/ "1.0.0";

    /** Minecraft-Version, gegen die dieses Jar gebaut wurde. */
    public static final String MINECRAFT = /*$ minecraft*/ "26.2";

    /**
     * Wird beim Start des Clients aufgerufen: laedt die Einstellungen, registriert die
     * Tastenbelegungen, das HUD-Element und die Tick-Logik.
     */
    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        KeybindManager.register();
        HudRenderer.register();

        // Die gesamte Klick-Logik laeuft im Client-Tick, nicht in einem eigenen Thread.
        // Der AutoEat kommt bewusst nach dem Klicker: dieser gibt seine Taste zuerst
        // frei, erst danach haelt der AutoEat die Taste "Benutzen" fuer den Bissen.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            KeybindManager.handleInput(client);
            OviClickerEngine.onEndClientTick(client);
            AutoEatHandler.onEndClientTick(client);
        });

        LOGGER.info("OviClicker {} fuer Minecraft {} geladen.", VERSION, MINECRAFT);
    }
}

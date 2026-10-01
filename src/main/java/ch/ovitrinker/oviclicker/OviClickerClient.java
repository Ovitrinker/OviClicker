package ch.ovitrinker.oviclicker;

import ch.ovitrinker.oviclicker.config.ConfigManager;
import ch.ovitrinker.oviclicker.feature.OviClickerEngine;
import ch.ovitrinker.oviclicker.feature.AutoEatHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side entry point of the mod.
 *
 * <p>The mod is purely client-side: it only simulates local input, forges no network packets
 * and bypasses no server logic.</p>
 */
public class OviClickerClient implements ClientModInitializer {

    /** Mod ID as listed in {@code fabric.mod.json}. */
    public static final String MOD_ID = "oviclicker";

    /** The mod's logger. */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Mod version, inserted by Stonecutter. */
    public static final String VERSION = /*$ mod_version*/ "1.0.0";

    /** Minecraft version this jar was built against. */
    public static final String MINECRAFT = /*$ minecraft*/ "26.2";

    /**
     * Called when the client starts: loads the settings, registers the key binds, the HUD
     * element and the tick logic.
     */
    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        KeybindManager.register();
        HudRenderer.register();

        // All click logic runs in the client tick, not in a thread of its own.
        // AutoEat deliberately comes after the clicker: the clicker releases its key first,
        // only then does AutoEat hold the "Use" key for the bite.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            KeybindManager.handleInput(client);
            OviClickerEngine.onEndClientTick(client);
            AutoEatHandler.onEndClientTick(client);
        });

        LOGGER.info("OviClicker {} for Minecraft {} loaded.", VERSION, MINECRAFT);
    }
}

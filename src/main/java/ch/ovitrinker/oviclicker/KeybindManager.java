package ch.ovitrinker.oviclicker;

import ch.ovitrinker.oviclicker.compat.ClientCompat;
import ch.ovitrinker.oviclicker.config.OviClickerConfig;
import ch.ovitrinker.oviclicker.config.ConfigManager;
import ch.ovitrinker.oviclicker.feature.OviClickerEngine;
import ch.ovitrinker.oviclicker.feature.ClickMode;
import ch.ovitrinker.oviclicker.gui.OviClickerScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

//? if <26.1 {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
*///?} else
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

/**
 * Registers and handles the mod's key binds.
 *
 * <p>All keys are registered via the Fabric API and therefore appear in their own category
 * under Options -&gt; Controls -&gt; Key Binds, where they can be rebound freely.</p>
 *
 * <p>Note on the category: since 1.21.11 categories are no longer free-form strings but
 * {@code KeyMapping.Category} objects with an {@code Identifier}. The resulting translation
 * key is {@code key.category.<namespace>.<path>}, here {@code key.category.oviclicker.main}.
 * This applies to all target versions of this mod, so no case distinction is needed.</p>
 */
public final class KeybindManager {

    /** Own category for all of the mod's keys. */
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("oviclicker", "main"));

    /**
     * Opens the settings.
     *
     * <p>The default binding is the key that produces a "$" on a Swiss layout. GLFW always
     * reports keys in the US layout. The Swiss "$" key sits on scancode 0x2B, the ISO key
     * left of Enter, which corresponds to the backslash key in the US layout. The matching
     * GLFW code is {@code GLFW_KEY_BACKSLASH} (92).</p>
     */
    public static KeyMapping openGuiKey;

    /** Turns the mod as a whole on and off. */
    public static KeyMapping toggleKey;

    /** Cycles the mode: OFF -&gt; AUTOATTACK -&gt; TIMER -&gt; OFF. */
    public static KeyMapping cycleModeKey;

    /** Default keys; from 26.3 on, key binds are SDL scancodes instead of GLFW key codes. */
    //? if <26.3 {
    private static final int KEY_OPEN_GUI = org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSLASH;
    private static final int KEY_TOGGLE = org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
    //?} else {
    /*private static final int KEY_OPEN_GUI = InputConstants.KEY_BACKSLASH;
    private static final int KEY_TOGGLE = InputConstants.KEY_RSHIFT;
    *///?}

    private KeybindManager() {
    }

    /**
     * Registers all key binds. Called once when the client starts.
     */
    public static void register() {
        openGuiKey = register(new KeyMapping(
                "key.oviclicker.open_gui", KEY_OPEN_GUI, CATEGORY));

        toggleKey = register(new KeyMapping(
                "key.oviclicker.toggle", KEY_TOGGLE, CATEGORY));

        // No default binding, to avoid conflicts
        cycleModeKey = register(new KeyMapping(
                "key.oviclicker.cycle_mode", InputConstants.UNKNOWN.getValue(), CATEGORY));
    }

    /**
     * Handles pressed keys. Called every client tick.
     *
     * @param client the client instance, may be {@code null}
     */
    public static void handleInput(Minecraft client) {
        if (client == null) return;

        OviClickerConfig config = ConfigManager.get();

        while (toggleKey.consumeClick()) {
            config.masterEnabled = !config.masterEnabled;
            ConfigManager.save();
            OviClickerEngine.resetTimer();
        }

        while (cycleModeKey.consumeClick()) {
            ClickMode next = config.getMode().next();
            config.setMode(next);
            ConfigManager.save();
            OviClickerEngine.resetTimer();
        }

        while (openGuiKey.consumeClick()) {
            // Only open outside of other screens. While typing, Minecraft doesn't pass key
            // presses to key binds anyway.
            if (ClientCompat.getCurrentScreen(client) == null) {
                ClientCompat.openScreen(client, new OviClickerScreen(
                        Component.translatable("oviclicker.gui.title"), null));
            }
        }
    }

    /**
     * Passes a key bind on to the matching Fabric API.
     *
     * <p>Up to 1.21.11 the module is called {@code fabric-key-binding-api-v1} with the class
     * {@code KeyBindingHelper}, from 26.1 on {@code fabric-key-mapping-api-v1} with
     * {@code KeyMappingHelper}.</p>
     *
     * @param mapping the key bind to register
     * @return the same key bind, now registered
     */
    private static KeyMapping register(KeyMapping mapping) {
        //? if <26.1 {
        /*return KeyBindingHelper.registerKeyBinding(mapping);
        *///?} else
        return KeyMappingHelper.registerKeyMapping(mapping);
    }

    /**
     * Determines the key currently bound to a key bind.
     *
     * @param mapping the key bind
     * @return the bound key, {@code InputConstants.UNKNOWN} if none is bound
     */
    public static InputConstants.Key boundKeyOf(KeyMapping mapping) {
        //? if <26.1 {
        /*return KeyBindingHelper.getBoundKeyOf(mapping);
        *///?} else
        return KeyMappingHelper.getBoundKeyOf(mapping);
    }
}

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
import org.lwjgl.glfw.GLFW;

//? if <26.1 {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
*///?} else
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

/**
 * Registriert und verarbeitet die Tastenbelegungen des Mods.
 *
 * <p>Alle Tasten werden ueber die Fabric-API registriert und erscheinen dadurch unter
 * Optionen -&gt; Steuerung -&gt; Tastenbelegung in einer eigenen Kategorie. Dort koennen sie
 * frei umbelegt werden.</p>
 *
 * <p>Hinweis zur Kategorie: seit 1.21.11 sind Kategorien keine freien Zeichenketten mehr,
 * sondern {@code KeyMapping.Category}-Objekte mit einem {@code Identifier}. Der daraus
 * gebildete Uebersetzungsschluessel lautet {@code key.category.<namespace>.<pfad>}, hier
 * also {@code key.category.oviclicker.main}. Das gilt fuer alle Zielversionen dieses Mods,
 * eine Fallunterscheidung ist deshalb nicht noetig.</p>
 */
public final class KeybindManager {

    /** Eigene Kategorie fuer alle Tasten des Mods. */
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("oviclicker", "main"));

    /**
     * Oeffnet die Einstellungen.
     *
     * <p>Standardbelegung ist die Taste, die auf einem Schweizer Layout ein "$" erzeugt.
     * GLFW meldet Tasten immer in der Belegung des US-Layouts. Die Schweizer "$"-Taste
     * liegt auf dem Scancode 0x2B, also auf der ISO-Taste links von der Eingabetaste,
     * die im US-Layout der Backslash-Taste entspricht. Der zugehoerige GLFW-Code ist
     * {@code GLFW_KEY_BACKSLASH} (92).</p>
     */
    public static KeyMapping openGuiKey;

    /** Schaltet den Mod als Ganzes ein und aus. */
    public static KeyMapping toggleKey;

    /** Schaltet den Modus weiter: OFF -&gt; AUTOATTACK -&gt; TIMER -&gt; OFF. */
    public static KeyMapping cycleModeKey;

    private KeybindManager() {
    }

    /**
     * Registriert alle Tastenbelegungen. Wird einmalig beim Start des Clients aufgerufen.
     */
    public static void register() {
        openGuiKey = register(new KeyMapping(
                "key.oviclicker.open_gui", GLFW.GLFW_KEY_BACKSLASH, CATEGORY));

        toggleKey = register(new KeyMapping(
                "key.oviclicker.toggle", GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));

        // Ohne Standardbelegung, damit es keine Konflikte gibt
        cycleModeKey = register(new KeyMapping(
                "key.oviclicker.cycle_mode", GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
    }

    /**
     * Verarbeitet die gedrueckten Tasten. Wird in jedem Client-Tick aufgerufen.
     *
     * @param client die Client-Instanz, darf {@code null} sein
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
            // Nur ausserhalb anderer Bildschirme oeffnen. Waehrend einer Texteingabe
            // liefert Minecraft ohnehin keine Tastendruecke an Tastenbelegungen aus.
            if (ClientCompat.getCurrentScreen(client) == null) {
                ClientCompat.openScreen(client, new OviClickerScreen(
                        Component.translatable("oviclicker.gui.title"), null));
            }
        }
    }

    /**
     * Reicht eine Tastenbelegung an die passende Fabric-API weiter.
     *
     * <p>Das Modul heisst bis 1.21.11 {@code fabric-key-binding-api-v1} mit der Klasse
     * {@code KeyBindingHelper}, ab 26.1 {@code fabric-key-mapping-api-v1} mit
     * {@code KeyMappingHelper}.</p>
     *
     * @param mapping die zu registrierende Tastenbelegung
     * @return dieselbe Tastenbelegung, nun registriert
     */
    private static KeyMapping register(KeyMapping mapping) {
        //? if <26.1 {
        /*return KeyBindingHelper.registerKeyBinding(mapping);
        *///?} else
        return KeyMappingHelper.registerKeyMapping(mapping);
    }

    /**
     * Ermittelt die Taste, die aktuell auf eine Tastenbelegung gelegt ist.
     *
     * @param mapping die Tastenbelegung
     * @return die belegte Taste, {@code InputConstants.UNKNOWN} wenn keine belegt ist
     */
    public static InputConstants.Key boundKeyOf(KeyMapping mapping) {
        //? if <26.1 {
        /*return KeyBindingHelper.getBoundKeyOf(mapping);
        *///?} else
        return KeyMappingHelper.getBoundKeyOf(mapping);
    }
}

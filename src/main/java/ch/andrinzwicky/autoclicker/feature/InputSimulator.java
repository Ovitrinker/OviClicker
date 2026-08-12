package ch.andrinzwicky.autoclicker.feature;

import ch.andrinzwicky.autoclicker.KeybindManager;
import ch.andrinzwicky.autoclicker.mixin.MinecraftAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Simuliert lokale Eingaben.
 *
 * <p>Es wird ausschliesslich der Zustand der Vanilla-Tastenbelegungen gesetzt oder eine
 * Vanilla-Methode aufgerufen. Es werden keine Netzwerkpakete erzeugt und keine Serverlogik
 * umgangen: das Spiel verarbeitet die simulierte Eingabe genau gleich wie eine echte.</p>
 *
 * <p>Es ist immer hoechstens eine Taste gleichzeitig gedrueckt. Beim Antippen wird sie nach
 * der eingestellten Anzahl Ticks automatisch wieder losgelassen, im Halte-Modus erst, wenn
 * die Bedingungen nicht mehr erfuellt sind oder der Mod abgeschaltet wird.</p>
 */
public final class InputSimulator {

    /** Aktuell gedrueckt gehaltene Tastenbelegung, {@code null} wenn keine. */
    private static KeyMapping heldMapping = null;

    /** Verbleibende Ticks, bis eine angetippte Taste wieder losgelassen wird. */
    private static int releaseCountdown = 0;

    private InputSimulator() {
    }

    /**
     * Zaehlt die Haltedauer eines Tastendrucks herunter. Muss in jedem Client-Tick
     * aufgerufen werden, damit angetippte Tasten zuverlaessig wieder losgelassen werden.
     */
    public static void tick() {
        if (releaseCountdown > 0 && --releaseCountdown <= 0) {
            release();
        }
    }

    /**
     * Loest eine Aktion einmalig aus.
     *
     * @param client         die Client-Instanz
     * @param action         die auszuloesende Aktion
     * @param durationTicks  Anzahl Ticks, die eine Taste gedrueckt bleibt (mindestens 1)
     */
    public static void tap(Minecraft client, ClickAction action, int durationTicks) {
        if (client == null || action == null) return;

        // Angriff und Benutzen laufen ueber die Vanilla-Methoden, damit Reichweite,
        // Cooldown und Animation exakt der Vanilla-Logik entsprechen.
        if (action == ClickAction.ATTACK) {
            ((MinecraftAccessor) (Object) client).autoclicker$startAttack();
            return;
        }
        if (action == ClickAction.USE) {
            ((MinecraftAccessor) (Object) client).autoclicker$startUseItem();
            return;
        }

        KeyMapping mapping = action.getMapping(client.options);
        if (mapping == null) return;

        if (heldMapping != null && heldMapping != mapping) release();

        mapping.setDown(true);

        // Aktionen wie das Ablegen von Gegenstaenden werten nicht den gehaltenen Zustand,
        // sondern die Anzahl Klicks aus. Das geht nur ueber die tatsaechlich belegte Taste.
        InputConstants.Key key = KeybindManager.boundKeyOf(mapping);
        if (key != null && !key.equals(InputConstants.UNKNOWN)) {
            KeyMapping.click(key);
        }

        heldMapping = mapping;
        releaseCountdown = Math.max(1, durationTicks);
    }

    /**
     * Haelt die Taste einer Aktion gedrueckt. Wiederholte Aufrufe halten sie weiterhin.
     *
     * @param client die Client-Instanz
     * @param action die zu haltende Aktion
     */
    public static void hold(Minecraft client, ClickAction action) {
        if (client == null || action == null) return;

        KeyMapping mapping = action.getMapping(client.options);
        if (mapping == null) return;

        if (heldMapping != null && heldMapping != mapping) release();

        mapping.setDown(true);
        heldMapping = mapping;
        releaseCountdown = 0;
    }

    /**
     * Laesst eine allenfalls gehaltene Taste los. Wird auch beim Abschalten des Mods, beim
     * Oeffnen eines Bildschirms und beim Verlassen der Welt aufgerufen, damit keine Taste
     * haengen bleibt.
     */
    public static void release() {
        if (heldMapping != null) {
            heldMapping.setDown(false);
            heldMapping = null;
        }
        releaseCountdown = 0;
    }

    /**
     * Gibt an, ob der Mod gerade eine Taste gedrueckt haelt.
     *
     * @return {@code true}, wenn eine Taste gehalten wird
     */
    public static boolean isHolding() {
        return heldMapping != null;
    }

    /**
     * Prueft, ob die Taste einer Belegung tatsaechlich am Geraet gedrueckt ist.
     *
     * <p>Der Zustand wird direkt bei GLFW abgefragt und nicht ueber
     * {@code KeyMapping.isDown()}. Andernfalls wuerde der Mod im Halte-Modus seinen eigenen
     * simulierten Tastendruck als Eingabe des Spielers lesen und sich selbst am Leben
     * halten.</p>
     *
     * @param client  die Client-Instanz
     * @param mapping die zu pruefende Tastenbelegung
     * @return {@code true}, wenn die Taste physisch gedrueckt ist
     */
    public static boolean isPhysicallyDown(Minecraft client, KeyMapping mapping) {
        if (client == null || mapping == null) return false;

        InputConstants.Key key = KeybindManager.boundKeyOf(mapping);
        if (key == null || key.equals(InputConstants.UNKNOWN)) return false;

        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(client.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS;
        }
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(client.getWindow(), key.getValue());
        }

        // Scancode-Belegungen lassen sich nicht direkt abfragen
        return mapping.isDown();
    }
}

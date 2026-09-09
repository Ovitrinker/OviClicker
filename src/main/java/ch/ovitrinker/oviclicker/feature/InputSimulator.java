package ch.ovitrinker.oviclicker.feature;

import ch.ovitrinker.oviclicker.KeybindManager;
import ch.ovitrinker.oviclicker.mixin.MinecraftAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
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
     * @param screenOpen     {@code true}, wenn gerade ein Bildschirm offen ist
     */
    public static void tap(Minecraft client, ClickAction action, int durationTicks, boolean screenOpen) {
        if (client == null || action == null) return;

        // Angriff und Benutzen laufen ueber die Vanilla-Methoden, damit Reichweite,
        // Cooldown und Animation exakt der Vanilla-Logik entsprechen.
        if (action == ClickAction.ATTACK) {
            ((MinecraftAccessor) (Object) client).oviclicker$startAttack();
            return;
        }
        if (action == ClickAction.USE) {
            ((MinecraftAccessor) (Object) client).oviclicker$startUseItem();
            return;
        }

        KeyMapping mapping = action.getMapping(client.options);
        if (mapping == null) return;

        if (heldMapping != null && heldMapping != mapping) release();

        mapping.setDown(true);

        if (screenOpen) {
            // Solange ein Bildschirm offen ist, wertet Minecraft keine Klickzaehler aus.
            // Gezaehlte Klicks wuerden sich anstauen und beim Schliessen auf einen Schlag
            // ausgeloest. Das Ablegen wird deshalb direkt so ausgefuehrt, wie es Minecraft
            // beim Druck auf die Ablegen-Taste tut.
            if (action == ClickAction.DROP) drop(client);
        } else {
            // Aktionen wie das Ablegen von Gegenstaenden werten nicht den gehaltenen Zustand,
            // sondern die Anzahl Klicks aus. Das geht nur ueber die tatsaechlich belegte Taste.
            InputConstants.Key key = KeybindManager.boundKeyOf(mapping);
            if (key != null && !key.equals(InputConstants.UNKNOWN)) {
                KeyMapping.click(key);
            }
        }

        heldMapping = mapping;
        releaseCountdown = Math.max(1, durationTicks);
    }

    /**
     * Haelt die Taste einer Aktion gedrueckt. Wiederholte Aufrufe halten sie weiterhin.
     *
     * @param client     die Client-Instanz
     * @param action     die zu haltende Aktion
     * @param screenOpen {@code true}, wenn gerade ein Bildschirm offen ist
     */
    public static void hold(Minecraft client, ClickAction action, boolean screenOpen) {
        if (client == null || action == null) return;

        KeyMapping mapping = action.getMapping(client.options);
        if (mapping == null) return;

        if (heldMapping != null && heldMapping != mapping) release();

        mapping.setDown(true);
        heldMapping = mapping;
        releaseCountdown = 0;

        if (!screenOpen || client.player == null) return;

        // Solange ein Bildschirm offen ist, ueberspringt Minecraft seine eigene
        // Tastenverarbeitung. Die Bewegungstasten wirken trotzdem, weil der Spieler den
        // gehaltenen Zustand direkt ausliest. Angriff und Benutzen laufen dagegen ueber
        // die uebersprungene Verarbeitung und werden hier genau gleich angestossen.
        MinecraftAccessor accessor = (MinecraftAccessor) (Object) client;
        if (action == ClickAction.ATTACK) {
            accessor.oviclicker$continueAttack(true);
        } else if (action == ClickAction.USE
                && accessor.oviclicker$getRightClickDelay() == 0
                && !client.player.isUsingItem()) {
            accessor.oviclicker$startUseItem();
        }
    }

    /**
     * Legt einen Gegenstand ab, genau wie es Minecraft beim Druck auf die Ablegen-Taste tut.
     *
     * @param client die Client-Instanz
     */
    private static void drop(Minecraft client) {
        if (client.player == null || client.player.isSpectator()) return;
        if (client.player.drop(false)) {
            client.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    /**
     * Laesst eine allenfalls gehaltene Taste los. Wird auch beim Abschalten des Mods, beim
     * Anhalten des Spiels und beim Verlassen der Welt aufgerufen, damit keine Taste
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

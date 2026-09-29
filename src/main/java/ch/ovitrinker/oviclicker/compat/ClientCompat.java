package ch.ovitrinker.oviclicker.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

//? if <26.3 {
import org.lwjgl.glfw.GLFW;
//?} else
/*import org.lwjgl.sdl.SDLMouse;*/

/**
 * Buendelt die wenigen Stellen, an denen sich die Minecraft-API zwischen den Zielversionen
 * unterscheidet.
 *
 * <p>Mit 26.2 ist die Screen-Verwaltung von {@code Minecraft} in die Klasse
 * {@code net.minecraft.client.gui.Gui} gewandert. In 1.21.11 und 26.1.x liegt sie noch
 * direkt auf {@code Minecraft}. Die Fallunterscheidung passiert ueber Stonecutter, damit
 * jede Version genau den Aufruf kompiliert, den sie kennt.</p>
 *
 * <p>Mit 26.3 hat Minecraft GLFW durch SDL3 ersetzt. Tastenbelegungen speichern seither
 * Scancodes, Maustasten werden bei SDL abgefragt, und {@code swing}/{@code drop} haben
 * neue Signaturen bekommen.</p>
 */
public final class ClientCompat {

    private ClientCompat() {
    }

    /**
     * Gibt den aktuell offenen Bildschirm zurueck.
     *
     * @param client die Client-Instanz
     * @return der offene Screen oder {@code null}, wenn keiner offen ist
     */
    public static Screen getCurrentScreen(Minecraft client) {
        //? if <26.2 {
        /*return client.screen;
        *///?} else
        return client.gui.screen();
    }

    /**
     * Oeffnet einen Bildschirm.
     *
     * @param client die Client-Instanz
     * @param screen der zu oeffnende Screen, {@code null} schliesst den aktuellen
     */
    public static void openScreen(Minecraft client, Screen screen) {
        //? if <26.2 {
        /*client.setScreen(screen);
        *///?} else
        client.gui.setScreen(screen);
    }

    /**
     * Schwingt die Haupthand, wie es Minecraft nach einem Angriff tut.
     *
     * @param player der lokale Spieler
     */
    public static void swingMainHand(LocalPlayer player) {
        //? if <26.3 {
        player.swing(InteractionHand.MAIN_HAND);
        //?} else
        /*player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);*/
    }

    /**
     * Legt einen einzelnen Gegenstand ab, wie es Minecraft beim Druck auf die Ablegen-Taste tut.
     *
     * @param client die Client-Instanz, {@code player} und {@code gameMode} muessen gesetzt sein
     */
    public static void dropOne(Minecraft client) {
        //? if <26.3 {
        if (client.player.drop(false)) {
            client.player.swing(InteractionHand.MAIN_HAND);
        }
        //?} else
        /*client.gameMode.dropItem(client.player, false);*/
    }

    /**
     * Prueft, ob eine Maustaste physisch gedrueckt ist.
     *
     * @param client die Client-Instanz
     * @param button die Maustaste im Zaehlschema von {@code InputConstants.MOUSE_BUTTON_*}
     * @return {@code true}, wenn die Taste gedrueckt ist
     */
    public static boolean isMouseButtonDown(Minecraft client, int button) {
        //? if <26.3 {
        return GLFW.glfwGetMouseButton(client.getWindow().handle(), button) == GLFW.GLFW_PRESS;
        //?} else
        /*return (SDLMouse.nSDL_GetMouseState(0L, 0L) & (1 << (button - 1))) != 0;*/
    }

    /**
     * Prueft, ob eine Tastatur-Taste physisch gedrueckt ist.
     *
     * @param client die Client-Instanz
     * @param key    die Taste einer Tastenbelegung
     * @return {@code true}, wenn die Taste gedrueckt ist; {@code null}, wenn sie sich nicht
     *         direkt abfragen laesst
     */
    public static Boolean isKeyboardKeyDown(Minecraft client, InputConstants.Key key) {
        //? if <26.3 {
        if (key.getType() != InputConstants.Type.KEYSYM) return null;
        return InputConstants.isKeyDown(client.getWindow(), key.getValue());
        //?} else
        /*return key.getType() == InputConstants.Type.KEYBOARD ? InputConstants.isKeyDown(key.getValue()) : null;*/
    }
}

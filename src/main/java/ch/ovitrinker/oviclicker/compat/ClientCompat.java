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
 * Bundles the few places where the Minecraft API differs between the target versions.
 *
 * <p>With 26.2, screen management moved from {@code Minecraft} into the class
 * {@code net.minecraft.client.gui.Gui}. In 1.21.11 and 26.1.x it still lives directly on
 * {@code Minecraft}. The case distinction is done with Stonecutter, so each version compiles
 * exactly the call it knows.</p>
 *
 * <p>With 26.3, Minecraft replaced GLFW with SDL3. Since then key binds store scancodes,
 * mouse buttons are polled from SDL, and {@code swing}/{@code drop} have new signatures.</p>
 */
public final class ClientCompat {

    private ClientCompat() {
    }

    /**
     * Returns the currently open screen.
     *
     * @param client the client instance
     * @return the open screen, or {@code null} if none is open
     */
    public static Screen getCurrentScreen(Minecraft client) {
        //? if <26.2 {
        /*return client.screen;
        *///?} else
        return client.gui.screen();
    }

    /**
     * Opens a screen.
     *
     * @param client the client instance
     * @param screen the screen to open, {@code null} closes the current one
     */
    public static void openScreen(Minecraft client, Screen screen) {
        //? if <26.2 {
        /*client.setScreen(screen);
        *///?} else
        client.gui.setScreen(screen);
    }

    /**
     * Swings the main hand, as Minecraft does after an attack.
     *
     * @param player the local player
     */
    public static void swingMainHand(LocalPlayer player) {
        //? if <26.3 {
        player.swing(InteractionHand.MAIN_HAND);
        //?} else
        /*player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);*/
    }

    /**
     * Drops a single item, as Minecraft does when the drop key is pressed.
     *
     * @param client the client instance, {@code player} and {@code gameMode} must be set
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
     * Checks whether a mouse button is physically pressed.
     *
     * @param client the client instance
     * @param button the mouse button in the numbering of {@code InputConstants.MOUSE_BUTTON_*}
     * @return {@code true} if the button is pressed
     */
    public static boolean isMouseButtonDown(Minecraft client, int button) {
        //? if <26.3 {
        return GLFW.glfwGetMouseButton(client.getWindow().handle(), button) == GLFW.GLFW_PRESS;
        //?} else
        /*return (SDLMouse.nSDL_GetMouseState(0L, 0L) & (1 << (button - 1))) != 0;*/
    }

    /**
     * Checks whether a keyboard key is physically pressed.
     *
     * @param client the client instance
     * @param key    the key of a key bind
     * @return {@code true} if the key is pressed; {@code null} if it can't be polled
     *         directly
     */
    public static Boolean isKeyboardKeyDown(Minecraft client, InputConstants.Key key) {
        //? if <26.3 {
        if (key.getType() != InputConstants.Type.KEYSYM) return null;
        return InputConstants.isKeyDown(client.getWindow(), key.getValue());
        //?} else
        /*return key.getType() == InputConstants.Type.KEYBOARD ? InputConstants.isKeyDown(key.getValue()) : null;*/
    }
}

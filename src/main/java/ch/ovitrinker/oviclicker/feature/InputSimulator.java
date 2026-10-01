package ch.ovitrinker.oviclicker.feature;

import ch.ovitrinker.oviclicker.KeybindManager;
import ch.ovitrinker.oviclicker.compat.ClientCompat;
import ch.ovitrinker.oviclicker.mixin.MinecraftAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Simulates local input.
 *
 * <p>Only the state of vanilla key binds is set or a vanilla method is called. No network
 * packets are created and no server logic is bypassed: the game processes the simulated
 * input exactly like real input.</p>
 *
 * <p>At most one key is pressed at a time. When tapping, it is released automatically after
 * the configured number of ticks; in hold mode only once the conditions are no longer met or
 * the mod is turned off.</p>
 */
public final class InputSimulator {

    /** Key bind currently held down, {@code null} if none. */
    private static KeyMapping heldMapping = null;

    /** Remaining ticks until a tapped key is released again. */
    private static int releaseCountdown = 0;

    private InputSimulator() {
    }

    /**
     * Counts down the hold duration of a key press. Must be called every client tick so tapped
     * keys are reliably released again.
     */
    public static void tick() {
        if (releaseCountdown > 0 && --releaseCountdown <= 0) {
            release();
        }
    }

    /**
     * Triggers an action once.
     *
     * @param client         the client instance
     * @param action         the action to trigger
     * @param durationTicks  number of ticks a key stays pressed (at least 1)
     * @param screenOpen     {@code true} if a screen is currently open
     */
    public static void tap(Minecraft client, ClickAction action, int durationTicks, boolean screenOpen) {
        if (client == null || action == null) return;

        // Attack and use go through the vanilla methods, so reach, cooldown and animation
        // match the vanilla logic exactly.
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
            // While a screen is open, Minecraft doesn't evaluate click counters. Counted
            // clicks would pile up and fire all at once on close. So dropping is executed
            // directly, the same way Minecraft does when the drop key is pressed.
            if (action == ClickAction.DROP) drop(client);
        } else {
            // Actions like dropping items evaluate the number of clicks, not the held state.
            // That only works through the key that is actually bound.
            InputConstants.Key key = KeybindManager.boundKeyOf(mapping);
            if (key != null && !key.equals(InputConstants.UNKNOWN)) {
                KeyMapping.click(key);
            }
        }

        heldMapping = mapping;
        releaseCountdown = Math.max(1, durationTicks);
    }

    /**
     * Holds the key of an action down. Repeated calls keep holding it.
     *
     * @param client     the client instance
     * @param action     the action to hold
     * @param screenOpen {@code true} if a screen is currently open
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

        // While a screen is open, Minecraft skips its own key handling. Movement keys still
        // work because the player reads the held state directly. Attack and use, however, go
        // through the skipped handling and are triggered here in exactly the same way.
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
     * Drops an item, exactly like Minecraft does when the drop key is pressed.
     *
     * @param client the client instance
     */
    private static void drop(Minecraft client) {
        if (client.player == null || client.gameMode == null || client.player.isSpectator()) return;
        ClientCompat.dropOne(client);
    }

    /**
     * Releases a key that may be held. Also called when the mod is turned off, the game is
     * paused and the world is left, so no key gets stuck.
     */
    public static void release() {
        if (heldMapping != null) {
            heldMapping.setDown(false);
            heldMapping = null;
        }
        releaseCountdown = 0;
    }

    /**
     * Returns whether the mod is currently holding a key.
     *
     * @return {@code true} if a key is held
     */
    public static boolean isHolding() {
        return heldMapping != null;
    }

    /**
     * Checks whether the key of a binding is actually pressed on the device.
     *
     * <p>The state is polled directly from the window system (GLFW, or SDL from 26.3 on) and
     * not via {@code KeyMapping.isDown()}. Otherwise the mod would read its own simulated key
     * press as player input in hold mode and keep itself alive.</p>
     *
     * @param client  the client instance
     * @param mapping the key bind to check
     * @return {@code true} if the key is physically pressed
     */
    public static boolean isPhysicallyDown(Minecraft client, KeyMapping mapping) {
        if (client == null || mapping == null) return false;

        InputConstants.Key key = KeybindManager.boundKeyOf(mapping);
        if (key == null || key.equals(InputConstants.UNKNOWN)) return false;

        if (key.getType() == InputConstants.Type.MOUSE) {
            return ClientCompat.isMouseButtonDown(client, key.getValue());
        }
        Boolean keyDown = ClientCompat.isKeyboardKeyDown(client, key);
        if (keyDown != null) return keyDown;

        // Scancode bindings can't be polled directly
        return mapping.isDown();
    }
}

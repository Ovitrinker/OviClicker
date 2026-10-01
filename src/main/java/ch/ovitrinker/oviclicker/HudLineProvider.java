package ch.ovitrinker.oviclicker;

import net.minecraft.network.chat.Component;

/**
 * Extension point for other mods that want to show an extra line in the OviClicker HUD
 * without registering a HUD element of their own.
 *
 * <p>Register via {@link HudExtensions#register(HudLineProvider)}.</p>
 */
public interface HudLineProvider {

    /**
     * Returns the line to display, or {@code null} if there is nothing to show right now.
     *
     * @return the line or {@code null}
     */
    Component extraLine();
}

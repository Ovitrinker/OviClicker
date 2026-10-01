package ch.ovitrinker.oviclicker.gui;

import net.minecraft.network.chat.Component;

/**
 * Extension point for other mods that want to hook their own section into the OviClicker
 * settings screen.
 *
 * <p>Register via {@link ScreenExtensions#register(ScreenExtension)}, usually when the
 * extending mod's client starts. OviClicker itself doesn't depend on any registered
 * extension: if the registry stays empty, the screen behaves exactly as it would without
 * this extension point.</p>
 */
public interface ScreenExtension {

    /**
     * Title of the section, shown above the hooked-in widgets.
     *
     * @return the title as a translatable component
     */
    Component sectionTitle();

    /**
     * Builds the section's widgets. Called again every time the screen is built (including
     * after a mode change).
     *
     * @param api access to the screen's scrollable area
     */
    void buildOptions(ExtensionApi api);

    /**
     * Called after the user chose "Save" in the screen.
     */
    default void onSave() {
    }

    /**
     * Called after the user chose "Cancel" in the screen.
     */
    default void onCancel() {
    }
}

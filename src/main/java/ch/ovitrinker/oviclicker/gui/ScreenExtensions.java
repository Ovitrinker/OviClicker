package ch.ovitrinker.oviclicker.gui;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Static registry of other mods' {@link ScreenExtension}s.
 *
 * <p>If the registry stays empty, {@link OviClickerScreen} behaves exactly as it would
 * without this extension point.</p>
 */
public final class ScreenExtensions {

    private static final List<ScreenExtension> REGISTRY = new CopyOnWriteArrayList<>();

    private ScreenExtensions() {
    }

    /**
     * Registers an extension. Usually when the extending mod's client starts.
     *
     * @param extension the extension to register, {@code null} is ignored
     */
    public static void register(ScreenExtension extension) {
        if (extension == null) return;
        REGISTRY.add(extension);
    }

    /**
     * Returns all registered extensions.
     *
     * @return the registered extensions
     */
    public static List<ScreenExtension> all() {
        return REGISTRY;
    }
}

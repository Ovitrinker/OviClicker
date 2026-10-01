package ch.ovitrinker.oviclicker;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Static registry of other mods' {@link HudLineProvider}s.
 *
 * <p>If the registry stays empty, {@link HudRenderer} behaves exactly as it would without
 * this extension point.</p>
 */
public final class HudExtensions {

    private static final List<HudLineProvider> REGISTRY = new CopyOnWriteArrayList<>();

    private HudExtensions() {
    }

    /**
     * Registers a line provider. Usually when the extending mod's client starts.
     *
     * @param provider the provider to register, {@code null} is ignored
     */
    public static void register(HudLineProvider provider) {
        if (provider == null) return;
        REGISTRY.add(provider);
    }

    /**
     * Returns all registered line providers.
     *
     * @return the registered providers
     */
    public static List<HudLineProvider> all() {
        return REGISTRY;
    }
}

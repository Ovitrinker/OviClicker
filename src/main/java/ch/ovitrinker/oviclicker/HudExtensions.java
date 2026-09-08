package ch.ovitrinker.oviclicker;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Statische Registry der {@link HudLineProvider} anderer Mods.
 *
 * <p>Bleibt die Registry leer, verhaelt sich {@link HudRenderer} exakt wie ohne diesen
 * Erweiterungspunkt.</p>
 */
public final class HudExtensions {

    private static final List<HudLineProvider> REGISTRY = new CopyOnWriteArrayList<>();

    private HudExtensions() {
    }

    /**
     * Registriert einen Zeilen-Anbieter. Ueblicherweise beim Start des Clients der
     * erweiternden Mod.
     *
     * @param provider der zu registrierende Anbieter, {@code null} wird ignoriert
     */
    public static void register(HudLineProvider provider) {
        if (provider == null) return;
        REGISTRY.add(provider);
    }

    /**
     * Gibt alle registrierten Zeilen-Anbieter zurueck.
     *
     * @return unveraenderliche Sicht auf die registrierten Anbieter
     */
    public static List<HudLineProvider> all() {
        return REGISTRY;
    }
}

package ch.ovitrinker.oviclicker.gui;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Statische Registry der {@link ScreenExtension}en anderer Mods.
 *
 * <p>Bleibt die Registry leer, verhaelt sich {@link OviClickerScreen} exakt wie ohne diesen
 * Erweiterungspunkt.</p>
 */
public final class ScreenExtensions {

    private static final List<ScreenExtension> REGISTRY = new CopyOnWriteArrayList<>();

    private ScreenExtensions() {
    }

    /**
     * Registriert eine Erweiterung. Ueblicherweise beim Start des Clients der erweiternden Mod.
     *
     * @param extension die zu registrierende Erweiterung, {@code null} wird ignoriert
     */
    public static void register(ScreenExtension extension) {
        if (extension == null) return;
        REGISTRY.add(extension);
    }

    /**
     * Gibt alle registrierten Erweiterungen zurueck.
     *
     * @return unveraenderliche Sicht auf die registrierten Erweiterungen
     */
    public static List<ScreenExtension> all() {
        return REGISTRY;
    }
}

package ch.ovitrinker.oviclicker.gui;

import net.minecraft.network.chat.Component;

/**
 * Erweiterungspunkt fuer andere Mods, die eine eigene Sektion in den Einstellungsbildschirm
 * des OviClickers einhaengen wollen.
 *
 * <p>Registrierung ueber {@link ScreenExtensions#register(ScreenExtension)}, ueblicherweise
 * beim Start des Clients der erweiternden Mod. Der OviClicker selbst haengt von keiner
 * registrierten Erweiterung ab: bleibt die Registry leer, verhaelt sich der Bildschirm exakt
 * wie ohne diesen Erweiterungspunkt.</p>
 */
public interface ScreenExtension {

    /**
     * Titel der Sektion, wird ueber den eingehaengten Bedienelementen angezeigt.
     *
     * @return der Titel als uebersetzbare Komponente
     */
    Component sectionTitle();

    /**
     * Baut die Bedienelemente der Sektion auf. Wird bei jedem Aufbau des Bildschirms
     * (auch nach einem Moduswechsel) erneut aufgerufen.
     *
     * @param api Zugriff auf den scrollbaren Bereich des Bildschirms
     */
    void buildOptions(ExtensionApi api);

    /**
     * Wird aufgerufen, nachdem der Nutzer im Bildschirm "Speichern" gewaehlt hat.
     */
    default void onSave() {
    }

    /**
     * Wird aufgerufen, nachdem der Nutzer im Bildschirm "Abbrechen" gewaehlt hat.
     */
    default void onCancel() {
    }
}

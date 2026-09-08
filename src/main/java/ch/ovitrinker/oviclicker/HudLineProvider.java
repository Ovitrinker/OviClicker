package ch.ovitrinker.oviclicker;

import net.minecraft.network.chat.Component;

/**
 * Erweiterungspunkt fuer andere Mods, die eine zusaetzliche Zeile im HUD des OviClickers
 * anzeigen wollen, ohne ein eigenes HUD-Element zu registrieren.
 *
 * <p>Registrierung ueber {@link HudExtensions#register(HudLineProvider)}.</p>
 */
public interface HudLineProvider {

    /**
     * Gibt die anzuzeigende Zeile zurueck, oder {@code null}, wenn gerade nichts anzuzeigen ist.
     *
     * @return die Zeile oder {@code null}
     */
    Component extraLine();
}

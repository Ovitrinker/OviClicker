package ch.ovitrinker.oviclicker.feature;

/**
 * Die drei moeglichen Betriebsmodi des OviClickers. Es ist immer genau ein Modus aktiv.
 */
public enum ClickMode {
    /** Der OviClicker ist ausgeschaltet. */
    OFF("oviclicker.mode.off"),
    /** Klickt automatisch, sobald das Fadenkreuz auf einer Entity liegt. */
    AUTOATTACK("oviclicker.mode.autoattack"),
    /** Klickt in einem festen Intervall, unabhaengig vom Ziel. */
    TIMER("oviclicker.mode.timer");

    private final String translationKey;

    ClickMode(String translationKey) {
        this.translationKey = translationKey;
    }

    /**
     * Gibt den Uebersetzungsschluessel fuer die Anzeige im GUI und HUD zurueck.
     *
     * @return Schluessel aus den Sprachdateien
     */
    public String getTranslationKey() {
        return translationKey;
    }

    /**
     * Gibt den naechsten Modus in der Reihenfolge OFF -&gt; AUTOATTACK -&gt; TIMER -&gt; OFF zurueck.
     *
     * @return der folgende Modus
     */
    public ClickMode next() {
        ClickMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /**
     * Wandelt einen gespeicherten Namen sicher in einen Modus um.
     *
     * @param name     der gespeicherte Name, darf {@code null} sein
     * @param fallback Rueckgabewert, falls der Name unbekannt oder {@code null} ist
     * @return der passende Modus oder {@code fallback}
     */
    public static ClickMode fromName(String name, ClickMode fallback) {
        if (name == null) return fallback;
        for (ClickMode mode : values()) {
            if (mode.name().equalsIgnoreCase(name)) return mode;
        }
        return fallback;
    }
}

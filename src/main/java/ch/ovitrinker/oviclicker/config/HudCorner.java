package ch.ovitrinker.oviclicker.config;

/**
 * Bildschirmecke, an der die HUD-Anzeige verankert wird.
 */
public enum HudCorner {
    /** Oben links. */
    TOP_LEFT("oviclicker.hud.corner.top_left"),
    /** Oben rechts. */
    TOP_RIGHT("oviclicker.hud.corner.top_right"),
    /** Unten links. */
    BOTTOM_LEFT("oviclicker.hud.corner.bottom_left"),
    /** Unten rechts. */
    BOTTOM_RIGHT("oviclicker.hud.corner.bottom_right");

    private final String translationKey;

    HudCorner(String translationKey) {
        this.translationKey = translationKey;
    }

    /**
     * Gibt den Uebersetzungsschluessel der Ecke zurueck.
     *
     * @return Schluessel aus den Sprachdateien
     */
    public String getTranslationKey() {
        return translationKey;
    }

    /**
     * Gibt die naechste Ecke im Durchlauf zurueck.
     *
     * @return die folgende Ecke
     */
    public HudCorner next() {
        HudCorner[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /**
     * Wandelt einen gespeicherten Namen sicher in eine Ecke um.
     *
     * @param name     gespeicherter Name, darf {@code null} sein
     * @param fallback Rueckgabewert bei unbekanntem oder fehlendem Namen
     * @return die passende Ecke oder {@code fallback}
     */
    public static HudCorner fromName(String name, HudCorner fallback) {
        if (name == null) return fallback;
        for (HudCorner corner : values()) {
            if (corner.name().equalsIgnoreCase(name)) return corner;
        }
        return fallback;
    }
}

package ch.ovitrinker.oviclicker.feature;

/**
 * The three possible OviClicker modes. Exactly one mode is always active.
 */
public enum ClickMode {
    /** OviClicker is turned off. */
    OFF("oviclicker.mode.off"),
    /** Clicks automatically as soon as the crosshair is on an entity. */
    AUTOATTACK("oviclicker.mode.autoattack"),
    /** Clicks at a fixed interval, regardless of the target. */
    TIMER("oviclicker.mode.timer");

    private final String translationKey;

    ClickMode(String translationKey) {
        this.translationKey = translationKey;
    }

    /**
     * Returns the translation key for display in GUI and HUD.
     *
     * @return key from the language files
     */
    public String getTranslationKey() {
        return translationKey;
    }

    /**
     * Returns the next mode in the order OFF -&gt; AUTOATTACK -&gt; TIMER -&gt; OFF.
     *
     * @return the following mode
     */
    public ClickMode next() {
        ClickMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /**
     * Safely converts a stored name into a mode.
     *
     * @param name     the stored name, may be {@code null}
     * @param fallback return value if the name is unknown or {@code null}
     * @return the matching mode or {@code fallback}
     */
    public static ClickMode fromName(String name, ClickMode fallback) {
        if (name == null) return fallback;
        for (ClickMode mode : values()) {
            if (mode.name().equalsIgnoreCase(name)) return mode;
        }
        return fallback;
    }
}

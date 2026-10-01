package ch.ovitrinker.oviclicker.config;

/**
 * Screen corner the HUD is anchored to.
 */
public enum HudCorner {
    /** Top left. */
    TOP_LEFT("oviclicker.hud.corner.top_left"),
    /** Top right. */
    TOP_RIGHT("oviclicker.hud.corner.top_right"),
    /** Bottom left. */
    BOTTOM_LEFT("oviclicker.hud.corner.bottom_left"),
    /** Bottom right. */
    BOTTOM_RIGHT("oviclicker.hud.corner.bottom_right");

    private final String translationKey;

    HudCorner(String translationKey) {
        this.translationKey = translationKey;
    }

    /**
     * Returns the corner's translation key.
     *
     * @return key from the language files
     */
    public String getTranslationKey() {
        return translationKey;
    }

    /**
     * Returns the next corner in the cycle.
     *
     * @return the following corner
     */
    public HudCorner next() {
        HudCorner[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /**
     * Safely converts a stored name into a corner.
     *
     * @param name     stored name, may be {@code null}
     * @param fallback return value for an unknown or missing name
     * @return the matching corner or {@code fallback}
     */
    public static HudCorner fromName(String name, HudCorner fallback) {
        if (name == null) return fallback;
        for (HudCorner corner : values()) {
            if (corner.name().equalsIgnoreCase(name)) return corner;
        }
        return fallback;
    }
}

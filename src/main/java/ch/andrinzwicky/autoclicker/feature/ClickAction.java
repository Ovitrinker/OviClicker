package ch.andrinzwicky.autoclicker.feature;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;

/**
 * Die Aktion, die der AutoClicker ausloest.
 *
 * <p>Alle Aktionen entsprechen einer Vanilla-Tastenbelegung. Der Mod setzt ausschliesslich
 * den lokalen Zustand dieser Belegung, genau so, wie es die echte Taste tun wuerde.
 * Angriff und Benutzen werden beim einzelnen Ausloesen zusaetzlich ueber die
 * Vanilla-Methoden {@code startAttack()} und {@code startUseItem()} angestossen, damit
 * Cooldown und Reichweite exakt der Vanilla-Logik folgen.</p>
 */
public enum ClickAction {
    /** Linksklick, also Angriff beziehungsweise Block abbauen. */
    ATTACK("autoclicker.action.attack"),
    /** Rechtsklick, also Gegenstand oder Block benutzen. */
    USE("autoclicker.action.use"),
    /** Springen. */
    JUMP("autoclicker.action.jump"),
    /** Vorwaerts laufen. */
    FORWARD("autoclicker.action.forward"),
    /** Rueckwaerts laufen. */
    BACK("autoclicker.action.back"),
    /** Nach links laufen. */
    LEFT("autoclicker.action.left"),
    /** Nach rechts laufen. */
    RIGHT("autoclicker.action.right"),
    /** Schleichen. */
    SNEAK("autoclicker.action.sneak"),
    /** Sprinten. */
    SPRINT("autoclicker.action.sprint"),
    /** Gegenstand ablegen. */
    DROP("autoclicker.action.drop");

    private final String translationKey;

    ClickAction(String translationKey) {
        this.translationKey = translationKey;
    }

    /**
     * Gibt den Uebersetzungsschluessel fuer GUI und HUD zurueck.
     *
     * @return Schluessel aus den Sprachdateien
     */
    public String getTranslationKey() {
        return translationKey;
    }

    /**
     * Gibt die naechste Aktion im Durchlauf zurueck.
     *
     * @return die folgende Aktion
     */
    public ClickAction next() {
        ClickAction[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /**
     * Gibt an, ob die Aktion vom Angriffs-Cooldown und der Waffenpruefung betroffen ist.
     * Das gilt nur fuer den Angriff.
     *
     * @return {@code true} beim Angriff, sonst {@code false}
     */
    public boolean isAttack() {
        return this == ATTACK;
    }

    /**
     * Ordnet der Aktion die zugehoerige Vanilla-Tastenbelegung zu. Damit wirkt die
     * Simulation genau wie ein echter Tastendruck, inklusive der vom Spieler gewaehlten
     * Belegung.
     *
     * @param options die Optionen des Clients
     * @return die passende Tastenbelegung, nie {@code null}
     */
    public KeyMapping getMapping(Options options) {
        return switch (this) {
            case ATTACK -> options.keyAttack;
            case USE -> options.keyUse;
            case JUMP -> options.keyJump;
            case FORWARD -> options.keyUp;
            case BACK -> options.keyDown;
            case LEFT -> options.keyLeft;
            case RIGHT -> options.keyRight;
            case SNEAK -> options.keyShift;
            case SPRINT -> options.keySprint;
            case DROP -> options.keyDrop;
        };
    }

    /**
     * Wandelt einen gespeicherten Namen sicher in eine Aktion um.
     *
     * @param name     der gespeicherte Name, darf {@code null} sein
     * @param fallback Rueckgabewert bei unbekanntem oder fehlendem Namen
     * @return die passende Aktion oder {@code fallback}
     */
    public static ClickAction fromName(String name, ClickAction fallback) {
        if (name == null) return fallback;
        for (ClickAction action : values()) {
            if (action.name().equalsIgnoreCase(name)) return action;
        }
        return fallback;
    }
}

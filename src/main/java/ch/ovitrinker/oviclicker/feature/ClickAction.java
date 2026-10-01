package ch.ovitrinker.oviclicker.feature;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;

/**
 * The action OviClicker triggers.
 *
 * <p>Every action corresponds to a vanilla key bind. The mod only sets the local state of that
 * binding, exactly as the real key would. For single triggers, attack and use are additionally
 * started via the vanilla methods {@code startAttack()} and {@code startUseItem()}, so cooldown
 * and reach follow the vanilla logic exactly.</p>
 */
public enum ClickAction {
    /** Left click, i.e. attack or break a block. */
    ATTACK("oviclicker.action.attack"),
    /** Right click, i.e. use an item or block. */
    USE("oviclicker.action.use"),
    /** Jump. */
    JUMP("oviclicker.action.jump"),
    /** Walk forward. */
    FORWARD("oviclicker.action.forward"),
    /** Walk backward. */
    BACK("oviclicker.action.back"),
    /** Walk left. */
    LEFT("oviclicker.action.left"),
    /** Walk right. */
    RIGHT("oviclicker.action.right"),
    /** Sneak. */
    SNEAK("oviclicker.action.sneak"),
    /** Sprint. */
    SPRINT("oviclicker.action.sprint"),
    /** Drop item. */
    DROP("oviclicker.action.drop");

    private final String translationKey;

    ClickAction(String translationKey) {
        this.translationKey = translationKey;
    }

    /**
     * Returns the translation key for GUI and HUD.
     *
     * @return key from the language files
     */
    public String getTranslationKey() {
        return translationKey;
    }

    /**
     * Returns the next action in the cycle.
     *
     * @return the following action
     */
    public ClickAction next() {
        ClickAction[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /**
     * Returns whether the action is affected by the attack cooldown and the weapon check.
     * This only applies to attacking.
     *
     * @return {@code true} for attack, otherwise {@code false}
     */
    public boolean isAttack() {
        return this == ATTACK;
    }

    /**
     * Maps the action to its vanilla key bind. That way the simulation works exactly like a
     * real key press, including the binding the player chose.
     *
     * @param options the client options
     * @return the matching key bind, never {@code null}
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
     * Safely converts a stored name into an action.
     *
     * @param name     the stored name, may be {@code null}
     * @param fallback return value for an unknown or missing name
     * @return the matching action or {@code fallback}
     */
    public static ClickAction fromName(String name, ClickAction fallback) {
        if (name == null) return fallback;
        for (ClickAction action : values()) {
            if (action.name().equalsIgnoreCase(name)) return action;
        }
        return fallback;
    }
}

package ch.ovitrinker.oviclicker.config;

import ch.ovitrinker.oviclicker.feature.ClickAction;
import ch.ovitrinker.oviclicker.feature.ClickMode;

/**
 * Data holder for all OviClicker settings.
 *
 * <p>GSON reads and writes this class directly from {@code config/oviclicker.json}. All
 * fields have default values, so missing or broken entries in the file automatically fall
 * back to the default.</p>
 *
 * <p>Mode and master toggle are deliberately saved too: OviClicker should keep running in
 * the same state after a server switch, a world switch or a client restart, without having
 * to be toggled again.</p>
 */
public class OviClickerConfig {

    // ------------------------------------------------------------------
    // General
    // ------------------------------------------------------------------

    /** Master switch. When off, the mod doesn't click regardless of the mode. */
    public boolean masterEnabled = true;

    /** Active mode. Stored as a name so unknown values can be caught. */
    public String mode = ClickMode.OFF.name();

    // ------------------------------------------------------------------
    // AUTOATTACK mode
    // ------------------------------------------------------------------

    /** Clicks per second in AUTOATTACK mode (0.1 - 20). */
    public double autoAttackCps = 8.0;

    /** Random deviation of the interval in AUTOATTACK mode, in percent (0 - 100). */
    public double autoAttackJitterPercent = 15.0;

    /** Maximum reach in blocks up to which a target is attacked (1 - 6). */
    public double maxReach = 3.0;

    /** Action triggered in AUTOATTACK mode. Stored as a name. */
    public String autoAttackAction = ClickAction.ATTACK.name();

    // ------------------------------------------------------------------
    // TIMER mode
    // ------------------------------------------------------------------

    /** Click interval in TIMER mode, in seconds (0.05 - 300). */
    public double timerIntervalSeconds = 10.0;

    /** Random deviation of the interval in TIMER mode, in percent (0 - 100). */
    public double timerJitterPercent = 0.0;

    /** Action triggered in TIMER mode. Stored as a name. */
    public String timerAction = ClickAction.ATTACK.name();

    // ------------------------------------------------------------------
    // Shared conditions
    // ------------------------------------------------------------------

    /** Only clicks while the attack key is held down. */
    public boolean onlyWhileAttackKeyHeld = false;

    /** Waits until the vanilla attack cooldown is fully charged. */
    public boolean respectAttackCooldown = true;

    /** Only clicks while a sword, axe, trident or mace is in the main hand. */
    public boolean requireWeapon = false;

    /**
     * Holds the key down instead of tapping it at an interval. Useful for movement keys
     * (auto-walk) or continuous mining. The interval is ignored.
     */
    public boolean holdInsteadOfTap = false;

    /** Number of ticks a tapped key stays pressed (1 - 20). */
    public int tapDurationTicks = 1;

    // ------------------------------------------------------------------
    // AutoEat
    // ------------------------------------------------------------------

    /**
     * Eats automatically as soon as hunger drops below the threshold. OviClicker pauses
     * while eating and resumes on its own afterwards.
     */
    public boolean autoEatEnabled = true;

    /**
     * Threshold in whole haunches (1 - 9). Eating starts as soon as hunger drops below this
     * value and continues until the hunger bar is full again.
     */
    public int autoEatThresholdHaunches = 6;

    /** Moves food from the inventory into the hotbar when there is none left there. */
    public boolean autoEatRefillFromInventory = true;

    /** Allows the regular golden apple. */
    public boolean autoEatAllowGoldenApples = false;

    /**
     * Allows the enchanted golden apple. Deliberately separate from the regular golden apple,
     * because it is far more valuable.
     */
    public boolean autoEatAllowEnchantedGoldenApples = false;

    // ------------------------------------------------------------------
    // Entity blacklist (AUTOATTACK mode only)
    // ------------------------------------------------------------------

    /** Doesn't attack players. */
    public boolean blacklistPlayers = true;

    /** Doesn't attack villagers and wandering traders. */
    public boolean blacklistVillagers = true;

    /** Doesn't attack tamed animals. */
    public boolean blacklistTamed = true;

    /** Doesn't attack passive animals. */
    public boolean blacklistPassive = false;

    // ------------------------------------------------------------------
    // HUD
    // ------------------------------------------------------------------

    /** Shows the active mode in the HUD. */
    public boolean hudEnabled = true;

    /** Screen corner of the HUD. Stored as a name. */
    public String hudCorner = HudCorner.TOP_LEFT.name();

    /** Horizontal offset of the HUD from the selected corner (0 - 200). */
    public int hudOffsetX = 4;

    /** Vertical offset of the HUD from the selected corner (0 - 200). */
    public int hudOffsetY = 4;

    /** Hides the HUD while the mode is OFF. */
    public boolean hudHideWhenOff = false;

    // ------------------------------------------------------------------
    // Access and validation
    // ------------------------------------------------------------------

    /**
     * Returns the active mode. Unknown values fall back to {@link ClickMode#OFF}.
     *
     * @return the active mode, never {@code null}
     */
    public ClickMode getMode() {
        return ClickMode.fromName(mode, ClickMode.OFF);
    }

    /**
     * Sets the active mode.
     *
     * @param value the new mode, {@code null} is treated as {@link ClickMode#OFF}
     */
    public void setMode(ClickMode value) {
        this.mode = (value == null ? ClickMode.OFF : value).name();
    }

    /**
     * Returns the action of the given mode.
     *
     * @param mode the mode, {@code null} is treated as {@link ClickMode#OFF}
     * @return the corresponding action, never {@code null}
     */
    public ClickAction getAction(ClickMode mode) {
        if (mode == ClickMode.TIMER) {
            return ClickAction.fromName(timerAction, ClickAction.ATTACK);
        }
        return ClickAction.fromName(autoAttackAction, ClickAction.ATTACK);
    }

    /**
     * Sets the action of the given mode.
     *
     * @param mode   the mode whose action is set
     * @param action the new action, {@code null} is treated as attack
     */
    public void setAction(ClickMode mode, ClickAction action) {
        String name = (action == null ? ClickAction.ATTACK : action).name();
        if (mode == ClickMode.TIMER) {
            timerAction = name;
        } else {
            autoAttackAction = name;
        }
    }

    /**
     * Returns the selected HUD corner. Unknown values fall back to top left.
     *
     * @return the HUD corner, never {@code null}
     */
    public HudCorner getHudCorner() {
        return HudCorner.fromName(hudCorner, HudCorner.TOP_LEFT);
    }

    /**
     * Sets the HUD corner.
     *
     * @param value the new corner, {@code null} is treated as top left
     */
    public void setHudCorner(HudCorner value) {
        this.hudCorner = (value == null ? HudCorner.TOP_LEFT : value).name();
    }

    /**
     * Clamps all numeric values to their valid range and repairs unknown enum values. Called
     * after loading and before saving, so a hand-edited file can't put the mod into a
     * nonsensical state.
     */
    public void clamp() {
        autoAttackCps = clampDouble(autoAttackCps, 0.1, 20.0, 8.0);
        autoAttackJitterPercent = clampDouble(autoAttackJitterPercent, 0.0, 100.0, 15.0);
        maxReach = clampDouble(maxReach, 1.0, 6.0, 3.0);
        timerIntervalSeconds = clampDouble(timerIntervalSeconds, 0.05, 300.0, 10.0);
        timerJitterPercent = clampDouble(timerJitterPercent, 0.0, 100.0, 0.0);
        hudOffsetX = clampInt(hudOffsetX, 0, 200, 4);
        hudOffsetY = clampInt(hudOffsetY, 0, 200, 4);
        tapDurationTicks = clampInt(tapDurationTicks, 1, 20, 1);
        autoEatThresholdHaunches = clampInt(autoEatThresholdHaunches, 1, 9, 6);

        // Repairs unknown or missing enum names
        setMode(getMode());
        setHudCorner(getHudCorner());
        setAction(ClickMode.AUTOATTACK, getAction(ClickMode.AUTOATTACK));
        setAction(ClickMode.TIMER, getAction(ClickMode.TIMER));
    }

    /**
     * Creates an independent copy of these settings.
     *
     * @return a copy with identical values
     */
    public OviClickerConfig copy() {
        OviClickerConfig copy = new OviClickerConfig();
        copy.masterEnabled = masterEnabled;
        copy.mode = mode;
        copy.autoAttackCps = autoAttackCps;
        copy.autoAttackJitterPercent = autoAttackJitterPercent;
        copy.maxReach = maxReach;
        copy.autoAttackAction = autoAttackAction;
        copy.timerIntervalSeconds = timerIntervalSeconds;
        copy.timerJitterPercent = timerJitterPercent;
        copy.timerAction = timerAction;
        copy.onlyWhileAttackKeyHeld = onlyWhileAttackKeyHeld;
        copy.respectAttackCooldown = respectAttackCooldown;
        copy.requireWeapon = requireWeapon;
        copy.holdInsteadOfTap = holdInsteadOfTap;
        copy.tapDurationTicks = tapDurationTicks;
        copy.autoEatEnabled = autoEatEnabled;
        copy.autoEatThresholdHaunches = autoEatThresholdHaunches;
        copy.autoEatRefillFromInventory = autoEatRefillFromInventory;
        copy.autoEatAllowGoldenApples = autoEatAllowGoldenApples;
        copy.autoEatAllowEnchantedGoldenApples = autoEatAllowEnchantedGoldenApples;
        copy.blacklistPlayers = blacklistPlayers;
        copy.blacklistVillagers = blacklistVillagers;
        copy.blacklistTamed = blacklistTamed;
        copy.blacklistPassive = blacklistPassive;
        copy.hudEnabled = hudEnabled;
        copy.hudCorner = hudCorner;
        copy.hudOffsetX = hudOffsetX;
        copy.hudOffsetY = hudOffsetY;
        copy.hudHideWhenOff = hudHideWhenOff;
        return copy;
    }

    /**
     * Takes over all values from another instance.
     *
     * @param other source of the values, {@code null} is ignored
     */
    public void copyFrom(OviClickerConfig other) {
        if (other == null) return;
        masterEnabled = other.masterEnabled;
        mode = other.mode;
        autoAttackCps = other.autoAttackCps;
        autoAttackJitterPercent = other.autoAttackJitterPercent;
        maxReach = other.maxReach;
        autoAttackAction = other.autoAttackAction;
        timerIntervalSeconds = other.timerIntervalSeconds;
        timerJitterPercent = other.timerJitterPercent;
        timerAction = other.timerAction;
        onlyWhileAttackKeyHeld = other.onlyWhileAttackKeyHeld;
        respectAttackCooldown = other.respectAttackCooldown;
        requireWeapon = other.requireWeapon;
        holdInsteadOfTap = other.holdInsteadOfTap;
        tapDurationTicks = other.tapDurationTicks;
        autoEatEnabled = other.autoEatEnabled;
        autoEatThresholdHaunches = other.autoEatThresholdHaunches;
        autoEatRefillFromInventory = other.autoEatRefillFromInventory;
        autoEatAllowGoldenApples = other.autoEatAllowGoldenApples;
        autoEatAllowEnchantedGoldenApples = other.autoEatAllowEnchantedGoldenApples;
        blacklistPlayers = other.blacklistPlayers;
        blacklistVillagers = other.blacklistVillagers;
        blacklistTamed = other.blacklistTamed;
        blacklistPassive = other.blacklistPassive;
        hudEnabled = other.hudEnabled;
        hudCorner = other.hudCorner;
        hudOffsetX = other.hudOffsetX;
        hudOffsetY = other.hudOffsetY;
        hudHideWhenOff = other.hudHideWhenOff;
    }

    /**
     * Clamps a floating-point value and catches {@code NaN} and infinity.
     *
     * @param value        the value to check
     * @param min          lower bound
     * @param max          upper bound
     * @param defaultValue fallback for {@code NaN} or infinity
     * @return the clamped value
     */
    private static double clampDouble(double value, double min, double max, double defaultValue) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return defaultValue;
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamps an integer value.
     *
     * @param value        the value to check
     * @param min          lower bound
     * @param max          upper bound
     * @param defaultValue currently unused, kept for consistency
     * @return the clamped value
     */
    private static int clampInt(int value, int min, int max, int defaultValue) {
        return Math.max(min, Math.min(max, value));
    }
}

package ch.andrinzwicky.autoclicker.config;

import ch.andrinzwicky.autoclicker.feature.ClickAction;
import ch.andrinzwicky.autoclicker.feature.ClickMode;

/**
 * Datenhalter aller Einstellungen des AutoClickers.
 *
 * <p>Die Klasse wird von GSON direkt aus {@code config/autoclicker.json} gelesen und
 * geschrieben. Alle Felder sind mit Standardwerten vorbelegt, damit fehlende oder
 * defekte Eintraege in der Datei automatisch auf den Standard zurueckfallen.</p>
 *
 * <p>Modus und Master-Toggle werden bewusst mitgespeichert: der AutoClicker soll nach
 * einem Server-Wechsel, einem Weltwechsel oder einem Client-Neustart im gleichen
 * Zustand weiterlaufen, ohne dass erneut umgeschaltet werden muss.</p>
 */
public class AutoClickerConfig {

    // ------------------------------------------------------------------
    // Allgemein
    // ------------------------------------------------------------------

    /** Master-Schalter. Ist er aus, klickt der Mod unabhaengig vom Modus nicht. */
    public boolean masterEnabled = true;

    /** Aktiver Modus. Wird als Name gespeichert, damit unbekannte Werte abgefangen werden koennen. */
    public String mode = ClickMode.OFF.name();

    // ------------------------------------------------------------------
    // Modus AUTOATTACK
    // ------------------------------------------------------------------

    /** Klicks pro Sekunde im Modus AUTOATTACK (0.1 - 20). */
    public double autoAttackCps = 8.0;

    /** Zufaellige Abweichung des Intervalls im Modus AUTOATTACK in Prozent (0 - 100). */
    public double autoAttackJitterPercent = 15.0;

    /** Maximale Reichweite in Bloecken, bis zu der ein Ziel angegriffen wird (1 - 6). */
    public double maxReach = 3.0;

    /** Aktion, die im Modus AUTOATTACK ausgeloest wird. Wird als Name gespeichert. */
    public String autoAttackAction = ClickAction.ATTACK.name();

    // ------------------------------------------------------------------
    // Modus TIMER
    // ------------------------------------------------------------------

    /** Klickintervall im Modus TIMER in Sekunden (0.05 - 300). */
    public double timerIntervalSeconds = 10.0;

    /** Zufaellige Abweichung des Intervalls im Modus TIMER in Prozent (0 - 100). */
    public double timerJitterPercent = 0.0;

    /** Aktion, die im Modus TIMER ausgeloest wird. Wird als Name gespeichert. */
    public String timerAction = ClickAction.ATTACK.name();

    // ------------------------------------------------------------------
    // Gemeinsame Bedingungen
    // ------------------------------------------------------------------

    /** Klickt nur, solange die Angriffstaste gedrueckt gehalten wird. */
    public boolean onlyWhileAttackKeyHeld = false;

    /** Wartet, bis der Angriffs-Cooldown der Vanilla-Mechanik voll aufgeladen ist. */
    public boolean respectAttackCooldown = true;

    /** Klickt nur, wenn ein Schwert, eine Axt oder ein Dreizack in der Haupthand liegt. */
    public boolean requireWeapon = false;

    /**
     * Haelt die Taste gedrueckt, statt sie im Intervall anzutippen. Sinnvoll fuer
     * Bewegungstasten (Autolauf) oder zum Dauerabbauen. Das Intervall wird dabei ignoriert.
     */
    public boolean holdInsteadOfTap = false;

    /** Anzahl Ticks, die eine angetippte Taste gedrueckt bleibt (1 - 20). */
    public int tapDurationTicks = 1;

    // ------------------------------------------------------------------
    // AutoEat
    // ------------------------------------------------------------------

    /**
     * Isst automatisch, sobald der Hunger unter die Schwelle faellt. Waehrend des Essens
     * pausiert der AutoClicker, danach laeuft er von selbst weiter.
     */
    public boolean autoEatEnabled = true;

    /**
     * Schwelle in ganzen Hungerkeulen (1 - 9). Gegessen wird, sobald der Hunger unter
     * diesen Wert faellt, und zwar so lange, bis die Hungerleiste wieder voll ist.
     */
    public int autoEatThresholdHaunches = 6;

    /** Holt Essen aus dem Inventar in die Hotbar, wenn dort keines mehr liegt. */
    public boolean autoEatRefillFromInventory = true;

    /** Erlaubt den gewoehnlichen goldenen Apfel. */
    public boolean autoEatAllowGoldenApples = false;

    /**
     * Erlaubt den verzauberten goldenen Apfel. Steht bewusst getrennt vom gewoehnlichen
     * goldenen Apfel, weil er ungleich wertvoller ist.
     */
    public boolean autoEatAllowEnchantedGoldenApples = false;

    // ------------------------------------------------------------------
    // Entity-Blacklist (nur Modus AUTOATTACK)
    // ------------------------------------------------------------------

    /** Greift keine Spieler an. */
    public boolean blacklistPlayers = true;

    /** Greift keine Dorfbewohner und fahrenden Haendler an. */
    public boolean blacklistVillagers = true;

    /** Greift keine gezaehmten Tiere an. */
    public boolean blacklistTamed = true;

    /** Greift keine friedlichen Tiere an. */
    public boolean blacklistPassive = false;

    // ------------------------------------------------------------------
    // HUD
    // ------------------------------------------------------------------

    /** Zeigt den aktiven Modus im HUD an. */
    public boolean hudEnabled = true;

    /** Bildschirmecke der HUD-Anzeige. Wird als Name gespeichert. */
    public String hudCorner = HudCorner.TOP_LEFT.name();

    /** Waagrechter Abstand der HUD-Anzeige zur gewaehlten Ecke (0 - 200). */
    public int hudOffsetX = 4;

    /** Senkrechter Abstand der HUD-Anzeige zur gewaehlten Ecke (0 - 200). */
    public int hudOffsetY = 4;

    /** Blendet die HUD-Anzeige aus, solange der Modus OFF ist. */
    public boolean hudHideWhenOff = false;

    // ------------------------------------------------------------------
    // Zugriff und Pruefung
    // ------------------------------------------------------------------

    /**
     * Gibt den aktiven Modus zurueck. Unbekannte Werte fallen auf {@link ClickMode#OFF} zurueck.
     *
     * @return der aktive Modus, nie {@code null}
     */
    public ClickMode getMode() {
        return ClickMode.fromName(mode, ClickMode.OFF);
    }

    /**
     * Setzt den aktiven Modus.
     *
     * @param value der neue Modus, {@code null} wird als {@link ClickMode#OFF} behandelt
     */
    public void setMode(ClickMode value) {
        this.mode = (value == null ? ClickMode.OFF : value).name();
    }

    /**
     * Gibt die Aktion des angegebenen Modus zurueck.
     *
     * @param mode der Modus, {@code null} wird als {@link ClickMode#OFF} behandelt
     * @return die zugehoerige Aktion, nie {@code null}
     */
    public ClickAction getAction(ClickMode mode) {
        if (mode == ClickMode.TIMER) {
            return ClickAction.fromName(timerAction, ClickAction.ATTACK);
        }
        return ClickAction.fromName(autoAttackAction, ClickAction.ATTACK);
    }

    /**
     * Setzt die Aktion des angegebenen Modus.
     *
     * @param mode   der Modus, dessen Aktion gesetzt wird
     * @param action die neue Aktion, {@code null} wird als Angriff behandelt
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
     * Gibt die gewaehlte HUD-Ecke zurueck. Unbekannte Werte fallen auf oben links zurueck.
     *
     * @return die HUD-Ecke, nie {@code null}
     */
    public HudCorner getHudCorner() {
        return HudCorner.fromName(hudCorner, HudCorner.TOP_LEFT);
    }

    /**
     * Setzt die HUD-Ecke.
     *
     * @param value die neue Ecke, {@code null} wird als oben links behandelt
     */
    public void setHudCorner(HudCorner value) {
        this.hudCorner = (value == null ? HudCorner.TOP_LEFT : value).name();
    }

    /**
     * Begrenzt alle Zahlenwerte auf ihren gueltigen Bereich und repariert unbekannte
     * Aufzaehlungswerte. Wird nach dem Laden und vor dem Speichern aufgerufen, damit eine
     * von Hand bearbeitete Datei den Mod nicht in einen unsinnigen Zustand bringt.
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

        // Repariert unbekannte oder fehlende Namen der Aufzaehlungen
        setMode(getMode());
        setHudCorner(getHudCorner());
        setAction(ClickMode.AUTOATTACK, getAction(ClickMode.AUTOATTACK));
        setAction(ClickMode.TIMER, getAction(ClickMode.TIMER));
    }

    /**
     * Erstellt eine unabhaengige Kopie dieser Einstellungen.
     *
     * @return eine Kopie mit identischen Werten
     */
    public AutoClickerConfig copy() {
        AutoClickerConfig copy = new AutoClickerConfig();
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
     * Uebernimmt alle Werte aus einer anderen Instanz.
     *
     * @param other Quelle der Werte, {@code null} wird ignoriert
     */
    public void copyFrom(AutoClickerConfig other) {
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
     * Begrenzt einen Gleitkommawert und faengt {@code NaN} sowie Unendlich ab.
     *
     * @param value        der zu pruefende Wert
     * @param min          untere Grenze
     * @param max          obere Grenze
     * @param defaultValue Ersatzwert bei {@code NaN} oder Unendlich
     * @return der begrenzte Wert
     */
    private static double clampDouble(double value, double min, double max, double defaultValue) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return defaultValue;
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Begrenzt einen Ganzzahlwert.
     *
     * @param value        der zu pruefende Wert
     * @param min          untere Grenze
     * @param max          obere Grenze
     * @param defaultValue derzeit ungenutzt, dient der Einheitlichkeit
     * @return der begrenzte Wert
     */
    private static int clampInt(int value, int min, int max, int defaultValue) {
        return Math.max(min, Math.min(max, value));
    }
}

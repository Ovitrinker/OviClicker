package ch.andrinzwicky.autoclicker.feature;

import ch.andrinzwicky.autoclicker.compat.ClientCompat;
import ch.andrinzwicky.autoclicker.config.AutoClickerConfig;
import ch.andrinzwicky.autoclicker.config.ConfigManager;
import ch.andrinzwicky.autoclicker.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Random;

/**
 * Die eigentliche Klick-Logik. Sie laeuft ausschliesslich im Client-Tick und niemals in
 * einem eigenen Thread.
 *
 * <p>Welche Aktion ausgeloest wird, legt die Einstellung pro Modus fest: Linksklick,
 * Rechtsklick oder eine der Bewegungstasten. Im Halte-Modus bleibt die Taste gedrueckt,
 * solange alle Bedingungen erfuellt sind, sonst wird sie im eingestellten Intervall
 * angetippt.</p>
 *
 * <p>Offene Bildschirme halten den AutoClicker nicht an: er laeuft im Esc-Menue, im Inventar
 * und auch dann weiter, wenn das Fenster den Fokus verliert, weil du in einem anderen
 * Programm bist. Minecraft ueberspringt in diesen Faellen seine eigene Tastenverarbeitung,
 * deshalb stoesst der Mod Angriff und Benutzen selbst an und fuehrt die Angriffs-Sperrzeit
 * selbst weiter. Nur wenn Minecraft das Spiel wirklich anhaelt – im Einzelspieler, sobald
 * ein Bildschirm offen ist –, pausiert auch der AutoClicker, denn dann laeuft die Welt
 * nicht.</p>
 *
 * <p>Der Zustand (Modus und Master-Schalter) liegt in der Konfiguration und wird beim
 * Verlassen eines Servers bewusst nicht zurueckgesetzt. Wer den Server verlaesst und
 * wieder beitritt, findet den AutoClicker unveraendert aktiv vor; lediglich der
 * Intervall-Timer startet frisch, damit nach dem Beitritt kein Klick-Stau entsteht.</p>
 */
public final class AutoClickerEngine {

    /** Zufallsgenerator fuer den Jitter. */
    private static final Random RANDOM = new Random();

    /** Zeitpunkt des naechsten erlaubten Klicks in Millisekunden. */
    private static long nextClickAtMs = 0L;

    /** Merkt sich, ob im letzten Tick eine Welt geladen war. */
    private static boolean inWorld = false;

    /** Selbst weitergefuehrte Angriffs-Sperrzeit, solange ein Bildschirm offen ist. */
    private static int missTime = 0;

    private AutoClickerEngine() {
    }

    /**
     * Wird am Ende jedes Client-Ticks aufgerufen und loest bei Bedarf eine Aktion aus.
     *
     * @param client die Client-Instanz, darf {@code null} sein
     */
    public static void onEndClientTick(Minecraft client) {
        if (client == null) return;

        // Laesst angetippte Tasten nach Ablauf ihrer Haltedauer wieder los
        InputSimulator.tick();

        AutoClickerConfig config = ConfigManager.get();

        // Ohne Spieler, Welt oder Interaktionsmanager gibt es nichts zu tun.
        // Der gewaehlte Modus bleibt dabei erhalten.
        if (client.player == null || client.level == null || client.gameMode == null) {
            InputSimulator.release();
            inWorld = false;
            return;
        }

        // Erster Tick nach dem (Wieder-)Betreten einer Welt: Timer neu ansetzen
        if (!inWorld) {
            inWorld = true;
            InputSimulator.release();
            resetTimer();
            return;
        }

        // Im Einzelspieler haelt Minecraft das ganze Spiel an, sobald ein Bildschirm offen
        // ist oder das Fenster den Fokus verliert. Dann tickt weder die Welt noch der
        // Server, es gibt also nichts auszuloesen; ein Klick wuerde nur in der Warteschlange
        // liegen und beim Fortsetzen nachgeholt. Der Timer startet deshalb frisch.
        if (client.isPaused()) {
            InputSimulator.release();
            resetTimer();
            return;
        }

        ClickMode mode = config.getMode();
        ClickAction action = config.getAction(mode);

        boolean screenOpen = ClientCompat.getCurrentScreen(client) != null;
        trackMissTime(client, screenOpen, config.masterEnabled && mode != ClickMode.OFF);

        if (!isAllowed(client, config, mode, action)) {
            InputSimulator.release();
            return;
        }

        // Halte-Modus: Taste bleibt gedrueckt, das Intervall spielt keine Rolle
        if (config.holdInsteadOfTap) {
            InputSimulator.hold(client, action, screenOpen);
        } else if (System.currentTimeMillis() >= nextClickAtMs) {
            InputSimulator.tap(client, action, config.tapDurationTicks, screenOpen);
            resetTimer();
        }

        // Ein Fehlschlag setzt die Sperrzeit neu. Bei offenem Bildschirm muss der Mod sie
        // uebernehmen, weil Minecraft den Wert im naechsten Tick wieder ueberschreibt.
        if (screenOpen) {
            missTime = ((MinecraftAccessor) (Object) client).autoclicker$getMissTime();
        }
    }

    /**
     * Fuehrt die Angriffs-Sperrzeit ueber offene Bildschirme hinweg weiter.
     *
     * <p>Minecraft setzt {@code missTime} in jedem Tick auf 10000, solange ein Bildschirm
     * offen ist, und blockiert damit jeden Angriff. Weil der AutoClicker auch im Esc-Menue
     * weiterlaeuft, zaehlt der Mod den echten Wert selbst herunter und schreibt ihn zurueck.
     * Ausserhalb von Bildschirmen wird nur mitgelesen.</p>
     *
     * <p>Zurueckgeschrieben wird ausschliesslich, solange der Mod tatsaechlich ausloesen
     * kann. Ist er abgeschaltet oder im Modus OFF, bleibt der Vanilla-Wert unberuehrt: er
     * verhindert unter anderem, dass ein noch gedrueckter Angriff nach dem Schliessen eines
     * Bildschirms sofort weiter abbaut.</p>
     *
     * @param client     die Client-Instanz
     * @param screenOpen {@code true}, wenn gerade ein Bildschirm offen ist
     * @param active     {@code true}, wenn der Mod eingeschaltet und nicht im Modus OFF ist
     */
    private static void trackMissTime(Minecraft client, boolean screenOpen, boolean active) {
        MinecraftAccessor accessor = (MinecraftAccessor) (Object) client;

        if (!screenOpen) {
            missTime = accessor.autoclicker$getMissTime();
            return;
        }

        if (missTime > 0) missTime--;
        if (active) accessor.autoclicker$setMissTime(missTime);
    }

    /**
     * Prueft alle Bedingungen, unter denen der Mod ueberhaupt ausloesen darf.
     *
     * @param client die Client-Instanz
     * @param config die aktiven Einstellungen
     * @param mode   der aktive Modus
     * @param action die eingestellte Aktion
     * @return {@code true}, wenn ausgeloest werden darf
     */
    private static boolean isAllowed(Minecraft client, AutoClickerConfig config,
                                     ClickMode mode, ClickAction action) {
        if (!config.masterEnabled) return false;
        if (mode == ClickMode.OFF) return false;

        // Waehrend des automatischen Essens wird nicht geschlagen
        if (AutoEatHandler.isEating()) return false;

        if (config.onlyWhileAttackKeyHeld
                && !InputSimulator.isPhysicallyDown(client, client.options.keyAttack)) {
            return false;
        }

        if (config.requireWeapon && !isWeapon(client.player.getMainHandItem())) return false;

        // Cooldown und Sperrzeit betreffen nur den Angriff
        if (action.isAttack()) {
            if (((MinecraftAccessor) (Object) client).autoclicker$getMissTime() > 0) return false;
            if (config.respectAttackCooldown && client.player.getAttackStrengthScale(0.0F) < 1.0F) {
                return false;
            }
        }

        return mode != ClickMode.AUTOATTACK || hasValidTarget(client, config);
    }

    /**
     * Setzt den Intervall-Timer neu. Wird nach jedem Ausloesen, nach dem Betreten einer Welt
     * und nach jeder Aenderung der Einstellungen aufgerufen.
     */
    public static void resetTimer() {
        AutoClickerConfig config = ConfigManager.get();

        double baseMs;
        double jitterPercent;

        if (config.getMode() == ClickMode.TIMER) {
            baseMs = config.timerIntervalSeconds * 1000.0;
            jitterPercent = config.timerJitterPercent;
        } else {
            baseMs = 1000.0 / Math.max(0.1, config.autoAttackCps);
            jitterPercent = config.autoAttackJitterPercent;
        }

        // Zufaellige Abweichung nach oben und unten
        double factor = 1.0 + (RANDOM.nextDouble() * 2.0 - 1.0) * (jitterPercent / 100.0);
        nextClickAtMs = System.currentTimeMillis() + Math.max(1L, Math.round(baseMs * factor));
    }

    /**
     * Prueft, ob das Fadenkreuz auf einer erlaubten Entity in Reichweite liegt.
     *
     * @param client die Client-Instanz
     * @param config die aktiven Einstellungen
     * @return {@code true}, wenn ein gueltiges Ziel anvisiert ist
     */
    private static boolean hasValidTarget(Minecraft client, AutoClickerConfig config) {
        HitResult hitResult = client.hitResult;
        if (!(hitResult instanceof EntityHitResult entityHitResult)) return false;

        Entity target = entityHitResult.getEntity();
        if (target == null || !target.isAlive() || target == client.player) return false;

        if (client.player.distanceTo(target) > config.maxReach) return false;

        return !isBlacklisted(target, config);
    }

    /**
     * Prueft, ob eine Entity durch die Blacklist ausgeschlossen ist.
     *
     * @param entity die zu pruefende Entity
     * @param config die aktiven Einstellungen
     * @return {@code true}, wenn die Entity nicht angegriffen werden darf
     */
    private static boolean isBlacklisted(Entity entity, AutoClickerConfig config) {
        if (config.blacklistPlayers && entity instanceof Player) return true;
        if (config.blacklistVillagers && entity instanceof AbstractVillager) return true;
        if (config.blacklistTamed && entity instanceof TamableAnimal tamable && tamable.isTame()) return true;
        if (config.blacklistPassive && entity instanceof Animal) return true;
        return false;
    }

    /**
     * Prueft, ob ein Gegenstand als Waffe gilt (Schwert, Axt, Dreizack oder Keule).
     *
     * @param stack der Gegenstand in der Haupthand
     * @return {@code true}, wenn es sich um eine Waffe handelt
     */
    private static boolean isWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.is(Items.TRIDENT)
                || stack.is(Items.MACE);
    }
}

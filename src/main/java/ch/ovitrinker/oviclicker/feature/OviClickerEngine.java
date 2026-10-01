package ch.ovitrinker.oviclicker.feature;

import ch.ovitrinker.oviclicker.compat.ClientCompat;
import ch.ovitrinker.oviclicker.compat.FreecamCompat;
import ch.ovitrinker.oviclicker.config.OviClickerConfig;
import ch.ovitrinker.oviclicker.config.ConfigManager;
import ch.ovitrinker.oviclicker.mixin.MinecraftAccessor;
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
 * The actual click logic. It runs exclusively in the client tick and never in a thread of
 * its own.
 *
 * <p>Which action is triggered is set per mode: left click, right click or one of the
 * movement keys. In hold mode the key stays pressed as long as all conditions are met;
 * otherwise it is tapped at the configured interval.</p>
 *
 * <p>Open screens don't stop OviClicker: it keeps running in the pause menu, in the inventory
 * and also when the window loses focus because you're in another program. Minecraft skips its
 * own key handling in these cases, so the mod triggers attack and use itself and keeps track
 * of the attack cooldown itself. Only when Minecraft really pauses the game – in singleplayer,
 * as soon as a screen is open – does OviClicker pause too, because then the world isn't
 * running.</p>
 *
 * <p>OviClicker also keeps running with the Freecam mod: while the camera is detached from the
 * player, it aims and attacks from the player, see {@link FreecamCompat}.</p>
 *
 * <p>The state (mode and master switch) lives in the config and is deliberately not reset when
 * leaving a server. Leave the server and rejoin and OviClicker is still active as before; only
 * the interval timer starts fresh, so there is no burst of clicks after joining.</p>
 */
public final class OviClickerEngine {

    /** Random generator for the jitter. */
    private static final Random RANDOM = new Random();

    /** Time of the next allowed click in milliseconds. */
    private static long nextClickAtMs = 0L;

    /** Remembers whether a world was loaded in the last tick. */
    private static boolean inWorld = false;

    /** Attack cooldown tracked by the mod itself while a screen is open. */
    private static int missTime = 0;

    private OviClickerEngine() {
    }

    /**
     * Called at the end of every client tick; triggers an action if needed.
     *
     * @param client the client instance, may be {@code null}
     */
    public static void onEndClientTick(Minecraft client) {
        if (client == null) return;

        // Releases tapped keys once their hold duration has passed
        InputSimulator.tick();

        OviClickerConfig config = ConfigManager.get();

        // Without player, world or interaction manager there's nothing to do.
        // The selected mode is kept.
        if (client.player == null || client.level == null || client.gameMode == null) {
            InputSimulator.release();
            inWorld = false;
            return;
        }

        // First tick after (re)entering a world: restart the timer
        if (!inWorld) {
            inWorld = true;
            InputSimulator.release();
            resetTimer();
            return;
        }

        // In singleplayer Minecraft pauses the whole game as soon as a screen is open or the
        // window loses focus. Then neither world nor server ticks, so there's nothing to
        // trigger; a click would just sit in the queue and fire on resume. So the timer
        // starts fresh.
        if (client.isPaused()) {
            InputSimulator.release();
            resetTimer();
            return;
        }

        ClickMode mode = config.getMode();
        ClickAction action = config.getAction(mode);

        boolean screenOpen = ClientCompat.getCurrentScreen(client) != null;
        trackMissTime(client, screenOpen, config.masterEnabled && mode != ClickMode.OFF);

        // With Freecam the crosshair aims from the camera, but OviClicker still aims from
        // the player
        boolean freecam = FreecamCompat.isActive();
        HitResult target = freecam ? FreecamCompat.pickEntityFromPlayer(client) : client.hitResult;

        if (!isAllowed(client, config, mode, action, target)) {
            InputSimulator.release();
            return;
        }

        // Hold mode: key stays pressed, the interval doesn't matter
        if (config.holdInsteadOfTap) {
            InputSimulator.hold(client, action, screenOpen);
        } else if (System.currentTimeMillis() >= nextClickAtMs) {
            if (freecam && action.isAttack()) {
                // Freecam blocks startAttack(), so the hit goes straight to the target
                if (target instanceof EntityHitResult entityHit) {
                    FreecamCompat.attack(client, entityHit.getEntity());
                }
            } else {
                InputSimulator.tap(client, action, config.tapDurationTicks, screenOpen);
            }
            resetTimer();
        }

        // A miss resets the cooldown. With a screen open the mod has to take it over,
        // because Minecraft overwrites the value again in the next tick.
        if (screenOpen) {
            missTime = ((MinecraftAccessor) (Object) client).oviclicker$getMissTime();
        }
    }

    /**
     * Keeps the attack cooldown running across open screens.
     *
     * <p>Minecraft sets {@code missTime} to 10000 every tick while a screen is open, which
     * blocks every attack. Because OviClicker keeps running in the pause menu, the mod counts
     * the real value down itself and writes it back. Outside of screens it only reads along.</p>
     *
     * <p>It only writes back while the mod can actually trigger. When it is turned off or in
     * OFF mode, the vanilla value stays untouched: among other things it prevents a still-held
     * attack from continuing to mine right after a screen is closed.</p>
     *
     * @param client     the client instance
     * @param screenOpen {@code true} if a screen is currently open
     * @param active     {@code true} if the mod is turned on and not in OFF mode
     */
    private static void trackMissTime(Minecraft client, boolean screenOpen, boolean active) {
        MinecraftAccessor accessor = (MinecraftAccessor) (Object) client;

        if (!screenOpen) {
            missTime = accessor.oviclicker$getMissTime();
            return;
        }

        if (missTime > 0) missTime--;
        if (active) accessor.oviclicker$setMissTime(missTime);
    }

    /**
     * Checks all conditions under which the mod may trigger at all.
     *
     * @param client the client instance
     * @param config the active settings
     * @param mode   the active mode
     * @param action the configured action
     * @param target the targeted object, may be {@code null}
     * @return {@code true} if it may trigger
     */
    private static boolean isAllowed(Minecraft client, OviClickerConfig config,
                                     ClickMode mode, ClickAction action, HitResult target) {
        if (!config.masterEnabled) return false;
        if (mode == ClickMode.OFF) return false;

        // No attacking while eating automatically
        if (AutoEatHandler.isEating()) return false;

        if (config.onlyWhileAttackKeyHeld
                && !InputSimulator.isPhysicallyDown(client, client.options.keyAttack)) {
            return false;
        }

        if (config.requireWeapon && !isWeapon(client.player.getMainHandItem())) return false;

        // Cooldown and miss time only affect attacking
        if (action.isAttack()) {
            if (((MinecraftAccessor) (Object) client).oviclicker$getMissTime() > 0) return false;
            if (config.respectAttackCooldown && client.player.getAttackStrengthScale(0.0F) < 1.0F) {
                return false;
            }
        }

        return mode != ClickMode.AUTOATTACK || hasValidTarget(client, config, target);
    }

    /**
     * Restarts the interval timer. Called after every trigger, after entering a world and
     * after every settings change.
     */
    public static void resetTimer() {
        OviClickerConfig config = ConfigManager.get();

        double baseMs;
        double jitterPercent;

        if (config.getMode() == ClickMode.TIMER) {
            baseMs = config.timerIntervalSeconds * 1000.0;
            jitterPercent = config.timerJitterPercent;
        } else {
            baseMs = 1000.0 / Math.max(0.1, config.autoAttackCps);
            jitterPercent = config.autoAttackJitterPercent;
        }

        // Random deviation up and down
        double factor = 1.0 + (RANDOM.nextDouble() * 2.0 - 1.0) * (jitterPercent / 100.0);
        nextClickAtMs = System.currentTimeMillis() + Math.max(1L, Math.round(baseMs * factor));
    }

    /**
     * Checks whether the target is an allowed entity within reach.
     *
     * @param client    the client instance
     * @param config    the active settings
     * @param hitResult the targeted object, may be {@code null}
     * @return {@code true} if a valid target is aimed at
     */
    private static boolean hasValidTarget(Minecraft client, OviClickerConfig config, HitResult hitResult) {
        if (!(hitResult instanceof EntityHitResult entityHitResult)) return false;

        Entity target = entityHitResult.getEntity();
        if (target == null || !target.isAlive() || target == client.player) return false;

        if (client.player.distanceTo(target) > config.maxReach) return false;

        return !isBlacklisted(target, config);
    }

    /**
     * Checks whether an entity is excluded by the blacklist.
     *
     * @param entity the entity to check
     * @param config the active settings
     * @return {@code true} if the entity must not be attacked
     */
    private static boolean isBlacklisted(Entity entity, OviClickerConfig config) {
        if (config.blacklistPlayers && entity instanceof Player) return true;
        if (config.blacklistVillagers && entity instanceof AbstractVillager) return true;
        if (config.blacklistTamed && entity instanceof TamableAnimal tamable && tamable.isTame()) return true;
        if (config.blacklistPassive && entity instanceof Animal) return true;
        return false;
    }

    /**
     * Checks whether an item counts as a weapon (sword, axe, trident or mace).
     *
     * @param stack the item in the main hand
     * @return {@code true} if it is a weapon
     */
    private static boolean isWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.is(Items.TRIDENT)
                || stack.is(Items.MACE);
    }
}

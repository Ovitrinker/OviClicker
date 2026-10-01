package ch.ovitrinker.oviclicker.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

/**
 * Interplay with the Freecam mod ({@code freecam}, package {@code net.xolt.freecam}).
 *
 * <p>While Freecam is active, the player looks through a detached camera while the player
 * character stays in place. Freecam interferes in two places that would otherwise cripple
 * OviClicker:</p>
 * <ul>
 *   <li>The crosshair target {@code Minecraft.hitResult} is calculated from the camera.
 *       AUTOATTACK therefore no longer finds a target in front of the player.</li>
 *   <li>While "Allow interaction" is off in Freecam, Freecam cancels {@code startAttack()}
 *       and {@code continueAttack()} immediately.</li>
 * </ul>
 *
 * <p>So while Freecam is active, OviClicker determines the target itself: a ray from the
 * player's eyes in their viewing direction, exactly as Minecraft does without Freecam. The
 * attack then goes through {@code MultiPlayerGameMode.attack()} and {@code swing()}, the same
 * calls {@code startAttack()} makes when it hits an entity.</p>
 *
 * <p>Freecam is not a build dependency. Its state is queried at runtime via reflection; if the
 * mod is missing or changes its interface, Freecam is simply treated as inactive.</p>
 */
public final class FreecamCompat {

    /** Freecam's mod ID. */
    private static final String MOD_ID = "freecam";

    /** Freecam's main class with the static method {@code isEnabled()}. */
    private static final String MAIN_CLASS = "net.xolt.freecam.Freecam";

    /** {@code Freecam.isEnabled()}, {@code null} if Freecam is missing or unreadable. */
    private static Method isEnabledMethod = null;

    /** Remembers whether the method has already been looked up. */
    private static boolean resolved = false;

    private FreecamCompat() {
    }

    /**
     * Returns whether Freecam is installed and currently enabled.
     *
     * @return {@code true} if the camera is currently detached from the player
     */
    public static boolean isActive() {
        if (!resolved) resolve();
        if (isEnabledMethod == null) return false;

        try {
            return (boolean) isEnabledMethod.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Interface has changed: ignore Freecam from now on
            isEnabledMethod = null;
            return false;
        }
    }

    /**
     * Looks up the method {@code Freecam.isEnabled()} once.
     */
    private static void resolve() {
        resolved = true;
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) return;

        try {
            isEnabledMethod = Class.forName(MAIN_CLASS).getMethod("isEnabled");
        } catch (ReflectiveOperationException | LinkageError e) {
            isEnabledMethod = null;
        }
    }

    /**
     * Determines the target the way Minecraft would without Freecam: from the player's eyes in
     * their own viewing direction, limited by attack reach and by blocks in the way.
     *
     * <p>The viewing direction is deliberately built from {@code getXRot()} and
     * {@code getYRot()} rather than {@code getViewVector()}: depending on its settings, Freecam
     * redirects the player's view-angle queries to the camera.</p>
     *
     * @param client the client instance
     * @return the entity hit, or {@code null} if none is targeted
     */
    public static EntityHitResult pickEntityFromPlayer(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) return null;

        double reach = player.entityInteractionRange();
        Vec3 eye = player.getEyePosition();
        Vec3 direction = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
        Vec3 end = eye.add(direction.scale(reach));

        // Blocks in the way shorten the ray, otherwise it would hit through walls
        double maxDistanceSq = reach * reach;
        HitResult blockHit = client.level.clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            maxDistanceSq = blockHit.getLocation().distanceToSqr(eye);
        }

        AABB searchBox = player.getBoundingBox().expandTowards(direction.scale(reach)).inflate(1.0);
        return ProjectileUtil.getEntityHitResult(player, eye, end, searchBox,
                entity -> !entity.isSpectator() && entity.isPickable(), maxDistanceSq);
    }

    /**
     * Attacks an entity, exactly as {@code startAttack()} does when it hits an entity.
     *
     * @param client the client instance
     * @param target the entity to attack
     */
    public static void attack(Minecraft client, Entity target) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || target == null) return;
        if (player.isHandsBusy()) return;

        client.gameMode.attack(player, target);
        ClientCompat.swingMainHand(player);
    }
}

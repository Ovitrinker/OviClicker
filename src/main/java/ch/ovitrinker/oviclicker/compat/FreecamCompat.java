package ch.ovitrinker.oviclicker.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

/**
 * Zusammenspiel mit der Freecam-Mod ({@code freecam}, Paket {@code net.xolt.freecam}).
 *
 * <p>Ist Freecam aktiv, sieht der Spieler durch eine losgeloeste Kamera, waehrend die
 * Spielfigur an Ort und Stelle stehen bleibt. Freecam greift dabei an zwei Stellen ein, die
 * den OviClicker sonst lahmlegen:</p>
 * <ul>
 *   <li>Das Fadenkreuz-Ziel {@code Minecraft.hitResult} wird von der Kamera aus berechnet.
 *       AUTOATTACK findet dadurch kein Ziel mehr vor der Spielfigur.</li>
 *   <li>Solange in Freecam „Interaktion erlauben" aus ist, bricht Freecam
 *       {@code startAttack()} und {@code continueAttack()} sofort ab.</li>
 * </ul>
 *
 * <p>Waehrend Freecam aktiv ist, bestimmt der OviClicker das Ziel deshalb selbst: ein
 * Strahl ab den Augen der Spielfigur in deren Blickrichtung, genau wie es Minecraft ohne
 * Freecam tut. Der Angriff laeuft dann ueber {@code MultiPlayerGameMode.attack()} und
 * {@code swing()}, also dieselben Aufrufe, die {@code startAttack()} bei einem getroffenen
 * Wesen macht.</p>
 *
 * <p>Freecam ist keine Build-Abhaengigkeit. Der Zustand wird zur Laufzeit per Reflection
 * abgefragt; fehlt die Mod oder aendert sie ihre Schnittstelle, gilt Freecam einfach als
 * inaktiv.</p>
 */
public final class FreecamCompat {

    /** Mod-ID von Freecam. */
    private static final String MOD_ID = "freecam";

    /** Hauptklasse von Freecam mit der statischen Methode {@code isEnabled()}. */
    private static final String MAIN_CLASS = "net.xolt.freecam.Freecam";

    /** {@code Freecam.isEnabled()}, {@code null} wenn Freecam fehlt oder nicht lesbar ist. */
    private static Method isEnabledMethod = null;

    /** Merkt sich, ob die Methode bereits gesucht wurde. */
    private static boolean resolved = false;

    private FreecamCompat() {
    }

    /**
     * Gibt an, ob Freecam installiert und gerade eingeschaltet ist.
     *
     * @return {@code true}, wenn die Kamera gerade von der Spielfigur geloest ist
     */
    public static boolean isActive() {
        if (!resolved) resolve();
        if (isEnabledMethod == null) return false;

        try {
            return (boolean) isEnabledMethod.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Schnittstelle hat sich geaendert: Freecam ab jetzt ignorieren
            isEnabledMethod = null;
            return false;
        }
    }

    /**
     * Sucht einmalig die Methode {@code Freecam.isEnabled()}.
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
     * Bestimmt das Ziel so, wie Minecraft es ohne Freecam tun wuerde: ab den Augen der
     * Spielfigur in ihrer eigenen Blickrichtung, begrenzt durch die Angriffsreichweite und
     * durch Bloecke im Weg.
     *
     * <p>Die Blickrichtung wird bewusst aus {@code getXRot()} und {@code getYRot()} gebildet
     * und nicht ueber {@code getViewVector()}: Freecam leitet die Blickwinkel-Abfragen der
     * Spielfigur je nach Einstellung auf die Kamera um.</p>
     *
     * @param client die Client-Instanz
     * @return das getroffene Wesen oder {@code null}, wenn keines anvisiert ist
     */
    public static EntityHitResult pickEntityFromPlayer(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) return null;

        double reach = player.entityInteractionRange();
        Vec3 eye = player.getEyePosition();
        Vec3 direction = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
        Vec3 end = eye.add(direction.scale(reach));

        // Bloecke im Weg verkuerzen den Strahl, sonst wuerde durch Waende geschlagen
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
     * Greift ein Wesen an, genau wie es {@code startAttack()} bei einem getroffenen Wesen tut.
     *
     * @param client die Client-Instanz
     * @param target das anzugreifende Wesen
     */
    public static void attack(Minecraft client, Entity target) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || target == null) return;
        if (player.isHandsBusy()) return;

        client.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
    }
}

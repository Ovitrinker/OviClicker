package ch.ovitrinker.oviclicker.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Access to the private input methods of {@code Minecraft}.
 *
 * <p>The mod uses them to trigger exactly the same local left click the mouse button would
 * trigger. No packets are forged and no server logic is bypassed.</p>
 *
 * <p>The names come from the official Mojang mappings and are identical in all target
 * versions (1.21.11, 26.1.x, 26.2, 26.3):
 * {@code startAttack()}, {@code continueAttack(boolean)}, {@code startUseItem()},
 * {@code missTime} and {@code rightClickDelay}.</p>
 */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {

    /**
     * Triggers a single left click (attack or start breaking a block).
     *
     * @return {@code true} if Minecraft counted the click as an attack
     */
    @Invoker("startAttack")
    boolean oviclicker$startAttack();

    /**
     * Continues holding the left mouse button, used for breaking blocks.
     *
     * @param pressed {@code true} while the button is held
     */
    @Invoker("continueAttack")
    void oviclicker$continueAttack(boolean pressed);

    /**
     * Triggers a single right click (use an item or block).
     */
    @Invoker("startUseItem")
    void oviclicker$startUseItem();

    /**
     * Reads the remaining miss cooldown in ticks.
     *
     * @return number of ticks during which Minecraft doesn't allow another attack
     */
    @Accessor("missTime")
    int oviclicker$getMissTime();

    /**
     * Sets the remaining miss cooldown.
     *
     * <p>Minecraft sets this field to 10000 while a screen is open, which blocks every attack.
     * During that time the mod keeps track of the real cooldown itself and writes it back here,
     * see {@code OviClickerEngine}.</p>
     *
     * @param value number of ticks until the next allowed attack
     */
    @Accessor("missTime")
    void oviclicker$setMissTime(int value);

    /**
     * Reads the wait time until the next automatic right click in ticks.
     *
     * @return number of ticks during which Minecraft doesn't allow another use
     */
    @Accessor("rightClickDelay")
    int oviclicker$getRightClickDelay();
}

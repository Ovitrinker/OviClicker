package ch.andrinzwicky.autoclicker.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Zugriff auf die privaten Eingabe-Methoden von {@code Minecraft}.
 *
 * <p>Der Mod loest damit exakt denselben lokalen Linksklick aus, den auch die Maustaste
 * ausloesen wuerde. Es werden keine Pakete gefaelscht und keine Serverlogik umgangen.</p>
 *
 * <p>Die Namen stammen aus den offiziellen Mojang-Mappings und sind in allen Zielversionen
 * (1.21.11, 26.1.x, 26.2) identisch:
 * {@code startAttack()}, {@code continueAttack(boolean)}, {@code startUseItem()},
 * {@code missTime} und {@code rightClickDelay}.</p>
 */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {

    /**
     * Loest einen einzelnen Linksklick aus (Angriff beziehungsweise Block-Anschlag).
     *
     * @return {@code true}, wenn Minecraft den Klick als Angriff gewertet hat
     */
    @Invoker("startAttack")
    boolean autoclicker$startAttack();

    /**
     * Setzt das Halten der linken Maustaste fort, wird fuer das Abbauen von Bloecken genutzt.
     *
     * @param pressed {@code true}, solange die Taste gehalten wird
     */
    @Invoker("continueAttack")
    void autoclicker$continueAttack(boolean pressed);

    /**
     * Loest einen einzelnen Rechtsklick aus (Gegenstand oder Block benutzen).
     */
    @Invoker("startUseItem")
    void autoclicker$startUseItem();

    /**
     * Liest die verbleibende Sperrzeit nach einem Fehlschlag in Ticks.
     *
     * @return Anzahl Ticks, in denen Minecraft keinen weiteren Angriff zulaesst
     */
    @Accessor("missTime")
    int autoclicker$getMissTime();

    /**
     * Setzt die verbleibende Sperrzeit nach einem Fehlschlag.
     *
     * <p>Minecraft setzt das Feld auf 10000, solange ein Bildschirm offen ist, und blockiert
     * damit jeden Angriff. Der Mod fuehrt die echte Sperrzeit in dieser Zeit selbst weiter
     * und schreibt sie hier zurueck, siehe {@code AutoClickerEngine}.</p>
     *
     * @param value Anzahl Ticks bis zum naechsten erlaubten Angriff
     */
    @Accessor("missTime")
    void autoclicker$setMissTime(int value);

    /**
     * Liest die Wartezeit bis zum naechsten automatischen Rechtsklick in Ticks.
     *
     * @return Anzahl Ticks, in denen Minecraft kein weiteres Benutzen zulaesst
     */
    @Accessor("rightClickDelay")
    int autoclicker$getRightClickDelay();
}

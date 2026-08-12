package ch.andrinzwicky.autoclicker.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Buendelt die wenigen Stellen, an denen sich die Minecraft-API zwischen den Zielversionen
 * unterscheidet.
 *
 * <p>Mit 26.2 ist die Screen-Verwaltung von {@code Minecraft} in die Klasse
 * {@code net.minecraft.client.gui.Gui} gewandert. In 1.21.11 und 26.1.x liegt sie noch
 * direkt auf {@code Minecraft}. Die Fallunterscheidung passiert ueber Stonecutter, damit
 * jede Version genau den Aufruf kompiliert, den sie kennt.</p>
 */
public final class ClientCompat {

    private ClientCompat() {
    }

    /**
     * Gibt den aktuell offenen Bildschirm zurueck.
     *
     * @param client die Client-Instanz
     * @return der offene Screen oder {@code null}, wenn keiner offen ist
     */
    public static Screen getCurrentScreen(Minecraft client) {
        //? if <26.2 {
        /*return client.screen;
        *///?} else
        return client.gui.screen();
    }

    /**
     * Oeffnet einen Bildschirm.
     *
     * @param client die Client-Instanz
     * @param screen der zu oeffnende Screen, {@code null} schliesst den aktuellen
     */
    public static void openScreen(Minecraft client, Screen screen) {
        //? if <26.2 {
        /*client.setScreen(screen);
        *///?} else
        client.gui.setScreen(screen);
    }
}

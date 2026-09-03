package ch.andrinzwicky.autoclicker;

import ch.andrinzwicky.autoclicker.config.AutoClickerConfig;
import ch.andrinzwicky.autoclicker.config.ConfigManager;
import ch.andrinzwicky.autoclicker.config.HudCorner;
import ch.andrinzwicky.autoclicker.feature.AutoEatHandler;
import ch.andrinzwicky.autoclicker.feature.ClickMode;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Zeichnet den aktiven Modus als kleine Textzeile ins HUD.
 *
 * <p>Registriert wird ein HUD-Element ueber {@code HudElementRegistry}. Die Schnittstelle
 * {@code HudElement} heisst in allen Zielversionen gleich, ihre Methode unterscheidet sich
 * jedoch: bis 1.21.11 zeichnet sie direkt ueber {@code GuiGraphics}, ab 26.1 sammelt sie
 * ueber {@code GuiGraphicsExtractor} einen Renderzustand ein. Da hier ein Lambda verwendet
 * wird, muss lediglich der eigentliche Zeichenaufruf versionsabhaengig sein.</p>
 */
public final class HudRenderer {

    /** Kennung des HUD-Elements. */
    private static final Identifier ELEMENT_ID =
            Identifier.fromNamespaceAndPath("autoclicker", "mode_display");

    /** Farbe fuer den ausgeschalteten Zustand (ARGB). */
    private static final int COLOUR_OFF = 0xFFAAAAAA;

    /** Farbe fuer den Modus AUTOATTACK (ARGB). */
    private static final int COLOUR_AUTOATTACK = 0xFFFF5555;

    /** Farbe fuer den Modus TIMER (ARGB). */
    private static final int COLOUR_TIMER = 0xFF55FF55;

    private HudRenderer() {
    }

    /**
     * Registriert das HUD-Element. Wird einmalig beim Start des Clients aufgerufen.
     */
    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, ELEMENT_ID,
                (graphics, tickCounter) -> {
                    Minecraft client = Minecraft.getInstance();
                    AutoClickerConfig config = ConfigManager.get();

                    if (!config.hudEnabled) return;
                    if (client == null || client.player == null || client.level == null) return;

                    ClickMode mode = config.getMode();
                    boolean off = !config.masterEnabled || mode == ClickMode.OFF;
                    if (config.hudHideWhenOff && off) return;

                    Component text = buildText(config, mode);
                    int colour = off ? COLOUR_OFF
                            : (mode == ClickMode.AUTOATTACK ? COLOUR_AUTOATTACK : COLOUR_TIMER);

                    int textWidth = client.font.width(text);
                    int screenWidth = client.getWindow().getGuiScaledWidth();
                    int screenHeight = client.getWindow().getGuiScaledHeight();

                    HudCorner corner = config.getHudCorner();
                    int x = switch (corner) {
                        case TOP_LEFT, BOTTOM_LEFT -> config.hudOffsetX;
                        case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - textWidth - config.hudOffsetX;
                    };
                    int y = switch (corner) {
                        case TOP_LEFT, TOP_RIGHT -> config.hudOffsetY;
                        case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - 9 - config.hudOffsetY;
                    };

                    //? if <26.1 {
                    /*graphics.drawString(client.font, text, x, y, colour);
                    *///?} else
                    graphics.text(client.font, text, x, y, colour);
                });
    }

    /**
     * Baut die anzuzeigende Textzeile zusammen.
     *
     * @param config die aktiven Einstellungen
     * @param mode   der aktive Modus
     * @return der fertige Text
     */
    private static Component buildText(AutoClickerConfig config, ClickMode mode) {
        Component base = config.masterEnabled
                ? Component.translatable("autoclicker.hud.mode",
                        Component.translatable(mode.getTranslationKey()))
                : Component.translatable("autoclicker.hud.disabled");

        // Waehrend des automatischen Essens ist sichtbar, warum gerade nicht geklickt wird
        if (AutoEatHandler.isEating()) {
            return Component.translatable("autoclicker.hud.eating", base);
        }

        return base;
    }
}

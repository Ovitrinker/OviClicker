package ch.ovitrinker.oviclicker;

import ch.ovitrinker.oviclicker.config.OviClickerConfig;
import ch.ovitrinker.oviclicker.config.ConfigManager;
import ch.ovitrinker.oviclicker.config.HudCorner;
import ch.ovitrinker.oviclicker.feature.AutoEatHandler;
import ch.ovitrinker.oviclicker.feature.ClickMode;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Draws the active mode as a small line of text in the HUD.
 *
 * <p>A HUD element is registered via {@code HudElementRegistry}. The {@code HudElement}
 * interface has the same name in all target versions, but its method differs: up to 1.21.11
 * it draws directly via {@code GuiGraphics}, from 26.1 on it collects a render state via
 * {@code GuiGraphicsExtractor}. Since a lambda is used here, only the actual draw call has to
 * be version-dependent.</p>
 */
public final class HudRenderer {

    /** ID of the HUD element. */
    private static final Identifier ELEMENT_ID =
            Identifier.fromNamespaceAndPath("oviclicker", "mode_display");

    /** Colour for the disabled state (ARGB). */
    private static final int COLOUR_OFF = 0xFFAAAAAA;

    /** Colour for AUTOATTACK mode (ARGB). */
    private static final int COLOUR_AUTOATTACK = 0xFFFF5555;

    /** Colour for TIMER mode (ARGB). */
    private static final int COLOUR_TIMER = 0xFF55FF55;

    private HudRenderer() {
    }

    /**
     * Registers the HUD element. Called once when the client starts.
     */
    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, ELEMENT_ID,
                (graphics, tickCounter) -> {
                    Minecraft client = Minecraft.getInstance();
                    OviClickerConfig config = ConfigManager.get();

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

                    // --- Extra lines from other mods ---
                    boolean stacksDown = corner == HudCorner.TOP_LEFT || corner == HudCorner.TOP_RIGHT;
                    int extraY = y;
                    for (HudLineProvider provider : HudExtensions.all()) {
                        Component extraLine = provider.extraLine();
                        if (extraLine == null) continue;

                        extraY += stacksDown ? 9 : -9;
                        int extraWidth = client.font.width(extraLine);
                        int extraX = switch (corner) {
                            case TOP_LEFT, BOTTOM_LEFT -> config.hudOffsetX;
                            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - extraWidth - config.hudOffsetX;
                        };

                        //? if <26.1 {
                        /*graphics.drawString(client.font, extraLine, extraX, extraY, colour);
                        *///?} else
                        graphics.text(client.font, extraLine, extraX, extraY, colour);
                    }
                });
    }

    /**
     * Builds the line of text to display.
     *
     * @param config the active settings
     * @param mode   the active mode
     * @return the finished text
     */
    private static Component buildText(OviClickerConfig config, ClickMode mode) {
        Component base = config.masterEnabled
                ? Component.translatable("oviclicker.hud.mode",
                        Component.translatable(mode.getTranslationKey()))
                : Component.translatable("oviclicker.hud.disabled");

        // While eating automatically, show why nothing is being clicked
        if (AutoEatHandler.isEating()) {
            return Component.translatable("oviclicker.hud.eating", base);
        }

        return base;
    }
}

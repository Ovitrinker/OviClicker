package ch.ovitrinker.oviclicker.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;

/**
 * Access a {@link ScreenExtension} gets while building its widgets.
 *
 * <p>Widgets submitted via {@link #addOption(AbstractWidget)} end up in the same scrollable
 * area as OviClicker's own options and are managed by its existing scroll mechanism.</p>
 */
public interface ExtensionApi {

    /**
     * Adds a widget to the screen's scrollable area.
     *
     * @param widget the widget, whose current position is used as its base position
     * @param <T>    the widget type
     * @return the same widget
     */
    <T extends AbstractWidget> T addOption(T widget);

    /**
     * Left edge of the area extensions can use for their rows.
     *
     * @return the horizontal position
     */
    int columnX();

    /**
     * Full width of the area extensions can use for their rows.
     *
     * @return the width in pixels
     */
    int columnWidth();

    /**
     * Height of a row including spacing, as OviClicker itself uses it.
     *
     * @return the row height in pixels
     */
    int rowHeight();

    /**
     * Height of a single widget, as OviClicker itself uses it.
     *
     * @return the widget height in pixels
     */
    int widgetHeight();

    /**
     * Returns the next free vertical position and then advances the internal cursor by
     * {@link #rowHeight()}.
     *
     * @return the vertical position for the next row
     */
    int nextRowY();

    /**
     * The screen's font, for widgets that need it.
     *
     * @return the font
     */
    Font font();

    /**
     * The client instance, for widgets that need it.
     *
     * @return the client instance
     */
    Minecraft minecraft();

    /**
     * Rebuilds the whole screen, including all hooked-in extensions. For changes that add or
     * remove rows, such as creating or deleting an entry.
     */
    void rebuild();
}

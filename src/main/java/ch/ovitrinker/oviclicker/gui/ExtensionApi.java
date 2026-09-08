package ch.ovitrinker.oviclicker.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;

/**
 * Zugriff, den eine {@link ScreenExtension} beim Aufbau ihrer Bedienelemente erhaelt.
 *
 * <p>Bedienelemente, die ueber {@link #addOption(AbstractWidget)} eingereicht werden, landen
 * im selben scrollbaren Bereich wie die eigenen Optionen des OviClickers und werden von
 * dessen bestehendem Scroll-Mechanismus mitverwaltet.</p>
 */
public interface ExtensionApi {

    /**
     * Nimmt ein Bedienelement in den scrollbaren Bereich des Bildschirms auf.
     *
     * @param widget das Bedienelement, dessen aktuelle Position als Grundposition gilt
     * @param <T>    der Typ des Bedienelements
     * @return dasselbe Bedienelement
     */
    <T extends AbstractWidget> T addOption(T widget);

    /**
     * Linke Kante des Bereichs, den Erweiterungen fuer ihre Zeilen nutzen koennen.
     *
     * @return die waagrechte Position
     */
    int columnX();

    /**
     * Volle Breite des Bereichs, den Erweiterungen fuer ihre Zeilen nutzen koennen.
     *
     * @return die Breite in Pixeln
     */
    int columnWidth();

    /**
     * Hoehe einer Zeile inklusive Abstand, wie sie der OviClicker selbst verwendet.
     *
     * @return die Zeilenhoehe in Pixeln
     */
    int rowHeight();

    /**
     * Hoehe eines einzelnen Bedienelements, wie sie der OviClicker selbst verwendet.
     *
     * @return die Elementhoehe in Pixeln
     */
    int widgetHeight();

    /**
     * Gibt die naechste freie senkrechte Position zurueck und zaehlt den internen Zeiger
     * anschliessend um {@link #rowHeight()} weiter.
     *
     * @return die senkrechte Position fuer die naechste Zeile
     */
    int nextRowY();

    /**
     * Schriftart des Bildschirms, fuer Bedienelemente, die sie benoetigen.
     *
     * @return die Schriftart
     */
    Font font();

    /**
     * Die Client-Instanz, fuer Bedienelemente, die sie benoetigen.
     *
     * @return die Client-Instanz
     */
    Minecraft minecraft();

    /**
     * Baut den gesamten Bildschirm neu auf, inklusive aller eingehaengten Erweiterungen.
     * Fuer Aenderungen, die zusaetzliche oder wegfallende Zeilen zur Folge haben, etwa
     * das Anlegen oder Loeschen eines Eintrags.
     */
    void rebuild();
}

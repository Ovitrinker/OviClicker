package ch.andrinzwicky.oviclicker.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.DoubleConsumer;

/**
 * Ein Schieberegler fuer einen Gleitkommawert innerhalb eines festen Bereichs.
 *
 * <p>Die Vanilla-Klasse {@code AbstractSliderButton} rechnet intern immer mit einem Wert
 * zwischen 0 und 1. Diese Klasse rechnet ihn auf den gewuenschten Bereich um und zeigt
 * den echten Wert in der Beschriftung an.</p>
 */
public class DoubleSliderWidget extends AbstractSliderButton {

    /** Uebersetzungsschluessel der Beschriftung, erhaelt den Wert als Platzhalter. */
    private final String labelKey;

    /** Kleinster einstellbarer Wert. */
    private final double minValue;

    /** Groesster einstellbarer Wert. */
    private final double maxValue;

    /** Anzahl Nachkommastellen in der Beschriftung. */
    private final int decimals;

    /** Empfaenger des geaenderten Werts. */
    private final DoubleConsumer applier;

    /**
     * Erstellt einen Schieberegler.
     *
     * @param x        linke Kante
     * @param y        obere Kante
     * @param width    Breite in Pixeln
     * @param height   Hoehe in Pixeln
     * @param labelKey Uebersetzungsschluessel der Beschriftung mit einem Platzhalter
     * @param value    aktueller Wert
     * @param minValue kleinster erlaubter Wert
     * @param maxValue groesster erlaubter Wert
     * @param decimals Anzahl Nachkommastellen in der Anzeige
     * @param applier  wird bei jeder Aenderung mit dem neuen Wert aufgerufen
     */
    public DoubleSliderWidget(int x, int y, int width, int height, String labelKey,
                              double value, double minValue, double maxValue, int decimals,
                              DoubleConsumer applier) {
        super(x, y, width, height, Component.empty(), toSliderValue(value, minValue, maxValue));
        this.labelKey = labelKey;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.decimals = decimals;
        this.applier = applier;
        updateMessage();
    }

    /**
     * Gibt den echten Wert des Reglers zurueck.
     *
     * @return Wert zwischen {@code minValue} und {@code maxValue}
     */
    public double getRealValue() {
        return minValue + this.value * (maxValue - minValue);
    }

    /**
     * Aktualisiert die Beschriftung mit dem aktuellen Wert.
     */
    @Override
    protected void updateMessage() {
        // Wird vom Konstruktor der Oberklasse noch vor dem Setzen der Felder aufgerufen,
        // deshalb der Schutz gegen einen noch fehlenden Schluessel.
        if (labelKey == null) return;
        setMessage(Component.translatable(labelKey, format(getRealValue())));
    }

    /**
     * Gibt den geaenderten Wert an die Einstellungen weiter.
     */
    @Override
    protected void applyValue() {
        if (applier != null) applier.accept(getRealValue());
    }

    /**
     * Formatiert einen Wert mit der eingestellten Anzahl Nachkommastellen.
     *
     * @param value der anzuzeigende Wert
     * @return der formatierte Text
     */
    private String format(double value) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    /**
     * Rechnet einen echten Wert auf den internen Bereich 0 bis 1 um.
     *
     * @param value    der echte Wert
     * @param minValue kleinster erlaubter Wert
     * @param maxValue groesster erlaubter Wert
     * @return Wert zwischen 0 und 1
     */
    private static double toSliderValue(double value, double minValue, double maxValue) {
        if (maxValue <= minValue) return 0.0;
        double normalised = (value - minValue) / (maxValue - minValue);
        return Math.max(0.0, Math.min(1.0, normalised));
    }
}

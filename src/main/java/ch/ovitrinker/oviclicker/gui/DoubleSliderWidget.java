package ch.ovitrinker.oviclicker.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.DoubleConsumer;

/**
 * A slider for a floating-point value within a fixed range.
 *
 * <p>The vanilla class {@code AbstractSliderButton} always works internally with a value
 * between 0 and 1. This class maps it to the desired range and shows the real value in the
 * label.</p>
 */
public class DoubleSliderWidget extends AbstractSliderButton {

    /** Translation key of the label, receives the value as a placeholder. */
    private final String labelKey;

    /** Smallest selectable value. */
    private final double minValue;

    /** Largest selectable value. */
    private final double maxValue;

    /** Number of decimal places in the label. */
    private final int decimals;

    /** Receiver of the changed value. */
    private final DoubleConsumer applier;

    /**
     * Creates a slider.
     *
     * @param x        left edge
     * @param y        top edge
     * @param width    width in pixels
     * @param height   height in pixels
     * @param labelKey translation key of the label with one placeholder
     * @param value    current value
     * @param minValue smallest allowed value
     * @param maxValue largest allowed value
     * @param decimals number of decimal places shown
     * @param applier  called with the new value on every change
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
     * Returns the slider's real value.
     *
     * @return value between {@code minValue} and {@code maxValue}
     */
    public double getRealValue() {
        return minValue + this.value * (maxValue - minValue);
    }

    /**
     * Updates the label with the current value.
     */
    @Override
    protected void updateMessage() {
        // Called by the superclass constructor before the fields are set,
        // hence the guard against a key that is still missing.
        if (labelKey == null) return;
        setMessage(Component.translatable(labelKey, format(getRealValue())));
    }

    /**
     * Passes the changed value on to the settings.
     */
    @Override
    protected void applyValue() {
        if (applier != null) applier.accept(getRealValue());
    }

    /**
     * Formats a value with the configured number of decimal places.
     *
     * @param value the value to display
     * @return the formatted text
     */
    private String format(double value) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    /**
     * Maps a real value to the internal range 0 to 1.
     *
     * @param value    the real value
     * @param minValue smallest allowed value
     * @param maxValue largest allowed value
     * @return value between 0 and 1
     */
    private static double toSliderValue(double value, double minValue, double maxValue) {
        if (maxValue <= minValue) return 0.0;
        double normalised = (value - minValue) / (maxValue - minValue);
        return Math.max(0.0, Math.min(1.0, normalised));
    }
}

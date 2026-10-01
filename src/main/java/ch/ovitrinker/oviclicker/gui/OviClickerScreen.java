package ch.ovitrinker.oviclicker.gui;

import ch.ovitrinker.oviclicker.compat.ClientCompat;
import ch.ovitrinker.oviclicker.config.OviClickerConfig;
import ch.ovitrinker.oviclicker.config.ConfigManager;
import ch.ovitrinker.oviclicker.config.HudCorner;
import ch.ovitrinker.oviclicker.feature.OviClickerEngine;
import ch.ovitrinker.oviclicker.feature.ClickAction;
import ch.ovitrinker.oviclicker.feature.ClickMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

//? if <26.1 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The OviClicker settings screen.
 *
 * <p>Deliberately built with the vanilla screen API only, without Cloth Config or any other
 * third-party library. That way the multi-version build doesn't depend on any additional
 * dependency.</p>
 *
 * <p>The options area between title and footer can be scrolled with the mouse wheel if not
 * all options fit on the screen. Scrolling happens in whole rows, so a row is never cut in
 * half at the edge. Widgets outside the visible area are hidden and therefore don't receive
 * clicks either.</p>
 *
 * <p>All changes go into a working copy of the settings first. Only the "Save" button
 * applies them and writes them to disk.</p>
 */
public class OviClickerScreen extends Screen {

    /** Width of a column in pixels. */
    private static final int COLUMN_WIDTH = 150;

    /** Gap between the columns. */
    private static final int COLUMN_GAP = 10;

    /** Height of a row including spacing. */
    private static final int ROW_HEIGHT = 22;

    /** Height of a widget. */
    private static final int WIDGET_HEIGHT = 20;

    /** Top edge of the scrollable area. */
    private static final int VIEWPORT_TOP = 32;

    /** Distance of the scrollable area from the bottom of the screen. */
    private static final int VIEWPORT_BOTTOM_MARGIN = 34;

    /** Screen to return to when closing. */
    private final Screen parent;

    /** Working copy of the settings. */
    private final OviClickerConfig working;

    /** All scrollable widgets with their unshifted position. */
    private final List<ScrollEntry> entries = new ArrayList<>();

    /** Current scroll offset in rows. */
    private int scrollRows = 0;

    /** Largest possible scroll offset in rows. */
    private int maxScrollRows = 0;

    /**
     * A scrollable widget together with its position without scrolling.
     *
     * @param widget the widget
     * @param baseY  the vertical position at scroll offset zero
     */
    private record ScrollEntry(AbstractWidget widget, int baseY) {
    }

    /**
     * Creates the settings screen.
     *
     * @param title  the screen title
     * @param parent the screen to return to when closing, may be {@code null}
     */
    public OviClickerScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
        this.working = ConfigManager.get().copy();
    }

    /**
     * Builds all widgets. Called again after every mode change.
     */
    @Override
    protected void init() {
        entries.clear();

        int leftX = this.width / 2 - COLUMN_WIDTH - COLUMN_GAP / 2;
        int rightX = this.width / 2 + COLUMN_GAP / 2;

        int leftY = 36;
        int rightY = 36;

        // --- Left column: mode, action and the mode's options ---
        addOption(Button.builder(
                        Component.translatable("oviclicker.gui.mode",
                                Component.translatable(working.getMode().getTranslationKey())),
                        button -> {
                            working.setMode(working.getMode().next());
                            rebuildWidgets();
                        })
                .bounds(leftX, leftY, COLUMN_WIDTH, WIDGET_HEIGHT).build());
        leftY += ROW_HEIGHT;

        // Action of the active mode: what OviClicker triggers
        if (working.getMode() != ClickMode.OFF) {
            final ClickMode currentMode = working.getMode();
            addOption(Button.builder(
                            Component.translatable("oviclicker.option.action",
                                    Component.translatable(working.getAction(currentMode).getTranslationKey())),
                            button -> {
                                ClickAction next = working.getAction(currentMode).next();
                                working.setAction(currentMode, next);
                                button.setMessage(Component.translatable("oviclicker.option.action",
                                        Component.translatable(next.getTranslationKey())));
                            })
                    .bounds(leftX, leftY, COLUMN_WIDTH, WIDGET_HEIGHT).build());
            leftY += ROW_HEIGHT;
        }

        switch (working.getMode()) {
            case AUTOATTACK -> {
                addOption(new DoubleSliderWidget(leftX, leftY, COLUMN_WIDTH, WIDGET_HEIGHT,
                        "oviclicker.option.cps", working.autoAttackCps, 0.1, 20.0, 1,
                        value -> working.autoAttackCps = value));
                leftY += ROW_HEIGHT;

                addOption(new DoubleSliderWidget(leftX, leftY, COLUMN_WIDTH, WIDGET_HEIGHT,
                        "oviclicker.option.jitter", working.autoAttackJitterPercent, 0.0, 100.0, 0,
                        value -> working.autoAttackJitterPercent = value));
                leftY += ROW_HEIGHT;

                addOption(new DoubleSliderWidget(leftX, leftY, COLUMN_WIDTH, WIDGET_HEIGHT,
                        "oviclicker.option.reach", working.maxReach, 1.0, 6.0, 1,
                        value -> working.maxReach = value));
                leftY += ROW_HEIGHT;

                // Entity blacklist, only meaningful in AUTOATTACK mode
                addOption(Checkbox.builder(
                                Component.translatable("oviclicker.option.blacklist_players"), this.font)
                        .pos(leftX, leftY).maxWidth(COLUMN_WIDTH).selected(working.blacklistPlayers)
                        .onValueChange((checkbox, selected) -> working.blacklistPlayers = selected).build());
                leftY += ROW_HEIGHT;

                addOption(Checkbox.builder(
                                Component.translatable("oviclicker.option.blacklist_villagers"), this.font)
                        .pos(leftX, leftY).maxWidth(COLUMN_WIDTH).selected(working.blacklistVillagers)
                        .onValueChange((checkbox, selected) -> working.blacklistVillagers = selected).build());
                leftY += ROW_HEIGHT;

                addOption(Checkbox.builder(
                                Component.translatable("oviclicker.option.blacklist_tamed"), this.font)
                        .pos(leftX, leftY).maxWidth(COLUMN_WIDTH).selected(working.blacklistTamed)
                        .onValueChange((checkbox, selected) -> working.blacklistTamed = selected).build());
                leftY += ROW_HEIGHT;

                addOption(Checkbox.builder(
                                Component.translatable("oviclicker.option.blacklist_passive"), this.font)
                        .pos(leftX, leftY).maxWidth(COLUMN_WIDTH).selected(working.blacklistPassive)
                        .onValueChange((checkbox, selected) -> working.blacklistPassive = selected).build());
                leftY += ROW_HEIGHT;
            }
            case TIMER -> {
                addOption(new DoubleSliderWidget(leftX, leftY, COLUMN_WIDTH, WIDGET_HEIGHT,
                        "oviclicker.option.interval", working.timerIntervalSeconds, 0.05, 300.0, 2,
                        value -> working.timerIntervalSeconds = value));
                leftY += ROW_HEIGHT;

                addOption(new DoubleSliderWidget(leftX, leftY, COLUMN_WIDTH, WIDGET_HEIGHT,
                        "oviclicker.option.jitter", working.timerJitterPercent, 0.0, 100.0, 0,
                        value -> working.timerJitterPercent = value));
                leftY += ROW_HEIGHT;
            }
            case OFF -> {
                // OFF mode has no further options
            }
        }

        // --- Left column: AutoEat, independent of the selected mode ---
        leftY += ROW_HEIGHT / 2;

        addOption(Checkbox.builder(
                        Component.translatable("oviclicker.option.auto_eat"), this.font)
                .pos(leftX, leftY).maxWidth(COLUMN_WIDTH).selected(working.autoEatEnabled)
                .onValueChange((checkbox, selected) -> working.autoEatEnabled = selected).build());
        leftY += ROW_HEIGHT;

        addOption(new DoubleSliderWidget(leftX, leftY, COLUMN_WIDTH, WIDGET_HEIGHT,
                "oviclicker.option.auto_eat_threshold", working.autoEatThresholdHaunches, 1.0, 9.0, 0,
                value -> working.autoEatThresholdHaunches = (int) Math.round(value)));
        leftY += ROW_HEIGHT;

        addOption(Checkbox.builder(
                        Component.translatable("oviclicker.option.auto_eat_refill"), this.font)
                .pos(leftX, leftY).maxWidth(COLUMN_WIDTH).selected(working.autoEatRefillFromInventory)
                .onValueChange((checkbox, selected) -> working.autoEatRefillFromInventory = selected).build());
        leftY += ROW_HEIGHT;

        addOption(Checkbox.builder(
                        Component.translatable("oviclicker.option.auto_eat_golden_apples"), this.font)
                .pos(leftX, leftY).maxWidth(COLUMN_WIDTH).selected(working.autoEatAllowGoldenApples)
                .onValueChange((checkbox, selected) -> working.autoEatAllowGoldenApples = selected).build());
        leftY += ROW_HEIGHT;

        addOption(Checkbox.builder(
                        Component.translatable("oviclicker.option.auto_eat_enchanted_golden_apples"), this.font)
                .pos(leftX, leftY).maxWidth(COLUMN_WIDTH).selected(working.autoEatAllowEnchantedGoldenApples)
                .onValueChange((checkbox, selected) ->
                        working.autoEatAllowEnchantedGoldenApples = selected).build());

        // --- Right column: shared conditions ---
        addOption(Checkbox.builder(
                        Component.translatable("oviclicker.option.master_enabled"), this.font)
                .pos(rightX, rightY).maxWidth(COLUMN_WIDTH).selected(working.masterEnabled)
                .onValueChange((checkbox, selected) -> working.masterEnabled = selected).build());
        rightY += ROW_HEIGHT;

        addOption(Checkbox.builder(
                        Component.translatable("oviclicker.option.only_while_attack_key"), this.font)
                .pos(rightX, rightY).maxWidth(COLUMN_WIDTH).selected(working.onlyWhileAttackKeyHeld)
                .onValueChange((checkbox, selected) -> working.onlyWhileAttackKeyHeld = selected).build());
        rightY += ROW_HEIGHT;

        addOption(Checkbox.builder(
                        Component.translatable("oviclicker.option.respect_cooldown"), this.font)
                .pos(rightX, rightY).maxWidth(COLUMN_WIDTH).selected(working.respectAttackCooldown)
                .onValueChange((checkbox, selected) -> working.respectAttackCooldown = selected).build());
        rightY += ROW_HEIGHT;

        addOption(Checkbox.builder(
                        Component.translatable("oviclicker.option.require_weapon"), this.font)
                .pos(rightX, rightY).maxWidth(COLUMN_WIDTH).selected(working.requireWeapon)
                .onValueChange((checkbox, selected) -> working.requireWeapon = selected).build());
        rightY += ROW_HEIGHT;

        addOption(Checkbox.builder(
                        Component.translatable("oviclicker.option.hold_instead_of_tap"), this.font)
                .pos(rightX, rightY).maxWidth(COLUMN_WIDTH).selected(working.holdInsteadOfTap)
                .onValueChange((checkbox, selected) -> working.holdInsteadOfTap = selected).build());
        rightY += ROW_HEIGHT;

        addOption(new DoubleSliderWidget(rightX, rightY, COLUMN_WIDTH, WIDGET_HEIGHT,
                "oviclicker.option.tap_duration", working.tapDurationTicks, 1.0, 20.0, 0,
                value -> working.tapDurationTicks = (int) Math.round(value)));
        rightY += ROW_HEIGHT;

        // --- Right column: HUD ---
        addOption(Checkbox.builder(
                        Component.translatable("oviclicker.option.hud_enabled"), this.font)
                .pos(rightX, rightY).maxWidth(COLUMN_WIDTH).selected(working.hudEnabled)
                .onValueChange((checkbox, selected) -> working.hudEnabled = selected).build());
        rightY += ROW_HEIGHT;

        addOption(Checkbox.builder(
                        Component.translatable("oviclicker.option.hud_hide_when_off"), this.font)
                .pos(rightX, rightY).maxWidth(COLUMN_WIDTH).selected(working.hudHideWhenOff)
                .onValueChange((checkbox, selected) -> working.hudHideWhenOff = selected).build());
        rightY += ROW_HEIGHT;

        addOption(Button.builder(
                        Component.translatable("oviclicker.option.hud_corner",
                                Component.translatable(working.getHudCorner().getTranslationKey())),
                        button -> {
                            HudCorner next = working.getHudCorner().next();
                            working.setHudCorner(next);
                            button.setMessage(Component.translatable("oviclicker.option.hud_corner",
                                    Component.translatable(next.getTranslationKey())));
                        })
                .bounds(rightX, rightY, COLUMN_WIDTH, WIDGET_HEIGHT).build());
        rightY += ROW_HEIGHT;

        addOption(new DoubleSliderWidget(rightX, rightY, COLUMN_WIDTH, WIDGET_HEIGHT,
                "oviclicker.option.hud_offset_x", working.hudOffsetX, 0.0, 200.0, 0,
                value -> working.hudOffsetX = (int) Math.round(value)));
        rightY += ROW_HEIGHT;

        addOption(new DoubleSliderWidget(rightX, rightY, COLUMN_WIDTH, WIDGET_HEIGHT,
                "oviclicker.option.hud_offset_y", working.hudOffsetY, 0.0, 200.0, 0,
                value -> working.hudOffsetY = (int) Math.round(value)));

        // --- Sections hooked in by other mods ---
        int addonX = leftX;
        int addonWidth = COLUMN_WIDTH * 2 + COLUMN_GAP;
        int addonY = Math.max(leftY, rightY) + ROW_HEIGHT / 2;

        for (ScreenExtension extension : ScreenExtensions.all()) {
            addOption(new StringWidget(addonX, addonY, addonWidth, WIDGET_HEIGHT,
                    extension.sectionTitle(), this.font));
            addonY += ROW_HEIGHT;

            ExtensionApiImpl api = new ExtensionApiImpl(addonX, addonWidth, addonY);
            extension.buildOptions(api);
            addonY = api.cursorY;
        }

        // --- Footer, does not scroll ---
        int footerY = this.height - 28;
        int buttonWidth = 100;

        addRenderableWidget(Button.builder(Component.translatable("oviclicker.gui.save"), button -> {
            ConfigManager.replaceAndSave(working);
            OviClickerEngine.resetTimer();
            ScreenExtensions.all().forEach(ScreenExtension::onSave);
            onClose();
        }).bounds(this.width / 2 - buttonWidth - 55, footerY, buttonWidth, WIDGET_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("oviclicker.gui.reset"), button -> {
            working.copyFrom(new OviClickerConfig());
            rebuildWidgets();
        }).bounds(this.width / 2 - buttonWidth / 2, footerY, buttonWidth, WIDGET_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("oviclicker.gui.cancel"),
                        button -> {
                            ScreenExtensions.all().forEach(ScreenExtension::onCancel);
                            onClose();
                        })
                .bounds(this.width / 2 + 55, footerY, buttonWidth, WIDGET_HEIGHT).build());

        updateScrollRange();
        applyScroll();
    }

    /**
     * Adds a widget to the scrollable area.
     *
     * @param widget the widget, whose current position is used as its base position
     * @param <T>    the widget type
     * @return the same widget
     */
    private <T extends AbstractWidget> T addOption(T widget) {
        entries.add(new ScrollEntry(widget, widget.getY()));
        return addRenderableWidget(widget);
    }

    /**
     * Passes the screen's scrollable area on to hooked-in {@link ScreenExtension}s.
     */
    private final class ExtensionApiImpl implements ExtensionApi {

        private final int columnX;
        private final int columnWidth;
        private int cursorY;

        private ExtensionApiImpl(int columnX, int columnWidth, int startY) {
            this.columnX = columnX;
            this.columnWidth = columnWidth;
            this.cursorY = startY;
        }

        @Override
        public <T extends AbstractWidget> T addOption(T widget) {
            return OviClickerScreen.this.addOption(widget);
        }

        @Override
        public int columnX() {
            return columnX;
        }

        @Override
        public int columnWidth() {
            return columnWidth;
        }

        @Override
        public int rowHeight() {
            return ROW_HEIGHT;
        }

        @Override
        public int widgetHeight() {
            return WIDGET_HEIGHT;
        }

        @Override
        public int nextRowY() {
            int y = cursorY;
            cursorY += ROW_HEIGHT;
            return y;
        }

        @Override
        public Font font() {
            return OviClickerScreen.this.font;
        }

        @Override
        public Minecraft minecraft() {
            return OviClickerScreen.this.minecraft;
        }

        @Override
        public void rebuild() {
            OviClickerScreen.this.rebuildWidgets();
        }
    }

    /**
     * Returns the bottom edge of the scrollable area.
     *
     * @return the vertical position of the bottom edge
     */
    private int viewportBottom() {
        return this.height - VIEWPORT_BOTTOM_MARGIN;
    }

    /**
     * Calculates how many rows can be scrolled at all and clamps the current offset to it.
     */
    private void updateScrollRange() {
        int lowestEdge = VIEWPORT_TOP;
        for (ScrollEntry entry : entries) {
            lowestEdge = Math.max(lowestEdge, entry.baseY() + entry.widget().getHeight());
        }

        int overflow = lowestEdge - viewportBottom();
        maxScrollRows = overflow <= 0 ? 0 : (int) Math.ceil((double) overflow / ROW_HEIGHT);
        scrollRows = Math.max(0, Math.min(scrollRows, maxScrollRows));
    }

    /**
     * Shifts all scrollable widgets and hides those that are not fully inside the visible
     * area.
     */
    private void applyScroll() {
        int shift = scrollRows * ROW_HEIGHT;
        int bottom = viewportBottom();

        for (ScrollEntry entry : entries) {
            AbstractWidget widget = entry.widget();
            int y = entry.baseY() - shift;
            widget.setY(y);

            boolean inside = y >= VIEWPORT_TOP && y + widget.getHeight() <= bottom;
            widget.visible = inside;
            // Invisible widgets must accept neither clicks nor focus
            widget.active = inside;
        }
    }

    /**
     * Scrolls the options area row by row with the mouse wheel.
     *
     * @param mouseX  horizontal mouse position
     * @param mouseY  vertical mouse position
     * @param scrollX horizontal scroll amount
     * @param scrollY vertical scroll amount
     * @return {@code true} if it scrolled
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScrollRows > 0 && scrollY != 0.0) {
            int before = scrollRows;
            scrollRows = Math.max(0, Math.min(maxScrollRows, scrollRows - (int) Math.signum(scrollY)));
            if (before != scrollRows) {
                applyScroll();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /**
     * Returns to the previous screen when closing.
     */
    @Override
    public void onClose() {
        ClientCompat.openScreen(this.minecraft, this.parent);
    }

    /**
     * The screen doesn't pause a singleplayer game, so settings can be tested while playing.
     *
     * @return always {@code false}
     */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Returns the position of the scrollbar: left edge, top edge of the thumb and its height.
     *
     * @return array with x, y and height of the thumb
     */
    private int[] scrollbarBounds() {
        int trackTop = VIEWPORT_TOP;
        int trackHeight = viewportBottom() - trackTop;
        int visibleRows = Math.max(1, trackHeight / ROW_HEIGHT);
        int totalRows = visibleRows + maxScrollRows;

        int thumbHeight = Math.max(20, trackHeight * visibleRows / totalRows);
        int thumbY = trackTop + (trackHeight - thumbHeight) * scrollRows / Math.max(1, maxScrollRows);
        int x = this.width / 2 + COLUMN_WIDTH + COLUMN_GAP / 2 + 6;

        return new int[]{x, thumbY, thumbHeight};
    }

    // Up to Minecraft 1.21.11 the screen draws directly via GuiGraphics,
    // from 26.1 on it collects a render state via GuiGraphicsExtractor instead.
    //? if <26.1 {
    /*@Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFFFF);

        if (maxScrollRows > 0) {
            int[] bounds = scrollbarBounds();
            graphics.fill(bounds[0], VIEWPORT_TOP, bounds[0] + 3, viewportBottom(), 0x40FFFFFF);
            graphics.fill(bounds[0], bounds[1], bounds[0] + 3, bounds[1] + bounds[2], 0xC0FFFFFF);
        }
    }
    *///?} else {
    /**
     * Collects the render state (Minecraft 26.1 and later).
     *
     * @param graphics drawing context of the new render system
     * @param mouseX   horizontal mouse position
     * @param mouseY   vertical mouse position
     * @param delta    partial tick
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, this.title, this.width / 2, 14, 0xFFFFFFFF);

        if (maxScrollRows > 0) {
            int[] bounds = scrollbarBounds();
            graphics.fill(bounds[0], VIEWPORT_TOP, bounds[0] + 3, viewportBottom(), 0x40FFFFFF);
            graphics.fill(bounds[0], bounds[1], bounds[0] + 3, bounds[1] + bounds[2], 0xC0FFFFFF);
        }
    }
    //?}
}

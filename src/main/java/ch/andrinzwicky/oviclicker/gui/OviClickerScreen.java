package ch.andrinzwicky.oviclicker.gui;

import ch.andrinzwicky.oviclicker.compat.ClientCompat;
import ch.andrinzwicky.oviclicker.config.OviClickerConfig;
import ch.andrinzwicky.oviclicker.config.ConfigManager;
import ch.andrinzwicky.oviclicker.config.HudCorner;
import ch.andrinzwicky.oviclicker.feature.OviClickerEngine;
import ch.andrinzwicky.oviclicker.feature.ClickAction;
import ch.andrinzwicky.oviclicker.feature.ClickMode;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

//? if <26.1 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Der Einstellungsbildschirm des OviClickers.
 *
 * <p>Bewusst ausschliesslich mit dem Vanilla-Screen-API gebaut, ohne Cloth Config oder
 * eine andere Fremdbibliothek. So haengt der Multiversion-Build an keiner zusaetzlichen
 * Abhaengigkeit.</p>
 *
 * <p>Der Optionsbereich zwischen Titel und Fusszeile laesst sich mit dem Mausrad scrollen,
 * falls nicht alle Optionen auf den Bildschirm passen. Gescrollt wird in ganzen Zeilen,
 * damit nie eine halbe Zeile am Rand abgeschnitten wird. Bedienelemente ausserhalb des
 * Sichtbereichs werden ausgeblendet und nehmen dadurch auch keine Klicks entgegen.</p>
 *
 * <p>Alle Aenderungen laufen zuerst in eine Arbeitskopie der Einstellungen. Erst der
 * Knopf "Speichern" uebernimmt sie und schreibt sie auf die Festplatte.</p>
 */
public class OviClickerScreen extends Screen {

    /** Breite einer Spalte in Pixeln. */
    private static final int COLUMN_WIDTH = 150;

    /** Abstand zwischen den Spalten. */
    private static final int COLUMN_GAP = 10;

    /** Hoehe einer Zeile inklusive Abstand. */
    private static final int ROW_HEIGHT = 22;

    /** Hoehe eines Bedienelements. */
    private static final int WIDGET_HEIGHT = 20;

    /** Obere Kante des scrollbaren Bereichs. */
    private static final int VIEWPORT_TOP = 32;

    /** Abstand des scrollbaren Bereichs zum unteren Bildschirmrand. */
    private static final int VIEWPORT_BOTTOM_MARGIN = 34;

    /** Bildschirm, zu dem beim Schliessen zurueckgekehrt wird. */
    private final Screen parent;

    /** Arbeitskopie der Einstellungen. */
    private final OviClickerConfig working;

    /** Alle scrollbaren Bedienelemente mit ihrer unverschobenen Position. */
    private final List<ScrollEntry> entries = new ArrayList<>();

    /** Aktuelle Verschiebung in Zeilen. */
    private int scrollRows = 0;

    /** Groesstmoegliche Verschiebung in Zeilen. */
    private int maxScrollRows = 0;

    /**
     * Ein scrollbares Bedienelement zusammen mit seiner Position ohne Verschiebung.
     *
     * @param widget das Bedienelement
     * @param baseY  die senkrechte Position bei Verschiebung null
     */
    private record ScrollEntry(AbstractWidget widget, int baseY) {
    }

    /**
     * Erstellt den Einstellungsbildschirm.
     *
     * @param title  der Titel des Bildschirms
     * @param parent der Bildschirm, zu dem beim Schliessen zurueckgekehrt wird, darf {@code null} sein
     */
    public OviClickerScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
        this.working = ConfigManager.get().copy();
    }

    /**
     * Baut alle Bedienelemente auf. Wird auch nach jedem Moduswechsel erneut aufgerufen.
     */
    @Override
    protected void init() {
        entries.clear();

        int leftX = this.width / 2 - COLUMN_WIDTH - COLUMN_GAP / 2;
        int rightX = this.width / 2 + COLUMN_GAP / 2;

        int leftY = 36;
        int rightY = 36;

        // --- Linke Spalte: Modus, Aktion und die Optionen des Modus ---
        addOption(Button.builder(
                        Component.translatable("oviclicker.gui.mode",
                                Component.translatable(working.getMode().getTranslationKey())),
                        button -> {
                            working.setMode(working.getMode().next());
                            rebuildWidgets();
                        })
                .bounds(leftX, leftY, COLUMN_WIDTH, WIDGET_HEIGHT).build());
        leftY += ROW_HEIGHT;

        // Aktion des aktiven Modus: was der OviClicker ausloest
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

                // Entity-Blacklist, nur im Modus AUTOATTACK sinnvoll
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
                // Im Modus OFF gibt es keine weiteren Optionen
            }
        }

        // --- Linke Spalte: AutoEat, unabhaengig vom gewaehlten Modus ---
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

        // --- Rechte Spalte: gemeinsame Bedingungen ---
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

        // --- Rechte Spalte: HUD ---
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

        // --- Fusszeile, scrollt nicht mit ---
        int footerY = this.height - 28;
        int buttonWidth = 100;

        addRenderableWidget(Button.builder(Component.translatable("oviclicker.gui.save"), button -> {
            ConfigManager.replaceAndSave(working);
            OviClickerEngine.resetTimer();
            onClose();
        }).bounds(this.width / 2 - buttonWidth - 55, footerY, buttonWidth, WIDGET_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("oviclicker.gui.reset"), button -> {
            working.copyFrom(new OviClickerConfig());
            rebuildWidgets();
        }).bounds(this.width / 2 - buttonWidth / 2, footerY, buttonWidth, WIDGET_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("oviclicker.gui.cancel"),
                        button -> onClose())
                .bounds(this.width / 2 + 55, footerY, buttonWidth, WIDGET_HEIGHT).build());

        updateScrollRange();
        applyScroll();
    }

    /**
     * Nimmt ein Bedienelement in den scrollbaren Bereich auf.
     *
     * @param widget das Bedienelement, dessen aktuelle Position als Grundposition gilt
     * @param <T>    der Typ des Bedienelements
     * @return dasselbe Bedienelement
     */
    private <T extends AbstractWidget> T addOption(T widget) {
        entries.add(new ScrollEntry(widget, widget.getY()));
        return addRenderableWidget(widget);
    }

    /**
     * Gibt die untere Kante des scrollbaren Bereichs zurueck.
     *
     * @return die senkrechte Position der unteren Kante
     */
    private int viewportBottom() {
        return this.height - VIEWPORT_BOTTOM_MARGIN;
    }

    /**
     * Berechnet, um wie viele Zeilen ueberhaupt gescrollt werden kann, und begrenzt die
     * aktuelle Verschiebung darauf.
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
     * Verschiebt alle scrollbaren Bedienelemente und blendet jene aus, die nicht
     * vollstaendig im Sichtbereich liegen.
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
            // Unsichtbare Bedienelemente sollen weder Klicks noch den Fokus annehmen
            widget.active = inside;
        }
    }

    /**
     * Scrollt den Optionsbereich mit dem Mausrad, zeilenweise.
     *
     * @param mouseX  Mausposition waagrecht
     * @param mouseY  Mausposition senkrecht
     * @param scrollX waagrechte Scrollmenge
     * @param scrollY senkrechte Scrollmenge
     * @return {@code true}, wenn gescrollt wurde
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
     * Kehrt beim Schliessen zum vorherigen Bildschirm zurueck.
     */
    @Override
    public void onClose() {
        ClientCompat.openScreen(this.minecraft, this.parent);
    }

    /**
     * Der Bildschirm pausiert ein Einzelspieler-Spiel nicht, damit sich Einstellungen
     * im laufenden Spiel testen lassen.
     *
     * @return immer {@code false}
     */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Gibt die Position des Scrollbalkens zurueck: linke Kante, obere Kante des Reglers
     * und dessen Hoehe.
     *
     * @return Feld mit x, y und Hoehe des Reglers
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

    // Bis Minecraft 1.21.11 zeichnet der Screen direkt ueber GuiGraphics,
    // ab 26.1 sammelt er stattdessen einen Renderzustand ueber GuiGraphicsExtractor ein.
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
     * Sammelt den Renderzustand ein (ab Minecraft 26.1).
     *
     * @param graphics Zeichenkontext des neuen Rendersystems
     * @param mouseX   Mausposition waagrecht
     * @param mouseY   Mausposition senkrecht
     * @param delta    Teiltick
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

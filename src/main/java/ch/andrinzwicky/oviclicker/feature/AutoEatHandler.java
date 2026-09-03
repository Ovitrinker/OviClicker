package ch.andrinzwicky.oviclicker.feature;

import ch.andrinzwicky.oviclicker.compat.ContainerCompat;
import ch.andrinzwicky.oviclicker.config.OviClickerConfig;
import ch.andrinzwicky.oviclicker.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Isst automatisch, sobald der Hunger unter die eingestellte Schwelle faellt, und hoert
 * erst wieder auf, wenn die Hungerleiste voll ist.
 *
 * <p>Solange gegessen wird, pausiert der OviClicker: {@code OviClickerEngine} fragt dazu
 * {@link #isEating()} ab und laesst in dieser Zeit jede gehaltene Taste los. Nach dem
 * Essen laeuft er ohne Zutun weiter.</p>
 *
 * <p>Liegt kein passendes Essen in der Hotbar, holt der Mod welches aus dem Inventar. Der
 * Tausch entspricht exakt dem Druck auf eine Hotbar-Taste im offenen Inventar (siehe
 * {@link ContainerCompat}). Musste dafuer ein Gegenstand aus der Hotbar weichen, wandert
 * er nach dem Essen wieder an seinen Platz zurueck; war der Platz vorher leer, bleibt der
 * Rest des Stapels einfach dort liegen.</p>
 *
 * <p>Gegessen wird bewusst ueber {@code MultiPlayerGameMode.useItem} und nicht ueber den
 * allgemeinen Rechtsklick des Spiels. Der allgemeine Rechtsklick wuerde zuerst den
 * anvisierten Block bedienen und damit womoeglich eine Truhe oeffnen oder einen Block
 * setzen, statt zu essen. Die Taste "Benutzen" wird trotzdem gedrueckt gehalten, weil
 * Minecraft das Essen sonst nach einem Tick wieder abbricht.</p>
 *
 * <p>Der Spieler behaelt jederzeit die Oberhand: wechselt er selbst den Hotbar-Platz oder
 * benutzt er selbst einen Gegenstand, bricht der Vorgang sauber ab.</p>
 */
public final class AutoEatHandler {

    /** Voller Hungerbalken in halben Keulen. */
    private static final int FULL_FOOD = 20;

    /** Wartezeit in Ticks, bis nach erfolgloser Suche erneut gesucht wird. */
    private static final int RETRY_TICKS = 20;

    /** Wartezeit in Ticks, bis nach einem Slotwechsel der erste Bissen ausgeloest wird. */
    private static final int SETTLE_TICKS = 2;

    /** {@code true}, solange ein Ess-Vorgang laeuft. */
    private static boolean eating = false;

    /** Hotbar-Platz, der vor dem Essen gewaehlt war, oder -1. */
    private static int previousSlot = -1;

    /** Hotbar-Platz, aus dem gerade gegessen wird, oder -1. */
    private static int eatingSlot = -1;

    /** Inventarplatz, aus dem Essen geholt wurde, oder -1 wenn nichts geholt wurde. */
    private static int borrowedFromSlot = -1;

    /** Hotbar-Platz, in den geholt wurde, oder -1. */
    private static int borrowedToSlot = -1;

    /** Gegenstand, der dabei aus der Hotbar weichen musste, oder {@code null}. */
    private static Item displacedItem = null;

    /** {@code true}, solange der Mod die Taste "Benutzen" gedrueckt haelt. */
    private static boolean holdingUse = false;

    /** Verbleibende Wartezeit in Ticks. */
    private static int delayTicks = 0;

    private AutoEatHandler() {
    }

    /**
     * Gibt an, ob gerade ein Ess-Vorgang laeuft.
     *
     * @return {@code true}, wenn der OviClicker pausieren soll
     */
    public static boolean isEating() {
        return eating;
    }

    /**
     * Wird am Ende jedes Client-Ticks aufgerufen, nach der Klick-Logik.
     *
     * <p>Die Reihenfolge ist wichtig: der OviClicker laesst seine Taste erst los, danach
     * darf der AutoEat die Taste "Benutzen" halten, ohne dass sie im selben Tick wieder
     * losgelassen wird.</p>
     *
     * @param client die Client-Instanz, darf {@code null} sein
     */
    public static void onEndClientTick(Minecraft client) {
        if (client == null || client.player == null || client.level == null || client.gameMode == null) {
            forget();
            return;
        }

        OviClickerConfig config = ConfigManager.get();
        LocalPlayer player = client.player;

        if (!config.masterEnabled || !config.autoEatEnabled || player.isSpectator()) {
            stop(client, player);
            return;
        }

        // Im Einzelspieler steht mit offenem Bildschirm die ganze Welt still. Ein Bissen
        // wuerde nur in der Warteschlange liegen, deshalb passiert hier nichts.
        if (client.isPaused()) return;

        if (delayTicks > 0) {
            delayTicks--;
            // Die Taste darf nur gehalten werden, solange wirklich gegessen wird. Haelt
            // Minecraft sie waehrend einer Pause gedrueckt, loest es einen gewoehnlichen
            // Rechtsklick aus und wuerde damit den anvisierten Block bedienen.
            if (eating && player.isUsingItem()) {
                holdUseKey(client);
            } else {
                releaseUseKey(client);
            }
            return;
        }

        int foodLevel = player.getFoodData().getFoodLevel();

        if (!eating) {
            // Erst ab der eingestellten Schwelle wird ueberhaupt gesucht
            if (foodLevel >= config.autoEatThresholdHaunches * 2) return;

            // Isst oder benutzt der Spieler gerade selbst etwas, wird nicht dazwischengefunkt
            if (player.isUsingItem()) return;

            begin(client, player, config, foodLevel);
            return;
        }

        // "bis man keinen Hunger mehr hat": erst bei vollem Balken ist Schluss
        if (foodLevel >= FULL_FOOD) {
            stop(client, player);
            return;
        }

        keepEating(client, player, config);
    }

    /**
     * Sucht Essen, legt es notfalls in die Hotbar und waehlt es aus.
     *
     * @param client    die Client-Instanz
     * @param player    der Spieler
     * @param config    die aktiven Einstellungen
     * @param foodLevel der aktuelle Hungerstand in halben Keulen
     */
    private static void begin(Minecraft client, LocalPlayer player,
                              OviClickerConfig config, int foodLevel) {
        Inventory inventory = player.getInventory();
        int missing = FULL_FOOD - foodLevel;

        int slot = findFood(inventory, 0, Inventory.SELECTION_SIZE, missing, config);
        boolean fetched = false;

        if (slot < 0) {
            slot = fetchFromInventory(client, player, config, missing);
            fetched = slot >= 0;
        }

        if (slot < 0) {
            // Nichts Essbares gefunden: nicht in jedem Tick erneut das Inventar durchgehen
            delayTicks = RETRY_TICKS;
            return;
        }

        previousSlot = inventory.getSelectedSlot();
        inventory.setSelectedSlot(slot);

        eatingSlot = slot;
        eating = true;
        // Dem Server einen Moment Zeit lassen, den Slotwechsel und den Tausch zu uebernehmen
        delayTicks = fetched ? SETTLE_TICKS : 1;
    }

    /**
     * Holt Essen aus dem Inventar in die Hotbar.
     *
     * @param client  die Client-Instanz
     * @param player  der Spieler
     * @param config  die aktiven Einstellungen
     * @param missing die Anzahl fehlender halber Hungerkeulen
     * @return der Hotbar-Platz mit dem Essen, oder -1 wenn nichts geholt werden konnte
     */
    private static int fetchFromInventory(Minecraft client, LocalPlayer player,
                                          OviClickerConfig config, int missing) {
        if (!config.autoEatRefillFromInventory) return -1;

        // Ist ein anderer Behaelter offen (Truhe, Ofen), gehoert das Klick-Paket dorthin
        if (player.containerMenu != player.inventoryMenu) return -1;

        Inventory inventory = player.getInventory();
        int source = findFood(inventory,
                InventoryMenu.INV_SLOT_START, InventoryMenu.INV_SLOT_END, missing, config);
        if (source < 0) return -1;

        int target = freeHotbarSlot(inventory);
        if (target >= 0) {
            // Freier Platz: der Rest des Stapels darf danach einfach dort liegen bleiben
            ContainerCompat.swapWithHotbar(client, player, source, target);
            borrowedFromSlot = -1;
            borrowedToSlot = -1;
            displacedItem = null;
            return target;
        }

        // Hotbar voll: der aktuell gewaehlte Gegenstand weicht und kommt spaeter zurueck
        target = inventory.getSelectedSlot();
        ItemStack displaced = inventory.getItem(target);
        if (displaced.isEmpty()) return -1;

        ContainerCompat.swapWithHotbar(client, player, source, target);
        borrowedFromSlot = source;
        borrowedToSlot = target;
        displacedItem = displaced.getItem();
        return target;
    }

    /**
     * Fuehrt einen laufenden Ess-Vorgang weiter.
     *
     * @param client die Client-Instanz
     * @param player der Spieler
     * @param config die aktiven Einstellungen
     */
    private static void keepEating(Minecraft client, LocalPlayer player, OviClickerConfig config) {
        Inventory inventory = player.getInventory();

        // Hat der Spieler selbst umgeschaltet, gehoert ihm die Steuerung
        if (inventory.getSelectedSlot() != eatingSlot) {
            stop(client, player);
            return;
        }

        if (!FoodFilter.isGoodFood(inventory.getItem(eatingSlot),
                config.autoEatAllowGoldenApples, config.autoEatAllowEnchantedGoldenApples)) {
            // Stapel aufgebraucht: aufraeumen und gleich darauf neu suchen
            stop(client, player);
            delayTicks = SETTLE_TICKS;
            return;
        }

        if (player.isUsingItem()) {
            holdUseKey(client);
            return;
        }

        // Gezielt den Gegenstand benutzen, nicht den anvisierten Block
        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);

        if (player.isUsingItem()) {
            holdUseKey(client);
        } else {
            // Der Bissen kam nicht zustande, etwa weil der Server ihn abgelehnt hat
            releaseUseKey(client);
            delayTicks = SETTLE_TICKS;
        }
    }

    /**
     * Beendet einen Ess-Vorgang, raeumt die Hotbar auf und gibt die Taste frei.
     *
     * @param client die Client-Instanz
     * @param player der Spieler
     */
    private static void stop(Minecraft client, LocalPlayer player) {
        releaseUseKey(client);

        if (!eating) {
            resetState();
            return;
        }

        if (player.isUsingItem()) {
            client.gameMode.releaseUsingItem(player);
        }

        returnDisplacedItem(client, player);

        Inventory inventory = player.getInventory();
        if (previousSlot >= 0 && previousSlot < Inventory.SELECTION_SIZE) {
            inventory.setSelectedSlot(previousSlot);
        }

        resetState();
    }

    /**
     * Legt einen Gegenstand, der fuer das Essen aus der Hotbar weichen musste, zurueck.
     *
     * <p>Getauscht wird nur, wenn beide Plaetze noch so aussehen wie erwartet. Hat sich in
     * der Zwischenzeit etwas anderes dorthin verirrt, bleibt alles unberuehrt, damit der
     * Mod niemals fremde Gegenstaende verschiebt.</p>
     *
     * @param client die Client-Instanz
     * @param player der Spieler
     */
    private static void returnDisplacedItem(Minecraft client, LocalPlayer player) {
        if (borrowedFromSlot < 0 || borrowedToSlot < 0 || displacedItem == null) return;

        if (player.containerMenu == player.inventoryMenu) {
            Inventory inventory = player.getInventory();
            ItemStack source = inventory.getItem(borrowedFromSlot);
            ItemStack target = inventory.getItem(borrowedToSlot);

            boolean sourceIsOurs = source.getItem() == displacedItem;
            boolean targetIsFoodOrEmpty = target.isEmpty() || FoodFilter.nutritionOf(target) > 0;

            if (sourceIsOurs && targetIsFoodOrEmpty) {
                ContainerCompat.swapWithHotbar(client, player, borrowedFromSlot, borrowedToSlot);
            }
        }

        borrowedFromSlot = -1;
        borrowedToSlot = -1;
        displacedItem = null;
    }

    /**
     * Haelt die Taste "Benutzen" gedrueckt. Ohne sie bricht Minecraft das Essen im
     * naechsten Tick wieder ab.
     *
     * @param client die Client-Instanz
     */
    private static void holdUseKey(Minecraft client) {
        client.options.keyUse.setDown(true);
        holdingUse = true;
    }

    /**
     * Gibt die Taste "Benutzen" wieder frei.
     *
     * <p>Haelt der Spieler die Taste in diesem Moment selbst gedrueckt, bleibt sie
     * gedrueckt: sonst wuerde der Mod eine echte Eingabe verschlucken.</p>
     *
     * @param client die Client-Instanz
     */
    private static void releaseUseKey(Minecraft client) {
        if (!holdingUse) return;
        holdingUse = false;

        if (!InputSimulator.isPhysicallyDown(client, client.options.keyUse)) {
            client.options.keyUse.setDown(false);
        }
    }

    /**
     * Sucht in einem Bereich des Inventars das passendste Essen.
     *
     * <p>Bevorzugt wird der naehrreichste Gegenstand, der noch vollstaendig in die
     * Hungerleiste passt. Passt keiner hinein, wird der schwaechste genommen, damit
     * moeglichst wenig Naehrwert verfaellt.</p>
     *
     * @param inventory   das Inventar des Spielers
     * @param from        erster zu pruefender Platz
     * @param toExclusive erster Platz, der nicht mehr geprueft wird
     * @param missing     die Anzahl fehlender halber Hungerkeulen
     * @param config      die aktiven Einstellungen
     * @return der gefundene Platz oder -1
     */
    private static int findFood(Inventory inventory, int from, int toExclusive,
                                int missing, OviClickerConfig config) {
        int bestFit = -1;
        int bestFitNutrition = -1;
        int weakest = -1;
        int weakestNutrition = Integer.MAX_VALUE;

        int limit = Math.min(toExclusive, inventory.getContainerSize());

        for (int slot = from; slot < limit; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!FoodFilter.isGoodFood(stack, config.autoEatAllowGoldenApples,
                    config.autoEatAllowEnchantedGoldenApples)) continue;

            int nutrition = FoodFilter.nutritionOf(stack);

            if (nutrition <= missing && nutrition > bestFitNutrition) {
                bestFit = slot;
                bestFitNutrition = nutrition;
            }
            if (nutrition < weakestNutrition) {
                weakest = slot;
                weakestNutrition = nutrition;
            }
        }

        return bestFit >= 0 ? bestFit : weakest;
    }

    /**
     * Sucht einen leeren Platz in der Hotbar.
     *
     * @param inventory das Inventar des Spielers
     * @return der Platz oder -1, wenn die Hotbar voll ist
     */
    private static int freeHotbarSlot(Inventory inventory) {
        for (int slot = 0; slot < Inventory.SELECTION_SIZE; slot++) {
            if (inventory.getItem(slot).isEmpty()) return slot;
        }
        return -1;
    }

    /**
     * Setzt den Zustand zurueck, ohne noch etwas an der Welt zu aendern. Wird beim
     * Verlassen einer Welt aufgerufen, wo Spieler und Inventar nicht mehr existieren.
     */
    private static void forget() {
        resetState();
        holdingUse = false;
    }

    /**
     * Setzt alle Merker eines Ess-Vorgangs zurueck.
     */
    private static void resetState() {
        eating = false;
        previousSlot = -1;
        eatingSlot = -1;
        borrowedFromSlot = -1;
        borrowedToSlot = -1;
        displacedItem = null;
        delayTicks = 0;
    }
}

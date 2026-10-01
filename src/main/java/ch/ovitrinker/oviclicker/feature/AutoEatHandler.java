package ch.ovitrinker.oviclicker.feature;

import ch.ovitrinker.oviclicker.compat.ContainerCompat;
import ch.ovitrinker.oviclicker.config.OviClickerConfig;
import ch.ovitrinker.oviclicker.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Eats automatically as soon as hunger drops below the configured threshold, and only stops
 * once the hunger bar is full.
 *
 * <p>While eating, OviClicker pauses: {@code OviClickerEngine} checks {@link #isEating()} and
 * releases any held key during that time. After eating it resumes on its own.</p>
 *
 * <p>If there is no suitable food in the hotbar, the mod fetches some from the inventory. The
 * swap is exactly the same as pressing a hotbar key in the open inventory (see
 * {@link ContainerCompat}). If an item had to leave the hotbar for this, it is moved back to
 * its slot after eating; if the slot was empty before, the rest of the stack simply stays
 * there.</p>
 *
 * <p>Eating deliberately uses {@code MultiPlayerGameMode.useItem} and not the game's general
 * right click. The general right click would interact with the targeted block first and
 * might open a chest or place a block instead of eating. The "Use" key is still held down,
 * because Minecraft would otherwise cancel eating after one tick.</p>
 *
 * <p>The player always stays in control: if they switch the hotbar slot or use an item
 * themselves, the process is cancelled cleanly.</p>
 */
public final class AutoEatHandler {

    /** Full hunger bar in half haunches. */
    private static final int FULL_FOOD = 20;

    /** Ticks to wait before searching again after an unsuccessful search. */
    private static final int RETRY_TICKS = 20;

    /** Ticks to wait after a slot change before the first bite is triggered. */
    private static final int SETTLE_TICKS = 2;

    /** {@code true} while eating is in progress. */
    private static boolean eating = false;

    /** Hotbar slot that was selected before eating, or -1. */
    private static int previousSlot = -1;

    /** Hotbar slot currently being eaten from, or -1. */
    private static int eatingSlot = -1;

    /** Inventory slot food was fetched from, or -1 if nothing was fetched. */
    private static int borrowedFromSlot = -1;

    /** Hotbar slot food was fetched into, or -1. */
    private static int borrowedToSlot = -1;

    /** Item that had to leave the hotbar for it, or {@code null}. */
    private static Item displacedItem = null;

    /** {@code true} while the mod is holding the "Use" key. */
    private static boolean holdingUse = false;

    /** Remaining wait time in ticks. */
    private static int delayTicks = 0;

    private AutoEatHandler() {
    }

    /**
     * Returns whether eating is currently in progress.
     *
     * @return {@code true} if OviClicker should pause
     */
    public static boolean isEating() {
        return eating;
    }

    /**
     * Called at the end of every client tick, after the click logic.
     *
     * <p>The order matters: OviClicker releases its key first, then AutoEat may hold the
     * "Use" key without it being released again in the same tick.</p>
     *
     * @param client the client instance, may be {@code null}
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

        // In singleplayer the whole world stands still while a screen is open. A bite
        // would just sit in the queue, so nothing happens here.
        if (client.isPaused()) return;

        if (delayTicks > 0) {
            delayTicks--;
            // The key may only be held while actually eating. If Minecraft keeps it pressed
            // during a pause, it triggers a regular right click and would interact with the
            // targeted block.
            if (eating && player.isUsingItem()) {
                holdUseKey(client);
            } else {
                releaseUseKey(client);
            }
            return;
        }

        int foodLevel = player.getFoodData().getFoodLevel();

        if (!eating) {
            // Only search once the configured threshold is reached
            if (foodLevel >= config.autoEatThresholdHaunches * 2) return;

            // If the player is eating or using something themselves, don't interfere
            if (player.isUsingItem()) return;

            begin(client, player, config, foodLevel);
            return;
        }

        // "until you're no longer hungry": only stop once the bar is full
        if (foodLevel >= FULL_FOOD) {
            stop(client, player);
            return;
        }

        keepEating(client, player, config);
    }

    /**
     * Looks for food, moves it to the hotbar if needed and selects it.
     *
     * @param client    the client instance
     * @param player    the player
     * @param config    the active settings
     * @param foodLevel the current food level in half haunches
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
            // Nothing edible found: don't scan the inventory again every tick
            delayTicks = RETRY_TICKS;
            return;
        }

        previousSlot = inventory.getSelectedSlot();
        inventory.setSelectedSlot(slot);

        eatingSlot = slot;
        eating = true;
        // Give the server a moment to apply the slot change and the swap
        delayTicks = fetched ? SETTLE_TICKS : 1;
    }

    /**
     * Fetches food from the inventory into the hotbar.
     *
     * @param client  the client instance
     * @param player  the player
     * @param config  the active settings
     * @param missing the number of missing half haunches
     * @return the hotbar slot with the food, or -1 if nothing could be fetched
     */
    private static int fetchFromInventory(Minecraft client, LocalPlayer player,
                                          OviClickerConfig config, int missing) {
        if (!config.autoEatRefillFromInventory) return -1;

        // If another container is open (chest, furnace), the click packet belongs there
        if (player.containerMenu != player.inventoryMenu) return -1;

        Inventory inventory = player.getInventory();
        int source = findFood(inventory,
                InventoryMenu.INV_SLOT_START, InventoryMenu.INV_SLOT_END, missing, config);
        if (source < 0) return -1;

        int target = freeHotbarSlot(inventory);
        if (target >= 0) {
            // Free slot: the rest of the stack may simply stay there afterwards
            ContainerCompat.swapWithHotbar(client, player, source, target);
            borrowedFromSlot = -1;
            borrowedToSlot = -1;
            displacedItem = null;
            return target;
        }

        // Hotbar full: the currently selected item moves aside and comes back later
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
     * Continues an ongoing eating process.
     *
     * @param client the client instance
     * @param player the player
     * @param config the active settings
     */
    private static void keepEating(Minecraft client, LocalPlayer player, OviClickerConfig config) {
        Inventory inventory = player.getInventory();

        // If the player switched slots themselves, they're in control
        if (inventory.getSelectedSlot() != eatingSlot) {
            stop(client, player);
            return;
        }

        if (!FoodFilter.isGoodFood(inventory.getItem(eatingSlot),
                config.autoEatAllowGoldenApples, config.autoEatAllowEnchantedGoldenApples)) {
            // Stack used up: clean up and search again right after
            stop(client, player);
            delayTicks = SETTLE_TICKS;
            return;
        }

        if (player.isUsingItem()) {
            holdUseKey(client);
            return;
        }

        // Use the item specifically, not the targeted block
        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);

        if (player.isUsingItem()) {
            holdUseKey(client);
        } else {
            // The bite didn't happen, e.g. because the server rejected it
            releaseUseKey(client);
            delayTicks = SETTLE_TICKS;
        }
    }

    /**
     * Ends an eating process, tidies up the hotbar and releases the key.
     *
     * @param client the client instance
     * @param player the player
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
     * Puts back an item that had to leave the hotbar for eating.
     *
     * <p>Swaps only if both slots still look as expected. If something else ended up there
     * in the meantime, everything stays untouched, so the mod never moves items it doesn't
     * own.</p>
     *
     * @param client the client instance
     * @param player the player
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
     * Holds the "Use" key down. Without it, Minecraft cancels eating in the next tick.
     *
     * @param client the client instance
     */
    private static void holdUseKey(Minecraft client) {
        client.options.keyUse.setDown(true);
        holdingUse = true;
    }

    /**
     * Releases the "Use" key again.
     *
     * <p>If the player is holding the key themselves at that moment, it stays pressed:
     * otherwise the mod would swallow real input.</p>
     *
     * @param client the client instance
     */
    private static void releaseUseKey(Minecraft client) {
        if (!holdingUse) return;
        holdingUse = false;

        if (!InputSimulator.isPhysicallyDown(client, client.options.keyUse)) {
            client.options.keyUse.setDown(false);
        }
    }

    /**
     * Finds the most suitable food in a range of the inventory.
     *
     * <p>The most nutritious item that still fits completely into the hunger bar is
     * preferred. If none fits, the weakest one is taken, so as little nutrition as possible
     * is wasted.</p>
     *
     * @param inventory   the player's inventory
     * @param from        first slot to check
     * @param toExclusive first slot that is no longer checked
     * @param missing     the number of missing half haunches
     * @param config      the active settings
     * @return the slot found, or -1
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
     * Looks for an empty hotbar slot.
     *
     * @param inventory the player's inventory
     * @return the slot, or -1 if the hotbar is full
     */
    private static int freeHotbarSlot(Inventory inventory) {
        for (int slot = 0; slot < Inventory.SELECTION_SIZE; slot++) {
            if (inventory.getItem(slot).isEmpty()) return slot;
        }
        return -1;
    }

    /**
     * Resets the state without changing anything in the world. Called when leaving a world,
     * where player and inventory no longer exist.
     */
    private static void forget() {
        resetState();
        holdingUse = false;
    }

    /**
     * Resets all markers of an eating process.
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

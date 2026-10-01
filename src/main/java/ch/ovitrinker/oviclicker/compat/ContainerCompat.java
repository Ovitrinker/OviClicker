package ch.ovitrinker.oviclicker.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

//? if <26.1 {
/*import net.minecraft.world.inventory.ClickType;
*///?} else
import net.minecraft.world.inventory.ContainerInput;

/**
 * Wraps the single call that swaps an item between inventory and hotbar.
 *
 * <p>It triggers exactly the same swap that pressing a hotbar key in the open inventory would.
 * The client sends a regular click packet to the server for this; nothing is forged and
 * nothing is bypassed.</p>
 *
 * <p>With 26.1 the enum {@code ClickType} was renamed to {@code ContainerInput} and the method
 * {@code handleInventoryMouseClick} to {@code handleContainerInput}. The case distinction is
 * done with Stonecutter.</p>
 */
public final class ContainerCompat {

    private ContainerCompat() {
    }

    /**
     * Swaps the contents of an inventory slot with a hotbar slot.
     *
     * <p>In the player's inventory menu, the slots of the main inventory (9 to 35) have the
     * same numbers as in the inventory itself, so the inventory index can be used directly as
     * the menu slot number.</p>
     *
     * @param client       the client instance
     * @param player       the player
     * @param menuSlot     slot number in the inventory menu (9 to 35 for the main inventory)
     * @param hotbarIndex  target slot in the hotbar (0 to 8)
     */
    public static void swapWithHotbar(Minecraft client, Player player, int menuSlot, int hotbarIndex) {
        if (client == null || client.gameMode == null || player == null) return;

        int containerId = player.inventoryMenu.containerId;

        //? if <26.1 {
        /*client.gameMode.handleInventoryMouseClick(
                containerId, menuSlot, hotbarIndex, ClickType.SWAP, player);
        *///?} else {
        client.gameMode.handleContainerInput(
                containerId, menuSlot, hotbarIndex, ContainerInput.SWAP, player);
        //?}
    }
}

package ch.andrinzwicky.oviclicker.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

//? if <26.1 {
/*import net.minecraft.world.inventory.ClickType;
*///?} else
import net.minecraft.world.inventory.ContainerInput;

/**
 * Kapselt den einen Aufruf, mit dem ein Gegenstand zwischen Inventar und Hotbar
 * getauscht wird.
 *
 * <p>Ausgeloest wird genau derselbe Tausch, den ein Druck auf eine Hotbar-Taste im
 * offenen Inventar ausloesen wuerde. Der Client schickt dazu ein regulaeres
 * Klick-Paket an den Server; es wird nichts gefaelscht und nichts umgangen.</p>
 *
 * <p>Mit 26.1 wurde die Aufzaehlung {@code ClickType} in {@code ContainerInput}
 * umbenannt und die Methode {@code handleInventoryMouseClick} in
 * {@code handleContainerInput}. Die Fallunterscheidung passiert ueber Stonecutter.</p>
 */
public final class ContainerCompat {

    private ContainerCompat() {
    }

    /**
     * Tauscht den Inhalt eines Inventarplatzes mit einem Hotbar-Platz.
     *
     * <p>Im Inventarmenue des Spielers tragen die Plaetze des Hauptinventars (9 bis 35)
     * dieselben Nummern wie im Inventar selbst, deshalb kann der Inventarindex direkt
     * als Platznummer im Menue verwendet werden.</p>
     *
     * @param client       die Client-Instanz
     * @param player       der Spieler
     * @param menuSlot     Platznummer im Inventarmenue (9 bis 35 fuer das Hauptinventar)
     * @param hotbarIndex  Ziel-Platz in der Hotbar (0 bis 8)
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

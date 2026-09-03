package ch.andrinzwicky.oviclicker.feature;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;

import java.util.Set;

/**
 * Entscheidet, welche Gegenstaende der AutoEat essen darf.
 *
 * <p>Die Pruefung laeuft in drei Stufen:</p>
 * <ol>
 *   <li>Der Gegenstand muss ueberhaupt Nahrung sein, also eine {@code FoodProperties}-
 *       Komponente mit einem Naehrwert groesser null besitzen. Damit fallen Milcheimer,
 *       Traenke und Kuchen (ein Block, kein Nahrungsmittel) von selbst weg.</li>
 *   <li>Nahrung, die beim Essen einen schaedlichen Statuseffekt ausloest, wird
 *       ausgeschlossen. Das wird nicht aus einer Liste gelesen, sondern direkt aus den
 *       Verzehr-Effekten des Gegenstands: verfaultes Fleisch (Hunger), Spinnenauge
 *       (Vergiftung), giftige Kartoffel, Kugelfisch und rohes Huhn fallen dadurch
 *       automatisch heraus, ebenso jedes Essen aus anderen Mods mit derselben
 *       Eigenschaft.</li>
 *   <li>Zusaetzlich sperrt eine kurze Liste jene Nahrung, die zwar keinen schaedlichen
 *       Effekt hat, aber trotzdem nichts fuer das automatische Essen ist.</li>
 * </ol>
 */
public final class FoodFilter {

    /**
     * Nahrung ohne schaedlichen Effekt, die der AutoEat trotzdem nie anruehrt.
     *
     * <p>Die Chorusfrucht teleportiert und der verdaechtige Eintopf hat einen erst beim
     * Essen bekannten Effekt. Die beiden goldenen Aepfel stehen bewusst nicht hier: fuer
     * sie gibt es je eine eigene Einstellung.</p>
     */
    private static final Set<Item> NEVER = Set.of(
            Items.CHORUS_FRUIT,
            Items.SUSPICIOUS_STEW);

    private FoodFilter() {
    }

    /**
     * Prueft, ob ein Gegenstand gutes Essen ist.
     *
     * @param stack                      der zu pruefende Gegenstand, darf {@code null} sein
     * @param allowGoldenApples          {@code true}, wenn der gewoehnliche goldene Apfel
     *                                   gegessen werden darf
     * @param allowEnchantedGoldenApples {@code true}, wenn der verzauberte goldene Apfel
     *                                   gegessen werden darf
     * @return {@code true}, wenn der AutoEat den Gegenstand essen darf
     */
    public static boolean isGoodFood(ItemStack stack, boolean allowGoldenApples,
                                     boolean allowEnchantedGoldenApples) {
        if (stack == null || stack.isEmpty()) return false;

        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null || food.nutrition() <= 0) return false;

        if (NEVER.contains(stack.getItem())) return false;
        if (!allowGoldenApples && stack.is(Items.GOLDEN_APPLE)) return false;
        if (!allowEnchantedGoldenApples && stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return false;

        return !hasHarmfulEffect(stack);
    }

    /**
     * Gibt den Naehrwert eines Gegenstands in halben Hungerkeulen zurueck.
     *
     * @param stack der Gegenstand, darf {@code null} sein
     * @return der Naehrwert, oder 0 wenn es keine Nahrung ist
     */
    public static int nutritionOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        FoodProperties food = stack.get(DataComponents.FOOD);
        return food == null ? 0 : food.nutrition();
    }

    /**
     * Prueft, ob das Essen einen schaedlichen Statuseffekt ausloest.
     *
     * <p>Seit den Datenkomponenten liegen die Effekte nicht mehr in den
     * {@code FoodProperties}, sondern als Verzehr-Effekte in der Komponente
     * {@code Consumable}. Gewertet wird nur die Kategorie des Effekts, damit auch
     * Nahrung aus anderen Mods richtig eingeordnet wird.</p>
     *
     * @param stack der zu pruefende Gegenstand
     * @return {@code true}, wenn beim Essen ein schaedlicher Effekt entsteht
     */
    private static boolean hasHarmfulEffect(ItemStack stack) {
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable == null) return false;

        for (ConsumeEffect effect : consumable.onConsumeEffects()) {
            if (!(effect instanceof ApplyStatusEffectsConsumeEffect apply)) continue;

            for (MobEffectInstance instance : apply.effects()) {
                if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    return true;
                }
            }
        }

        return false;
    }
}

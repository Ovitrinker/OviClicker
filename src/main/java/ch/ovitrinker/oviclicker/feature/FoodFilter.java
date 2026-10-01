package ch.ovitrinker.oviclicker.feature;

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
 * Decides which items AutoEat may eat.
 *
 * <p>The check runs in three stages:</p>
 * <ol>
 *   <li>The item has to be food at all, i.e. have a {@code FoodProperties} component with a
 *       nutrition value above zero. This rules out milk buckets, potions and cake (a block,
 *       not a food item) automatically.</li>
 *   <li>Food that applies a harmful status effect when eaten is excluded. This isn't read
 *       from a list but directly from the item's consumption effects: rotten flesh (hunger),
 *       spider eye (poison), poisonous potato, pufferfish and raw chicken drop out
 *       automatically, as does any food from other mods with the same property.</li>
 *   <li>Additionally, a short list blocks food that has no harmful effect but still isn't
 *       suitable for automatic eating.</li>
 * </ol>
 */
public final class FoodFilter {

    /**
     * Food without a harmful effect that AutoEat still never touches.
     *
     * <p>Chorus fruit teleports, and suspicious stew has an effect that is only known when
     * eaten. The two golden apples are deliberately not listed here: each has its own
     * setting.</p>
     */
    private static final Set<Item> NEVER = Set.of(
            Items.CHORUS_FRUIT,
            Items.SUSPICIOUS_STEW);

    private FoodFilter() {
    }

    /**
     * Checks whether an item is good food.
     *
     * @param stack                      the item to check, may be {@code null}
     * @param allowGoldenApples          {@code true} if the regular golden apple may be
     *                                   eaten
     * @param allowEnchantedGoldenApples {@code true} if the enchanted golden apple may be
     *                                   eaten
     * @return {@code true} if AutoEat may eat the item
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
     * Returns an item's nutrition value in half haunches.
     *
     * @param stack the item, may be {@code null}
     * @return the nutrition value, or 0 if it isn't food
     */
    public static int nutritionOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        FoodProperties food = stack.get(DataComponents.FOOD);
        return food == null ? 0 : food.nutrition();
    }

    /**
     * Checks whether the food applies a harmful status effect.
     *
     * <p>Since data components, the effects no longer live in {@code FoodProperties} but as
     * consumption effects in the {@code Consumable} component. Only the effect's category is
     * evaluated, so food from other mods is classified correctly too.</p>
     *
     * @param stack the item to check
     * @return {@code true} if eating it causes a harmful effect
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

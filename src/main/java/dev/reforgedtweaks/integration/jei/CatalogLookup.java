package dev.reforgedtweaks.integration.jei;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;

final class CatalogLookup<T> implements ISimpleRecipeManagerPlugin<T> {
    private final Supplier<List<T>> recipes;
    private final BiPredicate<T, ITypedIngredient<?>> input;
    private final BiPredicate<T, ITypedIngredient<?>> output;
    CatalogLookup(Supplier<List<T>> recipes, BiPredicate<T, ITypedIngredient<?>> input, BiPredicate<T, ITypedIngredient<?>> output) {
        this.recipes = recipes; this.input = input; this.output = output;
    }
    public List<T> getAllRecipes() { return recipes.get(); }
    public boolean isHandledInput(ITypedIngredient<?> ingredient) { return !getRecipesForInput(ingredient).isEmpty(); }
    public boolean isHandledOutput(ITypedIngredient<?> ingredient) { return !getRecipesForOutput(ingredient).isEmpty(); }
    public List<T> getRecipesForInput(ITypedIngredient<?> ingredient) { return getAllRecipes().stream().filter(recipe -> input.test(recipe, ingredient)).toList(); }
    public List<T> getRecipesForOutput(ITypedIngredient<?> ingredient) { return getAllRecipes().stream().filter(recipe -> output.test(recipe, ingredient)).toList(); }
}

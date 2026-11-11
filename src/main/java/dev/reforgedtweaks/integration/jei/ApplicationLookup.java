package dev.reforgedtweaks.integration.jei;

import java.util.List;
import dev.reforgedtweaks.client.data.InfusionApplications;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;

final class ApplicationLookup implements ISimpleRecipeManagerPlugin<InfusionApplications.Application> {
    private final InfusionApplications catalog;
    ApplicationLookup(InfusionApplications catalog) { this.catalog = catalog; }
    public List<InfusionApplications.Application> getAllRecipes() { return catalog.all(); }
    public boolean isHandledInput(ITypedIngredient<?> ingredient) { return !getRecipesForInput(ingredient).isEmpty(); }
    public boolean isHandledOutput(ITypedIngredient<?> ingredient) { return !getRecipesForOutput(ingredient).isEmpty(); }
    public List<InfusionApplications.Application> getRecipesForInput(ITypedIngredient<?> ingredient) {
        return ingredient.getIngredient(VanillaTypes.ITEM_STACK).map(stack -> {
            var result = new java.util.ArrayList<>(catalog.input(stack));
            for (var recipe : catalog.all()) if (recipe.plan().components().stream().anyMatch(part -> part.test(stack))) result.add(recipe);
            return (List<InfusionApplications.Application>) result;
        }).orElseGet(() -> ingredient.getIngredient(AspectIngredient.TYPE).map(aspect -> catalog.all().stream()
            .filter(recipe -> recipe.plan().aspects().getAmount(aspect.aspect()) > 0).toList()).orElse(List.of()));
    }
    public List<InfusionApplications.Application> getRecipesForOutput(ITypedIngredient<?> ingredient) {
        return ingredient.getIngredient(VanillaTypes.ITEM_STACK).map(catalog::output).orElse(List.of());
    }
}

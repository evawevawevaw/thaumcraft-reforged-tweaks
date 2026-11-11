package dev.reforgedtweaks.integration.jei;

import dev.reforgedtweaks.client.data.AspectSources;
import java.util.List;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import dev.reforgedtweaks.client.ClientLifecycle;

final class SourceLookup implements ISimpleRecipeManagerPlugin<AspectSources.Page> {
    private final SourceCatalog catalog;
    SourceLookup(SourceCatalog catalog) { this.catalog = catalog; }
    public List<AspectSources.Page> getAllRecipes() { return catalog.pages(); }
    public boolean isHandledInput(ITypedIngredient<?> ingredient) { return !getRecipesForInput(ingredient).isEmpty(); }
    public boolean isHandledOutput(ITypedIngredient<?> ingredient) { return ingredient.getIngredient(AspectIngredient.TYPE).isPresent(); }
    public List<AspectSources.Page> getRecipesForInput(ITypedIngredient<?> ingredient) {
        if (!ClientLifecycle.ready()) return List.of();
        var stack = ingredient.getIngredient(VanillaTypes.ITEM_STACK);
        if (stack.isPresent()) return catalog.focused(stack.get());
        return ingredient.getIngredient(SourceToken.TYPE).map(token -> catalog.pages().stream()
            .filter(page -> page.sources().stream().anyMatch(source -> source.token() != null && source.token().key().equals(token.key()))).toList()).orElse(List.of());
    }
    public List<AspectSources.Page> getRecipesForOutput(ITypedIngredient<?> ingredient) {
        return ingredient.getIngredient(AspectIngredient.TYPE).map(aspect -> catalog.pages().stream()
            .filter(page -> page.aspect().equals(aspect.aspect())).toList()).orElse(List.of());
    }
}

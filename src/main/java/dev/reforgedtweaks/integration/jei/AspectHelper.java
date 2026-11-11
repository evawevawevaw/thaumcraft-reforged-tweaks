package dev.reforgedtweaks.integration.jei;

import dev.reforgedtweaks.client.data.AspectNames;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.ResourceLocation;

final class AspectHelper implements IIngredientHelper<AspectIngredient> {
    public IIngredientType<AspectIngredient> getIngredientType() { return AspectIngredient.TYPE; }
    public String getDisplayName(AspectIngredient value) { return AspectNames.searchable(value.aspect()); }
    public String getUniqueId(AspectIngredient value, UidContext context) { return "thaumcraft:" + value.aspect().getTag(); }
    public ResourceLocation getResourceLocation(AspectIngredient value) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", value.aspect().getTag()); }
    public AspectIngredient copyIngredient(AspectIngredient value) { return value; }
    public AspectIngredient normalizeIngredient(AspectIngredient value) { return new AspectIngredient(value.aspect()); }
    public long getAmount(AspectIngredient value) { return value.amount(); }
    public boolean isValidIngredient(AspectIngredient value) { return value != null && value.amount() > 0; }
    public String getErrorInfo(AspectIngredient value) { return String.valueOf(value); }
}

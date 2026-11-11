package dev.reforgedtweaks.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import thaumcraft.registry.ModBlocks;

final class CombinationCategory extends AbstractRecipeCategory<Combination> {
    CombinationCategory(IGuiHelper gui) {
        super(ReforgedJei.COMBINATIONS, Component.translatable("trt.category.combinations"), gui.createDrawableItemLike(thaumcraft.registry.ModItems.SCRIBING_TOOLS.get()), 125, 18);
    }
    public net.minecraft.resources.ResourceLocation getRegistryName(Combination recipe) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft_reforged_tweaks", "aspect_combination/" + recipe.result().getTag());
    }
    public void setRecipe(IRecipeLayoutBuilder builder, Combination recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 1).setStandardSlotBackground().addIngredient(AspectIngredient.TYPE, new AspectIngredient(recipe.first()));
        builder.addInputSlot(50, 1).setStandardSlotBackground().addIngredient(AspectIngredient.TYPE, new AspectIngredient(recipe.second()));
        builder.addOutputSlot(108, 1).setStandardSlotBackground().addIngredient(AspectIngredient.TYPE, new AspectIngredient(recipe.result()));
    }
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Combination recipe, IFocusGroup focuses) {
        builder.addRecipePlusSign(27, 3);
        builder.addRecipeArrow(76, 1);
    }
}

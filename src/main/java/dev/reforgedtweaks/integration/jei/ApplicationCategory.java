package dev.reforgedtweaks.integration.jei;

import dev.reforgedtweaks.client.data.InfusionApplications;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.registry.ModBlocks;

final class ApplicationCategory extends AbstractRecipeCategory<InfusionApplications.Application> {
    ApplicationCategory(IGuiHelper gui) {
        super(ReforgedJei.APPLICATIONS, Component.translatable("trt.category.applications"), gui.createDrawableItemLike(ModBlocks.RUNIC_MATRIX.get()), 166, 158);
    }
    public ResourceLocation getRegistryName(InfusionApplications.Application recipe) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft_reforged_tweaks", "application/" + InfusionApplications.id(recipe));
    }
    public void setRecipe(IRecipeLayoutBuilder builder, InfusionApplications.Application recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 1).setStandardSlotBackground().addItemStack(recipe.central());
        builder.addOutputSlot(145, 1).setOutputSlotBackground().addItemStack(recipe.result());
        var components = dev.reforgedtweaks.client.data.ApplicationLayout.components(recipe.plan());
        for (int i = 0; i < components.size(); i++) builder.addInputSlot(1 + i % 8 * 20, 40 + i / 8 * 20)
            .setStandardSlotBackground().addItemStacks(components.get(i));
        int i = 0;
        for (var aspect : recipe.plan().aspects().getAspectsSorted()) builder.addInputSlot(1 + i++ * 20, 116)
            .addIngredient(AspectIngredient.TYPE, new AspectIngredient(aspect, recipe.plan().aspects().getAmount(aspect)));
    }
    public void createRecipeExtras(IRecipeExtrasBuilder builder, InfusionApplications.Application recipe, IFocusGroup focuses) {
        builder.addRecipeArrow(72, 1);
        builder.addText(Component.translatable("trt.application.cost", recipe.plan().instability(), recipe.plan().xp()), 164, 20, 1, 138);
    }
}

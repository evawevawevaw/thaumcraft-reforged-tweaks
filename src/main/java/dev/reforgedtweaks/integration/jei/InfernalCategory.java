package dev.reforgedtweaks.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import thaumcraft.registry.ModBlocks;

final class InfernalCategory extends AbstractRecipeCategory<ExtraCatalog.Smelting> {
    InfernalCategory(IGuiHelper gui) {
        super(ReforgedJei.INFERNAL, Component.translatable("trt.category.infernal"), gui.createDrawableItemLike(net.minecraft.world.item.Items.NETHER_BRICKS), 150, 64);
    }
    public net.minecraft.resources.ResourceLocation getRegistryName(ExtraCatalog.Smelting recipe) {
        var original = net.minecraft.resources.ResourceLocation.parse(recipe.key());
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft_reforged_tweaks", "infernal/" + original.getNamespace() + "/" + original.getPath());
    }
    public void setRecipe(IRecipeLayoutBuilder builder, ExtraCatalog.Smelting recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 1).setStandardSlotBackground().addItemStack(recipe.input());
        builder.addOutputSlot(74, 1).setOutputSlotBackground().addItemStack(recipe.result());
        if (!recipe.bonus().isEmpty()) builder.addOutputSlot(114, 1).setStandardSlotBackground().addItemStack(recipe.bonus())
            .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable("trt.infernal.bonus")));
    }
    public void createRecipeExtras(IRecipeExtrasBuilder builder, ExtraCatalog.Smelting recipe, IFocusGroup focuses) {
        builder.addRecipeArrow(39, 1);
        if (!recipe.bonus().isEmpty()) builder.addText(Component.translatable("trt.infernal.bonus"), 148, 40, 0, 24);
    }
}

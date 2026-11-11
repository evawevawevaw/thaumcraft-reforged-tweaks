package dev.reforgedtweaks.integration.jei.mixin;

import dev.reforgedtweaks.integration.jei.AspectSlots;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "thaumcraft.client.jei.InfusionCategory", remap = false)
public abstract class TrtInfusionMixin {
    @Inject(method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Lthaumcraft/crafting/InfusionRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V", at = @At("TAIL"), require = 1, remap = false)
    private void trt$aspectSlots(IRecipeLayoutBuilder builder, thaumcraft.crafting.InfusionRecipe recipe, IFocusGroup focuses, CallbackInfo callback) {
        AspectSlots.append(builder, recipe.aspects(), 5, 130, 102);
    }
}

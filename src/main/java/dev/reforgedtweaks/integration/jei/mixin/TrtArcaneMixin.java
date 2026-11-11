package dev.reforgedtweaks.integration.jei.mixin;

import dev.reforgedtweaks.integration.jei.AspectSlots;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import dev.reforgedtweaks.integration.jei.RecipeCosts;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "thaumcraft.client.jei.ArcaneCategory", remap = false)
public abstract class TrtArcaneMixin {
    @Inject(method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Lthaumcraft/client/jei/ArcaneDisplay;Lmezz/jei/api/recipe/IFocusGroup;)V", at = @At("TAIL"), require = 1, remap = false)
    private void trt$aspectSlots(IRecipeLayoutBuilder builder, @Coerce Object recipe, IFocusGroup focuses, CallbackInfo callback) {
        AspectSlots.append(builder, RecipeCosts.read(recipe), 7, 130, 60);
    }
}

package dev.reforgedtweaks.integration.jei;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.menu.ArcaneWorkbenchMenu;
import thaumcraft.menu.ThaumatoriumMenu;
import thaumcraft.registry.ModMenus;

/** Resolve REFORGED types publicly; its non-public classes/fields are never loaded reflectively here. */
final class RecipeTransfers {
    private RecipeTransfers() {}
    static void register(IRecipeTransferRegistration registration) {
        registration.getJeiHelpers().getRecipeType(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane"))
            .ifPresent(type -> arcane(registration, type));
        registration.addRecipeTransferHandler(new ItemTransfer<>(registration.getTransferHelper(), ArcaneWorkbenchMenu.class,
            ModMenus.ARCANE_WORKBENCH.get(), RecipeTypes.CRAFTING, 2, 9, 11, 36), RecipeTypes.CRAFTING);
        registration.getJeiHelpers().getRecipeType(ResourceLocation.fromNamespaceAndPath("thaumcraft", "crucible"), CrucibleRecipe.class)
            .ifPresent(type -> registration.addRecipeTransferHandler(new ItemTransfer<>(registration.getTransferHelper(), ThaumatoriumMenu.class,
                ModMenus.THAUMATORIUM.get(), type, 0, 1, 1, 36), type));
    }
    private static <R> void arcane(IRecipeTransferRegistration registration, RecipeType<R> type) {
        registration.addRecipeTransferHandler(new ItemTransfer<>(registration.getTransferHelper(), ArcaneWorkbenchMenu.class,
            ModMenus.ARCANE_WORKBENCH.get(), type, 2, 9, 11, 36), type);
    }
}

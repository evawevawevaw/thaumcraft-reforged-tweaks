package dev.reforgedtweaks.integration.emi;

import java.util.List;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.*;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import thaumcraft.registry.ModMenus;

/** Move only physical ingredients; vis/essentia remain a prerequisite supplied by REFORGED. */
final class NativeTransfer<T extends AbstractContainerMenu> implements StandardRecipeHandler<T> {
    private final String category;
    private final int gridStart, gridCount, inventoryStart;
    private NativeTransfer(String category,int gridStart,int gridCount,int inventoryStart) {
        this.category=category; this.gridStart=gridStart; this.gridCount=gridCount; this.inventoryStart=inventoryStart;
    }
    static void register(EmiRegistry registry) {
        registry.addRecipeHandler(ModMenus.ARCANE_WORKBENCH.get(),new NativeTransfer<>("arcane",2,9,11));
        registry.addRecipeHandler(ModMenus.THAUMATORIUM.get(),new NativeTransfer<>("crucible",0,1,1));
    }
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe instanceof NativeRecipe && recipe.getCategory().id.getPath().equals(category);
    }
    public List<Slot> getInputSources(T menu) { return menu.slots.subList(gridStart,inventoryStart+36); }
    public List<Slot> getCraftingSlots(T menu) { return menu.slots.subList(gridStart,gridStart+gridCount); }
    public boolean canCraft(EmiRecipe recipe,EmiCraftContext<T> context) { return StandardRecipeHandler.super.canCraft(physical(recipe),context); }
    public boolean craft(EmiRecipe recipe,EmiCraftContext<T> context) { return StandardRecipeHandler.super.craft(physical(recipe),context); }
    private static EmiRecipe physical(EmiRecipe recipe) {
        return new EmiRecipe() {
            public dev.emi.emi.api.recipe.EmiRecipeCategory getCategory(){return recipe.getCategory();}
            public net.minecraft.resources.ResourceLocation getId(){return recipe.getId();}
            public List<EmiIngredient> getInputs(){return recipe.getInputs().stream().filter(i->i.getEmiStacks().stream().noneMatch(s->s instanceof AspectStack)).toList();}
            public List<dev.emi.emi.api.stack.EmiStack> getOutputs(){return recipe.getOutputs();}
            public int getDisplayWidth(){return recipe.getDisplayWidth();}
            public int getDisplayHeight(){return recipe.getDisplayHeight();}
            public void addWidgets(WidgetHolder widgets){recipe.addWidgets(widgets);}
        };
    }
}

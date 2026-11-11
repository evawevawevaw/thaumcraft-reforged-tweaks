package dev.reforgedtweaks.integration.jei;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import thaumcraft.device.SmeltingBonuses;

final class ExtraCatalog {
    record Smelting(String key, ItemStack input, ItemStack result, ItemStack bonus) {}
    private volatile List<Smelting> smelting = List.of();
    List<Smelting> smelting() { return smelting; }
    void clear() { smelting = List.of(); }
    void rebuild() {
        clear();
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        List<Smelting> built = new ArrayList<>();
        for (var holder : level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING)) {
            var recipe = holder.value();
            if (recipe.getIngredients().isEmpty()) continue;
            int index = 0;
            for (ItemStack input : recipe.getIngredients().getFirst().getItems()) {
                ItemStack result = recipe.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(input), level.registryAccess());
                if (result.isEmpty()) continue;
                var bonus = SmeltingBonuses.get(input);
                built.add(new Smelting(holder.id() + "/" + index++, input.copyWithCount(1), result.copy(),
                    bonus == null ? ItemStack.EMPTY : new ItemStack(bonus)));
            }
        }
        smelting = List.copyOf(built);
    }
}

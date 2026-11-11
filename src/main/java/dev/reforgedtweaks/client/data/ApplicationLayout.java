package dev.reforgedtweaks.client.data;

import java.util.*;
import net.minecraft.world.item.ItemStack;
import thaumcraft.crafting.InfusionCrafting;

/** Identical pedestal ingredients are counted together instead of drawing hundreds of overlapping slots. */
public final class ApplicationLayout {
    private ApplicationLayout() {}
    public static List<List<ItemStack>> components(InfusionCrafting.Plan plan) {
        List<List<ItemStack>> groups = new ArrayList<>();
        for (var ingredient : plan.components()) {
            List<ItemStack> choices = ingredient.displayStacks().stream().filter(s -> !s.isEmpty()).map(s -> s.copyWithCount(1)).toList();
            if (choices.isEmpty()) continue;
            List<ItemStack> existing = groups.stream().filter(group -> equal(group, choices)).findFirst().orElse(null);
            if (existing == null) groups.add(choices);
            else existing.forEach(stack -> stack.grow(1));
        }
        return groups;
    }
    private static boolean equal(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) if (!ItemStack.isSameItemSameComponents(a.get(i), b.get(i))) return false;
        return true;
    }
}

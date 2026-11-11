package dev.reforgedtweaks.client.data;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import thaumcraft.crafting.*;
import thaumcraft.infusion.InfusionHooks;
import thaumcraft.infusion.InfusionKeys;

/** Lookup of infusion applications:
 * generate concrete applications, retaining components, and let REFORGED calculate every result/cost. */
public final class InfusionApplications {
    public record Application(ItemStack central, ItemStack result, InfusionCrafting.Plan plan) {}
    private volatile List<Application> recipes = List.of();
    public List<Application> all() { return recipes; }
    public void clear() { recipes = List.of(); }

    public void rebuild(Iterable<ItemStack> variants) {
        List<Application> built = new ArrayList<>();
        Map<String, Application> unique = new LinkedHashMap<>();
        for (var item : BuiltInRegistries.ITEM) {
            ItemStack stack = item.getDefaultInstance();
            built.addAll(input(stack));
            if (InfusionHooks.isRunicArmor(stack)) for (int hardening = 1; hardening < 10; hardening++) {
                ItemStack next = stack.copy();
                InfusionKeys.HARDEN.set(next, hardening);
                built.addAll(input(next));
            }
            var level = Minecraft.getInstance().level;
            if (level != null && stack.isDamageableItem() && stack.getMaxStackSize() == 1) {
                for (var recipe : TcRecipes.all(InfusionEnchantmentRecipe.class)) {
                    var holder = InfusionCrafting.enchantment(level, recipe.enchantment());
                    if (holder == null || !holder.value().isSupportedItem(stack)) continue;
                    for (int rank = 1; rank < holder.value().getMaxLevel(); rank++) {
                        ItemStack next = stack.copy();
                        int current = rank;
                        EnchantmentHelper.updateEnchantments(next, value -> value.set(holder, current));
                        addEnchantment(built, next, recipe);
                    }
                }
            }
        }
        for (ItemStack stack : variants) built.addAll(input(stack));
        for (Application recipe : built) unique.putIfAbsent(id(recipe), recipe);
        recipes = List.copyOf(unique.values());
    }

    public List<Application> input(ItemStack stack) {
        if (stack.isEmpty() || Minecraft.getInstance().level == null) return List.of();
        List<Application> result = new ArrayList<>();
        for (var recipe : TcRecipes.all(InfusionRecipe.class)) {
            if (InfusionRunicAugmentRecipe.isDisplayRecipe(recipe.key()) || !InfusionCrafting.isTagResult(recipe.result())) continue;
            if (recipe.central().test(stack)) add(result, stack, InfusionCrafting.TYPE_RECIPE, recipe.key());
        }
        // Prevent pathological component allocation for corrupt or excessively large hardening values.
        if (InfusionHooks.isRunicArmor(stack) && InfusionHooks.getFinalCharge(stack) >= 0 && InfusionHooks.getFinalCharge(stack) <= 256)
            add(result, stack, InfusionCrafting.TYPE_RUNIC, InfusionRunicAugmentRecipe.KEY);
        for (var recipe : TcRecipes.all(InfusionEnchantmentRecipe.class)) addEnchantment(result, stack, recipe);
        return result;
    }

    public List<Application> output(ItemStack target) {
        List<Application> found = new ArrayList<>();
        for (Application recipe : recipes) if (same(recipe.result(), target)) found.add(recipe);
        if (target.isEmpty()) return found;
        int hardening = InfusionHooks.getHardening(target);
        if (InfusionHooks.isRunicArmor(target) && hardening > 0) {
            ItemStack previous = target.copy();
            InfusionKeys.HARDEN.set(previous, hardening - 1);
            for (Application recipe : input(previous)) if (same(recipe.result(), target)) found.add(recipe);
        }
        var level = Minecraft.getInstance().level;
        if (level == null) return found;
        for (var recipe : TcRecipes.all(InfusionEnchantmentRecipe.class)) {
            var holder = InfusionCrafting.enchantment(level, recipe.enchantment());
            if (holder == null) continue;
            int rank = InfusionCrafting.enchantmentLevel(target, holder);
            if (rank <= 0) continue;
            ItemStack previous = target.copy();
            EnchantmentHelper.updateEnchantments(previous, value -> value.set(holder, rank - 1));
            List<Application> candidates = new ArrayList<>();
            addEnchantment(candidates, previous, recipe);
            for (Application candidate : candidates) if (same(candidate.result(), target)) found.add(candidate);
        }
        for (var recipe : TcRecipes.all(InfusionRecipe.class)) {
            if (InfusionRunicAugmentRecipe.isDisplayRecipe(recipe.key()) || !InfusionCrafting.isTagResult(recipe.result())) continue;
            var tags = new HashMap<>(InfusionKeys.TAGS.getOrDefault(target, Map.of()));
            if (!recipe.result().data().entrySet().stream().allMatch(entry -> Objects.equals(tags.get(entry.getKey()), entry.getValue()))) continue;
            recipe.result().data().keySet().forEach(tags::remove);
            ItemStack previous = target.copy(); InfusionKeys.TAGS.set(previous, tags);
            if (!recipe.central().test(previous)) continue;
            List<Application> candidates = new ArrayList<>();
            add(candidates, previous, InfusionCrafting.TYPE_RECIPE, recipe.key());
            for (Application candidate : candidates) if (same(candidate.result(), target)) found.add(candidate);
        }
        return found.stream().collect(java.util.stream.Collectors.toMap(InfusionApplications::id, r -> r, (a,b) -> a, LinkedHashMap::new)).values().stream().toList();
    }

    private static void addEnchantment(List<Application> result, ItemStack stack, InfusionEnchantmentRecipe recipe) {
        var level = Minecraft.getInstance().level;
        if (level == null || stack.isEmpty() || !stack.isDamageableItem() || stack.getMaxStackSize() != 1) return;
        var holder = InfusionCrafting.enchantment(level, recipe.enchantment());
        if (holder == null || !holder.value().isSupportedItem(stack)) return;
        var existing = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (existing.getLevel(holder) >= holder.value().getMaxLevel()) return;
        for (var other : existing.keySet()) if (!other.equals(holder)
            && (!Enchantment.areCompatible(holder, other) || !Enchantment.areCompatible(other, holder))) return;
        add(result, stack, InfusionCrafting.TYPE_ENCHANTMENT, recipe.key());
    }
    private static void add(List<Application> result, ItemStack central, int type, String key) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var plan = InfusionCrafting.resolve(level, type, key, central);
        if (plan == null || plan.components().stream().anyMatch(part -> !part.exists())) return;
        ItemStack output = InfusionCrafting.result(level, type, key, central);
        if (!output.isEmpty() && !same(central, output)) result.add(new Application(central.copyWithCount(1), output.copyWithCount(1), plan));
    }
    public static boolean same(ItemStack a, ItemStack b) { return ItemStack.isSameItemSameComponents(a, b); }
    public static String id(Application recipe) {
        var json = new com.google.gson.JsonObject();
        json.addProperty("type", recipe.plan().type());
        json.addProperty("key", recipe.plan().key());
        var level = Minecraft.getInstance().level;
        json.add("central", StackIdentity.encode(recipe.central(), level.registryAccess()));
        json.add("result", StackIdentity.encode(recipe.result(), level.registryAccess()));
        return RecipeIds.digest(json);
    }
}

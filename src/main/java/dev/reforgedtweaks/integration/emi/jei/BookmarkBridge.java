package dev.reforgedtweaks.integration.emi.jei;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.jemi.JemiRecipe;
import dev.reforgedtweaks.ReforgedTweaks;
import dev.reforgedtweaks.integration.emi.StableRecipeId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.LoggerFactory;
import thaumcraft.crafting.TcRecipe;

/** Exact-version compatibility with EMI 1.1.24's public JemiRecipe.id field; no reflective access. */
public final class BookmarkBridge {
    private static final Set<String> REFORGED = Set.of("crucible", "arcane", "infusion", "infusion_enchantment");
    private static final Set<String> ADDON = Set.of("aspect_sources", "aspect_combination", "infernal_smelting");
    private BookmarkBridge() {}

    public static void register(EmiRegistry registry) {
        AtomicBoolean announced = new AtomicBoolean();
        Set<ResourceLocation> warned = java.util.concurrent.ConcurrentHashMap.newKeySet();
        // EMI executes removal predicates before building the recipe-ID index (EmiRecipes.bake).
        // Keep each existing recipe and only fill its missing identity; category/widget/transfer objects stay intact.
        registry.removeRecipes(recipe -> {
            ResourceLocation category = recipe.getCategory().getId();
            if (!(recipe instanceof JemiRecipe<?> imported)) return false;
            // Real applications are registered natively; do not keep misleading books or runic/tag placeholders.
            if (category.getNamespace().equals("thaumcraft") && category.getPath().equals("infusion_enchantment")) return true;
            if (imported.recipe instanceof thaumcraft.crafting.InfusionRecipe infusion
                && (thaumcraft.crafting.InfusionRunicAugmentRecipe.isDisplayRecipe(infusion.key())
                    || thaumcraft.crafting.InfusionCrafting.isTagResult(infusion.result()))) return true;
            if (category.getNamespace().equals(ReforgedTweaks.MOD_ID + "_jei") && category.getPath().equals("aspect_sources")) {
                // A reference page lists alternative sources, not a craft consuming every displayed item.
                imported.allowTree = false;
            }
            if (imported.id != null || !handled(category)) return false;
            try {
                JsonObject identity = new JsonObject();
                identity.addProperty("category", category.toString());
                if (imported.recipe instanceof TcRecipe original) identity.addProperty("key", original.key());
                identity.add("inputs", ingredients(imported.getInputs()));
                identity.add("catalysts", ingredients(imported.getCatalysts()));
                identity.add("outputs", ingredients(imported.getOutputs()));
                imported.id = ResourceLocation.fromNamespaceAndPath(ReforgedTweaks.MOD_ID,
                    "emi/" + category.getNamespace() + "/" + category.getPath() + "/" + StableRecipeId.digest(identity));
                if (announced.compareAndSet(false, true)) LoggerFactory.getLogger(BookmarkBridge.class)
                    .info("EMI recipe bookmark IDs enabled for REFORGED (EMI 1.1.24); first recipe: {}", imported.id);
            } catch (RuntimeException exception) {
                if (warned.add(category)) LoggerFactory.getLogger(BookmarkBridge.class)
                    .warn("Cannot assign a persistent EMI bookmark ID for {}", category, exception);
            }
            return false;
        });
    }

    private static boolean handled(ResourceLocation category) {
        return category.getNamespace().equals("thaumcraft") && REFORGED.contains(category.getPath())
            || category.getNamespace().equals(ReforgedTweaks.MOD_ID + "_jei") && ADDON.contains(category.getPath());
    }

    private static JsonArray ingredients(List<? extends EmiIngredient> ingredients) {
        JsonArray result = new JsonArray();
        for (EmiIngredient ingredient : ingredients) {
            JsonObject entry = new JsonObject();
            entry.addProperty("amount", ingredient.getAmount());
            JsonArray options = new JsonArray();
            for (EmiStack stack : ingredient.getEmiStacks()) {
                JsonObject option = new JsonObject();
                option.addProperty("id", stack.getId().toString());
                option.addProperty("amount", stack.getAmount());
                // Serialize native item stacks so wand/potion/enchantment components distinguish recipe variants.
                if (!stack.getItemStack().isEmpty()) {
                    var level = java.util.Objects.requireNonNull(net.minecraft.client.Minecraft.getInstance().level, "EMI import requires a client world");
                    // Use structured components, not EMI's SNBT string form whose map order may change between launches.
                    var serialized = dev.reforgedtweaks.client.data.StackIdentity.encode(stack.getItemStack(), level.registryAccess());
                    option.add("item", serialized);
                }
                options.add(option);
            }
            // Ingredient alternatives are unordered, but grid slots and outputs retain their order.
            JsonArray sorted = new JsonArray();
            java.util.stream.StreamSupport.stream(options.spliterator(), false)
                .sorted(java.util.Comparator.comparing(StableRecipeId::digest)).forEach(sorted::add);
            entry.add("options", sorted);
            result.add(entry);
        }
        return result;
    }
}

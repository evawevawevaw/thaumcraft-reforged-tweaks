package dev.reforgedtweaks.integration.emi;

import java.util.ArrayList;
import java.util.List;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import witchinggadgets.recipeview.WgRecipeCatalog;
import static dev.reforgedtweaks.integration.emi.NativeRecipe.*;

/** Optional public-API bridge for Witching Gadgets 1.21.1. Loaded only without JEI. */
final class WitchingGadgetsCatalog {
    private WitchingGadgetsCatalog() {}

    static void register(EmiRegistry registry) {
        var wheel = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("witchinggadgets", "spinning_wheel"));
        var spinning = NativeCatalog.category(registry, "wg_spinning", "trt.category.wg_spinning", wheel);
        var blast = NativeCatalog.category(registry, "wg_blast", "trt.category.wg_blast", Items.NETHER_BRICKS);
        if (wheel != Items.AIR) registry.addWorkstation(spinning, EmiStack.of(wheel));

        // Machine recipes do not depend on the baked EMI index. Register immediately,
        // independently from late source-index callbacks. The addon resolves optional tags.
        var spinningRecipes = WgRecipeCatalog.spinning();
        var blastRecipes = WgRecipeCatalog.blastFurnace();
        int spinningCount = 0, blastCount = 0;
        var logger = org.slf4j.LoggerFactory.getLogger(WitchingGadgetsCatalog.class);
        for (var recipe : spinningRecipes) {
            try {
            var slots = new ArrayList<Positioned>();
            for (int i = 0; i < recipe.inputs().size(); i++) {
                slots.add(input(NativeCatalog.choices(recipe.inputs().get(i).displayStacks()), i < 5 ? 15 : 15 + (i-5)%8*20, i < 5 ? 15 + i*24 : 148+(i-5)/8*20));
            }
            int height = recipe.inputs().size() <= 5 ? 144 : 152 + ((recipe.inputs().size()-5+7)/8)*20;
            slots.add(output(EmiStack.of(recipe.output().create()), 141, 63));
            registry.addRecipe(new NativeRecipe(spinning, recipe.key(), slots, List.of(), height, true,
                new RecipePresentation(RecipePresentation.Kind.SPINNING, 0)));
            spinningCount++;
            } catch (RuntimeException failure) {
                logger.error("Cannot register native EMI spinning recipe {}", recipe.key(), failure);
            }
        }
        for (var recipe : blastRecipes) {
            try {
            var slots = new ArrayList<Positioned>();
            slots.add(input(NativeCatalog.choices(recipe.inputs()), 50, 40));
            slots.add(output(EmiStack.of(recipe.output()), 70, 7));
            if (!recipe.bonus().isEmpty()) {
                // EMI's recipe tree uses the unmodified furnace's 25% baseline.
                // Bellows can yield multiple independent bonuses; describe that explicitly.
                slots.add(new Positioned(EmiStack.of(recipe.bonus()).setChance(0.25F), 98, 40, true,
                    List.of(Component.translatable("trt.wg.bonus"))));
            }
            var text = new ArrayList<Component>();
            text.add(Component.translatable("trt.wg.time", recipe.time()));
            if (recipe.special()) text.add(Component.translatable("trt.wg.special"));
            registry.addRecipe(new NativeRecipe(blast, recipe.key() + "/" + recipe.time() + "/" + recipe.special(),
                slots, text, 72 + text.size() * 11, true,
                new RecipePresentation(RecipePresentation.Kind.FURNACE, 0)));
            blastCount++;
            } catch (RuntimeException failure) {
                logger.error("Cannot register native EMI blast recipe {}", recipe.key(), failure);
            }
        }
        logger.info("Witching Gadgets native EMI registered: spinning={}/{}, blast={}/{}",
            spinningCount, spinningRecipes.size(), blastCount, blastRecipes.size());
        if (spinningRecipes.isEmpty() || blastRecipes.isEmpty()) {
            logger.warn("Witching Gadgets returned an empty native EMI catalogue; check addon version and item/tag availability");
        }
    }
}

package dev.reforgedtweaks.integration.jei;

import dev.reforgedtweaks.client.data.AspectSources;
import dev.reforgedtweaks.client.data.InfusionApplications;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import dev.reforgedtweaks.ReforgedTweaks;
import dev.reforgedtweaks.client.ClientLifecycle;
import dev.reforgedtweaks.client.data.AspectNames;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.aspect.Aspect;
import thaumcraft.registry.ModItems;

@JeiPlugin
public final class ReforgedJei implements IModPlugin {
    // EMI treats native plugins as replacements for JEI plugins/categories in the owning mod namespace.
    // This companion JEI backend remains imported alongside the narrow EMI bookmark plugin.
    public static final String NAMESPACE = ReforgedTweaks.MOD_ID + "_jei";
    static final Logger LOGGER = LoggerFactory.getLogger(ReforgedJei.class);
    static final RecipeType<Combination> COMBINATIONS = RecipeType.create(NAMESPACE, "aspect_combination", Combination.class);
    static final RecipeType<AspectSources.Page> SOURCES = RecipeType.create(NAMESPACE, "aspect_sources", AspectSources.Page.class);
    static final RecipeType<ExtraCatalog.Smelting> INFERNAL = RecipeType.create(NAMESPACE, "infernal_smelting", ExtraCatalog.Smelting.class);
    static final RecipeType<InfusionApplications.Application> APPLICATIONS = RecipeType.create(ReforgedTweaks.MOD_ID, "infusion_applications", InfusionApplications.Application.class);
    private final InfusionApplications applications = new InfusionApplications();
    private final SourceCatalog sources = new SourceCatalog();
    private final ExtraCatalog extras = new ExtraCatalog();
    private record RuntimeSession(IJeiRuntime runtime) {}
    private volatile RuntimeSession session;
    private final Runnable refreshListener = this::refresh;
    private boolean refreshing;

    public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(NAMESPACE, "plugin"); }
    private static List<AspectIngredient> ingredients() { return Arrays.stream(Aspect.values()).map(AspectIngredient::new).toList(); }

    public void registerIngredients(IModIngredientRegistration registration) {
        AspectNames.reload();
        registration.register(AspectIngredient.TYPE, ingredients(), new AspectHelper(), AspectRenderer.VISIBLE, AspectIngredient.CODEC);
        SourceTokenHelper targets = new SourceTokenHelper();
        registration.register(SourceToken.TYPE, List.of(), targets, targets, SourceToken.CODEC);
    }
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new CombinationCategory(gui), new SourceCategory(gui), new InfernalCategory(gui), new ApplicationCategory(gui));
    }
    public void registerAdvanced(IAdvancedRegistration registration) {
        registration.addTypedRecipeManagerPlugin(APPLICATIONS, new ApplicationLookup(applications));
        registration.addTypedRecipeManagerPlugin(SOURCES, new SourceLookup(sources));
        registration.addTypedRecipeManagerPlugin(COMBINATIONS, new CatalogLookup<>(
            () -> ClientLifecycle.ready() ? Combination.all() : List.of(),
            (recipe, ingredient) -> ingredient.getIngredient(AspectIngredient.TYPE).map(aspect -> aspect.aspect().equals(recipe.first()) || aspect.aspect().equals(recipe.second())).orElse(false),
            (recipe, ingredient) -> ingredient.getIngredient(AspectIngredient.TYPE).map(aspect -> aspect.aspect().equals(recipe.result())).orElse(false)));
        registration.addTypedRecipeManagerPlugin(INFERNAL, new CatalogLookup<>(extras::smelting,
            (recipe, ingredient) -> item(ingredient, recipe.input()),
            (recipe, ingredient) -> item(ingredient, recipe.result()) || item(ingredient, recipe.bonus())));

    }
    private static boolean item(ITypedIngredient<?> ingredient, ItemStack stack) {
        return !stack.isEmpty() && ingredient.getIngredient(VanillaTypes.ITEM_STACK).map(focus -> ItemStack.isSameItemSameComponents(focus, stack)).orElse(false);
    }
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(COMBINATIONS, ModItems.SCRIBING_TOOLS.get());
        registration.addRecipeCatalysts(SOURCES, ModItems.THAUMOMETER.get());
        registration.addRecipeCatalysts(APPLICATIONS, thaumcraft.registry.ModBlocks.RUNIC_MATRIX.get());
    }
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        RecipeTransfers.register(registration);
    }
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        RuntimeSession owner = new RuntimeSession(runtime);
        session = owner;
        runtime.getIngredientManager().registerIngredientListener(new IIngredientManager.IIngredientListener() {
            public <V> void onIngredientsAdded(IIngredientHelper<V> helper, Collection<ITypedIngredient<V>> values) {
                if (active(owner) && helper.getIngredientType() == VanillaTypes.ITEM_STACK) ClientLifecycle.resourcesChanged();
            }
            public <V> void onIngredientsRemoved(IIngredientHelper<V> helper, Collection<ITypedIngredient<V>> values) {
                if (active(owner) && helper.getIngredientType() == VanillaTypes.ITEM_STACK) ClientLifecycle.resourcesChanged();
            }
        });
        // No second Minecraft.execute task: startup and invalidations share the next client tick.
        ClientLifecycle.listen(refreshListener);
    }

    private void refresh() {
        RuntimeSession owner = session;
        if (owner == null || !active(owner)) return;
        if (refreshing) {
            ClientLifecycle.resourcesChanged();
            return;
        }
        refreshing = true;
        try {
            refresh(owner);
        } catch (RuntimeException exception) {
            // Do not let an integration failure escape into ClientTickEvent/Minecraft.tick.
            // No automatic retry: a new sync/resource/runtime invalidation can request another attempt.
            if (active(owner)) LOGGER.error("JEI aspect refresh failed; waiting for the next data/runtime invalidation", exception);
            else LOGGER.debug("Discarded a refresh for an unavailable JEI runtime", exception);
        } finally {
            refreshing = false;
        }
    }

    private void refresh(RuntimeSession owner) {
        IJeiRuntime runtime = owner.runtime();
        var manager = runtime.getIngredientManager();
        AspectNames.reload();
        List<AspectIngredient> nextAspects = ingredients();
        if (ClientLifecycle.ready()) {
            sources.rebuild(manager.getAllIngredients(VanillaTypes.ITEM_STACK));
            extras.rebuild();
            applications.rebuild(manager.getAllIngredients(VanillaTypes.ITEM_STACK));
            hideTemplates(runtime);
        } else {
            sources.clear();
            extras.clear();
            applications.clear();
        }
        List<SourceToken> nextTargets = List.copyOf(SourceToken.CURRENT.values());
        if (!replace(owner, AspectIngredient.TYPE, nextAspects)) return;
        if (!replace(owner, SourceToken.TYPE, nextTargets)) return;
        for (RecipeType<?> type : List.of(SOURCES, COMBINATIONS, INFERNAL, APPLICATIONS)) {
            if (!active(owner)) return;
            unhide(runtime, type);
        }
        LOGGER.info("JEI aspect index refreshed: ready={}, aspects={}, source pages={}, scan targets={}",
            ClientLifecycle.ready(), nextAspects.size(), sources.pages().size(), nextTargets.size());
    }

    // Public JEI API only: hide REFORGED's display templates, keep its real recipes/categories.
    private static void hideTemplates(IJeiRuntime runtime) {
        var manager = runtime.getRecipeManager();
        manager.getRecipeType(ResourceLocation.fromNamespaceAndPath("thaumcraft", "infusion"), thaumcraft.crafting.InfusionRecipe.class)
            .ifPresent(type -> manager.hideRecipes(type, manager.createRecipeLookup(type).includeHidden().get().filter(recipe ->
                thaumcraft.crafting.InfusionRunicAugmentRecipe.isDisplayRecipe(recipe.key()) || thaumcraft.crafting.InfusionCrafting.isTagResult(recipe.result())).toList()));
        manager.getRecipeType(ResourceLocation.fromNamespaceAndPath("thaumcraft", "infusion_enchantment"))
            .ifPresent(type -> hideAll(runtime, type));
    }
    private static <T> void hideAll(IJeiRuntime runtime, RecipeType<T> type) {
        var manager = runtime.getRecipeManager();
        manager.hideRecipes(type, manager.createRecipeLookup(type).includeHidden().get().toList());
    }

    private <T> boolean replace(RuntimeSession owner, mezz.jei.api.ingredients.IIngredientType<T> type, List<T> next) {
        var manager = owner.runtime().getIngredientManager();
        return IngredientReplacement.replace(() -> manager.getAllIngredients(type), next,
            current -> manager.removeIngredientsAtRuntime(type, current),
            added -> manager.addIngredientsAtRuntime(type, added), () -> active(owner));
    }

    private boolean active(RuntimeSession owner) {
        return session == owner && ClientLifecycle.owns(refreshListener);
    }

    private static <T> void unhide(IJeiRuntime runtime, RecipeType<T> type) {
        runtime.getRecipeManager().unhideRecipes(type, List.of());
    }

    public void onRuntimeUnavailable() {
        session = null;
        boolean ownedListener = ClientLifecycle.unlisten(refreshListener);
        sources.discard();
        extras.clear();
            applications.clear();
        // A late callback from an old plugin instance must not clear the new instance's shared tokens.
        if (ownedListener) SourceToken.CURRENT.clear();
    }
}

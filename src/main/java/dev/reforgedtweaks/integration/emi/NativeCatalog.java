package dev.reforgedtweaks.integration.emi;

import java.util.*;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.*;
import dev.reforgedtweaks.client.data.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import thaumcraft.aspect.*;
import thaumcraft.crafting.*;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModItems;
import thaumcraft.wand.WandRegistry;
import static dev.reforgedtweaks.integration.emi.NativeRecipe.*;

/** Native EMI backend; no JEI classes or JEI display records are loaded here. */
final class NativeCatalog {
    private NativeCatalog() {}
    static EmiRecipeCategory category(EmiRegistry registry, String id, String title, net.minecraft.world.level.ItemLike icon) {
        var category = new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath("thaumcraft_reforged_tweaks", id), EmiStack.of(icon)) {
            public Component getName() { return Component.translatable(title); }
        };
        registry.addCategory(category); return category;
    }
    static void register(EmiRegistry registry, boolean standalone, java.util.function.BiFunction<Aspect,Integer,EmiIngredient> aspectFactory) {
        var variants = new ArrayList<ItemStack>();
        for (var item : BuiltInRegistries.ITEM) variants.add(item.getDefaultInstance());
        for (var stack : dev.emi.emi.api.EmiApi.getIndexStacks()) if (!stack.getItemStack().isEmpty()) variants.add(stack.getItemStack());
        var applications = new InfusionApplications(); applications.rebuild(variants);
        var applicationCategory = category(registry, "infusion_applications", "trt.category.applications", ModBlocks.RUNIC_MATRIX.get());
        registry.addWorkstation(applicationCategory, EmiStack.of(ModBlocks.RUNIC_MATRIX.get()));
        for (var recipe : applications.all()) {
            registry.setDefaultComparison(recipe.central().getItem(), Comparison.compareComponents());
            var components = ApplicationLayout.components(recipe.plan());
            var layout = InfusionGeometry.of(components.size());
            List<Positioned> slots = new ArrayList<>();
            slots.add(input(EmiStack.of(recipe.central()), InfusionGeometry.CENTER_X, InfusionGeometry.CENTER_Y));
            slots.add(output(EmiStack.of(recipe.result()), InfusionGeometry.OUTPUT_X, InfusionGeometry.OUTPUT_Y));
            for (int i = 0; i < components.size(); i++) {
                var point = layout.pedestals().get(i);
                slots.add(input(choices(components.get(i)), point.x(), point.y()));
            }
            aspects(slots, recipe.plan().aspects(), layout.aspectY(), aspectFactory);
            registry.addRecipe(new NativeRecipe(applicationCategory, InfusionApplications.id(recipe), slots,
                List.of(Component.translatable("tc.inst." + Math.min(5, recipe.plan().instability()/2)),
                    Component.translatable("trt.recipe.xp", recipe.plan().xp())),
                layout.height(recipe.plan().aspects().size()) + 11, true,
                new RecipePresentation(RecipePresentation.Kind.INFUSION, layout.aspectY())));
        }
        if (!standalone) return;
        AspectNames.reload();
        var combinations = category(registry,"aspect_combination","trt.category.combinations",ModItems.SCRIBING_TOOLS.get());
        var sources = category(registry,"aspect_sources","trt.category.sources",ModItems.THAUMOMETER.get());
        for (Aspect aspect : Aspect.values()) {
            var stack = new AspectStack(aspect,1); registry.addEmiStack(stack);
            registry.addAlias(stack, Component.literal(AspectNames.searchable(aspect)));
            var parents = aspect.getComponents();
            if (parents != null && parents.length == 2) registry.addRecipe(new NativeRecipe(combinations, aspect.getTag(),
                List.of(input(new AspectStack(parents[0],1),76,12),input(new AspectStack(parents[1],1),120,12),output(stack,20,8)),List.of(),100,true,
                new RecipePresentation(RecipePresentation.Kind.COMBINATION, 0)));
        }
        registry.addDeferredRecipes(consumer -> {
        var indexed = new ArrayList<ItemStack>(variants);
        for (var stack : dev.emi.emi.api.EmiApi.getIndexStacks()) if (!stack.getItemStack().isEmpty()) indexed.add(stack.getItemStack());
        var sourceData = new AspectSources();
        if (dev.reforgedtweaks.client.ClientLifecycle.ready()) sourceData.rebuild(indexed);
        for (var page : sourceData.pages()) {
            var slots = new ArrayList<Positioned>(); slots.add(output(new AspectStack(page.aspect(),1),0,0));
            for (int i = 0; i < page.sources().size(); i++) {
                var entry = page.sources().get(i);
                EmiIngredient ingredient = entry.token() == null ? EmiStack.of(entry.stack()) : new ScanStack(entry.token());
                var tip = new ArrayList<Component>();
                tip.add(Component.translatable("trt.aspect.amount", entry.aspects().getAmount(page.aspect())));
                tip.add(Component.translatable(entry.token() == null ? "trt.source.dissolve" : "trt.source.scan"));
                for (var a : entry.aspects().getAspectsSorted()) tip.add(Component.translatable("trt.source.content",a.getName(),a.getDescription(),entry.aspects().getAmount(a)));
                slots.add(new Positioned(ingredient, SourcePagination.x(i), SourcePagination.y(i),false,tip));
            }
            consumer.accept(new NativeRecipe(sources,page.aspect().getTag()+"/"+page.index(),slots,
                List.of(Component.literal(page.aspect().getName()+" ").append(page.aspect().getDescription()),
                    Component.translatable("trt.source.page",page.index()+1,page.count(),page.total())),140,false));
        }
        });
        var arcane = category(registry,"arcane","recipe.type.arcane",ModBlocks.ARCANE_WORKTABLE.get());
        var crucible = category(registry,"crucible","recipe.type.crucible",ModBlocks.CRUCIBLE.get());
        var infusion = category(registry,"infusion","recipe.type.infusion",ModBlocks.RUNIC_MATRIX.get());
        registry.addWorkstation(arcane,EmiStack.of(ModBlocks.ARCANE_WORKTABLE.get()));
        registry.addWorkstation(crucible,EmiStack.of(ModBlocks.CRUCIBLE.get()));
        registry.addWorkstation(infusion,EmiStack.of(ModBlocks.RUNIC_MATRIX.get()));
        for (var recipe : TcRecipes.all(ArcaneRecipe.class)) {
            if (!recipe.result().exists()) continue;
            if (recipe instanceof ShapedArcaneRecipe shaped) arcane(registry,arcane,recipe.key(),shaped.grid(),shaped.width(),recipe.result().create(),recipe.aspects());
            else arcane(registry,arcane,recipe.key(),((ShapelessArcaneRecipe)recipe).ingredients(),3,recipe.result().create(),recipe.aspects());
        }
        registry.setDefaultComparison(ModItems.WAND.get(), Comparison.compareData(stack -> thaumcraft.wand.Wands.getParts(stack.getItemStack())));
        // REFORGED ArcaneDisplay.wandRecipes, adapted into our own display model; no private access.
        for (var cap : WandRegistry.caps()) for (var rod : WandRegistry.rods()) for (boolean sceptre : new boolean[]{false,true}) {
            var grid = new ArrayList<ItemStack>(Collections.nCopies(9,ItemStack.EMPTY));
            var ingredients = new ArrayList<TcIngredient>(Collections.nCopies(9,TcIngredient.EMPTY));
            int[] positions = sceptre ? new int[]{1,5,6} : new int[]{2,6};
            for (int slot : positions) { ingredients.set(slot,TcIngredient.item(cap.item().toString())); grid.set(slot,new ItemStack(BuiltInRegistries.ITEM.get(cap.item()))); }
            ingredients.set(4,TcIngredient.item(rod.item().toString())); grid.set(4,new ItemStack(BuiltInRegistries.ITEM.get(rod.item())));
            if (sceptre) { ingredients.set(2,TcIngredient.item("thaumcraft:primal_charm")); grid.set(2,new ItemStack(ModItems.PRIMAL_CHARM.get())); }
            DynamicArcaneRecipe recipe = sceptre ? new ArcaneSceptreRecipe() : new ArcaneWandRecipe();
            var result = recipe.result(grid);
            if (!result.isEmpty()) {
                registry.addEmiStack(EmiStack.of(result));
                arcane(registry,arcane,"wand/"+cap.item()+"/"+rod.item()+"/"+sceptre,ingredients,3,result,recipe.aspects(grid));
            }
        }
        for (var recipe : TcRecipes.all(CrucibleRecipe.class)) {
            if (!recipe.result().exists() || !recipe.catalyst().exists()) continue;
            var slots = new ArrayList<Positioned>(); slots.add(input(choices(recipe.catalyst().displayStacks()),50,29));
            slots.add(output(EmiStack.of(recipe.result().create()),70,7));
            crucibleAspects(slots,recipe.aspects());
            registry.addRecipe(new NativeRecipe(crucible,recipe.key(),slots,List.of(),
                Math.max(138, 138 + ((Math.max(0,recipe.aspects().size()-9)+2)/3)*20),true,
                new RecipePresentation(RecipePresentation.Kind.CRUCIBLE,0)));
        }
        for (var recipe : TcRecipes.all(InfusionRecipe.class)) {
            if (!recipe.result().exists() || InfusionRunicAugmentRecipe.isDisplayRecipe(recipe.key()) || InfusionCrafting.isTagResult(recipe.result())) continue;
            var layout = InfusionGeometry.of(recipe.components().size());
            var slots = new ArrayList<Positioned>();
            slots.add(input(choices(recipe.central().displayStacks()), InfusionGeometry.CENTER_X, InfusionGeometry.CENTER_Y));
            slots.add(output(EmiStack.of(recipe.result().create()), InfusionGeometry.OUTPUT_X, InfusionGeometry.OUTPUT_Y));
            for (int i = 0; i < recipe.components().size(); i++) {
                var point = layout.pedestals().get(i);
                slots.add(input(choices(recipe.components().get(i).displayStacks()), point.x(), point.y()));
            }
            aspects(slots,recipe.aspects(),layout.aspectY());
            registry.addRecipe(new NativeRecipe(infusion,recipe.key(),slots,
                List.of(Component.translatable("tc.inst." + Math.min(5,recipe.instability()/2))),
                layout.height(recipe.aspects().size()),true,
                new RecipePresentation(RecipePresentation.Kind.INFUSION,layout.aspectY())));
        }
        infernal(registry);
        NativeTransfer.register(registry);
    }
    private static void arcane(EmiRegistry registry, EmiRecipeCategory category,String key,List<TcIngredient> ingredients,int width,ItemStack result,AspectList cost) {
        var slots = new ArrayList<Positioned>();
        // Retain empty crafting cells and row offsets for the transfer handler.
        for (int i = 0; i < 9; i++) {
            int x = i%3, y=i/3, index=y*width+x;
            var ingredient = x<width && index<ingredients.size() ? choices(ingredients.get(index).displayStacks()) : EmiStack.EMPTY;
            int[] xs = {48,75,103,49,76,103,49,76,103};
            slots.add(input(ingredient,xs[i]-1,32+y*27));
        }
        slots.add(output(EmiStack.of(result),73,1)); aspects(slots,cost,114);
        registry.addRecipe(new NativeRecipe(category,key,slots,List.of(),
            Math.max(180,140+((cost.size()+7)/8)*20),true, new RecipePresentation(RecipePresentation.Kind.ARCANE,0)));
    }
    private static void crucibleAspects(List<Positioned> slots,AspectList cost) {
        var sorted = cost.getAspectsSorted();
        int count = cost.size();
        int baseY = 79 - 10 * ((Math.min(9,count)-1)/3);
        for (int i=0; i<count; i++) {
            int rowSize = Math.min(3,count-i/3*3);
            int x = 80 - rowSize*10 + i%3*20;
            int y = i<9 ? baseY+i/3*20-1 : 137+(i-9)/3*20;
            slots.add(input(new AspectStack(sorted.get(i),cost.getAmount(sorted.get(i))),x,y));
        }
    }
    static EmiIngredient choices(List<ItemStack> stacks) { return EmiIngredient.of(stacks.stream().map(EmiStack::of).toList()); }
    private static void aspects(List<Positioned> slots,AspectList aspects,int y) { aspects(slots, aspects, y, (aspect, amount) -> new AspectStack(aspect, amount)); }
    private static void aspects(List<Positioned> slots,AspectList aspects,int y,java.util.function.BiFunction<Aspect,Integer,EmiIngredient> factory) {
        int i=0; for (var aspect : aspects.getAspectsSorted()) {
            int rowSize = Math.min(8, aspects.size() - i / 8 * 8);
            int x = (166 - rowSize * 20) / 2 + i % 8 * 20;
            slots.add(input(factory.apply(aspect,aspects.getAmount(aspect)),x,y+i/8*20)); i++;
        }
    }
    private static void infernal(EmiRegistry registry) {
        var category=category(registry,"infernal_smelting","trt.category.infernal",Items.NETHER_BRICKS);
        var level=net.minecraft.client.Minecraft.getInstance().level; if(level==null)return;
        for(var holder:registry.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.SMELTING)) {
            var recipe=holder.value(); if(recipe.getIngredients().isEmpty())continue;
            for(var input:recipe.getIngredients().getFirst().getItems()) {
                var result=recipe.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(input),level.registryAccess()); if(result.isEmpty())continue;
                var slots=new ArrayList<Positioned>(); slots.add(input(EmiStack.of(input),50,40)); slots.add(output(EmiStack.of(result),70,7));
                var bonus=thaumcraft.device.SmeltingBonuses.get(input);
                if(bonus!=null)slots.add(new Positioned(EmiStack.of(bonus).setChance(.25F),98,40,true,
                    List.of(Component.translatable("trt.infernal.bonus"))));
                registry.addRecipe(new NativeRecipe(category,holder.id().toString(),slots,List.of(Component.translatable("trt.recipe.bonus")),92,true,
                    new RecipePresentation(RecipePresentation.Kind.FURNACE,0)));
            }
        }
    }
}

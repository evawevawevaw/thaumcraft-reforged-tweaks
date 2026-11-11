package dev.reforgedtweaks.integration.emi;

import java.util.*;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Explicit positioned slots retain EMI recipe context for bookmarks and recipe previews. */
final class NativeRecipe implements EmiRecipe {
    record Positioned(EmiIngredient ingredient, int x, int y, boolean output, List<Component> tooltip) {}
    final EmiRecipeCategory category;
    final ResourceLocation id;
    final List<Positioned> slots;
    final List<Component> text;
    final int height;
    final boolean tree;
    final RecipePresentation presentation;
    NativeRecipe(EmiRecipeCategory category, String key, List<Positioned> slots, List<Component> text, int height, boolean tree) {
        this(category, key, slots, text, height, tree, null);
    }
    NativeRecipe(EmiRecipeCategory category, String key, List<Positioned> slots, List<Component> text, int height, boolean tree, RecipePresentation presentation) {
        this.category = category; this.slots = List.copyOf(slots); this.text = List.copyOf(text); this.height = height; this.tree = tree;
        this.presentation = presentation;
        var json = new com.google.gson.JsonObject(); json.addProperty("key", key);
        var values = new com.google.gson.JsonArray();
        for (Positioned slot : slots) {
            var encoded = identity(slot.ingredient());
            values.add(encoded); values.add(slot.output());
        }
        json.add("slots", values);
        id = ResourceLocation.fromNamespaceAndPath("thaumcraft_reforged_tweaks", category.id.getPath() + "/" + dev.reforgedtweaks.client.data.RecipeIds.digest(json));
    }
    public EmiRecipeCategory getCategory() { return category; }
    public ResourceLocation getId() { return id; }
    public List<EmiIngredient> getInputs() { return slots.stream().filter(s -> !s.output()).map(Positioned::ingredient).toList(); }
    public List<EmiStack> getOutputs() { return slots.stream().filter(Positioned::output).flatMap(s -> s.ingredient().getEmiStacks().stream()).toList(); }
    public int getDisplayWidth() { return presentation != null && presentation.kind() == RecipePresentation.Kind.SPINNING ? 174 : 166; }
    public int getDisplayHeight() { return height; }
    public boolean supportsRecipeTree() { return tree; }
    public void addWidgets(WidgetHolder widgets) {
        if (presentation != null) presentation.addWidgets(widgets, this);
        for (Positioned slot : slots) {
            var widget = presentation != null && presentation.kind() == RecipePresentation.Kind.COMBINATION
                ? widgets.add(new LegacyAspectSlot(slot.ingredient(), slot.x(), slot.y(), slot.output() ? 32 : 24))
                : widgets.addSlot(slot.ingredient(), slot.x(), slot.y());
            if (presentation != null) widget.drawBack(false);
            if (slot.output()) widget.recipeContext(this);
            for (Component line : slot.tooltip()) widget.appendTooltip(line);
        }
        for (int i = 0; i < text.size(); i++) {
            var line = text.get(i);
            boolean instability = presentation != null && presentation.kind() == RecipePresentation.Kind.INFUSION && i == 0;
            int y = instability ? 49 : height - (text.size() - i) * 11;
            // Scale long translations within the recipe instead of allowing text to escape its bounds.
            widgets.addDrawable(1, y, 164, 10, (graphics, mx, my, delta) -> {
                var font = net.minecraft.client.Minecraft.getInstance().font;
                float scale = Math.min(1F, 164F / Math.max(1, font.width(line)));
                graphics.pose().pushPose(); graphics.pose().scale(scale, scale, 1);
                int x = instability ? Math.round((164F / scale - font.width(line)) / 2) : 0;
                graphics.drawString(font, line, x, 0, instability ? 0xFFFFFFFF : 0xFF404040, false); graphics.pose().popPose();
            });
        }
    }
    private static com.google.gson.JsonElement identity(EmiIngredient ingredient) {
        var json = new com.google.gson.JsonObject();
        json.addProperty("amount", ingredient.getAmount());
        var options = new java.util.ArrayList<com.google.gson.JsonElement>();
        for (EmiStack stack : ingredient.getEmiStacks()) {
            var option = new com.google.gson.JsonObject();
            option.addProperty("id", stack.getId().toString()); option.addProperty("amount", stack.getAmount());
            if (!stack.getItemStack().isEmpty()) {
                var level = java.util.Objects.requireNonNull(net.minecraft.client.Minecraft.getInstance().level);
                option.add("item", dev.reforgedtweaks.client.data.StackIdentity.encode(stack.getItemStack(), level.registryAccess()));
            }
            options.add(option);
        }
        var sorted = new com.google.gson.JsonArray();
        options.stream().sorted(java.util.Comparator.comparing(dev.reforgedtweaks.client.data.RecipeIds::digest)).forEach(sorted::add);
        json.add("options", sorted); return json;
    }
    static Positioned input(EmiIngredient stack, int x, int y) { return new Positioned(stack,x,y,false,List.of()); }
    static Positioned output(EmiIngredient stack, int x, int y) { return new Positioned(stack,x,y,true,List.of()); }
}

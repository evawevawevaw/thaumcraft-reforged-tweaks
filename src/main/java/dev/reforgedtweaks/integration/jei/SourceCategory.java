package dev.reforgedtweaks.integration.jei;

import dev.reforgedtweaks.client.data.AspectSources;
import dev.reforgedtweaks.client.data.SourcePagination;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import thaumcraft.registry.ModItems;

final class SourceCategory extends AbstractRecipeCategory<AspectSources.Page> {
    SourceCategory(IGuiHelper gui) {
        super(ReforgedJei.SOURCES, Component.translatable("trt.category.sources"), gui.createDrawableItemLike(ModItems.THAUMOMETER.get()), 142, 120);
    }
    public void setRecipe(IRecipeLayoutBuilder builder, AspectSources.Page recipe, IFocusGroup focuses) {
        int focusedAmount = focuses.getItemStackFocuses(RecipeIngredientRole.INPUT).findFirst()
            .map(focus -> thaumcraft.crafting.CrucibleCrafting.dissolveTags(focus.getTypedValue().getIngredient()).getAmount(recipe.aspect())).orElse(1);
        builder.addOutputSlot(1, 1).setStandardSlotBackground()
            .addIngredient(AspectIngredient.TYPE, new AspectIngredient(recipe.aspect(), Math.max(1, focusedAmount)));
        int index = 0;
        for (AspectSources.Entry source : recipe.sources()) {
            var slot = builder.addInputSlot(SourcePagination.x(index), SourcePagination.y(index));
            index++;
            if (source.token() == null) slot.addItemStack(source.stack());
            else slot.addIngredient(SourceToken.TYPE, SourceToken.of(source.token()));
            int amount = source.aspects().getAmount(recipe.aspect());
            slot.setOverlay(new Amount(amount), 0, 0).addRichTooltipCallback((view, tooltip) -> {
                tooltip.add(Component.translatable("trt.aspect.amount", amount));
                tooltip.add(Component.translatable(source.token() == null ? "trt.source.dissolve" : "trt.source.scan"));
                for (var aspect : source.aspects().getAspectsSorted()) tooltip.add(Component.translatable("trt.source.content",
                    aspect.getName(), aspect.getDescription(), source.aspects().getAmount(aspect)));
            });
        }
    }
    public void draw(AspectSources.Page recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.literal(recipe.aspect().getName()), 22, 2, 0xFF202020, false);
        var lines = font.split(recipe.aspect().getDescription(), 118);
        for (int i = 0; i < Math.min(2, lines.size()); i++) graphics.drawString(font, lines.get(i), 22, 15 + i * 9, 0xFF404040, false);
        Component footer = Component.translatable("trt.source.page", recipe.index() + 1, recipe.count(), recipe.total());
        float scale = Math.min(1, 138.0F / Math.max(1, font.width(footer)));
        graphics.pose().pushPose();
        graphics.pose().translate(2, 112, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, footer, 0, 0, 0xFF404040, false);
        graphics.pose().popPose();
    }
    public net.minecraft.resources.ResourceLocation getRegistryName(AspectSources.Page recipe) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft_reforged_tweaks", "aspect_sources/" + recipe.aspect().getTag() + "/" + recipe.index());
    }
    private record Amount(int value) implements IDrawable {
        public int getWidth() { return 16; }
        public int getHeight() { return 16; }
        public void draw(GuiGraphics graphics, int x, int y) {
            var font = Minecraft.getInstance().font;
            String text = Integer.toString(value);
            graphics.pose().pushPose();
            // The slot renderer owns overlay depth (JEMI already adds +200).
            // Another +250 puts these labels above the tooltip's +400 layer.
            graphics.pose().translate(x + 16, y + 16, 0);
            float scale = Math.min(0.65F, 16.0F / Math.max(1, font.width(text)));
            graphics.pose().scale(scale, scale, 1);
            graphics.drawString(font, text, -font.width(text), -font.lineHeight, 0xFFFFFFFF, true);
            graphics.pose().popPose();
        }
    }
}

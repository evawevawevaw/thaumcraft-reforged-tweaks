package dev.reforgedtweaks.integration.jei;

import java.util.List;
import java.util.ArrayList;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

final class SourceTokenHelper implements IIngredientHelper<SourceToken>, IIngredientRenderer<SourceToken> {
    public IIngredientType<SourceToken> getIngredientType() { return SourceToken.TYPE; }
    public String getDisplayName(SourceToken value) { return value.name().getString() + " " + value.key(); }
    public String getUniqueId(SourceToken value, UidContext context) { return value.key(); }
    public ResourceLocation getResourceLocation(SourceToken value) { return ResourceLocation.parse(value.key()); }
    public SourceToken copyIngredient(SourceToken value) { return value; }
    public String getErrorInfo(SourceToken value) { return value.key(); }
    public void render(GuiGraphics graphics, SourceToken value) { graphics.renderItem(value.icon(), 0, 0); }
    public List<Component> getTooltip(SourceToken value, TooltipFlag flag) {
        List<Component> lines = new ArrayList<>();
        lines.add(value.name());
        lines.add(Component.translatable("trt.source.scan"));
        lines.addAll(value.details());
        if (flag.isAdvanced()) lines.add(Component.literal(value.key()));
        return lines;
    }
}

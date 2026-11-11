package dev.reforgedtweaks.integration.jei;

import java.util.List;
import java.util.ArrayList;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

final class AspectRenderer implements IIngredientRenderer<AspectIngredient> {
    static final AspectRenderer VISIBLE = new AspectRenderer(true);
    static final AspectRenderer EXISTING_STRIP = new AspectRenderer(false);
    private final boolean drawIcon;
    private AspectRenderer(boolean drawIcon) { this.drawIcon = drawIcon; }
    public void render(GuiGraphics graphics, AspectIngredient value) {
        if (drawIcon) thaumcraft.client.aspect.AspectRenderer.drawTag(graphics, 0, 0, value.aspect(), value.amount() == 1 ? 0 : value.amount());
    }
    public List<Component> getTooltip(AspectIngredient value, TooltipFlag flag) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(value.aspect().getName()).withColor(value.aspect().getColor()));
        lines.add(value.aspect().getDescription());
        lines.add(Component.translatable("trt.aspect.amount", value.amount()));
        if (flag.isAdvanced()) lines.add(Component.literal(value.aspect().getTag()));
        return lines;
    }
}

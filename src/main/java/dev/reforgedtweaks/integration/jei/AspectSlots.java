package dev.reforgedtweaks.integration.jei;

import dev.reforgedtweaks.ClientConfig;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import thaumcraft.aspect.AspectList;

public final class AspectSlots {
    private AspectSlots() {}
    public static void append(IRecipeLayoutBuilder builder, AspectList aspects, int perRow, int width, int y) {
        if (!ClientConfig.aspectSlots()) return;
        var sorted = aspects.getAspectsSorted();
        int shift = (width - Math.min(sorted.size(), perRow) * 18) / 2;
        for (int index = 0; index < sorted.size(); index++) {
            var aspect = sorted.get(index);
            if (aspects.getAmount(aspect) > 0) builder.addInputSlot(shift + index % perRow * 18, y + index / perRow * 18)
                .setSlotName("trt_aspect_" + aspect.getTag())
                .addIngredient(AspectIngredient.TYPE, new AspectIngredient(aspect, aspects.getAmount(aspect)))
                .setCustomRenderer(AspectIngredient.TYPE, AspectRenderer.EXISTING_STRIP);
        }
    }
}

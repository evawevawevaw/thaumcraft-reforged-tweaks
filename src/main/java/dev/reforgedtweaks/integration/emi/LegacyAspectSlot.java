package dev.reforgedtweaks.integration.emi;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import net.minecraft.client.gui.GuiGraphics;

/** Legacy aspect diagram sizes, with EMI hit bounds and recipe context kept intact. */
final class LegacyAspectSlot extends SlotWidget {
    private final int size;
    LegacyAspectSlot(EmiIngredient ingredient, int x, int y, int size) {
        super(ingredient, x, y);
        this.size = size;
    }
    @Override public Bounds getBounds() { return new Bounds(x, y, size, size); }
    @Override public void drawStack(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(size / 16F, size / 16F, 1);
        getStack().render(graphics, 0, 0, delta);
        graphics.pose().popPose();
    }
}

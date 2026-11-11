package dev.reforgedtweaks.client.book;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Small violet-and-brass book controls, independent of vanilla button textures. */
final class BookButton extends Button {
    BookButton(int x, int y, int width, int height, Component text, OnPress action) {
        super(x, y, width, height, text, action, DEFAULT_NARRATION);
    }
    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        boolean lit = isHoveredOrFocused();
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        graphics.fill(x, y, x + w, y + h, lit ? 0xFFFFDB84 : 0xFFC19448);
        graphics.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, lit ? 0xFF673563 : 0xFF38233D, 0xFF1B1425);
        graphics.fill(x + 2, y + 2, x + 4, y + 3, 0xFFEDD39A);
        graphics.fill(x + w - 4, y + h - 3, x + w - 2, y + h - 2, 0xFFEDD39A);
        if (w == 22) {
            int cx = x + w / 2, cy = y + h / 2;
            int gold = lit ? 0xFFFFE5AA : 0xFFD8B369;
            graphics.fill(cx - 1, cy - 4, cx + 1, cy + 5, gold);
            graphics.fill(cx - 4, cy - 1, cx + 5, cy + 1, gold);
            graphics.fill(cx - 3, cy - 3, cx - 1, cy - 1, gold);
            graphics.fill(cx + 2, cy + 2, cx + 4, cy + 4, gold);
            graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFB86FC6);
            return;
        }
        var font = Minecraft.getInstance().font;
        float scale = Math.min(1F, (w - 6F) / Math.max(1, font.width(getMessage())));
        graphics.pose().pushPose();
        graphics.pose().translate(x + w / 2F, y + (h - font.lineHeight * scale) / 2F, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, getMessage(), -font.width(getMessage()) / 2, 0, active ? 0xFFFFE8B2 : 0xFF897861, true);
        graphics.pose().popPose();
    }
}

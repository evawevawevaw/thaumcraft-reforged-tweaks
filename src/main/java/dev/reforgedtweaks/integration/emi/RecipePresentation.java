package dev.reforgedtweaks.integration.emi;

import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Legacy Thaumcraft NEI Plugin layout, rendered with REFORGED's identical book atlas.
 * Design reference: com.djgiannuzz.thaumcraftneiplugin.nei.recipehandler.* drawBackground.
 * No NEI classes, old rendering code or external textures are shipped.
 */
record RecipePresentation(Kind kind, int aspectY) {
    enum Kind { ARCANE, SPINNING, CRUCIBLE, INFUSION, FURNACE, COMBINATION }
    static final int TEXT = 0xFF404040;
    private static final ResourceLocation BOOK = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_researchbook_overlay.png");

    void addWidgets(WidgetHolder widgets, NativeRecipe recipe) {
        widgets.addDrawable(0, 0, recipe.getDisplayWidth(), recipe.height, (g, mx, my, delta) -> {
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            switch (kind) {
                case ARCANE -> {
                    atlas(g, 37.4F, 24.1F, 112, 15, 52, 52, 1.7F);
                    atlas(g, 68, -3.1F, 20, 3, 16, 16, 1.7F);
                    com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,0.4F);
                    try { atlas(g, 34, Math.max(149, recipe.height - 31), 68, 76, 12, 12, 2); }
                    finally { com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1); }
                }
                case SPINNING -> {
                    // Actual machine GUI, SpinningWheelScreen.renderBg at zero progress.
                    // Exclude the player's inventory and progress-thread strip.
                    var texture = ResourceLocation.fromNamespaceAndPath("witchinggadgets", "textures/gui/spinningwheel.png");
                    g.blit(texture, 0, 0, 0F, 0F, 38, 144, 256, 256);
                    g.blit(texture, 126, 0, 126F, 0F, 48, 144, 256, 256);
                }
                case CRUCIBLE -> {
                    atlas(g, 30, 3, 0, 3, 56, 17, 1.75F);
                    atlas(g, 30, 48.5F, 0, 20, 56, 48, 1.75F);
                    atlas(g, 66.75F, 34.5F, 100, 84, 11, 13, 1.75F);
                }
                case INFUSION -> {
                    atlas(g, 34, -5, 0, 3, 56, 17, 1.75F);
                    atlas(g, 34, 28.25F, 200, 77, 60, 44, 1.75F);
                }
                case FURNACE -> {
                    // No custom legacy NEI handler exists for these extra categories.
                    // Reuse its small result-banner motif, without introducing another style.
                    atlas(g, 30, 3, 0, 3, 56, 17, 1.75F);
                }
                case COMBINATION -> { }
            }
        });
        if (kind == Kind.COMBINATION) {
            widgets.addText(net.minecraft.network.chat.Component.literal("="), 59, 19, 0xFF999999, false);
            widgets.addText(net.minecraft.network.chat.Component.literal("+"), 107, 19, 0xFF999999, false);
            int[] order = {2,0,1};
            int[] centers = {36,88,132};
            for (int i=0; i<order.length; i++) {
                var name = recipe.slots.get(order[i]).ingredient().getEmiStacks().getFirst().getName();
                int center = centers[i];
                widgets.addDrawable(center-24, 46, 48, recipe.height-46, (g,mx,my,delta) -> {
                    var font = net.minecraft.client.Minecraft.getInstance().font;
                    int y=0;
                    for (var line : font.split(name,46)) {
                        g.drawString(font,line,(48-font.width(line))/2,y,0xFF505050,false);
                        y+=font.lineHeight;
                    }
                });
            }
        }
    }

    private static void atlas(GuiGraphics graphics, float x, float y, int u, int v, int width, int height, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1);
        // Legacy GuiDraw used a 256-unit UV space for this 512px atlas; preserve it.
        graphics.blit(BOOK, 0, 0, (float)u, (float)v, width, height, 256, 256);
        graphics.pose().popPose();
    }
}

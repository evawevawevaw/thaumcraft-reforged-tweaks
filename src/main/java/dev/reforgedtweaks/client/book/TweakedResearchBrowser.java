package dev.reforgedtweaks.client.book;

import dev.reforgedtweaks.ClientConfig;
import thaumcraft.client.research.ResearchPageScreen;
import static thaumcraft.client.research.ResearchBrowserScreen.lastX;
import static thaumcraft.client.research.ResearchBrowserScreen.lastY;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import javax.annotation.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.aspect.AspectRenderer;
import thaumcraft.client.platform.BrowserDraw;
import thaumcraft.client.platform.ClientNetwork;
import thaumcraft.client.platform.GuiBlit;
import thaumcraft.client.platform.GuiLines;
import thaumcraft.network.PlayerCompletePayload;
import thaumcraft.registry.ModSounds;
import thaumcraft.research.Knowledge;
import thaumcraft.research.PlayerKnowledge;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchCategory;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchManager;
import thaumcraft.research.ResearchNotes;

/** Adapted from REFORGED 1.1.2 client/research/ResearchBrowserScreen.java (2af59787).
 * Own browser geometry and local tab order; research visibility and server payload actions retain REFORGED rules.
 * No private field access, global category mutation, Mixin or reflection. */
public final class TweakedResearchBrowser extends Screen {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/gui/gui_research.png");
    private static final ResourceLocation NODES = Thaumcraft.id("textures/misc/nodes.png");
    private static final Style GALACTIC = Style.EMPTY.withFont(Thaumcraft.id("galactic"));
    private int paneWidth = 256;
    private int paneHeight = 230;
    private int viewWidth = 224;
    private int viewHeight = 196;
    private static final int SHADE = 0xC0000000;

    private static final List<String> HIGHLIGHTED = new ArrayList<>();
    private static @Nullable String selectedCategory;

    private double prevMapX;
    private double prevMapY;
    private double mapX;
    private double mapY;
    private double targetX;
    private double targetY;
    private boolean dragging;
    private int guiMapTop;
    private int guiMapLeft;
    private int guiMapBottom;
    private int guiMapRight;
    private final List<ResearchItem> research = new ArrayList<>();
    private @Nullable ResearchItem currentHighlight;
    private boolean hasScribeStuff;
    private long popupTime;
    private String popupMessage = "";
    private boolean settingsOpen;
    private int tabOffset;
    private String draggedTab;
    private String dropTarget;
    private boolean dropAfter;
    private final List<BookButton> settingsButtons = new ArrayList<>();
    private BookButton settingsButton;

    public TweakedResearchBrowser() {
        super(Component.empty());
        prevMapX = mapX = targetX = lastX * 24 - 141 / 2 - 12;
        prevMapY = mapY = targetY = lastY * 24 - 141 / 2;
        updateResearch();
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.setScreen(new TweakedResearchBrowser());
        }
    }

    public static void highlight(String key) {
        HIGHLIGHTED.add(key);
    }

    public void updateResearch() {
        Minecraft minecraft = Minecraft.getInstance();
        research.clear();
        hasScribeStuff = false;
        if (selectedCategory == null || ResearchCategories.get(selectedCategory) == null) {
            selectedCategory = ResearchCategories.all().iterator().next().key();
        }
        ResearchCategory category = ResearchCategories.get(selectedCategory);
        research.addAll(category.research());
        Player player = minecraft.player;
        if (player != null && ResearchNotes.consumeInk(player, false) && ResearchNotes.findItem(player, Items.PAPER) >= 0) {
            hasScribeStuff = true;
        }
        guiMapTop = category.minColumn() * 24 - 40;
        guiMapLeft = category.minRow() * 24 - 40;
        guiMapBottom = category.maxColumn() * 24 - viewWidth + 62;
        guiMapRight = category.maxRow() * 24 - viewHeight + 62;
        if (guiMapBottom < guiMapTop) guiMapBottom = guiMapTop = (category.minColumn() + category.maxColumn()) * 12 - (viewWidth - 22) / 2;
        if (guiMapRight < guiMapLeft) guiMapRight = guiMapLeft = (category.minRow() + category.maxRow()) * 12 - (viewHeight - 22) / 2;
    }

    @Override
    public void removed() {
        lastX = (int) ((mapX + 141 / 2 + 12.0 + (viewWidth - 224) / 2.0) / 24.0);
        lastY = (int) ((mapY + 141 / 2 + (viewHeight - 196) / 2.0) / 24.0);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (settingsOpen && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            toggleSettings();
            return true;
        }
        if (minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            HIGHLIGHTED.clear();
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            HIGHLIGHTED.clear();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        prevMapX = mapX;
        prevMapY = mapY;
        double dx = targetX - mapX;
        double dy = targetY - mapY;
        if (dx * dx + dy * dy < 4.0) {
            mapX += dx;
            mapY += dy;
        } else {
            mapX += dx * 0.85;
            mapY += dy * 0.85;
        }
        if (minecraft.player != null && minecraft.player.tickCount % 20 == 0) {
            hasScribeStuff = ResearchNotes.consumeInk(minecraft.player, false) && ResearchNotes.findItem(minecraft.player, Items.PAPER) >= 0;
        }
    }

    private int left() {
        return (width - paneWidth) / 2;
    }

    private int top() {
        return (height - paneHeight) / 2;
    }

    private PlayerKnowledge knowledge() {
        return Knowledge.get(minecraft.player);
    }

    private boolean canUnlock(ResearchItem item) {
        Set<String> done = knowledge().research();
        for (String parentKey : item.parents()) {
            ResearchItem parent = ResearchCategories.getResearch(parentKey);
            if (parent != null && !done.contains(parent.key())) {
                return false;
            }
        }
        for (String parentKey : item.parentsHidden()) {
            ResearchItem parent = ResearchCategories.getResearch(parentKey);
            if (parent != null && !done.contains(parent.key())) {
                return false;
            }
        }
        return true;
    }

    private List<ResearchCategory> allTabs() {
        List<ResearchCategory> tabs = new ArrayList<>();
        for (ResearchCategory category : ResearchCategories.all()) {
            if (!category.key().equals("ELDRITCH") || ResearchManager.isComplete(minecraft.player, "ELDRITCHMINOR")) tabs.add(category);
        }
        var order = TabOrder.reconcile(ClientConfig.tabOrder(), tabs.stream().map(ResearchCategory::key).toList());
        tabs.sort(java.util.Comparator.comparingInt(tab -> order.indexOf(tab.key())));
        return tabs;
    }

    private int tabRows() { return Math.max(1, (paneHeight - 18) / 24); }
    private List<ResearchCategory> visibleTabs() {
        var tabs = allTabs();
        tabOffset = Math.max(0, Math.min(tabOffset, Math.max(0, tabs.size() - 2 * tabRows())));
        return tabs.subList(tabOffset, Math.min(tabs.size(), tabOffset + 2 * tabRows()));
    }
    private String tabAt(double mouseX, double mouseY) {
        var tabs = visibleTabs();
        for (int index = 0; index < tabs.size(); index++) {
            int x = left() - 24 + (index >= tabRows() ? paneWidth + 24 : 0);
            int y = top() + index % tabRows() * 24;
            if (mouseX >= x && mouseX < x + 24 && mouseY >= y && mouseY < y + 24) return tabs.get(index).key();
        }
        return null;
    }

    private static float pulse() {
        return (float) Math.sin(Util.getMillis() % 600L / 600.0 * Math.PI * 2.0);
    }

    private static int gray(float value) {
        return FastColor.ARGB32.colorFromFloat(1.0F, value, value, value);
    }

    private Component styled(String key, boolean galactic) {
        MutableComponent component = Component.translatable(key);
        return galactic ? component.withStyle(GALACTIC) : component;
    }

    private static int lines(Font font, Component component, int wrapWidth) {
        return font.split(component, Math.max(1, wrapWidth)).size();
    }

    private void scaledText(GuiGraphics graphics, Component text, int x, int y, int color, boolean shadow) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        graphics.drawString(minecraft.font, text, 0, 0, color, shadow);
        graphics.pose().popPose();
    }

    private void scaledWrapped(GuiGraphics graphics, Component text, int x, int y, int wrapWidth, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        int lineY = 0;
        for (var line : minecraft.font.split(text, Math.max(1, wrapWidth))) {
            graphics.drawString(minecraft.font, line, 0, lineY, color, true);
            lineY += minecraft.font.lineHeight;
        }
        graphics.pose().popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Screen.render() also runs the blur/menu background pass. Run that pass
        // before the book, never from the foreground widget layer below.
        graphics.flush();
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.flush();
        if (minecraft.player == null || ResearchCategories.get(selectedCategory) == null) {
            return;
        }
        drawMap(graphics, settingsOpen || draggedTab != null ? -10000 : mouseX, settingsOpen || draggedTab != null ? -10000 : mouseY, partialTick);
        if (!settingsOpen) drawOverlays(graphics, mouseX, mouseY);
        graphics.flush();
        graphics.pose().pushPose();
        if (settingsOpen) graphics.pose().translate(0, 0, 600);
        try {
            if (settingsOpen) drawSettings(graphics);
            for (var renderable : renderables) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
            graphics.flush();
        } finally { graphics.pose().popPose(); }

    }

    private void drawMap(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long time = Util.getNanos() / 50000000L;
        double cameraX = Mth.clamp(prevMapX + (mapX - prevMapX) * partialTick, guiMapTop, guiMapBottom);
        double cameraY = Mth.clamp(prevMapY + (mapY - prevMapY) * partialTick, guiMapLeft, guiMapRight);
        int offsetX = Mth.floor(cameraX);
        int offsetY = Mth.floor(cameraY);
        offsetX = Math.max(offsetX, guiMapTop);
        offsetY = Math.max(offsetY, guiMapLeft);
        offsetX = Math.min(offsetX, guiMapBottom);
        offsetY = Math.min(offsetY, guiMapRight);
        int paneX = left();
        int paneY = top();
        int mapLeft = paneX + 16;
        int mapTop = paneY + 17;
        ResearchCategory category = ResearchCategories.get(selectedCategory);
        Set<String> done = knowledge().research();

        graphics.enableScissor(mapLeft, mapTop, mapLeft + viewWidth, mapTop + viewHeight);
        var background = BackgroundRegion.at(cameraX, cameraY, category.minColumn(), category.maxColumn(),
            category.minRow(), category.maxRow(), viewWidth, viewHeight);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1, 1, 1, 1);
        // The public GuiGraphics overload retains fractional UVs; GuiBlit takes integer UVs.
        graphics.blit(category.background(), mapLeft, mapTop, viewWidth, viewHeight,
            background.u(), background.v(), background.width(), background.height(), 512, 512);

        for (ResearchItem item : research) {
            for (String parentKey : item.parents()) {
                ResearchItem parent = parentKey == null ? null : ResearchCategories.getResearch(parentKey);
                if (parent == null || !parent.category().equals(selectedCategory) || parent.isVirtual()) {
                    continue;
                }
                int x1 = item.column() * 24 - offsetX + 11 + mapLeft;
                int y1 = item.row() * 24 - offsetY + 11 + mapTop;
                int x2 = parent.column() * 24 - offsetX + 11 + mapLeft;
                int y2 = parent.row() * 24 - offsetY + 11 + mapTop;
                boolean itemDone = done.contains(item.key());
                boolean parentDone = done.contains(parent.key());
                if (itemDone) {
                    drawLine(graphics, x1, y1, x2, y2, 0.1F, 0.1F, 0.1F, partialTick, false);
                } else if (!item.isLost()
                    && (!item.isHidden() && !item.isLost() || done.contains("@" + item.key()))
                    && (!item.isConcealed() || canUnlock(item))) {
                    if (parentDone) {
                        drawLine(graphics, x1, y1, x2, y2, 0.0F, 1.0F, 0.0F, partialTick, true);
                    } else if ((!parent.isHidden() && !item.isLost() || done.contains("@" + parent.key()))
                        && (!parent.isConcealed() || canUnlock(parent))) {
                        drawLine(graphics, x1, y1, x2, y2, 0.0F, 0.0F, 1.0F, partialTick, true);
                    }
                }
            }
            for (String siblingKey : item.siblings()) {
                ResearchItem sibling = siblingKey == null ? null : ResearchCategories.getResearch(siblingKey);
                if (sibling == null || !sibling.category().equals(selectedCategory) || sibling.isVirtual() || sibling.parents().contains(item.key())) {
                    continue;
                }
                int x1 = item.column() * 24 - offsetX + 11 + mapLeft;
                int y1 = item.row() * 24 - offsetY + 11 + mapTop;
                int x2 = sibling.column() * 24 - offsetX + 11 + mapLeft;
                int y2 = sibling.row() * 24 - offsetY + 11 + mapTop;
                boolean itemDone = done.contains(item.key());
                boolean siblingDone = done.contains(sibling.key());
                if (itemDone) {
                    drawLine(graphics, x1, y1, x2, y2, 0.1F, 0.1F, 0.2F, partialTick, false);
                } else if (!item.isLost()
                    && (!item.isHidden() || done.contains("@" + item.key()))
                    && (!item.isConcealed() || canUnlock(item))) {
                    if (siblingDone) {
                        drawLine(graphics, x1, y1, x2, y2, 0.0F, 1.0F, 0.0F, partialTick, true);
                    } else if ((!sibling.isHidden() || done.contains("@" + sibling.key()))
                        && (!sibling.isConcealed() || canUnlock(sibling))) {
                        drawLine(graphics, x1, y1, x2, y2, 0.0F, 0.0F, 1.0F, partialTick, true);
                    }
                }
            }
        }

        currentHighlight = null;
        List<Runnable> icons = new ArrayList<>();
        for (ResearchItem item : research) {
            int relativeX = item.column() * 24 - offsetX;
            int relativeY = item.row() * 24 - offsetY;
            if (item.isVirtual() || relativeX < -24 || relativeY < -24 || relativeX > viewWidth || relativeY > viewHeight) {
                continue;
            }
            int x = mapLeft + relativeX;
            int y = mapTop + relativeY;
            boolean complete = done.contains(item.key());
            boolean unlockable = canUnlock(item);
            int tint;
            if (complete) {
                if (item.warp() > 0) {
                    drawForbidden(graphics, x + 11, y + 11);
                }
                tint = 0xFFFFFFFF;
            } else {
                if (!done.contains("@" + item.key())
                    && (item.isLost() || item.isHidden() || item.isConcealed() && !unlockable)) {
                    continue;
                }
                if (item.warp() > 0) {
                    drawForbidden(graphics, x + 11, y + 11);
                }
                tint = unlockable ? gray(pulse() * 0.25F + 0.75F) : gray(0.3F);
            }
            boolean buyable = ResearchManager.isBuyable(item);
            int frameU;
            if (item.isRound()) {
                frameU = 54;
            } else if (item.isHidden()) {
                frameU = buyable ? 230 : 86;
            } else {
                frameU = buyable ? 110 : 0;
            }
            GuiBlit.blit(graphics, TEXTURE, x - 2, y - 2, 26, 26, frameU, 230, 26, 26, 256, 256, tint);
            if (item.isSpecial()) {
                GuiBlit.blit(graphics, TEXTURE, x - 2, y - 2, 26, 26, 26, 230, 26, 26, 256, 256, tint);
            }
            if (HIGHLIGHTED.contains(item.key())) {
                GuiBlit.blit(graphics, AspectRenderer.PARTICLES, x - 5, y - 5, 16, 16, (int) (time % 16L) * 16, 80, 16, 16, 256, 256, 0xFFFFFFFF);
            }
            final int iconX = x + 3;
            final int iconY = y + 3;
            final int iconTint = unlockable ? tint : gray(0.2F);
            ItemStack stack = item.iconItem() == null ? ItemStack.EMPTY : item.iconItem().create();
            icons.add(() -> {
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, iconX, iconY);
                } else {
                    ResearchIcons.drawFallback(graphics, item, iconX, iconY, iconTint);
                }
            });
            if (mouseX >= mapLeft && mouseY >= mapTop && mouseX < mapLeft + viewWidth && mouseY < mapTop + viewHeight
                && mouseX >= x && mouseX <= x + 22 && mouseY >= y && mouseY <= y + 22) {
                currentHighlight = item;
            }
        }
        graphics.flush();
        for (Runnable icon : icons) {
            icon.run();
        }
        graphics.flush();
        graphics.disableScissor();
        graphics.flush();

        List<ResearchCategory> tabs = visibleTabs();
        for (int index = 0; index < tabs.size(); index++) {
            ResearchCategory tab = tabs.get(index);
            boolean swop = index >= tabRows();
            int count = index % tabRows();
            boolean selected = tab.key().equals(selectedCategory);
            int s0 = swop ? paneWidth + 8 : 0;
            int s1 = 0;
            int s2 = swop ? 14 : 0;
            if (!selected) {
                s1 = 24;
                s2 = swop ? 6 : 8;
            }
            if (swop) {
                BrowserDraw.reversed(graphics, TEXTURE, paneX + s0 - 8, paneY + count * 24, 24, 24, 176 + s1, 232, 256, 256);
            } else {
                GuiBlit.blit(graphics, TEXTURE, paneX - 24 + s0, paneY + count * 24, 24, 24, 152 + s1, 232, 24, 24, 256, 256, 0xFFFFFFFF);
            }
            if (HIGHLIGHTED.contains(tab.key())) {
                GuiBlit.blit(graphics, AspectRenderer.PARTICLES, paneX - 27 + s2 + s0, paneY - 4 + count * 24, 16, 16, (int) (16L * (time % 16L)), 80, 16, 16, 256, 256, 0xFFFFFFFF);
            }
            BrowserDraw.full(graphics, tab.icon(), paneX - 19 + s2 + s0, paneY + 4 + count * 24, 16, 16, 0xFFFFFFFF);
            if (!selected) {
                if (swop) {
                    BrowserDraw.reversed(graphics, TEXTURE, paneX + s0 - 8, paneY + count * 24, 24, 24, 224, 232, 256, 256);
                } else {
                    GuiBlit.blit(graphics, TEXTURE, paneX - 24 + s0, paneY + count * 24, 24, 24, 200, 232, 24, 24, 256, 256, 0xFFFFFFFF);
                }
            }
        }
        drawFrame(graphics);
    }

    private void drawOverlays(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 400.0F);
        if (popupTime > System.currentTimeMillis()) {
            int centerX = left() + paneWidth / 2;
            int centerY = top() + paneHeight / 2;
            Component message = Component.literal(popupMessage);
            int half = lines(minecraft.font, message, 150) * minecraft.font.lineHeight / 2;
            graphics.fill(centerX - 78, centerY - half - 3, centerX + 78, centerY + half + 3, SHADE);
            int lineY = centerY - half;
            for (var line : minecraft.font.split(message, 150)) {
                graphics.drawString(minecraft.font, line, centerX - 75, lineY, -7302913, false);
                lineY += minecraft.font.lineHeight;
            }
        }
        List<ResearchCategory> tabs = visibleTabs();
        for (int index = 0; index < tabs.size(); index++) {
            boolean swop = index >= tabRows();
            int count = index % tabRows();
            int localX = mouseX - (left() - 24 + (swop ? paneWidth + 24 : 0));
            int localY = mouseY - (top() + count * 24);
            if (draggedTab == null && !Screen.hasControlDown() && localX >= 0 && localX < 24 && localY >= 0 && localY < 24) {
                graphics.drawString(minecraft.font, Component.translatable(tabs.get(index).nameKey()), mouseX, mouseY - 8, 0xFFFFFFFF, true);
            }
        }
        if (draggedTab != null || Screen.hasControlDown() && tabAt(mouseX, mouseY) != null) {
            graphics.renderTooltip(font, Component.translatable(draggedTab != null ? "trt.book.drop_tab" : "trt.book.drag_tab"), mouseX, mouseY);
        }
        if (dropTarget != null) {
            for (int i = 0; i < tabs.size(); i++) if (tabs.get(i).key().equals(dropTarget)) {
                int x = left() - 24 + (i >= tabRows() ? paneWidth + 24 : 0);
                int y = top() + i % tabRows() * 24;
                graphics.fill(x, y + (dropAfter ? 22 : 0), x + 24, y + (dropAfter ? 24 : 2), 0xFFFFD77F);
            }
        }
        if (currentHighlight != null && draggedTab == null) {
            drawTooltip(graphics, currentHighlight, mouseX, mouseY);
        }
        graphics.flush();
        graphics.pose().popPose();
    }

    private void drawTooltip(GuiGraphics graphics, ResearchItem item, int mouseX, int mouseY) {
        Font font = minecraft.font;
        PlayerKnowledge knowledge = knowledge();
        Set<String> done = knowledge.research();
        boolean complete = done.contains(item.key());
        boolean unlockable = canUnlock(item);
        boolean galactic = !complete && !unlockable;
        Component name = styled(item.nameKey(), galactic);
        int x = mouseX + 6;
        int y = mouseY - 4;
        int extra = 0;
        if (!unlockable) {
            Component missing = Component.translatable("tc.researchmissing");
            Component missingStyled = styled("tc.researchmissing", galactic);
            int width = (int) Math.max(font.width(name), font.width(missingStyled) / 1.5F);
            int splitHeight = lines(font, missingStyled, width * 2) * font.lineHeight;
            graphics.fill(x - 3, y - 3, x + width + 3, y + splitHeight + 10, SHADE);
            scaledWrapped(graphics, missing, x, y + 12, width * 2, -9416624);
        } else {
            boolean secondary = !complete && item.tags() != null && item.tags().size() > 0 && ResearchManager.isBuyable(item);
            boolean primary = !secondary && !complete;
            Component text = styled(item.textKey(), galactic);
            int width = (int) Math.max(font.width(name), font.width(text) / 1.9F);
            int height = lines(font, name, width) * font.lineHeight + 5;
            if (primary) {
                extra += 9;
                width = (int) Math.max(width, font.width(styled("tc.research.shortprim", galactic)) / 1.9F);
            }
            if (secondary) {
                extra += 29;
                width = (int) Math.max(width, font.width(styled("tc.research.short", galactic)) / 1.9F);
            }
            int warp = Math.min(item.warp(), 5);
            String warpText = "";
            if (item.warp() > 0) {
                String forbidden = Component.translatable("tc.forbidden").getString();
                String level = Component.translatable("tc.forbidden.level." + warp).getString();
                warpText = forbidden.replace("%n", level);
                extra += 9;
                width = (int) Math.max(width, font.width(Component.literal(warpText).withStyle(galactic ? GALACTIC : Style.EMPTY)) / 1.9F);
            }
            graphics.fill(x - 3, y - 3, x + width + 3, y + height + 6 + extra, SHADE);
            scaledText(graphics, Component.translatable(item.textKey()), x, y + height - 1, -7302913, true);
            if (warp > 0) {
                scaledText(graphics, Component.literal(warpText), x, y + height + 8, 0xFFFFFFFF, true);
                height += 9;
            }
            if (primary) {
                if (ResearchNotes.findNoteSlot(minecraft.player, item.key()) >= 0) {
                    scaledText(graphics, Component.translatable("tc.research.hasnote"), x, y + height + 8, 0xFFFFA500, true);
                } else if (hasScribeStuff) {
                    scaledText(graphics, Component.translatable("tc.research.getprim"), x, y + height + 8, 0xFF87EB6B, true);
                } else {
                    scaledText(graphics, Component.translatable("tc.research.shortprim"), x, y + height + 8, 0xFFDC4C3C, true);
                }
            } else if (secondary) {
                boolean enough = true;
                int index = 0;
                for (Aspect aspect : item.tags().getAspectsSortedAmount()) {
                    if (knowledge.hasDiscoveredAspect(aspect)) {
                        float alpha = 1.0F;
                        if (knowledge.getAspectPool(aspect) < item.tags().getAmount(aspect)) {
                            alpha = pulse() * 0.25F + 0.75F;
                            enough = false;
                        }
                        AspectRenderer.drawTag(graphics, x + index * 16, y + height + 8, aspect, item.tags().getAmount(aspect), 0, alpha, false);
                    } else {
                        enough = false;
                        GuiBlit.blit(graphics, AspectRenderer.UNKNOWN, x + index * 16, y + height + 8, 16, 16, 0, 0, 32, 32, 32, 32, FastColor.ARGB32.colorFromFloat(0.5F, 0.5F, 0.5F, 0.5F));
                    }
                    index++;
                }
                if (enough) {
                    scaledText(graphics, Component.translatable("tc.research.purchase"), x, y + height + 27, 0xFF87EB6B, true);
                } else {
                    scaledText(graphics, Component.translatable("tc.research.short"), x, y + height + 27, 0xFFDC4C3C, true);
                }
            }
        }
        int nameColor = unlockable ? (item.isSpecial() ? -128 : -1) : (item.isSpecial() ? -8355776 : -8355712);
        graphics.drawString(font, name, x, y, nameColor, true);
    }

    private void drawLine(GuiGraphics graphics, int x, int y, int x2, int y2, float red, float green, float blue, float partialTick, boolean wiggle) {
        float count = minecraft.player.tickCount + partialTick;
        double distanceX = x - x2;
        double distanceY = y - y2;
        float distance = Mth.sqrt((float) (distanceX * distanceX + distanceY * distanceY));
        int increments = (int) (distance / 2.0F);
        if (increments <= 0) {
            return;
        }
        float stepX = (float) (distanceX / increments);
        float stepY = (float) (distanceY / increments);
        boolean horizontal = Math.abs(distanceX) > Math.abs(distanceY);
        if (horizontal) {
            stepX *= 2.0F;
        } else {
            stepY *= 2.0F;
        }
        float[] pointsX = new float[increments + 1];
        float[] pointsY = new float[increments + 1];
        int[] colors = new int[increments + 1];
        for (int index = 0; index <= increments; index++) {
            float r = red;
            float g = green;
            float b = blue;
            float offsetX = 0.0F;
            float offsetY = 0.0F;
            float opacity = 0.6F;
            if (wiggle) {
                float phase = (float) index / increments;
                offsetX = Mth.sin((count + index) / 7.0F) * 5.0F * (1.0F - phase);
                offsetY = Mth.sin((count + index) / 5.0F) * 5.0F * (1.0F - phase);
                r *= 1.0F - phase;
                g *= 1.0F - phase;
                b *= 1.0F - phase;
                opacity *= phase;
            }
            pointsX[index] = x - stepX * index + offsetX;
            pointsY[index] = y - stepY * index + offsetY;
            colors[index] = FastColor.ARGB32.colorFromFloat(opacity, r, g, b);
            if (horizontal) {
                stepX *= 1.0F - 1.0F / (increments * 3.0F / 2.0F);
            } else {
                stepY *= 1.0F - 1.0F / (increments * 3.0F / 2.0F);
            }
        }
        GuiLines.strip(graphics, pointsX, pointsY, colors, false);
    }

    private void drawForbidden(GuiGraphics graphics, int x, int y) {
        int frames = 32;
        int part = minecraft.player.tickCount % frames;
        int frame = frames - 1 - part;
        GuiBlit.blit(graphics, NODES, x - 40, y - 40, 80, 80, frame * 64, 5 * 64, 64, 64, 2048, 2048, FastColor.ARGB32.color((int) (0.66F * 255.0F), 0x440055));
    }

    @Override
    public boolean mouseClicked(double eventX, double eventY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(eventX, eventY, button);
        }
        if (super.mouseClicked(eventX, eventY, button)) return true;
        if (settingsOpen) return true;
        String clickedTab = tabAt(eventX, eventY);
        if (clickedTab != null) {
            currentHighlight = null;
            dragging = false;
            if (Screen.hasControlDown() && ClientConfig.tabDrag()) {
                draggedTab = clickedTab;
                dropTarget = null;
            } else {
                selectedCategory = clickedTab;
                updateResearch();
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.CAMERACLACK.get(), 1.0F, 0.4F));
            }
            return true;
        }
        double mouseX = eventX;
        double mouseY = eventY;
        int viewX = left() + 16;
        int viewY = top() + 17;
        dragging = mouseX >= viewX && mouseX < viewX + viewWidth && mouseY >= viewY && mouseY < viewY + viewHeight;
        popupTime = System.currentTimeMillis() - 1L;
        Set<String> done = knowledge().research();
        ResearchItem item = currentHighlight;
        if (item != null && !done.contains(item.key()) && canUnlock(item)) {
            updateResearch();
            boolean secondary = item.tags() != null && item.tags().size() > 0 && ResearchManager.isBuyable(item);
            if (secondary) {
                boolean enough = true;
                for (Aspect aspect : item.tags().getAspects()) {
                    if (knowledge().getAspectPool(aspect) < item.tags().getAmount(aspect)) {
                        enough = false;
                        break;
                    }
                }
                if (enough) {
                    ClientNetwork.sendToServer(new PlayerCompletePayload(item.key(), 0));
                }
            } else if (hasScribeStuff && ResearchNotes.findNoteSlot(minecraft.player, item.key()) == -1) {
                ClientNetwork.sendToServer(new PlayerCompletePayload(item.key(), 1));
                popupTime = System.currentTimeMillis() + 3000L;
                popupMessage = Component.translatable("tc.research.popup", Component.translatable(item.nameKey()).getString()).getString();
            }
        } else if (item != null && done.contains(item.key())) {
            ResearchPageScreen.open(item);
        }
        return super.mouseClicked(eventX, eventY, button);
    }

    @Override
    public boolean mouseDragged(double eventX, double eventY, int button, double dragX, double dragY) {
        if (settingsOpen) return super.mouseDragged(eventX, eventY, button, dragX, dragY);
        if (draggedTab != null && button == InputConstants.MOUSE_BUTTON_LEFT) {
            dropTarget = tabAt(eventX, eventY);
            dropAfter = Math.floorMod((int) eventY - top(), 24) >= 12;
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT && dragging) {
            int viewX = left() + 16;
            int viewY = top() + 17;
            if (eventX >= viewX && eventX < viewX + viewWidth && eventY >= viewY && eventY < viewY + viewHeight) {
                mapX -= dragX;
                mapY -= dragY;
                targetX = mapX;
                targetY = mapY;
                prevMapX = mapX;
                prevMapY = mapY;
            }
            if (targetX < guiMapTop) {
                targetX = guiMapTop;
            }
            if (targetY < guiMapLeft) {
                targetY = guiMapLeft;
            }
            if (targetX >= guiMapBottom) {
                targetX = guiMapBottom;
            }
            if (targetY >= guiMapRight) {
                targetY = guiMapRight;
            }
            return true;
        }
        return super.mouseDragged(eventX, eventY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double eventX, double eventY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT && draggedTab != null) {
            String target = tabAt(eventX, eventY);
            if (target != null) {
                var order = TabOrder.reconcile(ClientConfig.tabOrder(), ResearchCategories.all().stream().map(ResearchCategory::key).toList());
                ClientConfig.saveTabOrder(TabOrder.move(order, draggedTab, target, Math.floorMod((int) eventY - top(), 24) >= 12));
            }
            draggedTab = null;
            dropTarget = null;
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT) dragging = false;
        return super.mouseReleased(eventX, eventY, button);
    }

    @Override
    protected void init() {
        int oldWidth = viewWidth, oldHeight = viewHeight;
        var geometry = BookGeometry.fit(width, height, ClientConfig.browserScale());
        paneWidth = geometry.width(); paneHeight = geometry.height();
        viewWidth = geometry.viewWidth(); viewHeight = geometry.viewHeight();
        // Keep the world coordinate in the centre stable when changing the viewport or window size.
        mapX -= (viewWidth - oldWidth) / 2.0; mapY -= (viewHeight - oldHeight) / 2.0;
        prevMapX = targetX = mapX; prevMapY = targetY = mapY;
        updateResearch();
        settingsButtons.clear();
        settingsButton = addRenderableWidget(new BookButton(left() + paneWidth - 36, top() + paneHeight - 15, 22, 13,
            Component.translatable("trt.book.settings"), button -> toggleSettings()));
        settingsButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("trt.book.settings")));
        int x = width / 2, y = height / 2;
        settingsButtons.add(addRenderableWidget(new BookButton(x - 86, y - 10, 24, 20, Component.literal("-"), button -> changeScale(-1))));
        settingsButtons.add(addRenderableWidget(new BookButton(x + 62, y - 10, 24, 20, Component.literal("+"), button -> changeScale(1))));
        settingsButtons.add(addRenderableWidget(new BookButton(x - 86, y + 19, 172, 18, Component.translatable("trt.book.reset_scale"), button -> {
            ClientConfig.saveScale(1); rebuild();
        })));
        settingsButtons.add(addRenderableWidget(new BookButton(x - 86, y + 43, 172, 18, Component.translatable("trt.book.reset_tabs"), button -> {
            ClientConfig.saveTabOrder(List.of()); tabOffset = 0;
        })));
        settingsButtons.add(addRenderableWidget(new BookButton(x - 45, y + 71, 90, 18, Component.translatable("gui.done"), button -> toggleSettings())));
        settingsButtons.forEach(button -> button.visible = settingsOpen);
        settingsButton.visible = settingsButton.active = !settingsOpen;
    }
    private void rebuild() { clearWidgets(); init(); }
    private void toggleSettings() {
        settingsOpen = !settingsOpen; dragging = false; currentHighlight = null; draggedTab = null; dropTarget = null;
        settingsButtons.forEach(button -> button.visible = settingsOpen);
        settingsButton.visible = settingsButton.active = !settingsOpen;
        if (!settingsOpen) setFocused(null);
    }
    private void changeScale(int direction) {
        ClientConfig.saveScale(BookGeometry.step(ClientConfig.browserScale(), direction));
        rebuild();
    }
    private void drawSettings(GuiGraphics graphics) {
        graphics.fill(0, 0, width, height, 0xA510091A);
        int x = width / 2, y = height / 2;
        graphics.fill(x - 112, y - 94, x + 112, y + 98, 0xFFD1AA61);
        graphics.fillGradient(x - 110, y - 92, x + 110, y + 96, 0xFF372439, 0xFF17101F);
        graphics.drawCenteredString(font, Component.translatable("trt.book.settings"), x, y - 80, 0xFFFFE2A0);
        graphics.drawCenteredString(font, Component.translatable("trt.book.scale", String.format(java.util.Locale.ROOT, "%.1f", ClientConfig.browserScale())), x, y - 5, 0xFFFFEBC7);
        int lineY = y - 59;
        for (var line : font.split(Component.translatable("trt.book.scale_hint"), 198)) {
            graphics.drawString(font, line, x - 99, lineY, 0xFFCABAD0, false); lineY += 10;
        }
        graphics.drawCenteredString(font, Component.translatable("trt.book.author", "evawevawevaw"), x, y + 100, 0xFFDFC18C);
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (settingsOpen) return true;
        if (Screen.hasControlDown()) {
            if (vertical != 0) changeScale(vertical > 0 ? 1 : -1);
            return true;
        }
        if (tabAt(mouseX, mouseY) != null || draggedTab != null) {
            tabOffset = Math.max(0, Math.min(Math.max(0, allTabs().size() - tabRows() * 2), tabOffset + (vertical > 0 ? -1 : vertical < 0 ? 1 : 0)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }
    private void drawFrame(GuiGraphics graphics) {
        int[] destinationX = {left(), left() + 16, left() + paneWidth - 16};
        int[] destinationY = {top(), top() + 17, top() + paneHeight - 17};
        int[] sourceX = {0, 16, 240}, sourceY = {0, 17, 213};
        int[] widths = {16, paneWidth - 32, 16}, heights = {17, paneHeight - 34, 17};
        int[] sourceWidths = {16, 224, 16}, sourceHeights = {17, 196, 17};
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            if (row == 1 && col == 1) continue;
            GuiBlit.blit(graphics, TEXTURE, destinationX[col], destinationY[row], widths[col], heights[row],
                sourceX[col], sourceY[row], sourceWidths[col], sourceHeights[row], 256, 256, -1);
        }
    }
}

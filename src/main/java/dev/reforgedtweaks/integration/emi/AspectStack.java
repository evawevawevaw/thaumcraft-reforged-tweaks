package dev.reforgedtweaks.integration.emi;

import java.util.List;
import com.google.gson.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.stack.serializer.EmiIngredientSerializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.aspect.Aspect;

/** Native EMI ingredient; identity follows REFORGED Aspect and rendering uses its public renderer. */
final class AspectStack extends EmiStack {
    final Aspect aspect;
    AspectStack(Aspect aspect, long amount) { this.aspect = aspect; this.amount = amount; }
    public EmiStack copy() { return new AspectStack(aspect, amount).setChance(chance).setRemainder(getRemainder()); }
    public boolean isEmpty() { return aspect == null || amount <= 0; }
    public DataComponentPatch getComponentChanges() { return DataComponentPatch.EMPTY; }
    public Object getKey() { return aspect; }
    public ResourceLocation getId() { return ResourceLocation.fromNamespaceAndPath("thaumcraft_reforged_tweaks", "aspect/" + aspect.getTag()); }
    public Component getName() { return Component.literal(aspect.getName()); }
    public List<Component> getTooltipText() { return List.of(getName().copy().withColor(aspect.getColor()), aspect.getDescription(),
        Component.translatable("trt.aspect.amount", amount), Component.literal(aspect.getTag())); }
    public void render(GuiGraphics graphics, int x, int y, float delta, int flags) {
        if ((flags & EmiIngredient.RENDER_ICON) != 0) thaumcraft.client.aspect.AspectRenderer.drawTag(graphics, x, y, aspect, 0);
        if ((flags & EmiIngredient.RENDER_AMOUNT) != 0 && amount != 1) {
            String text = Long.toString(amount);
            var font = Minecraft.getInstance().font;
            float scale = Math.min(.65F, 16F / Math.max(1, font.width(text)));
            graphics.pose().pushPose();
            graphics.pose().translate(x + 16, y + 16, 250);
            graphics.pose().scale(scale, scale, 1);
            graphics.drawString(font, text, -font.width(text), -font.lineHeight, -1, true);
            graphics.pose().popPose();
        }
    }
    static final class Serializer implements EmiIngredientSerializer<AspectStack> {
        public String getType() { return "thaumcraft_reforged_tweaks:aspect"; }
        public EmiIngredient deserialize(JsonElement element) {
            try {
                var json = element.getAsJsonObject();
                Aspect aspect = java.util.Arrays.stream(Aspect.values()).filter(a -> a.getTag().equals(json.get("aspect").getAsString())).findFirst().orElse(null);
                return aspect == null ? EmiStack.EMPTY : new AspectStack(aspect, Math.max(1, json.get("amount").getAsLong()));
            } catch (RuntimeException invalid) { return EmiStack.EMPTY; }
        }
        public JsonElement serialize(AspectStack stack) {
            JsonObject json = new JsonObject();
            json.addProperty("type", getType()); json.addProperty("aspect", stack.aspect.getTag()); json.addProperty("amount", stack.amount);
            return json;
        }
    }
}

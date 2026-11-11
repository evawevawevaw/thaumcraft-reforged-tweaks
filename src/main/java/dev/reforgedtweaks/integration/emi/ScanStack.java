package dev.reforgedtweaks.integration.emi;

import java.util.List;
import dev.emi.emi.api.stack.*;
import dev.reforgedtweaks.client.data.ScanTarget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** A scan target must never match a real spawn egg, bucket or paper in recipe searches. */
final class ScanStack extends EmiStack {
    private static final java.util.Map<String,ScanTarget> TARGETS = new java.util.concurrent.ConcurrentHashMap<>();
    final ScanTarget target;
    ScanStack(ScanTarget target) { this.target = target; TARGETS.put(target.key(),target); }
    public EmiStack copy() { return new ScanStack(target); }
    public boolean isEmpty() { return false; }
    public DataComponentPatch getComponentChanges() { return DataComponentPatch.EMPTY; }
    public Object getKey() { return target.key(); }
    public ResourceLocation getId() { return ResourceLocation.parse(target.key()); }
    public Component getName() { return target.name(); }
    public List<Component> getTooltipText() {
        var lines = new java.util.ArrayList<Component>(); lines.add(target.name()); lines.addAll(target.details());
        lines.add(Component.translatable("trt.source.scan")); return lines;
    }
    public void render(GuiGraphics graphics, int x, int y, float delta, int flags) {
        EmiStack.of(target.icon()).render(graphics, x, y, delta, flags);
    }
    static void clear() { TARGETS.clear(); }
    static final class Serializer implements dev.emi.emi.api.stack.serializer.EmiIngredientSerializer<ScanStack> {
        public String getType() { return "thaumcraft_reforged_tweaks:scan"; }
        public EmiIngredient deserialize(com.google.gson.JsonElement element) {
            try {
                String key = element.getAsJsonObject().get("key").getAsString();
                var target = TARGETS.get(key);
                if (target == null) target = new ScanTarget(key,Component.literal(key),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER),List.of());
                return new ScanStack(target);
            } catch(RuntimeException invalid) { return EmiStack.EMPTY; }
        }
        public com.google.gson.JsonElement serialize(ScanStack stack) {
            var json = new com.google.gson.JsonObject(); json.addProperty("type",getType()); json.addProperty("key",stack.target.key()); return json;
        }
    }
}

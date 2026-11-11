package dev.reforgedtweaks;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ASPECT_SLOTS;
    public static final ModConfigSpec.BooleanValue BOOK_TWEAKS;
    public static final ModConfigSpec.BooleanValue TAB_DRAG;
    public static final ModConfigSpec.DoubleValue BROWSER_SCALE;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> TAB_ORDER;
    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ASPECT_SLOTS = builder.translation("trt.config.aspect_slots")
            .comment("Add interactive aspect ingredients to the four existing REFORGED recipe categories.")
            .define("jei.aspect_slots", true);
        BOOK_TWEAKS = builder.translation("trt.config.book_tweaks").define("book.enabled", true);
        TAB_DRAG = builder.translation("trt.config.tab_drag").define("book.ctrl_drag_tabs", true);
        BROWSER_SCALE = builder.translation("trt.config.browser_scale")
            .comment("Research browser viewport multiplier. Changed and saved from the in-game book settings. Icons retain their size.")
            .defineInRange("book.browserScale", 1.0, 0.5, 4.0);
        TAB_ORDER = builder.defineListAllowEmpty("book.tab_order", java.util.List.<String>of(), () -> "",
            value -> value instanceof String key && !key.isBlank() && key.length() <= 256);
        SPEC = builder.build();
    }
    private ClientConfig() {}

    public static boolean bookTweaks() { return !SPEC.isLoaded() || BOOK_TWEAKS.get(); }
    public static boolean tabDrag() { return !SPEC.isLoaded() || TAB_DRAG.get(); }
    public static double browserScale() { return SPEC.isLoaded() ? BROWSER_SCALE.get() : 1.0; }
    public static java.util.List<String> tabOrder() { return SPEC.isLoaded() ? java.util.List.copyOf(TAB_ORDER.get()) : java.util.List.of(); }
    public static void saveScale(double value) {
        if (SPEC.isLoaded()) { BROWSER_SCALE.set(value); SPEC.save(); }
    }
    public static void saveTabOrder(java.util.List<String> value) {
        if (SPEC.isLoaded()) { TAB_ORDER.set(java.util.List.copyOf(value)); SPEC.save(); }
    }
    public static boolean aspectSlots() {
        return !SPEC.isLoaded() || ASPECT_SLOTS.get();
    }
}

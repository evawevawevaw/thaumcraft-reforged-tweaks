package dev.reforgedtweaks.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import net.neoforged.fml.ModList;

/** Optional EMI entry point. JEI-dependent code is loaded only when JEI is installed. */
@EmiEntrypoint
public final class ReforgedEmiPlugin implements EmiPlugin {
    public void initialize(dev.emi.emi.api.EmiInitRegistry registry) {
        registry.addIngredientSerializer(AspectStack.class, new AspectStack.Serializer());
        registry.addIngredientSerializer(ScanStack.class, new ScanStack.Serializer());
        if (!ModList.get().isLoaded("jei")) NativeReload.install();
    }
    public void register(EmiRegistry registry) {
        boolean standalone = !ModList.get().isLoaded("jei");
        boolean witchingGadgets = ModList.get().isLoaded("witchinggadgets");
        var logger = org.slf4j.LoggerFactory.getLogger(ReforgedEmiPlugin.class);
        logger.info("TWEAKS 0.3.15 EMI registration: standalone={}, witchinggadgets={}", standalone, witchingGadgets);
        if (standalone) { NativeReload.registering(); ScanStack.clear(); }
        // Register independent machine recipes before the broader REFORGED catalogue.
        // A failure building an unrelated dynamic infusion must not prevent these recipes.
        if (standalone && witchingGadgets) {
            try { WitchingGadgetsCatalog.register(registry); }
            catch (RuntimeException | LinkageError failure) {
                logger.error("Witching Gadgets native EMI registration failed", failure);
            }
        }
        NativeCatalog.register(registry, standalone, standalone ? (aspect, amount) -> new AspectStack(aspect, amount)
            : dev.reforgedtweaks.integration.emi.jei.AspectBridge::stack);
        if (ModList.get().isLoaded("jei")) {
            dev.reforgedtweaks.integration.emi.jei.BookmarkBridge.register(registry);
        }
    }
}

package dev.reforgedtweaks.integration.jei;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Inspects bytecode without loading JEI or target classes on an incompatible installation. */
public final class JeiMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(JeiMixinPlugin.class);
    private static final String PREFIX = "(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;L";
    private static final String SUFFIX = ";Lmezz/jei/api/recipe/IFocusGroup;)V";
    private static final Map<String, String> RECIPES = Map.of(
        "thaumcraft.client.jei.CrucibleCategory", "thaumcraft/crafting/CrucibleRecipe",
        "thaumcraft.client.jei.ArcaneCategory", "thaumcraft/client/jei/ArcaneDisplay",
        "thaumcraft.client.jei.InfusionCategory", "thaumcraft/crafting/InfusionRecipe",
        "thaumcraft.client.jei.EnchantmentCategory", "thaumcraft/client/jei/EnchantmentCategory$Entry");
    public void onLoad(String mixinPackage) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        var mods = FMLLoader.getLoadingModList();
        if (FMLLoader.getDist() != Dist.CLIENT || mods == null || mods.getModFileById("jei") == null || mods.getModFileById("thaumcraft") == null) return false;
        String recipe = RECIPES.get(targetClassName);
        if (recipe == null) return false;
        try {
            var target = MixinService.getService().getBytecodeProvider().getClassNode(targetClassName);
            boolean matches = target.methods.stream().anyMatch(method -> method.name.equals("setRecipe") && method.desc.equals(PREFIX + recipe + SUFFIX));
            if (!matches) LOGGER.warn("Skipping incompatible JEI aspect hook: {}", targetClassName);
            return matches;
        } catch (Exception exception) {
            LOGGER.warn("Skipping unavailable JEI aspect hook: {}", targetClassName, exception);
            return false;
        }
    }
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    public List<String> getMixins() { return null; }
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        LOGGER.info("JEI aspect-slot hook applied to {}", targetClassName);
    }
}

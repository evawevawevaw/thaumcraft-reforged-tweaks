package dev.reforgedtweaks;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(ReforgedTweaks.MOD_ID)
public final class ReforgedTweaks {
    public static final String MOD_ID = "thaumcraft_reforged_tweaks";

    public ReforgedTweaks(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }
}

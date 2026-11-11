package dev.reforgedtweaks.client.book;

import dev.reforgedtweaks.ClientConfig;
import dev.reforgedtweaks.ReforgedTweaks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import thaumcraft.client.research.ResearchBrowserScreen;

@EventBusSubscriber(modid = ReforgedTweaks.MOD_ID, value = Dist.CLIENT)
public final class BookScreens {
    private BookScreens() {}
    @SubscribeEvent
    public static void opening(ScreenEvent.Opening event) {
        // Exact class only: leave third-party subclasses/custom screens in control.
        if (ClientConfig.bookTweaks() && event.getNewScreen() != null
            && event.getNewScreen().getClass() == ResearchBrowserScreen.class) {
            event.setNewScreen(new TweakedResearchBrowser());
        }
    }
}

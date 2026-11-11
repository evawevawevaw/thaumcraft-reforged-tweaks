package dev.reforgedtweaks.integration.jei;

import dev.reforgedtweaks.client.data.AspectSources;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Viewer adapter only; source queries are shared with standalone EMI. */
final class SourceCatalog {
    private final AspectSources data = new AspectSources();
    List<AspectSources.Page> pages() { return data.pages(); }
    void discard() { data.discard(); }
    void clear() { data.clear(); SourceToken.CURRENT.clear(); }
    void rebuild(Iterable<ItemStack> variants) {
        clear();
        data.rebuild(variants);
        data.pages().stream().flatMap(page -> page.sources().stream()).filter(entry -> entry.token() != null)
            .forEach(entry -> SourceToken.of(entry.token()));
    }
    List<AspectSources.Page> focused(ItemStack stack) { return data.focused(stack); }
}

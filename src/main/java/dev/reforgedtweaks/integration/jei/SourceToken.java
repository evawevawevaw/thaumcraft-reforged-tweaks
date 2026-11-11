package dev.reforgedtweaks.integration.jei;

import com.mojang.serialization.Codec;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.ingredients.IIngredientType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** A scan target is not an item that can be melted into essentia. */
record SourceToken(String key, Component name, ItemStack icon, List<Component> details) {
    static final IIngredientType<SourceToken> TYPE = () -> SourceToken.class;
    static final Map<String, SourceToken> CURRENT = new ConcurrentHashMap<>();
    static final Codec<SourceToken> CODEC = Codec.STRING.xmap(SourceToken::resolve, SourceToken::key);
    SourceToken { icon = icon.copy(); details = List.copyOf(details); }
    static SourceToken of(dev.reforgedtweaks.client.data.ScanTarget target) {
        return CURRENT.computeIfAbsent(target.key(), key -> new SourceToken(key, target.name(), target.icon(), target.details()));
    }
    private static SourceToken resolve(String key) {
        return CURRENT.getOrDefault(key, new SourceToken(key, Component.literal(key), new ItemStack(Items.PAPER), List.of()));
    }
}

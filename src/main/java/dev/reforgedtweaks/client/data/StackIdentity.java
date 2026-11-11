package dev.reforgedtweaks.client.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Component-aware bookmark identity, independent of which world created enchantment holders. */
public final class StackIdentity {
    private StackIdentity() {}

    public static JsonElement encode(ItemStack stack, HolderLookup.Provider registries) {
        // Static recipe templates can retain server/previous-world holders. Codecs require
        // holders owned by this exact registry set, even when their resource keys match.
        // Rebind only a private copy; never alter the recipe, result or player's inventory.
        ItemStack copy = stack.copy();
        rebind(copy, DataComponents.ENCHANTMENTS, registries);
        rebind(copy, DataComponents.STORED_ENCHANTMENTS, registries);
        return ItemStack.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), copy).getOrThrow();
    }

    private static void rebind(ItemStack copy, DataComponentType<ItemEnchantments> type, HolderLookup.Provider registries) {
        ItemEnchantments original = copy.get(type);
        if (original == null || original.isEmpty()) return;
        var lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        // Start with the original tooltip flag, then clear ALL old key objects. Holders
        // compare equal by resource key in NeoForge; putting a replacement into the old
        // map without clearing can retain the original (foreign-owner) key object.
        var normalized = new ItemEnchantments.Mutable(original);
        normalized.removeIf(holder -> true);
        for (var entry : original.entrySet()) {
            var key = entry.getKey().unwrapKey().orElseThrow(() ->
                new IllegalStateException("Unregistered enchantment in recipe identity"));
            normalized.set(lookup.getOrThrow(key), entry.getIntValue());
        }
        copy.set(type, normalized.toImmutable());
    }
}

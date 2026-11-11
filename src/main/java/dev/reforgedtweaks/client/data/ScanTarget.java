package dev.reforgedtweaks.client.data;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public record ScanTarget(String key, Component name, ItemStack icon, List<Component> details) {
    public ScanTarget { icon = icon.copy(); details = List.copyOf(details); }
}

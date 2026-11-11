package dev.reforgedtweaks.client.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.aspect.AspectRegistry;
import thaumcraft.crafting.CrucibleCrafting;

public final class AspectSources {
    public record Entry(ItemStack stack, ScanTarget token, AspectList aspects) {
        public Entry { stack = stack.copy(); aspects = aspects.copy(); }
    }
    public record Page(Aspect aspect, List<Entry> sources, int index, int count, int total) {
        public Page { sources = List.copyOf(sources); }
    }
    private volatile List<Page> pages = List.of();
    public List<Page> pages() { return pages; }
    public void discard() { pages = List.of(); }
    public void clear() { discard(); }

    public void rebuild(Iterable<ItemStack> variants) {
        clear();
        AspectRegistry registry = AspectRegistry.active();
        List<Entry> sources = new ArrayList<>();
        Map<Item, List<ItemStack>> seen = new HashMap<>();
        for (ItemStack stack : variants) addItem(sources, seen, stack);
        for (Item item : BuiltInRegistries.ITEM) addItem(sources, seen, item.getDefaultInstance());
        registry.exportEntities().forEach((type, entries) -> {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            for (int index = 0; index < entries.size(); index++) {
                var entry = entries.get(index);
                List<Component> details = entry.conditions().stream()
                    .map(condition -> (Component) Component.translatable("trt.source.condition", condition.name(), String.valueOf(condition.value())))
                    .toList();
                Item icon = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_spawn_egg")).orElse(Items.PAPER);
                ScanTarget token = new ScanTarget("thaumcraft_reforged_tweaks:entity/" + id.getNamespace() + "/" + id.getPath() + "/" + index,
                    type.getDescription(), new ItemStack(icon), details);
                addToken(sources, token, entry.aspects());
            }
        });
        world(sources, "fire", Items.FIRE_CHARGE, new AspectList().add(Aspect.FIRE, 4));
        world(sources, "water", Items.WATER_BUCKET, new AspectList().add(Aspect.WATER, 4));
        world(sources, "lava", Items.LAVA_BUCKET, new AspectList().add(Aspect.FIRE, 3).add(Aspect.EARTH, 1));
        if (registry != AspectRegistry.active()) {
            clear();
            dev.reforgedtweaks.client.ClientLifecycle.resourcesChanged();
            return;
        }
        Map<Aspect, List<Entry>> grouped = new LinkedHashMap<>();
        for (Entry entry : sources) for (Aspect aspect : entry.aspects().getAspectsSorted()) {
            if (entry.aspects().getAmount(aspect) > 0) grouped.computeIfAbsent(aspect, ignored -> new ArrayList<>()).add(entry);
        }
        pages = grouped.entrySet().stream().sorted(Map.Entry.comparingByKey(java.util.Comparator.comparing(Aspect::getTag)))
            .flatMap(entry -> pages(entry.getKey(), entry.getValue()).stream()).toList();
    }

    public List<Page> focused(ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        ItemStack single = stack.copyWithCount(1);
        AspectList aspects = CrucibleCrafting.dissolveTags(single);
        return aspects.getAspectsSorted().stream().filter(aspect -> aspects.getAmount(aspect) > 0)
            .flatMap(aspect -> pages(aspect, List.of(new Entry(single, null, aspects))).stream()).toList();
    }

    private static List<Page> pages(Aspect aspect, List<Entry> entries) {
        return SourcePagination.split(entries).stream()
            .map(slice -> new Page(aspect, slice.entries(), slice.index(), slice.count(), slice.total())).toList();
    }

    private static void addItem(List<Entry> sources, Map<Item, List<ItemStack>> seen, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack single = stack.copyWithCount(1);
        List<ItemStack> known = seen.computeIfAbsent(single.getItem(), ignored -> new ArrayList<>());
        if (known.stream().anyMatch(other -> ItemStack.isSameItemSameComponents(single, other))) return;
        known.add(single);
        AspectList aspects = CrucibleCrafting.dissolveTags(single);
        if (!aspects.isEmpty()) sources.add(new Entry(single, null, aspects));
    }
    private static void addToken(List<Entry> sources, ScanTarget token, AspectList aspects) {
        if (aspects.isEmpty()) return;
        sources.add(new Entry(ItemStack.EMPTY, token, aspects));
    }
    private static void world(List<Entry> sources, String block, Item icon, AspectList aspects) {
        addToken(sources, new ScanTarget("thaumcraft_reforged_tweaks:world/" + block,
            Component.translatable("block.minecraft." + block), new ItemStack(icon), List.of()), aspects);
    }
}

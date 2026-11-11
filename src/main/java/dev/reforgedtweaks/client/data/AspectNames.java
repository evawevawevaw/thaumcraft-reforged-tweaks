package dev.reforgedtweaks.client.data;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.aspect.Aspect;

public final class AspectNames {
    private record Terms(Map<String, String> english, Map<String, String> russian) {}
    private static volatile Terms terms = new Terms(Map.of(), Map.of());
    private AspectNames() {}

    public static void reload() {
        Map<String, String> english = new HashMap<>();
        Map<String, String> russian = new HashMap<>();
        read("en_us", english);
        read("ru_ru", russian);
        terms = new Terms(Map.copyOf(english), Map.copyOf(russian));
    }

    private static void read(String language, Map<String, String> destination) {
        var manager = Minecraft.getInstance().getResourceManager();
        for (String namespace : manager.getNamespaces()) {
            for (var resource : manager.getResourceStack(ResourceLocation.fromNamespaceAndPath(namespace, "lang/" + language + ".json"))) {
                try (var reader = resource.openAsReader()) {
                    JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(entry -> {
                        if (entry.getKey().startsWith("tc.aspect.") && entry.getValue().isJsonPrimitive()) {
                            destination.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    });
                } catch (IOException | RuntimeException exception) {
                    org.slf4j.LoggerFactory.getLogger(AspectNames.class).warn("Cannot read aspect search terms from {} ({})", resource.sourcePackId(), language, exception);
                }
            }
        }
    }

    public static String searchable(Aspect aspect) {
        Terms current = terms;
        String key = "tc.aspect." + aspect.getTag();
        return aspect.getName() + " " + aspect.getTag() + " " + aspect.getDescription().getString()
            + " " + current.english().getOrDefault(key, "") + " " + current.russian().getOrDefault(key, "");
    }
}

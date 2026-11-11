package dev.reforgedtweaks.client.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Canonical content identity: independent of reload ordering, translated names and JSON object order. */
public final class RecipeIds {
    private RecipeIds() {}
    public static String digest(JsonElement content) {
        try {
            byte[] bytes = canonical(content).toString().getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Java 21 must provide SHA-256", exception);
        }
    }
    private static JsonElement canonical(JsonElement value) {
        if (value.isJsonObject()) {
            JsonObject result = new JsonObject();
            value.getAsJsonObject().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                .forEach(entry -> result.add(entry.getKey(), canonical(entry.getValue())));
            return result;
        }
        if (value.isJsonArray()) {
            JsonArray result = new JsonArray();
            value.getAsJsonArray().forEach(entry -> result.add(canonical(entry)));
            return result;
        }
        return value.deepCopy();
    }
}

package dev.reforgedtweaks.integration.emi;

import com.google.gson.JsonElement;

public final class StableRecipeId {
    private StableRecipeId() {}
    public static String digest(JsonElement content) { return dev.reforgedtweaks.client.data.RecipeIds.digest(content); }
}

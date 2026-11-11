package dev.reforgedtweaks.integration.emi;

import com.google.gson.JsonParser;
import org.junit.Test;
import static org.junit.Assert.*;

public class StableRecipeIdTest {
    private String id(String json) { return StableRecipeId.digest(JsonParser.parseString(json)); }
    @Test public void objectOrderDoesNotChangeBookmarksAfterReload() {
        assertEquals(id("{\"category\":\"arcane\",\"output\":{\"item\":\"wand\",\"rod\":\"wood\"}}"),
                     id("{\"output\":{\"rod\":\"wood\",\"item\":\"wand\"},\"category\":\"arcane\"}"));
    }
    @Test public void differentComponentsWithSameOutputItemHaveDifferentRecipeIds() {
        assertNotEquals(id("{\"output\":{\"item\":\"wand\",\"rod\":\"wood\"}}"),
                        id("{\"output\":{\"item\":\"wand\",\"rod\":\"silverwood\"}}"));
    }
    @Test public void sameOutputButDifferentInputGridDoesNotOverwriteFavorite() {
        assertNotEquals(id("{\"output\":\"wand\",\"inputs\":[\"cap\",\"rod\"]}"),
                        id("{\"output\":\"wand\",\"inputs\":[\"rod\",\"cap\"]}"));
    }
    @Test public void costsAndCategoriesArePartOfRecipeIdentity() {
        assertNotEquals(id("{\"category\":\"arcane\",\"amount\":1}"), id("{\"category\":\"arcane\",\"amount\":2}"));
        assertNotEquals(id("{\"category\":\"arcane\",\"amount\":1}"), id("{\"category\":\"infusion\",\"amount\":1}"));
    }
}

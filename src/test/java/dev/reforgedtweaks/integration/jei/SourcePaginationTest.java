package dev.reforgedtweaks.integration.jei;

import dev.reforgedtweaks.client.data.SourcePagination;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Test;
import static org.junit.Assert.*;

public class SourcePaginationTest {
    @Test public void hundredsOfWandVariantsStayBoundedWithoutDroppingSources() {
        List<Integer> sources = IntStream.range(0, 743).boxed().toList();
        var pages = SourcePagination.split(sources);
        assertEquals(42, pages.size());
        assertEquals(sources, pages.stream().flatMap(page -> page.entries().stream()).toList());
        for (var page : pages) {
            assertTrue(page.entries().size() <= 18);
            assertEquals(743, page.total());
            assertEquals(42, page.count());
        }
    }
    @Test public void allSlotsFitInsideTheRecipeFrame() {
        for (int index = 0; index < SourcePagination.PER_PAGE; index++) {
            int x = SourcePagination.x(index);
            int y = SourcePagination.y(index);
            assertTrue(x >= 0 && x + 16 <= 142);
            assertTrue(y >= 46 && y + 16 <= 108);
        }
    }
    @Test public void exactBoundaryAndNextSourceProduceCorrectPageCounts() {
        assertEquals(1, SourcePagination.split(IntStream.range(0, 18).boxed().toList()).size());
        var pages = SourcePagination.split(IntStream.range(0, 19).boxed().toList());
        assertEquals(2, pages.size());
        assertEquals(List.of(18), pages.get(1).entries());
        assertEquals(1, pages.get(1).index());
    }
    @Test public void emptySourceListHasNoFakeRecipePage() {
        assertTrue(SourcePagination.split(List.of()).isEmpty());
    }
    @Test public void reloadCannotMutatePreviouslyPublishedPages() {
        List<String> entries = new ArrayList<>(List.of("wand", "stone"));
        var pages = SourcePagination.split(entries);
        entries.clear();
        assertEquals(List.of("wand", "stone"), pages.getFirst().entries());
        assertThrows(UnsupportedOperationException.class, () -> pages.getFirst().entries().clear());
    }
}

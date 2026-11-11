package dev.reforgedtweaks.client.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class InfusionGeometryTest {
    private boolean overlaps(InfusionGeometry.Point a, InfusionGeometry.Point b) {
        return Math.abs(a.x() - b.x()) < 18 && Math.abs(a.y() - b.y()) < 18;
    }

    @Test public void allPedestalsRemainVisibleWithoutOverlapsIncludingOverflow() {
        for (int count = 0; count <= 88; count++) {
            var layout = InfusionGeometry.of(count);
            assertEquals(count, layout.pedestals().size());
            var center = new InfusionGeometry.Point(InfusionGeometry.CENTER_X, InfusionGeometry.CENTER_Y);
            for (int i = 0; i < count; i++) {
                var point = layout.pedestals().get(i);
                assertTrue(point.x() >= 1 && point.x() + 18 <= 165);
                assertTrue(point.y() >= 1 && point.y() + 18 <= layout.aspectY() - 4);
                assertFalse(overlaps(point, center));
                assertFalse(overlaps(point, new InfusionGeometry.Point(InfusionGeometry.OUTPUT_X, InfusionGeometry.OUTPUT_Y)));
                for (int j = 0; j < i; j++) assertFalse("count=" + count, overlaps(point, layout.pedestals().get(j)));
            }
        }
    }

    @Test public void usualAltarRecipesAreArrangedOnRing() {
        for (int count = 1; count <= 8; count++) for (var point : InfusionGeometry.of(count).pedestals()) {
            assertEquals(40, Math.hypot(point.x() - 74, point.y() - 58), 0.8);
        }
    }

    @Test public void aspectRowsAndCostFooterFitForLargeAddonRecipes() {
        var layout = InfusionGeometry.of(41);
        for (int aspects = 0; aspects <= 64; aspects++) {
            int rows = Math.max(1, (aspects + 7) / 8);
            assertTrue(layout.height(aspects) >= layout.aspectY() + rows * 20 + 20);
        }
    }

    @Test(expected = IllegalArgumentException.class) public void invalidCountRejected() { InfusionGeometry.of(-1); }
}

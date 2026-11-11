package dev.reforgedtweaks.client.data;

import java.util.ArrayList;
import java.util.List;

/** Legacy NEI altar: result above, central input below, components around it.
 * Extra pedestals continue below the diagram rather than overlapping the original ring.
 */
public record InfusionGeometry(List<Point> pedestals, int aspectY) {
    public record Point(int x, int y) {}
    // Slot origins are one pixel above/left of the legacy PositionedStack item origins.
    public static final int CENTER_X = 74, CENTER_Y = 57;
    public static final int OUTPUT_X = 74, OUTPUT_Y = 0;

    public static InfusionGeometry of(int count) {
        if (count < 0) throw new IllegalArgumentException("Negative pedestal count");
        List<Point> points = new ArrayList<>();
        int ring = Math.min(8, count);
        for (int i = 0; i < ring; i++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * i / ring;
            points.add(new Point(74 + (int)Math.round(40 * Math.cos(angle)),
                58 + (int)Math.round(40 * Math.sin(angle))));
        }
        for (int i = 8; i < count; i++) points.add(new Point(4 + (i - 8) % 8 * 20, 122 + (i - 8) / 8 * 20));
        int rows = (Math.max(0, count - 8) + 7) / 8;
        return new InfusionGeometry(List.copyOf(points), rows == 0 ? 122 : 126 + rows * 20);
    }

    public int height(int aspects) { return aspectY + Math.max(1, (aspects + 7) / 8) * 20 + 20; }
}

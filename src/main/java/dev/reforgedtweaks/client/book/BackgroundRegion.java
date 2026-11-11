package dev.reforgedtweaks.client.book;

/** One continuous texture crop. Screen-space parallax does not accelerate as the viewport grows. */
public record BackgroundRegion(float u, float v, int width, int height) {
    private static final int BASE_WIDTH = 224;
    private static final int BASE_HEIGHT = 196;
    private static final double MAX_SPEED = .5;

    public static BackgroundRegion at(double mapX, double mapY, int minColumn, int maxColumn,
                                     int minRow, int maxRow, int viewWidth, int viewHeight) {
        return new BackgroundRegion(
            offset(mapX, minColumn, maxColumn, viewWidth, BASE_WIDTH),
            offset(mapY, minRow, maxRow, viewHeight, BASE_HEIGHT), BASE_WIDTH, BASE_HEIGHT);
    }

    private static float offset(double map, int first, int last, int viewport, int baseViewport) {
        int size = Math.max(1, viewport);
        int textureTravel = 512 - baseViewport;
        // Match the category's scale-1 camera bounds, never the shrinking bounds of the enlarged window.
        double baseTravel = Math.max(1, (last - (double) first) * 24 - baseViewport + 102);
        double speed = Math.min(MAX_SPEED, textureTravel / baseTravel);
        double centre = map + size / 2.0;
        double anchor = (first + (double) last) * 12 + 11;
        // Compensate for the crop being stretched to the window: identical drag -> identical GUI-pixel motion.
        double uv = textureTravel / 2.0 + (centre - anchor) * speed * baseViewport / size;
        return (float) Math.max(0, Math.min(textureTravel, uv));
    }
}

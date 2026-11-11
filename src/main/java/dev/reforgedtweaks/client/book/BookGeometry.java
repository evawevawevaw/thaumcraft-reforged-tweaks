package dev.reforgedtweaks.client.book;

/** Pure layout: browserScale enlarges the viewport, not research icons. */
public record BookGeometry(int left, int top, int width, int height) {
    public static BookGeometry fit(int screenWidth, int screenHeight, double requested) {
        double scale = Double.isFinite(requested) ? Math.max(.5, Math.min(4, requested)) : 1;
        scale = Math.min(scale, Math.min(Math.max(64, screenWidth - 64) / 256.0,
            Math.max(64, screenHeight - 34) / 230.0));
        int width = (int) Math.floor(256 * scale);
        int height = (int) Math.floor(230 * scale);
        return new BookGeometry((screenWidth - width) / 2, (screenHeight - height) / 2, width, height);
    }
    public int viewWidth() { return width - 32; }
    public int viewHeight() { return height - 34; }
    public int rows() { return Math.max(1, (height - 18) / 24); }
    public static double step(double current, int direction) {
        return Math.max(.5, Math.min(4, Math.round((current + direction * .1) * 10) / 10.0));
    }
}

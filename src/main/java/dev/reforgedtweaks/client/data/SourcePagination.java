package dev.reforgedtweaks.client.data;

import java.util.ArrayList;
import java.util.List;

public final class SourcePagination {
    public static final int COLUMNS = 6;
    public static final int PER_PAGE = 18;
    public record Slice<T>(List<T> entries, int index, int count, int total) {}
    private SourcePagination() {}
    public static int x(int index) { return 4 + index % COLUMNS * 22; }
    public static int y(int index) { return 46 + index / COLUMNS * 22; }
    public static <T> List<Slice<T>> split(List<T> entries) {
        List<Slice<T>> pages = new ArrayList<>();
        int count = (entries.size() + PER_PAGE - 1) / PER_PAGE;
        for (int start = 0; start < entries.size(); start += PER_PAGE) {
            pages.add(new Slice<>(List.copyOf(entries.subList(start, Math.min(entries.size(), start + PER_PAGE))), start / PER_PAGE, count, entries.size()));
        }
        return List.copyOf(pages);
    }
}

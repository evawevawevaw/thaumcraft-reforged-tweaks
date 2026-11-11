package dev.reforgedtweaks.client.book;

import java.util.*;

/** Stable IDs, never translated names; hidden tabs keep their position and new tabs append. */
public final class TabOrder {
    private TabOrder() {}
    public static List<String> reconcile(List<String> saved, List<String> current) {
        var result = new LinkedHashSet<String>();
        // Keep temporarily unavailable addon IDs, so reinstalling does not erase the user's ordering.
        result.addAll(saved);
        result.addAll(current);
        return List.copyOf(result);
    }
    public static List<String> moveBefore(List<String> order, String moving, String target) {
        return move(order, moving, target, false);
    }
    public static List<String> move(List<String> order, String moving, String target, boolean after) {
        if (moving.equals(target) || !order.contains(moving) || !order.contains(target)) return List.copyOf(order);
        var result = new ArrayList<>(order);
        result.remove(moving);
        result.add(result.indexOf(target) + (after ? 1 : 0), moving);
        return List.copyOf(result);
    }
}

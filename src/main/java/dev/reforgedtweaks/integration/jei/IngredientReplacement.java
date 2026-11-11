package dev.reforgedtweaks.integration.jei;

import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** JEI requires non-empty mutation batches. Always read the captured manager, never an old bookkeeping list. */
final class IngredientReplacement {
    private IngredientReplacement() {}

    static <T> boolean replace(Supplier<? extends Collection<T>> registered, Collection<T> desired,
                              Consumer<List<T>> remove, Consumer<List<T>> add, BooleanSupplier active) {
        if (!active.getAsBoolean()) return false;
        List<T> current = List.copyOf(registered.get());
        List<T> next = List.copyOf(desired);
        if (!active.getAsBoolean()) return false;
        if (!current.isEmpty()) remove.accept(current);
        // JEI/EMI callbacks may invalidate the runtime synchronously during a mutation.
        if (!active.getAsBoolean()) return false;
        if (!next.isEmpty()) add.accept(next);
        return active.getAsBoolean();
    }
}

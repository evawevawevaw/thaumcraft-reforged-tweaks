package dev.reforgedtweaks.integration.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import static org.junit.Assert.*;

public class IngredientReplacementTest {
    /** Reproduces JEI's non-empty mutation precondition and rejects removal of absent ingredients. */
    private static final class StrictManager {
        final List<String> ingredients = new ArrayList<>();
        final List<String> calls = new ArrayList<>();
        boolean failNextAdd;
        void remove(List<String> values) {
            assertFalse("JEI rejects empty removal", values.isEmpty());
            assertTrue("Batch belongs to this manager", ingredients.containsAll(values));
            calls.add("remove:" + values.size());
            ingredients.removeAll(values);
        }
        void add(List<String> values) {
            assertFalse("JEI rejects empty addition", values.isEmpty());
            calls.add("add:" + values.size());
            if (failNextAdd) { failNextAdd = false; throw new IllegalStateException("interrupted refresh"); }
            ingredients.addAll(values);
        }
        boolean replace(List<String> next) {
            return IngredientReplacement.replace(() -> ingredients, next, this::remove, this::add, () -> true);
        }
    }

    @Test
    public void emptyStartupTargetsNeverReachJeiMutationApi() {
        StrictManager manager = new StrictManager();
        assertTrue(manager.replace(List.of()));
        assertTrue(manager.calls.isEmpty());
        assertTrue(manager.replace(List.of("entity", "fire")));
        assertEquals(List.of("add:2"), manager.calls);
    }

    @Test
    public void refreshAfterFailureReadsActualManagerInsteadOfRemovingOldListAgain() {
        StrictManager manager = new StrictManager();
        manager.ingredients.addAll(List.of("aer", "ignis"));
        manager.failNextAdd = true;
        assertThrows(IllegalStateException.class, () -> manager.replace(List.of("aer", "ignis")));
        assertTrue(manager.ingredients.isEmpty());
        assertTrue(manager.replace(List.of("aer", "ignis")));
        assertEquals(List.of("remove:2", "add:2", "add:2"), manager.calls);
        assertEquals(List.of("aer", "ignis"), manager.ingredients);
    }

    @Test
    public void clearingTargetsDoesNotCallAddWithEmptyList() {
        StrictManager manager = new StrictManager();
        manager.ingredients.add("entity");
        assertTrue(manager.replace(List.of()));
        assertEquals(List.of("remove:1"), manager.calls);
        assertTrue(manager.ingredients.isEmpty());
    }

    @Test
    public void runtimeInvalidatedDuringReadIsNotMutated() {
        StrictManager manager = new StrictManager();
        manager.ingredients.add("ignis");
        AtomicBoolean active = new AtomicBoolean(true);
        boolean completed = IngredientReplacement.replace(() -> {
            active.set(false);
            return manager.ingredients;
        }, List.of("aer"), manager::remove, manager::add, active::get);
        assertFalse(completed);
        assertTrue(manager.calls.isEmpty());
        assertEquals(List.of("ignis"), manager.ingredients);
    }

    @Test
    public void runtimeInvalidatedDuringRemovalReceivesNoLaterAddition() {
        StrictManager manager = new StrictManager();
        manager.ingredients.add("ignis");
        AtomicBoolean active = new AtomicBoolean(true);
        assertFalse(IngredientReplacement.replace(() -> manager.ingredients, List.of("aer"), values -> {
            manager.remove(values);
            active.set(false);
        }, manager::add, active::get));
        assertEquals(List.of("remove:1"), manager.calls);
    }

    @Test
    public void replacementRuntimeDoesNotInheritOldManagersBookkeeping() {
        StrictManager oldRuntime = new StrictManager();
        oldRuntime.ingredients.add("ignis");
        oldRuntime.replace(List.of("aer"));
        StrictManager newRuntime = new StrictManager();
        assertTrue(newRuntime.replace(List.of("ignis")));
        assertEquals(List.of("add:1"), newRuntime.calls);
        assertEquals(List.of("aer"), oldRuntime.ingredients);
    }

    @Test
    public void inactiveRuntimeDoesNotReadOrMutateManager() {
        assertFalse(IngredientReplacement.replace(() -> { fail("Stale runtime read"); return List.<String>of(); },
            List.of("ignis"), values -> fail("Stale runtime removal"), values -> fail("Stale runtime addition"), () -> false));
    }
}

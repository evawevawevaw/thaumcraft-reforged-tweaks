package dev.reforgedtweaks.integration.jei;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionEnchantmentRecipe;

/** User-approved read-only reflection for exactly two package-private REFORGED records. */
public final class RecipeCosts {
    private static final Set<Class<?>> REPORTED = ConcurrentHashMap.newKeySet();
    private static final ClassValue<Optional<Method>> READERS = new ClassValue<>() {
        protected Optional<Method> computeValue(Class<?> type) {
            String methodName;
            Class<?> expected;
            if (type.getName().equals("thaumcraft.client.jei.ArcaneDisplay")) {
                methodName = "aspects";
                expected = AspectList.class;
            } else if (type.getName().equals("thaumcraft.client.jei.EnchantmentCategory$Entry")) {
                methodName = "recipe";
                expected = InfusionEnchantmentRecipe.class;
            } else {
                return Optional.empty();
            }
            try {
                Method method = type.getDeclaredMethod(methodName);
                if (method.getReturnType() == expected && method.trySetAccessible()) return Optional.of(method);
            } catch (NoSuchMethodException | SecurityException exception) {
                report(type, exception);
            }
            return Optional.empty();
        }
    };
    private RecipeCosts() {}

    public static AspectList read(Object display) {
        var reader = READERS.get(display.getClass());
        if (reader.isPresent()) {
            try {
                Object value = reader.get().invoke(display);
                if (value instanceof AspectList aspects) return aspects;
                if (value instanceof InfusionEnchantmentRecipe recipe) return recipe.aspects();
            } catch (IllegalAccessException | InvocationTargetException exception) {
                report(display.getClass(), exception);
            }
        }
        report(display.getClass(), null);
        return new AspectList();
    }
    private static void report(Class<?> type, Exception exception) {
        if (REPORTED.add(type)) ReforgedJei.LOGGER.error("Cannot read approved JEI aspect costs for {}; slots for this display are disabled", type.getName(), exception);
    }
}

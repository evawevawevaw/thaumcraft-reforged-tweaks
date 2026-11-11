package dev.reforgedtweaks.integration.jei;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import thaumcraft.aspect.Aspect;

record Combination(Aspect first, Aspect second, Aspect result) {
    static List<Combination> all() {
        return Arrays.stream(Aspect.values()).filter(aspect -> !aspect.isPrimal())
            .sorted(Comparator.comparing(Aspect::getTag)).map(aspect ->
                new Combination(aspect.getComponents()[0], aspect.getComponents()[1], aspect)).toList();
    }
}

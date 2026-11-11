package dev.reforgedtweaks.integration.jei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mezz.jei.api.ingredients.IIngredientType;
import thaumcraft.aspect.Aspect;

public record AspectIngredient(Aspect aspect, int amount) {
    public static final IIngredientType<AspectIngredient> TYPE = () -> AspectIngredient.class;
    public static final Codec<AspectIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Aspect.CODEC.fieldOf("aspect").forGetter(AspectIngredient::aspect),
        Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("amount", 1).forGetter(AspectIngredient::amount)
    ).apply(instance, AspectIngredient::new));
    public AspectIngredient(Aspect aspect) { this(aspect, 1); }
    public AspectIngredient {
        if (aspect == null || amount < 1) throw new IllegalArgumentException("Invalid aspect ingredient");
    }
}

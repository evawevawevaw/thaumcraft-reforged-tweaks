package dev.reforgedtweaks.integration.emi.jei;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.jemi.JemiUtil;
import dev.reforgedtweaks.integration.jei.AspectIngredient;
import thaumcraft.aspect.Aspect;

/** Reuse the already imported ingredient identity, so aspect clicks connect to JEI source/combination cards. */
public final class AspectBridge {
    private AspectBridge() {}
    public static EmiIngredient stack(Aspect aspect,int amount) {
        return JemiUtil.getStack(AspectIngredient.TYPE,new AspectIngredient(aspect,amount)).setAmount(amount);
    }
}

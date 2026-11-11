package dev.reforgedtweaks.integration.jei;

import java.util.Optional;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class ItemTransfer<C extends AbstractContainerMenu, R> implements IRecipeTransferHandler<C, R> {
    private final IRecipeTransferHandler<C, R> delegate;
    private final IRecipeTransferHandlerHelper helper;
    private final Class<? extends C> menu;
    private final RecipeType<R> type;
    public ItemTransfer(IRecipeTransferHandlerHelper helper, Class<? extends C> menu, MenuType<C> menuType,
                        RecipeType<R> type, int gridStart, int gridCount, int inventoryStart, int inventoryCount) {
        this.helper = helper; this.menu = menu; this.type = type;
        this.delegate = helper.createUnregisteredRecipeTransferHandler(helper.createBasicRecipeTransferInfo(menu, menuType, type,
            gridStart, gridCount, inventoryStart, inventoryCount));
    }
    public Class<? extends C> getContainerClass() { return menu; }
    public Optional<MenuType<C>> getMenuType() { return delegate.getMenuType(); }
    public RecipeType<R> getRecipeType() { return type; }
    public IRecipeTransferError transferRecipe(C container, R recipe, IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
        IRecipeSlotsView itemsOnly = helper.createRecipeSlotsView(slots.getSlotViews().stream()
            .filter(slot -> slot.getIngredients(AspectIngredient.TYPE).findAny().isEmpty()).toList());
        return delegate.transferRecipe(container, recipe, itemsOnly, player, maxTransfer, doTransfer);
    }
}

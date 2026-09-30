package com.alonie.brbe.jei.plugins.loader;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** {@link IRecipeCatalystRegistration} implementation that records every
 *  workstation association ({@code recipeType -> item}) reported by the loaded
 *  JEI plugins, instead of registering them with a JEI runtime.  Only item
 *  ingredients are collected — workstation blocks are always items. */
public final class CatalystCollector implements IRecipeCatalystRegistration {

    private final Map<Identifier, Set<Identifier>> collected = new LinkedHashMap<>();

    public Map<Identifier, Set<Identifier>> collected() {
        return collected;
    }

    @Override
    public void addCraftingStation(IRecipeType<?> recipeType, ItemLike... ingredients) {
        if (ingredients == null) return;
        for (ItemLike itemLike : ingredients) {
            if (itemLike != null && itemLike.asItem() != null) {
                add(recipeType, itemLike.asItem());
            }
        }
    }

    // Gameoverse 26.1.2 backport: JEI 29 declares a SlotDisplay overload.
    @Override
    public void addCraftingStation(IRecipeType<?> recipeType, net.minecraft.world.item.crafting.display.SlotDisplay display) {
        if (display == null) return;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return;
        for (net.minecraft.world.item.ItemStack stack : display.resolveForStacks(
                net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(mc.level))) {
            if (!stack.isEmpty()) {
                add(recipeType, stack.getItem());
            }
        }
    }

    @Override
    public <T> void addCraftingStation(IRecipeType<?> recipeType, IIngredientType<T> ingredientType, T ingredient) {
        addTyped(recipeType, ingredientType, ingredient);
    }

    @Override
    public <T> void addCraftingStations(IRecipeType<?> recipeType, IIngredientType<T> ingredientType, List<T> ingredients) {
        if (ingredients == null) return;
        for (T ingredient : ingredients) {
            addTyped(recipeType, ingredientType, ingredient);
        }
    }

    @Override
    public <T> void addRecipeCatalyst(IIngredientType<T> ingredientType, T ingredient, IRecipeType<?>... recipeTypes) {
        if (recipeTypes == null) return;
        for (IRecipeType<?> recipeType : recipeTypes) {
            addTyped(recipeType, ingredientType, ingredient);
        }
    }

    @Override
    public IIngredientManager getIngredientManager() {
        return com.alonie.brbe.jei.plugins.stub.JeiRuntimeView.ingredientManager();
    }

    @Override
    public IJeiHelpers getJeiHelpers() {
        return com.alonie.brbe.jei.plugins.stub.JeiHelpersStub.INSTANCE;
    }

    private <T> void addTyped(IRecipeType<?> recipeType, IIngredientType<T> ingredientType, T ingredient) {
        if (recipeType == null || ingredient == null) return;
        if (ingredientType != VanillaTypes.ITEM_STACK) return;
        Item item = ((ItemStack) ingredient).getItem();
        if (item != null) add(recipeType, item);
    }

    private void add(IRecipeType<?> recipeType, Item item) {
        if (recipeType == null || item == null) return;
        Identifier uid = recipeType.getUid();
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        if (uid == null || itemId == null) return;
        collected.computeIfAbsent(uid, k -> new LinkedHashSet<>()).add(itemId);
    }
}

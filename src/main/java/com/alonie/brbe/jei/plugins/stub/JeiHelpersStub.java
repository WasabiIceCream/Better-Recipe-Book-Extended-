package com.alonie.brbe.jei.plugins.stub;

import com.mojang.serialization.Codec;
import mezz.jei.api.helpers.ICodecHelper;
import mezz.jei.api.helpers.IColorHelper;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.helpers.IModIdHelper;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IIngredientVisibility;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Minimal {@link IJeiHelpers}: provides a {@link GuiHelperStub} (the one helper
 * plugin category constructors actually need), and null/empty for the rest.
 * BRBE only reads the recipes a plugin exposes via {@code setRecipe}.
 *
 * <p>[Gameoverse backport] Apart from the GUI helper (kept: it records category
 * backgrounds), every helper now comes from the JEI runtime when one exists
 * (see {@link JeiRuntimeView}); the ingredient manager is its read-only view.
 * Null/empty only while no runtime is up.</p>
 */
public final class JeiHelpersStub implements IJeiHelpers {

    public static final JeiHelpersStub INSTANCE = new JeiHelpersStub();

    private JeiHelpersStub() {}

    @Override
    public IGuiHelper getGuiHelper() {
        return GuiHelperStub.INSTANCE;
    }

    @Override
    public IStackHelper getStackHelper() {
        IJeiHelpers h = JeiRuntimeView.runtimeHelpers();
        return h == null ? null : h.getStackHelper();
    }

    @Override
    public IModIdHelper getModIdHelper() {
        IJeiHelpers h = JeiRuntimeView.runtimeHelpers();
        return h == null ? null : h.getModIdHelper();
    }

    @Override
    public IFocusFactory getFocusFactory() {
        IJeiHelpers h = JeiRuntimeView.runtimeHelpers();
        return h == null ? null : h.getFocusFactory();
    }

    @Override
    public IColorHelper getColorHelper() {
        IJeiHelpers h = JeiRuntimeView.runtimeHelpers();
        return h == null ? null : h.getColorHelper();
    }

    @Override
    public IPlatformFluidHelper<?> getPlatformFluidHelper() {
        IJeiHelpers h = JeiRuntimeView.runtimeHelpers();
        return h == null ? null : h.getPlatformFluidHelper();
    }

    @Override
    public <T> Optional<IRecipeType<T>> getRecipeType(Identifier uid, Class<? extends T> recipeClass) {
        IJeiHelpers h = JeiRuntimeView.runtimeHelpers();
        return h == null ? Optional.empty() : h.getRecipeType(uid, recipeClass);
    }

    @Override
    public Optional<IRecipeType<?>> getRecipeType(Identifier uid) {
        IJeiHelpers h = JeiRuntimeView.runtimeHelpers();
        return h == null ? Optional.empty() : h.getRecipeType(uid);
    }

    @Override
    public Stream<IRecipeType<?>> getAllRecipeTypes() {
        IJeiHelpers h = JeiRuntimeView.runtimeHelpers();
        return h == null ? Stream.empty() : h.getAllRecipeTypes();
    }

    @Override
    public IIngredientManager getIngredientManager() {
        return JeiRuntimeView.ingredientManager();
    }

    @Override
    public ICodecHelper getCodecHelper() {
        IJeiHelpers h = JeiRuntimeView.runtimeHelpers();
        return h == null ? null : h.getCodecHelper();
    }

    @Override
    public IVanillaRecipeFactory getVanillaRecipeFactory() {
        return JeiRuntimeView.vanillaRecipeFactory();
    }

    @Override
    public IIngredientVisibility getIngredientVisibility() {
        IJeiHelpers h = JeiRuntimeView.runtimeHelpers();
        return h == null ? null : h.getIngredientVisibility();
    }
}

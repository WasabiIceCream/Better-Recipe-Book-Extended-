package com.alonie.brbe.jei.plugins.stub;

import com.alonie.brbe.jei.plugins.engine.JeiRuntimeBridge;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Set;

/**
 * [Gameoverse backport] What BRBE's plugin collectors hand to other mods' JEI
 * plugins when they re-run {@code registerCategories}/{@code registerRecipes}.
 *
 * <p>The collectors used to return {@code null} for the ingredient manager, the
 * vanilla recipe factory and the context map, so any plugin that touches them
 * in {@code registerRecipes} (Bits and Balance: brewing factory; Create:
 * {@code getAllIngredients}; Polymer: the manager) threw, and BRBE indexed
 * nothing after that point for the plugin. When a JEI runtime exists (real JEI,
 * or the embedded headless core) these come from it instead:</p>
 * <ul>
 *   <li>ingredient manager: a read-only view. Reads go to the runtime's manager;
 *       {@code addIngredientsAtRuntime}/{@code removeIngredientsAtRuntime}/
 *       {@code registerIngredientListener} are dropped, because the replay must not
 *       change the live JEI ingredient list a second time (Polymer removes and
 *       re-adds its items in {@code registerRecipes}).</li>
 *   <li>vanilla recipe factory: the runtime's, via {@link IJeiHelpers} (it only
 *       builds recipe objects).</li>
 *   <li>context map: {@link SlotDisplayContext#fromLevel}, what JEI itself passes.</li>
 * </ul>
 * All return {@code null} while no runtime is available, as before.
 */
public final class JeiRuntimeView {

    private static final Set<String> MUTATORS = Set.of(
            "addIngredientsAtRuntime", "removeIngredientsAtRuntime", "registerIngredientListener");

    private static IIngredientManager wrappedFor;
    private static IIngredientManager readOnly;

    private JeiRuntimeView() {}

    public static IJeiHelpers runtimeHelpers() {
        IJeiRuntime runtime = JeiRuntimeBridge.runtime();
        return runtime == null ? null : runtime.getJeiHelpers();
    }

    public static synchronized IIngredientManager ingredientManager() {
        IJeiRuntime runtime = JeiRuntimeBridge.runtime();
        IIngredientManager live = runtime == null ? null : runtime.getIngredientManager();
        if (live == null) return null;
        if (live != wrappedFor) {
            wrappedFor = live;
            readOnly = readOnlyView(live);
        }
        return readOnly;
    }

    public static IVanillaRecipeFactory vanillaRecipeFactory() {
        IJeiHelpers helpers = runtimeHelpers();
        return helpers == null ? null : helpers.getVanillaRecipeFactory();
    }

    public static ContextMap contextMap() {
        Minecraft mc = Minecraft.getInstance();
        return mc == null || mc.level == null ? null : SlotDisplayContext.fromLevel(mc.level);
    }

    private static IIngredientManager readOnlyView(IIngredientManager live) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "BRBE read-only view of " + live;
                    default -> method.invoke(live, args);
                };
            }
            if (MUTATORS.contains(method.getName())) return null;
            if (method.isDefault()) {
                // default methods call back into the interface: keep them on the proxy
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            try {
                return method.invoke(live, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        };
        return (IIngredientManager) Proxy.newProxyInstance(
                IIngredientManager.class.getClassLoader(), new Class<?>[]{IIngredientManager.class}, handler);
    }
}

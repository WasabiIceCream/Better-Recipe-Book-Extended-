package com.alonie.brbe.mixins.hideoverlay;

import com.alonie.brbe.BetterRecipeBook;
import com.alonie.brbe.compat.ItemViewCompat;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts A/R/U keys on container screens when the "Hide REI/JEI Overlay" config is enabled.
 * <p>
 * When REI/JEI overlays are hidden (hiding the overlay also switches off JEI's own
 * R/U handling) and BRBE's own viewer is off:
 * - R/U keys: routed to {@link ItemViewCompat} — delegates to each viewer's own key config
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void brbe$handleKeysOnHiddenOverlay(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!BetterRecipeBook.config.hideReiJeiOverlay) {
            return;
        }

        // Gameoverse: only when BRBE's own viewer (LEI) is off; otherwise its handler
        // owns R/U and routing here too would open two views. The A-key swallow the
        // original also did is gone: A is now upstream's default pin key.
        if (BetterRecipeBook.config.recipeViewerEnabled) {
            return;
        }

        int keyCode = event.key();
        int scanCode = event.scancode();

        // R / U: route to the active recipe viewer (delegates to viewer's own key config)
        if (!ItemViewCompat.matchesShowRecipe(keyCode, scanCode)
                && !ItemViewCompat.matchesShowUses(keyCode, scanCode)) {
            return;
        }

        if (!ItemViewCompat.isLoaded()) {
            return;
        }

        Slot slot = this.hoveredSlot;
        if (slot == null || !slot.hasItem()) {
            return;
        }

        ItemStack stack = slot.getItem();
        boolean handled = ItemViewCompat.matchesShowRecipe(keyCode, scanCode)
                ? ItemViewCompat.openRecipeView(stack)
                : ItemViewCompat.openUsageView(stack);

        if (handled) {
            cir.setReturnValue(true);
        }
    }
}

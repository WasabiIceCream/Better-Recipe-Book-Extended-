package com.alonie.brbe.mixins.hideoverlay;

import com.alonie.brbe.BetterRecipeBook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "mezz.jei.gui.overlay.IngredientListOverlay", remap = false)
public abstract class IngredientListOverlayMixin {

    // JEI split the old drawScreen() into drawBackground()/drawForeground() at
    // some point after this mixin was written -- "drawScreen" hasn't existed
    // as a method on this class for a while, so this mixin was silently
    // no-opping (soft mixin, defaultRequire: 0). The config gear button in
    // particular is drawn unconditionally in drawForeground() regardless of
    // isOverlayEnabled(), which is why it kept showing even with the overlay
    // itself correctly hidden.
    @Inject(method = {"drawBackground", "drawForeground"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void brbe$cancelIngredientListOverlay(CallbackInfo ci) {
        if (BetterRecipeBook.config != null && BetterRecipeBook.config.hideReiJeiOverlay) {
            ci.cancel();
        }
    }
}

package com.alonie.brbe.mixins.hideoverlay;

import com.alonie.brbe.BetterRecipeBook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "mezz.jei.gui.overlay.bookmarks.BookmarkOverlay", remap = false)
public abstract class BookmarkOverlayMixin {

    // Same stale-method-name issue as IngredientListOverlayMixin: "drawScreen"
    // no longer exists on this class (split into drawBackground()/drawForeground()),
    // so this mixin was silently no-opping. The bookmark-toggle and
    // lookup-history-toggle buttons are drawn unconditionally in
    // drawForeground() regardless of isBookmarkOverlayEnabled().
    @Inject(method = {"drawBackground", "drawForeground"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void brbe$cancelBookmarkOverlay(CallbackInfo ci) {
        if (BetterRecipeBook.config != null && BetterRecipeBook.config.hideReiJeiOverlay) {
            ci.cancel();
        }
    }
}

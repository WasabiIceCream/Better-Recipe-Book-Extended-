package com.alonie.brbe.mixins.accessors;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets {@link com.alonie.brbe.util.RecipeRefreshDebouncer} run the deferred refresh. */
@Mixin(ClientPacketListener.class)
public interface ClientPacketListenerInvoker {
    @Invoker("refreshRecipeBook")
    void brbe$refreshRecipeBook(ClientRecipeBook book);
}

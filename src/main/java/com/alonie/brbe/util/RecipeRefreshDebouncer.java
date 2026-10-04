package com.alonie.brbe.util;

import com.alonie.brbe.mixins.accessors.ClientPacketListenerInvoker;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;

/**
 * Coalesces recipe-book refreshes (Gameoverse, 2026-10-03). The server sends one recipe-book packet per advancement
 * that unlocks recipes, and a new player picking up a single item can trigger several in a tick and more over the
 * next ticks. Vanilla runs a full {@code refreshRecipeBook} (rebuildCollections + search trees + recipesUpdated, plus
 * this mod's creative-group rebuild, namespace overrides and crafting index) on EVERY packet; a client profile showed
 * those bursts as frame hitches. Packets now only mark the book dirty; one refresh runs once packets have been quiet
 * for {@link #QUIET_TICKS}, at most {@link #MAX_TICKS} after the first, or on the next tick while a recipe book
 * screen is open so the visible book never lags.
 */
public final class RecipeRefreshDebouncer {
    private static final int QUIET_TICKS = 5;
    private static final int MAX_TICKS = 20;

    private static ClientRecipeBook pending;
    private static long ticks;
    private static long firstPacket = -1;
    private static long lastPacket;
    private static boolean flushing;

    private RecipeRefreshDebouncer() {
    }

    /** True while our own flush runs: the refresh must go through. */
    public static boolean flushing() {
        return flushing;
    }

    /** A packet wants a refresh: remember it instead (the caller cancels the immediate refresh). */
    public static void defer(ClientRecipeBook book) {
        pending = book;
        if (firstPacket < 0) {
            firstPacket = ticks;
        }
        lastPacket = ticks;
    }

    public static void tick(Minecraft client) {
        ticks++;
        if (pending == null) {
            return;
        }
        ClientPacketListener connection = client.getConnection();
        if (connection == null) {
            pending = null;
            firstPacket = -1;
            return;
        }
        boolean quiet = ticks - lastPacket >= QUIET_TICKS;
        boolean overdue = ticks - firstPacket >= MAX_TICKS;
        boolean bookOpen = client.screen instanceof AbstractRecipeBookScreen<?>;
        if (!quiet && !overdue && !bookOpen) {
            return;
        }
        ClientRecipeBook book = pending;
        pending = null;
        firstPacket = -1;
        flushing = true;
        try {
            ((ClientPacketListenerInvoker) connection).brbe$refreshRecipeBook(book);
        } finally {
            flushing = false;
        }
    }
}

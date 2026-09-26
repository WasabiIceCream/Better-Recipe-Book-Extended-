package com.alonie.brbe.fabric;

import java.nio.file.Files;
import java.nio.file.Path;

import com.alonie.brbe.BetterRecipeBook;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Gameoverse 26.1.2 backport: upstream renamed the mod id zzzbrbe -> brbe on
 * 2026-08-28, and with it every file BRBE keeps. Players upgrading from the
 * 2.3-beta.3 backport would lose their pins and workstation data. Copy each old
 * file to its new name once, before anything reads them, and never overwrite a
 * new file that already exists. The config (zzzbrbe.toml -> brbe.toml) is
 * included for players who don't get brbe.toml from the server.
 */
final class LegacyFileMigration {
    private LegacyFileMigration() {
    }

    static void run() {
        Path game = FabricLoader.getInstance().getGameDir();
        Path config = FabricLoader.getInstance().getConfigDir();
        copy(game.resolve("zzzbrbe.pins.json"), game.resolve("brbe.pins.json"));
        copy(game.resolve("zzzbrbe.tabpins.json"), game.resolve("brbe.tabpins.json"));
        copy(game.resolve("zzzbrbe.pinoverlays.json"), game.resolve("brbe.pinoverlays.json"));
        copy(config.resolve("zzzbrbe_workstations.json"), config.resolve("brbe_workstations.json"));
        copy(config.resolve("zzzbrbe.toml"), config.resolve("brbe.toml"));
    }

    private static void copy(Path from, Path to) {
        try {
            if (Files.exists(from) && !Files.exists(to)) {
                Files.copy(from, to);
                BetterRecipeBook.LOGGER.info("[BRBE] Migrated {} -> {}", from.getFileName(), to.getFileName());
            }
        } catch (Exception e) {
            BetterRecipeBook.LOGGER.warn("[BRBE] Couldn't migrate {}: {}", from.getFileName(), e.toString());
        }
    }
}

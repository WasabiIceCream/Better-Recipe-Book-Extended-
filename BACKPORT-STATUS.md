# Gameoverse Fabric 26.1.2 Backport — Status

This is a fork of upstream `Avalonia-0/Better-Recipe-Book-Extended-` (see `README.md`
for the original project), retargeted to Fabric 26.1.2 on branch `backport-26.1.2`.
Relocated here from an ephemeral job tmp directory on 2026-09-08 so the source
survives — previously it only existed as a checked-out clone outside this repo, with
the built jar deployed but no durable source location.

## Why this exists

Gameoverse's Modrinth-available "Better Recipe Book" build for 26.1.2 was stale.
Rather than cherry-pick commits onto that stale branch, this backport takes upstream's
26.2 branch tip (before its JEI-companion mod split, which was deliberately dropped —
see `mod_version` in `gradle.properties`) and reverses the known 26.1.2→26.2 API
renames (`docs/26.2-api-changes.md`, `docs/26.2-migration-plan-v2.md` in this repo),
rather than porting 26.1.2 forward.

## Changes from upstream

- `gradle.properties`: retargeted to `minecraft_version=26.1.2`,
  `fabric_loader_version=0.19.5`, `fabric_api_version=0.155.3+26.1.2`,
  `cloth_config_version=26.1.154`, `mod_version=2.3-beta.3-backport26.1.2`.
- `fabric.mod.json`: `depends.minecraft` lowered from `>=26.2` to `>=26.1.2`.
- Reversed the 26.2 API renames across `src/main/java/**` (`.gui.setScreen()` →
  `.setScreen()`, `.gui.overlay()` → `.getOverlay()`, `.gui.screen()` → `.screen`).
- **Crash fix (2026-09-08)**: `RecipeBookIsPain.withCreativeTabs()` called
  `tab.getIconItem()` unconditionally on every registered creative-mode tab, with no
  guard for a tab whose icon `Supplier` was never set (observed with a
  `resourcefullib`-based `ResourcefulCreativeModeTab` from another installed mod) —
  threw an NPE that crashed the client's inventory screen. Added `safeIconItem(tab)`,
  catching the failure and skipping just that tab instead of crashing. See client
  crash report `crash-2026-09-08_02.40.21-client.txt` (in the PrismLauncher instance's
  `crash-reports/`, not this repo) for the original trace.
- **Lag-spike fix (2026-09-08)**: `RecipeViewerIndex.rebuildEngine()` did a full
  O(known recipes) rescan every time `ClientRecipeBook.rebuildCollections()` returned —
  once per `ClientboundRecipeBookAddPacket`, not once per batch. Several recipes
  unlocking in quick succession (several packets close together) meant several full
  rescans back to back, visible as a stutter. Added a `dirty` flag on
  `RecipeViewerIndex` (`markDirty()`/`tick()`) and moved the actual rebuild to a
  `ClientTickEvents.END_CLIENT_TICK` hook in `BetterRecipeBookClientFabric`, coalescing
  a burst of packets into a single rebuild per tick. Also removed two leftover
  per-rebuild `LOGGER.info` debug lines (`[BRBE-CACHE] rebuild RETURN`,
  `[BRBE] rebuildEngine: …`) that fired on every cycle.
- **Stale recipe-list fix (2026-09-08)**: the partial-craftability gate in
  `mixins/incompletecrafting/RecipeBookComponentMixin#brbe$keepPartiallyCraftable`
  skipped its whole classification pass whenever an inventory-contents hash was
  unchanged — with no separate check for the known-recipe set itself changing. A
  newly-unlocked recipe with the inventory untouched would sit unclassified (not
  marked partial/craftable) until something unrelated (e.g. picking an item up and
  placing it back, which touches the hash via `carried`) forced a full pass — matching
  a user report of the recipe list "not updating until I touch my inventory." Added
  `RecipeBookState.consumeKnownSetChanged()` (set in `beginCycle()`, i.e. every
  `rebuildCollections()` cycle) as a third trigger alongside the existing
  inventory-hash and config-change checks. Found via a dedicated fork review of the
  caching/invalidation code, not the user's own bug report alone — see that review's
  findings for one more flagged-but-unconfirmed spot (`IncompatibleCraftingUtil`'s own
  independent tagger, not inspected).

## Deployment

Built jar deploys to
`fabric 26.1/automodpack/host-modpack/main/mods/brbe-ava-fabric-26.1.2-2.3-beta.3-backport26.1.2.jar`
(client-only mod, synced via AutoModpack). Its client-owned config
(`config/zzzbrbe.toml`) is force-synced to all clients via `allowEditsInFiles` in
`automodpack/automodpack-server.json` — see that file's comment/entry and
`AGENTS.md`'s AutoModpack section for how that mechanism works.

## Status

Live-verified working by the user in-game 2026-09-08 after the icon-supplier crash
fix, including a follow-up settings sync from client to server. `effortless-crafting`
was removed from the client mod set the same night as a suspected conflict — not
confirmed as an actual BRBE incompatibility, just a precaution.

Two more real bugs (lag spike, stale recipe list — both above) found and fixed the
same night, the second via a dedicated code-review pass rather than a live report
alone. Built and deployed; not yet re-verified in-game.

**Build variant, discovered the hard way (2026-09-14)**: this project has two jar
variants (`./gradlew build` vs `./gradlew jeiJar` — see `CLAUDE.md`'s "HudHider
API" section and `build.gradle`'s comment above the `jeiJar` task). The default
`build`/`jar` task bundles a vendored fork of JEI's own API classes for use
*without* real JEI installed; `jeiJar` excludes those and depends on the real JEI
mod. **Gameoverse always needs the `jeiJar` variant now that the real JEI mod is
installed** — the default variant was deployed by mistake for a while (harmless
until JEI was actually added to the server, at which point it silently broke
`hideReiJeiOverlay`: BRBE's reflection-based JEI-overlay hider resolved its own
vendored `mezz.jei.common.Internal`/`IClientToggleState` classes instead of real
JEI's, so toggling the overlay state did nothing, with no visible error). Also
had to retarget `src/jei/resources/fabric.mod.json` to `minecraft: >=26.1.2` —
it was still `>=26.2`, never updated when the rest of the project was backported.
Full incident writeup: `docs/current-state.md` in the parent server repo, dated
2026-09-14. **Always build with `./gradlew jeiJar` for this server, not
`./gradlew build`.**

**Second bug found the same night, same underlying cause (stale mixin
target)**: `mixins/hideoverlay/IngredientListOverlayMixin.java` and
`BookmarkOverlayMixin.java` both targeted a `drawScreen()` method that JEI
split into `drawBackground()`/`drawForeground()` at some point -- both mixins
are `required: false`/`defaultRequire: 0` so they were silently no-opping with
zero log output. This is why the JEI config gear button, bookmark-toggle
button, and lookup-history-toggle button all stayed visible even after the
overlay itself was correctly hidden (those three are drawn unconditionally in
`drawForeground()`, gated only by `hasValidScreen()`, never by the toggle
state). Fixed by retargeting both mixins to `{"drawBackground", "drawForeground"}`.
Also deleted two disabled, unused duplicate draft mixins under
`fabric/mixin/` (`JeiIngredientListOverlayMixin.java.disabled`,
`JeiBookmarkOverlayMixin.java.disabled`) that were never referenced by any
mixin config -- dead weight from an earlier attempt at the same fix.
**Confirmed working live by the user 2026-09-14.**

# Gameoverse Fabric 26.1.2 Backport — Status

> **Current: branch `backport-26.1.2-v2`, 2.3.1-backport26.1.2 (2026-09-26).** The
> "Rebase onto 2.3.1" section at the end describes it. Everything above that
> section is the history of the first backport (`backport-26.1.2`, 2.3-beta.3),
> kept for reference; its build-variant notes no longer apply (upstream dropped
> the `jeiJar` variant).

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

**Possible related but distinct staleness case, not yet root-caused
(2026-09-23)**: user reported a recipe with two valid recipes for the same
result (`minecraft:flint_and_steel` - vanilla's own Iron Sheet version and
a mod-added Steel version, both real, both live server-side) showed only
one recipe in the book with no alternate-recipe cycle arrow, i.e. the
grouping never picked up the second variant at all. Confirmed the recipe
itself worked fine when crafted by hand and that the server-side config
was correct (Reliable Recipes had genuinely applied both), so this wasn't
a server bug. A full client quit-and-relaunch didn't fix it either. It
self-resolved on its own a short time later with no config/server change
in between. Never root-caused - could be the same underlying staleness
class as the already-fixed "stale recipe-list" bug above (a known-set-changed
trigger not covering "a new alternate recipe became available for an
already-known result," as opposed to "a wholly new recipe ID became
known"), but not confirmed. Worth a real look if it recurs or becomes
reproducible on demand - the `RecipeCollectionTagger`/`PartialCraftingUtil`
caching path is the first place to check, same area as the original fix.

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

## Incremental rebuild fix (2026-09-24)

User report: "My client still hangs for a few seconds when I unlock a bunch
of recipes at once." A follow-up to the already-fixed lag spike above, not
a regression of it -- the 2026-09-08 fix coalesces a burst of
`ClientboundRecipeBookAddPacket`s into one `rebuildEngine()` call per client
tick, but never addressed what that one call actually costs.
`RecipeViewerIndex.rebuildEngine()` rescanned **every entry in
`ClientRecipeBook.known`** on every call -- not just the newly unlocked
ones -- resolving each entry's input/output item stacks and doing a
workstation lookup per entry, synchronously on the render thread. In a
modpack with thousands of known recipes, that per-call cost alone is
enough to visibly hang when a burst of new unlocks lands in one tick (which
is exactly what happened tonight: several `gameoverse_forge_gate`/
`gameoverse_more_buckets_recipe_unlocks`/`gameoverse_early_game_shulker_recipe_unlock`
recipe-unlock advancements firing at once for a player already holding the
matching materials, right after a `/reload`).

Fix: `rebuildEngine()` now tracks which `RecipeDisplayId`s are already
folded into the index (`indexedIds`) and, in the common case (the known set
only grew, which is how recipe unlocking normally works), processes only
the entries not already in that set -- appending them via a new
`RecipeViewerEngine.addRecipe(uid, recipe, stations)` that adds to an
existing type's data instead of replacing it (unlike the pre-existing
`registerType`, which always starts from a fresh, empty `RecipeTypeData`).
Falls back to the original full O(known recipes) rescan on the very first
build, or if the known set ever *shrinks* (a recipe actually removed --
e.g. by Reliable Recipes, or a recipe-book reset) since that can't be
expressed as pure appends and needs a from-scratch reindex to stay
correct.

Built via the `jeiJar` task (this modpack has real JEI installed, so the
plain `build` task's vendored-JEI-API variant is wrong here -- see the
"Build variant" note above). Deployed directly into the user's own
PrismLauncher instance (`Gameoverse (Working)`) and to
`automodpack/host-modpack/main/mods/` for future AutoModpack distribution.
**Confirmed working live by the user 2026-09-24** -- no more hang on a
big recipe-unlock burst.

## Rebase onto 2.3.1 (2026-09-26, branch `backport-26.1.2-v2`)

Rebuilt the backport on upstream `origin/26.2` at 2.3.1 (167 commits past our old
base, ~530 files changed; ghost-item hover preview, cycle lock, pipeline cache
fixes, several crash fixes). Upstream's own `26.1.2` branch wasn't usable as a base:
it's the older Architectury multi-loader lineage.

**Retargeting**
- 26.1.2 versions in `gradle.properties`; `fabric.mod.json` depends on
  `minecraft >=26.1.2` and on `jei` (this build requires real JEI).
- Upstream embeds a headless JEI jar (`zheadlessjei`, 26.2, with its own copies of
  `mezz.jei.*`). Dropped: it declares `minecraft >=26.2` and would duplicate real
  JEI's classes. We compile against real JEI 29.43.0.105 (`libs/`).
- BRBE's own `com.alonie.brbe.jei.*` plugin classes lived inside that jar;
  brought back into `src/` from upstream's `headless-jei` branch (26.2 tree) and
  adapted to the JEI 29 API (extra interface overloads, key-mapping types). The
  headless core only boots without real JEI, so it returns early here.
- 26.2 API renames reversed (`gui.screen()` -> `screen`, `gui.setScreen` ->
  `setScreen`, `gui.overlay()` -> `getOverlay()`, `gui.toastManager()` ->
  `getToastManager()`, `ItemStackTemplate.fromStack` -> `fromNonEmptyStack`).
- `PauseScreenConfigButtonMixin` removed: it redirects a call in 26.2's pause-menu
  icon row, which 26.1.2 doesn't have (crashed at startup). Config stays in Mod Menu.

**Our fixes, carried or dropped**
- Creative-tab icon guard (`safeIconItem`): carried, now four call sites; warns
  once per tab.
- Incremental recipe-viewer rebuild: carried onto upstream's coalesced rebuild,
  plus a full rebuild on level change (display ids are per world).
- Stale recipe-list fix: dropped, upstream's `hasUncheckedCollections` fixes it.
- Hide JEI/REI overlay: upstream removed the feature (1d24bb90); reverted that
  removal (their last working version, `isListDisplayed()` hooks). Its old
  `AbstractContainerScreenMixin` stays only for its R/U forwarding to JEI (hiding
  the overlay switches JEI's own R/U off), and only when LEI is off; its A-key
  swallow is gone (A is upstream's pin key).

**Found in testing and fixed**
- JEI runtime pass (`indexVanillaRuntimeTypes`, new upstream): its text
  fingerprints exhausted an 8 GB client heap. Now 128-bit hashes of
  `ItemStack.hashItemAndComponents`, plus a 50,000-per-type safety cap. Measured:
  anvil 1324, grindstone 1139, brewing 484, smithing 351, stonecutting 6530.
- "Enable LEI" (`recipeViewerEnabled`) was only honoured by one old R/U path; the
  new multi-window paths, workstation tab/title clicks and window restore ignored
  it. Gameoverse ships LEI off: those now defer to real JEI's recipe screen
  (station clicks focus the CRAFTING_STATION role) and no floating window opens.
- Upstream renamed the mod id `zzzbrbe` -> `brbe` (2026-08-28): config
  `brbe.toml`, keybinding ids `key.brbe.*`, pin files `brbe.*.json`.
  `LegacyFileMigration` copies the old pin/workstation files once. The config is
  not migrated (old `[newRecipes] unlockAll` would be lost and upstream's new
  default is `true`); Gameoverse ships `brbe.toml` via AutoModpack instead.

**Checks** (`tools/`, see its README): every injection point exists in the 26.1.2
method bodies, every `@Shadow`/`@Accessor` member exists on its target class; also
verified every entrypoint/mixin class/plugin is in the jar and every reflective
JEI/BRBE lookup resolves.

**Gameoverse config shipped** (`host-modpack/main/config/brbe.toml`, forced by
`!/config/brbe.toml` in `automodpack-server.json`): LEI off, hide JEI overlay on,
`unlockAll = false`, `showAllRecipesInSurvival = true`,
`partialOnlyWhenCarrying = false`, pin key F. Default keybinds: R/U, pin A, cycle
lock X (upstream's Alt clashes with Create/Spell Engine/Heirlooms).

**Verified in-game 2026-09-26** (Working instance): no crash opening inventories
and crafting tables; JEI list plus its gear/bookmark buttons hidden; R/U and
workstation clicks open JEI's screen; stonecutter recipes indexed; hover ghost
preview; X cycle lock; memory steady after joining.

## JEI corner buttons visible again after the 2.3.1 rebase (2026-09-30)

The rebase onto upstream 2.3.1 took upstream's `hideoverlay` mixins, which target only `drawScreen` plus
`isListDisplayed`. With JEI 29.43 the list is hidden by `isListDisplayed`, but the config gear and bookmark buttons are
drawn in `drawForeground`, which the screen calls directly, so they showed again (the 2026-09-14 fix, lost). Both
mixins now cancel `drawScreen`, `drawBackground` and `drawForeground` while `hideReiJeiOverlay` is on. Mixin checks: the
same 3 pre-existing NOT FOUND results as before this change (SmithingScreenMixin, two cyclelock ModifyArgs), not
introduced here; worth a separate look.

## Mixin-check false alarms and the JEI plugin replay (2026-09-30)

**The 3 NOT FOUND mixin-check results were checker bugs.** `javap` of the 26.1.2 classes: `RecipeBookComponent.<init>`
does `new GhostSlots(SlotSelectTime)` and `new RecipeBookPage(this, SlotSelectTime, boolean)` exactly as the cycle-lock
`@ModifyArg`s expect, and `SmithingScreen.extractBackground` calls `CyclingSlotBackground.extractRenderState(...)` three
times (that target belongs to the `@Redirect` on `extractBackground`; the `@Inject` in `subInit` is `@At("RETURN")`).
`tools/check_injections.py` (a) never matched a constructor target, because javap prints `GhostSlots."<init>"`, and (b)
let an annotation on a package-private method (`void init(...)`, no modifier) run on into the next injector's target.
Both fixed; the checker now reports 0 problems (19 points). No mixin change was needed: no warnings for these in the
Working instance log, and the cycle lock (X) was verified in-game on 2026-09-26.

**JEI plugin replay failed for Bits and Balance, Create and Polymer.** BRBE re-runs every mod's JEI plugin
(`BrbeJeiPlugins.collectAndInject`, once per join after the JEI runtime exists) against its own collector objects to
index their recipes for BRBE's own recipe viewer (LEI windows, BRBE's popups/pins). The collectors returned `null` for
`getIngredientManager()`, `getVanillaRecipeFactory()` and `getContextMap()`, so:
- Bits and Balance threw at its first call (`createBrewingRecipe`): its brewing and Kiln recipes were never indexed.
- Create threw at `getAllIngredients` after `automatic_brewing`: draining, spout filling, toolbox and block cutting missing.
- Polymer threw on `synchronized (manager)` (it has no virtual items here, so nothing was missing).
Real JEI was unaffected (it loads plugins itself with its own registrations; its log shows all three registering).
With LEI off in Gameoverse, R/U and station clicks open real JEI, so players mostly saw nothing; the gap showed only in
BRBE's own viewer if LEI is switched on.

Fix: new `jei/plugins/stub/JeiRuntimeView` gives the collectors and `JeiHelpersStub` the runtime's objects: the vanilla
recipe factory (`IJeiHelpers.getVanillaRecipeFactory()`), `SlotDisplayContext.fromLevel(level)` as the context map
(what JEI passes), the other helpers delegated (the GUI helper stays the recording stub), and the ingredient manager as
a **read-only proxy**: `addIngredientsAtRuntime`/`removeIngredientsAtRuntime`/`registerIngredientListener` are dropped,
because Polymer's `registerRecipes` removes and re-adds its items and the replay must not change live JEI a second time.
All still `null` when no runtime exists. `CatalystCollector.getJeiHelpers()` now returns the stub instead of `null`.

Also logged: "broken fabric plugin container: InvocationTargetException" was Haunted Harvest, whose `jei_mod_plugin`
class (`net.mehvahdjukaar.hauntedharvest.integration.JEICompat`) is missing from its own jar; real JEI logs the same
error. Not ours to fix; the warning now names the mod and the root cause.

To check after joining: `latest.log` has no `[BRBE-JEI-Plugins] plugin ... failed`; `logs/brbe-debug.log` lists
`mod type create:draining`, `create:spout_filling`, `create:block_cutting` and `bitsandbalance:*`/`minecraft:brewing`
lines, and `collected from plugin bitsandbalance:jei_plugin`/`create:jei_plugin`/`polymer:jei_plugin`.

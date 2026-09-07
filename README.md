# Colorful Lighting Ponder Guard

Stops [Colorful Lighting](https://www.curseforge.com/minecraft/mc-mods/colorful-lighting-sodium)
from crashing the client whenever a [Ponder](https://github.com/Creators-of-Create/Ponder)
scene is opened.

Minecraft 1.20.1 / Forge. Client-side only. MIT.

## The problem

Ponder draws its scenes in a `PonderLevel`, which is not a real client level.

Colorful Lighting creates its coloured light engine in `LevelMixin.postInit`, but only when
`this instanceof CLSupportingLevel` — and the only class that implements that marker is
`ClientLevel`. So a `PonderLevel` never gets an engine. Its renderer mixins then call
`getEngine()` without checking the result:

```text
java.lang.NullPointerException: Cannot invoke
  "ColoredLightEngine.sampleLightColorInt(BlockPos)" because the return value of
  "LevelAttachments.colorfullighting$getEngine()" is null
```

The screen dies while rendering, so the whole game crashes.

There are **two** paths that hit it:

| path | upstream escape hatch |
| --- | --- |
| `LevelRenderer.getLightColor` → `WorldSectionElementImpl.buildStructureBuffer` | **exists** — `CreateCompat` returns a flat white for `PonderLevel` |
| `EntityRenderer.getPackedLightCoords` → `PonderLevel.renderEntity` | **none** |

The block-path hatch is only initialised inside `ModList.get().isLoaded("create")`. Ponder is
shipped standalone now (bundled inside other mods via jar-in-jar), so a pack that has Ponder but
no Create never reaches it. The entity path has no hatch at all, which is why the crash also
happens for people who *do* have Create — see
[Camawama/colorful-lighting-sodium#47](https://github.com/Camawama/colorful-lighting-sodium/issues/47)
("Create crashes when using ponder really often with this mod").

## What this does

Two `@Inject(at = HEAD, cancellable = true)` mixins at `priority = 500`, so they run before
Colorful Lighting's own injectors. When the level is a `PonderLevel`, they return the same flat
white Colorful Lighting itself uses for Ponder scenes and stop there.

**The value matters.** Colorful Lighting redefines what the packed light `int` means —
`PackedLightData.packData` lays it out as `r | (g << 8) | (sky << 16) | (b << 20) | (15 << 28)`.
Returning a vanilla-packed value instead would tint the scene rather than fix it (that was
upstream issue #11, "Create's ponders are tinted red"). The constant used here, `0xFE10E1E1`,
unpacks to red 225 / green 225 / sky 0 / blue 225 / alpha 15 — identical to upstream's
`packData(0, 225, 225, 225)`.

Nothing outside a Ponder scene is touched, so coloured lighting keeps working everywhere else,
entities included.

## Why not just patch Colorful Lighting's jar?

That was tried first and it works, but it costs more than it looks:

- the block path can be re-enabled by pointing the `isLoaded("create")` check at `"ponder"`, but
  `CreateCompat` also does `instanceof VirtualRenderWorld` on a Create class that is not present,
  so that branch has to be neutralised too;
- the entity path cannot be fixed by swapping constants — it needs an inserted branch, which
  means rebuilding jump targets and the `StackMapTable`;
- and every Colorful Lighting version bump means redoing and re-verifying all of it.

A separate mod targets vanilla classes only, so it survives Colorful Lighting updates and lets
the stock jar be used.

## Building

```sh
./gradlew build
```

`libs/ponder.jar` is `Ponder-Forge-1.20.1-1.0.91.jar`, extracted from
`casualswing-0.1.0-1.20.1-Forge.jar`'s `META-INF/jarjar/`. It is `compileOnly` — Ponder is not
bundled into the output.

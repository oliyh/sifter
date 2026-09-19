# Phase 3 — Give it a real look

Companion to the Phase 3 section of [`LESSON_PLAN.md`](../LESSON_PLAN.md).

This is the creative one. Phase 2 already gave the Sifter its own
`SifterModel`, built from two boxes ("bottom" and "top") instead of a
borrowed pig - but they're still two plain, undecorated cubes. Here we
reshape those same two boxes into an actual design and paint them with a
real texture.

Notice what *doesn't* change: `setupAnim()` - the undulation code you
wrote in Phase 2 - stays exactly as it is. That's the payoff of keeping
"what the model is made of" separate from "how it moves": we get to
completely redesign the shape without touching a single line of
animation math.

## You type this

### 1. The texture - a plain yellow image

`SifterModel.createBodyLayer()` already declares a 64x64 texture canvas
(`LayerDefinition.create(mesh, 64, 64)`), with the bottom box's faces
read from the top-left region and the top box's from the region below
it. The simplest texture that satisfies that layout is a single flat
colour. If you don't already have
`src/client/resources/assets/sifter/textures/entity/sifter.png`, create
one - any 64x64 image filled with a yellow colour works (an image
editor, or ask for a hand generating one).

### 2. Reshape the boxes - back in `SifterModel.createBodyLayer()`

You're editing the method you already wrote in Phase 2, not writing a
new one. Recall what the six numbers in `addBox` mean, e.g.
`.addBox(-8.0F, -16.0F, -8.0F, 16.0F, 16.0F, 16.0F)`:

- The first three (`-8, -16, -8`) are where the box's *corner* starts,
  relative to the part's own pivot point.
- The last three (`16, 16, 16`) are the box's width, height, and depth.

Try tapering the shape - a narrower top box sitting on a wider bottom
one, say:

```java
root.addOrReplaceChild("bottom",
	CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -14.0F, -8.0F, 16.0F, 14.0F, 16.0F),
	PartPose.offset(0.0F, 24.0F, 0.0F));

root.addOrReplaceChild("top",
	CubeListBuilder.create().texOffs(0, 32).addBox(-6.0F, -12.0F, -6.0F, 12.0F, 12.0F, 12.0F),
	PartPose.offset(0.0F, 10.0F, 0.0F));
```

The exact numbers are the bit meant to be played with together - resize,
rebuild, look, repeat. `setupAnim()` doesn't care about any of this: it
only ever moves `bottom` and `top` sideways by a number, whatever shape
they happen to be.

### 3. Give it a real texture - `src/client/java/com/sifter/client/SifterRenderer.java`

Swap the borrowed placeholder texture for our own, and drop the shader
tint - our texture is already yellow, so we don't need to fake it with a
tint any more.

The texture constant changes from:

```java
	private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/pig/pig.png");
```

to:

```java
	private static final ResourceLocation TEXTURE =
		ResourceLocation.fromNamespaceAndPath(Sifter.MOD_ID, "textures/entity/sifter.png");
```

(add `import com.sifter.Sifter;`)

And you can now delete the `RenderSystem.setShaderColor(...)` lines
(both of them) from `render(...)`, along with the now-unused
`com.mojang.blaze3d.systems.RenderSystem` import - the whole `render(...)`
override can go entirely if nothing else is left in it, since
`MobRenderer`'s default behaviour is now exactly what we want.

## How to check it

```bash
./gradlew build
```

Spawn a Sifter - it should be wearing your own shape and colour, no
tint trick required, and it should still undulate exactly as it did at
the end of Phase 2.

## Design together

This is the phase to sit down and actually design the Sifter's shape.
Ideas to try, one at a time, rebuilding between each:

- Adjust `bottom` and `top`'s sizes and pivots until the proportions look
  right for a two-block creature.
- Add a third part - e.g. `"legs"`, a child of `bottom` the same way
  `bottom` and `top` are children of `root` - another `addOrReplaceChild`
  call, another field, another line in the constructor. A part that's a
  *child* of `bottom` will automatically follow `bottom`'s sway without
  needing any animation code of its own - a good moment to notice how
  parent/child parts differ from the sibling relationship `bottom` and
  `top` have.
- Once there's more than one colour wanted, the texture stops being a
  flat fill and needs actual pixel art matching the UV layout
  (`texOffs(x, y)` says where on the texture image each box's faces are
  read from) - a good "next session" project on its own.

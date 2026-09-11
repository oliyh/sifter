# Phase 3 — Give it a real look

Companion to the Phase 3 section of [`LESSON_PLAN.md`](../LESSON_PLAN.md).

This is the creative one. The code below builds a Sifter out of two boxes
(matching Phase 1's original "two yellow blocks" idea) - but this time
it's *our* design, not a borrowed pig. The numbers describing size and
position are the bit meant to be played with together.

## You type this

### 1. The texture - a plain yellow image

Before the model, we need something to paint the boxes with. The
simplest possible texture is a single flat colour. If you don't already
have `src/client/resources/assets/sifter/textures/entity/sifter.png`,
create one - any 32x32 image filled with a yellow colour works (an image
editor, or ask for a hand generating one).

### 2. The model - `src/client/java/com/sifter/client/SifterModel.java`

```java
package com.sifter.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import com.sifter.entity.SifterEntity;

// Our own design, built from two boxes wired together into a little
// skeleton: the body, and the head sitting on top of it.
//
// Want to change how the Sifter looks? The numbers in createBodyLayer()
// below are the only place you need to touch.
public class SifterModel extends HierarchicalModel<SifterEntity> {

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart head;

	public SifterModel(ModelPart root) {
		this.root = root;
		this.body = root.getChild("body");
		this.head = body.getChild("head");
	}

	// Describes the shape once, when the game starts: every box's size,
	// and where it sits relative to its parent part. The head is a child
	// of the body, so it automatically follows the body around.
	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create()
				.texOffs(0, 0)
				.addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(0.0F, 16.0F, 0.0F));

		body.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(0, 16)
				.addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F),
			PartPose.offset(0.0F, -8.0F, 0.0F));

		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(SifterEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
			float netHeadYaw, float headPitch) {
		// Turns "degrees the head should turn" into "radians", which is
		// what the model math underneath actually uses.
		head.yRot = netHeadYaw * ((float) Math.PI / 180F);
		head.xRot = headPitch * ((float) Math.PI / 180F);
	}
}
```

**What the six numbers in `addBox` mean**, e.g.
`.addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)`:

- The first three (`-4, -8, -4`) are where the box's *corner* starts,
  relative to the part's origin.
- The last three (`8, 8, 8`) are the box's width, height, and depth.
- To make the body wider, change the 4th number. To move the head up,
  make the head's `PartPose.offset(0.0F, -8.0F, 0.0F)` more negative
  (Y grows downward in these coordinates).

### 3. Point the renderer at it - `src/client/java/com/sifter/client/SifterRenderer.java`

Replace the `PigModel` imports and usage. The class declaration changes
from:

```java
public class SifterRenderer extends MobRenderer<SifterEntity, PigModel<SifterEntity>> {
```

to:

```java
public class SifterRenderer extends MobRenderer<SifterEntity, SifterModel> {
```

The texture constant changes from the borrowed pig texture to our own:

```java
	private static final ResourceLocation TEXTURE =
		ResourceLocation.fromNamespaceAndPath(Sifter.MOD_ID, "textures/entity/sifter.png");
```

(add `import com.sifter.Sifter;`)

And the constructor now bakes our own layer instead of the pig's:

```java
	public SifterRenderer(EntityRendererProvider.Context context) {
		super(context, new SifterModel(context.bakeLayer(SifterClient.SIFTER_LAYER)), 0.5F);
	}
```

You can also delete the `PigModel`/`ModelLayers` imports and the shader
tint lines in `render(...)` (`RenderSystem.setShaderColor(...)` and its
reset) - our texture is already yellow, so we don't need to fake it with
a tint any more.

### 4. Register the model layer - `src/client/java/com/sifter/client/SifterClient.java`

Add a constant identifying our shape, and register it before the
renderer:

```java
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

import com.sifter.Sifter;
```

```java
	public static final ModelLayerLocation SIFTER_LAYER =
		new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Sifter.MOD_ID, "sifter"), "main");

	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(SIFTER_LAYER, SifterModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.SIFTER, SifterRenderer::new);
	}
```

## How to check it

```bash
./gradlew build
```

Spawn a Sifter - it should now be built from your own two boxes, not a
pig shape.

## Design together

This is the phase to sit down and actually design the Sifter's shape.
Ideas to try, one at a time, rebuilding between each:

- Change the body/head sizes so it looks less blocky.
- Add a third part (e.g. `"legs"`, a child of `body`) the same way `head`
  was added - another `addOrReplaceChild` call, another field, another
  line in the constructor.
- Once there's more than one colour wanted, the texture stops being a
  flat fill and needs actual pixel art matching the UV layout
  (`texOffs(x, y)` says where on the texture image each box's faces are
  read from) - a good "next session" project on its own.

# Phase 2 — The wavy dance

Companion to the Phase 2 section of [`LESSON_PLAN.md`](../LESSON_PLAN.md).

## A change of plan

The first draft of this phase swayed the *whole* Sifter side to side, as
one rigid piece, by rotating it in the renderer. That's a cheap trick,
but it's not what we actually want: the Sifter should still walk in a
straight line like everything else, it's the *body* that should undulate
- and since the Sifter is two blocks stacked, the top block and the
bottom block should sway independently, slightly out of step with each
other, like a wave travelling up its body.

That means the top and bottom need to be two separate pieces we can move
on their own - which means we need a tiny version of the custom model
Phase 3 was going to build, earlier than planned. The upside: once each
block is its own piece, Minecraft calls our animation code automatically,
every frame, so the awkward manual `PoseStack` juggling from the first
draft disappears entirely. Phase 3 will reshape these two crude boxes
into the Sifter's real design later, but the skeleton - two parts, one
on top of the other - won't need to change, and neither will the maths
below.

## You type this

### 1. The pure maths - `src/main/java/com/sifter/entity/SifterAnimation.java`

Notice this file imports nothing from Minecraft at all - it's just
`Math.sin`. That's deliberate: it means we can test it (next file)
without starting the game.

```java
package com.sifter.entity;

// The wavy-dance maths, pulled out on its own so we can test it without
// starting Minecraft. It doesn't know anything about entities, renderers,
// or the game - give it a moment in time, get a wiggly number back.
public class SifterAnimation {

	private SifterAnimation() {
	}

	// Math.sin() turns a number into a smooth wave between -1 and 1, that
	// repeats forever. We control how fast it repeats (period, in ticks),
	// how big the wiggle is (amplitude), and where in the cycle it starts
	// (phase, also in ticks). Two calls with the same period and amplitude
	// but different phase trace out the *same* wave, just at different
	// points along it - which is exactly how we'll get the top and bottom
	// blocks to move out of step with each other, using this one method.
	public static float wobbleOffset(float ticksAlive, float period, float amplitude, float phase) {
		return (float) (Math.sin((ticksAlive + phase) / period * Math.PI * 2.0) * amplitude);
	}
}
```

### 2. The test - `src/test/java/com/sifter/entity/SifterAnimationTest.java`

```java
package com.sifter.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SifterAnimationTest {

	@Test
	void startsAtZeroWithNoPhase() {
		assertEquals(0.0f, SifterAnimation.wobbleOffset(0f, 20f, 1f, 0f), 0.0001f);
	}

	@Test
	void neverExceedsTheAmplitude() {
		float amplitude = 1f;
		for (float ticks = 0f; ticks < 1000f; ticks += 0.5f) {
			float offset = SifterAnimation.wobbleOffset(ticks, 20f, amplitude, 0f);
			assertTrue(offset >= -amplitude && offset <= amplitude,
				"offset " + offset + " at ticks=" + ticks + " went outside the amplitude");
		}
	}

	@Test
	void repeatsAfterOnePeriod() {
		float period = 20f;
		assertEquals(
			SifterAnimation.wobbleOffset(5f, period, 1f, 0f),
			SifterAnimation.wobbleOffset(5f + period, period, 1f, 0f),
			0.0001f
		);
	}

	@Test
	void aPhaseIsJustAHeadStartAlongTheSameWave() {
		// Being 5 ticks into the wave with no phase should look identical
		// to being at tick 0 with a phase of 5 - that's the whole idea
		// behind giving the top and bottom blocks different phases below.
		assertEquals(
			SifterAnimation.wobbleOffset(5f, 20f, 1f, 0f),
			SifterAnimation.wobbleOffset(0f, 20f, 1f, 5f),
			0.0001f
		);
	}
}
```

Run `./gradlew test` - green, before we've touched the model or renderer
at all. That's the point of testing pure logic: it works the same whether
or not the game is even installed.

### 3. Give the Sifter two independent parts - `src/client/java/com/sifter/client/SifterModel.java`

This is a new file, and a new idea: a **model part**. Up to now the
Sifter has borrowed the vanilla pig's shape - one pre-built model we
can't take apart. Here we build our own model, out of two boxes we
define ourselves, each one a `ModelPart` we can move independently. It's
still deliberately crude (two plain 16x16x16 boxes, no real design) -
Phase 3 will make it look good. What matters for this phase is that
"top" and "bottom" are separate objects.

```java
package com.sifter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

import com.sifter.Sifter;
import com.sifter.entity.SifterAnimation;
import com.sifter.entity.SifterEntity;

// Two stacked boxes, each its own ModelPart so they can move on their
// own. Phase 3 reshapes these into the Sifter's real design - the two
// parts, and the animation below, won't need to change when it does.
public class SifterModel extends EntityModel<SifterEntity> {

	// Every custom model needs a unique name to register itself under -
	// this is that name.
	public static final ModelLayerLocation LAYER =
		new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Sifter.MOD_ID, "sifter"), "main");

	private final ModelPart bottom;
	private final ModelPart top;

	public SifterModel(ModelPart root) {
		this.bottom = root.getChild("bottom");
		this.top = root.getChild("top");
	}

	// Describes the shape once, up front: two boxes, 16 units per side
	// (one Minecraft block each), stacked directly on top of each other.
	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("bottom",
			CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 16.0F, 16.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F));

		root.addOrReplaceChild("top",
			CubeListBuilder.create().texOffs(0, 32).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 16.0F, 16.0F),
			PartPose.offset(0.0F, 8.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}

	// Minecraft calls this itself, every frame, before drawing - we don't
	// have to hook into tick() or the renderer to make this run. ageInTicks
	// is "how far through time are we," the same idea as tickCount +
	// partialTicks from the first draft, just handed to us directly.
	//
	// Both blocks share a period and amplitude - only the phase differs -
	// so the top always follows the same motion as the bottom, just a
	// short delay behind it. That delay is what makes it read as one wave
	// travelling up the body, instead of two blocks wobbling at random.
	@Override
	public void setupAnim(SifterEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
			float netHeadYaw, float headPitch) {
		float period = 20.0F;
		float amplitude = 1.5F;
		float phaseGap = period / 4.0F;

		bottom.x = SifterAnimation.wobbleOffset(ageInTicks, period, amplitude, 0.0F);
		top.x = SifterAnimation.wobbleOffset(ageInTicks, period, amplitude, phaseGap);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
			int color) {
		bottom.render(poseStack, buffer, packedLight, packedOverlay, color);
		top.render(poseStack, buffer, packedLight, packedOverlay, color);
	}
}
```

`ModelPart.x` is a sideways offset in the entity's own frame of
reference - "left/right relative to which way I'm facing," not
"left/right on the world map." That's exactly why this makes the Sifter
undulate on the spot while it still walks in a straight line: its actual
position and heading are untouched, only the two boxes shift sideways
relative to that heading.

### 4. Register the new model - `src/client/java/com/sifter/client/SifterClient.java`

Every custom model has to be registered with a "how do I build this
shape" recipe before anything can bake and use it. Add this line (and
its import):

```java
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
```

```java
		EntityModelLayerRegistry.registerModelLayer(SifterModel.LAYER, SifterModel::createBodyLayer);
```

Put it before the existing `EntityRendererRegistry.register(...)` line -
the renderer we're about to update needs the layer to already be
registered when it bakes it.

### 5. Point the renderer at the new model - `src/client/java/com/sifter/client/SifterRenderer.java`

Swap the borrowed `PigModel` for our own `SifterModel`. Nothing else
about rendering changes - notice there's no `PoseStack` animation code
here any more. That's now `SifterModel`'s job, not the renderer's.

```java
package com.sifter.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import com.sifter.entity.SifterEntity;

// Still a placeholder texture (real art is Phase 3), but the shape is
// now our own SifterModel, not a borrowed pig - that's what let us give
// the top and bottom blocks independent motion above.
public class SifterRenderer extends MobRenderer<SifterEntity, SifterModel> {

	private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/pig/pig.png");

	public SifterRenderer(EntityRendererProvider.Context context) {
		super(context, new SifterModel(context.bakeLayer(SifterModel.LAYER)), 0.5F);
	}

	@Override
	public ResourceLocation getTextureLocation(SifterEntity entity) {
		return TEXTURE;
	}

	@Override
	public void render(SifterEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
			MultiBufferSource buffer, int packedLight) {
		// Multiply every pixel we draw by this colour before drawing it.
		// (1, 1, 1) would mean "no change"; we push red and green down and
		// leave blue at 0, which turns things yellow.
		RenderSystem.setShaderColor(1.0F, 0.85F, 0.0F, 1.0F);
		super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
```

## How to check it

```bash
./gradlew build
```

Spawn a Sifter and watch it walk - it should move in a straight line
(same as any other mob), while its body undulates: the bottom block
sways one way, the top block follows a beat behind, so the whole thing
looks like a wave travelling up its body as it walks. It'll also
noticeably be two separate boxes now, rather than a pig shape - that's
expected, real art is Phase 3.

## Try tweaking it

- Change `period` (20.0F) to `40.0F` - the dance should slow down.
- Change `phaseGap` from a quarter of the period to a half
  (`period / 2.0F`) - the top and bottom should end up moving in
  opposite directions at the same time, more of a scissor motion than a
  travelling wave. Set it to `0.0F` and they move in perfect lockstep -
  that's how you can tell the phase is doing the work.
- Change `amplitude` (1.5F) to something bigger - a wilder sway. `0.0F`
  freezes that block in place, useful for isolating what the other one
  is doing.
- Each change: save, `./gradlew build`, look in game. That loop - change,
  build, look - is the whole rhythm of this project.

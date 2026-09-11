# Phase 2 — The wavy dance

Companion to the Phase 2 section of [`LESSON_PLAN.md`](../LESSON_PLAN.md).

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
	// repeats forever. We control how fast it repeats (period, in ticks)
	// and how big the wiggle is (amplitude).
	public static float wobbleOffset(float ticksAlive, float period, float amplitude) {
		return (float) (Math.sin(ticksAlive / period * Math.PI * 2.0) * amplitude);
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
	void startsAtZero() {
		assertEquals(0.0f, SifterAnimation.wobbleOffset(0f, 20f, 1f), 0.0001f);
	}

	@Test
	void neverExceedsTheAmplitude() {
		float amplitude = 1f;
		for (float ticks = 0f; ticks < 1000f; ticks += 0.5f) {
			float offset = SifterAnimation.wobbleOffset(ticks, 20f, amplitude);
			assertTrue(offset >= -amplitude && offset <= amplitude,
				"offset " + offset + " at ticks=" + ticks + " went outside the amplitude");
		}
	}

	@Test
	void repeatsAfterOnePeriod() {
		float period = 20f;
		assertEquals(
			SifterAnimation.wobbleOffset(5f, period, 1f),
			SifterAnimation.wobbleOffset(5f + period, period, 1f),
			0.0001f
		);
	}
}
```

Run `./gradlew test` - green, before we've touched the renderer at all.
That's the point of testing pure logic: it works the same whether or not
the game is even installed.

### 3. Wire it into the renderer - `src/client/java/com/sifter/client/SifterRenderer.java`

Add an import:

```java
import com.mojang.math.Axis;

import com.sifter.entity.SifterAnimation;
```

Then replace the `render(...)` method with this version, which wraps the
existing tint code in a pushed/popped pose transform:

```java
	@Override
	public void render(SifterEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
			MultiBufferSource buffer, int packedLight) {
		// tickCount + partialTicks is "how far through time are we,
		// including the fraction of a tick between game updates" -
		// smoother than tickCount alone. The actual wave maths lives in
		// SifterAnimation, where we just tested it.
		float ticksAlive = entity.tickCount + partialTicks;
		float bob = SifterAnimation.wobbleOffset(ticksAlive, 20.0F, 0.1F);
		float sway = SifterAnimation.wobbleOffset(ticksAlive, 20.0F, 12.0F);

		poseStack.pushPose();
		poseStack.translate(0.0, bob, 0.0);
		poseStack.mulPose(Axis.YP.rotationDegrees(sway));

		RenderSystem.setShaderColor(1.0F, 0.85F, 0.0F, 1.0F);
		super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		poseStack.popPose();
	}
```

## How to check it

```bash
./gradlew build
```

Spawn a Sifter and watch it - it should bob up and down and sway side to
side as it exists, distinct from a normal pig's animation.

## Try tweaking it

- Change `20.0F` (the period) to `40.0F` - the dance should slow down.
- Change `12.0F` (the sway amount, in degrees) to something bigger - a
  wilder dance. Try `0.0F` to isolate just the bob, or set the bob
  amplitude to `0.0F` to isolate just the sway.
- Each change: save, `./gradlew build`, look in game. That loop - change,
  build, look - is the whole rhythm of this project.

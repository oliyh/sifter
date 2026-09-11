# Phase 4 — Mining skulk

Companion to the Phase 4 section of [`LESSON_PLAN.md`](../LESSON_PLAN.md).

## You type this

### 1. The pure search logic - `src/main/java/com/sifter/entity/SkulkFinder.java`

```java
package com.sifter.entity;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Optional;

// Pure logic, pulled out of the goal that actually uses it: given a
// starting point and a list of candidate positions, which one (if any) is
// the closest within range? No world, no blocks, no entities - just
// coordinates - so we can test it directly.
public class SkulkFinder {

	private SkulkFinder() {
	}

	public static Optional<BlockPos> nearest(BlockPos origin, List<BlockPos> candidates, int range) {
		BlockPos best = null;
		double bestDistanceSquared = Double.MAX_VALUE;
		double rangeSquared = (double) range * range;

		for (BlockPos candidate : candidates) {
			double distanceSquared = origin.distSqr(candidate);
			if (distanceSquared > rangeSquared) {
				continue;
			}
			if (distanceSquared < bestDistanceSquared) {
				bestDistanceSquared = distanceSquared;
				best = candidate;
			}
		}

		return Optional.ofNullable(best);
	}
}
```

`BlockPos` (a block coordinate) is safe to use in a test - it's just
three numbers. Some Minecraft classes are *not* safe to use outside a
running game (see the note at the bottom of this file) - part of the
skill here is learning to tell which is which.

### 2. The test - `src/test/java/com/sifter/entity/SkulkFinderTest.java`

```java
package com.sifter.entity;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkulkFinderTest {

	@Test
	void picksTheClosestCandidate() {
		BlockPos origin = new BlockPos(0, 0, 0);
		BlockPos near = new BlockPos(2, 0, 0);
		BlockPos far = new BlockPos(5, 0, 0);

		Optional<BlockPos> result = SkulkFinder.nearest(origin, List.of(far, near), 10);

		assertEquals(Optional.of(near), result);
	}

	@Test
	void ignoresCandidatesOutsideRange() {
		BlockPos origin = new BlockPos(0, 0, 0);
		BlockPos tooFar = new BlockPos(100, 0, 0);

		Optional<BlockPos> result = SkulkFinder.nearest(origin, List.of(tooFar), 10);

		assertTrue(result.isEmpty());
	}

	@Test
	void handlesAnEmptyListWithoutCrashing() {
		BlockPos origin = new BlockPos(0, 0, 0);

		Optional<BlockPos> result = SkulkFinder.nearest(origin, List.of(), 10);

		assertTrue(result.isEmpty());
	}
}
```

### 3. The real goal - `src/main/java/com/sifter/entity/MineSkulkGoal.java`

`MoveToBlockGoal` is a goal Minecraft already provides for "wander over
to a matching block, then do something once you're there" (foxes and
turtles both use it). We only decide which blocks count, and what
happens on arrival.

```java
package com.sifter.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;

// This class talks directly to the live world (level, blockPos), so
// unlike SkulkFinder it isn't something we unit test - we check it by
// playing.
public class MineSkulkGoal extends MoveToBlockGoal {

	// Ticks (1/20th of a second each) spent standing next to the block
	// before it breaks - a slower dig than a player with a pickaxe.
	private static final int MINE_TICKS = 60;

	private int mining;

	public MineSkulkGoal(SifterEntity sifter, double speedModifier, int searchRange) {
		super(sifter, speedModifier, searchRange);
	}

	@Override
	protected boolean isValidTarget(LevelReader level, BlockPos pos) {
		return level.getBlockState(pos).is(Blocks.SCULK);
	}

	@Override
	public void tick() {
		super.tick();

		if (!isReachedTarget()) {
			mining = 0;
			return;
		}

		mining++;
		if (mining >= MINE_TICKS) {
			mob.level().removeBlock(blockPos, false);
			mining = 0;
		}
	}
}
```

### 4. Add it to the Sifter's goal list - `src/main/java/com/sifter/entity/SifterEntity.java`

In `registerGoals()`, add the new goal (priority `1`, ahead of just
looking around):

```java
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new MineSkulkGoal(this, 1.0, 8));
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
```

(the old goal 1 and 2 just shift down to 2 and 3)

## How to check it

```bash
./gradlew test    # SkulkFinderTest should be green
./gradlew build
```

Place some skulk blocks (creative mode: `/give @s sculk`) near a Sifter
and watch it walk over and mine one after about 3 seconds standing next
to it.

## The pure-vs-impure split, made concrete

`SkulkFinder.nearest(...)` never touches a `Level`, a `Block`, or
anything from the live game - just `BlockPos`, which is safe anywhere.
`MineSkulkGoal` is the opposite: almost everything it does needs a real
running world. If you're ever unsure which category something falls
into, a quick way to check: try referencing it from a test. If Gradle
prints `BUILD SUCCESSFUL`, it was safe. If it throws an error the moment
the test class loads, that class needs the game running first - keep it
out of tests, same as `MineSkulkGoal` here.

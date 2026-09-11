# Phase 5 — Sifter homes

Companion to the Phase 5 section of [`LESSON_PLAN.md`](../LESSON_PLAN.md).

**Goal:** the first time a Sifter gets the chance, it builds itself a
tiny home: a blue bed, a crafting table at the foot of the bed, and a
furnace with a potted spruce sapling on top.

Same pattern as Phase 4: the *shape* of the home is pure data we can
test, and the code that actually places blocks in the world is a
separate, untested-by-JUnit goal.

## You type this

### 1. The shape, as pure coordinates - `src/main/java/com/sifter/entity/HomeLayout.java`

```java
package com.sifter.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;

// The shape of a Sifter's home, as plain coordinates relative to one
// corner (the foot of the bed) - no blocks, no world, just geometry.
// That's what lets us test the *layout* without starting the game, even
// though building it for real (BuildHomeGoal) very much needs the game.
public class HomeLayout {

	private HomeLayout() {
	}

	public static final BlockPos BED_FOOT = BlockPos.ZERO;
	public static final BlockPos BED_HEAD = BED_FOOT.relative(Direction.SOUTH);
	public static final BlockPos CRAFTING_TABLE = BED_FOOT.relative(Direction.NORTH);
	public static final BlockPos FURNACE = CRAFTING_TABLE.relative(Direction.NORTH);
	public static final BlockPos POTTED_SAPLING = FURNACE.above();

	public static List<BlockPos> allOffsets() {
		return List.of(BED_FOOT, BED_HEAD, CRAFTING_TABLE, FURNACE, POTTED_SAPLING);
	}
}
```

This lays everything out in a line: crafting table, then the bed's foot,
then the bed's head, running north-to-south - with the furnace on the
opposite side of the crafting table from the bed, and the potted sapling
sitting on top of the furnace.

### 2. The test - `src/test/java/com/sifter/entity/HomeLayoutTest.java`

```java
package com.sifter.entity;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeLayoutTest {

	@Test
	void theTwoBedPartsAreNextToEachOther() {
		assertEquals(1, HomeLayout.BED_FOOT.distManhattan(HomeLayout.BED_HEAD));
	}

	@Test
	void everyPartHasItsOwnSpot() {
		// If two parts ever shared a position, this set would be smaller
		// than the list - a bug a human eye could easily miss.
		assertEquals(HomeLayout.allOffsets().size(), Set.copyOf(HomeLayout.allOffsets()).size());
	}

	@Test
	void theSaplingSitsDirectlyAboveTheFurnace() {
		assertEquals(HomeLayout.FURNACE.above(), HomeLayout.POTTED_SAPLING);
	}
}
```

### 3. The goal that actually builds it - `src/main/java/com/sifter/entity/BuildHomeGoal.java`

```java
package com.sifter.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;

// Builds the Sifter's home, once, the moment this goal first gets a
// chance to run. Turns the plain coordinates from HomeLayout into real
// blocks - this is the "impure" half that touches the live world, so
// unlike HomeLayout itself, we check this one by playing, not by testing.
public class BuildHomeGoal extends Goal {

	private final SifterEntity sifter;
	private boolean built;

	public BuildHomeGoal(SifterEntity sifter) {
		this.sifter = sifter;
	}

	@Override
	public boolean canUse() {
		return !built;
	}

	@Override
	public void start() {
		BlockPos anchor = sifter.blockPosition();
		Level level = sifter.level();

		level.setBlockAndUpdate(anchor.offset(HomeLayout.BED_FOOT),
			Blocks.BLUE_BED.defaultBlockState()
				.setValue(BedBlock.FACING, Direction.SOUTH)
				.setValue(BedBlock.PART, BedPart.FOOT));

		level.setBlockAndUpdate(anchor.offset(HomeLayout.BED_HEAD),
			Blocks.BLUE_BED.defaultBlockState()
				.setValue(BedBlock.FACING, Direction.SOUTH)
				.setValue(BedBlock.PART, BedPart.HEAD));

		level.setBlockAndUpdate(anchor.offset(HomeLayout.CRAFTING_TABLE), Blocks.CRAFTING_TABLE.defaultBlockState());
		level.setBlockAndUpdate(anchor.offset(HomeLayout.FURNACE), Blocks.FURNACE.defaultBlockState());
		level.setBlockAndUpdate(anchor.offset(HomeLayout.POTTED_SAPLING), Blocks.POTTED_SPRUCE_SAPLING.defaultBlockState());

		built = true;
	}
}
```

A bed is actually *two* blocks in Minecraft (the pillow end and the foot
end) - that's why we place `BLUE_BED` twice, once with `BedPart.FOOT` and
once with `BedPart.HEAD`, both sharing the same `FACING` direction.

### 4. Add it to the goal list - `src/main/java/com/sifter/entity/SifterEntity.java`

```java
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(0, new BuildHomeGoal(this));
		this.goalSelector.addGoal(1, new MineSkulkGoal(this, 1.0, 8));
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
```

`BuildHomeGoal` shares priority `0` with `FloatGoal` - it only ever
actually does anything once (`canUse()` returns `false` forever after),
so it doesn't compete with anything for long.

## How to check it

```bash
./gradlew test    # HomeLayoutTest should be green
./gradlew build
```

Spawn a Sifter somewhere with flat, open space around it and watch - a
small home should appear built around wherever it was standing.

## Try tweaking it

- The whole home is described by five lines in `HomeLayout` - try
  changing `Direction.SOUTH`/`Direction.NORTH` to rotate the layout, or
  add a sixth part (e.g. a chest) the same way `FURNACE` was added:
  another constant, another line in `allOffsets()`, another
  `setBlockAndUpdate` call in `BuildHomeGoal`.
- Right now the home always builds exactly where the Sifter happens to
  be standing when it spawns, which can end up half-buried in a hill or
  overlapping something. A good "next session" challenge: before
  building, check the five target positions are all currently air (and
  have solid ground underneath) - and if not, don't build yet, try again
  next tick.

# Phase 12 — Sifter migrations

Companion to the Phase 12 section of [`LESSON_PLAN.md`](../LESSON_PLAN.md).

**Goal:** once a Sifter has abandoned its home (Phase 10), it sets off on
a long journey:

1. **Climb.** Head for high ground, at least 25 blocks above sea level.
2. **Look.** From up there, find the nearest sculk.
3. **Go.** Walk to it. Once there, settle and build a new home.

If it can't see any sculk from the hilltop, it wanders off in a new
direction, finds another hill, and looks again.

This is the biggest lesson in the plan, but it's mostly pieces you've
already met, put together. The sums go in small pure classes we can test.
The walking around goes in one goal we check by playing.

## Before we code: four ideas

### 1. Angles and directions

To go "that way" we need a direction. Directions are **angles**, and
programmers measure angles in **radians** rather than degrees: a full
turn is `2 * Math.PI` (about 6.28), a half turn is `Math.PI`, a quarter
turn is `Math.PI / 2`. Two functions turn an angle into steps:

```java
double eastSteps  = Math.cos(angle);   // 1.0 when facing east, 0.0 when facing south
double southSteps = Math.sin(angle);   // 0.0 when facing east, 1.0 when facing south
```

Multiply both by a distance and you have "walk 10 blocks that way".
`Math.sin` is the same function that made the Sifter sway in Phase 2 -
this time we use its partner `Math.cos` to point somewhere instead.
Spacing angles evenly around a full turn (`2 * Math.PI * i / points`)
gives a ring of directions, which is how we'll look around for a hill.

### 2. Do big things in small hops

A mob can only plan a walking route a short way ahead (about 16
blocks - its "follow range"). Telling it to walk 100 blocks in one go just
fails. The fix is to break the trip into **hops**: take the straight
line to the goal, walk the first 14 blocks of it, then work out the next
14, and so on. `nextWaypoint` does that, and it's pure maths, so it's
testable.

### 3. Stages (a state machine, again)

The journey is "in" one stage at a time: CLIMB, then SCAN, then TRAVEL,
and sometimes back to CLIMB. That's the state-machine idea from
Phase 10, so we hold the stages in an `enum` and use a `switch` to do
the right thing for the current one.

### 4. Method references and `stream().map(...)`

When you want to apply the same function to every item in a list, you
could write a `for` loop. Java also has a shorter way:

```java
List<Integer> doubled = numbers.stream().map(n -> n * 2).toList();
```

Read it left to right: "take the numbers, **map** each one to its double,
and make a list of the results." When the thing you want to do to each
item is just calling an existing method, you can name the method after
`::` instead of writing an arrow: `.map(this::onSurface)` means "call
`this.onSurface(...)` on each item". We use that below, and again in the
scanner; each time you could swap in a `for` loop and get the same result.

## You type this

### 1. Is it high enough? - `src/main/java/com/sifter/entity/HighGround.java`

```java
package com.sifter.entity;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Optional;

// Pure questions about height: "is this high enough to see from?" and
// "which of these spots is the highest?". Just numbers and coordinates.
public class HighGround {

	// A good lookout is this many blocks above the sea.
	public static final int HEIGHT_ABOVE_SEA = 25;

	private HighGround() {
	}

	public static boolean isHighEnough(int y, int seaLevel) {
		return y >= seaLevel + HEIGHT_ABOVE_SEA;
	}

	public static Optional<BlockPos> highest(List<BlockPos> candidates) {
		BlockPos best = null;

		for (BlockPos candidate : candidates) {
			if (best == null || candidate.getY() > best.getY()) {
				best = candidate;
			}
		}

		return Optional.ofNullable(best);
	}
}
```

### 2. The test - `src/test/java/com/sifter/entity/HighGroundTest.java`

```java
package com.sifter.entity;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HighGroundTest {

	private static final int SEA_LEVEL = 63;

	@Test
	void exactlyTwentyFiveBlocksAboveTheSeaIsHighEnough() {
		assertTrue(HighGround.isHighEnough(SEA_LEVEL + 25, SEA_LEVEL));
	}

	@Test
	void oneBlockLowerIsNot() {
		assertFalse(HighGround.isHighEnough(SEA_LEVEL + 24, SEA_LEVEL));
	}

	@Test
	void theSeaLevelItselfIsNotHighGround() {
		assertFalse(HighGround.isHighEnough(SEA_LEVEL, SEA_LEVEL));
	}

	@Test
	void picksTheHighestSpot() {
		BlockPos low = new BlockPos(0, 64, 0);
		BlockPos high = new BlockPos(5, 90, 5);
		BlockPos middle = new BlockPos(9, 70, 9);

		assertEquals(Optional.of(high), HighGround.highest(List.of(low, high, middle)));
	}

	@Test
	void whenSpotsTieTheFirstOneWins() {
		BlockPos first = new BlockPos(0, 80, 0);
		BlockPos second = new BlockPos(9, 80, 9);

		assertEquals(Optional.of(first), HighGround.highest(List.of(first, second)));
	}

	@Test
	void handlesAnEmptyListWithoutCrashing() {
		assertTrue(HighGround.highest(List.of()).isEmpty());
	}
}
```

The first three tests check *both sides of the line* - exactly 25 above
the sea is good, 24 isn't. Tests at the edge catch the same `<` vs `<=`
mistake from Phase 11.

### 3. The geometry of a journey - `src/main/java/com/sifter/entity/MigrationPlanner.java`

```java
package com.sifter.entity;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

// The geometry of a journey, as pure functions: where is "15 blocks that
// way", and how do I take a long trip in short hops? None of these touch
// the world, so they never look at height - every position that comes out
// keeps the Y of the one that went in, and the goal fills in the real
// ground height afterwards.
public class MigrationPlanner {

	private MigrationPlanner() {
	}

	// A point `distance` blocks away from `from`, in the direction
	// `heading`. The heading is an angle in radians (a full circle is 2 * PI):
	// 0 points east, PI / 2 points south. Math.cos and Math.sin turn an angle
	// into "how far east" and "how far south" - the same sin you met in
	// Phase 2, now used for walking in a circle instead of swaying.
	public static BlockPos pointAlong(BlockPos from, double heading, int distance) {
		int x = from.getX() + (int) Math.round(Math.cos(heading) * distance);
		int z = from.getZ() + (int) Math.round(Math.sin(heading) * distance);
		return new BlockPos(x, from.getY(), z);
	}

	// `points` spots spread evenly on a circle around `centre`.
	public static List<BlockPos> ringAround(BlockPos centre, int radius, int points) {
		List<BlockPos> ring = new ArrayList<>();

		for (int i = 0; i < points; i++) {
			double heading = 2 * Math.PI * i / points;
			ring.add(pointAlong(centre, heading, radius));
		}

		return ring;
	}

	// One hop of a long trip: the point at most `maxStep` blocks from `from`
	// on the straight line towards `to`. If `to` is already that close, the
	// hop just goes all the way there.
	public static BlockPos nextWaypoint(BlockPos from, BlockPos to, int maxStep) {
		double dx = to.getX() - from.getX();
		double dz = to.getZ() - from.getZ();
		double distance = Math.sqrt(dx * dx + dz * dz);

		if (distance <= maxStep) {
			return new BlockPos(to.getX(), from.getY(), to.getZ());
		}

		double scale = maxStep / distance;
		return new BlockPos(
			from.getX() + (int) Math.round(dx * scale),
			from.getY(),
			from.getZ() + (int) Math.round(dz * scale));
	}
}
```

### 4. The test - `src/test/java/com/sifter/entity/MigrationPlannerTest.java`

```java
package com.sifter.entity;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationPlannerTest {

	private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);

	// How far apart two spots are, ignoring height.
	private static double flatDistance(BlockPos a, BlockPos b) {
		double dx = a.getX() - b.getX();
		double dz = a.getZ() - b.getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}

	@Test
	void heading0PointsEast() {
		assertEquals(new BlockPos(10, 64, 0), MigrationPlanner.pointAlong(ORIGIN, 0, 10));
	}

	@Test
	void aQuarterTurnPointsSouth() {
		assertEquals(new BlockPos(0, 64, 10), MigrationPlanner.pointAlong(ORIGIN, Math.PI / 2, 10));
	}

	@Test
	void aHalfTurnPointsWest() {
		assertEquals(new BlockPos(-10, 64, 0), MigrationPlanner.pointAlong(ORIGIN, Math.PI, 10));
	}

	@Test
	void theRingHasTheRequestedNumberOfPoints() {
		assertEquals(8, MigrationPlanner.ringAround(ORIGIN, 12, 8).size());
	}

	@Test
	void everyPointOnTheRingIsAboutRadiusAway() {
		for (BlockPos point : MigrationPlanner.ringAround(ORIGIN, 12, 8)) {
			assertEquals(12.0, flatDistance(ORIGIN, point), 1.0);
		}
	}

	@Test
	void aRingOfNoPointsIsEmpty() {
		assertTrue(MigrationPlanner.ringAround(ORIGIN, 12, 0).isEmpty());
	}

	@Test
	void aTargetWithinOneHopIsReachedDirectly() {
		BlockPos target = new BlockPos(5, 64, 5);

		assertEquals(target, MigrationPlanner.nextWaypoint(ORIGIN, target, 14));
	}

	@Test
	void aFarTargetIsApproachedOneHopAtATime() {
		BlockPos farEast = new BlockPos(100, 64, 0);

		assertEquals(new BlockPos(14, 64, 0), MigrationPlanner.nextWaypoint(ORIGIN, farEast, 14));
	}

	@Test
	void aDiagonalHopStillTravelsAboutOneHopLength() {
		BlockPos farAway = new BlockPos(100, 64, 100);

		BlockPos hop = MigrationPlanner.nextWaypoint(ORIGIN, farAway, 14);

		assertEquals(14.0, flatDistance(ORIGIN, hop), 1.0);
		assertEquals(hop.getX(), hop.getZ());
	}

	@Test
	void hoppingRepeatedlyEventuallyArrives() {
		BlockPos target = new BlockPos(-60, 64, 45);
		BlockPos position = ORIGIN;

		for (int hops = 0; hops < 20 && !position.equals(target); hops++) {
			position = MigrationPlanner.nextWaypoint(position, target, 14);
		}

		assertEquals(target, position);
	}

	@Test
	void standingOnTheTargetStaysPut() {
		assertEquals(ORIGIN, MigrationPlanner.nextWaypoint(ORIGIN, ORIGIN, 14));
	}
}
```

Two things to notice. `assertEquals(12.0, distance, 1.0)` has a third
number, a **tolerance**: "equal, give or take 1". Block positions are
whole numbers, so a point 12 blocks away at an awkward angle can't be
exactly 12.0 away. And `hoppingRepeatedlyEventuallyArrives` is a
different style of test: instead of checking one hop, it checks that the
*whole strategy* works, by repeating hops until it arrives.

### 5. Seeing far, cheaply - `src/main/java/com/sifter/entity/SculkScanner.java`

Checking every block within 128 blocks would be millions of checks.
But the world is cut into 16x16x16 **sections**, and each section can
answer "do you contain any sculk at all?" very quickly. We ask every
section and keep the centre of each one that says yes.

```java
package com.sifter.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.List;

// Looks for sculk from far away, cheaply. Checking every block in a big
// area would take far too long, so instead we ask each 16x16x16 "section"
// of the world one question - "do you contain any sculk at all?" - which
// Minecraft can answer almost instantly. The answer is the middle of every
// section that says yes. Touches the live world, so we check it by playing.
public class SculkScanner {

	private SculkScanner() {
	}

	public static List<BlockPos> sectionsWithSculk(ServerLevel level, BlockPos from, int chunkRadius) {
		List<BlockPos> found = new ArrayList<>();
		int centreChunkX = SectionPos.blockToSectionCoord(from.getX());
		int centreChunkZ = SectionPos.blockToSectionCoord(from.getZ());

		for (int chunkX = centreChunkX - chunkRadius; chunkX <= centreChunkX + chunkRadius; chunkX++) {
			for (int chunkZ = centreChunkZ - chunkRadius; chunkZ <= centreChunkZ + chunkRadius; chunkZ++) {
				// getChunkNow gives back null for a chunk that isn't loaded,
				// instead of loading it - the Sifter can only see what's
				// already there.
				LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
				if (chunk == null) {
					continue;
				}

				LevelChunkSection[] sections = chunk.getSections();
				for (int i = 0; i < sections.length; i++) {
					if (sections[i].maybeHas(SculkBlocks::isMinable)) {
						int sectionY = chunk.getMinSection() + i;
						found.add(new BlockPos(
							SectionPos.sectionToBlockCoord(chunkX, 8),
							SectionPos.sectionToBlockCoord(sectionY, 8),
							SectionPos.sectionToBlockCoord(chunkZ, 8)));
					}
				}
			}
		}

		return found;
	}
}
```

`SculkBlocks::isMinable` is a method reference (idea 4). It reuses the
same "what counts as sculk" rule as the miner. The result is a plain list
of coordinates - which `SculkFinder.nearest` from Phase 4 can already
sort out. Nothing new to write for "which is closest".

### 6. The journey - `src/main/java/com/sifter/entity/MigrateGoal.java`

```java
package com.sifter.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

// After abandoning its home, the Sifter goes on a journey:
//   1. CLIMB  - head for higher ground, until it is 25 blocks above sea level.
//   2. SCAN   - from up there, look for the nearest sculk.
//   3. TRAVEL - walk towards it, in short hops, until it gets there.
// If the scan finds nothing, it wanders off in a new direction and starts
// again from the climb. All the sums live in HighGround, MigrationPlanner
// and SculkFinder (which we test); this class is the part that actually
// walks around the live world, so we check it by playing.
public class MigrateGoal extends Goal {

	// Each of the three steps above is a "stage". The goal is always in
	// exactly one of them - this is the "state" idea from Phase 4 again,
	// with an enum (like HomeStatus) holding the possibilities.
	private enum Stage {
		CLIMB, SCAN, TRAVEL
	}

	private static final double SPEED = 1.0;
	private static final int RING_RADIUS = 12;
	private static final int RING_POINTS = 8;
	// Navigation can't plan paths much beyond the mob's follow range
	// (16 blocks), so a long trip is a chain of shorter ones.
	private static final int HOP_LENGTH = 14;
	private static final int WANDER_DISTANCE = 40;
	// How far the scan looks, in chunks (16 blocks each), and how close
	// counts as having arrived, in blocks.
	private static final int VIEW_CHUNKS = 8;
	private static final int ARRIVAL_DISTANCE = 8;
	// If this many hops in a row get nowhere, we must be stuck: turn.
	private static final int STUCK_LIMIT = 3;

	private final SifterEntity sifter;

	private Stage stage;
	private double heading;
	private BlockPos destination;
	private BlockPos lastPosition;
	private int stuckCount;

	public MigrateGoal(SifterEntity sifter) {
		this.sifter = sifter;
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		return sifter.getHomeStatus() == HomeStatus.ABANDONED && !sifter.isSculkNearby();
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public void start() {
		stage = Stage.CLIMB;
		heading = sifter.getRandom().nextDouble() * 2 * Math.PI;
		destination = null;
		lastPosition = sifter.blockPosition();
		stuckCount = 0;
	}

	@Override
	public void tick() {
		// Only decide the next move once the last one is finished.
		if (!sifter.getNavigation().isDone()) {
			return;
		}

		switch (stage) {
			case CLIMB -> climb();
			case SCAN -> scan();
			case TRAVEL -> travel();
		}
	}

	private void climb() {
		Level level = sifter.level();
		if (HighGround.isHighEnough(sifter.getBlockY(), level.getSeaLevel())) {
			stage = Stage.SCAN;
			return;
		}

		List<BlockPos> ring = MigrationPlanner.ringAround(sifter.blockPosition(), RING_RADIUS, RING_POINTS)
			.stream()
			.map(this::onSurface)
			.toList();
		Optional<BlockPos> best = HighGround.highest(ring);

		if (best.isPresent() && best.get().getY() > sifter.getBlockY()) {
			walkTo(best.get());
		} else {
			// Nothing around is any higher - keep going the way we were.
			walkTo(onSurface(MigrationPlanner.pointAlong(sifter.blockPosition(), heading, WANDER_DISTANCE)));
		}
	}

	private void scan() {
		ServerLevel level = (ServerLevel) sifter.level();
		BlockPos here = sifter.blockPosition();

		List<BlockPos> candidates = SculkScanner.sectionsWithSculk(level, here, VIEW_CHUNKS);
		Optional<BlockPos> nearest = SculkFinder.nearest(here, candidates, (VIEW_CHUNKS + 1) * 16);

		if (nearest.isPresent()) {
			destination = nearest.get();
			stage = Stage.TRAVEL;
		} else {
			// Nothing in sight. Turn a little, wander off, and try again
			// from the next hill.
			heading += (sifter.getRandom().nextDouble() - 0.5) * Math.PI;
			walkTo(onSurface(MigrationPlanner.pointAlong(here, heading, WANDER_DISTANCE)));
			stage = Stage.CLIMB;
		}
	}

	private void travel() {
		BlockPos here = sifter.blockPosition();
		int blocksAway = Math.abs(destination.getX() - here.getX()) + Math.abs(destination.getZ() - here.getZ());

		if (blocksAway <= ARRIVAL_DISTANCE) {
			// We're here. Settle down: BuildHomeGoal will do the rest.
			sifter.settle();
			return;
		}

		walkTo(onSurface(MigrationPlanner.nextWaypoint(here, destination, HOP_LENGTH)));
	}

	// Start walking to a spot, and notice if we're not getting anywhere.
	private void walkTo(BlockPos target) {
		BlockPos here = sifter.blockPosition();
		if (here.distSqr(lastPosition) < 4) {
			stuckCount++;
		} else {
			stuckCount = 0;
		}
		lastPosition = here;

		if (stuckCount >= STUCK_LIMIT) {
			// Blocked by water or a cliff. Turn a right angle and try
			// again from the top.
			heading += Math.PI / 2;
			stuckCount = 0;
			stage = Stage.CLIMB;
			target = onSurface(MigrationPlanner.pointAlong(here, heading, WANDER_DISTANCE));
		}

		sifter.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, SPEED);
	}

	// The planner only knows flat coordinates. This finds the real ground
	// height there - but only if that part of the world is already loaded,
	// because asking about an unloaded chunk would make the game load it.
	private BlockPos onSurface(BlockPos flat) {
		Level level = sifter.level();
		if (!level.hasChunkAt(flat)) {
			return flat;
		}
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, flat.getX(), flat.getZ());
		return new BlockPos(flat.getX(), y, flat.getZ());
	}
}
```

Read it from the top. `tick()` is the heartbeat: wait until the last
walk is finished, then do one step of whichever stage we're in. `climb`,
`scan` and `travel` each end by setting where to go and (sometimes)
which stage is next. The only maths is calls into the tested classes.

Two details worth a second look:

- `onSurface` exists because the pure planner only knows flat X/Z
  coordinates; this turns "(120, ?, 85)" into "(120, 71, 85)" by asking the
  world how high the ground is there. It checks `hasChunkAt` first,
  because asking about a part of the world that isn't loaded would force
  the game to load or even generate it, in the middle of a tick.
- `walkTo` is also where "am I stuck?" is spotted: if three walks in a row
  leave the Sifter in the same place (a lake, a cliff), it turns 90
  degrees, goes back to CLIMB, and tries again.

### 7. Tell the Sifter - `src/main/java/com/sifter/entity/SifterEntity.java`

Add the new goal at priority 2 (the same as `GoHomeGoal`; only one can
be active, because the Sifter's status is either `LIVED_IN` or
`ABANDONED`, never both):

```java
		this.goalSelector.addGoal(2, new MigrateGoal(this));
```

and the method `MigrateGoal` calls when it arrives:

```java
	// Called by MigrateGoal when the journey is over: forget the old home,
	// so BuildHomeGoal builds a new one right here.
	public void settle() {
		this.homePos = null;
		this.homeStatus = HomeStatus.NO_HOME;
	}
```

That `NO_HOME` is the key: `BuildHomeGoal` can use the Sifter again, a
new home goes up, and the cycle of Phase 10 starts over in a new place.

## How to check it

```bash
./gradlew test    # HighGroundTest, MigrationPlannerTest should be green
./gradlew build
```

A migration takes real time, so set the scene in a creative world:

1. Find a place with a hill (or mountain) and a lump of sculk some
   distance off - an ancient city entrance is ideal
   (`/locate structure minecraft:ancient_city`). Stay within about 100
   blocks so the chunks stay loaded.
2. Spawn a Sifter *without* sculk near it. It builds a home (Phase 5) and,
   a few seconds later, abandons it (Phase 10).
3. Watch it set off. First it should wander towards higher ground.
   When it reaches a tall enough hill it should pause, then walk steadily
   towards the sculk, in hops. On arrival, a fresh home should appear.

If you want to watch the details, temporarily log the stage changes with
`Sifter.LOGGER.info("stage: " + stage)` (Phase 0's logging!) at the start
of `climb`, `scan` and `travel`, and read them in the game's log.

## Things worth knowing (honest limits)

- **Sculk underground.** Sculk usually lives deep underground, but
  `MineSculkGoal` only looks one block above and below the Sifter. A
  Sifter that finishes its journey on the surface above a deep sculk
  patch will settle there, find nothing minable, abandon the new home -
  and migrate again. A real improvement would be teaching the miner to
  search further up and down (the 4-argument `MoveToBlockGoal`
  constructor takes a vertical range), and the radar the same.
- **Loaded chunks only.** The Sifter can only see, and only moves, while
  its part of the world is loaded - that is, while you are somewhere
  near. If you fly away, it pauses mid-journey and carries on when you
  return. (This is the same reason it should not despawn: Phase 9.)
- **Simple pathfinding.** The Sifter will not build bridges or swim long
  distances. If it's stuck on an island, it will keep turning and trying.

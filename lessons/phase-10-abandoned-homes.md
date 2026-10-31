# Phase 10 — Abandoned homes

Companion to the Phase 10 section of [`LESSON_PLAN.md`](../LESSON_PLAN.md).

**Goal:** when a Sifter can't find any sculk nearby, it walks back to its
home, and the home becomes *abandoned*: the bed and the sapling are gone,
leaving the crafting table, the furnace and an empty flower pot.

Right now the Sifter only knows one thing about its home: `homeBuilt`,
true or false. This lesson needs it to know three things (no home, living
in one, left one behind) *and* where the home is. We'll replace the
true/false with something better suited.

## Before we code: four ideas

### 1. Enums

A boolean has two values. What if you need three? An **enum** is a type
whose values are a short, fixed list that you write yourself:

```java
enum TrafficLight { RED, AMBER, GREEN }

TrafficLight light = TrafficLight.RED;
```

A `TrafficLight` can only ever be one of those three - Java won't let you
write `TrafficLight.PURPLE` - so a whole class of silly mistakes can't
happen. Use an enum whenever something is in exactly one of several named
situations.

### 2. `switch`

With an enum, the natural way to say "do something different for each
value" is `switch`. It's a tidier `if / else if / else if`:

```java
String instruction = switch (light) {
	case RED -> "stop";
	case AMBER -> "get ready";
	case GREEN -> "go";
};
```

Bonus: if you add a fourth colour later and forget to handle it here,
Java refuses to compile. The mistake can't hide.

### 3. State machines

A **state machine** is a thing that is always in exactly one state, plus
a list of rules for moving between states. A traffic light is one:
RED becomes GREEN, GREEN becomes AMBER, AMBER becomes RED. The rules
can be written as a pure function - `next(currentState, whatIsHappening)`
returns the new state - which means we can test every rule with a plain
table of examples. That's exactly what we do for the Sifter's home.

### 4. `null`: "nothing here"

A variable that is supposed to hold an object (like a position) but
currently holds *nothing* has the value `null`. A Sifter with no home has
no home position, so `homePos` is `null`. The danger: asking a `null`
for anything (`homePos.getX()`) crashes the game. So **check before you
use**: `if (homePos != null) { ... }`. You'll see that check in a few
places below.

### Bonus idea: saving and loading

When you quit and come back, the game writes every mob's data to disk
and reads it back later. The Sifter already does this for `homeBuilt`
(Phase 5). Our new status and position need the same treatment, or a
Sifter would forget where its home was every time you reloaded. And
there's a subtlety: worlds saved *before* this lesson contain the old
`HomeBuilt` flag. A real mod must keep loading old worlds, so we read
the old flag as well.

## You type this

### 1. The three situations - `src/main/java/com/sifter/entity/HomeStatus.java`

```java
package com.sifter.entity;

// An enum is a type with a short, fixed list of allowed values - and
// nothing else. A Sifter's home can be in exactly one of these three
// situations, which is easier to get right than a pile of true/false flags
// (a boolean can't say "has no home" and "had one but left" differently).
public enum HomeStatus {
	// Hasn't built a home yet (or has just settled somewhere new).
	NO_HOME,
	// Built a home and still lives in it.
	LIVED_IN,
	// Built a home, ran out of sculk nearby and left it behind.
	ABANDONED
}
```

### 2. The rules - `src/main/java/com/sifter/entity/HomeLifecycle.java`

```java
package com.sifter.entity;

// The rules for how a home's status changes, as a pure function: current
// status plus what's going on around the Sifter in, new status out. No
// world, no entity - which means we can test every rule with a table of
// examples.
public class HomeLifecycle {

	private HomeLifecycle() {
	}

	public static HomeStatus next(HomeStatus current, boolean sculkNearby, boolean atHome) {
		return switch (current) {
			// Nothing to leave behind yet. (Building a home is what moves a
			// Sifter out of NO_HOME - see BuildHomeGoal.)
			case NO_HOME -> HomeStatus.NO_HOME;
			// No sculk left? Once the Sifter has made it home, give up on it.
			case LIVED_IN -> (!sculkNearby && atHome) ? HomeStatus.ABANDONED : HomeStatus.LIVED_IN;
			// Found sculk again: ready to settle and build somewhere new.
			case ABANDONED -> sculkNearby ? HomeStatus.NO_HOME : HomeStatus.ABANDONED;
		};
	}
}
```

The three `case` lines *are* the whole story of a home:

```
NO_HOME --(build a home)--> LIVED_IN --(no sculk, and at home)--> ABANDONED
   ^                                                                  |
   +---------------------(found sculk again)--------------------------+
```

### 3. The test - `src/test/java/com/sifter/entity/HomeLifecycleTest.java`

```java
package com.sifter.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeLifecycleTest {

	@Test
	void aSifterWithNoHomeStaysThatWayWhateverHappens() {
		assertEquals(HomeStatus.NO_HOME, HomeLifecycle.next(HomeStatus.NO_HOME, true, true));
		assertEquals(HomeStatus.NO_HOME, HomeLifecycle.next(HomeStatus.NO_HOME, false, false));
	}

	@Test
	void aHomeIsKeptWhileThereIsSculkNearby() {
		assertEquals(HomeStatus.LIVED_IN, HomeLifecycle.next(HomeStatus.LIVED_IN, true, true));
		assertEquals(HomeStatus.LIVED_IN, HomeLifecycle.next(HomeStatus.LIVED_IN, true, false));
	}

	@Test
	void aHomeIsKeptUntilTheSifterActuallyGetsThere() {
		assertEquals(HomeStatus.LIVED_IN, HomeLifecycle.next(HomeStatus.LIVED_IN, false, false));
	}

	@Test
	void noSculkAndAtHomeMeansAbandoned() {
		assertEquals(HomeStatus.ABANDONED, HomeLifecycle.next(HomeStatus.LIVED_IN, false, true));
	}

	@Test
	void anAbandonedHomeStaysAbandonedWhileThereIsStillNoSculk() {
		assertEquals(HomeStatus.ABANDONED, HomeLifecycle.next(HomeStatus.ABANDONED, false, false));
		assertEquals(HomeStatus.ABANDONED, HomeLifecycle.next(HomeStatus.ABANDONED, false, true));
	}

	@Test
	void findingSculkAfterAbandoningMeansTimeToSettleAgain() {
		assertEquals(HomeStatus.NO_HOME, HomeLifecycle.next(HomeStatus.ABANDONED, true, false));
	}
}
```

Six small tests cover every row of the rules. If you later change a rule
and forget what depended on it, one of these goes red.

### 4. The blueprint learns about beds - `src/main/java/com/sifter/entity/HomeLayout.java`

Abandoning a home means removing the bed. Add one method next to
`allOffsets()`:

```java
	public static List<BlockPos> bedOffsets() {
		return List.of(BED_FOOT, BED_HEAD);
	}
```

and two tests at the bottom of `HomeLayoutTest` (plus the imports
`java.util.List` and `static org.junit.jupiter.api.Assertions.assertTrue`):

```java
	@Test
	void theBedIsMadeOfTheTwoBedBlocks() {
		assertEquals(List.of(HomeLayout.BED_FOOT, HomeLayout.BED_HEAD), HomeLayout.bedOffsets());
	}

	@Test
	void everyBedBlockIsPartOfTheHome() {
		assertTrue(HomeLayout.allOffsets().containsAll(HomeLayout.bedOffsets()));
	}
```

### 5. Looking for sculk - `src/main/java/com/sifter/entity/SculkBlocks.java`

Two parts of the code now need to ask "is this block something we mine?"
(the miner, and the new "is there any left?" check), so the question
gets one home:

```java
package com.sifter.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// Questions about sculk that more than one part of the Sifter asks:
// "is this block something we mine?" and "is there any of it around here?"
public class SculkBlocks {

	// MoveToBlockGoal (which MineSculkGoal is built on) only looks one
	// block up and down by default. The radar looks exactly as far as the
	// miner can, so "radar says yes" always means "the miner can reach it".
	private static final int VERTICAL_RANGE = 1;

	private SculkBlocks() {
	}

	public static boolean isMinable(BlockState state) {
		return state.is(Blocks.SCULK) || state.is(Blocks.SCULK_VEIN) || state.is(Blocks.SCULK_CATALYST);
	}

	// Checks every block in a flat slab around the centre. About 11,000
	// blocks for a range of 30 - fine once every few seconds, far too slow
	// to do every tick.
	public static boolean isNear(LevelReader level, BlockPos centre, int range) {
		BlockPos corner1 = centre.offset(-range, -VERTICAL_RANGE, -range);
		BlockPos corner2 = centre.offset(range, VERTICAL_RANGE, range);
		for (BlockPos pos : BlockPos.betweenClosed(corner1, corner2)) {
			if (isMinable(level.getBlockState(pos))) {
				return true;
			}
		}
		return false;
	}
}
```

Why the "about 11,000 blocks" comment matters: this check really does
look at every block in a flat slab, and doing that 20 times a second for
every Sifter would make the game stutter. We'll run it rarely and
**remember the answer** (a technique called *caching*).

Now make `MineSculkGoal` use it. In `isValidTarget`, replace the two
lines that name the three blocks with:

```java
		return SculkBlocks.isMinable(level.getBlockState(pos));
```

(You can then delete the now-unused `BlockState` import.)

### 6. Stripping a home - `src/main/java/com/sifter/entity/AbandonedHome.java`

```java
package com.sifter.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

// The opposite of BuildHomeGoal: takes the bed and the sapling away but
// leaves the crafting table, furnace and (empty) flower pot standing, so
// you can tell a Sifter used to live here. Like the goal, it touches the
// live world, so we check it by playing.
public class AbandonedHome {

	private AbandonedHome() {
	}

	public static void strip(Level level, BlockPos anchor) {
		for (BlockPos offset : HomeLayout.bedOffsets()) {
			level.setBlockAndUpdate(anchor.offset(offset), Blocks.AIR.defaultBlockState());
		}
		level.setBlockAndUpdate(anchor.offset(HomeLayout.POTTED_SAPLING), Blocks.FLOWER_POT.defaultBlockState());
	}
}
```

### 7. Walking home - `src/main/java/com/sifter/entity/GoHomeGoal.java`

```java
package com.sifter.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

// When the Sifter has no sculk left nearby, walk back to its home. Just
// the walking - noticing that it has arrived, and abandoning the home, is
// the SifterEntity's job (see updateHomeStatus there).
public class GoHomeGoal extends Goal {

	private final SifterEntity sifter;
	private final double speedModifier;

	public GoHomeGoal(SifterEntity sifter, double speedModifier) {
		this.sifter = sifter;
		this.speedModifier = speedModifier;
		// This goal moves the Sifter, so it can't run at the same time as
		// another goal that also wants to move it.
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		return sifter.getHomeStatus() == HomeStatus.LIVED_IN
			&& !sifter.isSculkNearby()
			&& sifter.getHomePos() != null;
	}

	@Override
	public boolean canContinueToUse() {
		return canUse() && !sifter.isAtHome();
	}

	@Override
	public void start() {
		BlockPos home = sifter.getHomePos();
		sifter.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, speedModifier);
	}
}
```

`setFlags(EnumSet.of(Flag.MOVE))` is new. Goals declare which of the mob's
abilities they use (`MOVE`, `LOOK`, `JUMP`). Two goals that both want to
`MOVE` can't run at once, so the higher priority one wins. Without the
flag, this goal and the sculk miner would fight over the legs.

### 8. Change the Sifter - `src/main/java/com/sifter/entity/SifterEntity.java`

This is the biggest edit. Go through it one piece at a time.

**Imports to add:**

```java
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
```

**Replace** the `homeBuilt` field and its comment with:

```java
	// Where this Sifter is in the life of its home, and where that home is.
	// Lives on the entity (not a goal) because goals are recreated on load,
	// but the entity's saved data survives.
	private HomeStatus homeStatus = HomeStatus.NO_HOME;
	private BlockPos homePos;
	// The latest answer to "is there sculk within reach?". Starts true so a
	// brand-new Sifter doesn't assume the worst before its first look.
	private boolean sculkNearby = true;
```

**New constants**, with the other constants:

```java
	// How often (in ticks) to look for sculk, and how far to look. Looking
	// is slow, so it's every 5 seconds; the range matches MineSculkGoal's.
	private static final int SCULK_CHECK_INTERVAL = 100;
	private static final int SCULK_SEARCH_RANGE = 30;
	// How close counts as "at home", in blocks.
	private static final double HOME_REACH = 3.0;
	// Re-evaluating the home's status is cheap, so it's done more often.
	private static final int HOME_STATUS_INTERVAL = 20;
```

**Replace** `hasBuiltHome()` and `setHomeBuilt(...)` with:

```java
	public HomeStatus getHomeStatus() {
		return homeStatus;
	}

	public BlockPos getHomePos() {
		return homePos;
	}

	public boolean isSculkNearby() {
		return sculkNearby;
	}

	// Called by BuildHomeGoal once the blocks are down.
	public void moveIn(BlockPos anchor) {
		this.homePos = anchor;
		this.homeStatus = HomeStatus.LIVED_IN;
	}

	public boolean isAtHome() {
		return homePos != null && homePos.closerThan(this.blockPosition(), HOME_REACH);
	}
```

**Replace** the two save/load methods:

```java
	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putString("HomeStatus", homeStatus.name());
		if (homePos != null) {
			tag.put("HomePos", NbtUtils.writeBlockPos(homePos));
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("HomeStatus")) {
			homeStatus = HomeStatus.valueOf(tag.getString("HomeStatus"));
		} else if (tag.getBoolean("HomeBuilt")) {
			// A Sifter saved before Phase 10 only knew "built or not".
			homeStatus = HomeStatus.LIVED_IN;
		}
		homePos = NbtUtils.readBlockPos(tag, "HomePos").orElse(null);
	}
```

`.name()` turns an enum value into its text (`"LIVED_IN"`) and
`valueOf` turns the text back. `readBlockPos` returns an `Optional` (you
met those in `SculkFinder`); `.orElse(null)` means "the position if it was
saved, otherwise nothing".

**Add** the goal to `registerGoals()`, and shift the two look goals down
by one:

```java
		this.goalSelector.addGoal(2, new GoHomeGoal(this, 1.0));
		this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 5.0F));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
```

**Extend** `customServerAiStep()` from Phase 8. Under the warden
`if`, add two more ticking jobs, each using `TickTimer`:

```java
		if (TickTimer.isDue(this.tickCount, SCULK_CHECK_INTERVAL)) {
			sculkNearby = SculkBlocks.isNear(this.level(), this.blockPosition(), SCULK_SEARCH_RANGE);
		}
		if (TickTimer.isDue(this.tickCount, HOME_STATUS_INTERVAL)) {
			updateHomeStatus();
		}
```

and add the method they call:

```java
	// Ask HomeLifecycle what should happen next, and if the home has just
	// been given up on, take the bed and the sapling away.
	private void updateHomeStatus() {
		HomeStatus before = homeStatus;
		homeStatus = HomeLifecycle.next(before, sculkNearby, isAtHome());

		if (before == HomeStatus.LIVED_IN && homeStatus == HomeStatus.ABANDONED) {
			AbandonedHome.strip(this.level(), homePos);
		}
	}
```

Notice the split: the decision lives in the pure, tested
`HomeLifecycle`; this method only *acts* on it.

### 9. Update the builder - `src/main/java/com/sifter/entity/BuildHomeGoal.java`

Two one-line changes:

```java
	public boolean canUse() {
		return sifter.getHomeStatus() == HomeStatus.NO_HOME;
	}
```

and, at the end of `start()`, replace `sifter.setHomeBuilt(true);` with:

```java
		sifter.moveIn(anchor);
```

## How to check it

```bash
./gradlew test    # HomeLifecycleTest, HomeLayoutTest should be green
./gradlew build
```

In the game, make a patch of sculk (a few blocks, as in Phase 4) and
spawn a Sifter beside it. It builds its home, mines sculk, and carries
on. Now **mine away or remove all the sculk** within 30 blocks. Within
about 5 seconds the Sifter should notice, walk back to its home, and the
bed and the sapling should disappear, leaving the furnace, crafting
table and an empty pot.

If you spawn a Sifter with *no* sculk anywhere near, the same thing
happens in about 5 seconds: it builds a home and abandons it almost
immediately. That's correct - it's doing exactly what the rules say. It
just looks silly, which suggests a good challenge below.

## Try tweaking it

- Patience: make the Sifter wait for *three* sculk checks in a row to find
  nothing before giving up. (Hint: where would a counter live? On the
  entity, like `sculkNearby`. What could `HomeLifecycle` take as an extra
  parameter?)
- Add a test for that new rule first, watch it go red, then make it
  green.

# Phase 8 — Wardens attack Sifters

Companion to the Phase 8 section of [`LESSON_PLAN.md`](../LESSON_PLAN.md).

**Goal:** a Warden that's near a Sifter notices it and attacks it.

A Warden isn't built like our Sifter. Most mobs choose what to do using
the **goals** list from Phase 4; the Warden uses a much more elaborate
system of its own ("brain", with memories and senses), and that system
has no door for us to add a goal to. What it does have is an *anger
list*: a record of how angry it is at each creature, and a public
method, `increaseAngerAt(...)`, that raises it. When anger at something
passes a threshold, the Warden attacks it.

So the plan is a little backwards but works well: **the Sifter pokes
nearby Wardens, once a second.** To do that the Sifter needs two new
abilities: doing something *every so often*, and *looking for other
mobs* in the area.

## Before we code: three small ideas

### 1. The remainder operator `%`

Division with leftovers. `%` gives you the leftover:

```java
17 % 5   // 2  - five goes into 17 three times (15), with 2 left over
20 % 5   // 0  - five goes into 20 exactly, nothing left over
```

A remainder of 0 means "it divides evenly", and that happens exactly
once every N steps. Try it on paper: which of 0, 1, 2, ... 12 give `% 4 == 0`?
(0, 4, 8, 12 - every fourth one.) That's the trick for "do this every N
ticks".

### 2. Ticks

Minecraft runs 20 times a second. Each run is a **tick**, and every
entity has a counter, `tickCount`, that goes up by one each tick. So
"once a second" is "every 20 ticks", and `tickCount % 20 == 0` is true
on exactly those ticks.

### 3. The server and the client

When you play singleplayer, two halves are running inside the one
program: a **server** that owns the world and decides what happens, and a
**client** that draws it and takes your keyboard and mouse. Mob
behaviour belongs to the server. Code that only the server should run
goes in methods like `customServerAiStep()`, which the game calls for us
and which never runs on the client half.

## You type this

### 1. A pure timer - `src/main/java/com/sifter/entity/TickTimer.java`

```java
package com.sifter.entity;

// "Is it time yet?" - pure arithmetic, pulled out so we can test it.
// Minecraft counts ticks upwards forever (20 per second); this answers
// "should the thing I only do every N ticks happen on this tick?"
public class TickTimer {

	private TickTimer() {
	}

	// % is the "remainder" operator: 45 % 20 is 5, because 20 goes into 45
	// twice (40) with 5 left over. The remainder is 0 exactly once per
	// interval - on tick 0, 20, 40, 60...
	public static boolean isDue(int tick, int interval) {
		return tick % interval == 0;
	}
}
```

Pure again: a number goes in, a yes/no comes out.

### 2. The test - `src/test/java/com/sifter/entity/TickTimerTest.java`

```java
package com.sifter.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TickTimerTest {

	@Test
	void isDueOnTickZeroAndEveryIntervalAfter() {
		assertTrue(TickTimer.isDue(0, 20));
		assertTrue(TickTimer.isDue(20, 20));
		assertTrue(TickTimer.isDue(40, 20));
	}

	@Test
	void isNotDueInBetween() {
		for (int tick = 1; tick < 20; tick++) {
			assertFalse(TickTimer.isDue(tick, 20), "tick " + tick + " should not be due");
		}
	}

	@Test
	void anIntervalOfOneIsDueEveryTick() {
		for (int tick = 0; tick < 10; tick++) {
			assertTrue(TickTimer.isDue(tick, 1));
		}
	}
}
```

The middle test uses the loop you met in Phase 4 to check *every* tick
between two pulses in one go, with a message that says which tick was
wrong if one fails.

### 3. Poke the Wardens - `src/main/java/com/sifter/entity/SifterEntity.java`

Add these imports:

```java
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.AABB;
```

Add these constants next to the other fields:

```java
	// Wardens within this many blocks get provoked, once per second, for this
	// much anger each time. A warden turns "agitated" at 40 anger and
	// "angry" (starts attacking) at 80, so three pulses is enough.
	private static final int WARDEN_PROVOKE_RANGE = 24;
	private static final int WARDEN_PROVOKE_INTERVAL = 20;
	private static final int WARDEN_PROVOKE_ANGER = 35;
```

Add these two methods (anywhere in the class):

```java
	// Minecraft calls this every tick, on the server only (the client never
	// runs mob AI). We use it to do one small job once a second.
	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();

		if (TickTimer.isDue(this.tickCount, WARDEN_PROVOKE_INTERVAL)) {
			provokeNearbyWardens();
		}
	}

	// Look in a box around us for Wardens, and make each one angrier at us.
	// Wardens already track anger per target - we're just adding ourselves
	// to the list of things they're angry at.
	private void provokeNearbyWardens() {
		AABB searchBox = this.getBoundingBox().inflate(WARDEN_PROVOKE_RANGE);
		for (Warden warden : this.level().getEntitiesOfClass(Warden.class, searchBox)) {
			warden.increaseAngerAt(this, WARDEN_PROVOKE_ANGER, true);
		}
	}
```

Reading the pieces:

- `super.customServerAiStep()` runs the parent's version first, so we add
  to what mobs already do instead of replacing it. (Same `super` idea as
  the constructor in Phase 1.)
- An `AABB` is "axis-aligned bounding box": a box with its sides lined up
  with the world's axes. `getBoundingBox()` is the box the Sifter fills;
  `.inflate(24)` makes it 24 blocks bigger in every direction.
- `getEntitiesOfClass(Warden.class, box)` returns a **list** of every
  Warden inside the box - the list idea from Phase 4, and we walk it with
  a `for` loop. An empty list just means the loop runs zero times, so
  "no Wardens nearby" needs no special case.
- The `true` at the end of `increaseAngerAt` makes the Warden play its
  "I heard something" sound.

## How to check it

```bash
./gradlew test    # TickTimerTest should be green
./gradlew build
```

In the game, in a creative world: summon a Warden and a Sifter about 15
blocks apart (`/summon minecraft:warden ~ ~ ~` and use your spawn egg).
Within a few seconds the Warden should turn towards the Sifter and attack.
Wardens hit hard, so it won't last long - that's what Phase 11 is for.

`customServerAiStep` isn't something JUnit can run (it needs a live
world), so the unit test covers the one part that isn't tied to the game:
*when* to do it. Same split as every phase since Phase 4.

## Things worth knowing

- If a *player* is also standing near the Warden and has made it angrier
  than the Sifter has, the Warden goes for the player first. Wardens
  always attack whoever they're angriest at.
- 35 per pulse is a number we picked by reasoning (two pulses reach
  "agitated", three reach "angry"). Wardens also calm down slowly over
  time. If you want it to attack faster or slower, change
  `WARDEN_PROVOKE_ANGER` - this is exactly why the number is a named
  constant at the top rather than buried in the loop.

# Phase 9 — Sifters spawn in ancient cities

Companion to the Phase 9 section of [`LESSON_PLAN.md`](../LESSON_PLAN.md).

**Goal:** Sifters appear on their own in the Deep Dark, where ancient
cities are, without anyone using a spawn egg.

Minecraft decides what spawns where with **spawn lists**, one per biome
(a biome is a region type: desert, plains, Deep Dark...). Each entry says
"this mob, this often, in groups of this size". Fabric lets a mod add
entries to a biome's list. Ancient cities only ever generate in the Deep
Dark biome, so adding Sifters to the Deep Dark's list puts them in ancient
cities.

## Before we code: three small ideas

### 1. Weights

Imagine a bag with 10 red marbles and 1 blue marble. Reach in without
looking: red is ten times as likely as blue. Minecraft's spawn lists work
like that bag - every mob in the list has a **weight**, and the chance of
a given mob is its weight divided by all the weights added up. A weight of
10 for the Sifter next to a weight of 100 for another mob means the
Sifter gets picked about one time in eleven.

### 2. Refusing nonsense: exceptions

What should happen if someone writes a spawn weight of `-5`, or a group
size that goes from 4 *down to* 2? The code could carry on and quietly
produce a mob that never spawns - a bug that's very hard to notice. Better
to **stop immediately with a clear message**. In Java you do that by
**throwing an exception**:

```java
if (age < 0) {
	throw new IllegalArgumentException("age can't be negative, was " + age);
}
```

`throw` stops the code on the spot and shouts the message. Checking your
inputs at the door like this is called **validation**.

### 3. Testing that something *fails*

A test normally checks that a good input gives the right answer. For
validation we need the opposite: that a *bad* input is rejected.
`assertThrows` runs a piece of code and passes only if it throws the
exception we named:

```java
assertThrows(IllegalArgumentException.class, () -> new SpawnRule(0, 1, 2));
```

The `() -> ...` is "a little piece of code to run later" - the same arrow
you've seen in event callbacks. A test that only checks the happy path
would miss all of this.

## You type this

### 1. The rule, with validation - `src/main/java/com/sifter/spawn/SpawnRule.java`

Make a new package folder `spawn`. A record (Phase 5) can have a
"compact constructor" - code that runs before the fields are stored,
perfect for validation:

```java
package com.sifter.spawn;

// "How often, and in what group sizes, does something spawn?" - plain
// numbers, no Minecraft. The record's constructor refuses nonsense, so a
// typo in the numbers fails loudly the moment the mod starts, instead of
// quietly producing a mob that never (or always) spawns.
public record SpawnRule(int weight, int minGroupSize, int maxGroupSize) {

	// A "compact constructor": runs whenever a SpawnRule is created, before
	// the fields are stored. Throwing an exception stops the creation.
	public SpawnRule {
		if (weight <= 0) {
			throw new IllegalArgumentException("weight must be above 0, was " + weight);
		}
		if (minGroupSize < 1) {
			throw new IllegalArgumentException("minGroupSize must be at least 1, was " + minGroupSize);
		}
		if (maxGroupSize < minGroupSize) {
			throw new IllegalArgumentException(
				"maxGroupSize (" + maxGroupSize + ") can't be smaller than minGroupSize (" + minGroupSize + ")");
		}
	}
}
```

### 2. The test - `src/test/java/com/sifter/spawn/SpawnRuleTest.java`

```java
package com.sifter.spawn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpawnRuleTest {

	@Test
	void aSensibleRuleIsAccepted() {
		SpawnRule rule = new SpawnRule(10, 1, 2);

		assertEquals(10, rule.weight());
	}

	@Test
	void aWeightOfZeroIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> new SpawnRule(0, 1, 2));
	}

	@Test
	void aGroupCannotBeEmpty() {
		assertThrows(IllegalArgumentException.class, () -> new SpawnRule(10, 0, 2));
	}

	@Test
	void theMaximumCannotBeSmallerThanTheMinimum() {
		assertThrows(IllegalArgumentException.class, () -> new SpawnRule(10, 3, 2));
	}

	@Test
	void theRealAncientCityRuleIsValid() {
		// Reading the constant creates it, so a bad number would throw here.
		assertEquals(10, ModSpawns.ANCIENT_CITY.weight());
	}
}
```

Run it and check it's green. Then do the red/green exercise from the
Interlude: change `new SpawnRule(10, 3, 2)` in one test to
`new SpawnRule(10, 2, 3)`. That's a *valid* rule now, so `assertThrows`
fails ("Expected IllegalArgumentException to be thrown, but nothing was
thrown"). Put it back.

### 3. Register the spawn - `src/main/java/com/sifter/spawn/ModSpawns.java`

```java
package com.sifter.spawn;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biomes;

import com.sifter.entity.ModEntities;

// Where, and how often, Sifters appear on their own.
public class ModSpawns {

	// Ancient cities only ever generate inside the Deep Dark biome, so
	// "spawn in the Deep Dark" is the same as "spawn in ancient cities".
	// A weight of 10 is a fairly rare spawn; most animals use 8-12.
	public static final SpawnRule ANCIENT_CITY = new SpawnRule(10, 1, 2);

	public static void register() {
		BiomeModifications.addSpawn(
			BiomeSelectors.includeByKey(Biomes.DEEP_DARK),
			MobCategory.CREATURE,
			ModEntities.SIFTER,
			ANCIENT_CITY.weight(),
			ANCIENT_CITY.minGroupSize(),
			ANCIENT_CITY.maxGroupSize());
	}
}
```

`ModSpawns.ANCIENT_CITY` is read by the last test above. That's safe
because the class only touches Minecraft inside `register()`, which the
test never calls. (If you ever see an error that mentions the game isn't
started, a static field somewhere is touching a real Minecraft object.)

### 4. Call it from the entry point - `src/main/java/com/sifter/Sifter.java`

```java
import com.sifter.spawn.ModSpawns;
```

and in `onInitialize()`, under `ModItems.register();`:

```java
		ModSpawns.register();
```

### 5. Stop natural Sifters vanishing - `src/main/java/com/sifter/entity/SifterEntity.java`

Mobs that spawn on their own normally "despawn" (disappear) when you
walk far away. A Sifter has a home and a life, so we say no:

```java
	// Mobs that spawn naturally normally vanish when you wander far away
	// (that's why you don't find old zombies from hours ago). A Sifter has a
	// home and a life to get on with, so it should stay put instead.
	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}
```

Minecraft asks every mob this yes/no question; we override the answer.
It's the "Minecraft asks, we answer" shape from the trading lesson.

## How to check it

```bash
./gradlew test    # SpawnRuleTest should be green
./gradlew build
```

In a new creative world, find the Deep Dark. The quickest way:
`/locate structure minecraft:ancient_city`, then teleport near it
(`/tp @s x y z` with the coordinates it gives). Walk around the city.
Sifters should show up in ones and twos as you explore.

## Things worth knowing

- **It's the whole biome, not only the city's buildings.** Spawn lists
  belong to biomes. To spawn only inside the city's structure we would
  need a different, much fiddlier mechanism.
- **It's a trickle, not a flood.** Sifters count as "creatures", the
  same as cows. Minecraft tries creature spawns roughly every 20
  seconds near players and stops at a cap shared by all creatures. Be
  patient, and explore.
- **Wardens live in ancient cities too.** A Sifter in the middle of a city
  will annoy any Warden nearby (Phase 8), and Wardens usually win.
  That's a real and slightly sad consequence of two earlier lessons
  meeting. Phase 11 gives Sifters a protector.
- Sifters that spawn here will build a home right where they stand
  (Phase 5), including inside the city. That's by design.

# Phase 7 — A spawn egg

Companion to the Phase 7 section of [`LESSON_PLAN.md`](../LESSON_PLAN.md).

**Goal:** a "Sifter Spawn Egg" item in the creative inventory. Use it on
a block and a Sifter appears. No more typing `/summon`.

Everything so far has been about the Sifter *mob*. An egg is a different
kind of thing: an **item**. Items live in their own registry, separate
from the entity registry from Phase 1, but the idea is the same - nothing
exists until it's registered.

## Before we code: four small ideas

### 1. Hexadecimal colours

A colour on a screen is three numbers - how much red, green and blue,
each from 0 (none) to 255 (full). Programmers write them in
**hexadecimal** ("hex"), which counts in sixteens instead of tens, using
the letters A-F for ten to fifteen. Two hex digits cover 0-255 exactly,
so a colour is six digits:

```java
int red   = 0xFF0000;   // FF = 255 red, 00 green, 00 blue
int green = 0x00FF00;
int yellow = 0xFFFF00;  // red and green together make yellow
```

The `0x` at the front just means "the number that follows is in hex".
You don't need to convert anything in your head: any colour picker shows
the hex code, and you can paste it in after the `0x`.

### 2. A resource file

Not everything in a mod is Java. The look of an item in your inventory
and the *words* it shows are written in small text files called
**JSON**. JSON is just labels and values in curly braces:

```json
{
	"name": "Sifter",
	"legs": 2
}
```

These sit in the `resources` folder next to the texture you made in
Phase 3. The game finds them by their folder and file name.

### 3. A translation key

Code never says "Sifter Spawn Egg" directly. It says
`item.sifter.sifter_spawn_egg` - a **key** - and the game looks that key
up in a language file (`en_us.json`) to get the words to display. That way
somebody could add a French file later without touching the Java. If you
forget to add the words, the game shows the raw key, which is how you
know something's missing.

### 4. Reading a file inside a test

Last time we met a test that checks *code*. A test can check *files*
too. Java's `Files.readString(path)` reads a whole file into a string, and
we can then ask whether it contains what we expect.

## You type this

### 1. The item - `src/main/java/com/sifter/item/ModItems.java`

Make a new package folder `item` next to `entity`.

```java
package com.sifter.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import com.sifter.Sifter;
import com.sifter.entity.ModEntities;

// Items have a registry of their own, separate from the entity registry
// in ModEntities. Same idea: nothing exists until it's registered.
public class ModItems {

	// Colours are written as 0xRRGGBB - two hex digits each of red, green
	// and blue. The egg is painted in two colours: a background and the
	// spots on top of it.
	public static final Item SIFTER_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		ResourceLocation.fromNamespaceAndPath(Sifter.MOD_ID, "sifter_spawn_egg"),
		new SpawnEggItem(ModEntities.SIFTER, 0xE8C547, 0x0D5560, new Item.Properties())
	);

	public static void register() {
		// An event again (like Phase 0.5): when Minecraft builds the
		// "Spawn Eggs" creative tab, it calls this so we can add ours.
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS)
			.register(entries -> entries.accept(SIFTER_SPAWN_EGG));
	}
}
```

Two things to notice:

- `SpawnEggItem` already knows how to spawn an entity when used on a
  block. We only tell it *which* entity (`ModEntities.SIFTER`) and the
  two colours. All the real work is inherited - the same inheritance
  idea as Phase 1, with an item this time.
- `register()` hands Minecraft a function (the part after `->`) to run
  when it builds the creative tab. That's the event/callback idea from
  Phase 0.5 again.

### 2. Call it from the mod's entry point - `src/main/java/com/sifter/Sifter.java`

Add the import:

```java
import com.sifter.item.ModItems;
```

and, in `onInitialize()`, just after the `FabricDefaultAttributeRegistry`
line:

```java
		ModItems.register();
```

### 3. How the egg looks - `src/client/resources/assets/sifter/models/item/sifter_spawn_egg.json`

Create the folders `models/item` as needed. Vanilla already has a model
for "an egg with two colours"; we just point at it.

```json
{
	"parent": "minecraft:item/template_spawn_egg"
}
```

The two colours you gave `SpawnEggItem` are applied to it automatically.

### 4. The words - `src/client/resources/assets/sifter/lang/en_us.json`

Create the folder `lang`. This also names the Sifter itself, which has so
far been showing up as `entity.sifter.sifter` (see it in the death
message or when you point at one).

```json
{
	"entity.sifter.sifter": "Sifter",
	"entity.sifter.sifter_guardian": "Sifter Guardian",
	"item.sifter.sifter_spawn_egg": "Sifter Spawn Egg"
}
```

(In the phase 11 lesson we add a third line to this file.)

### 5. The test - `src/test/java/com/sifter/resources/LangFileTest.java`

```java
package com.sifter.resources;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

// Checks the English names file has an entry for everything we add. It
// reads the file as plain text, so no game is needed. The keys are typed
// out here on purpose: referencing ModItems would load real Items, which
// (see Phase 6) can't happen in a test.
class LangFileTest {

	private static final Path LANG_FILE = Path.of("src/client/resources/assets/sifter/lang/en_us.json");

	@Test
	void theSifterHasAName() throws IOException {
		assertHasKey("entity.sifter.sifter");
	}

	@Test
	void theGuardianHasAName() throws IOException {
		assertHasKey("entity.sifter.sifter_guardian");
	}

	@Test
	void theSpawnEggHasAName() throws IOException {
		assertHasKey("item.sifter.sifter_spawn_egg");
	}

	private static void assertHasKey(String key) throws IOException {
		String text = Files.readString(LANG_FILE);
		assertTrue(text.contains("\"" + key + "\""), "en_us.json is missing " + key);
	}
}
```

Why type the keys out again instead of using something from `ModItems`?
Same reason as Phase 6: touching `ModItems` makes Minecraft create a real
`Item`, which only works with the game running. Plain strings are safe.

## How to check it

```bash
./gradlew test    # LangFileTest should be green
./gradlew build
```

Now **break it on purpose**: rename a key in `en_us.json` (say, change
`sifter_spawn_egg` to `sifter_spawn_eg`), run `./gradlew test`, and read
the failure message. That's the test catching a typo the game would only
have shown you as ugly text. Put it back and watch it go green.

In the game: open a creative world, open the inventory, search "sifter".
You should see a yellow egg with teal spots called **Sifter Spawn Egg**
in the Spawn Eggs tab. Right-click a block with it.

## Try tweaking it

- Change the two colours. Which looks more like a Sifter next to a
  sculk block?
- Spawn eggs can also be put in a dispenser - try it.

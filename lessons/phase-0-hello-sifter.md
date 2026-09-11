# Phase 0 — Hello, Sifter

Companion to the Phase 0 section of [`LESSON_PLAN.md`](../LESSON_PLAN.md).

## You type this

Create `src/main/java/com/sifter/Sifter.java`:

```java
package com.sifter;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This is the "entrypoint" of our mod - the very first bit of our own code
// that Minecraft runs when it loads. Fabric knows to run it because we told
// it to in fabric.mod.json ("entrypoints" -> "main").
public class Sifter implements ModInitializer {

	public static final String MOD_ID = "sifter";

	// A logger writes lines to the game's console/log file. It's the
	// simplest way to prove our code actually ran.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Sifter mod loaded!");
	}
}
```

And `src/main/resources/fabric.mod.json` - this one isn't Java, it's how we
tell Fabric our mod exists at all, and which class to run:

```json
{
	"schemaVersion": 1,
	"id": "sifter",
	"version": "${version}",
	"name": "Sifter",
	"description": "Adds the Sifter, a yellow mob that mines skulk and trades with the player.",
	"authors": [
		"You!"
	],
	"license": "CC0-1.0",
	"environment": "*",
	"entrypoints": {
		"main": [
			"com.sifter.Sifter"
		]
	},
	"depends": {
		"fabricloader": ">=0.19.5",
		"minecraft": "~1.21.1",
		"java": ">=21",
		"fabric-api": "*"
	}
}
```

## How to check it

```bash
./gradlew build
```

Launch Minecraft with the mod in place (see `SETUP.md`) and check:

1. Main menu → **Mods** button lists "Sifter".
2. `.minecraft/logs/latest.log` contains the line `Sifter mod loaded!`.

## Stretch idea

Change the message in `LOGGER.info(...)` to something else, rebuild, and
check the log again - proof that *you* control what it says, not just that
it happened to work once.

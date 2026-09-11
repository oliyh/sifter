# Phase 0.5 — Say hello in chat

Companion to the Phase 0.5 section of [`LESSON_PLAN.md`](../LESSON_PLAN.md).

## You type this

Add to `src/main/java/com/sifter/Sifter.java` - two new imports at the
top:

```java
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.network.chat.Component;
```

And inside `onInitialize()`, after the `LOGGER.info(...)` line:

```java
		// Register a callback. We don't call this code ourselves -
		// Minecraft calls it automatically, every time a player finishes
		// joining a world.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			handler.player.sendSystemMessage(Component.literal("The Sifter mod says hello!"));
		});
```

The whole method should now look like:

```java
	@Override
	public void onInitialize() {
		LOGGER.info("Sifter mod loaded!");

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			handler.player.sendSystemMessage(Component.literal("The Sifter mod says hello!"));
		});
	}
```

## How to check it

```bash
./gradlew build
```

Join your world. You should see *"The Sifter mod says hello!"* appear in
chat as you spawn in.

## Notes for the "we read this together" part

- `handler` represents the connection to one specific player who just
  joined. `handler.player` is that player.
- `Component.literal("...")` turns plain text into the kind of object
  Minecraft's chat system understands. `Component` can also do fancier
  things later (colour, click actions) - we're using the simplest form.

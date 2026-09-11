# Phase 1 — The Sifter exists (two yellow blocks)

Companion to the Phase 1 section of [`LESSON_PLAN.md`](../LESSON_PLAN.md).

This phase touches more files than Phase 0 did, because a mob has two
separate halves: **what it is** (health, AI, exists in the world - runs
everywhere, including on a server with no screen) and **how it's drawn**
(runs only where there's a screen). That split is why some of these files
live under `src/main` and others under `src/client`.

For this first version we borrow the vanilla pig's shape and tint it
yellow, so we can focus on "does a custom mob exist at all" before
worrying about a real design (that's Phase 3).

## You type this

### 1. The entity itself - `src/main/java/com/sifter/entity/SifterEntity.java`

```java
package com.sifter.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// A SifterEntity is one particular Sifter that exists in the world - the
// "object" made from this "class" every time one spawns.
//
// Extending PathfinderMob means we inherit a huge amount for free: gravity,
// walking, health, taking damage. We only write the bits that make a
// Sifter different from every other mob.
public class SifterEntity extends PathfinderMob {

	// The constructor: code that runs once, the moment a new Sifter is
	// created. Minecraft calls this itself - we never call "new
	// SifterEntity(...)" directly.
	public SifterEntity(EntityType<? extends SifterEntity> type, Level level) {
		super(type, level);
	}

	// Every mob type must declare its starting stats. 20 health = 10 hearts,
	// same as a player. 0.25 movement speed matches a pig.
	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes()
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.MOVEMENT_SPEED, 0.25);
	}

	// Called once when the Sifter spawns. We give it a short list of
	// "goals" - behaviours it's allowed to choose between. Lower priority
	// numbers run first.
	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
	}
}
```

### 2. Registering it - `src/main/java/com/sifter/entity/ModEntities.java`

```java
package com.sifter.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import com.sifter.Sifter;

// A registry is Minecraft's phone book of every entity/item/block type
// that exists, keyed by name. Something has to be registered here before
// it can ever spawn.
public class ModEntities {

	public static final EntityType<SifterEntity> SIFTER = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(Sifter.MOD_ID, "sifter"),
		EntityType.Builder.of(SifterEntity::new, MobCategory.CREATURE)
			.sized(0.9F, 1.3F)
			.build("sifter")
	);
}
```

### 3. Wire it up in `src/main/java/com/sifter/Sifter.java`

Add these imports:

```java
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import com.sifter.entity.ModEntities;
import com.sifter.entity.SifterEntity;
```

And this line inside `onInitialize()` (order relative to the Phase 0.5
code doesn't matter - put it before or after):

```java
		// Tell Minecraft the Sifter exists, and what its starting stats
		// are. Referencing ModEntities.SIFTER here is also what triggers
		// its registration (see ModEntities.java).
		FabricDefaultAttributeRegistry.register(ModEntities.SIFTER, SifterEntity.createAttributes());
```

### 4. The renderer (client-only) - `src/client/java/com/sifter/client/SifterRenderer.java`

Note the folder: `src/client/java`, not `src/main/java`. This code never
runs on a server.

```java
package com.sifter.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import com.sifter.entity.SifterEntity;

// Placeholder look for Phase 1: borrow the vanilla pig's shape, but tint
// every pixel yellow. We'll design the Sifter's real look in Phase 3.
public class SifterRenderer extends MobRenderer<SifterEntity, PigModel<SifterEntity>> {

	private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/pig/pig.png");

	public SifterRenderer(EntityRendererProvider.Context context) {
		super(context, new PigModel<>(context.bakeLayer(ModelLayers.PIG)), 0.5F);
	}

	@Override
	public ResourceLocation getTextureLocation(SifterEntity entity) {
		return TEXTURE;
	}

	@Override
	public void render(SifterEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
			MultiBufferSource buffer, int packedLight) {
		// Multiply every pixel we draw by this colour before drawing it.
		// (1, 1, 1) would mean "no change"; we push red and green down and
		// leave blue at 0, which turns the pig's pinks and browns yellow.
		RenderSystem.setShaderColor(1.0F, 0.85F, 0.0F, 1.0F);
		super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
```

### 5. The client entrypoint - `src/client/java/com/sifter/client/SifterClient.java`

A mod can have two entrypoints: `Sifter` (runs everywhere, already
wired up) and this one (runs only on the client).

```java
package com.sifter.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import com.sifter.entity.ModEntities;

public class SifterClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.SIFTER, SifterRenderer::new);
	}
}
```

### 6. Tell Fabric about the new entrypoint - `src/main/resources/fabric.mod.json`

Add `"client"` alongside the existing `"main"` entry:

```json
	"entrypoints": {
		"main": [
			"com.sifter.Sifter"
		],
		"client": [
			"com.sifter.client.SifterClient"
		]
	},
```

## How to check it

```bash
./gradlew build
```

In-game, run:

```
/summon sifter:sifter
```

A yellow, pig-shaped mob should appear.

## Notes for the "we read this together" part

- `PathfinderMob`, `MobRenderer`, `EntityType.Builder` etc. are all
  Minecraft's own classes - we're not writing a mob from scratch, we're
  plugging into a huge amount of existing machinery.
- `context.bakeLayer(ModelLayers.PIG)` is Mojang's own pig shape - we
  haven't designed anything yet, just borrowed something that already
  moves and animates correctly.

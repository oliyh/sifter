# Phase 11 — Sifter guardians

Companion to the Phase 11 section of [`LESSON_PLAN.md`](../LESSON_PLAN.md).

**Goal:** a big, glowing blue blaze-like creature - the Sifter Guardian -
that stands near a Sifter's home, fights anything hostile that comes
close, and goes after whoever hurts a Sifter. Think of Iron Golems
protecting a village, but for Sifters.

Is there an Iron Golem class to build on? Yes: `IronGolem` in Minecraft's
code (it extends `AbstractGolem`). Extending it gives us, for free: the
strong punch that throws mobs in the air, the high health, the
knockback resistance, the cracks that appear as it's damaged, and repair
with iron ingots. We replace only what makes it a *village* golem (who it
defends) and how it *looks*.

## Before we code: three ideas

### 1. Inheritance, properly

In Phase 1 you wrote `SifterEntity extends PathfinderMob` and got walking
for free. Here we go one step further: a class can **override** a method
it inherits, which *replaces* the parent's version with its own:

```java
class Animal {
	String sound() { return "..."; }
}

class Dog extends Animal {
	@Override
	String sound() { return "woof"; }
}
```

`@Override` is a promise: "this method has the same name as one in my
parent, and I mean to replace it." If you misspell the name, Java
refuses to compile instead of quietly adding a new, unused method. And
if you *don't* want to lose the parent's behaviour you call
`super.sound()` inside the override. `IronGolem.registerGoals()` is full
of village behaviour we don't want, so ours overrides it and
deliberately never calls `super`.

### 2. Two lists: what to do, and who to attack

Mobs have two goal lists. `goalSelector` is *what to do* (walk, look
around, punch). `targetSelector` is *who to go after*. A **target goal**
only picks the victim; once a victim is chosen, the ordinary goals (here,
`MeleeAttackGoal`) do the walking up and punching.

### 3. Separate the dice from the decision

"Sometimes a guardian appears" means a random number. But random numbers
are awkward to test, because you can't control them. The trick:
**roll the dice outside the function, and hand the result in as a
parameter**. The function then has no randomness inside it, and a test can
pass in any roll it likes - 0.1, 0.9, exactly 0.5 - to check each case.
Compare this with the seeded `Random(42)` from Phase 6, which fixed the
dice; this goes one better and removes them from the function entirely.

## You type this

### 1. The decision - `src/main/java/com/sifter/entity/GuardianSpawning.java`

```java
package com.sifter.entity;

// The "should a guardian appear?" decision, as a pure function. Note that
// it doesn't roll any dice itself: whoever calls it rolls the dice and
// hands in the result (a number from 0.0 up to, but not including, 1.0).
// That keeps this method completely predictable - same inputs, same
// answer - so the tests can try any roll they like, instead of hoping a
// random number happens to land somewhere useful.
public class GuardianSpawning {

	// How likely a newly built home is to get a guardian: 0.0 is never,
	// 1.0 is always, 0.5 is a coin flip.
	public static final double CHANCE_PER_HOME = 0.5;

	// One guardian is plenty for one little home.
	public static final int MAX_GUARDIANS_NEARBY = 1;

	private GuardianSpawning() {
	}

	public static boolean shouldSpawn(double roll, double chance, int guardiansNearby) {
		if (guardiansNearby >= MAX_GUARDIANS_NEARBY) {
			return false;
		}
		// With a chance of 0.5, rolls below 0.5 (half of all the possible
		// rolls) count as a success.
		return roll < chance;
	}
}
```

### 2. The test - `src/test/java/com/sifter/entity/GuardianSpawningTest.java`

```java
package com.sifter.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuardianSpawningTest {

	@Test
	void aLowRollSpawnsAGuardian() {
		assertTrue(GuardianSpawning.shouldSpawn(0.1, 0.5, 0));
	}

	@Test
	void aHighRollDoesNot() {
		assertFalse(GuardianSpawning.shouldSpawn(0.9, 0.5, 0));
	}

	@Test
	void aRollExactlyOnTheChanceDoesNot() {
		// Rolls go from 0.0 up to *just under* 1.0, so "below the chance" is
		// the right test. If it were "at or below", a chance of 0.0 would
		// still spawn one whenever the roll was exactly 0.0.
		assertFalse(GuardianSpawning.shouldSpawn(0.5, 0.5, 0));
	}

	@Test
	void aChanceOfZeroNeverSpawns() {
		assertFalse(GuardianSpawning.shouldSpawn(0.0, 0.0, 0));
	}

	@Test
	void aChanceOfOneAlwaysSpawnsWhenTheHomeIsUnguarded() {
		assertTrue(GuardianSpawning.shouldSpawn(0.999999, 1.0, 0));
	}

	@Test
	void neverSpawnsAnotherWhenAGuardianIsAlreadyNearby() {
		assertFalse(GuardianSpawning.shouldSpawn(0.0, 1.0, GuardianSpawning.MAX_GUARDIANS_NEARBY));
	}
}
```

The "exactly on the chance" test is the kind that catches a classic
mistake: writing `<=` where `<` was meant. Try changing `<` to `<=`
in `GuardianSpawning` and watch exactly that test (and
`aChanceOfZeroNeverSpawns`) go red. Then change it back.

### 3. Who to defend - `src/main/java/com/sifter/entity/DefendSiftersGoal.java`

```java
package com.sifter.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;

// A "target goal" decides who a mob wants to attack (as opposed to the
// other goals, which decide what it does). This one says: if any Sifter
// nearby has just been hurt, go after whoever hurt it.
public class DefendSiftersGoal extends TargetGoal {

	private static final double LOOK_RANGE = 16.0;

	private final SifterGuardianEntity guardian;
	private LivingEntity attacker;

	public DefendSiftersGoal(SifterGuardianEntity guardian) {
		// "false" = we don't need a clear line of sight to the attacker.
		super(guardian, false);
		this.guardian = guardian;
	}

	@Override
	public boolean canUse() {
		AABB area = guardian.getBoundingBox().inflate(LOOK_RANGE);
		for (SifterEntity sifter : guardian.level().getEntitiesOfClass(SifterEntity.class, area)) {
			LivingEntity culprit = sifter.getLastHurtByMob();
			if (culprit != null && canAttack(culprit, TargetingConditions.DEFAULT)) {
				attacker = culprit;
				return true;
			}
		}
		return false;
	}

	@Override
	public void start() {
		guardian.setTarget(attacker);
		super.start();
	}
}
```

`getLastHurtByMob()` is something every creature remembers for a few
seconds: the last mob that damaged it. If any Sifter nearby has one, the
guardian picks that mob as its target. (Notice `null` from Phase 10: the
answer is "nothing" when the Sifter hasn't been hurt.)

### 4. The guardian itself - `src/main/java/com/sifter/entity/SifterGuardianEntity.java`

```java
package com.sifter.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// A guardian is an iron golem in everything but looks and loyalty. By
// extending IronGolem we inherit the strong punch (and throwing things
// into the air), the knockback resistance, the cracking as it takes
// damage, and being repaired with iron ingots - without writing any of it.
public class SifterGuardianEntity extends IronGolem {

	public SifterGuardianEntity(EntityType<? extends SifterGuardianEntity> type, Level level) {
		super(type, level);
	}

	// Start from the iron golem's stats, then change just the bits we want
	// different. (Adding an attribute that's already there replaces it.)
	public static AttributeSupplier.Builder createGuardianAttributes() {
		return IronGolem.createAttributes()
			.add(Attributes.MAX_HEALTH, 80.0);
	}

	// IronGolem already has a registerGoals(), full of village-defending
	// behaviour that means nothing to us. Writing our own with the same
	// name *replaces* it - that's what @Override is for. Notice we never
	// call super.registerGoals(): we want none of the old ones.
	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.9, 32.0F));
		this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

		// Who to attack, in priority order: whoever hurt a Sifter, whoever
		// hurt us, then any hostile mob that wanders close (but not
		// creepers - same as an iron golem, they'd blow us up).
		this.targetSelector.addGoal(1, new DefendSiftersGoal(this));
		this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 5, false, false,
			target -> target instanceof Enemy && !(target instanceof Creeper)));
	}
}
```

Wardens are `Enemy` too, so a guardian will take on a Warden that wanders
into range. It will lose (a Warden has 500 health), but it is brave.

### 5. Register the entity type - `src/main/java/com/sifter/entity/ModEntities.java`

Add under the existing `SIFTER` constant, inside the class:

```java
	// MISC is the category iron golems use too: it isn't an animal or a
	// monster, so it never counts towards their spawn limits. fireImmune()
	// is what stops blazes burning in lava, and now our guardian too.
	public static final EntityType<SifterGuardianEntity> SIFTER_GUARDIAN = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(Sifter.MOD_ID, "sifter_guardian"),
		EntityType.Builder.of(SifterGuardianEntity::new, MobCategory.MISC)
			.sized(1.0F, 2.9F)
			.fireImmune()
			.clientTrackingRange(10)
			.build("sifter_guardian")
	);
```

and in `Sifter.java`, add the import
`import com.sifter.entity.SifterGuardianEntity;` and, under the Sifter's
attributes line:

```java
		FabricDefaultAttributeRegistry.register(ModEntities.SIFTER_GUARDIAN, SifterGuardianEntity.createGuardianAttributes());
```

### 6. Make a guardian appear - `SifterEntity.java` and `BuildHomeGoal.java`

In `SifterEntity.java`, add imports:

```java
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
```

a constant:

```java
	// How far from this Sifter to count guardians that already exist.
	private static final int GUARDIAN_COUNT_RANGE = 32;
```

and a method:

```java
	// Called by BuildHomeGoal just after a home goes up. Sometimes (see
	// GuardianSpawning) a guardian turns up to watch over it.
	public void maybeSummonGuardian() {
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		AABB nearby = this.getBoundingBox().inflate(GUARDIAN_COUNT_RANGE);
		int guardiansNearby = serverLevel.getEntitiesOfClass(SifterGuardianEntity.class, nearby).size();

		if (GuardianSpawning.shouldSpawn(this.getRandom().nextDouble(), GuardianSpawning.CHANCE_PER_HOME, guardiansNearby)) {
			ModEntities.SIFTER_GUARDIAN.spawn(serverLevel, this.blockPosition(), MobSpawnType.MOB_SUMMONED);
		}
	}
```

`this.level() instanceof ServerLevel serverLevel` asks "is the world I'm
in the server's one?" and, if so, hands it to you under the new name
`serverLevel` already converted to that type. This is where *the dice are
rolled*: `getRandom().nextDouble()` produces the 0.0-to-just-under-1.0
number that `shouldSpawn` takes.

In `BuildHomeGoal.java`, add one line at the end of `start()`:

```java
		sifter.maybeSummonGuardian();
```

### 7. What it looks like - `src/client/java/com/sifter/client/SifterGuardianRenderer.java`

Minecraft already has a Blaze model. Rather than design a new shape, we
reuse it, and give it our own colours and a bigger size - the same idea
as the pig-with-a-tint from Phase 1.

```java
package com.sifter.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.BlazeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import com.sifter.Sifter;
import com.sifter.entity.SifterGuardianEntity;

// The guardian borrows the vanilla Blaze's model (the spinning rods
// around a head) but paints it with our own blue, sculk-coloured texture
// and draws it bigger. No new model code at all - the same idea as the
// pig-with-a-tint we started the Sifter with in Phase 1.
public class SifterGuardianRenderer extends MobRenderer<SifterGuardianEntity, BlazeModel<SifterGuardianEntity>> {

	private static final ResourceLocation TEXTURE =
		ResourceLocation.fromNamespaceAndPath(Sifter.MOD_ID, "textures/entity/sifter_guardian.png");

	// 1.0 would be exactly blaze-sized.
	private static final float SIZE = 1.6F;

	public SifterGuardianRenderer(EntityRendererProvider.Context context) {
		super(context, new BlazeModel<>(context.bakeLayer(ModelLayers.BLAZE)), 0.8F);
	}

	@Override
	public ResourceLocation getTextureLocation(SifterGuardianEntity entity) {
		return TEXTURE;
	}

	// Called just before drawing: a chance to resize the whole model.
	@Override
	protected void scale(SifterGuardianEntity entity, PoseStack poseStack, float partialTickTime) {
		poseStack.scale(SIZE, SIZE, SIZE);
	}

	// Always draw at full brightness, like a blaze - it's meant to glow.
	@Override
	protected int getBlockLightLevel(SifterGuardianEntity entity, BlockPos pos) {
		return 15;
	}
}
```

Register it in `SifterClient.java`, under the Sifter's line:

```java
		EntityRendererRegistry.register(ModEntities.SIFTER_GUARDIAN, SifterGuardianRenderer::new);
```

### 8. The texture - `src/client/resources/assets/sifter/textures/entity/sifter_guardian.png`

It must be **64 wide by 32 tall** (the Blaze's layout: head in the top
left, the rods below). Any image editor will do - paint it blue and
teal with a few bright cyan dots like sculk's glow. If you'd rather not
open an editor, this small Python script paints a plain starting point
(save it as `make_guardian_texture.py` *outside* the project, run it from
the project folder with `python3 make_guardian_texture.py`):

```python
# Paints a 64x32 sculk-blue texture for the guardian. Uses only Python's
# standard library. Layout matches the vanilla Blaze: the head is in the
# top-left, the rods are in the rows below it.
import random
import struct
import zlib

WIDTH, HEIGHT = 64, 32
random.seed(5)

pixels = []
for y in range(HEIGHT):
	row = []
	for x in range(WIDTH):
		shade = random.randint(-14, 14)
		if random.random() < 0.06:
			# a few bright spots, like sculk's glowing dots
			row.append((90, 230, 235, 255))
		else:
			row.append((14 + shade, 70 + shade * 2, 95 + shade * 2, 255))
	pixels.append(row)

# eyes on the front of the head (the head's front face starts at 8,8)
for x, y in [(10, 12), (11, 12), (14, 12), (15, 12)]:
	pixels[y][x] = (190, 255, 255, 255)

def chunk(kind, data):
	body = kind + data
	return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body))

raw = b"".join(b"\x00" + bytes(c for px in row for c in px) for row in pixels)
png = (b"\x89PNG\r\n\x1a\n"
	+ chunk(b"IHDR", struct.pack(">IIBBBBB", WIDTH, HEIGHT, 8, 6, 0, 0, 0))
	+ chunk(b"IDAT", zlib.compress(raw))
	+ chunk(b"IEND", b""))

with open("src/client/resources/assets/sifter/textures/entity/sifter_guardian.png", "wb") as f:
	f.write(png)
```

Replacing it with something you've painted by hand, later, needs no code
changes at all.

### 9. Its name - `src/client/resources/assets/sifter/lang/en_us.json`

Add a line (mind the commas between lines):

```json
	"entity.sifter.sifter_guardian": "Sifter Guardian",
```

and a matching test in `LangFileTest.java`:

```java
	@Test
	void theGuardianHasAName() throws IOException {
		assertHasKey("entity.sifter.sifter_guardian");
	}
```

## How to check it

```bash
./gradlew test    # GuardianSpawningTest and LangFileTest should be green
./gradlew build
```

In the game:

1. `/summon sifter:sifter_guardian` - a big glowing blue blaze shape.
   Check the size and brightness; edit the texture and `SIZE` until you
   like it.
2. `/summon minecraft:zombie ~5 ~ ~` near it. It should walk over and
   punch it into the air.
3. Spawn a Sifter near sculk (as in Phase 10) a few times. About half the
   time, a guardian should appear with its home.
4. Hit a Sifter yourself, in creative or survival: the guardian should
   come for you. (Be ready to run, or use creative.)
5. Phase 8 payoff: bring a Warden near a Sifter *and* a guardian.

## Things worth knowing

- The guardian also accepts iron ingots for repair, like any iron golem.
  A good small project: override `mobInteract` so it wants an Echo Shard
  instead.
- It keeps the iron golem's iron-clanking sounds. Overriding
  `getHurtSound` and `getDeathSound` (look at how `SifterEntity` picks a
  trade sound) is an easy way to make it sound right.

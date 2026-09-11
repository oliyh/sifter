# Phase 6 — Trading with the player

Companion to the Phase 6 (Trading) section of [`LESSON_PLAN.md`](../LESSON_PLAN.md).

Trading is the most involved phase, because Minecraft's trading system
(the same one villagers use) wants an entity to implement two interfaces
- `Npc` and `Merchant` - and `Merchant` alone asks for nine methods. Most
of them are short, mechanical bookkeeping; the interesting part is
`getOffers()`.

## You type this

### 1. The trade data, kept separate from real Items - `src/main/java/com/sifter/entity/TradePricing.java`

Earlier phases used `BlockPos` freely in tests because it's completely
safe outside a running game. Real Minecraft `Item`s are **not** - if you
try to reference `Items.EMERALD` from a test, Gradle fails with an error
before the test even runs, because items only get created once the real
game boots up. So the *testable* part of trading has to be plain numbers
instead of real items:

```java
package com.sifter.entity;

import java.util.List;
import java.util.random.RandomGenerator;

// The "which trade, and how much does it cost" decision, kept separate
// from real Items/ItemStacks on purpose: creating an actual Item needs
// the live game running, but plain numbers don't need anything running
// at all.
public class TradePricing {

	private TradePricing() {
	}

	public record Offer(int emeraldCost, int maxUses) {
	}

	public static final List<Offer> OFFERS = List.of(
		new Offer(3, 12),
		new Offer(5, 6)
	);

	public static Offer choose(RandomGenerator random) {
		return OFFERS.get(random.nextInt(OFFERS.size()));
	}
}
```

### 2. The test - `src/test/java/com/sifter/entity/TradePricingTest.java`

```java
package com.sifter.entity;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradePricingTest {

	@Test
	void everyOfferHasAPositivePrice() {
		for (TradePricing.Offer offer : TradePricing.OFFERS) {
			assertTrue(offer.emeraldCost() > 0, "found a trade with a price of " + offer.emeraldCost());
		}
	}

	@Test
	void theSameRandomSeedAlwaysPicksTheSameOffer() {
		TradePricing.Offer first = TradePricing.choose(new Random(42));
		TradePricing.Offer second = TradePricing.choose(new Random(42));

		assertEquals(first, second);
	}
}
```

The second test is worth pausing on: `Random(42)` with the same seed
number always produces the same sequence of "random" numbers. That's
what makes randomness testable at all - without a fixed seed, you
couldn't write a test that expects one specific answer.

### 3. Make `SifterEntity` a trader

This is a bigger change to `src/main/java/com/sifter/entity/SifterEntity.java`.
Three things change: the class declaration, some new fields, and a chunk
of new methods.

**Imports to add:**

```java
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
```

**Class declaration** - add the two interfaces:

```java
public class SifterEntity extends PathfinderMob implements Npc, Merchant {
```

**New fields**, alongside the constructor:

```java
	// Merchant requires us to keep track of these two bits of state
	// ourselves - who's currently trading with us, and our trade list.
	private Player tradingPlayer;
	private final MerchantOffers offers = new MerchantOffers();
	private int villagerXp;
```

**Right-click handling** - add this method (anywhere in the class):

```java
	// Right-click handling: if a player (not a spectator) right-clicks us,
	// open the trading screen instead of the usual "attack" interaction.
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!this.level().isClientSide && !player.isSpectator()) {
			this.setTradingPlayer(player);
			this.openTradingScreen(player, this.getDisplayName(), 1);
		}
		return InteractionResult.sidedSuccess(this.level().isClientSide);
	}
```

**The `Merchant` interface implementation** - nine methods. `getOffers()`
is the one that actually matters; the rest are short and mostly
mechanical:

```java
	// --- Merchant interface: the bits Minecraft needs to run a trade ---

	@Override
	public void setTradingPlayer(Player player) {
		this.tradingPlayer = player;
	}

	@Override
	public Player getTradingPlayer() {
		return this.tradingPlayer;
	}

	@Override
	public MerchantOffers getOffers() {
		if (this.offers.isEmpty()) {
			// TradePricing decides *what* the trades are (tested in
			// TradePricingTest). Here we turn that plain data into the
			// real Items a trade needs - which only works with the game
			// actually running.
			this.offers.add(new MerchantOffer(
				new ItemCost(Items.EMERALD, TradePricing.OFFERS.get(0).emeraldCost()),
				new ItemStack(Items.ECHO_SHARD),
				TradePricing.OFFERS.get(0).maxUses(),
				5,
				0.05F));
			this.offers.add(new MerchantOffer(
				new ItemCost(Items.EMERALD, TradePricing.OFFERS.get(1).emeraldCost()),
				new ItemStack(Items.SCULK_CATALYST),
				TradePricing.OFFERS.get(1).maxUses(),
				10,
				0.05F));
		}
		return this.offers;
	}

	@Override
	public void overrideOffers(MerchantOffers offers) {
		this.offers.clear();
		this.offers.addAll(offers);
	}

	@Override
	public void notifyTrade(MerchantOffer offer) {
		offer.increaseUses();
		this.playSound(this.getNotifyTradeSound(), 1.0F, 1.0F);
	}

	@Override
	public void notifyTradeUpdated(ItemStack stack) {
		// Nothing extra to do when the player is just holding an item up
		// to a trade slot - only a completed trade (notifyTrade) matters
		// to us.
	}

	@Override
	public int getVillagerXp() {
		return this.villagerXp;
	}

	@Override
	public void overrideXp(int xp) {
		this.villagerXp = xp;
	}

	@Override
	public boolean showProgressBar() {
		return true;
	}

	@Override
	public SoundEvent getNotifyTradeSound() {
		return SoundEvents.VILLAGER_YES;
	}

	@Override
	public boolean isClientSide() {
		return this.level().isClientSide();
	}
```

## How to check it

```bash
./gradlew test    # TradePricingTest should be green
./gradlew build
```

In-game: give yourself some emeralds (`/give @s emerald 8`), right-click
a Sifter, and the trading screen should open. Trade 3 emeralds for an
Echo Shard.

## Notes for the "we read this together" part

- `Npc` has no methods to implement at all - it's a "marker interface",
  just a label Minecraft checks for (e.g. "don't let zombies target
  this"). Not every interface asks something of you.
- Every method here starting with `@Override` is Minecraft *asking* our
  class a question ("who's trading with you right now?", "what are your
  offers?") rather than us calling out to Minecraft. That's the same
  event/callback shape from Phase 0.5, just with more questions being
  asked.

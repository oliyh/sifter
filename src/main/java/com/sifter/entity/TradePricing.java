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
		new Offer(1, 20), // sculk block
		new Offer(4, 5),  // sculk catalyst
        new Offer(3, 7)   // sculk vein
	);

	public static Offer choose(RandomGenerator random) {
		return OFFERS.get(random.nextInt(OFFERS.size()));
	}
}
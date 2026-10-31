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
package com.sifter.spawn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpawnRuleTest {

	@Test
	void aSensibleRuleIsAccepted() {
		SpawnRule rule = new SpawnRule(10, 3, 7);

		assertEquals(10, rule.weight());
        assertEquals(3, rule.minGroupSize());
        assertEquals(7, rule.maxGroupSize());
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
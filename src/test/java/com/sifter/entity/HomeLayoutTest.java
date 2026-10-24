package com.sifter.entity;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeLayoutTest {

	@Test
	void theTwoBedPartsAreNextToEachOther() {
		assertEquals(1, HomeLayout.BED_FOOT.distManhattan(HomeLayout.BED_HEAD));
	}

	@Test
	void everyPartHasItsOwnSpot() {
		// If two parts ever shared a position, this set would be smaller
		// than the list - a bug a human eye could easily miss.
		assertEquals(HomeLayout.allOffsets().size(), Set.copyOf(HomeLayout.allOffsets()).size());
	}

	@Test
	void theSaplingSitsDirectlyAboveTheFurnace() {
		assertEquals(HomeLayout.FURNACE.above(), HomeLayout.POTTED_SAPLING);
	}
}
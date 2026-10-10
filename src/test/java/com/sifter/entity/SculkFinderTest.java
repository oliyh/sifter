package com.sifter.entity;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SculkFinderTest {

	@Test
	void picksTheClosestCandidate() {
		BlockPos origin = new BlockPos(0, 0, 0);
		BlockPos near = new BlockPos(2, 0, 0);
		BlockPos far = new BlockPos(5, 0, 0);

		Optional<BlockPos> result = SculkFinder.nearest(origin, List.of(far, near), 30);

		assertEquals(Optional.of(near), result);
	}

	@Test
	void ignoresCandidatesOutsideRange() {
		BlockPos origin = new BlockPos(0, 0, 0);
		BlockPos tooFar = new BlockPos(100, 0, 0);

		Optional<BlockPos> result = SculkFinder.nearest(origin, List.of(tooFar), 30);

		assertTrue(result.isEmpty());
	}

	@Test
	void handlesAnEmptyListWithoutCrashing() {
		BlockPos origin = new BlockPos(0, 0, 0);

		Optional<BlockPos> result = SculkFinder.nearest(origin, List.of(), 10);

		assertTrue(result.isEmpty());
	}
}
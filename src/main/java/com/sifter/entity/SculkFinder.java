package com.sifter.entity;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Optional;

// Pure logic, pulled out of the goal that actually uses it: given a
// starting point and a list of candidate positions, which one (if any) is
// the closest within range? No world, no blocks, no entities - just
// coordinates - so we can test it directly.
public class SculkFinder {

	private SculkFinder() {
	}

	public static Optional<BlockPos> nearest(BlockPos origin, List<BlockPos> candidates, int range) {
		BlockPos best = null;
		double bestDistanceSquared = Double.MAX_VALUE;
		double rangeSquared = (double) range * range;

		for (BlockPos candidate : candidates) {
			double distanceSquared = origin.distSqr(candidate);
			if (distanceSquared > rangeSquared) {
				continue;
			}
			if (distanceSquared < bestDistanceSquared) {
				bestDistanceSquared = distanceSquared;
				best = candidate;
			}
		}

		return Optional.ofNullable(best);
	}
}
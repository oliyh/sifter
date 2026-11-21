package com.sifter.spawn;

// "How often, and in what group sizes, does something spawn?" - plain
// numbers, no Minecraft. The record's constructor refuses nonsense, so a
// typo in the numbers fails loudly the moment the mod starts, instead of
// quietly producing a mob that never (or always) spawns.
public record SpawnRule(int weight, int minGroupSize, int maxGroupSize) {

	// A "compact constructor": runs whenever a SpawnRule is created, before
	// the fields are stored. Throwing an exception stops the creation.
	public SpawnRule {
		if (weight <= 0) {
			throw new IllegalArgumentException("weight must be above 0, was " + weight);
		}
		if (minGroupSize < 1) {
			throw new IllegalArgumentException("minGroupSize must be at least 1, was " + minGroupSize);
		}
		if (maxGroupSize < minGroupSize) {
			throw new IllegalArgumentException(
				"maxGroupSize (" + maxGroupSize + ") can't be smaller than minGroupSize (" + minGroupSize + ")");
		}
	}
}
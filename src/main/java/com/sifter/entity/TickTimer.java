package com.sifter.entity;

// "Is it time yet?" - pure arithmetic, pulled out so we can test it.
// Minecraft counts ticks upwards forever (20 per second); this answers
// "should the thing I only do every N ticks happen on this tick?"
public class TickTimer {

	private TickTimer() {
	}

	// % is the "remainder" operator: 45 % 20 is 5, because 20 goes into 45
	// twice (40) with 5 left over. The remainder is 0 exactly once per
	// interval - on tick 0, 20, 40, 60...
	public static boolean isDue(int tick, int interval) {
		return tick % interval == 0;
	}
}
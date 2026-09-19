package com.sifter.entity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SifterAnimationTest {

	@Test
	void startsAtZeroWithNoPhase() {
		assertEquals(0.0f, SifterAnimation.wobbleOffset(0f, 20f, 1f, 0f), 0.0001f);
	}

	@Test
	void neverExceedsTheAmplitude() {
		float amplitude = 1f;
		for (float ticks = 0f; ticks < 1000f; ticks += 0.5f) {
			float offset = SifterAnimation.wobbleOffset(ticks, 20f, amplitude, 0f);
			assertTrue(offset >= -amplitude && offset <= amplitude,
				"offset " + offset + " at ticks=" + ticks + " went outside the amplitude");
		}
	}

	@Test
	void repeatsAfterOnePeriod() {
		float period = 20f;
		assertEquals(
			SifterAnimation.wobbleOffset(5f, period, 1f, 0f),
			SifterAnimation.wobbleOffset(5f + period, period, 1f, 0f),
			0.0001f
		);
	}

	@Test
	void aPhaseIsJustAHeadStartAlongTheSameWave() {
		// Being 5 ticks into the wave with no phase should look identical
		// to being at tick 0 with a phase of 5 - that's the whole idea
		// behind giving the top and bottom blocks different phases below.
		assertEquals(
			SifterAnimation.wobbleOffset(5f, 20f, 1f, 0f),
			SifterAnimation.wobbleOffset(0f, 20f, 1f, 5f),
			0.0001f
		);
	}
}

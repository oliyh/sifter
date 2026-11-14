package com.sifter.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TickTimerTest {

	@Test
	void isDueOnTickZeroAndEveryIntervalAfter() {
		assertTrue(TickTimer.isDue(0, 20));
		assertTrue(TickTimer.isDue(20, 20));
		assertTrue(TickTimer.isDue(40, 20));
	}

	@Test
	void isNotDueInBetween() {
		for (int tick = 1; tick < 20; tick++) {
			assertFalse(TickTimer.isDue(tick, 20), "tick " + tick + " should not be due");
		}
	}

	@Test
	void anIntervalOfOneIsDueEveryTick() {
		for (int tick = 0; tick < 10; tick++) {
			assertTrue(TickTimer.isDue(tick, 1));
		}
	}
}
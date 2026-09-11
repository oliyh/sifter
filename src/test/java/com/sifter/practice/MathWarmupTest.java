package com.sifter.practice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MathWarmupTest {

	// A test is just a method that calls our code, then checks the
	// answer with an "assertion". assertEquals(expected, actual) fails
	// loudly if the two don't match.
	@Test
	void addsTwoNumbers() {
		assertEquals(5, MathWarmup.add(2, 3));
	}
}

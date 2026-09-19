package com.sifter.entity;

public class SifterAnimation {

    private SifterAnimation() {
	}

	// Math.sin() turns a number into a smooth wave between -1 and 1, that
	// repeats forever. We control how fast it repeats (period, in ticks),
	// how big the wiggle is (amplitude), and where in the cycle it starts
	// (phase, also in ticks). Two calls with the same period and amplitude
	// but different phase trace out the *same* wave, just at different
	// points along it - which is exactly how we'll get the top and bottom
	// blocks to move out of step with each other, using this one method.
	public static float wobbleOffset(float ticksAlive, float period, float amplitude, float phase) {
		return (float) (Math.sin((ticksAlive + phase) / period * Math.PI * 2.0) * amplitude);
	}
}

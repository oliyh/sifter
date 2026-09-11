# Interlude — Writing tests

Companion to the Interlude section of [`LESSON_PLAN.md`](../LESSON_PLAN.md).

## You type this

Create `src/main/java/com/sifter/practice/MathWarmup.java` - plain Java,
nothing Minecraft-specific on purpose:

```java
package com.sifter.practice;

// A throwaway practice class - see the "Writing tests" section of
// LESSON_PLAN.md. Nothing here talks to Minecraft at all, on purpose:
// it's just plain Java, so we can focus on what a test IS before we
// use tests on real mod code.
public class MathWarmup {

	public static int add(int a, int b) {
		return a + b;
	}
}
```

Create `src/test/java/com/sifter/practice/MathWarmupTest.java` - notice
it's under `src/test`, not `src/main`. Test code lives in its own tree,
separate from the real mod, and never ships inside the mod jar:

```java
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
```

## How to check it

```bash
./gradlew test
```

Should print `BUILD SUCCESSFUL` - green.

Now break it on purpose: change `assertEquals(5, MathWarmup.add(2, 3));`
to `assertEquals(10, MathWarmup.add(2, 3));`, save, and run `./gradlew
test` again. Read the failure message Gradle prints - that's red. Put the
`5` back, run once more, watch it go green again.

## Why `./gradlew build` matters from now on

`build.gradle` is already set up so `./gradlew build` runs the tests as
one of its steps. If any test is red, the whole build fails - you'll
never be able to accidentally ship a jar with a known-broken test in it.

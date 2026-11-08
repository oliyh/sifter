package com.sifter.resources;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

// Checks the English names file has an entry for everything we add. It
// reads the file as plain text, so no game is needed. The keys are typed
// out here on purpose: referencing ModItems would load real Items, which
// (see Phase 6) can't happen in a test.
class LangFileTest {

	private static final Path LANG_FILE = Path.of("src/client/resources/assets/sifter/lang/en_us.json");

	@Test
	void theSifterHasAName() throws IOException {
		assertHasKey("entity.sifter.sifter");
	}

	@Test
	void theSpawnEggHasAName() throws IOException {
		assertHasKey("item.sifter.sifter_spawn_egg");
	}

	private static void assertHasKey(String key) throws IOException {
		String text = Files.readString(LANG_FILE);
		assertTrue(text.contains("\"" + key + "\""), "en_us.json is missing " + key);
	}
}
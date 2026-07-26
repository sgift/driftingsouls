package net.driftingsouls.ds2.server.framework.authentication;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PasswordGeneratorTest
{
	@Test
	public void generate_shouldProduceAPasswordOfTheDocumentedLength() {
		String password = new PasswordGenerator().generate();

		assertEquals(16, password.length());
	}

	@Test
	public void generate_shouldOnlyUseUnambiguousAlphanumericCharacters() {
		String password = new PasswordGenerator().generate();

		assertTrue("unexpected characters in " + password, password.matches("[a-zA-Z2-9&&[^lIO]]+"));
	}

	@Test
	public void generate_shouldNotRepeatItself() {
		// The old implementation drew a single int from ThreadLocalRandom; over 1000 draws a collision
		// was likely. With 93 bits of entropy any repetition here means the generator is broken.
		var generator = new PasswordGenerator();
		Set<String> passwords = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			passwords.add(generator.generate());
		}

		assertEquals(1000, passwords.size());
	}

	@Test
	public void generate_shouldUseMoreThanAHandfulOfDistinctCharacters() {
		var generator = new PasswordGenerator();
		Set<Character> characters = new HashSet<>();
		for (int i = 0; i < 200; i++) {
			for (char c : generator.generate().toCharArray()) {
				characters.add(c);
			}
		}

		// All 57 alphabet characters are expected in 3200 draws; the bound allows slack for chance.
		assertTrue("only saw " + characters.size() + " distinct characters", characters.size() > 40);
	}
}

package net.driftingsouls.ds2.server.framework.authentication;

import net.driftingsouls.ds2.server.framework.Common;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PasswordHasherTest
{
	// md5("test123") is cc03e747..., the legacy encoding drops the zero of the 0x03 byte.
	private static final String LEGACY_TEST123 = "cc3e747a6afbbcbf8be7668acfebee5";

	private final PasswordHasher hasher = new PasswordHasher();

	@Test
	public void hash_shouldProduceBcrypt() {
		String hash = hasher.hash("test123");

		assertTrue(hash, hash.startsWith("$2a$10$"));
		assertEquals(60, hash.length());
	}

	@Test
	public void hash_shouldBeSalted() {
		assertNotEquals(hasher.hash("test123"), hasher.hash("test123"));
	}

	@Test
	public void givenABcryptHash_matches_shouldAcceptTheRightPasswordOnly() {
		String hash = hasher.hash("test123");

		assertTrue(hasher.matches("test123", hash));
		assertFalse(hasher.matches("test124", hash));
		assertFalse(hasher.matches("", hash));
	}

	@Test
	public void givenA31CharacterLegacyHash_matches_shouldAcceptTheRightPasswordOnly() {
		assertEquals(31, LEGACY_TEST123.length());

		assertTrue(hasher.matches("test123", LEGACY_TEST123));
		assertFalse(hasher.matches("test124", LEGACY_TEST123));
	}

	@Test
	public void givenAStandardMd5Hash_matches_shouldReject() {
		// Legacy values were never zero-padded, so the standard encoding never occurs in the database.
		assertFalse(hasher.matches("test123", "cc03e747a6afbbcbf8be7668acfebee5"));
	}

	@Test
	public void givenLegacyHashesFromCommonMd5_matches_shouldAcceptThem() {
		// The stored legacy values were all written by Common.md5, including for non-ASCII passwords.
		for (String password : new String[] {"test123", "bar", "Passwört", "ünïcödé €", " ", "a\"b'c$d"}) {
			assertTrue(password, hasher.matches(password, Common.md5(password)));
		}
	}

	@Test
	public void givenAnEmptyOrMissingHash_matches_shouldReject() {
		assertFalse(hasher.matches("", ""));
		assertFalse(hasher.matches("test123", null));
	}

	@Test
	public void needsRehash_shouldFlagLegacyHashesOnly() {
		assertTrue(hasher.needsRehash(LEGACY_TEST123));
		assertTrue(hasher.needsRehash(Common.md5("bar")));
		assertFalse(hasher.needsRehash(hasher.hash("test123")));
	}
}

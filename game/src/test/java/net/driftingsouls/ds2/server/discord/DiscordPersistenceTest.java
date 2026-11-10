package net.driftingsouls.ds2.server.discord;

import net.driftingsouls.ds2.server.DBSingleTransactionTest;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Prueft die Abbildung der Discord-Entities auf die Tabellen aus V042 samt ihrer HQL-Abfragen.
 */
public class DiscordPersistenceTest extends DBSingleTransactionTest
{
	private static final String GUILD = "471610776126226432";

	@Test
	public void givenTheMigration_load_shouldReturnThePresetConfiguration()
	{
		DiscordConfiguration configuration = DiscordConfiguration.load(getEM());

		assertFalse(configuration.isActive());
		assertEquals(GUILD, configuration.getGuildId());
		assertEquals("https://discord.gg/cpxxAGy", configuration.getSafeInviteUrl());
	}

	@Test
	public void givenAStoredChannel_all_shouldReturnItWithAllFields()
	{
		DiscordChannel channel = new DiscordChannel(GUILD, "7");
		channel.setName("handel");
		channel.setCategory("Spiel");
		channel.setPosition(3);
		channel.setReadAccess(DiscordAccess.ALL);
		channel.setWriteAccess(DiscordAccess.GAMEMASTER);
		channel.setDefaultChannel(true);
		channel.setBotNotice("Bot fehlt: Webhooks verwalten");
		channel.setVisibleToEveryone(true);
		channel.setDiscordSignature("a".repeat(64));
		channel.approve();
		persist(channel);
		getEM().flush();
		getEM().clear();

		List<DiscordChannel> channels = DiscordChannel.all(getEM(), GUILD);

		assertEquals(1, channels.size());
		DiscordChannel loaded = channels.get(0);
		assertEquals("handel", loaded.getName());
		assertEquals("Spiel", loaded.getCategory());
		assertEquals(DiscordAccess.ALL, loaded.getReadAccess());
		assertEquals(DiscordAccess.GAMEMASTER, loaded.getWriteAccess());
		assertTrue(loaded.isDefaultChannel());
		assertEquals("Bot fehlt: Webhooks verwalten", loaded.getBotNotice());
		assertTrue(loaded.isVisibleToEveryone());
		assertTrue(loaded.isApprovalCurrent());
	}

	@Test
	public void givenSentMessages_authorsAndDeleteFor_shouldUseTheStoredAuthors()
	{
		persist(new DiscordSentMessage("1001", "500", 10));
		persist(new DiscordSentMessage("1002", "501", 11));
		getEM().flush();

		Map<String, String> authors = DiscordSentMessage.authors(getEM(), List.of("1001", "1002", "1003"));
		assertEquals(Map.of("1001", "500", "1002", "501"), authors);

		// Gerade gesendete Nachrichten sind noch nicht abgelaufen
		DiscordSentMessage.deleteExpired(getEM());
		DiscordSentMessage.deleteFor(getEM(), 10);
		getEM().clear();

		assertEquals(Map.of("1002", "501"), DiscordSentMessage.authors(getEM(), List.of("1001", "1002")));
	}
}

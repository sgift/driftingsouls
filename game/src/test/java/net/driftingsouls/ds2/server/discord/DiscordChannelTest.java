package net.driftingsouls.ds2.server.discord;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiscordChannelTest
{
	private static class TestUser extends User
	{
		TestUser(int id, int accesslevel, UserFlag flag)
		{
			setId(id);
			setAccesslevel(accesslevel);
			setFlag(flag, true);
		}
	}

	private final User player = new TestUser(10, 0, UserFlag.HIDE);
	private final User npc = new TestUser(-10, 0, UserFlag.HIDE);
	private final User gamemaster = new TestUser(11, 0, UserFlag.GAMEMASTER);
	private final User admin = new TestUser(12, 30, UserFlag.HIDE);

	@Test
	public void givenBlockedChannel_shouldBeReadableByNobody()
	{
		DiscordChannel channel = channel(DiscordAccess.BLOCKED, DiscordAccess.BLOCKED);

		assertFalse(channel.mayRead(player));
		assertFalse(channel.mayRead(npc));
		assertFalse(channel.mayRead(gamemaster));
		assertFalse(channel.mayRead(admin));
	}

	@Test
	public void givenNpcChannel_shouldOnlyBeReadableByNpcsAndAdmins()
	{
		DiscordChannel channel = channel(DiscordAccess.NPC, DiscordAccess.NPC);

		assertFalse(channel.mayRead(player));
		assertTrue(channel.mayRead(npc));
		assertFalse(channel.mayRead(gamemaster));
		assertTrue(channel.mayRead(admin));
	}

	@Test
	public void givenGameMasterChannel_shouldOnlyBeReadableByGameMastersAndAdmins()
	{
		DiscordChannel channel = channel(DiscordAccess.GAMEMASTER, DiscordAccess.GAMEMASTER);

		assertFalse(channel.mayRead(player));
		assertFalse(channel.mayRead(npc));
		assertTrue(channel.mayRead(gamemaster));
		assertTrue(channel.mayRead(admin));
	}

	@Test
	public void givenAdminChannel_shouldOnlyBeReadableByAdmins()
	{
		DiscordChannel channel = channel(DiscordAccess.ADMIN, DiscordAccess.ADMIN);

		assertFalse(channel.mayRead(player));
		assertFalse(channel.mayRead(npc));
		assertFalse(channel.mayRead(gamemaster));
		assertTrue(channel.mayRead(admin));
	}

	@Test
	public void givenAnnouncementChannel_shouldBeReadableByAllButWritableOnlyByAdmins()
	{
		DiscordChannel channel = channel(DiscordAccess.ALL, DiscordAccess.ADMIN);

		assertTrue(channel.mayRead(player));
		assertFalse(channel.mayWrite(player));
		assertTrue(channel.mayWrite(admin));
	}

	@Test
	public void givenWriteAccessWithoutReadAccess_shouldNotAllowWriting()
	{
		DiscordChannel channel = channel(DiscordAccess.ADMIN, DiscordAccess.ALL);

		assertFalse(channel.mayWrite(player));
		assertTrue(channel.mayWrite(admin));
	}

	private static DiscordChannel channel(DiscordAccess readAccess, DiscordAccess writeAccess)
	{
		DiscordChannel channel = new DiscordChannel("1", "2");
		channel.setReadAccess(readAccess);
		channel.setWriteAccess(writeAccess);
		return channel;
	}
}

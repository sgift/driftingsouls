package net.driftingsouls.ds2.server.discord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DiscordServiceTest
{
	private static final String GUILD = "100";
	private static final String BOT = "200";
	private static final String BOT_ROLE = "300";
	private static final String OTHER_ROLE = "400";

	@Test
	public void givenDiscordMarkup_formatContent_shouldReturnReadableText()
	{
		// setup
		Map<String, String> users = Collections.singletonMap("42", "Bob");
		Map<String, String> channels = Collections.singletonMap("7", "handel");

		// run
		String text = DiscordService.formatContent("Hi <@42> und <@!43>, siehe <#7> und <#8> <@&99> <:ds:12345> <a:tanz:6789>", users, channels);

		// assert: Server-Emojis bleiben stehen, die zeigt der Chat als Bild an
		assertEquals("Hi @Bob und @unbekannt, siehe #handel und #kanal @Rolle <:ds:12345> <a:tanz:6789>", text);
	}

	@Test
	public void givenShortcodes_formatContent_shouldTurnThemIntoEmojis()
	{
		String text = DiscordService.formatContent("Super :thumbsup: :heart: :gibtsnicht: um 10:30:45", Collections.emptyMap(), Collections.emptyMap());

		assertEquals("Super 👍 ❤ :gibtsnicht: um 10:30:45", text);
	}

	@Test
	public void givenServerEmojiWithUnicodeName_formatContent_shouldNotBreakIt()
	{
		String text = DiscordService.formatContent("<:heart:123> <a:smile:456>", Collections.emptyMap(), Collections.emptyMap());

		assertEquals("<:heart:123> <a:smile:456>", text);
	}

	@Test
	public void givenShortcodesWhenSending_replaceShortcodes_shouldPreferServerEmojis()
	{
		Map<String, String> serverEmojis = Collections.singletonMap("heart", "<:heart:999>");

		String text = DiscordService.replaceShortcodes(":heart: :thumbsup: :small_green_triangle:", serverEmojis);

		assertEquals("<:heart:999> 👍 :small_green_triangle:", text);
	}

	@Test
	public void givenSpecialCharactersInText_formatContent_shouldLeaveThemUnchanged()
	{
		String text = DiscordService.formatContent("Preis: 5$ \\o/ <b>", Collections.emptyMap(), Collections.emptyMap());

		assertEquals("Preis: 5$ \\o/ <b>", text);
	}

	@Test
	public void givenPlayerName_webhookUsername_shouldRemoveForbiddenParts()
	{
		assertEquals("Max", DiscordService.webhookUsername("Max@Discord"));
		assertEquals("Spieler", DiscordService.webhookUsername("clyde"));
		assertEquals("Spieler", DiscordService.webhookUsername("everyone"));
		assertEquals("Spieler", DiscordService.webhookUsername(null));
		assertEquals(80, DiscordService.webhookUsername("x".repeat(100)).length());
	}

	@Test
	public void givenAvatar_avatarUrl_shouldReturnCdnUrl()
	{
		assertEquals("https://cdn.discordapp.com/avatars/123/0123456789abcdef0123456789abcdef.png?size=64", DiscordUser.avatarUrl("123", "0123456789abcdef0123456789abcdef"));
		assertEquals("https://cdn.discordapp.com/avatars/123/a_0123456789abcdef0123456789abcdef.png?size=64", DiscordUser.avatarUrl("123", "a_0123456789abcdef0123456789abcdef"));
		assertEquals("https://cdn.discordapp.com/embed/avatars/" + ((471610776126226432L >>> 22) % 6) + ".png", DiscordUser.avatarUrl("471610776126226432", ""));
	}

	@Test
	public void givenAllPermissionsOnTheServer_missingChannelPermissions_shouldReportNothing()
	{
		DiscordService.BotPermissions permissions = botPermissions(DiscordService.CHANNEL_PERMISSIONS);

		assertNull(DiscordService.missingChannelPermissions(permissions, GUILD, channel("[]")));
	}

	@Test
	public void givenChannelBlockedForEveryone_missingChannelPermissions_shouldReportMissingView()
	{
		DiscordService.BotPermissions permissions = botPermissions(DiscordService.CHANNEL_PERMISSIONS);

		String notice = DiscordService.missingChannelPermissions(permissions, GUILD,
				channel("[{\"id\":\"" + GUILD + "\",\"type\":0,\"allow\":\"0\",\"deny\":\"" + DiscordService.PERMISSION_VIEW_CHANNEL + "\"}]"));

		assertEquals("Bot fehlt: Kanäle ansehen", notice);
	}

	@Test
	public void givenChannelBlockedForEveryoneButAllowedForBotRole_missingChannelPermissions_shouldReportNothing()
	{
		DiscordService.BotPermissions permissions = botPermissions(DiscordService.CHANNEL_PERMISSIONS);

		String notice = DiscordService.missingChannelPermissions(permissions, GUILD, channel("["
				+ "{\"id\":\"" + GUILD + "\",\"type\":0,\"allow\":\"0\",\"deny\":\"" + DiscordService.PERMISSION_VIEW_CHANNEL + "\"},"
				+ "{\"id\":\"" + BOT_ROLE + "\",\"type\":0,\"allow\":\"" + DiscordService.PERMISSION_VIEW_CHANNEL + "\",\"deny\":\"0\"}]"));

		assertNull(notice);
	}

	@Test
	public void givenChannelOnlyForOtherRole_missingChannelPermissions_shouldReportMissingView()
	{
		DiscordService.BotPermissions permissions = botPermissions(DiscordService.CHANNEL_PERMISSIONS);

		String notice = DiscordService.missingChannelPermissions(permissions, GUILD, channel("["
				+ "{\"id\":\"" + GUILD + "\",\"type\":0,\"allow\":\"0\",\"deny\":\"" + DiscordService.PERMISSION_VIEW_CHANNEL + "\"},"
				+ "{\"id\":\"" + OTHER_ROLE + "\",\"type\":0,\"allow\":\"" + DiscordService.PERMISSION_VIEW_CHANNEL + "\",\"deny\":\"0\"}]"));

		assertEquals("Bot fehlt: Kanäle ansehen", notice);
	}

	@Test
	public void givenWebhooksDeniedForBot_missingChannelPermissions_shouldReportWebhooks()
	{
		DiscordService.BotPermissions permissions = botPermissions(DiscordService.CHANNEL_PERMISSIONS);

		String notice = DiscordService.missingChannelPermissions(permissions, GUILD,
				channel("[{\"id\":\"" + BOT + "\",\"type\":1,\"allow\":\"0\",\"deny\":\"" + DiscordService.PERMISSION_MANAGE_WEBHOOKS + "\"}]"));

		assertEquals("Bot fehlt: Webhooks verwalten", notice);
	}

	@Test
	public void givenBotIsAdministrator_missingChannelPermissions_shouldIgnoreOverwrites()
	{
		DiscordService.BotPermissions permissions = botPermissions(-1L);

		assertNull(DiscordService.missingChannelPermissions(permissions, GUILD,
				channel("[{\"id\":\"" + GUILD + "\",\"type\":0,\"allow\":\"0\",\"deny\":\"" + DiscordService.PERMISSION_VIEW_CHANNEL + "\"}]")));
	}


	private static DiscordService.BotPermissions botPermissions(long serverPermissions)
	{
		DiscordService.BotPermissions permissions = new DiscordService.BotPermissions();
		permissions.botId = BOT;
		permissions.roles.add(BOT_ROLE);
		permissions.serverPermissions = serverPermissions;
		return permissions;
	}

	private static JsonObject channel(String overwrites)
	{
		return JsonParser.parseString("{\"id\":\"1\",\"type\":0,\"permission_overwrites\":" + overwrites + "}").getAsJsonObject();
	}
}

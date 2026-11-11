package net.driftingsouls.ds2.server.discord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests der Schutzmassnahmen der Discord-Anbindung: Rechte, Sichtbarkeit, Sende-Limits, Links und Konfiguration.
 */
public class DiscordSecurityTest
{
	private static final String GUILD = "100";
	private static final String OWNER = "1";
	private static final String PLAYER = "500";
	private static final String ROLE = "300";
	private static final String MOD_ROLE = "400";
	private static final long WRITE = DiscordService.PERMISSION_VIEW_CHANNEL | DiscordService.PERMISSION_SEND_MESSAGES;

	@After
	public void tearDown()
	{
		DiscordConfiguration.environment = System::getenv;
	}

	// ---- Rechte eines Mitglieds im Kanal ----

	@Test
	public void givenEveryoneMayWrite_memberPermissions_shouldAllowWriting()
	{
		long permissions = DiscordService.memberPermissions(OWNER, roles(WRITE), GUILD, PLAYER, Collections.emptySet(), channel("[]"));

		assertEquals(WRITE, permissions & WRITE);
	}

	@Test
	public void givenChannelMutedForEveryone_memberPermissions_shouldDenyWriting()
	{
		long permissions = DiscordService.memberPermissions(OWNER, roles(WRITE), GUILD, PLAYER, Collections.emptySet(),
				channel("[" + overwrite(GUILD, 0, 0, DiscordService.PERMISSION_SEND_MESSAGES) + "]"));

		assertEquals(0, permissions & DiscordService.PERMISSION_SEND_MESSAGES);
	}

	@Test
	public void givenRoleAllowedDespiteEveryoneBlock_memberPermissions_shouldAllowWriting()
	{
		long permissions = DiscordService.memberPermissions(OWNER, roles(WRITE), GUILD, PLAYER, Set.of(ROLE),
				channel("[" + overwrite(GUILD, 0, 0, WRITE) + "," + overwrite(ROLE, 0, WRITE, 0) + "]"));

		assertEquals(WRITE, permissions & WRITE);
	}

	@Test
	public void givenAccountBlockedDespiteRole_memberPermissions_shouldDenyWriting()
	{
		long permissions = DiscordService.memberPermissions(OWNER, roles(WRITE), GUILD, PLAYER, Set.of(ROLE),
				channel("[" + overwrite(ROLE, 0, WRITE, 0) + "," + overwrite(PLAYER, 1, 0, DiscordService.PERMISSION_SEND_MESSAGES) + "]"));

		assertEquals(0, permissions & DiscordService.PERMISSION_SEND_MESSAGES);
	}

	@Test
	public void givenOwnerAndAdministrator_memberPermissions_shouldAllowEverything()
	{
		Map<String, Long> rolePermissions = roles(0);
		rolePermissions.put(MOD_ROLE, DiscordService.PERMISSION_ADMINISTRATOR);
		JsonObject blocked = channel("[" + overwrite(GUILD, 0, 0, WRITE) + "]");

		assertEquals(DiscordService.ALL_PERMISSIONS, DiscordService.memberPermissions(OWNER, rolePermissions, GUILD, OWNER, Collections.emptySet(), blocked));
		assertEquals(DiscordService.ALL_PERMISSIONS, DiscordService.memberPermissions(OWNER, rolePermissions, GUILD, PLAYER, Set.of(MOD_ROLE), blocked));
	}

	// ---- Sichtbarkeit und Freigabe ----

	@Test
	public void givenChannelWithoutOverwrites_isVisibleToEveryone_shouldBeVisible()
	{
		assertTrue(DiscordService.isVisibleToEveryone(GUILD, DiscordService.PERMISSION_VIEW_CHANNEL, channel("[]")));
	}

	@Test
	public void givenChannelHiddenFromEveryone_isVisibleToEveryone_shouldNotBeVisible()
	{
		assertFalse(DiscordService.isVisibleToEveryone(GUILD, DiscordService.PERMISSION_VIEW_CHANNEL,
				channel("[" + overwrite(GUILD, 0, 0, DiscordService.PERMISSION_VIEW_CHANNEL) + "]")));
	}

	@Test
	public void givenChannelBecomesPrivate_permissionSignature_shouldChange()
	{
		String before = DiscordService.permissionSignature(GUILD, DiscordService.PERMISSION_VIEW_CHANNEL, channel("[]"));
		String after = DiscordService.permissionSignature(GUILD, DiscordService.PERMISSION_VIEW_CHANNEL,
				channel("[" + overwrite(GUILD, 0, 0, DiscordService.PERMISSION_VIEW_CHANNEL) + "]"));

		assertNotEquals(before, after);
		assertEquals(64, before.length());
	}

	@Test
	public void givenRoleMayRead_permissionSignature_shouldChange()
	{
		String before = DiscordService.permissionSignature(GUILD, 0, channel("[]"));
		String after = DiscordService.permissionSignature(GUILD, 0, channel("[" + overwrite(ROLE, 0, DiscordService.PERMISSION_READ_MESSAGE_HISTORY, 0) + "]"));

		assertNotEquals(before, after);
	}

	@Test
	public void givenOnlyWriteRightOrOrderChanged_permissionSignature_shouldStayTheSame()
	{
		String a = overwrite(ROLE, 0, DiscordService.PERMISSION_VIEW_CHANNEL, 0);
		String b = overwrite(MOD_ROLE, 0, 0, DiscordService.PERMISSION_VIEW_CHANNEL);
		String before = DiscordService.permissionSignature(GUILD, DiscordService.PERMISSION_VIEW_CHANNEL, channel("[" + a + "," + b + "]"));
		String after = DiscordService.permissionSignature(GUILD, DiscordService.PERMISSION_VIEW_CHANNEL | DiscordService.PERMISSION_SEND_MESSAGES,
				channel("[" + b + "," + a + "," + overwrite(PLAYER, 1, 0, DiscordService.PERMISSION_SEND_MESSAGES) + "]"));

		assertEquals(before, after);
	}

	@Test
	public void givenApproval_isApprovalCurrent_shouldOnlyHoldForTheApprovedVisibility()
	{
		DiscordChannel channel = new DiscordChannel(GUILD, "7");
		channel.setDiscordSignature("a");
		assertFalse(channel.isApprovalCurrent());

		channel.approve();
		assertTrue(channel.isApprovalCurrent());

		channel.setDiscordSignature("b");
		assertFalse(channel.isApprovalCurrent());
	}

	// ---- Senden ----

	@Test
	public void givenMaskedLink_defuseLinks_shouldShowTheTarget()
	{
		assertEquals("Hier (https://evil.example/x) klicken", DiscordService.defuseLinks("[Hier](https://evil.example/x) klicken"));
		assertEquals("Hier (https://evil.example)", DiscordService.defuseLinks("[Hier](<https://evil.example> \"Titel\")"));
		assertEquals("Normaler Text [ohne] Link (kein)", DiscordService.defuseLinks("Normaler Text [ohne] Link (kein)"));
	}

	@Test
	public void givenNestedLink_defuseLinks_shouldLeaveNoMaskedLink()
	{
		String text = DiscordService.defuseLinks("[[a](https://x.example)](https://y.example)");

		assertFalse(text, text.matches("(?s).*\\[[^\\]]*\\]\\([^)]*\\).*"));
	}

	@Test
	public void givenSendLimits_checkAndRecord_shouldRejectSendingTooFast()
	{
		DiscordService.SendLimits limits = new DiscordService.SendLimits();
		limits.checkAndRecord(PLAYER, "7", 0, 10_000);

		expectRejection(() -> limits.checkAndRecord(PLAYER, "8", 0, 10_000 + DiscordService.SEND_INTERVAL_MILLIS - 1));
		limits.checkAndRecord(PLAYER, "8", 0, 10_000 + DiscordService.SEND_INTERVAL_MILLIS);
	}

	@Test
	public void givenSendLimits_checkAndRecord_shouldLimitMessagesPerAccountAndMinute()
	{
		DiscordService.SendLimits limits = new DiscordService.SendLimits();
		long time = 0;
		for (int i = 0; i < DiscordService.MAX_MESSAGES_PER_ACCOUNT; i++)
		{
			limits.checkAndRecord(PLAYER, "7", 0, time);
			time += DiscordService.SEND_INTERVAL_MILLIS;
		}
		long last = time;
		expectRejection(() -> limits.checkAndRecord(PLAYER, "7", 0, last));

		// Ein anderer Account darf weiter schreiben, nach einer Minute auch der erste wieder
		limits.checkAndRecord("501", "7", 0, last);
		limits.checkAndRecord(PLAYER, "7", 0, DiscordService.SEND_WINDOW_MILLIS);
	}

	@Test
	public void givenSendLimits_checkAndRecord_shouldLimitMessagesPerChannel()
	{
		DiscordService.SendLimits limits = new DiscordService.SendLimits();
		for (int i = 0; i < DiscordService.MAX_MESSAGES_PER_CHANNEL; i++)
		{
			limits.checkAndRecord("account" + i, "7", 0, 1000);
		}

		expectRejection(() -> limits.checkAndRecord("nochEiner", "7", 0, 1000));
		limits.checkAndRecord("nochEiner", "8", 0, 1000);
	}

	@Test
	public void givenSlowmode_checkAndRecord_shouldEnforceItPerAccountAndChannel()
	{
		DiscordService.SendLimits limits = new DiscordService.SendLimits();
		limits.checkAndRecord(PLAYER, "7", 30, 0);

		expectRejection(() -> limits.checkAndRecord(PLAYER, "7", 30, 29_999));
		limits.checkAndRecord(PLAYER, "8", 30, 29_999);
		limits.checkAndRecord(PLAYER, "7", 30, 60_000);
	}

	@Test
	public void givenRejectedMessage_checkAndRecord_shouldNotCount()
	{
		DiscordService.SendLimits limits = new DiscordService.SendLimits();
		limits.checkAndRecord(PLAYER, "7", 600, 0);
		expectRejection(() -> limits.checkAndRecord(PLAYER, "7", 600, 5_000));

		// Der abgelehnte Versuch hat das Intervall nicht neu gestartet
		limits.checkAndRecord(PLAYER, "8", 0, DiscordService.SEND_INTERVAL_MILLIS);
	}

	// ---- Medien und Avatare ----

	@Test
	public void givenUrls_isDiscordCdn_shouldOnlyAcceptDiscordsCdn()
	{
		assertTrue(DiscordService.isDiscordCdn("https://cdn.discordapp.com/attachments/1/2/bild.png?ex=1"));
		assertTrue(DiscordService.isDiscordCdn("https://media.discordapp.net/attachments/1/2/bild.png"));
		assertTrue(DiscordService.isDiscordCdn("https://images-ext-1.discordapp.net/external/abc/https/example.com/x.png"));

		assertFalse(DiscordService.isDiscordCdn(null));
		assertFalse(DiscordService.isDiscordCdn("http://cdn.discordapp.com/x.png"));
		assertFalse(DiscordService.isDiscordCdn("https://example.com/x.png"));
		assertFalse(DiscordService.isDiscordCdn("https://cdn.discordapp.com.evil.example/x.png"));
		assertFalse(DiscordService.isDiscordCdn("https://cdn.discordapp.com@evil.example/x.png"));
		assertFalse(DiscordService.isDiscordCdn("https://cdn.discordapp.com:8443/x.png"));
		assertFalse(DiscordService.isDiscordCdn("javascript:alert(1)"));
	}

	@Test
	public void givenInvalidAvatarHash_avatarUrl_shouldReturnTheDefaultAvatar()
	{
		assertTrue(DiscordUser.avatarUrl("123", "\"><script>").startsWith("https://cdn.discordapp.com/embed/avatars/"));
		assertTrue(DiscordUser.avatarUrl("../x", "0123456789abcdef0123456789abcdef").startsWith("https://cdn.discordapp.com/embed/avatars/"));
	}

	// ---- Konfiguration ----

	@Test
	public void givenInviteLinks_isValidInvite_shouldOnlyAcceptDiscordInvites()
	{
		assertTrue(DiscordConfiguration.isValidInvite("https://discord.gg/cpxxAGy"));
		assertTrue(DiscordConfiguration.isValidInvite("https://discord.com/invite/cpxxAGy"));

		assertFalse(DiscordConfiguration.isValidInvite(null));
		assertFalse(DiscordConfiguration.isValidInvite("javascript:alert(document.cookie)"));
		assertFalse(DiscordConfiguration.isValidInvite("https://evil.example/discord.gg/x"));
		assertFalse(DiscordConfiguration.isValidInvite("https://discord.gg/x\" onclick=\"alert(1)"));
		assertFalse(DiscordConfiguration.isValidInvite("http://discord.gg/cpxxAGy"));
	}

	@Test
	public void givenInvalidInviteLinkInTheDatabase_getSafeInviteUrl_shouldReturnNull()
	{
		DiscordConfiguration configuration = new DiscordConfiguration();
		configuration.setInviteUrl("javascript:alert(1)");
		assertNull(configuration.getSafeInviteUrl());

		configuration.setInviteUrl("https://discord.gg/cpxxAGy");
		assertEquals("https://discord.gg/cpxxAGy", configuration.getSafeInviteUrl());
	}

	@Test
	public void givenNoRedirectUri_isLoginConfigured_shouldReturnFalse()
	{
		DiscordConfiguration configuration = configured();
		configuration.setRedirectUri(null);
		assertFalse(configuration.isLoginConfigured());

		configuration.setRedirectUri(" https://ds.example/ds?module=options&action=discordCallback ");
		assertTrue(configuration.isLoginConfigured());
		assertEquals("https://ds.example/ds?module=options&action=discordCallback", configuration.getRedirectUri());
	}

	@Test
	public void givenRequestUrl_suggestRedirectUri_shouldSuggestTheCallback()
	{
		assertEquals("http://localhost:8080/ds?module=options&action=discordCallback",
				DiscordConfiguration.suggestRedirectUri("http://localhost:8080/ds?module=admin"));
	}

	@Test
	public void givenSecretsInEnvironmentVariables_getBotTokenAndClientSecret_shouldTakePrecedence()
	{
		Map<String, String> environment = new HashMap<>();
		environment.put(DiscordConfiguration.ENV_BOT_TOKEN, " env-token ");
		DiscordConfiguration.environment = environment::get;
		DiscordConfiguration configuration = new DiscordConfiguration();
		configuration.setBotToken("db-token");
		configuration.setClientSecret("db-secret");

		assertEquals("env-token", configuration.getBotToken());
		assertTrue(configuration.isBotTokenFromEnvironment());
		assertTrue(configuration.isBotTokenStored());
		assertEquals("db-secret", configuration.getClientSecret());
		assertFalse(configuration.isClientSecretFromEnvironment());
	}

	@Test
	public void givenLink_isLinkCurrent_shouldExpireAfterTheValidityPeriod()
	{
		long now = System.currentTimeMillis();
		assertTrue(DiscordUser.isLinkCurrent(now - TimeUnit.DAYS.toMillis(DiscordUser.LINK_VALID_DAYS - 1)));
		assertFalse(DiscordUser.isLinkCurrent(now - TimeUnit.DAYS.toMillis(DiscordUser.LINK_VALID_DAYS + 1)));
		assertFalse(DiscordUser.isLinkCurrent(0L));
		assertFalse(DiscordUser.isLinkCurrent(null));
	}

	private static DiscordConfiguration configured()
	{
		DiscordConfiguration.environment = name -> null;
		DiscordConfiguration configuration = new DiscordConfiguration();
		configuration.setActive(true);
		configuration.setBotToken("token");
		configuration.setGuildId(GUILD);
		configuration.setClientId("42");
		configuration.setClientSecret("secret");
		return configuration;
	}

	private static void expectRejection(Runnable call)
	{
		try
		{
			call.run();
			fail("DiscordException erwartet");
		}
		catch (DiscordException e)
		{
			// erwartet
		}
	}

	private static Map<String, Long> roles(long everyone)
	{
		Map<String, Long> roles = new HashMap<>();
		roles.put(GUILD, everyone);
		roles.put(ROLE, 0L);
		return roles;
	}

	private static String overwrite(String id, int embedType, long allowed, long denied)
	{
		return "{\"id\":\"" + id + "\",\"type\":" + embedType + ",\"allow\":\"" + allowed + "\",\"deny\":\"" + denied + "\"}";
	}

	private static JsonObject channel(String overwrites)
	{
		return JsonParser.parseString("{\"id\":\"7\",\"type\":0,\"permission_overwrites\":" + overwrites + "}").getAsJsonObject();
	}
}

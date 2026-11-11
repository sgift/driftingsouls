package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.discord.DiscordException;
import net.driftingsouls.ds2.server.discord.DiscordGuildChannel;
import net.driftingsouls.ds2.server.discord.DiscordChannel;
import net.driftingsouls.ds2.server.discord.DiscordConfiguration;
import net.driftingsouls.ds2.server.discord.DiscordCheck;
import net.driftingsouls.ds2.server.discord.DiscordService;
import net.driftingsouls.ds2.server.discord.DiscordAccess;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Einstellungen des Discord-Bots fuer den Ingame-Chat: Zugangsdaten, Server und
 * die Rechte je Kanal.
 * <p>
 * Aenderungen werden nur per POST angenommen, damit sie sich nicht ueber eine Bild-URL
 * (z.B. <code>[img]</code> in einer PM an einen Admin) ausloesen lassen.
 */
@AdminMenuEntry(category = "Sonstiges", name = "Discord-Bot", permission = WellKnownAdminPermission.DISCORD)
public class DiscordAdmin implements AdminPlugin
{
	@Autowired
	private DiscordService discordService;

	private final List<String> messages = new ArrayList<>();
	private final List<String> errors = new ArrayList<>();

	@Override
	public void output(StringBuilder echo)
	{
		Context context = ContextMap.getContext();
		Request request = context.getRequest();
		EntityManager em = context.getEM();
		DiscordConfiguration configuration = DiscordConfiguration.load(em);

		DiscordCheck check = null;
		String action = request.getParameterString("aktion");
		if (!action.isEmpty() && !request.isPost())
		{
			errors.add("Änderungen sind nur über die Formulare dieser Seite möglich.");
			action = "";
		}
		try
		{
			switch (action)
			{
				case "speichern":
					saveSettings(em, request, configuration);
					break;
				case "pruefen":
					check = discordService.check(configuration);
					break;
				case "synchronisieren":
					synchronize(em, configuration);
					break;
				case "kanaele":
					saveChannels(em, request, configuration);
					break;
				default:
					break;
			}
		}
		catch (DiscordException e)
		{
			errors.add(e.getMessage());
		}

		echo.append("<div class='gfxbox' style='width:900px'>");
		echo.append("<h1>Discord-Bot</h1>");
		printMessages(echo);
		printSetupGuide(echo, request, configuration);
		printSettings(echo, request, configuration);
		printCheck(echo, check);
		printChannels(echo, em, configuration);
		echo.append("</div>");
	}

	private void saveSettings(EntityManager em, Request request, DiscordConfiguration configuration)
	{
		String oldGuildId = configuration.getGuildId();
		String oldToken = configuration.getBotToken();

		configuration.setActive("1".equals(request.getParameterString("aktiv")));

		String guildId = request.getParameterString("guild_id").trim();
		if (!guildId.isEmpty() && !guildId.matches("\\d{1,25}"))
		{
			errors.add("Die Server-ID muss eine Zahl sein.");
		}
		else
		{
			configuration.setGuildId(emptyToNull(guildId));
		}

		String clientId = request.getParameterString("client_id").trim();
		if (!clientId.isEmpty() && !clientId.matches("\\d{1,25}"))
		{
			errors.add("Die Client-ID muss eine Zahl sein.");
		}
		else
		{
			configuration.setClientId(emptyToNull(clientId));
		}

		// Geheimnisse werden nie ausgegeben: ein leeres Feld laesst den bisherigen Wert stehen
		String botToken = request.getParameterString("bot_token").trim();
		if (!botToken.isEmpty())
		{
			configuration.setBotToken(botToken);
		}
		if ("1".equals(request.getParameterString("bot_token_loeschen")))
		{
			configuration.setBotToken(null);
		}
		String clientSecret = request.getParameterString("client_secret").trim();
		if (!clientSecret.isEmpty())
		{
			configuration.setClientSecret(clientSecret);
		}
		if ("1".equals(request.getParameterString("client_secret_loeschen")))
		{
			configuration.setClientSecret(null);
		}

		String redirectUri = request.getParameterString("redirect_uri").trim();
		if (!redirectUri.isEmpty() && !redirectUri.matches("https?://[^\\s\"'<>]+"))
		{
			errors.add("Die Redirect-URI muss mit http:// oder https:// beginnen.");
		}
		else
		{
			configuration.setRedirectUri(emptyToNull(redirectUri));
		}

		String inviteUrl = request.getParameterString("invite_url").trim();
		if (!inviteUrl.isEmpty() && !DiscordConfiguration.isValidInvite(inviteUrl))
		{
			errors.add("Der Einladungslink muss eine Discord-Einladung sein (https://discord.gg/... oder https://discord.com/invite/...).");
		}
		else
		{
			configuration.setInviteUrl(emptyToNull(inviteUrl));
		}

		if (!em.contains(configuration))
		{
			em.persist(configuration);
		}
		if (!Objects.equals(oldGuildId, configuration.getGuildId()) || !Objects.equals(oldToken, configuration.getBotToken()))
		{
			discordService.reset();
		}
		messages.add("Einstellungen gespeichert.");
	}

	private void synchronize(EntityManager em, DiscordConfiguration configuration)
	{
		if (!DiscordConfiguration.isSet(configuration.getBotToken()) || !DiscordConfiguration.isSet(configuration.getGuildId()))
		{
			errors.add("Zum Synchronisieren müssen Bot-Token und Server-ID gesetzt sein.");
			return;
		}

		List<DiscordGuildChannel> guildChannels = discordService.loadChannels(configuration);
		Map<String, DiscordChannel> existingChannels = new HashMap<>();
		for (DiscordChannel channel : DiscordChannel.all(em, configuration.getGuildId()))
		{
			existingChannels.put(channel.getChannelId(), channel);
		}

		int newCount = 0;
		int changed = 0;
		for (DiscordGuildChannel guildChannel : guildChannels)
		{
			DiscordChannel channel = existingChannels.remove(guildChannel.id);
			boolean isNew = channel == null;
			if (isNew)
			{
				channel = new DiscordChannel(configuration.getGuildId(), guildChannel.id);
				newCount++;
			}
			apply(channel, guildChannel);
			if (!isNew && channel.getApprovedSignature() != null && !channel.isApprovalCurrent())
			{
				changed++;
			}
			if (isNew)
			{
				em.persist(channel);
			}
		}
		for (DiscordChannel removed : existingChannels.values())
		{
			em.remove(removed);
		}

		messages.add(guildChannels.size() + " Kanäle synchronisiert, davon " + newCount + " neu (gesperrt), "
				+ existingChannels.size() + " nicht mehr vorhandene entfernt.");
		if (changed > 0)
		{
			errors.add("Bei " + changed + " Kanälen hat sich in Discord geändert, wer sie sehen darf. Sie sind im Spiel gesperrt, bis du die Kanalrechte erneut speicherst.");
		}
	}

	private static void apply(DiscordChannel channel, DiscordGuildChannel guildChannel)
	{
		channel.setName(guildChannel.name);
		channel.setCategory(guildChannel.category);
		channel.setPosition(guildChannel.position);
		channel.setBotNotice(guildChannel.notice);
		channel.setVisibleToEveryone(guildChannel.visibleToEveryone);
		channel.setDiscordSignature(guildChannel.signature);
	}

	/**
	 * Speichert die Kanalrechte. Vorher wird der aktuelle Stand von Discord geladen: Die Freigabe
	 * gilt fuer genau die Sichtbarkeit, die die Kanaele jetzt in Discord haben.
	 */
	private void saveChannels(EntityManager em, Request request, DiscordConfiguration configuration)
	{
		if (!DiscordConfiguration.isSet(configuration.getBotToken()) || !DiscordConfiguration.isSet(configuration.getGuildId()))
		{
			errors.add("Zum Freigeben müssen Bot-Token und Server-ID gesetzt sein.");
			return;
		}
		Map<String, DiscordGuildChannel> guildChannels = new HashMap<>();
		for (DiscordGuildChannel guildChannel : discordService.loadChannels(configuration))
		{
			guildChannels.put(guildChannel.id, guildChannel);
		}

		String defaultChannel = request.getParameterString("standard");
		for (DiscordChannel channel : DiscordChannel.all(em, configuration.getGuildId()))
		{
			DiscordGuildChannel guildChannel = guildChannels.get(channel.getChannelId());
			if (guildChannel == null)
			{
				errors.add("Den Kanal #" + channel.getName() + " gibt es in Discord nicht mehr. Synchronisiere die Kanäle.");
				continue;
			}
			apply(channel, guildChannel);
			channel.setReadAccess(access(request.getParameterString("lesen_" + channel.getId()), channel.getReadAccess()));
			channel.setWriteAccess(access(request.getParameterString("schreiben_" + channel.getId()), channel.getWriteAccess()));
			channel.setDefaultChannel(String.valueOf(channel.getId()).equals(defaultChannel));
			channel.approve();
			if (channel.isDefaultChannel() && channel.getReadAccess() != DiscordAccess.ALL)
			{
				errors.add("Der Standardkanal #" + channel.getName() + " ist nicht für alle Spieler lesbar. Wer ihn nicht lesen darf, bekommt den ersten für ihn lesbaren Kanal angezeigt.");
			}
			if (channel.getReadAccess() == DiscordAccess.ALL && !channel.isVisibleToEveryone())
			{
				errors.add("Hinweis: #" + channel.getName() + " ist in Discord nicht für @everyone sichtbar, im Spiel aber für alle Spieler lesbar.");
			}
		}
		messages.add("Kanalrechte gespeichert und für die aktuelle Sichtbarkeit in Discord freigegeben.");
	}

	private static DiscordAccess access(String value, DiscordAccess previous)
	{
		try
		{
			return DiscordAccess.valueOf(value);
		}
		catch (IllegalArgumentException | NullPointerException e)
		{
			return previous;
		}
	}

	private void printMessages(StringBuilder echo)
	{
		for (String message : messages)
		{
			echo.append("<div style='color:#4ec94e'>").append(Common.escapeHTML(message)).append("</div>");
		}
		for (String message : errors)
		{
			echo.append("<div style='color:#ff6060'>").append(Common.escapeHTML(message)).append("</div>");
		}
		if (!messages.isEmpty() || !errors.isEmpty())
		{
			echo.append("<br />");
		}
	}

	/**
	 * Anleitung fuer die Ersteinrichtung in Discord und im Spiel. Sie ist aufgeklappt,
	 * solange der Chat nicht aktiv ist.
	 */
	private void printSetupGuide(StringBuilder echo, Request request, DiscordConfiguration configuration)
	{
		String suggestion = DiscordConfiguration.suggestRedirectUri(request.getRequestURL());

		echo.append("<details").append(configuration.isActive() ? "" : " open='open'").append(" style='margin-bottom:12px'>");
		echo.append("<summary style='cursor:pointer;font-weight:bold;color:#FEB626'>Ersteinrichtung Schritt für Schritt</summary>");
		echo.append("<p>Solange <i>Chat aktiv</i> aus ist, sehen Spieler nichts vom Chat. Du kannst also in Ruhe einrichten und prüfen.</p>");

		echo.append("<p><b>1. Im Discord Developer Portal</b></p><ol>");
		echo.append("<li>Unter <a href='https://discord.com/developers/applications' target='_blank' rel='noopener noreferrer'>discord.com/developers/applications</a> ")
				.append("mit <i>New Application</i> eine Anwendung anlegen. Ihr Name und Bild erscheinen später am Bot.</li>");
		echo.append("<li>Seite <i>Bot</i>: mit <i>Reset Token</i> das Bot-Token erzeugen und kopieren, Discord zeigt es nur einmal an. ")
				.append("Unter <i>Privileged Gateway Intents</i> den <i>Message Content Intent</i> einschalten, sonst kommen die Nachrichten ohne Text an. ")
				.append("Schaltest du <i>Public Bot</i> aus, kann nur der Eigentümer der Anwendung den Bot einladen.</li>");
		echo.append("<li>Seite <i>OAuth2</i>: Client-ID und Client-Secret (<i>Reset Secret</i>) kopieren und unter <i>Redirects</i> die Redirect-URI eintragen, ")
				.append("hier z.B. <code>").append(Common.escapeHTML(suggestion)).append("</code>. Sie muss Zeichen für Zeichen mit dem Feld unten übereinstimmen.</li>");
		echo.append("</ol>");

		echo.append("<p><b>2. Hier im Spiel</b></p><ol start='4'>");
		echo.append("<li>Server-ID: in Discord unter <i>Benutzereinstellungen → Erweitert</i> den <i>Entwicklermodus</i> einschalten, ")
				.append("dann Rechtsklick auf den Server → <i>Server-ID kopieren</i>.</li>");
		echo.append("<li>Client-ID, Redirect-URI und einen Einladungslink für Spieler eintragen (in Discord: Server → <i>Leute einladen</i>, Link ohne Ablaufdatum). ")
				.append("Bot-Token und Client-Secret hier eintragen oder besser als Umgebungsvariablen setzen. Dann <i>Speichern</i>.</li>");
		echo.append("</ol>");

		echo.append("<p><b>3. Auf dem Discord-Server</b></p><ol start='6'>");
		echo.append("<li>Den Bot über den <i>Einladungslink des Bots</i> unter den Einstellungen einladen. Dafür brauchst du auf dem Server das Recht ")
				.append("<i>Server verwalten</i>. Der Bot bekommt damit nur <i>Kanäle ansehen</i>, <i>Nachrichtenverlauf lesen</i> und <i>Einladung erstellen</i>.</li>");
		echo.append("<li>In jedem Kanal, den der Chat nutzen soll: <i>Kanal bearbeiten → Berechtigungen</i>, den Bot hinzufügen und <i>Webhooks verwalten</i> erlauben. ")
				.append("Nicht serverweit, und den Bot nie zum Administrator machen.</li>");
		echo.append("<li>Private Kanäle: dem Bot dort zusätzlich <i>Kanäle ansehen</i> und <i>Nachrichtenverlauf lesen</i> geben, am einfachsten über eine eigene Rolle.</li>");
		echo.append("</ol>");

		echo.append("<p><b>4. Prüfen und freigeben</b></p><ol start='9'>");
		echo.append("<li><i>Verbindung prüfen</i>: Alle Punkte müssen grün sein.</li>");
		echo.append("<li><i>Kanäle von Discord synchronisieren</i>, je Kanal Lesen und Schreiben einstellen, einen für alle Spieler lesbaren Standardkanal wählen ")
				.append("und <i>Kanalrechte speichern</i>. Erst das Speichern gibt die Kanäle frei.</li>");
		echo.append("<li><i>Chat aktiv</i> anhaken und speichern.</li>");
		echo.append("</ol>");

		echo.append("<p><b>Außerhalb des Spiels</b></p><ul>");
		echo.append("<li>Die Datenschutzerklärung um den Discord-Chat ergänzen: Spieler, die zustimmen, laden Avatare, Emojis und Bilder direkt von Discord, ")
				.append("Discord sieht dabei ihre IP-Adresse. Welcher Discord-Account eine Nachricht aus dem Spiel geschrieben hat, speichert das Spiel 30 Tage.</li>");
		echo.append("<li>In den Discord-Serverregeln erwähnen, dass die öffentlichen Kanäle im Spiel gelesen und von dort beschrieben werden können.</li>");
		echo.append("</ul>");
		echo.append("<p>Ausführlicher im Repository: <code>docker/README.md</code> und <code>docs/discord-chat-merge.md</code>.</p>");
		echo.append("</details>");
	}

	private void printSettings(StringBuilder echo, Request request, DiscordConfiguration configuration)
	{
		String suggestion = DiscordConfiguration.suggestRedirectUri(request.getRequestURL());

		echo.append("<h2>Einstellungen</h2>");
		beginForm(echo, "speichern");
		echo.append("<table width='100%'>");
		echo.append("<tr><td width='200'>Chat aktiv</td><td><input type='checkbox' name='aktiv' value='1'")
				.append(configuration.isActive() ? " checked='checked'" : "").append(" /></td></tr>");
		textRow(echo, "Server-ID (Guild)", "guild_id", configuration.getGuildId(), "z.B. 471610776126226432 - zum Testen einfach einen anderen Server eintragen");
		textRow(echo, "Einladungslink", "invite_url", configuration.getInviteUrl(), "wird Spielern angezeigt, die noch nicht auf dem Server sind - nur https://discord.gg/... oder https://discord.com/invite/...");
		secretRow(echo, "Bot-Token", "bot_token", configuration.isBotTokenStored(), configuration.isBotTokenFromEnvironment(), DiscordConfiguration.ENV_BOT_TOKEN);
		textRow(echo, "Client-ID", "client_id", configuration.getClientId(), "Developer Portal → OAuth2");
		secretRow(echo, "Client-Secret", "client_secret", configuration.isClientSecretStored(), configuration.isClientSecretFromEnvironment(), DiscordConfiguration.ENV_CLIENT_SECRET);
		textRow(echo, "Redirect-URI", "redirect_uri", configuration.getRedirectUri(), "Pflichtfeld für die Verknüpfung, z.B. " + suggestion);
		echo.append("<tr><td></td><td><input type='submit' value='Speichern' /></td></tr>");
		echo.append("</table></form>");

		echo.append("<p>Die Redirect-URI genau so im Developer Portal unter OAuth2 → Redirects eintragen. ")
				.append("Bot-Token und Client-Secret lassen sich statt hier auch in den Umgebungsvariablen <code>")
				.append(DiscordConfiguration.ENV_BOT_TOKEN).append("</code> und <code>").append(DiscordConfiguration.ENV_CLIENT_SECRET)
				.append("</code> hinterlegen, dann stehen sie in keinem Datenbank-Dump.<br />");
		if (DiscordConfiguration.isSet(configuration.getClientId()))
		{
			String invite = "https://discord.com/oauth2/authorize?client_id=" + configuration.getClientId().trim()
					+ "&scope=bot&permissions=" + DiscordService.REQUIRED_PERMISSIONS;
			echo.append("Bot auf den Server einladen: <a href='").append(Common.escapeHTML(invite)).append("' target='_blank' rel='noopener noreferrer'>Einladungslink des Bots</a>. ")
					.append("<i>Webhooks verwalten</i> erlaubst du ihm danach nur in den Kanälen, die der Chat nutzen soll.");
		}
		else
		{
			echo.append("Nach dem Eintragen der Client-ID erscheint hier der Einladungslink für den Bot.");
		}
		echo.append("</p>");
	}

	private void printCheck(StringBuilder echo, DiscordCheck check)
	{
		echo.append("<h2>Verbindung</h2>");
		if (check != null)
		{
			echo.append("<ul>");
			for (DiscordCheck.Item item : check.items)
			{
				echo.append("<li style='color:").append(item.ok ? "#4ec94e" : "#ff6060").append("'>")
						.append(item.ok ? "&#10004; " : "&#10008; ")
						.append(Common.escapeHTML(item.text)).append("</li>");
			}
			echo.append("</ul>");
		}
		beginForm(echo, "pruefen");
		echo.append("<input type='submit' value='Verbindung prüfen' /></form>");
	}

	private void printChannels(StringBuilder echo, EntityManager em, DiscordConfiguration configuration)
	{
		echo.append("<h2>Kanäle</h2>");
		echo.append("<p>Lesen und Schreiben lassen sich getrennt freigeben. Schreiben setzt das Leserecht voraus. ")
				.append("Admins dürfen jeden nicht gesperrten Kanal nutzen. Neue Kanäle sind gesperrt, bis sie hier freigegeben werden. ")
				.append("Spieler bekommen nur Kanäle zu sehen, die sie lesen dürfen; Anfragen an andere Kanäle lehnt der Server ab.</p>")
				.append("<p>Die Freigabe gilt für die Sichtbarkeit, die ein Kanal beim Speichern in Discord hat. Ändert sich dort, ")
				.append("wer ihn sehen darf, ist er im Spiel gesperrt, bis du die Kanalrechte erneut speicherst. ")
				.append("Beim Schreiben prüft das Spiel zusätzlich die Discord-Rechte des Accounts, Timeouts und den Slowmode.</p>");

		beginForm(echo, "synchronisieren");
		echo.append("<input type='submit' value='Kanäle von Discord synchronisieren' /></form><br />");

		List<DiscordChannel> channels = DiscordChannel.all(em, configuration.getGuildId());
		if (channels.isEmpty())
		{
			echo.append("<p>Für diesen Server sind noch keine Kanäle synchronisiert.</p>");
			return;
		}

		beginForm(echo, "kanaele");
		echo.append("<table width='100%'>");
		echo.append("<tr><th align='left'>Kanal</th><th align='left'>Lesen</th><th align='left'>Schreiben</th><th>Standard</th><th align='left'>Hinweis</th></tr>");
		String category = null;
		boolean firstRow = true;
		for (DiscordChannel channel : channels)
		{
			if (firstRow || !Objects.equals(category, channel.getCategory()))
			{
				category = channel.getCategory();
				echo.append("<tr><td colspan='5' style='padding-top:8px;font-weight:bold;color:#FEB626'>")
						.append(category != null ? Common.escapeHTML(category) : "Ohne Kategorie").append("</td></tr>");
				firstRow = false;
			}
			echo.append("<tr><td>#").append(Common.escapeHTML(channel.getName()));
			if (!channel.isVisibleToEveryone())
			{
				echo.append(" <span style='color:#a0a0a0;font-size:11px' title='In Discord nicht für @everyone sichtbar'>(privat)</span>");
			}
			echo.append("</td><td>");
			accessSelect(echo, "lesen_" + channel.getId(), channel.getReadAccess());
			echo.append("</td><td>");
			accessSelect(echo, "schreiben_" + channel.getId(), channel.getWriteAccess());
			echo.append("</td><td align='center'><input type='radio' name='standard' value='").append(channel.getId()).append("'")
					.append(channel.isDefaultChannel() ? " checked='checked'" : "").append(" /></td>");
			echo.append("<td style='color:#ff6060;font-size:12px'>");
			List<String> notices = new ArrayList<>();
			if (channel.getBotNotice() != null)
			{
				notices.add(channel.getBotNotice());
			}
			if (channel.getReadAccess() != DiscordAccess.BLOCKED && !channel.isApprovalCurrent())
			{
				notices.add(channel.getApprovedSignature() == null
						? "Noch nicht freigegeben - zum Freigeben speichern"
						: "Sichtbarkeit in Discord geändert - gesperrt, bis die Kanalrechte erneut gespeichert werden");
			}
			echo.append(Common.escapeHTML(String.join(". ", notices))).append("</td></tr>");
		}
		echo.append("<tr><td colspan='5'><br /><input type='submit' value='Kanalrechte speichern' /></td></tr>");
		echo.append("</table></form>");
	}

	private void beginForm(StringBuilder echo, String action)
	{
		echo.append("<form action='./ds' method='post'>");
		echo.append("<input type='hidden' name='module' value='admin' />");
		echo.append("<input type='hidden' name='namedplugin' value='").append(getClass().getName()).append("' />");
		echo.append("<input type='hidden' name='aktion' value='").append(action).append("' />");
	}

	private static void textRow(StringBuilder echo, String label, String name, String value, String notice)
	{
		echo.append("<tr><td>").append(label).append("</td><td><input type='text' size='60' name='").append(name)
				.append("' value='").append(value != null ? Common.escapeHTML(value) : "").append("' />");
		echo.append("<br /><span style='font-size:11px;color:#a0a0a0'>").append(Common.escapeHTML(notice)).append("</span></td></tr>");
	}

	private static void secretRow(StringBuilder echo, String label, String name, boolean stored, boolean fromEnvironment, String environmentVariable)
	{
		echo.append("<tr><td>").append(label).append("</td><td>");
		if (fromEnvironment)
		{
			echo.append("<span style='color:#4ec94e'>aus der Umgebungsvariable <code>").append(environmentVariable).append("</code></span>");
		}
		else
		{
			echo.append("<input type='password' size='60' autocomplete='new-password' name='").append(name)
					.append("' placeholder='").append(stored ? "gesetzt - leer lassen, um ihn zu behalten" : "nicht gesetzt").append("' />");
		}
		if (stored)
		{
			echo.append(" <label><input type='checkbox' name='").append(name).append("_loeschen' value='1' /> ")
					.append(fromEnvironment ? "zusätzlich in der Datenbank gespeicherten Wert löschen" : "löschen").append("</label>");
		}
		echo.append("</td></tr>");
	}

	private static void accessSelect(StringBuilder echo, String name, DiscordAccess current)
	{
		echo.append("<select name='").append(name).append("'>");
		for (DiscordAccess access : DiscordAccess.values())
		{
			echo.append("<option value='").append(access.name()).append("'")
					.append(access == current ? " selected='selected'" : "").append(">")
					.append(access.getLabel()).append("</option>");
		}
		echo.append("</select>");
	}

	private static String emptyToNull(String value)
	{
		return value.isEmpty() ? null : value;
	}
}

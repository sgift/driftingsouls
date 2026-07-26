package net.driftingsouls.ds2.server.framework.authentication;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.driftingsouls.ds2.server.framework.Configuration;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the password mails really leave the application over SMTP and arrive with the issued
 * password in them. Runs against a MailHog container, which accepts SMTP on 1025 and serves the
 * received mails over HTTP on 8025.
 */
public class PasswordMailerTest
{
	private static final int SMTP_PORT = 1025;
	private static final int HTTP_PORT = 8025;

	@ClassRule
	public static final GenericContainer<?> MAILHOG = new GenericContainer<>("mailhog/mailhog:v1.0.1")
		.withExposedPorts(SMTP_PORT, HTTP_PORT)
		.waitingFor(Wait.forHttp("/api/v2/messages").forPort(HTTP_PORT));

	@ClassRule
	public static final TemporaryFolder CONFIG_DIR = new TemporaryFolder();

	private final PasswordMailer mailer = new PasswordMailer();

	@Before
	public void pointConfigurationAtMailhog() throws Exception {
		Path configFile = CONFIG_DIR.getRoot().toPath().resolve("config.xml");
		Files.write(configFile, ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<config>\n"
			+ "\t<setting type=\"string\" name=\"SMTP-SERVER\" value=\"" + MAILHOG.getHost() + "\" />\n"
			+ "\t<setting type=\"string\" name=\"SMTP-PORT\" value=\"" + MAILHOG.getMappedPort(SMTP_PORT) + "\" />\n"
			+ "</config>\n").getBytes(StandardCharsets.UTF_8));

		Configuration.init(CONFIG_DIR.getRoot().getAbsolutePath() + "/");

		deleteAllMails();
	}

	@Test
	public void sendRegistrationMail_shouldDeliverTheIssuedPasswordToTheUser() throws Exception {
		String password = new PasswordGenerator().generate();

		mailer.sendRegistrationMail("testkolonist", "kolonist@example.invalid", password);

		JsonObject mail = theOnlyMail();
		assertEquals("Anmeldung bei Drifting Souls 2", subjectOf(mail));
		assertEquals("kolonist@example.invalid", firstRecipientOf(mail));

		String body = bodyOf(mail);
		assertTrue("password missing from " + body, body.contains(password));
		assertNoPlaceholdersLeft(body);
		assertTrue("login name missing from " + body, body.contains("testkolonist"));
	}

	@Test
	public void sendNewPasswordMail_shouldDeliverTheIssuedPasswordToTheUser() throws Exception {
		String password = new PasswordGenerator().generate();

		mailer.sendNewPasswordMail("testkolonist", "kolonist@example.invalid", password);

		JsonObject mail = theOnlyMail();
		assertEquals("Neues Passwort für Drifting Souls 2", subjectOf(mail));
		assertEquals("kolonist@example.invalid", firstRecipientOf(mail));

		String body = bodyOf(mail);
		assertTrue("password missing from " + body, body.contains(password));
		assertNoPlaceholdersLeft(body);
		assertTrue("user name missing from " + body, body.contains("testkolonist"));
	}

	@Test
	public void sendRegistrationMail_shouldNotSendAnythingWithoutAConfiguredMailServer() throws Exception {
		Path configFile = CONFIG_DIR.getRoot().toPath().resolve("config.xml");
		Files.write(configFile, ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<config>\n"
			+ "\t<setting type=\"string\" name=\"SMTP-SERVER\" value=\"\" />\n"
			+ "</config>\n").getBytes(StandardCharsets.UTF_8));
		Configuration.init(CONFIG_DIR.getRoot().getAbsolutePath() + "/");

		mailer.sendRegistrationMail("testkolonist", "kolonist@example.invalid", "irrelevant");

		assertEquals(0, receivedMails().size());
	}

	private void assertNoPlaceholdersLeft(String body) {
		for (String placeholder : new String[]{"{loginName}", "{username}", "{password}", "{date}"}) {
			assertFalse("unreplaced " + placeholder + " in " + body, body.contains(placeholder));
		}
	}

	private JsonObject theOnlyMail() throws Exception {
		// Transport.send has returned by now, but MailHog stores asynchronously - give it a moment.
		JsonArray mails = receivedMails();
		for (int attempt = 0; attempt < 50 && mails.size() == 0; attempt++) {
			Thread.sleep(100);
			mails = receivedMails();
		}

		assertEquals("expected exactly one mail", 1, mails.size());
		return mails.get(0).getAsJsonObject();
	}

	private JsonArray receivedMails() throws IOException {
		String json = httpGet("/api/v2/messages");
		return JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("items");
	}

	private String subjectOf(JsonObject mail) {
		return decodeHeader(headerOf(mail, "Subject"));
	}

	private String firstRecipientOf(JsonObject mail) {
		return headerOf(mail, "To");
	}

	private String headerOf(JsonObject mail, String name) {
		return mail.getAsJsonObject("Content").getAsJsonObject("Headers")
			.getAsJsonArray(name).get(0).getAsString();
	}

	/**
	 * Undoes the quoted-printable transfer encoding far enough for the assertions above: soft line
	 * breaks are dropped and encoded bytes are decoded as UTF-8.
	 */
	private String bodyOf(JsonObject mail) {
		return decodeQuotedPrintable(mail.getAsJsonObject("Content").get("Body").getAsString());
	}

	private String decodeHeader(String header) {
		// RFC 2047, as MailHog reports it: =?UTF-8?Q?...?=
		if (!header.startsWith("=?UTF-8?Q?") || !header.endsWith("?=")) {
			return header;
		}
		return decodeQuotedPrintable(header.substring("=?UTF-8?Q?".length(), header.length() - 2))
			.replace('_', ' ');
	}

	private String decodeQuotedPrintable(String encoded) {
		String withoutSoftBreaks = encoded.replace("=\r\n", "").replace("=\n", "");

		var bytes = new java.io.ByteArrayOutputStream();
		for (int i = 0; i < withoutSoftBreaks.length(); i++) {
			char c = withoutSoftBreaks.charAt(i);
			if (c == '=' && i + 2 < withoutSoftBreaks.length()) {
				bytes.write(Integer.parseInt(withoutSoftBreaks.substring(i + 1, i + 3), 16));
				i += 2;
			} else {
				bytes.write(c);
			}
		}

		return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
	}

	private void deleteAllMails() throws IOException {
		HttpURLConnection connection = openConnection("/api/v1/messages");
		connection.setRequestMethod("DELETE");
		connection.getResponseCode();
		connection.disconnect();
	}

	private String httpGet(String path) throws IOException {
		HttpURLConnection connection = openConnection(path);
		try (InputStream in = connection.getInputStream()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} finally {
			connection.disconnect();
		}
	}

	private HttpURLConnection openConnection(String path) throws IOException {
		var url = new URL("http://" + MAILHOG.getHost() + ":" + MAILHOG.getMappedPort(HTTP_PORT) + path);
		return (HttpURLConnection) url.openConnection();
	}
}

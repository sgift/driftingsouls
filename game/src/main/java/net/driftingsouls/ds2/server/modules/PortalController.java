/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.SectorTemplateManager;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.*;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.authentication.*;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.*;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.units.TransientUnitCargo;
import net.driftingsouls.ds2.server.user.authentication.AccountInVacationModeException;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Das Portal.
 *
 * @author Christopher Jung
 */
@KeinLoginNotwendig
@KeineTicksperre
@Module(name = "portal", defaultModule = true, outputHandler = EmptyHeaderOutputHandler.class)
public class PortalController extends Controller
{
	private AuthenticationManager authManager;
	private TemplateViewResultFactory templateViewResultFactory;
	private ConfigService configService;

	@Autowired
	public PortalController(AuthenticationManager authManager, TemplateViewResultFactory templateViewResultFactory, ConfigService configService)
	{
		this.authManager = authManager;
		this.templateViewResultFactory = templateViewResultFactory;
		this.configService = configService;
	}

	/**
	 * Ermoeglicht das generieren eines neuen Passworts und anschliessenden
	 * zumailens dessen.
	 *
	 * @param username der Benutzername des Accounts
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine passwordLostAction(String username)
	{
		org.hibernate.Session db = getDB();
		TemplateEngine t = templateViewResultFactory.createFor(this);

		if ("".equals(username))
		{
			t.setVar("show.passwordlost", 1);
		}
		else
		{
			User user = (User) db.createQuery("from User where un = :username")
					.setString("username", username)
					.uniqueResult();
			if (user != null)
			{
				if (!"".equals(user.getEmail()))
				{
					String password = Common.md5(Integer.toString(ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE)));
					String enc_pw = Common.md5(password);

					user.setPassword(enc_pw);
					//db.update("UPDATE users SET passwort='",enc_pw,"' WHERE un='",username,"'");

					String subject = "Neues Passwort fuer Drifting Souls 2";

					String message = "Hallo {username},\n" +
							"du hast ein neues Password angefordert. Dein neues Password lautet \"{password}\" und wurde verschluesselt gespeichert. Wenn es verloren geht, musst du dir ueber die \"Passwort vergessen?\" Funktion der Login-Seite ein neue\n" +
							"s erstellen lassen.\n" +
							"Bitte beachte, dass dein Passwort nicht an andere Nutzer weiter gegeben werden darf.\n" +
							"Das Admin-Team wuenscht weiterhin einen angenehmen Aufenthalt in Drifting Souls 2\n" +
							"Gruss Guzman\n" +
							"Admin\n" +
							"{date} Serverzeit".replace("{username}", username);
					message = message.replace("{password}", password);
					message = message.replace("{date}", Common.date("H:i j.m.Y"));

					Common.mail(user.getEmail(), subject, message);

					Common.writeLog("login.log", Common.date("j.m.Y H:i:s") + ": <" + getRequest().getRemoteAddress() + "> (" + user.getId() + ") <" + username + "> Passwortanforderung von Browser <" + getRequest().getUserAgent() + ">\n");

					t.setVar("show.passwordlost.msg.ok", 1,
							"passwordlost.email", user.getEmail());
				}
				else
				{
					Common.writeLog("login.log", Common.date("j.m.Y H:i:s") + ": <" + getRequest().getRemoteAddress() + "> (" + user.getId() + ") <" + username + "> Passwortanforderung von Browser <" + getRequest().getUserAgent() + ">\n");

					t.setVar("show.passwordlost.msg.error", 1);
				}
			}
			else
			{
				Common.writeLog("login.log", Common.date("j.m.Y H:i:s") + ": <" + getRequest().getRemoteAddress() + "> <" + username + "> Passwortanforderung von Browser <" + getRequest().getUserAgent() + ">\n");
			}
		}
		return t;
	}


	/**
	 * Zeigt die Banner Seite an an.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine bannerAction()
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		t.setVar("show.banner", 1);
		return t;
	}

	/**
	 * Zeigt die AGB an.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine infosAgbAction()
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		t.setVar("show.agb", 1);
		return t;
	}

	/**
	 * Zeigt das Impressum an.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine impressumAction()
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		t.setVar("show.impressum", 1);
		return t;
	}

	private static class StartLocations
	{
		final int systemID;
		@SuppressWarnings("unused")
		final int orderLocationID;
		final HashMap<Integer, StartLocation> minSysDistance;

		StartLocations(int systemID, int orderLocationID, HashMap<Integer, StartLocation> minSysDistance)
		{
			this.systemID = systemID;
			this.orderLocationID = orderLocationID;
			this.minSysDistance = minSysDistance;
		}
	}

	private static class StartLocation
	{
		int orderLocationID;
		int distance;

		StartLocation(int orderLocationID, int distance)
		{
			this.orderLocationID = orderLocationID;
			this.distance = distance;
		}
	}

	private StartLocations getStartLocation()
	{
		org.hibernate.Session db = getDB();

		int systemID = 0;
		int orderLocationID = 0;
		int mindistance = 99999;
		HashMap<Integer, StartLocation> minsysdistance = new HashMap<>();

		List<?> systems = db.createQuery("from StarSystem order by id asc").list();
		for (Object system1 : systems)
		{
			StarSystem system = (StarSystem) system1;
			Location[] locations = system.getOrderLocations();

			for (int i = 0; i < locations.length; i++)
			{
				int dist = 0;
				int count = 0;
				Iterator<?> distiter = db.createQuery("SELECT sqrt((:x-x)*(:x-x)+(:y-y)*(:y-y)) FROM Base WHERE owner.id = 0 AND system = :system AND klasse.id = 1 ORDER BY sqrt((:x-x)*(:x-x)+(:y-y)*(:y-y))")
						.setInteger("x", locations[i].getX())
						.setInteger("y", locations[i].getY())
						.setInteger("system", system.getID())
						.setMaxResults(15)
						.iterate();

				while (distiter.hasNext())
				{
					dist += (Double) distiter.next();
					count++;
				}

				if (count < 15)
				{
					continue;
				}

				if (!minsysdistance.containsKey(system.getID()) || (minsysdistance.get(system.getID()).distance > dist))
				{
					minsysdistance.put(system.getID(), new StartLocation(i, dist));

					if (mindistance > dist)
					{
						mindistance = dist;
						systemID = system.getID();
						orderLocationID = i;
					}
				}
			}
		}
		return new StartLocations(systemID, orderLocationID, minsysdistance);
	}

	private boolean register(TemplateEngine t, String username, String email, int race, StarSystem system, String key, ConfigValue keys)
	{
		Session db = getDB();

		if ("".equals(username) || "".equals(email))
		{
			return false;
		}

		User user1 = (User) db.createQuery("from User where un = :username")
				.setString("username", username)
				.setMaxResults(1)
				.uniqueResult();
		User user2 = (User) db.createQuery("from User where email = :email")
				.setString("email", email)
				.setMaxResults(1)
				.uniqueResult();

		if (user1 != null)
		{
			t.setVar("show.register.msg.wrongname", 1);
			return false;
		}
		if (user2 != null)
		{
			t.setVar("show.register.msg.wrongemail", 1);
			return false;
		}
		if (!Rassen.get().rasse(race).isPlayable())
		{
			t.setVar("show.register.msg.wrongrace", 1);
			return false;
		}

		boolean needkey = false;
		if (keys.getValue().indexOf('*') == -1)
		{
			needkey = true;
		}

		if (needkey && !keys.getValue().contains("<" + key + ">"))
		{
			t.setVar("show.register.msg.wrongkey", 1);
			return false;
		}

		List<StarSystem> systems = Common.cast(db.createQuery("from StarSystem").list());

		if ((system == null) || (system.getOrderLocations().length == 0))
		{
			t.setBlock("_PORTAL", "register.systems.listitem", "register.systems.list");
			t.setBlock("_PORTAL", "register.systemdesc.listitem", "register.systemdesc.list");

			StartLocations locations = getStartLocation();
			t.setVar("register.system.id", locations.systemID,
					"register.system.name", system != null ? system.getName() : "",
					"show.register.choosesystem", 1);

			for (StarSystem sys : systems)
			{
				if ((sys.getOrderLocations().length > 0) && locations.minSysDistance.containsKey(sys.getID()))
				{
					t.setVar("system.id", sys.getID(),
							"system.name", sys.getName(),
							"system.selected", (sys.getID() == locations.systemID),
							"system.description", Common._text(sys.getDescription()));

					t.parse("register.systems.list", "register.systems.listitem", true);
					t.parse("register.systemdesc.list", "register.systemdesc.listitem", true);
				}
			}

			return true;
		}

		if (needkey)
		{
			String[] keylist = keys.getValue().replace("\r\n", "\n").split("\n");
			HashMap<String, String> parameters = new HashMap<>();
			int pos;
			for (pos = 0; pos < keylist.length; pos++)
			{
				if (keylist[pos].indexOf("<" + key + ">") == 0)
				{
					if (keylist[pos].length() > ("<" + key + ">").length())
					{
						String[] params = keylist[pos].substring(("<" + key + ">").length()).split(",");

						for (String param : params)
						{
							String[] aParam = param.split("=");
							parameters.put(aParam[0], aParam[1]);
						}
					}

					break;
				}
			}

			if (parameters.containsKey("race") && (Integer.parseInt(parameters.get("race")) != race))
			{
				t.setVar("show.register.msg.wrongrace", 1);
				return false;
			}
			String[] newKeyList = new String[keylist.length - 1];
			if (pos != 0)
			{
				System.arraycopy(keylist, 0, newKeyList, 0, pos);
			}
			if (pos != keylist.length - 1)
			{
				System.arraycopy(keylist, pos + 1, newKeyList, pos, keylist.length - pos - 1);
			}

			keys.setValue(Common.implode("\n", newKeyList));
		}

		String password = Common.md5(Integer.toString(ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE)));
		String enc_pw = Common.md5(password);

		int maxid = (Integer) db.createQuery("SELECT max(id) FROM User").iterate().next();
		int newid = maxid + 1;

		int ticks = getContext().get(ContextCommon.class).getTick();

		String history = "Kolonistenlizenz erworben am " + Common.getIngameTime(ticks) + " [" + Common.date("d.m.Y H:i:s") + "]";

		User newuser = new User(username, enc_pw, race, history, new Cargo(), email);

		// Startgeld festlegen
		newuser.setKonto(BigInteger.valueOf(50000));

		// Schiffe erstellen
		StartLocations locations = getStartLocation();
		Location[] orderlocs = system.getOrderLocations();
		Location orderloc = orderlocs[locations.minSysDistance.get(system.getID()).orderLocationID];

		Base base = (Base) db.createQuery("from Base where klasse.id=1 and owner.id=0 and system=:sys order by sqrt((:x-x)*(:x-x)+(:y-y)*(:y-y)) ")
				.setInteger("sys", system.getID())
				.setInteger("x", orderloc.getX())
				.setInteger("y", orderloc.getY())
				.setMaxResults(1)
				.uniqueResult();

		erstelleStartBasis(db, newuser, base);

		Nebel nebel = (Nebel) db.createQuery("from Nebel where loc.system=:sys and type<3 order by sqrt((:x-loc.x)*(:x-loc.x)+(:y-loc.y)*(:y-loc.y))*(mod(type+1,3)+1)*3")
				.setInteger("sys", system.getID())
				.setInteger("x", base.getX())
				.setInteger("y", base.getY())
				.setMaxResults(1)
				.uniqueResult();

		if (race == 1)
		{
			SectorTemplateManager.getInstance().useTemplate(db, "ORDER_TERRANER", base.getLocation(), newid);
			SectorTemplateManager.getInstance().useTemplate(db, "ORDER_TERRANER_TANKER", nebel.getLocation(), newid);
		}
		else
		{
			SectorTemplateManager.getInstance().useTemplate(db, "ORDER_VASUDANER", base.getLocation(), newid);
			SectorTemplateManager.getInstance().useTemplate(db, "ORDER_VASUDANER_TANKER", nebel.getLocation(), newid);
		}

		//Willkommens-PM versenden
		User source = (User) db.get(User.class, configService.getValue(WellKnownConfigValue.REGISTER_PM_SENDER));
		PM.send(source, newid, "Willkommen bei Drifting Souls 2",
				"[font=arial]Herzlich willkommen bei Drifting Souls 2.\n" +
						"Diese PM wird automatisch an alle neuen Spieler versandt, um\n" +
						"ihnen Hilfsquellen fuer den relativ komplizierten Einstieg zu\n" +
						"nennen.\n" +
						" Falls Probleme auftreten sollten, gibt es:\n" +
						"- eine Wiki ([url=http://wiki.drifting-souls.net/]Wiki zu Drifing-Souls[/url])\n" +
						"- Informationen via Menue/Technische Datenbank\n" +
						"- das Forum via Menue/Technische Datenbank oder [url=http://ds.rnd-it.de]Forum-Link[/url]\n" +
						"- einen IRC-Chat (Server: irc.euirc.net / Chan #ds2 oder\n" +
						"- #ds-help) - und die Moeglichkeit via PM an die ID -16 Fragen zu stellen.\n" +
						"\n" +
						"\n" +
						"Viel Spass noch bei DS wuenschen dir die Admins[/font]");

		t.setVar("show.register.msg.ok", 1,
				"register.newid", newid);

		Common.copyFile(Configuration.getAbsolutePath() + "data/logos/user/0.gif",
				Configuration.getAbsolutePath() + "data/logos/user/" + newid + ".gif");

		versendeRegistrierungsEmail(username, email, password);

		return true;
	}

	private void versendeRegistrierungsEmail(String username, String email, String password)
	{
		String message = "Hallo {username},\n" +
				"du hast dich als \"{username}\" angemeldet. Dein Passwort lautet \"{password}\" (ohne \\\"\\\"). Im Spiel heisst du noch Kolonist. Dies sowie das Passwort kannst du aber unter \"Optionen\" aendern.\n" +
				"\n" +
				"Das Admin-Team wuenscht einen angenehmen Aufenthalt in DS\n" +
				"Gruss Guzman\n" +
				"Admin\n" +
				"{date} Serverzeit";
		message = message.replace("{username}", username);
		message = message.replace("{password}", password);
		message = message.replace("{date}", Common.date("H:i j.m.Y"));

		Common.mail(email, "Anmeldung bei Drifting Souls 2", message);
	}

	private void erstelleStartBasis(Session db, User newuser, Base base)
	{
		String[] baselayoutStr = configService.getValue(WellKnownConfigValue.REGISTER_BASELAYOUT).split(",");
		Integer[] activebuildings = new Integer[baselayoutStr.length];
		Integer[] baselayout = new Integer[baselayoutStr.length];
		int bewohner = 0;
		int arbeiter = 0;

		for (int i = 0; i < baselayoutStr.length; i++)
		{
			baselayout[i] = Integer.parseInt(baselayoutStr[i]);

			if (baselayout[i] != 0)
			{
				activebuildings[i] = 1;
				Building building = Building.getBuilding(baselayout[i]);
				bewohner += building.getBewohner();
				arbeiter += building.getArbeiter();
			}
			else
			{
				activebuildings[i] = 0;
			}
		}

		// Alte Gebaeude entfernen
		Integer[] bebauung = base.getBebauung();
		for (Integer aBebauung : bebauung)
		{
			if (aBebauung == 0)
			{
				continue;
			}

			Building building = Building.getBuilding(aBebauung);
			building.cleanup(getContext(), base, aBebauung);
		}

		BaseType basetype = (BaseType) db.get(BaseType.class, 1);
		//User newuser = (User)getDB().get(User.class, newid);

		base.setEnergy(base.getMaxEnergy());
		base.setOwner(newuser);
		base.setBebauung(baselayout);
		base.setActive(activebuildings);
		base.setArbeiter(arbeiter);
		base.setBewohner(bewohner);
		base.setWidth(basetype.getWidth());
		base.setHeight(basetype.getHeight());
		base.setMaxCargo(basetype.getCargo());
		base.setCargo(new Cargo(Cargo.Type.AUTO, configService.getValue(WellKnownConfigValue.REGISTER_BASECARGO)));
		base.setCore(null);
		base.setUnits(new TransientUnitCargo());
		base.setCoreActive(false);
		base.setAutoGTUActs(new ArrayList<>());

		for (Offizier offi : Offizier.getOffiziereByDest(base))
		{
			offi.setOwner(base.getOwner());
		}

		for (Integer aBaselayout : baselayout)
		{
			if (aBaselayout > 0)
			{
				Building building = Building.getBuilding(aBaselayout);
				building.build(base, aBaselayout);
			}
		}
	}

	/**
	 * Registriert einen neuen Spieler. Falls keine Daten eingegeben wurden,
	 * wird die GUI zum registrieren angezeigt.
	 *
	 * @param username der Benutzername des Accounts
	 * @param race Die Rasse des Accounts
	 * @param email Die Email-Adresse
	 * @param key Der Registrierungssschluessel
	 * @param system Das Startsystem
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine registerAction(String username, int race, String email, String key, StarSystem system)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);

		boolean showform;

		String disableregister = configService.getValue(WellKnownConfigValue.DISABLE_REGISTER);
		if (!"".equals(disableregister))
		{
			t.setVar("show.register.registerdisabled", 1,
					"register.registerdisabled.msg", Common._text(disableregister));

			return t;
		}

		ConfigValue keys = configService.get(WellKnownConfigValue.KEYS);
		boolean needkey = false;
		if (keys.getValue().indexOf('*') == -1)
		{
			needkey = true;
		}

		t.setVar("register.username", username,
				"register.email", email,
				"register.needkey", needkey,
				"register.key", key,
				"register.race", race,
				"register.system.id", system != null ? system.getID() : 1,
				"register.system.name", (system != null ? system.getName() : ""));

		showform = !register(t, username, email, race, system, key, keys);

		if (showform)
		{
			t.setBlock("_PORTAL", "register.rassen.listitem", "register.rassen.list");
			t.setBlock("_PORTAL", "register.rassendesc.listitem", "register.rassendesc.list");

			int first = -1;

			for (Rasse rasse : Rassen.get())
			{
				if (rasse.isPlayable())
				{
					t.setVar("rasse.id", rasse.getId(),
							"rasse.name", rasse.getName(),
							"rasse.selected", (first == -1 ? 1 : 0),
							"rasse.description", Common._text(rasse.getDescription()));

					if (first == -1)
					{
						first = rasse.getId();
					}

					t.parse("register.rassen.list", "register.rassen.listitem", true);
					t.parse("register.rassendesc.list", "register.rassendesc.listitem", true);
				}
			}

			t.setVar("show.register", 1,
					"register.rassen.selected", first);
		}

		return t;
	}

	/**
	 * Loggt einen Spieler ein. Falls keine Daten angegeben wurden,
	 * wird die GUI zum einloggen angezeigt.
	 *
	 * @param username Der Benutzername
	 * @param password Das Passwort
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine loginAction(String username, String password, String rememberMe)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);

		if (!username.isEmpty() && !password.isEmpty())
		{
			try
			{
				this.authManager.login(username, password, Boolean.parseBoolean(rememberMe));

				doLogin(t);

				return t;
			}
			catch (LoginDisabledException e)
			{
				t.setVar("show.login.logindisabled", 1,
						"login.logindisabled.msg", Common._text(e.getMessage()));

				return t;
			}
			catch (AccountInVacationModeException e)
			{
				t.setVar(
						"show.login.vacmode", 1,
						"login.vacmode.dauer", e.getDauer(),
						"login.vacmode.username", username,
						"login.vacmode.password", password);

				return t;
			}
			catch (WrongPasswordException e)
			{
				t.setVar("show.msg.login.wrongpassword", 1);
			}
			catch (AccountDisabledException e)
			{
				t.setVar("show.login.msg.accdisabled", 1);
			}
			catch (TickInProgressException e)
			{
				t.setVar("show.login.msg.tick", 1);
			}
			catch (AuthenticationException e)
			{
				// EMPTY
			}
		}

		t.setVar("show.login", 1,
				"login.username", username);

		zeigeNewsListeAn(t, false);

		return t;
	}

	private void doLogin(TemplateEngine t)
	{
		t.setVar("show.login.msg.ok", 1);

		getResponse().redirectTo("ds?module=main&action=default");
	}

	/**
	 * Ermoeglicht das Absenden einer Anfrage zur Deaktivierung des Vac-Modus.
	 *
	 * @param username Der Benutzername
	 * @param pw Das Passwort
	 * @param reason Der Grund fuer eine vorzeitige Deaktivierung
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine loginVacmodeDeakAction(String username, String pw, String reason)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		org.hibernate.Session db = getDB();

		User user = (User) db.createQuery("from User where un=:username")
				.setString("username", username)
				.uniqueResult();

		String encPw = Common.md5(pw);

		if (user == null || !encPw.equals(user.getPassword()))
		{
			t.setVar("show.login.vacmode.msg.accerror", 1);
			return t;
		}

		PM.sendToAdmins(user, "VACMODE-DEAK",
				"[VACMODE-DEAK]\nMY ID: " + user.getId() + "\nREASON:\n" + reason, 0);

		t.setVar("show.login.vacmode.msg.send", 1);

		return t;
	}

	/**
	 * Allows players, which are remembered by ds to login directly.
	 */
	@Action(ActionType.DEFAULT)
	public void reloginAction()
	{
		getResponse().redirectTo("ds?module=main&action=default");
	}

	/**
	 * Zeigt die News an.
	 *
	 * @param archiv <code>true</code>, falls alte News angezeigt werden sollen
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction(boolean archiv)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);

		if (this.authManager.isRemembered())
		{
			t.setVar("is.logged.in", 1);
		}

		zeigeNewsListeAn(t, archiv);

		return t;
	}

	private void zeigeNewsListeAn(TemplateEngine t, boolean archiv)
	{
		org.hibernate.Session db = getDB();
		t.setVar(
				"show.news", 1,
				"show.overview", !archiv,
				"show.news.archiv", archiv);

		t.setBlock("_PORTAL", "news.listitem", "news.list");

		List<NewsEntry> allnews = Common.cast(db.createQuery("FROM NewsEntry ORDER BY date DESC")
											  .setMaxResults(archiv ? 100 : 2)
											  .list());
		for (NewsEntry news : allnews)
		{
			t.setVar("news.date", Common.date("d.m.Y H:i", news.getDate()),
					"news.title", news.getTitle(),
					"news.author", news.getAuthor(),
					"news.text", Common._text(news.getNewsText()));
			t.parse("news.list", "news.listitem", true);
		}
	}
}

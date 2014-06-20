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

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.ComNetService;
import net.driftingsouls.ds2.server.entities.GuiHelpText;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Version;
import net.driftingsouls.ds2.server.framework.ViewMessage;
import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Controller;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Das Hauptframe von DS.
 *
 * @author Christopher Jung
 */
@Module(name = "main")
public class MainController extends Controller
{
	private static final String SCRIPT_FORUM = "http://forum.drifting-souls.net/phpbb3/";

	private Version version;
	private TemplateViewResultFactory templateViewResultFactory;
	private ConfigService configService;

	@Autowired
	public MainController(Version version, TemplateViewResultFactory templateViewResultFactory, ConfigService configService)
	{
		this.version = version;
		this.templateViewResultFactory = templateViewResultFactory;
		this.configService = configService;
	}

	/**
	 * Persistiert die Notizen eines Benutzers.
	 *
	 * @return Die JSON-Nachricht ueber den Erfolg des speicherns
	 */
	@Action(ActionType.AJAX)
	public ViewMessage speicherNotizen(String notizen)
	{
		User user = (User) getUser();
		user.setUserValue(WellKnownUserValue.TBLORDER_MAIN_NOTIZEN, notizen.trim());

		return ViewMessage.success("gespeichert");
	}

	@ViewModel
	public static class Status
	{
		public boolean pm;
		public boolean comNet;
		public String version;
	}

	/**
	 * Ermittelt ein Statusupdate fuer die Oberflaeche z.B.
	 * mit der Info ob ungelesene PMs vorliegen.
	 *
	 */
	@Action(ActionType.AJAX)
	public Status statusUpdateAction()
	{
		User user = (User) this.getUser();
		org.hibernate.Session db = getDB();

		Status status = new Status();

		int pmcount = ((Number) db.createQuery("select count(*) from PM where empfaenger= :user and gelesen=0")
				.setEntity("user", user)
				.iterate().next()).intValue();
		status.pm = pmcount > 0;
		status.comNet = new ComNetService().hatAktiverUserUngeleseneComNetNachrichten();
		status.version = version.getVersion();

		return status;
	}

	/**
	 * Gibt zu einer Seite den Hilfetext zurueck.
	 *
	 * @throws IOException
	 */
	@Action(ActionType.AJAX)
	public void getHelpText(GuiHelpText page) throws IOException
	{
		if (page != null)
		{
			getResponse().getWriter().append(Common._text(page.getText()));
		}
	}

	/**
	 * Generiert das Hauptframe.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction()
	{
		User user = (User) getUser();
		TemplateEngine t = templateViewResultFactory.createFor(this);
		org.hibernate.Session db = getDB();

		t.setVar("SCRIPT_FORUM", SCRIPT_FORUM);

		t.setVar(
				"user.npc", user.hasFlag(UserFlag.ORDER_MENU),
				"user.adminSichtbar", hasPermission(WellKnownAdminPermission.SICHTBAR),
				"admin.showconsole", hasPermission(WellKnownAdminPermission.CONSOLE),
				"user.notizen", user.getUserValue(WellKnownUserValue.TBLORDER_MAIN_NOTIZEN));

		t.setBlock("_MAIN", "bases.listitem", "bases.list");

		List<?> baseList = db.createQuery("from Base where owner= :user order by system,x,y")
				.setEntity("user", user)
				.list();
		for (Object aBaseList : baseList)
		{
			Base base = (Base) aBaseList;

			t.setVar(
					"base.id", base.getId(),
					"base.name", base.getName(),
					"base.klasse", base.getKlasse().getId(),
					"base.location", base.getLocation().displayCoordinates(false));

			t.parse("bases.list", "bases.listitem", true);
		}

		return t;
	}

	@ViewModel
	public class VersionInformation {
		public String build;
		public String commit;
		public String buildTime;
		public String buildUrl;
	}

	@Action(ActionType.AJAX)
	public VersionInformation loadVersionInfo()
	{
		VersionInformation info = new VersionInformation();
		info.commit = version.getVersion();
		info.buildTime = version.getBuildTime();
		info.build = version.getBuild();
		info.buildUrl = configService.getValue(WellKnownConfigValue.BAMBOO_URL)+"browse/"+version.getBuild();
		return info;
	}

	@Action(ActionType.AJAX)
	public String loadLastCommits() throws IOException
	{
		String stashUrl = configService.getValue(WellKnownConfigValue.STASH_URL);
		if( !stashUrl.endsWith("/") )
		{
			stashUrl += "/";
		}
		String urlStr = String.format("%srest/api/1.0/projects/%s/repos/%s/commits",
				stashUrl,
				configService.getValue(WellKnownConfigValue.STASH_PROJECT_NAME),
				configService.getValue(WellKnownConfigValue.STASH_REPO_NAME));
		if( version.isVersioned() )
		{
			urlStr += "?until="+version.getVersion();
		}
		URL url = new URL(urlStr);


		try( Reader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8) ) {
			return IOUtils.readLines(reader).stream().collect(Collectors.joining("\n"));
		}
	}
}

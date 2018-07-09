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
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.GuiHelpText;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.framework.*;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.services.ComNetService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;

/**
 * Das Hauptframe von DS.
 *
 * @author Christopher Jung
 */
@Module(name = "main")
public class MainController extends Controller {
	private static final String SCRIPT_FORUM = "http://ds.rnd-it.de/";

	private Version version;
	private TemplateViewResultFactory templateViewResultFactory;

	@Autowired
	public MainController(Version version, TemplateViewResultFactory templateViewResultFactory) {
		this.version = version;
		this.templateViewResultFactory = templateViewResultFactory;
	}

	/**
	 * Persistiert die Notizen eines Benutzers.
	 *
	 * @return Die JSON-Nachricht ueber den Erfolg des speicherns
	 */
	@Action(ActionType.AJAX)
	public ViewMessage speicherNotizen(String notizen) {
		User user = (User) getUser();
		user.setUserValue(WellKnownUserValue.TBLORDER_MAIN_NOTIZEN, notizen.trim());

		return ViewMessage.success("gespeichert");
	}

	@ViewModel
	public static class Status {
		public boolean pm;
		public boolean comNet;
		public String version;
	}

	/**
	 * Ermittelt ein Statusupdate fuer die Oberflaeche z.B.
	 * mit der Info ob ungelesene PMs vorliegen.
	 */
	@Action(ActionType.AJAX)
	public Status statusUpdateAction() {
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
	public void getHelpText(GuiHelpText page) throws IOException {
		if (page != null) {
			getResponse().getWriter().append(Common._text(page.getText()));
		}
	}

	/**
	 * Generiert das Hauptframe.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction() {
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

		@SuppressWarnings("unchecked")
		List<Base> baseList = db.createQuery("from Base where owner= :user order by system,x,y")
				.setEntity("user", user)
				.list();
		for (Base base : baseList) {
			t.setVar(
					"base.id", base.getId(),
					"base.name", base.getName(),
					"base.klasse", base.getKlasse().getId(),
					"base.location", base.getLocation().displayCoordinates(false));

			t.parse("bases.list", "bases.listitem", true);
		}

		return t;
	}
}

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
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.services.ComNetService;
import net.driftingsouls.ds2.server.services.LocationService;
import net.driftingsouls.ds2.server.services.UserValueService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.IOException;
import java.util.*;

/**
 * Das Hauptframe von DS.
 *
 * @author Christopher Jung
 */
@Module(name = "main")
public class MainController extends Controller {
	private static final String SCRIPT_FORUM = "http://ds.rnd-it.de/";

	private final Version version;
	private final TemplateViewResultFactory templateViewResultFactory;
	private final BBCodeParser bbCodeParser;
	private final UserValueService userValueService;
	private final LocationService locationService;

	@PersistenceContext
	private EntityManager em;

	@Autowired
	public MainController(Version version, TemplateViewResultFactory templateViewResultFactory, BBCodeParser bbCodeParser, UserValueService userValueService, LocationService locationService) {
		this.version = version;
		this.templateViewResultFactory = templateViewResultFactory;
		this.bbCodeParser = bbCodeParser;
		this.userValueService = userValueService;
		this.locationService = locationService;
	}

	/**
	 * Persistiert die Notizen eines Benutzers.
	 *
	 * @return Die JSON-Nachricht ueber den Erfolg des speicherns
	 */
	@Action(ActionType.AJAX)
	public ViewMessage speicherNotizen(String notizen) {
		User user = (User) getUser();
		userValueService.setUserValue(user, WellKnownUserValue.TBLORDER_MAIN_NOTIZEN, notizen.trim());

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

		Status status = new Status();

		long pmcount = em.createQuery("select count(*) from PM where empfaenger= :user and gelesen=0", Long.class)
				.setParameter("user", user)
				.getSingleResult();
		status.pm = pmcount > 0;
		status.comNet = new ComNetService().hatAktiverUserUngeleseneComNetNachrichten();
		status.version = version.getVersion();

		return status;
	}

	/**
	 * Gibt zu einer Seite den Hilfetext zurueck.
	 */
	@Action(ActionType.AJAX)
	public void getHelpText(GuiHelpText page) throws IOException {
		if (page != null) {
			getResponse().getWriter().append(Common._text(bbCodeParser, page.getText()));
		}
	}

	/**
	 * Generiert das Hauptframe.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction() {
		User user = (User) getUser();
		TemplateEngine t = templateViewResultFactory.createFor(this);

		t.setVar("SCRIPT_FORUM", SCRIPT_FORUM);

		t.setVar(
				"user.npc", user.hasFlag(UserFlag.ORDER_MENU),
				"user.adminSichtbar", hasPermission(WellKnownAdminPermission.SICHTBAR),
				"admin.showconsole", hasPermission(WellKnownAdminPermission.CONSOLE),
				"user.notizen", userValueService.getUserValue(user, WellKnownUserValue.TBLORDER_MAIN_NOTIZEN),
				"user.battle", isUserInBattle(user),
				"user.sound.mute", userValueService.getUserValue(user, WellKnownUserValue.GAMEPLAY_USER_SOUNDS_MUTE),
				"user.sound.volume", userValueService.getUserValue(user, WellKnownUserValue.GAMEPLAY_USER_SOUNDS_VOLUME) / 100.0d);//Skalierung auf 0.0-1.0

		t.setBlock("_MAIN", "bases.listitem", "bases.list");

		List<Base> baseList = em.createQuery("from Base where owner= :user order by system,x,y", Base.class)
				.setParameter("user", user)
				.getResultList();
		for (Base base : baseList) {
			t.setVar(
					"base.id", base.getId(),
					"base.name", base.getName(),
					"base.klasse", base.getKlasse().getId(),
					"base.location", locationService.displayCoordinates(base.getLocation(), false));

			t.parse("bases.list", "bases.listitem", true);
		}

		return t;
	}

	private Boolean isUserInBattle(User user)
	{
		Set<User> commanderSet = new LinkedHashSet<>();
		commanderSet.add(user);
		boolean isInBattle;

		String query = "from Battle " +
				"where commander1 in :commanders or commander2 in :commanders ";

		//hat der Benutzer eine ally, dann haeng das hier an
		if (user.getAlly() != null)
		{
			query += " or ally1 = :ally or ally2 = :ally";
		}

		Query battleQuery = em.createQuery(query)
				.setParameter("commanders", commanderSet);

		if (user.getAlly() != null)
		{
			battleQuery = battleQuery.setParameter("ally", user.getAlly());
		}

		battleQuery.setMaxResults(1);
		isInBattle = !battleQuery.getResultList().isEmpty();

		return isInBattle;
	}
}

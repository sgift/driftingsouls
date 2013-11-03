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

import java.util.List;
import java.util.Set;

import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.config.Medals;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserRank;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateController;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ValidierungException;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import org.apache.commons.lang.StringUtils;

/**
 * Zeigt das Profil eines Benutzers an.
 *
 * @author Christopher Jung
 */
@Module(name = "userprofile")
public class UserProfileController extends TemplateController
{
	/**
	 * Konstruktor.
	 *
	 * @param context Der zu verwendende Kontext
	 */
	public UserProfileController(Context context)
	{
		super(context);

		setTemplate("userprofile.html");

		setPageTitle("Profil");
	}

	private void validiereBenutzer(User benutzer)
	{
		if ((benutzer == null) || (benutzer.hasFlag(User.FLAG_HIDE) && !hasPermission("user", "versteckteSichtbar")))
		{
			throw new ValidierungException("Ihnen ist kein Benutzer unter der angegebenen ID bekannt", Common.buildUrl("default", "module", "ueber"));
		}
	}

	/**
	 * Setzt die Beziehung des Users mit dem aktuell angezeigtem User.
	 *
	 * @param ausgewaehlterBenutzer Die ID des anzuzeigenden Benutzers
	 * @param relation Die neue Beziehung. 1 fuer feindlich, 2 fuer freundlich und neural bei allen anderen Werten
	 */
	@Action(ActionType.DEFAULT)
	public void changeRelationAction(@UrlParam(name = "user") User ausgewaehlterBenutzer, User.Relation relation)
	{
		validiereBenutzer(ausgewaehlterBenutzer);

		User user = (User) getUser();
		TemplateEngine t = getTemplateEngine();

		if (ausgewaehlterBenutzer.getId() == user.getId())
		{
			redirect();
			return;
		}

		user.setRelation(ausgewaehlterBenutzer.getId(), relation);
		t.setVar("userprofile.message", "Beziehungsstatus ge&auml;ndert");

		redirect();
	}

	/**
	 * Setzt die Beziehung aller User der Ally des aktiven Users mit dem aktuell angezeigtem User.
	 * Die Operation kann nur vom Allianzpraesidenten ausgefuehrt werden.
	 *
	 * @param ausgewaehlterBenutzer Die ID des anzuzeigenden Benutzers
	 * @param relation Die neue Beziehung. 1 fuer feindlich, 2 fuer freundlich und neural bei allen anderen Werten
	 */
	@Action(ActionType.DEFAULT)
	public void changeRelationAllyAction(@UrlParam(name = "user") User ausgewaehlterBenutzer, User.Relation relation)
	{
		validiereBenutzer(ausgewaehlterBenutzer);

		User user = (User) getUser();
		TemplateEngine t = getTemplateEngine();

		if (user.getAlly() == null)
		{
			addError("Sie sind in keiner Allianz");
			redirect();
			return;
		}

		if (user.getAlly() == ausgewaehlterBenutzer.getAlly())
		{
			addError("Sie befinden sich in der selben Allianz");
			redirect();
			return;
		}

		User allypresi = user.getAlly().getPresident();
		if (allypresi.getId() != user.getId())
		{
			addError("Sie sind nicht der Pr&auml;sident der Allianz");
			redirect();
			return;
		}

		List<User> allymemberList = user.getAlly().getMembers();
		for (User auser : allymemberList)
		{
			auser.setRelation(ausgewaehlterBenutzer.getId(), relation);
		}

		t.setVar("userprofile.message", "Beziehungsstatus ge&auml;ndert");

		redirect();
	}

	/**
	 * Zeigt die Daten des angegebenen Benutzers an.
	 *
	 * @param ausgewaehlterBenutzer Die ID des anzuzeigenden Benutzers
	 */
	@Action(ActionType.DEFAULT)
	public void defaultAction(@UrlParam(name = "user") User ausgewaehlterBenutzer)
	{
		validiereBenutzer(ausgewaehlterBenutzer);

		TemplateEngine t = getTemplateEngine();
		User user = (User) getUser();

		ausgewaehlterBenutzer.setTemplateVars(t);

		if (ausgewaehlterBenutzer.getAlly() != null)
		{
			Ally ally = ausgewaehlterBenutzer.getAlly();
			t.setVar("user.ally.name", Common._title(ally.getName()),
					"user.ally.id", ally.getId());

			String pstatus = "";
			if (ally.getPresident() == ausgewaehlterBenutzer)
			{
				pstatus = "<span style=\"font-weight:bold; font-style:italic\">" + Common._plaintitle(ally.getPname()) + "</span>";
			}

			if (ausgewaehlterBenutzer.getAllyPosten() != null)
			{
				String postenname = ausgewaehlterBenutzer.getAllyPosten().getName();
				t.setVar("user.ally.position", (pstatus.length() != 0 ? pstatus + ", " : "") + Common._plaintitle(postenname));
			}
			else
			{
				t.setVar("user.ally.position", pstatus);
			}
		}

		if ((user.getAlly() != null) && (user.getAlly() != ausgewaehlterBenutzer.getAlly()))
		{
			if (user.getAlly().getPresident() == user)
			{
				t.setVar("user.allyrelationchange", 1);
			}
		}

		if (user.getId() != ausgewaehlterBenutzer.getId())
		{
			if ((user.getAlly() == null) || (user.getAlly() != ausgewaehlterBenutzer.getAlly()))
			{
				User.Relation relation = user.getRelation(ausgewaehlterBenutzer.getId());

				if (relation == User.Relation.ENEMY)
				{
					t.setVar("relation.enemy", 1);
				}
				else if (relation == User.Relation.NEUTRAL)
				{
					t.setVar("relation.neutral", 1);
				}
				else
				{
					t.setVar("relation.friend", 1);
				}
			}
		}

		t.setVar("user.name", Common._title(ausgewaehlterBenutzer.getName()),
				"user.rasse.name", Rassen.get().rasse(ausgewaehlterBenutzer.getRace()).getName(),
				"user.rang", Medals.get().rang(ausgewaehlterBenutzer.getRang()),
				"user.signupdate", (ausgewaehlterBenutzer.getSignup() > 0 ? Common.date("d.m.Y H:i:s", ausgewaehlterBenutzer.getSignup()) : "schon immer"));

		npcRangAnzeigen(ausgewaehlterBenutzer, t, user);

		// Beziehung
		beziehungZumBenutzerAnzeigen(ausgewaehlterBenutzer, t, user);

		// Vacstatus
		int vaccount = ausgewaehlterBenutzer.getVacationCount();
		int wait4vac = ausgewaehlterBenutzer.getWait4VacationCount();

		if (vaccount > 0 && wait4vac <= 0)
		{
			t.setVar("user.vacstatus", "aktiv");
		}
		else
		{
			t.setVar("user.vacstatus", "-");
		}

		// Faction

		// History
		historieAnzeigen(ausgewaehlterBenutzer, t);

		// Orden
		ordenAnzeigen(ausgewaehlterBenutzer, t);
	}

	private void beziehungZumBenutzerAnzeigen(User ausgewaehlterBenutzer, TemplateEngine t, User user)
	{
		String relname = "neutral";
		String relcolor = "#c7c7c7";
		if (user.getId() != ausgewaehlterBenutzer.getId())
		{
			User.Relation relation = ausgewaehlterBenutzer.getRelation(user.getId());
			switch (relation)
			{
				case ENEMY:
					relname = "feindlich";
					relcolor = "#E00000";
					break;
				case FRIEND:
					relname = "freundlich";
					relcolor = "#00E000";
					break;
			}
		}

		t.setVar("user.relation", relname,
				"user.relation.color", relcolor);
	}

	private void historieAnzeigen(User ausgewaehlterBenutzer, TemplateEngine t)
	{
		t.setBlock("_USERPROFILE", "history.listitem", "history.list");
		if (ausgewaehlterBenutzer.getHistory().length() != 0)
		{
			String[] history = StringUtils.split(StringUtils.replace(ausgewaehlterBenutzer.getHistory(), "\r\n", "\n"), "\n");

			for (String aHistory : history)
			{
				t.setVar("history.line", Common._title(aHistory, new String[0]));

				t.parse("history.list", "history.listitem", true);
			}
		}
	}

	private void ordenAnzeigen(User ausgewaehlterBenutzer, TemplateEngine t)
	{
		t.setBlock("_USERPROFILE", "medals.listitem", "medals.list");

		if (ausgewaehlterBenutzer.getMedals().length() != 0)
		{
			int[] medals = Common.explodeToInt(";", ausgewaehlterBenutzer.getMedals());

			for (int i = 0; i < medals.length; i++)
			{
				int medal = medals[i];
				if (Medals.get().medal(medal) == null)
				{
					continue;
				}
				t.setVar("medal.index", i,
						"medal.image", Medals.get().medal(medal).getImage(Medal.IMAGE_NORMAL),
						"medal.image.highlight", Medals.get().medal(medal).getImage(Medal.IMAGE_HIGHLIGHT));

				t.parse("medals.list", "medals.listitem", true);
			}
		}
	}

	private void npcRangAnzeigen(User ausgewaehlterBenutzer, TemplateEngine t, User user)
	{
		t.setBlock("_USERPROFILE", "user.npcrang", "user.npcrang.list");
		if (ausgewaehlterBenutzer.getId() == user.getId())
		{
			for (UserRank rang : user.getOwnRanks())
			{
				if (rang.getRank() < 0)
				{
					continue;
				}
				t.setVar("npcrang", rang.getRankGiver().getOwnGrantableRank(rang.getRank()),
						"npcrang.npc", Common._title(rang.getRankGiver().getName()));

				t.parse("user.npcrang.list", "user.npcrang", true);
			}
		}
		else
		{
			// IDs der Ranggeber
			int ownGiverId;
			int foreignGiverId;
			// Sets aller Raenge
			Set<UserRank> ownRanks = user.getOwnRanks();
			Set<UserRank> foreignRanks = ausgewaehlterBenutzer.getOwnRanks();
			// fur jeden eigenen Rang mit den fremden abgleichen ob er den gleichen Ranggeber hat
			for (UserRank ownRank : ownRanks)
			{
				// hab ich selbst einen Rang? andernfalls brauch ich gar nicht schauen
				if (ownRank.getRank() > 0)
				{
					// holen wir uns die ID vom Ranggeber
					ownGiverId = ownRank.getRankGiver().getId();
					// nun alle fremden Ränge durchgehen und vergleichen
					for (UserRank foreignRank : foreignRanks)
					{
						// Ranggeber holen
						foreignGiverId = foreignRank.getRankGiver().getId();
						// Ranggeber identisch und Fremdling hat selbst einen Rang, dann weiter
						if (ownGiverId == foreignGiverId && foreignRank.getRank() > 0)
						{
							// zeige den Rang an
							t.setVar("npcrang", foreignRank.getRankGiver().getOwnGrantableRank(foreignRank.getRank()),
									"npcrang.npc", Common._title(foreignRank.getRankGiver().getName()));

							// user.getOwnGrantableRanks()

							t.parse("user.npcrang.list", "user.npcrang", true);
						}
					}
				}
			}
		}
	}
}

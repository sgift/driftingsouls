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

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ValidierungException;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Ansicht eines bestimmten Offiziers. Die Ansicht bietet eine Uebersicht
 * ueber die Daten des Offiziers. Der Spieler hat die Moeglichkeit
 * den Namen des Offiziers zu aendern.
 *
 * @author Christopher Jung
 */
@Module(name = "choff")
public class ChoffController extends TemplateGenerator
{
	/**
	 * Konstruktor.
	 *
	 * @param context Der zu verwendende Kontext
	 */
	public ChoffController(Context context)
	{
		super(context);

		setTemplate("choff.html");

		setPageTitle("Offizier");
	}

	private void validiereOffizier(Offizier offizier)
	{
		User user = (User) getUser();

		if (offizier == null)
		{
			throw new ValidierungException("Der angegebene Offizier ist ung&uuml;ltig", Common.buildUrl("default", "module", "ueber"));
		}

		if (offizier.getOwner() != user)
		{
			throw new ValidierungException("Dieser Offizier untersteht nicht ihrem Kommando", Common.buildUrl("default", "module", "ueber"));
		}
	}

	/**
	 * Benennt einen Offizier um.
	 *
	 * @param name Der neue Name des Offiziers
	 */
	@Action(ActionType.DEFAULT)
	public void renameAction(Offizier off, String name)
	{
		validiereOffizier(off);

		TemplateEngine t = getTemplateEngine();

		if (name.length() != 0)
		{
			int MAX_NAME_LENGTH = 60; //See db/offiziere_create.sql
			if (name.length() > MAX_NAME_LENGTH)
			{
				t.setVar("choff.message", "<span style=\"color:red\">Der eingegebene Name ist zu lang (maximal " + MAX_NAME_LENGTH + " Zeichen)</span>");
			}
			else
			{
				off.setName(name);
				t.setVar("choff.message", "Der Name wurde in " + Common._plaintitle(name) + " ge&auml;ndert");
			}
		}
		else
		{
			t.setVar("choff.message", "<span style=\"color:red\">Sie m&uuml;ssen einen Namen angeben</span>");
		}

		redirect();
	}

	@Action(ActionType.DEFAULT)
	public void defaultAction(Offizier off)
	{
		validiereOffizier(off);

		TemplateEngine t = getTemplateEngine();

		t.setVar("offizier.id", off.getID(),
				"offizier.name", Common._plaintitle(off.getName()),
				"offizier.picture", off.getPicture(),
				"offizier.ability.ing", off.getAbility(Offizier.Ability.ING),
				"offizier.ability.waf", off.getAbility(Offizier.Ability.WAF),
				"offizier.ability.nav", off.getAbility(Offizier.Ability.NAV),
				"offizier.ability.sec", off.getAbility(Offizier.Ability.SEC),
				"offizier.ability.com", off.getAbility(Offizier.Ability.COM),
				"offizier.special", off.getSpecial().getName(),
				"base.id", off.getStationiertAufBasis() != null ? off.getStationiertAufBasis().getId() : 0,
				"ship.id", off.getStationiertAufSchiff() != null ? off.getStationiertAufSchiff().getId() : 0);
	}


}

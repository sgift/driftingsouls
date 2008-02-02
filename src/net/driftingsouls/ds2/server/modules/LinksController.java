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

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;

/**
 * Die Menueleiste
 * @author Christopher Jung
 *
 */
public class LinksController extends TemplateGenerator {
	private static final String SCRIPT_FORUM = "http://forum.drifting-souls.net/phpbb3/";
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public LinksController(Context context) {
		super(context);
		
		setTemplate("links.html");
		setDisableDefaultCSS(true);
		setDisableDebugOutput(true);
	}

	@Override
	protected boolean validateAndPrepare(String action) {
		getTemplateEngine().setVar("SCRIPT_FORUM", SCRIPT_FORUM);
		
		return true;
	}

	/**
	 * Prueft, ob der Spieler eine neue PM hat, welche noch nicht gelesen wurde
	 *
	 */
	@Action(ActionType.AJAX)
	public void hasNewPmAjaxAct() {
		User user = (User)this.getUser();
		Database db = getDatabase();
		
		int pmcount = db.first("SELECT count(*) `count` FROM transmissionen WHERE empfaenger='",user.getId(),"' AND gelesen='0'").getInt("count");
		if( pmcount > 0 ) {
			getResponse().getContent().append("1");
		}
		else {
			getResponse().getContent().append("0");
		}
	}
	
	/**
	 * Zeigt die Menueleiste an
	 */
	@Action(ActionType.DEFAULT)
	@Override
	public void defaultAction() {
		User user = (User)getUser();
		
		getTemplateEngine().setVar(
				"user.npc"		, user.hasFlag( User.FLAG_ORDER_MENU ),
				"user.admin"	, (user.getAccessLevel() >= 30) );
	}
}

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

import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;

/**
 * Zeigt alle Objekte an, welche zu einem Suchbegriff passen
 * @author Christopher Jung
 *
 * @urlparam String search Der Suchbegriff
 */
public class SearchController extends TemplateGenerator {
	private static final int MAX_OBJECTS = 25;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public SearchController(Context context) {
		super(context);

		setTemplate("search.html");
		
		addBodyParameter("style", "background-image: url('"+Configuration.getSetting("URL")+"data/interface/border/border_background.gif')");
		setDisableDebugOutput(true);
		setDisablePageMenu(true);
		
		parameterString("search");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		return true;	
	}

	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {		
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();

		final String search = getString("search");
		
		if( search.isEmpty() ) {
			return;
		}
		
		if( search.length() < 2 ) {
			t.setVar("objects.termtoshort", 1);
			return;
		}
		
		t.setVar("search", search);
		
		t.setBlock("_SEARCH", "base.listitem", "none");				 			
		t.setBlock("_SEARCH", "ship.listitem", "none");
		t.setBlock("_SEARCH", "user.listitem", "none");

		int count = 0;
		
		/*
			Basen
		*/
		List baseList = db.createQuery("from Base where owner= :user and name like :search")
			.setEntity("user", getUser())
			.setString("search", "%"+search+"%")
			.setMaxResults(MAX_OBJECTS-count)
			.list();
		for( Iterator iter=baseList.iterator(); iter.hasNext(); ) {
			Base base = (Base)iter.next();
			
			t.setVar(	"base.id",		base.getId(),
						"base.name",	Common._plaintitle(base.getName()),
						"base.location",	base.getLocation());

			t.parse("objects.list", "base.listitem", true);
			
			count++;
		}
		
		if( count < MAX_OBJECTS ) {
			/*
				Schiffe
			*/
			List shipList = db.createQuery("from Ship as s left join fetch s.modules where s.owner= :user and s.name like :search")
				.setEntity("user", getUser())
				.setString("search", "%"+search+"%")
				.setMaxResults(MAX_OBJECTS-count)
				.list();
			for( Iterator iter=shipList.iterator(); iter.hasNext(); ) {
				Ship ship = (Ship)iter.next();
				
				t.setVar(	"ship.id",		ship.getId(),
							"ship.name",	Common._plaintitle(ship.getName()),
							"ship.type.name",	ship.getTypeData().getNickname(),
							"ship.type.picture",	ship.getTypeData().getPicture(),
							"ship.location",	ship.getLocation());
		
				t.parse("objects.list", "ship.listitem", true);
				
				count++;
			}
		}
		
		if( count < MAX_OBJECTS ) {
			/*
				User
			*/
			List userList = db.createQuery("from User where plainname like :search")
				.setString("search", "%"+search+"%")
				.setMaxResults(MAX_OBJECTS-count)
				.list();
			for( Iterator iter=userList.iterator(); iter.hasNext(); ) {
				User user = (User)iter.next();
				
				t.setVar(	"user.id",		user.getId(),
							"user.name",	Common._title(user.getName()));
		
				t.parse("objects.list", "user.listitem", true);
				
				count++;
			}
		}
		
		if( count >= MAX_OBJECTS ) {
			t.setVar("objects.tomany", 1);
		}
	}
}

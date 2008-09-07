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
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;

/**
 * Zeigt alle wichtigen Objekte in einem System an wie z.B.
 * eigene Basen, Sprungpunkte usw.
 * @author Christopher Jung
 *
 * @urlparam Integer system Die ID des Sternensystems
 */
public class ImpObjectsController extends TemplateGenerator {
	private int system;
	private boolean viewableSystem;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public ImpObjectsController(Context context) {
		super(context);
		
		this.viewableSystem = true;
		
		setTemplate("impobjects.html");
		
		addBodyParameter("style", "background-image: url('"+Configuration.getSetting("URL")+"data/interface/border/border_background.gif')");
		setDisableDebugOutput(true);
		
		parameterNumber("system");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		User user = (User)getUser();
		int sys = getInteger("system");
		
		if( sys == 0 ) {
			sys = 1;
		}
		else if( Systems.get().system(sys) == null ) {
			sys = 1;
		}

		if( (Systems.get().system(sys).getAccess() == StarSystem.AC_ADMIN) && !user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) ) {
			viewableSystem = false;
		} 
		else if( (Systems.get().system(sys).getAccess() == StarSystem.AC_NPC) && !user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) && !user.hasFlag( User.FLAG_VIEW_SYSTEMS ) ) {
			viewableSystem = false;
		}
		
		system = sys;
		
		return true;	
	}

	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {		
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		
		t.setVar(	"global.sysname",	Systems.get().system(system).getName(),
				 	"global.sysid",		system );
		
		t.setBlock("_IMPOBJECTS", "jn.listitem", "jn.list");				 			
		t.setBlock("_IMPOBJECTS", "gtuposten.listitem", "gtuposten.list");
		
		if( viewableSystem ) {	 			
			/*
				Sprungpunkte
			*/
		
			List jnList = db.createQuery("from JumpNode where system=?  and hidden=0")
				.setInteger(0, system)
				.list();
			for( Iterator iter=jnList.iterator(); iter.hasNext(); ) {
				JumpNode node = (JumpNode)iter.next();
				
				t.setVar(	"jn.x",			node.getX(),
						  	"jn.y",			node.getY(),
						  	"jn.name",		node.getName(),
						 	"jn.target",	node.getSystemOut(),
							"jn.targetname",	Systems.get().system(node.getSystemOut()).getName() );

				t.parse("jn.list", "jn.listitem", true);
			}
		
			/*
				Handelsposten
			*/
		
			List postenList = db.createQuery("from Ship where id>0 and owner=? and system=? and locate('tradepost',status)!=0")
				.setInteger(0, Faction.GTU)
				.setInteger(1, system)
				.list();
			for( Iterator iter=postenList.iterator(); iter.hasNext(); ) {
				Ship posten = (Ship)iter.next();
				
				t.setVar(	"gtuposten.x",		posten.getX(),
							"gtuposten.y",		posten.getY(),
							"gtuposten.name",	posten.getName() );

				t.parse("gtuposten.list", "gtuposten.listitem", true);
			}
		}
		
		/*
			Basen
		*/
		t.setBlock("_IMPOBJECTS", "base.listitem", "base.list");
		
		List baseList = db.createQuery("from Base where owner=? and system=?")
			.setEntity(0, getUser())
			.setInteger(1, system)
			.list();
		for( Iterator iter=baseList.iterator(); iter.hasNext(); ) {
			Base base = (Base)iter.next();
			
			t.setVar(	"base.x",		base.getX(),
						"base.y",		base.getY(),
						"base.name",	base.getName() );

			t.parse("base.list", "base.listitem", true);
		}
	}
}

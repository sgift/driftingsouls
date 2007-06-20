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

import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Zeigt alle wichtigen Objekte in einem System an wie z.B.
 * eigene Basen, Sprungpunkte usw.
 * @author Christopher Jung
 *
 * @urlparam Integer system Die ID des Sternensystems
 */
public class ImpObjectsController extends DSGenerator {
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
		User user = getUser();
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
	public void defaultAction() {		
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		t.set_var(	"global.sysname",	Systems.get().system(system).getName(),
				 	"global.sysid",		system );
		
		t.set_block("_IMPOBJECTS", "jn.listitem", "jn.list");				 			
		t.set_block("_IMPOBJECTS", "gtuposten.listitem", "gtuposten.list");
		
		if( viewableSystem ) {	 			
			/*
				Sprungpunkte
			*/
		
			SQLQuery jn = db.query("SELECT x,y,systemout,name FROM jumpnodes WHERE system=",system," AND hidden=0");
			while( jn.next() ) {
				t.set_var(	"jn.x",			jn.getInt("x"),
						  	"jn.y",			jn.getInt("y"),
						  	"jn.name",		jn.getString("name"),
						 	"jn.target",	jn.getInt("systemout"),
							"jn.targetname",	Systems.get().system(jn.getInt("systemout")).getName() );

				t.parse("jn.list", "jn.listitem", true);
			}
			jn.free();
		
			/*
				Handelsposten
			*/
		
			SQLQuery posten = db.query("SELECT x,y,name FROM ships WHERE id>0 AND owner=",Faction.GTU," AND system=",system," AND LOCATE('tradepost',status)");
			while( posten.next() ) {
				t.set_var(	"gtuposten.x",		posten.getInt("x"),
							"gtuposten.y",		posten.getInt("y"),
							"gtuposten.name",	posten.getString("name") );

				t.parse("gtuposten.list", "gtuposten.listitem", true);
			}
			posten.free();
		}
		
		/*
			Basen
		*/
		t.set_block("_IMPOBJECTS", "base.listitem", "base.list");
		
		SQLQuery base = db.query("SELECT x,y,name FROM bases WHERE owner=",getUser().getID()," AND system=",system);
		while( base.next() ) {
			t.set_var(	"base.x",		base.getInt("x"),
						"base.y",		base.getInt("y"),
						"base.name",	base.getString("name") );

			t.parse("base.list", "base.listitem", true);
		}
		base.free();
	}
}

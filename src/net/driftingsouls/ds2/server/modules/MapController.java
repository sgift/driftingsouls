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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.config.Rassen;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Zeigt die Sternenkarte an
 * @author Christopher Jung
 *
 * @urlparam Integer sys Die ID des anzuzeigenden Systems
 * @urlparam Integer loadmap Falls != 0 wird die Sternenkarte geladen
 */
public class MapController extends TemplateGenerator {
	private static final Log log = LogFactory.getLog(MapController.class);
	
	private int system = 1;
	private boolean showSystem = true;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public MapController(Context context) {
		super(context);
		
		parameterNumber("sys");
		parameterNumber("loadmap");
		
		setTemplate("map.html");
		
		setPageTitle("Sternenkarte");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		int sys = getInteger("sys");
		
		showSystem = true;
		
		if( this.getInteger("loadmap") == 0 ) {
			showSystem = false;	
		}
		
		if( sys == 0 ) {
			t.setVar("map.message", "Bitte w&auml;hlen sie ein System aus:" );
			sys = 1;
			showSystem = false; //Zeige das System nicht an
		}
		else if( Systems.get().system(sys) == null ) {
			t.setVar("map.message", "&Uuml;ber dieses System liegen keine Informationen vor");
			sys = 1;
			showSystem = false; //Zeige das System nicht an
		}

		if( (Systems.get().system(sys).getAccess() == StarSystem.AC_ADMIN) && !user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) ) {
			t.setVar("map.message", "Sie haben keine entsprechenden Karten - Ihnen sind bekannt:");
			sys = 1;
			showSystem = false; //Zeige das System nicht an
		} 
		else if( (Systems.get().system(sys).getAccess() == StarSystem.AC_NPC) && !user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) && !user.hasFlag( User.FLAG_VIEW_SYSTEMS ) ) {
			t.setVar("map.message", "Sie haben keine entsprechenden Karten - Ihnen sind bekannt:");
			sys = 1;
			showSystem = false; //Zeige das System nicht an
		}
		
		this.system = sys;
		
		t.setVar(	"map.showsystem",	showSystem,
					"map.system",		sys,
					"global.datapath",	Configuration.getSetting("URL") );
		
		return true;
	}

	/**
	 * Zeigt die Sternenkarte an
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		
		t.setBlock("_MAP", "systems.listitem", "systems.list");

		for( StarSystem system : Systems.get() ) {
			String systemAddInfo = " ";
	
			if( (system.getAccess() == StarSystem.AC_ADMIN) && user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) ) {
				systemAddInfo += "[admin]";
			}
			else if( (system.getAccess() == StarSystem.AC_NPC) && (user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) || user.hasFlag( User.FLAG_VIEW_SYSTEMS ) ) ) {
				systemAddInfo += "[hidden]";		
			} 
			else if( (system.getAccess() == StarSystem.AC_ADMIN) || (system.getAccess() == StarSystem.AC_NPC) ) {
				continue;
			}

			t.setVar(	"system.name",		system.getName(),
						"system.id",		system.getID(),
						"system.addinfo",	systemAddInfo,
						"system.selected",	(system.getID() == this.system) );
	
			t.parse("systems.list", "systems.listitem", true);
		}
	
		t.setBlock("_MAP", "jumpnodes.listitem", "jumpnodes.list");
			
		if( !this.showSystem ) {
			return;
		}
						
		List<?> nodeList = db.createQuery("from JumpNode where system= :sys and hidden=0 order by id")
			.setInteger("sys", system)
			.list();
		for( Iterator<?> iter=nodeList.iterator(); iter.hasNext(); )
		{
			JumpNode node = (JumpNode)iter.next();
			
			String blocked = "";
			if( node.isGcpColonistBlock() && Rassen.get().rasse(user.getRace()).isMemberIn(0) )
			{
				blocked = " - blockiert";
			}
			
			t.setVar(	"jumpnode.x",			node.getX(),
						"jumpnode.y",			node.getY(),
						"jumpnode.name",		node.getName(),
						"jumpnode.systemout",	node.getSystemOut(),
						"jumpnode.blocked",		blocked );
			
			t.parse("jumpnodes.list", "jumpnodes.listitem", true);
		}
		
		String index = "";
		String findex = "";

		File starmapIndex = new File(Configuration.getSetting("ABSOLUTE_PATH")+"java/jstarmap.index");
		if( starmapIndex.exists() )
		{
			try
			{
				BufferedReader b = new BufferedReader(new FileReader(starmapIndex));
				try
				{
					index = b.readLine();
				}
				finally
				{
					b.close();
				}
			}
			catch( IOException e )
			{
				log.warn(e, e);
			}
		}
		
		File frameworkIndex = new File(Configuration.getSetting("ABSOLUTE_PATH")+"java/jframework.index");
		if( frameworkIndex.exists() )
		{
			try
			{
				BufferedReader b = new BufferedReader(new FileReader(frameworkIndex));
				try
				{
					findex = b.readLine();
				}
				finally
				{
					b.close();
				}
			}
			catch( IOException e )
			{
				log.warn(e, e);
			}
		}
		
		t.setVar(	"map.applet.index",		index,
					"map.framework.index",	findex,
					"map.applet.codebase",	Configuration.getSetting("URL")+"java/",
					"map.applet.width",		user.getUserValue("TBLORDER/map/width"),
					"map.applet.height",	user.getUserValue("TBLORDER/map/height") );
	}
}

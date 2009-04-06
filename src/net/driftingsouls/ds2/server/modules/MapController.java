package net.driftingsouls.ds2.server.modules;

import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.Location;
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

import org.springframework.beans.factory.annotation.Autowired;

public class MapController extends TemplateGenerator 
{
	
	private boolean showSystem;
	private int system;
	private Configuration config;

	public MapController(Context context)
	{
		super(context);
		
		parameterNumber("sys");
		parameterNumber("loadmap");
		
		setTemplate("map.html");
		
		setPageTitle("Sternenkarte");
	}
	
    /**
     * Injiziert die DS-Konfiguration.
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config) 
    {
    	this.config = config;
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
					"map.system",		sys );
		
		return true;
	}
	
	/**
	 * Zeigt die Sternenkarte an.
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
		
		StarSystem displayedSystem = Systems.get().system(this.system);
		int width = displayedSystem.getWidth();
		int height = displayedSystem.getHeight();
		
		StringBuilder map = new StringBuilder();
		map.append("<table>");
		for(int y = 0; y < height; y++)
		{
			map.append("<tr>");
			for(int x = 0; x < width; x++)
			{
				map.append("<td>");
				new Location(this.system, x, y);
				map.append("</td>");
			}
			map.append("</tr>");
		}
		map.append("</table>");
	}
}
package net.driftingsouls.ds2.server.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.entities.Ally;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

@Configurable
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
		
		String dataPath = config.get("URL") + "data/starmap/";
		Ally userAlly = user.getAlly();
		
		List<Ship> ships = Common.cast(db.createQuery("from Ship where system=:system")
							 			 .setParameter("system", system)
							 			 .list());
		
		List<Nebel> nebulas = Common.cast(db.createQuery("from Nebel where system=:system")
											.setParameter("system", system)
											.list());
		
		List<JumpNode> nodes = Common.cast(db.createQuery("from JumpNode where system=:system or systemOut=:system")
											 .setParameter("system", system)
											 .list());
		
		List<Base> bases = Common.cast(db.createQuery("from Base where system=:system")
										 .setParameter("system", system)
										 .list());
		
		Map<Location, List<Ship>> shipMap = getShipMap(ships);
		Map<Location, Nebel> nebulaMap = getNebulaMap(nebulas);
		Map<Location, List<JumpNode>> nodeMap = getNodeMap(nodes);
		Map<Location, List<Base>> baseMap = getBaseMap(bases);
		
		StringBuilder map = new StringBuilder();
		
		//X markings
		map.append("<table>");
		map.append("<tr>");
		map.append("<td>x/y</td>");
		for(int x = 1; x < width; x++)
		{
			map.append("<td>");
			map.append(x);
			map.append("</td>");
		}
		map.append("</tr>");
		
		for(int y = 0; y < height; y++)
		{
			map.append("<tr>");
			map.append("<td>");
			map.append(y + 1);
			map.append("</td>");
			for(int x = 0; x < width; x++)
			{
				map.append("<td>");
				Location position = new Location(this.system, x, y);
				
				map.append("<img src=\"" + dataPath);
				
				//Basic image
				Nebel nebula = nebulaMap.get(position);
				if(nebula != null)
				{
					
					map.append(dataPath + nebula.getImage());
				}
				else
				{
					List<Base> positionBases = baseMap.get(position);
					if(positionBases != null && !positionBases.isEmpty())
					{
						Base base = positionBases.get(0);
						map.append(base.getImage(position, user));
					}
					else 
					{
						List<JumpNode> positionNodes = nodeMap.get(position);
						if(positionNodes != null && !positionNodes.isEmpty())
						{
							map.append("jumpnode/jumpnode");
						}
						else
						{
							map.append("space/space");
						}
					}
				}
				
				//Fleet attachment
				List<Ship> sectorShips = shipMap.get(position);
				int ownShips = 0;
				int alliedShips = 0;
				int enemyShips = 0;
				
				if(sectorShips != null && !sectorShips.isEmpty())
				{
					for(Ship ship: sectorShips)
					{
						if(ship.getOwner().equals(user))
						{
							ownShips++;
						}
						else 
						{
							Ally shipAlly = ship.getOwner().getAlly();
							if(shipAlly != null && shipAlly.equals(userAlly))
							{
								alliedShips++;
							}
							else
							{
								enemyShips++;
							}
						}
					}
					
					if(ownShips > 0 || alliedShips > 0)
					{
						if(ownShips > 0)
						{
							map.append("_fo");
						}
						
						if(alliedShips > 0)
						{
							map.append("_fa");
						}
						
						map.append("_fe");
					}
				}
				
				map.append(".png\"/>");
				map.append("</td>");
			}
			map.append("</tr>");
		}
		map.append("</table>");
		
		t.setVar("map.fields", map);
	}

	private Map<Location, List<Base>> getBaseMap(List<Base> bases)
	{
		Map<Location, List<Base>> shipMap = new HashMap<Location, List<Base>>();
		
		for(Base base: bases)
		{
			Location position = base.getLocation();
			if(!shipMap.containsKey(position))
			{
				shipMap.put(position, new ArrayList<Base>());
			}
			
			shipMap.get(position).add(base);
		}
		
		return shipMap;
	}

	private Map<Location, List<JumpNode>> getNodeMap(List<JumpNode> nodes)
	{
		Map<Location, List<JumpNode>> nodeMap = new HashMap<Location, List<JumpNode>>();
		
		for(JumpNode node: nodes)
		{
			Location position;
			if(node.getSystem() == system)
			{
				position = new Location(node.getSystem(), node.getX(), node.getY());
			}
			else
			{
				position = new Location(node.getSystemOut(), node.getXOut(), node.getYOut());
			}
			
			if(!nodeMap.containsKey(position))
			{
				nodeMap.put(position, new ArrayList<JumpNode>());
			}
			
			nodeMap.get(position).add(node);
		}
		
		return nodeMap;
	}

	private Map<Location, Nebel> getNebulaMap(List<Nebel> nebulas)
	{
		Map<Location, Nebel> nebulaMap = new HashMap<Location, Nebel>();
		
		for(Nebel nebula: nebulas)
		{
			nebulaMap.put(nebula.getLocation(), nebula);
		}
		
		return nebulaMap;
	}

	private Map<Location, List<Ship>> getShipMap(List<Ship> ships)
	{
		Map<Location, List<Ship>> shipMap = new HashMap<Location, List<Ship>>();
		
		for(Ship ship: ships)
		{
			Location position = ship.getLocation();
			if(!shipMap.containsKey(position))
			{
				shipMap.put(position, new ArrayList<Ship>());
			}
			
			shipMap.get(position).add(ship);
		}
		
		return shipMap;
	}
}
package net.driftingsouls.ds2.server.modules;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.entities.Ally;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Response;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Zeigt die Sternenkarte eines Systems an.
 * 
 * @author Sebastian Gift
 */
@Configurable
public class MapController extends TemplateGenerator 
{

	private boolean showSystem;
	private int system;
	private Configuration config;

	/**
	 * Legt den MapController an.
	 * 
	 * @param context Der Kontext.
	 */
	public MapController(Context context)
	{
		super(context);

		parameterNumber("sys");
		parameterNumber("loadmap");
		parameterNumber("xstart");
		parameterNumber("xend");
		parameterNumber("ystart");
		parameterNumber("yend");

		setTemplate("map.html");

		setPageTitle("Sternenkarte");
	}

	/**
	 * Injiziert die DS-Konfiguration.
	 * @param config Die DS-Konfiguration
	 */
	@Autowired
	public void setConfiguration(Configuration config) {
		this.config = config;
	}

	@Override
	protected void printHeader(String action) throws IOException 
	{
		//The map uses jquery instead of the default javascript libraries, so
		//we disable the default header and print our own header here

		Response response = getContext().getResponse();
		response.setContentType("text/html", "UTF-8");
		Writer sb = response.getWriter();

		String url = config.get("URL")+"/";
		final BasicUser user = getContext().getActiveUser();
		if( user != null ) 
		{
			url = user.getImagePath();
		}

		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n");
		sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"de\" lang=\"de\">\n");
		sb.append("<head>\n");
		sb.append("<title>Drifting Souls 2</title>\n");
		sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n");
		if( !getDisableDefaultCSS() ) { 
			sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\""+config.get("URL")+"format.css\" />\n");
		}
		sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\""+url+"data/css/ui-darkness/jquery.ui.darkness.css\" />\n");

		sb.append("<script src=\""+url+"data/javascript/jquery.js\" type=\"text/javascript\"></script>\n");
		sb.append("<script src=\""+url+"data/javascript/jquery.ui.js\" type=\"text/javascript\"></script>\n");
		sb.append("<script src=\""+url+"data/javascript/jquery.blockUI.js\" type=\"text/javascript\"></script>\n");

		sb.append("</head>\n");
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
	public void defaultAction() throws IOException
	{
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

		if( !this.showSystem ) 
		{
			return;
		}

		List<?> nodeList = db.createQuery("from JumpNode where system= :sys and hidden=0 order by id")
		.setInteger("sys", system)
		.list();
		for(Iterator<?> iter=nodeList.iterator(); iter.hasNext();)
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

		String dataPath = templateEngine.getVar("global.datadir") + "data/starmap/";
		Ally userAlly = user.getAlly();

		List<Ship> ships = Common.cast(db.createQuery("from Ship where system=:system")
				.setParameter("system", system)
				.list());

		List<Nebel> nebulas = Common.cast(db.createQuery("from Nebel where system=:system")
				.setParameter("system", system)
				.list());

		List<JumpNode> nodes = Common.cast(db.createQuery("from JumpNode where system=:system and hidden=0")
				.setParameter("system", system)
				.list());

		List<Base> bases = Common.cast(db.createQuery("from Base b inner join fetch b.owner where system=:system")
				.setParameter("system", system)
				.list());

		Map<Location, List<Ship>> shipMap = getShipMap(ships);
		Map<Location, Nebel> nebulaMap = getNebulaMap(nebulas);
		Map<Location, List<JumpNode>> nodeMap = getNodeMap(nodes);
		Map<Location, List<Base>> baseMap = getBaseMap(bases);

		Set<Location> scannableLocations = getScannableLocations(shipMap, nebulaMap);

		int xStart = getInteger("xstart");
		int xEnd = getInteger("xend");
		int yStart = getInteger("ystart");
		int yEnd = getInteger("yend");

		//Limit width and height to map size
		if(xStart < 1)
		{
			xStart = 1;
		}

		if(xEnd > width)
		{
			xEnd = width;
		}

		if(yStart < 1)
		{
			yStart = 1;
		}

		if(yEnd > height)
		{
			yEnd = height;
		}

		//Use sensible defaults in case of useless input
		if(yEnd <= yStart)
		{
			yEnd = height;
		}

		if(xEnd <= xStart)
		{
			xEnd = width;
		}

		Writer map = getContext().getResponse().getWriter();
		
		map.append("<table id=\"starmap\">");
		printXLegend(map, xStart, xEnd);

		for(int y = yStart; y <= yEnd; y++)
		{
			map.append("<tr>");
			map.append("<td width=\"25\" height=\"25\">");
			map.append("" + y);
			map.append("</td>");
			for(int x = xStart; x <= xEnd; x++)
			{
				map.append("<td width=\"25\" height=\"25\">");
				map.append("<img width=\"25\" height=\"25\" src=\"" + dataPath);

				Location position = new Location(this.system, x, y);
				boolean scannable = scannableLocations.contains(position);
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
						map.append(base.getImage(position, user, scannable));
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


				if(scannable)
				{

					//Fleet attachment
					List<Ship> sectorShips = shipMap.get(position);
					int ownShips = 0;
					int alliedShips = 0;
					int enemyShips = 0;

					if(sectorShips != null && !sectorShips.isEmpty())
					{
						for(Ship ship: sectorShips)
						{
							User shipOwner = ship.getOwner();
							if(shipOwner.equals(user))
							{
								ownShips++;
							}
							else 
							{
								Ally shipAlly = shipOwner.getAlly();
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

						if(ownShips > 0)
						{
							map.append("_fo");
						}

						if(alliedShips > 0)
						{
							map.append("_fa");
						}

						if(enemyShips > 0)
						{
							map.append("_fe");
						}
					}
					
					map.append(".png\" alt=\"" + x + "/" + y + "\"/>");
				}
				else
				{
					map.append(".png\" alt=\"" + x + "/" + y + "\" class=\"noscan\"/>");
				}

				map.append("</td>");
			}
			map.append("<td width=\"25\" height=\"25\">");
			map.append("" + y);
			map.append("</td>");
			map.append("</tr>");
		}

		printXLegend(map, xStart, xEnd);

		map.append("</table>");
	}

	private void printXLegend(Writer map, int start, int end) throws IOException
	{
		map.append("<tr>");
		map.append("<td width=\"25\" height=\"25\">x/y</td>");
		for(int x = start; x <= end; x++)
		{
			map.append("<td width=\"25\" height=\"25\">");
			map.append("" + x);
			map.append("</td>");
		}
		map.append("</tr>");
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

			int size = base.getSize();
			if(size > 0)
			{
				for(int y = base.getY() - size; y <= base.getY() + size; y++)
				{
					for(int x = base.getX() - size; x <= base.getX() + size; x++)
					{
						Location loc = new Location(system, x, y);

						if( !position.sameSector( 0, loc, base.getSize() ) ) {
							continue;	
						}

						if(!shipMap.containsKey(loc))
						{
							shipMap.put(loc, new ArrayList<Base>());
						}

						shipMap.get(loc).add(0, base); //Big objects are always printed first
					}
				}
			}
			else
			{
				shipMap.get(position).add(base);
			}
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

	private Set<Location> getScannableLocations(Map<Location, List<Ship>> locatedShips, Map<Location, Nebel> nebulas)
	{
		User user = (User)getUser();
		Ally ally = user.getAlly();
		Set<Location> scannableLocations = new HashSet<Location>();

		for(Map.Entry<Location, List<Ship>> sectorShips: locatedShips.entrySet())
		{
			Location position = sectorShips.getKey();
			List<Ship> ships = sectorShips.getValue();

			int scanRange = -1;
			//Find ship with best scanrange
			for(Ship ship: ships)
			{
				//Own ship?
				if(!ship.getOwner().equals(user))
				{
					//See allied scans?
					if(ally != null && ally.getShowLrs())
					{
						//Allied ship?
						Ally ownerAlly = ship.getOwner().getAlly();
						if(ownerAlly == null || !ownerAlly.equals(ally))
						{
							continue;
						}
					}
					else
					{
						continue;
					}
				}

				int shipScanRange = ship.getTypeData().getSensorRange();
				if(shipScanRange > scanRange)
				{
					scanRange = shipScanRange;
				}
			}
			
			//No ship found
			if(scanRange == -1)
			{
				continue;
			}
			
			//Adjust for nebula position
			//TODO: Check, if there's an performant way to bring this part into the Ship class
			if(nebulas.containsKey(position))
			{
				scanRange /= 2;
			}

			//Find sectors scanned from ship
			for(int y = position.getY() - scanRange; y <= position.getY() + scanRange; y++)
			{
				for(int x = position.getX() - scanRange; x <= position.getX() + scanRange; x++)
				{
					Location loc = new Location(system, x, y);

					if(!position.sameSector(scanRange, loc, 0)) 
					{
						continue;	
					}

					//No nebula scan
					if(!nebulas.containsKey(loc) || loc.equals(position))
					{
						scannableLocations.add(loc);
					}
				}
			}
		}
		return scannableLocations;
	}
}

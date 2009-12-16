package net.driftingsouls.ds2.server.modules;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.JumpNode;
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
import net.driftingsouls.ds2.server.map.PlayerField;
import net.driftingsouls.ds2.server.map.PlayerStarmap;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

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
	private StarSystem system;
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
		parameterNumber("x");
		parameterNumber("y");
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
		if(getActionType() != ActionType.DEFAULT)
		{
			return;
		}
		
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
		sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\""+url+"data/css/jquery.ui.darkness.css\" />\n");

		sb.append("<script src=\""+url+"data/javascript/jquery.js\" type=\"text/javascript\"></script>\n");
		sb.append("<script src=\""+url+"data/javascript/jquery.ui.js\" type=\"text/javascript\"></script>\n");
		sb.append("<script src=\""+url+"data/javascript/starmap.js\" type=\"text/javascript\"></script>\n");
		sb.append("</head>\n");
	}

	@Override
	protected boolean validateAndPrepare(String action) {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		int sys = getInteger("sys");

		showSystem = true;

		if( this.getInteger("loadmap") == 0 ) {
			showSystem = false;	
		}
		
		StarSystem system = (StarSystem)db.get(StarSystem.class, sys);
		
		if( sys == 0 ) {
			t.setVar("map.message", "Bitte w&auml;hlen Sie ein System aus:" );
			sys = 1;
			showSystem = false; //Zeige das System nicht an
		}
		else if( system == null ) {
			t.setVar("map.message", "&Uuml;ber dieses System liegen keine Informationen vor");
			sys = 1;
			showSystem = false; //Zeige das System nicht an
		}

		if( (system.getAccess() == StarSystem.AC_ADMIN) && !user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) ) {
			t.setVar("map.message", "Sie haben keine entsprechenden Karten - Ihnen sind bekannt:");
			sys = 1;
			showSystem = false; //Zeige das System nicht an
		} 
		else if( (system.getAccess() == StarSystem.AC_NPC) && !user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) && !user.hasFlag( User.FLAG_VIEW_SYSTEMS ) ) {
			t.setVar("map.message", "Sie haben keine entsprechenden Karten - Ihnen sind bekannt:");
			sys = 1;
			showSystem = false; //Zeige das System nicht an
		}
		else if( !system.isStarmapVisible() && !user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS )) {
			t.setVar("map.message", "Sie haben keine entsprechenden Karten - Ihnen sind bekannt:");
			sys = 1;
			showSystem = false; //Zeige das System nicht an
		}

		this.system = system;

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

		List<?> systems = db.createQuery("from StarSystem order by id asc").list();
		for( Iterator<?> iter = systems.iterator(); iter.hasNext(); )
		{
			StarSystem system = (StarSystem)iter.next();
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
			else if( !system.isStarmapVisible() && !user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS )) {
				continue;
			}

			t.setVar(	"system.name",		system.getName(),
						"system.id",		system.getID(),
						"system.addinfo",	systemAddInfo,
						"system.selected",	(system.getID() == this.system.getID()) );

			t.parse("systems.list", "systems.listitem", true);
		}

		t.setBlock("_MAP", "jumpnodes.listitem", "jumpnodes.list");

		if( !this.showSystem ) 
		{
			return;
		}

		
		PlayerStarmap content = new PlayerStarmap(db, user, system.getID());
		List<JumpNode> publicNodes = content.getPublicNodes();
		for(JumpNode node: publicNodes)
		{
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

		int width = this.system.getWidth();
		int height = this.system.getHeight();
		
		String dataPath = templateEngine.getVar("global.datadir") + "data/starmap/";

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

				Location position = new Location(this.system.getID(), x, y);
				boolean scannable = content.isScannable(position);
				String sectorImage = dataPath + content.getSectorImage(position);
				
				if(scannable)
				{
					map.append(sectorImage);
					//map.append("\" alt=\"" + x + "/" + y + "\"/ class=\"scan, showsector\" onClick=\"showSector("+this.system.getID()+","+x+","+y+")\">");
					map.append("\" alt=\"" + x + "/" + y + "\"/ class=\"scan, showsector\">");
				}
				else
				{
					map.append(sectorImage);
					map.append("\" alt=\"" + x + "/" + y + "\" class=\"noscan\"/>");
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
		map.append("<div id=\"sectors\"></div>");
	}
	
	/**
	 * Zeigt einen einzelnen Sektor mit allen Details an.
	 */
	@Action(ActionType.AJAX)
	public void sectorAction() throws IOException
	{
		User user = (User)getUser();
		org.hibernate.Session db = getDB();
		
		int system = getInteger("sys");
		int x = getInteger("x");
		int y = getInteger("y");
		
		PlayerStarmap map = new PlayerStarmap(db, user, system);
		PlayerField field = new PlayerField(db, user, new Location(system, x, y), map);
		
		JSONObject json = new JSONObject();
		JSONArray users = new JSONArray();
		for(Map.Entry<User, Map<ShipType, List<Ship>>> owner: field.getShips().entrySet())
		{
			JSONObject jsonUser = new JSONObject();
			jsonUser.accumulate("name", Common._text(owner.getKey().getNickname()));
			JSONArray shiptypes = new JSONArray();
			for(Map.Entry<ShipType, List<Ship>> shiptype: owner.getValue().entrySet())
			{
				JSONObject jsonShiptype = new JSONObject();
				jsonShiptype.accumulate("name", shiptype.getKey().getNickname());
				JSONArray ships = new JSONArray();
				for(Ship ship: shiptype.getValue())
				{
					ships.add(ship.getName());
				}
				jsonShiptype.accumulate("ships", ships);
				shiptypes.add(jsonShiptype);
			}
			jsonUser.accumulate("shiptypes", shiptypes);
			users.add(jsonUser);
		}
		json.accumulate("ships", users);
		
		getResponse().getWriter().append(json.toString());
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
}

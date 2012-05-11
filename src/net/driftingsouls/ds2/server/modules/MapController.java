package net.driftingsouls.ds2.server.modules;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.Version;
import net.driftingsouls.ds2.server.framework.pipeline.Response;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.map.PlayerField;
import net.driftingsouls.ds2.server.map.PlayerStarmap;
import net.driftingsouls.ds2.server.map.PublicStarmap;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.hibernate.FlushMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.sun.imageio.plugins.common.PaletteBuilder;

/**
 * Zeigt die Sternenkarte eines Systems an.
 * 
 * @author Drifting-Souls Team
 */
@Configurable
public class MapController extends TemplateGenerator 
{
	private static final int TILE_SIZE = 20;
	private static final int SECTOR_IMAGE_SIZE = 25;
	
	private boolean showSystem;
	private StarSystem system;
	private int sys;
	private Configuration config;
	private Version version;

	/**
	 * Legt den MapController an.
	 * 
	 * @param context Der Kontext.
	 */
	public MapController(Context context)
	{
		super(context);

		parameterNumber("scanship");
		parameterNumber("sys");
		parameterNumber("x");
		parameterNumber("y");
		parameterNumber("loadmap");
		parameterNumber("xstart");
		parameterNumber("xend");
		parameterNumber("ystart");
		parameterNumber("yend");
		parameterNumber("tileX");
		parameterNumber("tileY");

		setTemplate("map.html");

		setPageTitle("Sternenkarte");
		
		setDisableDebugOutput(true);
	}

	/**
	 * Injiziert die DS-Konfiguration.
	 * @param config Die DS-Konfiguration
	 */
	@Autowired
	public void setConfiguration(Configuration config) {
		this.config = config;
	}
	
	/**
	 * Injiziert die DS-Version.
	 * @param version Die DS-Version
	 */
	@Autowired
	public void setVersion(Version version) {
		this.version = version;
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
			sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"").append(config.get("URL")).append("data/css/v").append(version.getHgVersion()).append("/format.css\" />\n");
		}
		sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"").append(config.get("URL")).append("data/css/v").append(version.getHgVersion()).append("/starmap.css\" />\n");
        sb.append("<script src=\"").append(url).append("data/javascript/v").append(version.getHgVersion()).append("/jquery-1.7.2.min.js\" type=\"text/javascript\"></script>\n");
        sb.append("<script src=\"").append(url).append("data/javascript/v").append(version.getHgVersion()).append("/starmap.js\" type=\"text/javascript\"></script>\n");
        
        sb.append("<script type=\"text/javascript\">\n");
		sb.append("if( parent && parent.setCurrentPage ) {\n");
		sb.append("parent.setCurrentPage('map','Sternenkarte');\n");
		sb.append("parent.completePage();");
		sb.append("}\n");
		sb.append("</script>");
        
		sb.append("</head>\n");
	}

	@Override
	protected boolean validateAndPrepare(String action)
	{
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		sys = getInteger("sys");

		showSystem = this.getInteger("loadmap") != 0;
		
		StarSystem system = (StarSystem)db.get(StarSystem.class, sys);
		
		if( sys == 0 )
		{
			t.setVar("map.message", "Bitte w&auml;hlen Sie ein System aus:" );
			sys = 1;
			showSystem = false; //Zeige das System nicht an
		}
		else if( system == null ||
				( (system.getAccess() == StarSystem.AC_ADMIN) && !user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) ) ||
				( (system.getAccess() == StarSystem.AC_NPC) && !user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) && !user.hasFlag( User.FLAG_VIEW_SYSTEMS ) ) ||
				( !system.isStarmapVisible() && !user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS )))
		{
			t.setVar("map.message", "Sie haben keine entsprechenden Karten - Ihnen sind bekannt:");
			sys = 1;
			showSystem = false; //Zeige das System nicht an
		}

		this.system = system;

		t.setVar(
				"map.showsystem",	showSystem,
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
		
		List<StarSystem> systems = Common.cast(db.createQuery("from StarSystem order by id asc").list());
		for(StarSystem system: systems)
		{
			String systemAddInfo = " ";

			if( (system.getAccess() == StarSystem.AC_ADMIN) && user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) )
			{
				systemAddInfo += "[admin]";
			}
			else if( (system.getAccess() == StarSystem.AC_NPC) && (user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) || user.hasFlag( User.FLAG_VIEW_SYSTEMS ) ) )
			{
				systemAddInfo += "[hidden]";		
			} 
			else if( (system.getAccess() == StarSystem.AC_ADMIN) || (system.getAccess() == StarSystem.AC_NPC) )
			{
				continue;
			}
			else if( !system.isStarmapVisible() && !user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ))
			{
				continue;
			}

			t.setVar(	"system.name",		system.getName(),
						"system.id",		system.getID(),
						"system.addinfo",	systemAddInfo,
						"system.selected",	(system.getID() == sys) );

			t.parse("systems.list", "systems.listitem", true);
		}
	}
	
	/**
	 * Gibt eine einzelne Tile zurueck, entweder aus dem Cache oder, falls nicht vorhanden, neu generiert.
	 * @throws IOException Speicherfehler
	 */
	@Action(ActionType.BINARY)
	public void tileAction() throws IOException
	{
		if( this.system == null )
		{
			getResponse().getWriter().append("ERROR");
			return;
		}
	
		int tileX = getInteger("tileX");
		int tileY = getInteger("tileY");
		
		final File cacheDir = new File(this.config.get("ABSOLUTE_PATH")+"data/starmap/_tilecache/");
		if( !cacheDir.isDirectory() ) {
			cacheDir.mkdir();
		}
		
		File tileCacheFile = new File(this.config.get("ABSOLUTE_PATH")+"data/starmap/_tilecache/"+this.system.getID()+"_"+tileX+"_"+tileY+".png");
		if( !tileCacheFile.isFile() ) {
			createTile(tileCacheFile, tileX, tileY);
		}
		
		InputStream in = new FileInputStream(tileCacheFile);
		try {
			getResponse().setContentType("image/png");
			final OutputStream outputStream = getResponse().getOutputStream();
			try
			{
				IOUtils.copy(in, outputStream);
			}
			finally
			{
				outputStream.close();
			}
		}
		finally {
			in.close();
		}
	}

	private void createTile(File tileCacheFile, int tileX, int tileY) throws IOException
	{
		org.hibernate.Session db = getDB();
		
		PublicStarmap content = new PublicStarmap(db, system.getID());
	
		BufferedImage img = new BufferedImage(TILE_SIZE*25,TILE_SIZE*25,BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		try
		{
			for(int y = 0; y < TILE_SIZE; y++)
			{
				for(int x = 0; x < TILE_SIZE; x++)
				{
					Location position = new Location(this.system.getID(), tileX*TILE_SIZE+x+1, tileY*TILE_SIZE+y+1);
					String sectorImageName = content.getSectorBaseImage(position);
					
					BufferedImage sectorImage = ImageIO.read(new File(this.config.get("ABSOLUTE_PATH")+"data/starmap/"+sectorImageName));
					g.drawImage(sectorImage, 
							x*SECTOR_IMAGE_SIZE, y*SECTOR_IMAGE_SIZE, 
							SECTOR_IMAGE_SIZE, SECTOR_IMAGE_SIZE, 
							null);
				}
			}
		}
		finally
		{
			g.dispose();
		}
		
		final OutputStream outputStream = new FileOutputStream(tileCacheFile);
		try
		{
			ImageIO.write(CustomPaletteBuilder.createIndexedImage(img, 64), "png", outputStream);
			
			outputStream.flush();
		}
		finally
		{
			outputStream.close();
		}
	}
	
	private static final class CustomPaletteBuilder extends PaletteBuilder
	{

		protected CustomPaletteBuilder(RenderedImage src, int size)
		{
			super(src, size);
		}
	
		public static RenderedImage createIndexedImage(RenderedImage img, int colors)
		{
			CustomPaletteBuilder pb = new CustomPaletteBuilder(img, colors);
			pb.buildPalette();
			return pb.getIndexedImage();
		}
	}
	
	/**
	 * Gibt die Kartendaten des gewaehlten Ausschnitts als JSON-Response zurueck.
	 * @throws IOException
	 */
	@Action(ActionType.AJAX)
	public void mapAction() throws IOException {
		JSONObject json = new JSONObject();
		
		if( !this.showSystem ) 
		{
			getResponse().getWriter().append(json.toString());
			return;
		}
		
		org.hibernate.Session db = getDB();
		// Flushmode aendern um autoflushes auf den grossen geladenen Datenmengen zu vermeiden.
		FlushMode oldFlushMode = db.getFlushMode();
		db.setFlushMode(FlushMode.MANUAL);
		try {
			User user = (User)getUser();
			
			JSONObject sysObj = new JSONObject();
			sysObj.accumulate("id", this.system.getID());
			sysObj.accumulate("width", this.system.getWidth());
			sysObj.accumulate("height", this.system.getHeight());
			json.accumulate("system", sysObj);
		
			int width = this.system.getWidth();
			int height = this.system.getHeight();
		
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
			
			PlayerStarmap content = new PlayerStarmap(db, user, system.getID(), new int[] {xStart,yStart,xEnd-xStart,yEnd-yStart});
	
			JSONArray publicNodeArray = new JSONArray();
			for(JumpNode node: content.getPublicNodes())
			{
				String blocked = "";
				if( node.isGcpColonistBlock() && Rassen.get().rasse(user.getRace()).isMemberIn(0) )
				{
					blocked = " - blockiert";
				}
	
				JSONObject nodeObj = new JSONObject();
				nodeObj.accumulate("x", node.getX());
				nodeObj.accumulate("y", node.getY());
				nodeObj.accumulate("name", node.getName());
				nodeObj.accumulate("systemout", node.getSystemOut());
				nodeObj.accumulate("blocked", blocked);
				
				publicNodeArray.add(nodeObj);
			}
			json.accumulate("jumpnodes", publicNodeArray);
	
			
			String dataPath = templateEngine.getVar("global.datadir") + "data/starmap/";
			json.accumulate("dataPath", dataPath);
			
			JSONObject sizeObj = new JSONObject();
			sizeObj.accumulate("minx", xStart);
			sizeObj.accumulate("miny", yStart);
			sizeObj.accumulate("maxx", xEnd);
			sizeObj.accumulate("maxy", yEnd);
			
			json.accumulate("size", sizeObj);
		
			JSONArray locationArray = new JSONArray();
			for(int y = yStart; y <= yEnd; y++)
			{
				for(int x = xStart; x <= xEnd; x++)
				{
					Location position = new Location(this.system.getID(), x, y);
					boolean scannable = content.isScannable(position);			
					String sectorImage = content.getUserSectorBaseImage(position);
					String sectorOverlayImage = content.getSectorOverlayImage(position);
					
					boolean endTag = false;
					
					JSONObject posObj = new JSONObject();
					posObj.accumulate("x", x);
					posObj.accumulate("y", y);
					posObj.accumulate("scan", scannable);
					
					if( sectorImage != null )
					{
						endTag = true;
						posObj.accumulate("bg", sectorImage);
						sectorImage = sectorOverlayImage;
					}
					else if( scannable )
					{
						endTag = true;
						posObj.accumulate("bg", content.getSectorBaseImage(position));
						sectorImage = sectorOverlayImage;
					}
					else if( sectorOverlayImage != null )
					{
						endTag = true;
						sectorImage = sectorOverlayImage;
					}
					
					if( sectorImage != null )
					{
						if(scannable)
						{
							int scannerId = content.getSectorScanner(position).getId();
							
							posObj.accumulate("fg", sectorImage);
							posObj.accumulate("scanner", scannerId);						
						}
						else
						{
							posObj.accumulate("fg", sectorImage);
						}
					}
					
					if( endTag ) {
						locationArray.add(posObj);
					}
				}
			}
			json.accumulate("locations", locationArray);
			
			getResponse().getWriter().append(json.toString());
			
			// Das Anzeigen sollte keine DB-Aenderungen verursacht haben
			db.clear();
		}
		finally {
			db.setFlushMode(oldFlushMode);
		}
	}
	
	/**
	 * Zeigt einen einzelnen Sektor mit allen Details an.
	 * @throws IOException 
	 */
	@Action(ActionType.AJAX)
	public void sectorAction() throws IOException
	{
		User user = (User)getUser();
		org.hibernate.Session db = getDB();
		
		int system = getInteger("sys");
		int x = getInteger("x");
		int y = getInteger("y");
		int shipId = getInteger("scanship");
		
		JSONObject json = new JSONObject();
		JSONArray users = new JSONArray();

		Ship scanShip = (Ship)db.get(Ship.class, shipId);
		if( scanShip == null )
		{
			json.accumulate("users", users);
			getResponse().getWriter().append(json.toString());
			return;
		}
		
		PlayerField field = new PlayerField(db, user, new Location(system, x, y), scanShip);
		for(Map.Entry<User, Map<ShipType, List<Ship>>> owner: field.getShips().entrySet())
		{
			JSONObject jsonUser = new JSONObject();
			jsonUser.accumulate("name", Common._text(owner.getKey().getNickname()));
			jsonUser.accumulate("id", owner.getKey().getId());
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
		json.accumulate("users", users);
		
		getResponse().getWriter().append(json.toString());
	}
}

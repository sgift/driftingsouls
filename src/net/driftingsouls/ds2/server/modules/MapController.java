package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.JSONUtils;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.AngularGenerator;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParamType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParams;
import net.driftingsouls.ds2.server.map.AdminFieldView;
import net.driftingsouls.ds2.server.map.AdminStarmap;
import net.driftingsouls.ds2.server.map.FieldView;
import net.driftingsouls.ds2.server.map.PlayerFieldView;
import net.driftingsouls.ds2.server.map.PlayerStarmap;
import net.driftingsouls.ds2.server.map.PublicStarmap;
import net.driftingsouls.ds2.server.map.TileCache;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Configurable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Zeigt die Sternenkarte eines Systems an.
 *
 * @author Drifting-Souls Team
 */
@Configurable
@Module(name="map")
@UrlParams({
		@UrlParam(name="sys", type=UrlParamType.NUMBER, description = "Die ID des zu landenden Sternensystems"),
		@UrlParam(name="loadmap", type=UrlParamType.NUMBER, description = "1 falls die Kartendaten geladen werden sollen"),
		@UrlParam(name="admin", type=UrlParamType.NUMBER, description = "1 falls die Adminsicht auf die Sternenkarte verwendet werden soll")
})
public class MapController extends AngularGenerator
{
	private boolean showSystem;
	private StarSystem system;
	private int sys;
	private boolean adminView;
	private Ship scanner;

	/**
	 * Legt den MapController an.
	 *
	 * @param context Der Kontext.
	 */
	public MapController(Context context)
	{
		super(context);

		setPageTitle("Sternenkarte");
	}

	@Override
	protected boolean validateAndPrepare(String action)
	{
		User user = (User)getUser();
		org.hibernate.Session db = getDB();
		sys = getInteger("sys");

		showSystem = this.getInteger("loadmap") != 0;

		StarSystem system = (StarSystem)db.get(StarSystem.class, sys);

		if( sys == 0 )
		{
			sys = 1;
			showSystem = false; //Zeige das System nicht an
		}
		else if( system == null || !system.isVisibleFor(user) )
		{
			addError("Sie haben keine entsprechenden Karten - Ihnen sind bekannt:");
			return false;
		}

		this.system = system;
		this.adminView = getInteger("admin") == 1 && user.isAdmin();

		return true;
	}

	@Action(value=ActionType.AJAX)
	public JSONObject speichereSystemkarteAction()
	{
		if( !getUser().isAdmin() )
		{
			return JSONUtils.error("Du bist nicht berechtigt diese Aktion auszuführen");
		}

		org.hibernate.Session db = getDB();

		List<StarSystem> systems = Common.cast(db.createCriteria(StarSystem.class).list());
		for(StarSystem system: systems)
		{
			int x = getRequest().getParameterInt("sys"+system.getID()+"x");
			int y = getRequest().getParameterInt("sys"+system.getID()+"y");

			if( x != 0 || y != 0 )
			{
				system.setMapX(x);
				system.setMapY(y);
			}
		}

		return JSONUtils.success("Systeme gespeichert");
	}

	private JSONObject createResultObj()
	{
		JSONObject result = new JSONObject();
		result.accumulate("system", sys);
		result.accumulate("adminSichtVerfuegbar", getUser().isAdmin());
		result.accumulate("systemkarteEditierbar", getUser().isAdmin());

		return result;
	}

	/**
	 * Zeigt die Sternenkarte an.
	 */
	@Action(value=ActionType.AJAX, readOnly=true)
	public JSONObject systemauswahlAction()
	{
		User user = (User)getUser();
		org.hibernate.Session db = getDB();

		JSONObject result = createResultObj();

		JSONArray systemListObj = new JSONArray();

		List<JumpNode> jumpNodes = Common.cast(db
				.createQuery("from JumpNode jn where "+(!user.isAdmin() ? "jn.hidden=0 and " : "")+"jn.system!=jn.systemOut")
				.list());

		Map<Integer, Ally> systemFraktionen = ermittleDominierendeAllianzen(db);
		Set<Integer> basen = ermittleSystemeMitEigenerBasis(db);
		Set<Integer> schiffe = ermittleSystemeMitEigenenSchiffen(db);

		List<StarSystem> systems = Common.cast(db.createQuery("from StarSystem order by id asc").list());
		for(StarSystem system: systems)
		{
			if( !system.isVisibleFor(user) )
			{
				continue;
			}

			String systemAddInfo = " ";

			if( system.getAccess() == StarSystem.AC_ADMIN )
			{
				systemAddInfo += "[admin]";
			}
			else if( system.getAccess() == StarSystem.AC_NPC )
			{
				systemAddInfo += "[hidden]";
			}

			JSONObject sysObj = new JSONObject();
			sysObj.accumulate("name", system.getName());
			sysObj.accumulate("id", system.getID());
			sysObj.accumulate("addinfo", systemAddInfo);
			sysObj.accumulate("npcOnly", system.getAccess() == StarSystem.AC_NPC );
			sysObj.accumulate("adminOnly", system.getAccess() == StarSystem.AC_ADMIN );
			sysObj.accumulate("mapX", system.getMapX());
			sysObj.accumulate("mapY", system.getMapY());

			// Sprungpunkte
			JSONArray jnListObj = new JSONArray();
			for( JumpNode jn : jumpNodes )
			{
				if( jn.getSystem() == system.getID() )
				{
					jnListObj.add(jn.toJSON());
				}
			}
			sysObj.accumulate("sprungpunkte", jnListObj);

			// Dominierende NPC-Allianzen
			Ally maxAlly = systemFraktionen.get(system.getID());
			if( maxAlly != null )
			{
				JSONObject allyObj = new JSONObject();
				allyObj.accumulate("name", Common._title(maxAlly.getName()));
				allyObj.accumulate("plainname", BBCodeParser.getInstance().parse(maxAlly.getName(), new String[]{"all"}));
				allyObj.accumulate("id", maxAlly.getId());
				sysObj.accumulate("allianz", allyObj);
			}

			// Basen
			sysObj.accumulate("basis", basen.contains(system.getID()));

			// Schiffe
			sysObj.accumulate("schiff", schiffe.contains(system.getID()));

			systemListObj.add(sysObj);
		}
		result.accumulate("systeme", systemListObj);
		return result;
	}

	private Set<Integer> ermittleSystemeMitEigenenSchiffen(Session db)
	{
		List<Integer> result = Common.cast(db
				.createQuery("select s.system from Ship s where s.owner=:user group by s.system")
				.setEntity("user", getUser())
				.list());

		return new HashSet<Integer>(result);
	}

	private Set<Integer> ermittleSystemeMitEigenerBasis(Session db)
	{
		List<Integer> result = Common.cast(db
				.createQuery("select b.system from Base b where b.owner=:user group by b.system")
				.setEntity("user", getUser())
				.list());

		return new HashSet<Integer>(result);
	}

	private Map<Integer,Ally> ermittleDominierendeAllianzen(Session db)
	{
		List<Object[]> data = Common.cast(db
				.createQuery("select s.system,s.owner.ally,sum(s.shiptype.size) " +
						"from Ship s " +
						"where s.status like '%tradepost%' and s.owner.id<0 and s.owner.ally is not null " +
						"group by s.system,s.owner.ally " +
						"order by s.system,count(*)")
				.list());

		Map<Integer,Ally> systeme = new HashMap<Integer,Ally>();
		int currentSys = -1;
		Ally currentAlly = null;
		long maxCount = 0;
		for( Object[] entry : data )
		{
			if( (Integer)entry[0] != currentSys )
			{
				systeme.put(currentSys, currentAlly);
				maxCount = 0;
				currentAlly = null;
				currentSys = (Integer)entry[0];
			}
			if( (Long)entry[2] > maxCount )
			{
				maxCount = (Long)entry[2];
				currentAlly = (Ally)entry[1];
			}
			else if( (Long)entry[2] == maxCount )
			{
				currentAlly = null;
			}
		}
		systeme.put(currentSys, currentAlly);

		return systeme;
	}

	/**
	 * Gibt eine einzelne Tile zurueck, entweder aus dem Cache oder, falls nicht vorhanden, neu generiert.
	 * @throws IOException Speicherfehler
	 */
	@Action(value=ActionType.BINARY, readOnly=true)
	@UrlParams({
			@UrlParam(name="tileX", type=UrlParamType.NUMBER, description = "Die X-Kachel"),
			@UrlParam(name="tileY", type=UrlParamType.NUMBER, description = "Die Y-Kachel")
	})
	public void tileAction() throws IOException
	{
		if( this.system == null )
		{
			getResponse().getWriter().append("ERROR");
			return;
		}

		int tileX = getInteger("tileX");
		int tileY = getInteger("tileY");

		if( tileX < 0 )
		{
			tileX = 0;
		}
		if( tileY < 0 )
		{
			tileY = 0;
		}

		TileCache cache = TileCache.forSystem(this.system);
		File tileCacheFile = cache.getTile(tileX, tileY);

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

	/**
	 * Gibt die Kartendaten des gewaehlten Ausschnitts als JSON-Response zurueck.
	 */
	@Action(value=ActionType.AJAX, readOnly=true)
	@UrlParams({
			@UrlParam(name="xstart",type= UrlParamType.NUMBER),
			@UrlParam(name="xend",type= UrlParamType.NUMBER),
			@UrlParam(name="ystart",type= UrlParamType.NUMBER),
			@UrlParam(name="yend",type= UrlParamType.NUMBER)
	})
	public JSONObject mapAction() {
		JSONObject json = new JSONObject();

		if( !this.showSystem )
		{
			return json;
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

			PublicStarmap content;
			if( this.adminView )
			{
				content = new AdminStarmap(system, user, new int[] {xStart,yStart,xEnd-xStart,yEnd-yStart});
			}
			else {
				content = new PlayerStarmap(user, system, new int[] {xStart,yStart,xEnd-xStart,yEnd-yStart});
			}

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
					boolean scannable = content.isScannbar(position);
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

					if( scannable && content.isHasSectorContent(position)) {
						scanner = content.getScanSchiffFuerSektor(position);

						posObj.accumulate("scanner", scanner != null ? scanner.getId() : -1);
					}

					if( sectorImage != null )
					{
						posObj.accumulate("fg", sectorImage);
					}

					posObj.accumulate("battle", content.isSchlachtImSektor(position));
					posObj.accumulate("roterAlarm", content.isRoterAlarmImSektor(position));

					if( endTag ) {
						locationArray.add(posObj);
					}
				}
			}
			json.accumulate("locations", locationArray);

			// Das Anzeigen sollte keine DB-Aenderungen verursacht haben
			db.clear();

			return json;
		}
		finally {
			db.setFlushMode(oldFlushMode);
		}
	}

	/**
	 * Zeigt einen einzelnen Sektor mit allen Details an.
	 */
	@Action(value=ActionType.AJAX, readOnly=true)
	@UrlParams({
			@UrlParam(name="x", type=UrlParamType.NUMBER, description = "Die X-Koordinate des zu scannenden Sektors"),
			@UrlParam(name="y", type=UrlParamType.NUMBER, description = "Die Y-Koordinate des zu scannenden Sektors"),
			@UrlParam(name="scanship", type=UrlParamType.NUMBER, description = "Die ID des fuer den Scanvorgang zu verwendenden Schiffs")
	})
	public JSONObject sectorAction()
	{
		User user = (User)getUser();
		org.hibernate.Session db = getDB();

		int system = getInteger("sys");
		int x = getInteger("x");
		int y = getInteger("y");
		int shipId = getInteger("scanship");

		JSONObject json = new JSONObject();

		Ship scanShip = (Ship)db.get(Ship.class, shipId);

		final Location loc = new Location(system, x, y);

		FieldView field;
		if( this.adminView )
		{
			field = new AdminFieldView(db, user, loc);
		}
		else {
			field = new PlayerFieldView(db, user, loc, scanShip);
		}

		JSONArray users = exportSectorShips(field);
		json.accumulate("users", users);

		JSONArray baseListObj = new JSONArray();
		for( Base base : field.getBases() )
		{
			JSONObject baseObj = new JSONObject();
			baseObj.accumulate("id", base.getId());
			baseObj.accumulate("name", base.getName());
			baseObj.accumulate("username", Common._title(base.getOwner().getName()));
			baseObj.accumulate("image", base.getBaseImage(loc));
			baseObj.accumulate("imageX", base.getBaseImageOffset(loc)[0]);
			baseObj.accumulate("imageY", base.getBaseImageOffset(loc)[1]);
			baseObj.accumulate("klasse", base.getKlasse());
			baseObj.accumulate("typ", base.getBaseType().getName());
			baseObj.accumulate("eigene", base.getOwner().getId() == user.getId());

			baseListObj.add(baseObj);
		}

		json.accumulate("bases", baseListObj);

		JSONArray jumpNodeListObj = new JSONArray();
		for (JumpNode jumpNode : field.getJumpNodes())
		{
			JSONObject jnObj = new JSONObject();
			jnObj.accumulate("id", jumpNode.getId());
			jnObj.accumulate("name", jumpNode.getName());
			jnObj.accumulate("blocked", jumpNode.isGcpColonistBlock() && Rassen.get().rasse(user.getRace()).isMemberIn( 0 ));
			jumpNodeListObj.add(jnObj);
		}
		json.accumulate("jumpnodes", jumpNodeListObj);

		Nebel nebel = field.getNebel();
		if( nebel != null )
		{
			JSONObject nebelObj = new JSONObject();
			nebelObj.accumulate("type", nebel.getType().getCode());
			nebelObj.accumulate("image", nebel.getImage());
			json.accumulate("nebel", nebelObj);
		}

		JSONArray battleListObj = exportSectorBattles(db, field);
		json.accumulate("battles", battleListObj);

		json.accumulate("subraumspaltenCount", field.getSubraumspalten().size());
		json.accumulate("roterAlarm", field.isRoterAlarm());


		return json;
	}

	private JSONArray exportSectorBattles(Session db, FieldView field)
	{
		JSONArray battleListObj = new JSONArray();
		List<Battle> battles = field.getBattles();
		if( battles.isEmpty() )
		{
			return battleListObj;
		}

		User user = (User)getUser();
		boolean viewable = getContext().hasPermission("schlacht", "alleAufrufbar");

		if( !viewable )
		{
			Map<ShipType, List<Ship>> ships = field.getShips().get(user);
			if( ships != null && !ships.isEmpty() )
			{
				for (ShipType shipType : ships.keySet())
				{
					if( shipType.getShipClass().isDarfSchlachtenAnsehen() )
					{
						viewable = true;
					}
				}
			}
		}

		for (Battle battle : battles)
		{
			JSONObject battleObj = new JSONObject();
			battleObj.accumulate("id", battle.getId());
			battleObj.accumulate("einsehbar", viewable || battle.getSchlachtMitglied(user) != -1);
			JSONArray sideListObj = new JSONArray();

			for( int i=0; i < 2; i++ )
			{
				JSONObject sideObj = new JSONObject();
				sideObj.accumulate("commander", battle.getCommander(i).toJSON());
				if( battle.getAlly(i) != 0 )
				{
					Ally ally = (Ally)db.get(Ally.class, battle.getAlly(i));
					sideObj.accumulate("ally", ally.toJSON());
				}
				sideListObj.add(sideObj);
			}
			battleObj.accumulate("sides", sideListObj);

			battleListObj.add(battleObj);
		}
		return battleListObj;
	}

	private JSONArray exportSectorShips(FieldView field)
	{
		JSONArray users = new JSONArray();
		for(Map.Entry<User, Map<ShipType, List<Ship>>> owner: field.getShips().entrySet())
		{
			JSONObject jsonUser = new JSONObject();
			jsonUser.accumulate("name", Common._text(owner.getKey().getName()));
			jsonUser.accumulate("id", owner.getKey().getId());
			jsonUser.accumulate("race", owner.getKey().getRace());

			boolean ownFleet = owner.getKey().getId() == getUser().getId();
			jsonUser.accumulate("eigener", ownFleet);

			JSONArray shiptypes = new JSONArray();
			for(Map.Entry<ShipType, List<Ship>> shiptype: owner.getValue().entrySet())
			{
				JSONObject jsonShiptype = new JSONObject();
				jsonShiptype.accumulate("id", shiptype.getKey().getId());
				jsonShiptype.accumulate("name", shiptype.getKey().getNickname());
				jsonShiptype.accumulate("picture", shiptype.getKey().getPicture());
				jsonShiptype.accumulate("size", shiptype.getKey().getSize());

				JSONArray ships = new JSONArray();
				for(Ship ship: shiptype.getValue())
				{
					ShipTypeData typeData = ship.getTypeData();
					JSONObject shipObj = new JSONObject();
					shipObj.accumulate("id", ship.getId());
					shipObj.accumulate("name", ship.getName());
					shipObj.accumulate("gedockt", ship.getDockedCount());
					shipObj.accumulate("maxGedockt", typeData.getADocks());
					if( ownFleet )
					{
						shipObj.accumulate("gelandet", ship.getLandedCount());
						shipObj.accumulate("maxGelandet", typeData.getJDocks());

						shipObj.accumulate("energie", ship.getEnergy());
						shipObj.accumulate("maxEnergie", typeData.getEps());

						shipObj.accumulate("ueberhitzung", ship.getHeat());

						int sensorRange = ship.getEffectiveScanRange();
						if( field.getNebel() != null ) {
							sensorRange /= 2;
						}
						shipObj.accumulate("sensorRange", sensorRange);
					}

					if( ship.getFleet() != null )
					{
						shipObj.accumulate("fleet", ship.getFleet().toJSON());
					}

					ships.add(shipObj);
				}
				jsonShiptype.accumulate("ships", ships);
				shiptypes.add(jsonShiptype);
			}
			jsonUser.accumulate("shiptypes", shiptypes);
			users.add(jsonUser);
		}
		return users;
	}
}

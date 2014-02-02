package net.driftingsouls.ds2.server.modules;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import net.driftingsouls.ds2.server.framework.pipeline.generators.AngularController;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ValidierungException;
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
import net.driftingsouls.ds2.server.ships.ShipTypes;
import org.apache.commons.io.IOUtils;
import org.hibernate.FlushMode;
import org.hibernate.Session;

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
@Module(name = "map")
public class MapController extends AngularController
{
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

	public void validiereSystem(StarSystem system)
	{
		User user = (User) getUser();

		if (system == null || !system.isVisibleFor(user))
		{
			throw new ValidierungException("Sie haben keine entsprechenden Karten");
		}
	}

	@Action(value = ActionType.AJAX)
	public JsonElement speichereSystemkarteAction()
	{
		if (!getUser().isAdmin())
		{
			return JSONUtils.error("Du bist nicht berechtigt diese Aktion auszuführen");
		}

		org.hibernate.Session db = getDB();

		List<StarSystem> systems = Common.cast(db.createCriteria(StarSystem.class).list());
		for (StarSystem system : systems)
		{
			int x = getRequest().getParameterInt("sys" + system.getID() + "x");
			int y = getRequest().getParameterInt("sys" + system.getID() + "y");

			if (x != 0 || y != 0)
			{
				system.setMapX(x);
				system.setMapY(y);
			}
		}

		return JSONUtils.success("Systeme gespeichert");
	}

	private JsonObject createResultObj(StarSystem system)
	{
		JsonObject result = new JsonObject();
		result.addProperty("system", system != null ? system.getID() : 1);
		result.addProperty("adminSichtVerfuegbar", getUser().isAdmin());
		result.addProperty("systemkarteEditierbar", getUser().isAdmin());

		return result;
	}

	/**
	 * Zeigt die Sternenkarte an.
	 *
	 * @param sys Das momentan ausgewaehlte Sternensystem
	 */
	@Action(value = ActionType.AJAX, readOnly = true)
	public JsonObject systemauswahlAction(StarSystem sys)
	{
		User user = (User) getUser();
		org.hibernate.Session db = getDB();

		JsonObject result = createResultObj(sys);

		JsonArray systemListObj = new JsonArray();

		List<JumpNode> jumpNodes = Common.cast(db
				.createQuery("from JumpNode jn where " + (!user.isAdmin() ? "jn.hidden=0 and " : "") + "jn.system!=jn.systemOut")
				.list());

		Map<Integer, Ally> systemFraktionen = ermittleDominierendeAllianzen(db);
		Set<Integer> basen = ermittleSystemeMitEigenerBasis(db);
		Set<Integer> schiffe = ermittleSystemeMitEigenenSchiffen(db);

		List<StarSystem> systems = Common.cast(db.createQuery("from StarSystem order by id asc").list());
		for (StarSystem system : systems)
		{
			if (!system.isVisibleFor(user))
			{
				continue;
			}

			String systemAddInfo = " ";

			if (system.getAccess() == StarSystem.AC_ADMIN)
			{
				systemAddInfo += "[admin]";
			}
			else if (system.getAccess() == StarSystem.AC_NPC)
			{
				systemAddInfo += "[hidden]";
			}

			JsonObject sysObj = new JsonObject();
			sysObj.addProperty("name", system.getName());
			sysObj.addProperty("id", system.getID());
			sysObj.addProperty("addinfo", systemAddInfo);
			sysObj.addProperty("npcOnly", system.getAccess() == StarSystem.AC_NPC);
			sysObj.addProperty("adminOnly", system.getAccess() == StarSystem.AC_ADMIN);
			sysObj.addProperty("mapX", system.getMapX());
			sysObj.addProperty("mapY", system.getMapY());

			// Sprungpunkte
			JsonArray jnListObj = new JsonArray();
			for (JumpNode jn : jumpNodes)
			{
				if (jn.getSystem() == system.getID())
				{
					jnListObj.add(jn.toJSON());
				}
			}
			sysObj.add("sprungpunkte", jnListObj);

			// Dominierende NPC-Allianzen
			Ally maxAlly = systemFraktionen.get(system.getID());
			if (maxAlly != null)
			{
				JsonObject allyObj = new JsonObject();
				allyObj.addProperty("name", Common._title(maxAlly.getName()));
				allyObj.addProperty("plainname", BBCodeParser.getInstance().parse(maxAlly.getName(), new String[]{"all"}));
				allyObj.addProperty("id", maxAlly.getId());
				sysObj.add("allianz", allyObj);
			}

			// Basen
			sysObj.addProperty("basis", basen.contains(system.getID()));

			// Schiffe
			sysObj.addProperty("schiff", schiffe.contains(system.getID()));

			systemListObj.add(sysObj);
		}
		result.add("systeme", systemListObj);
		return result;
	}

	private Set<Integer> ermittleSystemeMitEigenenSchiffen(Session db)
	{
		List<Integer> result = Common.cast(db
				.createQuery("select s.system from Ship s where s.owner=:user group by s.system")
				.setEntity("user", getUser())
				.list());

		return new HashSet<>(result);
	}

	private Set<Integer> ermittleSystemeMitEigenerBasis(Session db)
	{
		List<Integer> result = Common.cast(db
				.createQuery("select b.system from Base b where b.owner=:user group by b.system")
				.setEntity("user", getUser())
				.list());

		return new HashSet<>(result);
	}

	private Map<Integer, Ally> ermittleDominierendeAllianzen(Session db)
	{
		List<Object[]> data = Common.cast(db
				.createQuery("select s.system,s.owner.ally,sum(s.shiptype.size) " +
						"from Ship s join s.shiptype st left join s.modules sm " +
						"where (s.status like :flags or sm.flags like :flags or st.flags like :flags) and s.owner.id<0 and s.owner.ally is not null " +
						"group by s.system,s.owner.ally " +
						"order by s.system,count(*)")
				.setParameter("flags", "%" + ShipTypes.SF_TRADEPOST + "%")
				.list());

		Map<Integer, Ally> systeme = new HashMap<>();
		int currentSys = -1;
		Ally currentAlly = null;
		long maxCount = 0;
		for (Object[] entry : data)
		{
			if ((Integer) entry[0] != currentSys)
			{
				systeme.put(currentSys, currentAlly);
				maxCount = 0;
				currentAlly = null;
				currentSys = (Integer) entry[0];
			}
			if ((Long) entry[2] > maxCount)
			{
				maxCount = (Long) entry[2];
				currentAlly = (Ally) entry[1];
			}
			else if ((Long) entry[2] == maxCount)
			{
				currentAlly = null;
			}
		}
		systeme.put(currentSys, currentAlly);

		return systeme;
	}

	/**
	 * Gibt eine einzelne Tile zurueck, entweder aus dem Cache oder, falls nicht vorhanden, neu generiert.
	 *
	 * @param sys Das anzuzeigende Sternensystem
	 * @param tileX Die X-Kachel
	 * @param tileY Die Y-Kachel
	 * @throws IOException Speicherfehler
	 */
	@Action(value = ActionType.BINARY, readOnly = true)
	public void tileAction(StarSystem sys, int tileX, int tileY) throws IOException
	{
		validiereSystem(sys);

		if (tileX < 0)
		{
			tileX = 0;
		}
		if (tileY < 0)
		{
			tileY = 0;
		}

		TileCache cache = TileCache.forSystem(sys);
		File tileCacheFile = cache.getTile(tileX, tileY);

		try (InputStream in = new FileInputStream(tileCacheFile))
		{
			getResponse().setContentType("image/png");
			try (OutputStream outputStream = getResponse().getOutputStream())
			{
				IOUtils.copy(in, outputStream);
			}
		}
	}

	/**
	 * Gibt die Kartendaten des gewaehlten Ausschnitts als JSON-Response zurueck.
	 *
	 * @param sys Das anzuzeigende Sternensystem
	 * @param xstart Die untere Grenze des Ausschnitts auf der X-Achse
	 * @param xend Die obere Grenze des Ausschnitts  auf der X-Achse
	 * @param ystart Die untere Grenze des Ausschnitts auf der Y-Achse
	 * @param yend Die obere Grenze des Ausschnitts  auf der Y-Achse
	 * @param admin {@code true} falls die Adminsicht auf die Sternenkarte verwendet werden soll
	 */
	@Action(value = ActionType.AJAX, readOnly = true)
	public JsonObject mapAction(StarSystem sys, int xstart, int xend, int ystart, int yend, boolean admin)
	{
		validiereSystem(sys);

		JsonObject json = new JsonObject();

		org.hibernate.Session db = getDB();
		// Flushmode aendern um autoflushes auf den grossen geladenen Datenmengen zu vermeiden.
		FlushMode oldFlushMode = db.getFlushMode();
		db.setFlushMode(FlushMode.MANUAL);
		try
		{
			User user = (User) getUser();

			JsonObject sysObj = new JsonObject();
			sysObj.addProperty("id", sys.getID());
			sysObj.addProperty("width", sys.getWidth());
			sysObj.addProperty("height", sys.getHeight());
			json.add("system", sysObj);

			int width = sys.getWidth();
			int height = sys.getHeight();

			//Limit width and height to map size
			if (xstart < 1)
			{
				xstart = 1;
			}

			if (xend > width)
			{
				xend = width;
			}

			if (ystart < 1)
			{
				ystart = 1;
			}

			if (yend > height)
			{
				yend = height;
			}

			//Use sensible defaults in case of useless input
			if (yend <= ystart)
			{
				yend = height;
			}

			if (xend <= xstart)
			{
				xend = width;
			}

			PublicStarmap content;
			if (admin && user.isAdmin())
			{
				content = new AdminStarmap(sys, user, new int[]{xstart, ystart, xend - xstart, yend - ystart});
			}
			else
			{
				content = new PlayerStarmap(user, sys, new int[]{xstart, ystart, xend - xstart, yend - ystart});
			}

			JsonObject sizeObj = new JsonObject();
			sizeObj.addProperty("minx", xstart);
			sizeObj.addProperty("miny", ystart);
			sizeObj.addProperty("maxx", xend);
			sizeObj.addProperty("maxy", yend);

			json.add("size", sizeObj);

			JsonArray locationArray = new JsonArray();
			for (int y = ystart; y <= yend; y++)
			{
				for (int x = xstart; x <= xend; x++)
				{
					Location position = new Location(sys.getID(), x, y);
					boolean scannable = content.isScannbar(position);
					String sectorImage = content.getUserSectorBaseImage(position);
					String sectorOverlayImage = content.getSectorOverlayImage(position);

					boolean endTag = false;

					JsonObject posObj = new JsonObject();
					posObj.addProperty("x", x);
					posObj.addProperty("y", y);
					posObj.addProperty("scan", scannable);

					if (sectorImage != null)
					{
						endTag = true;
						posObj.addProperty("bg", sectorImage);
						sectorImage = sectorOverlayImage;
					}
					else if (scannable)
					{
						endTag = true;
						posObj.add("bg", new Gson().toJsonTree(content.getSectorBaseImage(position)));
						sectorImage = sectorOverlayImage;
					}
					else if (sectorOverlayImage != null)
					{
						endTag = true;
						sectorImage = sectorOverlayImage;
					}

					if (scannable && content.isHasSectorContent(position))
					{
						Ship scanner = content.getScanSchiffFuerSektor(position);

						posObj.addProperty("scanner", scanner != null ? scanner.getId() : -1);
					}

					if (sectorImage != null)
					{
						posObj.addProperty("fg", sectorImage);
					}

					posObj.addProperty("battle", content.isSchlachtImSektor(position));
					posObj.addProperty("roterAlarm", content.isRoterAlarmImSektor(position));

					if (endTag)
					{
						locationArray.add(posObj);
					}
				}
			}
			json.add("locations", locationArray);

			// Das Anzeigen sollte keine DB-Aenderungen verursacht haben
			db.clear();

			return json;
		}
		finally
		{
			db.setFlushMode(oldFlushMode);
		}
	}

	/**
	 * Zeigt einen einzelnen Sektor mit allen Details an.
	 *
	 * @param sys Das anzuzeigende Sternensystem
	 * @param x Die X-Koordinate des zu scannenden Sektors
	 * @param y Die Y-Koordinate des zu scannenden Sektors
	 * @param scanship Die ID des fuer den Scanvorgang zu verwendenden Schiffs
	 * @param admin {@code true} falls die Adminsicht auf die Sternenkarte verwendet werden soll
	 */
	@Action(value = ActionType.AJAX, readOnly = true)
	public JsonObject sectorAction(StarSystem sys, int x, int y, Ship scanship, boolean admin)
	{
		validiereSystem(sys);

		User user = (User) getUser();
		org.hibernate.Session db = getDB();

		JsonObject json = new JsonObject();

		final Location loc = new Location(sys.getID(), x, y);

		FieldView field;
		if (admin && user.isAdmin())
		{
			field = new AdminFieldView(db, user, loc);
		}
		else
		{
			field = new PlayerFieldView(db, user, loc, scanship);
		}

		JsonArray users = exportSectorShips(field);
		json.add("users", users);

		JsonArray baseListObj = new JsonArray();
		for (Base base : field.getBases())
		{
			JsonObject baseObj = new JsonObject();
			baseObj.addProperty("id", base.getId());
			baseObj.addProperty("name", base.getName());
			baseObj.addProperty("username", Common._title(base.getOwner().getName()));
			baseObj.addProperty("image", base.getBaseImage(loc));
			baseObj.addProperty("imageX", base.getBaseImageOffset(loc)[0]);
			baseObj.addProperty("imageY", base.getBaseImageOffset(loc)[1]);
			baseObj.addProperty("klasse", base.getKlasse());
			baseObj.addProperty("typ", base.getBaseType().getName());
			baseObj.addProperty("eigene", base.getOwner().getId() == user.getId());

			baseListObj.add(baseObj);
		}

		json.add("bases", baseListObj);

		JsonArray jumpNodeListObj = new JsonArray();
		for (JumpNode jumpNode : field.getJumpNodes())
		{
			JsonObject jnObj = new JsonObject();
			jnObj.addProperty("id", jumpNode.getId());
			jnObj.addProperty("name", jumpNode.getName());
			jnObj.addProperty("blocked", jumpNode.isGcpColonistBlock() && Rassen.get().rasse(user.getRace()).isMemberIn(0));
			jumpNodeListObj.add(jnObj);
		}
		json.add("jumpnodes", jumpNodeListObj);

		Nebel nebel = field.getNebel();
		if (nebel != null)
		{
			JsonObject nebelObj = new JsonObject();
			nebelObj.addProperty("type", nebel.getType().getCode());
			nebelObj.addProperty("image", nebel.getImage());
			json.add("nebel", nebelObj);
		}

		JsonArray battleListObj = exportSectorBattles(db, field);
		json.add("battles", battleListObj);

		json.addProperty("subraumspaltenCount", field.getSubraumspalten().size());
		json.addProperty("roterAlarm", field.isRoterAlarm());


		return json;
	}

	private JsonArray exportSectorBattles(Session db, FieldView field)
	{
		JsonArray battleListObj = new JsonArray();
		List<Battle> battles = field.getBattles();
		if (battles.isEmpty())
		{
			return battleListObj;
		}

		User user = (User) getUser();
		boolean viewable = getContext().hasPermission("schlacht", "alleAufrufbar");

		if (!viewable)
		{
			Map<ShipType, List<Ship>> ships = field.getShips().get(user);
			if (ships != null && !ships.isEmpty())
			{
				for (ShipType shipType : ships.keySet())
				{
					if (shipType.getShipClass().isDarfSchlachtenAnsehen())
					{
						viewable = true;
					}
				}
			}
		}

		for (Battle battle : battles)
		{
			JsonObject battleObj = new JsonObject();
			battleObj.addProperty("id", battle.getId());
			battleObj.addProperty("einsehbar", viewable || battle.getSchlachtMitglied(user) != -1);
			JsonArray sideListObj = new JsonArray();

			for (int i = 0; i < 2; i++)
			{
				JsonObject sideObj = new JsonObject();
				sideObj.add("commander", battle.getCommander(i).toJSON());
				if (battle.getAlly(i) != 0)
				{
					Ally ally = (Ally) db.get(Ally.class, battle.getAlly(i));
					sideObj.add("ally", ally.toJSON());
				}
				sideListObj.add(sideObj);
			}
			battleObj.add("sides", sideListObj);

			battleListObj.add(battleObj);
		}
		return battleListObj;
	}

	private JsonArray exportSectorShips(FieldView field)
	{
		JsonArray users = new JsonArray();
		for (Map.Entry<User, Map<ShipType, List<Ship>>> owner : field.getShips().entrySet())
		{
			JsonObject jsonUser = new JsonObject();
			jsonUser.addProperty("name", Common._text(owner.getKey().getName()));
			jsonUser.addProperty("id", owner.getKey().getId());
			jsonUser.addProperty("race", owner.getKey().getRace());

			boolean ownFleet = owner.getKey().getId() == getUser().getId();
			jsonUser.addProperty("eigener", ownFleet);

			JsonArray shiptypes = new JsonArray();
			for (Map.Entry<ShipType, List<Ship>> shiptype : owner.getValue().entrySet())
			{
				JsonObject jsonShiptype = new JsonObject();
				jsonShiptype.addProperty("id", shiptype.getKey().getId());
				jsonShiptype.addProperty("name", shiptype.getKey().getNickname());
				jsonShiptype.addProperty("picture", shiptype.getKey().getPicture());
				jsonShiptype.addProperty("size", shiptype.getKey().getSize());

				JsonArray ships = new JsonArray();
				for (Ship ship : shiptype.getValue())
				{
					ShipTypeData typeData = ship.getTypeData();
					JsonObject shipObj = new JsonObject();
					shipObj.addProperty("id", ship.getId());
					shipObj.addProperty("name", ship.getName());
					shipObj.addProperty("gedockt", ship.getDockedCount());
					shipObj.addProperty("maxGedockt", typeData.getADocks());
					if (ownFleet)
					{
						shipObj.addProperty("gelandet", ship.getLandedCount());
						shipObj.addProperty("maxGelandet", typeData.getJDocks());

						shipObj.addProperty("energie", ship.getEnergy());
						shipObj.addProperty("maxEnergie", typeData.getEps());

						shipObj.addProperty("ueberhitzung", ship.getHeat());

						shipObj.addProperty("kannFliegen", typeData.getCost() > 0 && !ship.isDocked() && !ship.isLanded());

						int sensorRange = ship.getEffectiveScanRange();
						if (field.getNebel() != null)
						{
							sensorRange /= 2;
						}
						shipObj.addProperty("sensorRange", sensorRange);
					}

					if (ship.getFleet() != null)
					{
						shipObj.add("fleet", ship.getFleet().toJSON());
					}

					ships.add(shipObj);
				}
				jsonShiptype.add("ships", ships);
				shiptypes.add(jsonShiptype);
			}
			jsonUser.add("shiptypes", shiptypes);
			users.add(jsonUser);
		}
		return users;
	}
}

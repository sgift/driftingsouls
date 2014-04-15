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
package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.map.TileCache;
import net.driftingsouls.ds2.server.framework.db.batch.EvictableUnitOfWork;
import net.driftingsouls.ds2.server.framework.db.batch.UnitOfWork;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Ermoeglicht das Erstellen von Objekten in einem System aus einer Grafikdatei
 * heraus.
 *
 * @author Christopher Jung
 */
@AdminMenuEntry(category = "Objekte", name = "hinzuf&uuml;gen aus Grafik")
public class CreateObjectsFromImage extends AbstractEditPlugin<StarSystem> implements AdminPlugin
{
	public CreateObjectsFromImage()
	{
		super(StarSystem.class);
	}

	private static class SystemImg
	{
		private String path;
		private BufferedImage img;
		private Set<Integer> erkannteFarben;

		SystemImg(String path) throws IOException
		{
			this.path = path;

			File file = new File(this.path);
			this.img = ImageIO.read(file);
			if (this.img == null)
			{
				throw new IOException("Das Bild wurde aus unbekannten Gruenden nicht geladen");
			}

			this.erkannteFarben = new TreeSet<>();

			for (int x = 0; x < this.img.getWidth(); x++)
			{
				for (int y = 0; y < this.img.getHeight(); y++)
				{
					int color = img.getRGB(x, y);
					this.erkannteFarben.add(color);
				}
			}
		}

		public Set<Integer> getErkannteFarben()
		{
			return erkannteFarben;
		}

		String getPath()
		{
			return this.path;
		}

		int getWidth()
		{
			return this.img.getWidth();
		}

		int getHeight()
		{
			return this.img.getHeight();
		}

		int getColorAt(int x, int y)
		{
			return this.img.getRGB(x, y);
		}
	}

	@Override
	protected boolean isUpdatePossible(StarSystem entity)
	{
		Context context = ContextMap.getContext();
		Request request = context.getRequest();

		return "true".equalsIgnoreCase(request.getParameterString("doupdate"));
	}

	@Override
	protected void update(StatusWriter statusWriter, StarSystem system) throws IOException
	{
		Context context = ContextMap.getContext();
		Request request = context.getRequest();
		String imgPath = request.getParameterString("imgPath");

		boolean clearsystem = "true".equalsIgnoreCase(request.getParameterString("clearSystem"));

		SystemImg img = new SystemImg(imgPath);
		if (clearsystem)
		{
			clearSystem(statusWriter, system.getID());
		}

		for (int x = 0; x < img.getWidth(); x++)
		{
			for (int y = 0; y < img.getHeight(); y++)
			{
				int color = img.getColorAt(x, y);

				executeColorAt(system.getID(), x, y, color);
			}
		}

		TileCache.forSystem(system.getID()).resetCache();
	}

	@Override
	protected void edit(EditorForm form, StarSystem sys)
	{
		SystemImg img = null;
		Exception loadError = null;
		Request request = ContextMap.getContext().getRequest();
		String imgPath = request.getParameterString("imgPath");
		if (imgPath != null && !imgPath.trim().isEmpty())
		{
			try
			{
				img = new SystemImg(imgPath);
			}
			catch (IOException e)
			{
				loadError = e;
			}
		}
		boolean clearsystem = "true".equalsIgnoreCase(request.getParameterString("clearSystem"));

		form.field("Bildatei", "imgPath", String.class, imgPath);
		if (loadError != null)
		{
			form.label("", "Fehler: " + loadError.getMessage());
		}
		else if (img == null)
		{
			form.label("", "Bitte gib einen Dateipfad ein und bestätige den Dialog um weitere Konfigurationsoptionen zu erhalten");
		}
		else if (img.getWidth() != sys.getWidth() || img.getHeight() != sys.getHeight())
		{
			form.label("", "Die Grafik passt nicht zum System. Die Größenangaben weichen von einander ab. Bitte verwende eine Grafik der Größe " + img.getWidth() + "x" + img.getHeight());
		}
		else
		{
			List<BaseType> baseTypes = Common.cast(getDB().createCriteria(BaseType.class).list());

			for (Integer color : img.getErkannteFarben())
			{
				form.custom(new ColorFieldGenerator(baseTypes, color));
			}

			form.field("Alte Basen/Nebel entfernen", "clearSystem", Boolean.class, clearsystem);
			form.field("Änderungen ausführen", "doupdate", Boolean.class, false);
		}
	}

	private static class ColorFieldGenerator implements EditorForm.CustomFieldGenerator
	{
		private final List<BaseType> baseTypes;
		private final Integer color;

		public ColorFieldGenerator(List<BaseType> baseTypes, Integer color)
		{
			this.baseTypes = baseTypes;
			this.color = color;
		}

		public void generate(StringBuilder echo) throws IOException
		{
			String hex = Integer.toHexString(color & 0xFFFFFF);
			hex = StringUtils.leftPad(hex, 6, '0');
			echo.append("<tr>");
			echo.append("<td colspan='2' style='vertical-align:top'><div style='display:inline-block;width:15px;height:15px; background-color: #").append(hex).append("'></div>#").append(hex).append(":</td>");
			echo.append("<td>");

			echo.append("<select name='color" + color + "' onchange='Admin.CreateObjectsFromImage.objectTypeChanged(this)'>");
			boolean first = true;
			String curvalue = ContextMap.getContext().getRequest().getParameterString("color" + color);
			if (curvalue != null && curvalue.trim().isEmpty())
			{
				curvalue = null;
			}

			Map<String, String> options = new LinkedHashMap<>();
			options.put("empty", "Weltraum");
			for (BaseType bt : baseTypes)
			{
				options.put("basetype_" + bt.getId(), "[Basis] " + bt.getName() + " (" + bt.getId() + ")");
			}
			for (Nebel.Typ typ : Nebel.Typ.values())
			{
				options.put("nebel_" + typ, "[Nebel] " + typ);
			}

			for (Map.Entry<String, String> optionEntry : options.entrySet())
			{
				echo.append("<option value='").append(optionEntry.getKey()).append("'");
				if ((curvalue == null && first) || optionEntry.getKey().equals(curvalue))
				{
					first = false;
					echo.append(" selected='selected'");
				}
				echo.append(">").append(optionEntry.getValue()).append("</option>");
			}
			echo.append("</select>");

			if (curvalue != null && curvalue.startsWith("basetype_"))
			{
				echo.append("<br />");
				BaseType bt = parseBaseTypeFromRequest(ContextMap.getContext(), color);
				if (bt == null)
				{
					bt = parseBaseType(ContextMap.getContext(), curvalue);
				}
				echo.append("Breite: <input type='text' name='color" + color + "_width' value='" + bt.getWidth() + "' /><br />");
				echo.append("Höhe: <input type='text' name='color" + color + "_height' value='" + bt.getHeight() + "' /><br />");
				echo.append("Max Felder: <input type='text' name='color" + color + "_maxtiles' value='" + bt.getMaxTiles() + "' /><br />");
				echo.append("Max Energie: <input type='text' name='color" + color + "_maxe' value='" + bt.getEnergy() + "' /><br />");
				echo.append("Max Cargo: <input type='text' name='color" + color + "_maxcargo' value='" + bt.getCargo() + "' /><br />");
			}

			echo.append("</td></tr>\n");
		}

		private BaseType parseBaseType(Context context, String objectType)
		{
			int baseType = Integer.parseInt(objectType.substring(objectType.indexOf('_') + 1));
			return (BaseType) context.getDB().get(BaseType.class, baseType);
		}
	}

	private void clearSystem(StatusWriter echo, int systemid) throws IOException
	{
		Context context = ContextMap.getContext();
		Session db = context.getDB();

		List<Nebel> nebelList = Common.cast(db
											.createQuery("from Nebel where loc.system=:sys")
											.setInteger("sys", systemid)
											.list());
		for (Nebel nebel : nebelList)
		{
			db.delete(nebel);
		}

		db.getTransaction().commit();

		List<Integer> baseList = Common.cast(db
											 .createQuery("select id from Base where system=:sys")
											 .setInteger("sys", systemid)
											 .list());

		UnitOfWork<Integer> euw = new EvictableUnitOfWork<Integer>(this.getClass().getName() + ": delete bases")
		{
			@Override
			public void doWork(Integer baseId) throws Exception
			{
				Session db = getDB();
				Base base = (Base) db.get(Base.class, baseId);
				Integer[] bebauung = base.getBebauung();
				for (int i = 0; i < bebauung.length; i++)
				{
					if (bebauung[i] == 0)
					{
						continue;
					}

					Building building = Building.getBuilding(bebauung[i]);
					building.cleanup(ContextMap.getContext(), base, bebauung[i]);
					bebauung[i] = 0;
				}

				for (Offizier offizier : Offizier.getOffiziereByDest(base))
				{
					db.delete(offizier);
				}

				db.delete(base);
			}
		}.setFlushSize(1);

		euw.executeFor(baseList);
		if (!euw.getUnsuccessfulWork().isEmpty())
		{
			echo.append("<p>Konnte Basen nicht löschen: " + euw.getUnsuccessfulWork() + "</p>");
		}

		echo.append("<p>Alte Basen/Nebel entfernt.</p>");

		context.getDB().beginTransaction();
	}

	private void executeColorAt(int system, int x, int y, int color)
	{
		Context context = ContextMap.getContext();
		Request request = context.getRequest();

		String objectType = request.getParameterString("color" + color);
		if (objectType.startsWith("basetype_"))
		{
			createBase(new Location(system, x, y), color, context);
		}
		else if (objectType.startsWith("nebel_"))
		{
			String nebel = objectType.substring(objectType.indexOf('_') + 1);
			createNebula(context.getDB(), new Location(system, x, y), Nebel.Typ.valueOf(nebel));
		}
	}

	private void createBase(Location loc, int color, Context context)
	{
		BaseType bt = parseBaseTypeFromRequest(context, color);

		User user = (User) context.getDB().get(User.class, 0);
		Base base = new Base(loc, user);
		base.setKlasse(bt);
		base.setWidth(bt.getWidth());
		base.setHeight(bt.getHeight());
		base.setMaxTiles(bt.getMaxTiles());
		base.setMaxCargo(bt.getCargo());
		base.setMaxEnergy(bt.getEnergy());
		base.setAvailableSpawnableRess(null);
		base.setSize(bt.getSize());
		context.getDB().persist(base);
	}

	private static BaseType parseBaseTypeFromRequest(Context context, int color)
	{
		Request request = context.getRequest();

		String param = "color" + color;
		String objectType = request.getParameterString(param);

		if (!objectType.startsWith("basetype_"))
		{
			return null;
		}

		int baseType = Integer.parseInt(objectType.substring(objectType.indexOf('_') + 1));
		BaseType base = (BaseType) context.getDB().get(BaseType.class, baseType);
		BaseType bt = new BaseType(base);
		if (request.getParameter(param + "_width") != null &&
			!request.getParameter(param + "_width").trim().isEmpty())
		{
			bt.setWidth(request.getParameterInt(param + "_width"));
		}
		if (request.getParameter(param + "_height") != null &&
			!request.getParameter(param + "_height").trim().isEmpty())
		{
			bt.setHeight(request.getParameterInt(param + "_height"));
		}
		if (request.getParameter(param + "_maxtiles") != null &&
			!request.getParameter(param + "_maxtiles").trim().isEmpty())
		{
			bt.setMaxTiles(request.getParameterInt(param + "_maxtiles"));
		}
		if (request.getParameter(param + "_maxe") != null &&
			!request.getParameter(param + "_maxe").trim().isEmpty())
		{
			bt.setEnergy(request.getParameterInt(param + "_maxe"));
		}
		if (request.getParameter(param + "_maxcargo") != null &&
			!request.getParameter(param + "_maxcargo").trim().isEmpty())
		{
			bt.setCargo(request.getParameterInt(param + "_maxcargo"));
		}
		return bt;
	}

	private void createNebula(Session db, Location loc, Nebel.Typ type)
	{
		Nebel nebel = (Nebel) db.get(Nebel.class, new MutableLocation(loc));
		if (nebel != null)
		{
			db.delete(nebel);
		}
		nebel = new Nebel(new MutableLocation(loc), type);
		db.persist(nebel);
	}
}

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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.config.Offiziere;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;
import net.driftingsouls.ds2.server.map.TileCache;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ermoeglicht das Absetzen von Admin-Kommandos.
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Objekte", name="hinzufügen", permission = WellKnownAdminPermission.CREATE_OBJECTS)
@Component
public class CreateObjects implements AdminPlugin {
	private final Map<String,DialogEntry[]> options = new LinkedHashMap<>();

	public CreateObjects() {
		options.put("Base", new DialogEntry[] {
			new TextEntry("Anzahl", "anzahl", 18, "0"),
			new EntityEntry<>("Klasse", "klasse", BaseType.class, "name"),
			new TextEntry("Vorhandene Spawn-Ressourcen", "availspawnress", 18, "0"),
			new TextEntry("System", "system", 18, "0"),
			new TextEntry("Min X", "minX", 18, "0"),
			new TextEntry("Min Y", "minY", 18, "0"),
			new TextEntry("Max X", "maxX", 18, "0"),
			new TextEntry("Max Y", "maxY", 18, "0")
		});

		options.put("Nebel", new DialogEntry[] {
			new TextEntry("Anzahl", "anzahl", 18, "0"),
			new EnumEntry<>("Typ", "type", Nebel.Typ.values()),
			new TextEntry("System", "system", 18, "0"),
			new TextEntry("Min X", "minX", 18, "0"),
			new TextEntry("Min Y", "minY", 18, "0"),
			new TextEntry("Max X", "maxX", 18, "0"),
			new TextEntry("Max Y", "maxY", 18, "0")
		});

		options.put("Jumpnode", new DialogEntry[] {
			new TextEntry("System", "system", 18, "0"),
			new TextEntry("X", "minX", 18, "0"),
			new TextEntry("Y", "minY", 18, "0"),
			new TextEntry("Austritts-X", "maxX", 18, "0"),
			new TextEntry("Austritts-Y", "maxY", 18, "0"),
			new TextEntry("Austritts-System", "systemout", 18, "0"),
			new TextEntry("Austritts-Name", "systemname", 18, "0")
		});

		options.put("SystemXML", new DialogEntry[] {
			new TextEntry("Pfad", "xmlpath", 50, ""),
			new TextEntry("System", "system", 18, "0")
		});
	}

	@PersistenceContext
	private EntityManager em;

	private interface DialogEntry {
		/**
		 * Wandelt den Eintrag in HTML um.
		 * @param request Die Request
		 * @return Der HTML-Code
		 */
		String toHtml(Request request);
	}

	private static class TextEntry implements DialogEntry {
		final String title;
		final String name;
		final int size;
		final String defaultValue;

		TextEntry(String title, String name, int size, String defaultValue) {
			this.title = title;
			this.name = name;
			this.size = size;
			this.defaultValue = defaultValue;
		}

		@Override
		public String toHtml(Request request) {
			String out = "<tr><td class=\"noBorderX\">"+this.title+"</td>\n";

			String value = this.defaultValue;
			if( request.getParameter(this.name) != null ) {
				value = request.getParameter(this.name);
			}

			out += "<td class=\"noBorderX\"><input name=\""+this.name+"\" type=\"text\" size=\""+this.size+"\" value=\""+value+"\" /></td></tr>\n";

			return out;
		}
	}

	private static class EnumEntry<T extends Enum<?>> implements DialogEntry {
		final String title;
		final String name;
		final T[] values;

		EnumEntry(String title, String name, T[] values) {
			this.title = title;
			this.name = name;
			this.values = values;
		}

		@Override
		public String toHtml(Request request) {
			StringBuilder out = new StringBuilder("<tr><td class=\"noBorderX\">" + this.title + "</td>\n");

			String currentVal = request.getParameter(this.name);

			boolean first = true;

			out.append("<td class=\"noBorderX\"><select name=\"").append(this.name).append("\">");
			for( T value : values )
			{
				out.append("<option value='").append(value).append("'");
				if( value.toString().equals(currentVal) || (currentVal == null && first) )
				{
					out.append(" selected='selected'");
					first = false;
				}
				out.append(">").append(value).append("</option>");
			}
			out.append("</select></td></tr>\n");

			return out.toString();
		}
	}

	private class EntityEntry<T> implements DialogEntry {
		final String title;
		final String name;
		final Class<T> entityCls;
		final String labelProperty;

		EntityEntry(String title, String name, Class<T> entityCls, String labelProperty) {
			this.title = title;
			this.name = name;
			this.entityCls = entityCls;
			this.labelProperty = labelProperty;
		}

		@Override
		public String toHtml(Request request) {
			StringBuilder out = new StringBuilder("<tr><td class=\"noBorderX\">" + this.title + "</td>\n");

			String currentVal = request.getParameter(this.name);

			boolean first = true;

			out.append("<td class=\"noBorderX\"><select name=\"").append(this.name).append("\">");
			List<T> results = em.createQuery("from " + this.entityCls.getName(), entityCls).getResultList();
			for( T value :  results )
			{
				Object id = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(value);
				if( id == null )
				{
					continue;
				}

				String label = readLabel(value);
				if( label == null )
				{
					continue;
				}

				out.append("<option value='").append(id).append("'");
				if( id.toString().equals(currentVal) || (currentVal == null && first) )
				{
					out.append(" selected='selected'");
					first = false;
				}
				out.append(">").append(label).append(" (").append(id).append(")</option>");
			}
			out.append("</select></td></tr>\n");

			return out.toString();
		}

		private String readLabel(T value)
		{
			String label;
			try
			{
				label = BeanUtils.getProperty(value, labelProperty);
			}
			catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e)
			{
				label = null;
			}
            return label;
		}
	}

	@Override
	public void output(StringBuilder echo) throws IOException {
		Context context = ContextMap.getContext();

		String objekt = context.getRequest().getParameterString("objekt");
		int system = context.getRequest().getParameterInt("system");

		if( !options.containsKey(objekt) ) {
			objekt = options.keySet().iterator().next();
		}

		echo.append("<script type=\"text/javascript\">\n");
		echo.append("<!--\n");
		echo.append("function Go(x) {\n");
	   	echo.append("self.location.href = \"./ds?module=admin&namedplugin=").append(getClass().getName()).append("&objekt=\"+x;\n");
		echo.append("}\n");
		echo.append("//-->\n");
		echo.append("</script>\n");
		echo.append("<div class='gfxbox' style='width:340px'>");
		echo.append("<form action=\"./ds\" method=\"post\">\n");
		echo.append("<table class=\"noBorderX\" border=\"1\">\n");
		echo.append("<tr>\n");
		echo.append("<td class=\"noBorderX\">Objekt</td>\n");
		echo.append("<td class=\"noBorderX\">\n");
		echo.append("<select name=\"objekt\" onChange=\"Go(this.form.objekt.options[this.form.objekt.options.selectedIndex].value)\">\n");
		for( String key : options.keySet() ) {
			if( objekt.equals(key) ) {
				echo.append("<option selected=\"selected\" value=\"").append(key).append("\">").append(key).append("</option>\n");
			}
			else {
				echo.append("<option value=\"").append(key).append("\">").append(key).append("</option>\n");
			}
		}

		echo.append("</select></td></tr>");

		DialogEntry[] entries = options.get(objekt);
		for (DialogEntry entry : entries)
		{
			echo.append(entry.toHtml(context.getRequest()));
		}

		echo.append("<tr><td class=\"noBorderX\" colspan=\"2\" align=\"center\"><input type=\"submit\" value=\".: create\" />&nbsp");
		echo.append("<input type=\"reset\" value=\".: reset\" /></td></tr>");
		echo.append("</table>\n");
		echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("</form>");
		echo.append("</font>");

		echo.append("</div>");

		if( system != 0 ) {
			echo.append("Bearbeite System: ").append(system).append("<br />\n");

			switch (objekt)
			{
				case "Base":
					handleBase(context, echo, system);
					break;
				case "Nebel":
					handleNebel(context, system);
					break;
				case "Jumpnode":
					handleJumpnode(context, echo, system);
					break;
				case "SystemXML":
					handleSystemXML(context, echo, system);
					break;
			}
			TileCache.forSystem(system).resetCache();
		}
	}

	private void handleSystemXML(Context context, StringBuilder echo, int system) {
		final String xmlpath = context.getRequest().getParameterString("xmlpath");

		File xml = new File(xmlpath);
		if( !xml.isFile() ) {
			echo.append("File not found: ").append(xmlpath).append("<br />\n");
			return;
		}

		try {
			Document doc = XMLUtils.readFile(xmlpath);
			Element systemElement = doc.getDocumentElement();

			// Map
			Element mapElement = (Element)XMLUtils.firstNodeByTagName(systemElement, "map");
			if( mapElement != null ) {
				NodeList layerList = mapElement.getElementsByTagName("layer");
				for( int i=0; i < layerList.getLength(); i++ ) {
					Element layerElement = (Element)layerList.item(i);

					String file = layerElement.getAttribute("file");

					parsePngFile(echo, system, new File(xml.getParent()+File.separatorChar+file));
				}
			}

			// Jumpnodes
			NodeList jumpNodeList = systemElement.getElementsByTagName("jumpnode");
			for( int i=0; i < jumpNodeList.getLength(); i++ ) {
				Element jmpElement = (Element)jumpNodeList.item(i);

				String name = jmpElement.getAttribute("name");

				Location source = parseLocation(system, jmpElement.getAttribute("location"));
				if( source.getSystem() != system ) {
					echo.append("Fehler: Jumpnode liegt nicht im System");
					return;
				}
				Location target = parseLocation(system, jmpElement.getAttribute("target"));

				JumpNode jn = new JumpNode(source, target, name);
				em.persist(jn);
			}

			// Grosse Objekte
			NodeList largeObjects = systemElement.getElementsByTagName("large-object");
			for( int i=0; i < largeObjects.getLength(); i++ ) {
				Element object = (Element)largeObjects.item(i);

				Location source = parseLocation(system, object.getAttribute("location"));
				if( source.getSystem() != system ) {
					echo.append("Fehler: large-object liegt nicht im System");
					return;
				}

				int size = Integer.parseInt(object.getAttribute("size"));
				int klasse = Integer.parseInt(object.getAttribute("klasse"));
				int owner = Integer.parseInt(object.getAttribute("owner"));
				String name = object.getAttribute("name");

				User ownerObj = em.find(User.class, owner);
				if( ownerObj == null ) {
					echo.append("Fehler: large-object Besitzer '").append(owner).append("' existiert nicht");
					return;
				}

				BaseType type = em.find(BaseType.class, klasse);
				Base base = new Base(source, ownerObj, type);
				base.setSize(size);
				base.setName(name);
				base.setWidth(10);
				base.setHeight(10);
				base.setMaxCargo(10000000);
				base.setMaxTiles(100);
				base.setMaxEnergy(10000);
				base.setEnergy(10000);
				em.persist(base);
			}

			// Schiffe
			NodeList ships = systemElement.getElementsByTagName("ship");
			for( int i=0; i < ships.getLength(); i++ ) {
				Element ship = (Element)ships.item(i);

				Location source = parseLocation(system, ship.getAttribute("location"));
				if( source.getSystem() != system ) {
					echo.append("Fehler: Schiff liegt nicht im System");
					return;
				}

				String name = ship.getAttribute("name");
				int owner = Integer.parseInt(ship.getAttribute("owner"));
				int typeId = Integer.parseInt(ship.getAttribute("type"));
				boolean tradepost = Boolean.parseBoolean(ship.getAttribute("tradepost"));

				User ownerObj = em.find(User.class, owner);
				if( ownerObj == null ) {
					echo.append("Fehler: Schiff Besitzer '").append(owner).append("' existiert nicht");
					return;
				}

				ShipType type = em.find(ShipType.class, typeId);
				if( type == null ) {
					echo.append("Fehler: Schiff Typ '").append(typeId).append("' existiert nicht");
					return;
				}

				Cargo cargo = new Cargo();
				Node cargoNode = XMLUtils.firstNodeByTagName(ship, "cargo");
				if( cargoNode != null ) {
					cargo = new Cargo(cargoNode);
				}

				Ship shipObj = new Ship(ownerObj, type, system, source.getX(), source.getY());
				shipObj.setName(name);
				shipObj.setCargo(cargo);
				if( tradepost ) {
					shipObj.setStatus((shipObj.getStatus()+" tradepost").trim());
				}

				shipObj.setCrew(type.getCrew());
				shipObj.setEnergy(type.getEps());
				shipObj.setHull(type.getHull());
				shipObj.setShields(type.getShields());
				shipObj.getHistory().addHistory("Indienststellung am "+Common.getIngameTime(context.get(ContextCommon.class).getTick()));
				shipObj.setEngine(100);
				shipObj.setWeapons(100);
				shipObj.setComm(100);
				shipObj.setSensors(100);
				em.persist(shipObj);

				// Offizier
				Element offiElement = (Element)XMLUtils.firstNodeByTagName(ship, "offizier");
				if( offiElement != null ) {
					int rangcount = 0;

					Offizier offizier = new Offizier(ownerObj, offiElement.getAttribute("name"));
					if( offiElement.getAttribute("all") != null ) {
						int all = Integer.parseInt(offiElement.getAttribute("all"));
						for( Offizier.Ability ab : Offizier.Ability.values() ) {
							offizier.setAbility(ab, all);
							rangcount += all;
						}
					}

					offizier.setSpecial(Offizier.Special.values()[ThreadLocalRandom.current().nextInt(1,7)]);

					int rangf = rangcount/5;
					int rang = rangf/125;
					if( rang > Offiziere.MAX_RANG ) {
						rang = Offiziere.MAX_RANG;
					}
					offizier.setRang(rang);
					offizier.stationierenAuf(shipObj);

					em.persist(offizier);
				}
			}
		}
		catch( IOException e ) {
			echo.append("Kann XML ").append(xmlpath).append(" nicht oeffnen: ").append(e);
		}
		catch( SAXException | ParserConfigurationException e ) {
			echo.append("Kann XML ").append(xmlpath).append(" nicht parsen: ").append(e);
		}
	}

	private Location parseLocation(int system, String location) {
		// Position mit Systemangabe
		if( location.indexOf(':') > -1 ) {
			return Location.fromString(location);
		}

		// Position ohne Systemangabe (d.h. innerhalb dieses Systems)
		return Location.fromString(system+":"+location);
	}

	private void parsePngFile(StringBuilder echo, int system, File png) throws IOException {
		BufferedImage image = ImageIO.read(new FileInputStream(png));

		StarSystem thissystem = StarSystem.getSystem(system);

		for( int x=0; x < thissystem.getWidth(); x++ ) {
			for( int y=0; y < thissystem.getHeight(); y++ ) {
				Location loc = new Location(system, x+1, y+1);

				int colorhex = image.getRGB(x, y);

				switch(colorhex) {
				case 0xff000000:
					continue;

				// Deut-Nebel Normal
				case 0xffFF0000:
					createNebula(loc, Nebel.Typ.MEDIUM_DEUT);
					break;

				// Deut-Nebel Schwach
				case 0xffCB0000:
					createNebula(loc, Nebel.Typ.LOW_DEUT);
					break;

				// Deut-Nebel Stark
				case 0xffFF00AE:
					createNebula(loc, Nebel.Typ.STRONG_DEUT);
					break;

				// EMP-Nebel Schwach
				case 0xff3B9400:
					createNebula(loc, Nebel.Typ.LOW_EMP);
					break;

				// EMP-Nebel Mittel
				case 0xff4FC500:
					createNebula(loc, Nebel.Typ.MEDIUM_EMP);
					break;

				// EMP-Nebel Stark
				case 0xff66FF00:
					createNebula(loc, Nebel.Typ.STRONG_EMP);
					break;

				// Schadensnebel
				case 0xffFFBA00:
					createNebula(loc, Nebel.Typ.DAMAGE);
					break;

				// Normaler Asteroid
				case 0xff0000FF:
					createPlanet(loc, em.find(BaseType.class, 1));
					break;

				// Grosser Asteroid
				case 0xff0000AF:
					createPlanet(loc, em.find(BaseType.class, 3));
					break;

				// Kleiner Asteroid
				case 0xff00006F:
					createPlanet(loc, em.find(BaseType.class, 4));
					break;

				// Sehr kleiner Asteroid
				case 0xff40406F:
					createPlanet(loc, em.find(BaseType.class, 5));
					break;

				// Sehr grosser Asteroid
				case 0xff4040AF:
					createPlanet(loc, em.find(BaseType.class, 2));
					break;

				default:
					echo.append("Unknown color: #").append(Integer.toHexString(colorhex)).append("<br />");
				}
			}
		}
	}

	private void createPlanet(Location loc, BaseType klasse) {
		int height = klasse.getHeight();
		int width = klasse.getWidth();

		User nullUser = em.find(User.class, 0);

		Base base = new Base(loc, nullUser, klasse);
		base.setMaxTiles(width*height);
		base.setMaxEnergy(1000);
		em.persist(base);
	}

	private void handleJumpnode(Context context, StringBuilder echo, int system) {
		final int minX = context.getRequest().getParameterInt("minX");
		final int minY = context.getRequest().getParameterInt("minY");
		final int maxX = context.getRequest().getParameterInt("maxX");
		final int maxY = context.getRequest().getParameterInt("maxY");
		final int systemout = context.getRequest().getParameterInt("systemout");
		String systemname = context.getRequest().getParameterString("systemname");

		echo.append("Erstelle Jumpnode...<br />\n");

		JumpNode jn = new JumpNode(new Location(system, minX, minY), new Location(systemout, maxX, maxY), systemname);
		em.persist(jn);
	}

	private void handleNebel(Context context, int system) {
		final Nebel.Typ type = Nebel.Typ.valueOf(context.getRequest().getParameterString("type"));
		final int anzahl = context.getRequest().getParameterInt("anzahl");
		final int minX = context.getRequest().getParameterInt("minX");
		final int minY = context.getRequest().getParameterInt("minY");
		final int maxX = context.getRequest().getParameterInt("maxX");
		final int maxY = context.getRequest().getParameterInt("maxY");

		for( int i=1; i <= anzahl; i++ ) {
			int x = ThreadLocalRandom.current().nextInt(minX,maxX+1);
			int y = ThreadLocalRandom.current().nextInt(minY,maxY+1);

			createNebula(new Location(system, x, y), type);
		}
	}

	private void handleBase(Context context, StringBuilder echo, int system) {
		final int anzahl = context.getRequest().getParameterInt("anzahl");
		final int klasse = context.getRequest().getParameterInt("klasse");
		final String availablespawnableress = context.getRequest().getParameterString("availspawnress");
		final int minX = context.getRequest().getParameterInt("minX");
		final int minY = context.getRequest().getParameterInt("minY");
		final int maxX = context.getRequest().getParameterInt("maxX");
		final int maxY = context.getRequest().getParameterInt("maxY");

		final User nullUser = em.find(User.class, 0);
		BaseType type = em.find(BaseType.class, klasse);

		if(type == null)
		{
			echo.append("Basis-Klasse nicht gefunden!\n");
			return;
		}

		for( int i=1; i <= anzahl; i++ ) {
			int x = ThreadLocalRandom.current().nextInt(minX,maxX+1);
			int y = ThreadLocalRandom.current().nextInt(minY,maxY+1);

			Base base = new Base(new Location(system, x, y), nullUser, type);
			base.setAvailableSpawnableRess(availablespawnableress);
			em.persist(base);

			echo.append("Erstelle Kolonie...<br />\n");
		}
	}

	private void createNebula(Location loc, Nebel.Typ type) {
		Nebel nebel = em.find(Nebel.class, new MutableLocation(loc));
		if( nebel != null ) {
			em.remove(nebel);
		}
		nebel = new Nebel(new MutableLocation(loc), type);
		em.persist(nebel);
	}
}

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
package net.driftingsouls.ds2.server.cargo;

import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Repraesentiert einen Cargo, also eine Liste von Waren und Items mit jeweils einer bestimmten Menge, in DS.
 * <p>Hinweis zu {@link #equals(Object)} und {@link #hashCode()}:<br>
 * Zwei Cargoobjekte sind dann gleich, wenn sie zum Zeitpunkt des Vergleichs den selben Inhalt haben. Es wird nicht
 * beruecksichtigt ob die Optionen gleich sind oder die Cargos bei der Initalisierung einen unterschiedlichen Inhalt hatten.</p>
 * @author Christopher Jung
 *
 */
public class Cargo implements Cloneable {
	private static final Log log = LogFactory.getLog(Cargo.class);

	protected static final int MAX_RES = 18;
	private static final int ITEMS = 18;

	/**
	 * Die Typen, aus denen ein Cargo gelesen sowie auch wieder geschrieben werden kann.
	 * Einige sind nur aus historischen Gruenden noch aufgelistet, werden jedoch nicht mehr
	 * unterstuetzt.
	 *
	 */
	public enum Type {
		/**
		 * Ein leerer Cargo - Bitte den Default-Konstruktor stattdessen verwenden.
		 */
		EMPTY,
		/**
		 * Automatische Erkennung des Cargo-Formats. Unterstuetzt ein Fallback auf das alte Waren/Items-Format.
		 */
		AUTO,
		/**
		 * Ein Item-String. Vergleichbar einem Cargo-String, jedoch auf Items beschraenkt.
		 */
		ITEMSTRING
	}


	/**
	 * Die verschiedenen Rundungstypen bei {@link Cargo#multiply(double, net.driftingsouls.ds2.server.cargo.Cargo.Round)}.
	 * @author Christopher Jung
	 *
	 */
	public enum Round {
		/**
		 * Nachkommastellen abschneiden.
		 */
		NONE,
		/**
		 * {@link java.lang.Math#round(double)} zum Runden verwenden.
		 */
		ROUND,
		/**
		 * Alias fuer {@link #NONE}.
		 */
		FLOOR,
		/**
		 * {@link java.lang.Math#ceil(double)} zum Runden verwenden.
		 */
		CEIL
	}

	/**
	 * Die verschiedenen Optionen des Cargo-Objekts.
	 * @author Christopher Jung
	 * @see Cargo#setOption(net.driftingsouls.ds2.server.cargo.Cargo.Option, Object)
	 */
	public enum Option {
		/**
		 * Die fuer die Ausgabe zu verwendende CSS-Link-Klasse (java.lang.String).
		 */
		LINKCLASS,
		/**
		 * Soll die Masse, die eine Resource verbraucht, angezeigt werden? (java.lang.Boolean)
		 */
		SHOWMASS,
		/**
		 * Sollen die grossen Fassungen von Resourcen-Bildern angezeigt werden? (java.lang.Boolean)
		 */
		LARGEIMAGES,
		/**
		 * Soll die Ausgabe vollstaendig ohne HTML generiert werden? (java.lang.Boolean)
		 */
		NOHTML
	}

	private static final String NAMESPACE = "http://www.drifting-souls.net/ds2/resources/2006";

	private List<Long[]> items = new ArrayList<>();
	private String orgitems = null;

	private String linkclass = "forschinfo";
	private boolean showmass = true;
	private boolean largeImages = false;
	private boolean nohtml = false;

	/**
	 * Erstellt ein neues leeres Cargo-Objekt.
	 *
	 */
	public Cargo() {
		// Type.EMPTY
	}

	/**
	 * <p>Konstruktor.</p>
	 * Erstellt einen neuen Cargo aus dem aktuellen Cargo sowie den Optionen eines anderen Cargo-Objekts.
	 * @param cargo Der Cargo, dessen Daten genommen werden sollen
	 */
	public Cargo(Cargo cargo) {
		List<Long[]> itemArray = cargo.getItemArray();
		for (Long[] item : itemArray)
		{
			this.items.add(new Long[]{item[0], item[1], item[2], item[3]});
		}

		StringBuilder itemString = new StringBuilder(items.size()*8);
		if( !items.isEmpty() ) {
			for( Long[] aItem : items ) {
				if( aItem[1] != 0 ) {
					if( itemString.length() != 0 ) {
						itemString.append(';');
					}
					itemString.append(Common.implode("|",aItem ));
				}
			}
		}
		this.orgitems = itemString.toString();

		this.linkclass = (String)cargo.getOption(Option.LINKCLASS);
		this.showmass = (Boolean)cargo.getOption(Option.SHOWMASS);
		this.largeImages = (Boolean)cargo.getOption(Option.LARGEIMAGES);
		this.nohtml = (Boolean)cargo.getOption(Option.NOHTML);
	}

	/**
	 * Erstellt einen Cargo aus einer XML-Node.
	 * @param node Die Node unter der die Cargo-Infos stehen
	 */
	public Cargo(Node node) {
		NodeList list = node.getChildNodes();
		for( int i=0; i < list.getLength(); i++ ) {
			Node item = list.item(i);
			if( item.getNodeType() != Node.ELEMENT_NODE ) {
				continue;
			}
			if( !NAMESPACE.equals(item.getNamespaceURI()) ) {
				continue;
			}

			String count = XMLUtils.getStringAttribute(item, "count");
			if(orgitems == null)
			{
				orgitems = count;
				items.add(parseItems(count));
			}
			else
			{
				orgitems += ";";
				orgitems += count;
				items.add(parseItems(count));
			}
		}
	}

	private Long[] parseItems(String str) {
		String[] items = StringUtils.splitPreserveAllTokens(str, '|');
		if( items.length < 2 || items.length > 4 ) {
			throw new RuntimeException("Ungueltiges Item '"+str+"'");
		}
		long id = Long.parseLong(items[0]);
		long count = Long.parseLong(items[1]);
		long quest = 0;
		if( items.length > 2 && !items[2].isEmpty() )
		{
			quest = Long.parseLong(items[2]);
		}
		long uses = 0;
		if( items.length > 3 && !items[3].isEmpty() )
		{
			uses = Long.parseLong(items[3]);
		}
		return new Long[] {id, count, quest, uses};
	}

	/**
	 * Erstellt ein neues Cargo-Objekt aus einem Cargo-String oder einem Item-String.
	 * @param type der Typ (entweder {@link Type#AUTO} oder {@link Type#ITEMSTRING})
	 * @param source Der Cargo-String/Item-String
	 */
	public Cargo(Type type, String source ) {
		this();
		try {
			switch(type) {
			case AUTO: {
				if( source.indexOf(',') > -1 )
				{
					parseCargoString(source);
				}
				else
				{
					parseItemCargoString(source);
				}

				break;
			}

			case ITEMSTRING: {
				parseItemCargoString(source);
				break;
			}
			}
		}
		catch( RuntimeException e ) {
			log.error("Kann Cargo-String '"+source+"' im Format "+type+"' nicht laden", e);
			throw e;
		}
	}

	private void parseItemCargoString(String source)
	{
		if( source.length() > 0 ) {
			orgitems = source;

			String[] myitems = StringUtils.splitPreserveAllTokens(source, ';');
			for (String myitem : myitems)
			{
				if (!myitem.equals(""))
				{
					items.add(parseItems(myitem));
				}
			}
		}
	}

	private void parseCargoString(String source)
	{
		String[] mycargo = StringUtils.splitPreserveAllTokens(source, ',');

		if( mycargo.length != MAX_RES + 1 ) {
			String[] mycargo2 = new String[MAX_RES+1];
			System.arraycopy(mycargo, 0, mycargo2, 0, Math.min(mycargo.length,mycargo2.length));
			mycargo = mycargo2;
		}

		String[] myitems = new String[0];

		orgitems = mycargo[ITEMS];

		if( (mycargo[ITEMS] != null) && (mycargo[ITEMS].length() > 0) ) {
			myitems = StringUtils.splitPreserveAllTokens(mycargo[ITEMS],';');
		}

		for (String myitem : myitems)
		{
			if (!myitem.equals(""))
			{
				items.add(parseItems(myitem));
			}
		}
	}

	/**
	 * Schreibt den Cargo in einen Itemstring. Auf Wunsch wird nicht der aktuelle
	 * sondern der urspruengliche Cargo verwendet. Der Typ {@link Type#AUTO} wird
	 * nicht unterstuetzt.
	 * @param type Der zu schreibende Typ (Cargostring,Itemstring)
	 * @param orginalCargo Soll der urspruengliche Cargo verwendet werden
	 * @return der String mit dem Cargo
	 */
	private String getData(Type type, boolean orginalCargo ) {
		List<Long[]> items = this.items;

		switch(type) {
		case AUTO: {
			throw new UnsupportedOperationException("Der Typ 'AUTO' kann nur zum Laden verwendet werden");
		}
		case ITEMSTRING: {
			if( orginalCargo ) {
				return orgitems;
			}

			StringBuilder itemString = new StringBuilder(items.size()*8);

			if( !items.isEmpty() ) {
				for( Long[] aItem : items ) {
					if( aItem[1] != 0 ) {
						if( itemString.length() != 0 ) {
							itemString.append(';');
						}
						itemString.append(Common.implode("|",aItem ));
					}
				}
			}
			return itemString.toString();
		}
		}

		return null;
	}

	protected List<Long[]> getItemArray() {
		return items;
	}

	/**
	 * Gibt den Cargo als Resourcen-String zurueck.
	 * @return Der Resourcen-String
	 */
	public String save() {
		return save(false);
	}

	/**
	 * Gibt den aktuellen oder den bei der Erstellung verwendeten Cargo als
	 * Item-String zurueck.
	 * @param orginalCargo Soll der urspruengliche Cargo zurueckgegeben werden (<code>true</code>)?
	 * @return Der Resourcen-String
	 */
	public String save(boolean orginalCargo) {
		return getData(Type.ITEMSTRING, orginalCargo);
	}

	private boolean isSameIID( ResourceID resid, Long[] item ) {
		if( item[0] != resid.getItemID() ) {
			return false;
		}
		if( item[2] != resid.getUses() ) {
			return false;
		}
		return item[3] == resid.getQuest();
	}

	private boolean isSameIID( Long[] resid, Long[] item ) {
		if( !item[0].equals(resid[0]) ) {
			return false;
		}
		if( !item[2].equals(resid[2]) ) {
			return false;
		}
		return item[3].equals(resid[3]);
	}

	/**
	 * Fuegt den angegebenen ItemCargo-Eintrag zum Cargo hinzu.
	 * @param item Der ItemCargo-Eintrag
	 */
	public void addItem( ItemCargoEntry item ) {
		addResource(item.getResourceID(), item.getCount());
	}

	/**
	 * Fuegt dem Cargo die angegebene Resource in der angegebenen Hoehe hinzu.
	 * @param resourceid Die Resource
	 * @param count Die Anzahl an hinzuzufuegenden Resourceneinheiten
	 */
	public void addResource( ResourceID resourceid, long count ) {
		if( count == 0 )
		{
			return;
		}
		boolean done = false;

		for( int i=0; i < items.size(); i++ ) {
			Long[] aitem = items.get(i);
			if( isSameIID(resourceid, aitem) ) {
				aitem[1] = aitem[1]+count;
				items.set(i, aitem);
				done = true;
				break;
			}
		}

		if( !done ) {
			items.add( new Long[] {(long) resourceid.getItemID(), count, (long) resourceid.getUses(), (long) resourceid.getQuest()} );
		}
	}

	/**
	 * Zieht den angegebenen ItemCargo-Eintrag von Cargo ab.
	 * @param item der ItemCargo-Eintrag
	 */
	public void substractItem( ItemCargoEntry item ) {
		substractResource(item.getResourceID(), item.getCount());
	}

	/**
	 * Verringert die angegebene Resource im Cargo um den angegebenen Wert.
	 * @param resourceid Die Resource
	 * @param count Die Anzahl an Einheiten, die abgezogen werden sollen
	 */
	public void substractResource( ResourceID resourceid, long count ) {
		if( count == 0 )
		{
			return;
		}
		for( int i=0; i < items.size(); i++ ) {
			Long[] aitem = items.get(i);
			if( isSameIID(resourceid, aitem) ) {
				aitem[1] = aitem[1]-count;
				if( aitem[1] == 0 ) {
					items.remove(i);
				}

				return;
			}
		}

		// Diese Anweisung wird nur ausgefuerht, wenn das Item nicht im Cargo vorhanden ist
		items.add( new Long[] {(long) resourceid.getItemID(), -count, (long) resourceid.getUses(), (long) resourceid.getQuest()} );
	}

	/**
	 * Ueberprueft ob eine Resource vorhanden ist.
	 * @param resourceid Die Resourcen-ID
	 * @return <code>true</code>, falls die Resource vorhanden ist
	 */
	public boolean hasResource( ResourceID resourceid ) {
		return hasResource(resourceid, 0 );
	}

	/**
	 * Ueberprueft ob eine Resource in mindestens der angegebenen Menge vorhanden ist.
	 * @param resourceid Die Resourcen-ID
	 * @param count Die Mindestmenge
	 * @return <code>true</code>, falls die Resource in der Menge vorhanden ist
	 */
	public boolean hasResource( ResourceID resourceid, long count ) {
		long amount = getResourceCount(resourceid);
		if( count != 0 ) {
			return (amount >= count);
		}
		return amount != 0;
	}

	/**
	 * Gibt alle Items eines bestimmten Itemtyps als <code>ItemCargoEntry</code>-Instanzen zurueck.
	 * @param itemid Die ID des Item-Typs
	 * @return Liste aller Items des Typs im Cargo
	 */
	public List<ItemCargoEntry> getItem( int itemid ) {
		List<ItemCargoEntry> result = new ArrayList<>();
		for (Long[] item : items)
		{
			if (item[0] != itemid)
			{
				continue;
			}
			result.add(new ItemCargoEntry(this, item[0].intValue(), item[1], item[2].intValue(), item[3].intValue()));
		}

		return result;
	}

	/**
	 * Gibt alle Items im Cargo als <code>ItemCargoEntry</code>-Instanzen zurueck.
	 * @return Liste aller Items im Cargo
	 */
	public List<ItemCargoEntry> getItems() {
		List<ItemCargoEntry> result = new ArrayList<>();
		for (Long[] item : items)
		{
			result.add(new ItemCargoEntry(this, item[0].intValue(), item[1], item[2].intValue(), item[3].intValue()));
		}

		return result;
	}

	/**
	 * Gibt die Anzahl der vorhandenen Resourceneinheiten der Resource im Cargo zurueck.
	 * @param resourceid Die gewuenschte Resource
	 * @return die Anzahl der Resourceneinheiten
	 */
	public long getResourceCount( ResourceID resourceid ) {
		for (Long[] aitem : items)
		{
			if (isSameIID(resourceid, aitem))
			{
				return aitem[1];
			}
		}

		return 0;
	}

	/**
	 * Gibt die Gesamtmasse aller Waren und Items im <code>Cargo</code>-Objekt zurueck.
	 * @return Die Gesamtmasse
	 */
	public long getMass() {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		long tmp = 0;

		for (Long[] item1 : items)
		{
			Item item = (Item) db.get(Item.class, item1[0].intValue());
			if (item == null)
			{
				log.warn("Unbekanntes Item " + item1[0] + " geortet");
				continue;
			}
			tmp += item1[1] * item.getCargo();
		}

		return tmp;
	}

	/**
	 * Gibt die Liste der im Cargo vorhandenen Resourcen zurueck. Die Liste
	 * wird dabei bereit vorformatiert.
	 * @return Eine Resourcenliste
	 */
	public ResourceList getResourceList() {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		ResourceList reslist = new ResourceList();

		if( !items.isEmpty() ) {
			for( Long[] item : items  ) {
				Item itemType = (Item)db.get(Item.class, item[0].intValue());
				if( itemType == null )
				{
					log.warn("Unbekanntes Item "+item[0]+" geortet");
					continue;
				}

				String name = itemType.getName();
				String plainname = name;
				String image;
				boolean large = false;
				if( !largeImages ) {
					image = itemType.getPicture();
				}
				else {
					large = true;
					image = itemType.getLargePicture();
					if( image == null ) {
						large = false;
						image = itemType.getPicture();
					}
				}
				String fcount = Common.ln(item[1]);

				if( showmass && (itemType.getCargo() > 1) ) {
					fcount += " ("+Common.ln(itemType.getCargo()*item[1])+")";
				}

				ResourceID itemId = buildItemID(item);
				if( !nohtml ) {
					String style = "";
					if( item[3] != 0 ) {
						style += "text-decoration:underline;";
					}
					if( item[2] != 0 ) {
						style += "font-style:italic;";
					}

					if( !itemType.getQuality().color().equals("") ) {
						style += "color:"+itemType.getQuality().color()+";";
					}

					if( !style.equals("") ) {
						style = "style=\""+style+"\"";
					}

					String tooltiptext = "<img align=\"left\" src=\""+itemType.getPicture()+"\" alt=\"\" />"+itemType.getName();
					if( item[3] != 0 ) {
						tooltiptext += "<br /><span class=\"verysmallfont\">Questgegenstand</span>";
					}
					if( item[2] != 0 ) {
						name = "<span style=\"font-style:italic\">"+name+"</span>";
						tooltiptext += "<br /><span class=\"verysmallfont\">Benutzungen: "+item[2]+"</span>";
					}

					name = "<a "+style+" class=\"tooltip "+linkclass+"\" href=\"./ds?module=iteminfo&amp;itemlist="+itemId+"\">"+
						name+
						" <span class=\"ttcontent ttitem\" ds-item-id=\""+itemId+"\">"+tooltiptext+"</span></a>";
					fcount = "<a "+style+" class=\"tooltip "+linkclass+"\" href=\"./ds?module=iteminfo&amp;itemlist="+itemId+"\">"+
						fcount+
						" <span class=\"ttcontent ttitem\" ds-item-id=\""+itemId+"\">"+tooltiptext+"</span></a>";
				}
				else {
					if( item[3] != 0 ) {
						name += " [quest: "+item[3]+"]";
					}
					if( item[2] != 0 ) {
						name += " [limit: "+item[2]+"]";
					}
				}

				ResourceEntry res = new ResourceEntry(itemId, name, plainname,
						image, fcount, item[1] );
				res.setLargeImages(large);

				reslist.addEntry(res);
			}
		}

		return reslist;
	}

	private ResourceID buildItemID(Long[] item) {
		return new ItemID(item[0].intValue(), item[2].intValue(), item[3].intValue());
	}

	/**
	 * Vergleicht diesen Cargo mit einem anderen Cargo und gibt das Ergebnis als
	 * Resourcenliste teilweise vorformatiert zurueck. Der Vergleich kann entweder einseitig
	 * (nur die Resourcen in diesem Cargo werden im anderen Cargo geprueft) oder auch gegenseitig
	 * (jede Resource, welche in einem der beiden Cargos vorkommt, wird geprueft) erfolgen.<br>
	 * In der zurueckgegebenen Resourcenliste ist dieser Cargo der erste, der Cargo, gegen den geprueft
	 * werden soll (das Argument) der zweite.
	 *
	 * @param cargoObj Das Cargoobjekt, gegen das geprueft werden soll.
	 * @param echoBothSides Soll gegenseitig geprueft werden (<code>true</code>)?
	 * @return Die Resourcenliste mit dem Ergebnis.
	 */
	public ResourceList compare( Cargo cargoObj, boolean echoBothSides ) {
		return compare(cargoObj,echoBothSides,false,false);
    }

	/**
	 * Vergleicht diesen Cargo mit einem anderen Cargo und gibt das Ergebnis als
	 * Resourcenliste teilweise vorformatiert zurueck. Der Vergleich kann entweder einseitig
	 * (nur die Resourcen in diesem Cargo werden im anderen Cargo geprueft) oder auch gegenseitig
	 * (jede Resource, welche in einem der beiden Cargos vorkommt, wird geprueft) erfolgen.<br>
	 * In der zurueckgegebenen Resourcenliste ist dieser Cargo der erste, der Cargo, gegen den geprueft
	 * werden soll (das Argument) der zweite.
	 *
	 * @param cargoObj Das Cargoobjekt, gegen das geprueft werden soll.
	 * @param echoBothSides Soll gegenseitig geprueft werden (<code>true</code>)?
	 * @param basis Soll fuer den BaseController geprueft werden (<code>true</code>)?
	 * @return Die Resourcenliste mit dem Ergebnis.
	 */
  public ResourceList compare( Cargo cargoObj, boolean echoBothSides, boolean basis) {
    return compare(cargoObj,echoBothSides,basis,false);
  }

	/**
	 * Vergleicht diesen Cargo mit einem anderen Cargo und gibt das Ergebnis als
	 * Resourcenliste teilweise vorformatiert zurueck. Der Vergleich kann entweder einseitig
	 * (nur die Resourcen in diesem Cargo werden im anderen Cargo geprueft) oder auch gegenseitig
	 * (jede Resource, welche in einem der beiden Cargos vorkommt, wird geprueft) erfolgen.<br>
	 * In der zurueckgegebenen Resourcenliste ist dieser Cargo der erste, der Cargo, gegen den geprueft
	 * werden soll (das Argument) der zweite.
	 *
	 * @param cargoObj Das Cargoobjekt, gegen das geprueft werden soll.
	 * @param echoBothSides Soll gegenseitig geprueft werden (<code>true</code>)?
	 * @param basis Soll fuer den BaseController geprueft werden (<code>true</code>)?
	 * @param baukosten Soll fuer den BaseController geprueft werden (<code>true</code>)?
	 * @return Die Resourcenliste mit dem Ergebnis.
	 */
	public ResourceList compare( Cargo cargoObj, boolean echoBothSides, boolean basis, boolean baukosten) {
		ResourceList reslist = new ResourceList();
		org.hibernate.Session db = ContextMap.getContext().getDB();

		List<Long[]> items = cargoObj.getItemArray();

		// Ersteinmal feststellen was fuer items wir ueberhaupt im Cargo haben
		List<ItemID> itemlist = new ArrayList<>();
		if( this.items.size() > 0 ) {
			for( Long[] aitem : this.items ) {
				ItemID id = new ItemID(aitem[0].intValue(), aitem[2].intValue(), aitem[3].intValue());
				if( !itemlist.contains(id) ) {
					itemlist.add(id);
				}
			}
		}

		if( echoBothSides && (items.size() > 0) ) {
			for( Long[] aitem : items ) {
				ItemID id = new ItemID(aitem[0].intValue(), aitem[2].intValue(), aitem[3].intValue());
				if( !itemlist.contains(id) ) {
					itemlist.add(id);
				}
			}
		}

		if( itemlist.size() > 0 ) {
			itemlist.sort(new ResourceIDComparator(false));

			for( ItemID aitem : itemlist ) {
				Item item = (Item)db.get(Item.class, aitem.getItemID());
				if( item == null ) {
					log.warn("Ungueliges Item (Data: "+aitem+") entdeckt");
					continue;
				}

				// Nun suchen wir mal unsere Items im Cargo
				long cargo1 = 0;
				long cargo2 = 0;

				for( Long[] myitem : this.items ) {
					if( (myitem[0] == aitem.getItemID()) && (myitem[2] == aitem.getUses()) && (myitem[3] == aitem.getQuest()) ) {
						cargo1 += myitem[1];
					}
				}

				for( Long[] myitem : items ) {
					if( (myitem[0] == aitem.getItemID()) && (myitem[2] == aitem.getUses()) && (myitem[3] == aitem.getQuest()) ) {
						cargo2 += myitem[1];
					}
				}

				String style = "";

				String name = item.getName();
				String plainname = name;
				String image;
				boolean large = false;
				String tooltiptext = "";

				if( !nohtml ) {
					if( !item.getQuality().color().equals("") ) {
						style = "color:"+item.getQuality().color()+";";
					}

					if( !style.equals("") ) {
						style = "style='"+style+"'";
					}

					tooltiptext = "<img align=\"left\" src=\""+item.getPicture()+"\" alt=\"\" /><span "+StringEscapeUtils.escapeEcmaScript(style)+">"+item.getName()+"</span>";

					if( aitem.getQuest() != 0 ) {
						name = "<span style=\"text-decoration:underline\">"+name+"</span>";
						tooltiptext += "<br /><span class=\"verysmallfont\">Questgegenstand</span>";
					}
					if( aitem.getUses() != 0 ) {
						name = "<span style=\"font-style:italic\">"+name+"</span>";
						tooltiptext += "<br /><span class=\"verysmallfont\">Benutzungen: "+aitem.getUses()+"</span>";
					}

					name = "<a "+style+" class=\"tooltip "+linkclass+"\" href=\"./ds?module=iteminfo&amp;itemlist="+aitem.toString()+"\">"+
						name+
						"<span class=\"ttcontent ttitem\" ds-item-id=\""+aitem.toString()+"\">"+tooltiptext+"</span></a>";
				}
				else {
					if( aitem.getQuest() != 0 ) {
						name = name+" [quest: "+aitem.getQuest()+"]";
					}
					if( aitem.getUses() != 0 ) {
						name = name+" [limit: "+aitem.getUses()+"]";
					}
				}

				if( !largeImages ) {
					image = item.getPicture();
				}
				else {
					large = true;
					image = item.getLargePicture();
					if( image == null) {
						image = item.getPicture();
						large = false;
					}
				}

				long diff = cargo1 - cargo2;

				String fcargo1 = Common.ln(cargo1);
				String fcargo2 = Common.ln(cargo2);

				if( showmass && (item.getCargo() != 1) ) {
					if( cargo1 != 0 ) {
						fcargo1 = fcargo1+" ("+Common.ln(item.getCargo()*cargo1)+")";
					}
					if( cargo2 != 0 ) {
						fcargo2 = fcargo2+" ("+Common.ln(item.getCargo()*cargo2)+")";
					}
				}

				if( !nohtml )
				{
					if( diff > 0 && baukosten )
					{
						fcargo1 = "<a "+style+" class=\"cargo1 negativ tooltip\" href=\"./ds?module=iteminfo&amp;itemlist="+aitem.toString()+"\">"+
							fcargo1+
							"<span class=\"ttcontent ttitem\" ds-item-id=\""+aitem.toString()+"\">"+tooltiptext+"</span></a>";
					}
					else if( diff <= 0 && baukosten )
					{
						fcargo1 = "<a "+style+" class=\"cargo1 positiv tooltip\" href=\"./ds?module=iteminfo&amp;itemlist="+aitem.toString()+"\">"+
							fcargo1+
							"<span class=\"ttcontent ttitem\" ds-item-id=\""+aitem.toString()+"\">"+tooltiptext+"</span></a>";
					}
					else
					{
						fcargo1 = "<a "+style+" class=\"cargo1 "+linkclass+" tooltip\" href=\"./ds?module=iteminfo&amp;itemlist="+aitem.toString()+"\">"+
							fcargo1+
							"<span class=\"ttcontent ttitem\" ds-item-id=\""+aitem.toString()+"\">"+tooltiptext+"</span></a>";
					}
					if( cargo2 > 0 && basis )
					{
						fcargo2 = "<a "+style+" class=\"cargo2 positiv tooltip\" href=\"./ds?module=iteminfo&amp;itemlist="+aitem.toString()+"\">"+
							fcargo2+
							"<span class=\"ttcontent ttitem\" ds-item-id=\""+aitem.toString()+"\">"+tooltiptext+"</span></a>";
					}
					else if( cargo2 < 0 && basis )
					{
						fcargo2 = "<a "+style+" class=\"cargo2 negativ tooltip\" href=\"./ds?module=iteminfo&amp;itemlist="+aitem.toString()+"\">"+
							fcargo2+
							"<span class=\"ttcontent ttitem\" ds-item-id=\""+aitem.toString()+"\">"+tooltiptext+"</span></a>";
					}
					else
					{
						fcargo2 = "<a "+style+" class=\"cargo2 "+linkclass+" tooltip\" href=\"./ds?module=iteminfo&amp;itemlist="+aitem.toString()+"\">" +
							fcargo2 +
							"<span class=\"ttcontent ttitem\" ds-item-id=\""+aitem.toString()+"\">"+tooltiptext+"</span></a>";
					}
				}

				ResourceEntry entry = new ResourceEntry(aitem, name, plainname, image, fcargo1, fcargo2, cargo1, cargo2, diff);
				if( large ) {
					entry.setLargeImages(large);
				}

				reslist.addEntry(entry);
			}
		}

		return reslist;
	}

	/**
	 * Zieht vom Cargo den angegebenen Cargo ab.
	 *
	 * @param subcargo Der Cargo, um dessen Inhalt dieser Cargo verringert werden soll
	 */
	public void substractCargo( Cargo subcargo ) {
		List<Long[]> items = subcargo.getItemArray();

		if( !items.isEmpty() ) {
			for( Long[] item : items ) {
				// Nun suchen wir mal unsere Items im Cargo
				Long[] entry = null;

				if( !this.items.isEmpty() ) {
					for( Long[] myitem : this.items ) {
						if( !isSameIID(myitem,item) ) {
							continue;
						}
						entry = myitem;
						break;
					}
				}

				// Wurde das Item evt nicht in unserem Cargo gefunden? Dann neu hinzufuegen
				if( entry == null ) {
					this.items.add( new Long[] {item[0], -item[1], item[2], item[3]} );
				}
				else {
					long sum = entry[1]-item[1];
					if( sum != 0 ) {
						entry[1] = sum;
					}
					else {
						this.items.remove(entry);
					}
				}
			}
		}
	}

	/**
	 * Fuegt dem Cargo den angegebenen Cargo ab.
	 *
	 * @param addcargo Der Cargo, um dessen Inhalt dieser Cargo erhoeht werden soll
	 */
	public void addCargo( Cargo addcargo ) {
		List<Long[]> items = addcargo.getItemArray();

		if( !items.isEmpty() ) {
			for( Long[] item : items ) {
				// Nun suchen wir mal unsere Items im Cargo
				Long[] entry = null;

				if( !this.items.isEmpty() ) {
					for( Long[] myitem : this.items ) {
						if( !isSameIID(myitem,item) )
						{
							continue;
						}
						entry = myitem;
						break;
					}
				}

				// Wurde das Item evt nicht in unserem Cargo gefunden? Dann neu hinzufuegen
				if( entry == null ) {
					this.items.add( new Long[] {item[0], item[1], item[2], item[3]} );
				}
				else {
					entry[1] = entry[1]+item[1];
				}
			}
		}
	}

	/**
	 * Multipliziert jede Resource im Cargo mit dem angegebenen Faktor und rundet
	 * das Ergebnis entsprechend des Rundungsmodus.
	 * @param factor Der Faktor
	 * @param round Der Rundungsmodus
	 */
	public void multiply( double factor, Round round ) {
		if( items.size() > 0 )
		{
			List<Long[]> toRemove = new ArrayList<>();
			for( Long[] aitem : items ) {
				double val = aitem[1]*factor;
				switch( round ) {
				case NONE:
				case FLOOR:
					aitem[1] = (long)Math.floor(val);
					break;
				case ROUND:
					aitem[1] = Math.round(val);
					break;
				case CEIL:
					aitem[1] = (long)Math.ceil(val);
					break;
				}
				if( aitem[1] == 0 ) {
					toRemove.add(aitem);
				}
			}

			items.removeAll(toRemove);
		}
	}

	/**
	 * Spaltet vom Cargo ein Cargo-Objekt ab. Das neue Cargo-Objekt enthaelt
	 * Resourcen in der angegebenen Masse (oder weniger, falls nicht genug im
	 * Ausgangsobjekt waren). Das alte Cargoobjekt wird um diese Resourcenmenge
	 * verringert.
	 * @param mass Die gewuenschte Masse
	 * @return Das abgespaltene Cargoobjekt.
	 */
	public Cargo cutCargo( long mass ) {
		Cargo retcargo;
		org.hibernate.Session db = ContextMap.getContext().getDB();

		if( mass >= getMass() ) {
			retcargo = (Cargo)clone();
			items.clear();

			return retcargo;
		}
		retcargo = new Cargo();

		long currentmass = 0;

		if( currentmass != mass ) {
			for( int i=0; i < items.size(); i++ ) {
				Long[] aitem = items.get(i);

				Item item = (Item)db.get(Item.class, aitem[0].intValue());
				if( item.getCargo()*aitem[1] + currentmass < mass ) {
					currentmass += item.getCargo()*aitem[1];
					retcargo.getItemArray().add(aitem);
					items.remove(aitem);
					i--;
				}
				else if( item.getCargo() > 0 ) {
					Long[] newitem = aitem.clone();
					newitem[1] = (mass-currentmass)/item.getCargo();
					aitem[1] -= newitem[1];
					currentmass += item.getCargo()*newitem[1];
					retcargo.getItemArray().add(newitem);
				}
			}
		}

		return retcargo;
	}

	/**
	 * Setzt die vorhandene Menge der angegebenen Resource auf
	 * den angegebenen Wert.
	 * @param resourceid Die Resourcen-ID
	 * @param count Die neue Menge
	 */
	public void setResource( ResourceID resourceid, long count ) {
		boolean done = false;

		for( int i=0; i < items.size(); i++ ) {
			Long[] aitem = items.get(i);
			if( isSameIID(resourceid, aitem) ) {
				aitem[1] = count;
				items.set(i, aitem);
				done = true;
				break;
			}
		}

		if( !done ) {
			items.add( new Long[] {(long) resourceid.getItemID(), count, (long) resourceid.getUses(), (long) resourceid.getQuest()} );
		}
	}



	/**
	 * Gibt das erstbeste Item des angebenen Typs zurueck.
	 * Wenn kein Item diesen Effekt besitzt, wird <code>null</code> zurueckgegeben.
	 * @param itemType Der gesuchte Item-Typ
	 * @return Ein Item oder <code>null</code>
	 */
	public <T extends Item> ItemCargoEntry<T> getItemOfType( Class<T> itemType )
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();
		for (Long[] aitem : items)
		{
			final int itemid = aitem[0].intValue();
			@SuppressWarnings("unchecked") T item = (T) db.get(Item.class, itemid);
			if (item == null)
			{
				throw new RuntimeException("Unbekanntes Item " + itemid);
			}
			if (!itemType.isInstance(item))
			{
				continue;
			}
			return new ItemCargoEntry<>(this, item, aitem[1], aitem[2].intValue(), aitem[3].intValue());
		}
		return null;
	}

	/**
	 * Gibt eine Liste aller Items des gewuenschten Typs im Cargo zurueck.
	 * Wenn kein Item den passenden Typ hat, wird eine leere Liste zurueckgegeben.
	 * @param itemType Der gesuchte Item-Typ
	 * @return Die Liste aller Items des gesuchten Typs
	 */
	public <T extends Item> List<ItemCargoEntry<T>> getItemsOfType(Class<T> itemType)
	{
		List<ItemCargoEntry<T>> itemlist = new ArrayList<>();
		org.hibernate.Session db = ContextMap.getContext().getDB();

		for (Long[] aitem : items)
		{
			final int itemid = aitem[0].intValue();
			@SuppressWarnings("unchecked") T item = (T) db.get(Item.class, itemid);

			if (item == null)
			{
				continue;
			}
			if (!itemType.isInstance(item))
			{
				continue;
			}
			itemlist.add(new ItemCargoEntry<>(this, item, aitem[1], aitem[2].intValue(), aitem[3].intValue()));
		}

		return itemlist;
	}

	/**
	 * Prueft, ob der Cargo leer ist.
	 * @return <code>true</code>, falls er leer ist
	 */
	public boolean isEmpty() {
		for (Long[] aitem : items)
		{
			if (aitem[1] > 0)
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * Setzt eine Option auf den angegebenen Wert.
	 * @param option Die Option
	 * @param data Der Wert
	 */
	public void setOption( Option option, Object data ) {
		switch( option ) {
		case LINKCLASS:
			linkclass = data.toString();
			break;

		case SHOWMASS:
			showmass = (Boolean)data;
			break;

		case LARGEIMAGES:
			largeImages = (Boolean)data;
			break;

		case NOHTML:
			nohtml = (Boolean)data;
			break;
		}
	}

	/**
	 * Gibt den Wert einer Option zurueck.
	 * @param option Die Option
	 * @return Der Wert
	 */
	public Object getOption( Option option ) {
		switch( option ) {
		case LINKCLASS:
			return linkclass;

		case SHOWMASS:
			return showmass;

		case LARGEIMAGES:
			return largeImages;

		case NOHTML:
			return nohtml;
		}
		return null;
	}

	@Override
	public Object clone() {
		try {
			Cargo cargo = (Cargo)super.clone();
			cargo.items = new ArrayList<>();
			for( int i=0; i < this.items.size(); i++ ) {
				cargo.items.add(i, this.items.get(i).clone());
			}
			cargo.orgitems = this.orgitems;
			cargo.linkclass = this.linkclass;
			cargo.showmass = this.showmass;
			cargo.largeImages = this.largeImages;
			cargo.nohtml = this.nohtml;

			return cargo;
		}
		catch( CloneNotSupportedException e ) {
			// Sollte nie passieren, da alle Klassen ueber uns klonen koennen....
			return null;
		}
	}

	/**
	 * Gibt das zu einer Resourcen-ID gehoerende Bild zurueck.
	 * Der Pfad ist bereits vollstaendig und in URL-Form.
	 *
	 * @param resid Die Resourcen-ID
	 * @return Der Pfad zum Bild
	 */
	public static String getResourceImage( ResourceID resid ) {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		Item item = (Item)db.get(Item.class, resid.getItemID());
		if( item != null ) {
			return item.getPicture();
		}
		return "Kein passendes Item gefunden";
	}

	/**
	 * Gibt den Namen einer Resource zurueck.
	 * @param resid Die Resourcen-ID
	 * @return Der Name
	 */
	public static String getResourceName( ResourceID resid ) {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		Item item = (Item)db.get(Item.class, resid.getItemID());
		if( item != null ) {
			return item.getName();
		}
		return "Kein passendes Item gefunden";
	}

	/**
	 * Gibt die Masse einer Resource in einer bestimmten Menge zurueck.
	 *
	 * @param resourceid Die Resourcen-ID
	 * @param count Die Menge
	 * @return Die Masse, die die Resource in der Menge verbraucht
	 */
	public static long getResourceMass( ResourceID resourceid, long count ) {
		long tmp = 0;
		org.hibernate.Session db = ContextMap.getContext().getDB();

		Item item = (Item)db.get(Item.class, resourceid.getItemID());
		if( item != null ) {
			tmp = count * item.getCargo();
		}

		return tmp;
	}

	@Override
	public String toString() {
		return save();
	}

	/**
	 * Prueft, ob zwei Cargos im Moment den selben Inhalt haben.
	 * Es wird nicht geprueft, ob sie auch urspruenglich den selben Inhalt hatten
	 * oder ob die Optionen gleich sind!
	 * @param obj Der zu vergleichende Cargo
	 * @return <code>true</code>, falls der Inhalt gleich ist
	 */
	@Override
	public boolean equals(Object obj) {
		if( !(obj instanceof Cargo) ) {
			return false;
		}
		Cargo c = (Cargo)obj;

		if( this.items.size() != c.items.size() ) {
			return false;
		}

		// Bei vielen Items etwas ineffizient
		for( int i=0; i < this.items.size(); i++ ) {
			Long[] item = this.items.get(i);

			boolean found = false;

			for( int j=0; j < c.items.size(); j++ ) {
				Long[] item2 = c.items.get(j);

				// ID, Quest und Uses vergleichen
				if( !item[0].equals(item2[0]) ) {
					continue;
				}
				if( !item[2].equals(item2[2]) ) {
					continue;
				}
				if( !item[3].equals(item2[3]) ) {
					continue;
				}

				// Item erfolgreich lokalisiert
				found = true;

				if(!item[1].equals(item2[1])) {
					return false;
				}

				break;
			}

			if( !found ) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		long hash = 37;

		// Items in eine Sortierung bringen, damit zwei Cargos mit gleichen Items aber
		// unterschiedlicher Reihenfolge den selben hashCode haben
		Comparator<Long[]> comp = (o1, o2) -> {
			// IDs vergleichen
			if( !o1[0].equals(o2[0]) ) {
				return o1[0] > o2[0] ? 1 : -1;
			}
			// Quests vergleichen
			if( !o1[3].equals(o2[3]) ) {
				return o1[3] > o2[3] ? 1 : -1;
			}
			// Benutzungen vergleichen
			if( !o1[2].equals(o2[2]) ) {
				return o1[2] > o2[2] ? 1 : -1;
			}

			// Menge vergleichen
			return o1[1] > o2[1] ? 1 : (o1[1] < o2[1] ? -1 : 0);
		};

		List<Long[]> items = new ArrayList<>();
		items.addAll(this.items);
		items.sort(comp);

		for (Long[] item : items)
		{
			hash *= 17;
			hash += item[0];
			hash *= 17;
			hash += item[1];
			hash *= 17;
			hash += item[2];
			hash *= 17;
			hash += item[3];
		}

		return (int)hash;
	}
}

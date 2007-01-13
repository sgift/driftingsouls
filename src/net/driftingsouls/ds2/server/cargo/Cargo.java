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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.driftingsouls.ds2.server.config.Item;
import net.driftingsouls.ds2.server.config.ItemEffect;
import net.driftingsouls.ds2.server.config.Items;
import net.driftingsouls.ds2.server.config.ResourceConfig;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Repraesentiert einen Cargo, also eine Liste von Waren und Items mit jeweils einer bestimmten Menge, in DS
 * @author Christopher Jung
 *
 */
public class Cargo implements Loggable, Cloneable {
	protected static final int MAX_RES = 18;
	
	/**
	 * Die Typen, aus denen ein Cargo gelesen sowie auch wieder geschrieben werden kann.
	 * Einige sind nur aus historischen Gruenden noch aufgelistet, werden jedoch nicht mehr 
	 * unterstuetzt.
	 *
	 */
	public enum Type { 
		/**
		 * Ein leerer Cargo - Bitte den Default-Konstruktor stattdessen verwenden
		 */
		EMPTY,
		/**
		 * Ein Cargo-String
		 */
		STRING,
		/**
		 * Ein Array von Waren/Items - Nicht mehr unterstuetzt
		 */
		ARRAY,
		/**
		 * Ein Item-String. Vergleichbar einem Cargo-String, jedoch auf Items beschraenkt
		 */
		ITEMSTRING
	};
	
	
	/**
	 * Die verschiedenen Rundungstypen bei {@link Cargo#multiply(double, net.driftingsouls.ds2.server.cargo.Cargo.Round)}
	 * @author Christopher Jung
	 *
	 */
	public enum Round {
		/**
		 * Nachkommastellen abschneiden
		 */
		NONE,
		/**
		 * {@link java.lang.Math#round(double)} zum Runden verwenden
		 */
		ROUND,
		/**
		 * Alias fuer {@link #NONE}
		 */
		FLOOR,
		/**
		 * {@link java.lang.Math#ceil(double)} zum Runden verwenden
		 */
		CEIL
	};

	/**
	 * Die verschiedenen Optionen des Cargo-Objekts
	 * @author Christopher Jung
	 * @see Cargo#setOption(net.driftingsouls.ds2.server.cargo.Cargo.Option, Object)
	 */
	public enum Option {
		/**
		 * Die fuer die Ausgabe zu verwendende CSS-Link-Klasse (java.lang.String)
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
		 * Soll die Ausgabe vollstaendig ohne HTML generiert werden? (java.lang.Boolean);
		 */
		NOHTML
	};
	
	private static final String NAMESPACE = "http://www.drifting-souls.net/ds2/resources/2006";
	
	private long[] cargo = new long[MAX_RES+1];
	private long[] orgcargo = new long[MAX_RES+1];
	
	private List<Long[]> items = new ArrayList<Long[]>();
	private List<Long[]> orgitems = new ArrayList<Long[]>();
	
	private String linkclass = "forschinfo";
	private boolean showmass = true;
	private boolean largeImages = false;
	private boolean nohtml = false;
	
	/**
	 * Erstellt ein neues leeres Cargo-Objekt
	 *
	 */
	public Cargo() {
		// Type.EMPTY
	}
	
	private Long[] parseItems(String str) {
		String[] items = StringUtils.split(str, '|');
		if( items.length != 4 ) {
			throw new RuntimeException("Ungueltiges Item '"+str+"'");
		}
		return new Long[] {Long.parseLong(items[0]), Long.parseLong(items[1]), Long.parseLong(items[2]), Long.parseLong(items[3])};
	}
	
	/**
	 * Erstellt einen Cargo aus einer XML-Node
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
			String name = item.getLocalName();
			Integer id = ResourceConfig.getResourceIDByTag(name);
			if( id == null ) {
				LOG.warn("Unbekannte Resource "+name+" - ignoriere Eintrag");
				continue;
			}
			long count = XMLUtils.getLongAttribute(item, "count");
			cargo[id] = count;
			orgcargo[id] = count;
		}
		
		// TODO: Item-Unterstuetzung
	}
	
	/**
	 * Erstellt ein neues Cargo-Objekt aus einem Cargo-String oder einem Item-String
	 * @param type der Typ (entweder {@link Type#STRING} oder {@link Type#ITEMSTRING})
	 * @param source Der Cargo-String/Item-String
	 */
	public Cargo(Type type, String source ) {
		this();
		try {
			switch(type) {
			case STRING: {
				String[] mycargo = source.split(",");
				if( mycargo.length != MAX_RES + 1 ) {
					String[] mycargo2 = new String[MAX_RES+1];
					System.arraycopy(mycargo, 0, mycargo2, 0, Math.min(mycargo.length,mycargo2.length));
					mycargo = mycargo2;
				}
				String[] myitems = new String[0];
				if( (mycargo[Resources.ITEMS.getID()] != null) && !"".equals(mycargo[Resources.ITEMS.getID()]) ) {
					myitems = mycargo[Resources.ITEMS.getID()].split(";");
				}
				int itemcount = 0;
				for( int i=0; i < myitems.length; i++ ) {
					if( !myitems[i].equals("") ) { 
						itemcount++;
						items.add(parseItems(myitems[i]));
					}
				}
				for( int i=0; i <= MAX_RES; i++ ) {
					if( i != Resources.ITEMS.getID() ) {
						cargo[i] = Long.parseLong(mycargo[i]);
					}
				}
				break;
			}
				
			case ITEMSTRING: {
				String[] myitems = source.split(";");
				int itemcount = 0;
				for( int i=0; i < myitems.length; i++ ) {
					if( !myitems[i].equals("") ) { 
						itemcount++;
						items.add(parseItems(myitems[i]));
					}
				}
				break;
			}
			}
			
			System.arraycopy(cargo, 0, orgcargo, 0, cargo.length);
			orgitems = new ArrayList<Long[]>(items);
		}
		catch( RuntimeException e ) {
			LOG.error("Kann Cargo-String '"+source+"' im Format "+type+"' nicht laden", e);
			throw e;
		}
	}
	
	/**
	 * Schreibt den Cargo in einen Cargo- oder Itemstring
	 * @param type Der zu schreibende Typ (Cargostring/Itemstring)
	 * @return der String mit dem Cargo
	 */
	public String getData(Type type) {
		return getData(type, false);
	}
	
	/**
	 * Schreibt den Cargo in einen Cargo- oder Itemstring. Auf Wunsch wird nicht der aktuelle
	 * sondern der urspruengliche Cargo verwendet.
	 * @param type Der zu schreibende Typ (Cargostring,Itemstring)
	 * @param orginalCargo Soll der urspruengliche Cargo verwendet werden
	 * @return der String mit dem Cargo
	 */
	public String getData(Type type, boolean orginalCargo ) {
		long[] cargo = null;
		List<Long[]> items = null;
		
		if( orginalCargo ) {
			cargo = this.orgcargo;
			items = this.orgitems;
		}
		else {
			cargo = this.cargo;	
			items = this.items;
		}
		
		switch(type) {
		case STRING: {
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
			String[] cargoString = new String[cargo.length];
			for( int i=0; i < cargo.length; i++ ) {
				cargoString[i] = Long.toString(cargo[i]);
			}
			cargoString[Resources.ITEMS.getID()] = itemString.toString();
			
			return Common.implode(",", cargoString);
		}
		case ITEMSTRING: {
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
		case ARRAY:
			throw new RuntimeException("DEPRECATED");
		}
		
		return null;
	}
	
	protected long[] getCargoArray() {
		return cargo;
	}
	
	protected List<Long[]> getItemArray() {
		return items;
	}
	
	/**
	 * Gibt den Cargo als Resourcen-String zurueck
	 * @return Der Resourcen-String
	 */
	public String save() {
		return save(false);
	}
	
	/**
	 * Gibt den aktuellen oder den bei der Erstellung verwendeten Cargo als
	 * Resourcen-String zurueck
	 * @param orginalCargo Soll der urspruengliche Cargo zurueckgegeben werden (<code>true</code>)?
	 * @return Der Resourcen-String
	 */
	public String save(boolean orginalCargo) {
		return getData(Type.STRING, orginalCargo);
	}
	
	private boolean isSameIID( ResourceID resid, Long[] item ) {
		if( item[0] != resid.getItemID() ) {
			return false;
		}
		if( item[2] != resid.getUses() ) {
			return false; 
		}
		if( item[3] != resid.getQuest() ) {
			return false;
		}

		return true;
	}
	
	private boolean isSameIID( Long[] resid, Long[] item ) {
		if( !item[0].equals(resid[0]) ) {
			return false;
		}
		if( !item[2].equals(resid[2]) ) {
			return false; 
		}
		if( !item[3].equals(resid[3]) ) {
			return false;
		}

		return true;
	}
	
	/**
	 * Fuegt den angegebenen ItemCargo-Eintrag zum Cargo hinzu
	 * @param item Der ItemCargo-Eintrag
	 */
	public void addItem( ItemCargoEntry item ) {
		addResource(item.getResourceID(), item.getCount());
	}
	
	/**
	 * Fuegt dem Cargo die angegebene Resource in der angegebenen Hoehe hinzu
	 * @param resourceid Die Resource
	 * @param count Die Anzahl an hinzuzufuegenden Resourceneinheiten
	 */
	public void addResource( ResourceID resourceid, long count ) {
		if( resourceid.isItem() ) {
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
				items.add( new Long[] {new Long(resourceid.getItemID()), count, new Long(resourceid.getUses()), new Long(resourceid.getQuest())} );
			}
		}
		else {
			cargo[resourceid.getID()] += count;
		}
	}
	
	/**
	 * Zieht den angegebenen ItemCargo-Eintrag von Cargo ab
	 * @param item der ItemCargo-Eintrag
	 */
	public void substractItem( ItemCargoEntry item ) {
		substractResource(item.getResourceID(), item.getCount());
	}
	
	/**
	 * Verringert die angegebene Resource im Cargo um den angegebenen Wert
	 * @param resourceid Die Resource
	 * @param count Die Anzahl an Einheiten, die abgezogen werden sollen
	 */
	public void substractResource( ResourceID resourceid, long count ) {
		if( resourceid.isItem() ) {
			boolean done = false;
			
			for( int i=0; i < items.size(); i++ ) {
				Long[] aitem = items.get(i);
				if( isSameIID(resourceid, aitem) ) {
					aitem[1] = aitem[1]-count;
					if( aitem[1] == 0 ) {
						items.remove(i);
					}
					else {
						items.set(i, aitem);
					}
					done = true;
					break;
				}
			}
			
			if( !done ) {
				items.add( new Long[] {new Long(resourceid.getItemID()), -count, new Long(resourceid.getUses()), new Long(resourceid.getQuest())} );
			}
		}
		else {
			cargo[resourceid.getID()] -= count;
		}
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
		if( amount != 0 ) {
			return true;
		}
		return false;
	}
	
	/**
	 * Gibt alle Items eines bestimmten Itemtyps als <code>ItemCargoEntry</code>-Instanzen zurueck
	 * @param itemid Die ID des Item-Typs
	 * @return Liste aller Items des Typs im Cargo
	 */
	public List<ItemCargoEntry> getItem( int itemid ) {
		List<ItemCargoEntry> result = new ArrayList<ItemCargoEntry>();
		for( int i=0; i < items.size(); i++ ) {
			Long[] item = items.get(i);
			if( item[0] != itemid ) {
				continue;
			}
			result.add(new ItemCargoEntry(this, item[0].intValue(), item[1], item[2].intValue(), item[3].intValue()));
		}
		
		return result;
	}
	
	/**
	 * Gibt alle Items im Cargo als <code>ItemCargoEntry</code>-Instanzen zurueck
	 * @return Liste aller Items im Cargo
	 */
	public List<ItemCargoEntry> getItems() {
		List<ItemCargoEntry> result = new ArrayList<ItemCargoEntry>();
		for( int i=0; i < items.size(); i++ ) {
			Long[] item = items.get(i);
			result.add(new ItemCargoEntry(this, item[0].intValue(), item[1], item[2].intValue(), item[3].intValue()));
		}
		
		return result;
	}
	
	/**
	 * Gibt die Anzahl der vorhandenen Resourceneinheiten der Resource im Cargo zurueck
	 * @param resourceid Die gewuenschte Resource
	 * @return die Anzahl der Resourceneinheiten
	 */
	public long getResourceCount( ResourceID resourceid ) {
		if( resourceid.isItem() ) {		
			for( int i=0; i < items.size(); i++ ) {
				Long[] aitem = items.get(i);
				if( isSameIID(resourceid, aitem) ) {
					return aitem[1];
				}
			}
			
			return 0;
		}
		return cargo[resourceid.getID()];
	}
	
	/**
	 * Gibt die Gesamtmasse aller Waren und Items im <code>Cargo</code>-Objekt zurueck
	 * @return Die Gesamtmasse
	 */
	public long getMass() {
		long tmp = 0;
		
		for( int i=0; i <= MAX_RES; i++ ) {
			if( i != Resources.ITEMS.getID() ) {
				tmp += cargo[i];
			}
			else {
				for( int j=0; j < items.size(); j++ ) {
					if( Items.get().item(items.get(j)[0].intValue()) == null ) {
						LOG.warn("Unbekanntes Item "+items.get(j)[0]+" geortet");
						continue;
					}
					tmp += items.get(j)[1]*Items.get().item(items.get(j)[0].intValue()).getCargo();
				} 
			}
		}
		
		return tmp;
	}
	
	/**
	 * Gibt die Liste der im Cargo vorhandenen Resourcen zurueck. Die Liste
	 * wird dabei bereit vorformatiert.
	 * @return Eine Resourcenliste
	 */
	public ResourceList getResourceList() {
		ResourceList reslist = new ResourceList();
		String sess = "";
		if( !nohtml ) {
			Context context = ContextMap.getContext();
			sess = context.getSession();
		}
		
		for( int i=0; i <= MAX_RES; i++ ) {
			if( i == Resources.ITEMS.getID() ) continue;
			if( cargo[i] == 0 ) continue;
			
			ResourceID id = new WarenID(i);
			
			ResourceEntry res = new ResourceEntry(id, getResourceName(id), getResourceName(id), 
					getResourceImage(id), Common.ln(cargo[i]), cargo[i] );
			
			if( largeImages ) {
				res.setLargeImages(false);
			}
			
			reslist.addEntry(res);
		}
		if( !items.isEmpty() ) {
			for( Long[] item : items  ) {
				Item itemType = Items.get().item(item[0].intValue());
				if( itemType == null ) {
					LOG.warn("Unbekanntes Item "+item[0]+" geortet");
					continue;
				}
				
				String name = itemType.getName();
				String plainname = name;
				String image = null;
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
					fcount += " ("+(itemType.getCargo()*item[1])+")";
				}
				
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
										
					String tooltiptext = "&lt;span class=\\'nobr\\'&gt;&lt;span class=\\'libwarentt\\'&gt;&lt;img align=\\'left\\' src=\\'"+itemType.getPicture()+"\\' alt=\\'\\' /&gt;"+itemType.getName()+"&lt;/span&gt;&lt;/span&gt;";
					if( item[3] != 0 ) {
						tooltiptext += "&lt;br /&gt;&lt;span class=\\'verysmallfont\\'&gt;Questgegenstand&lt;/span&gt;";
					}
					if( item[2] != 0 ) {
						name = "<span style=\"font-style:italic\">"+name+"</span>";
						tooltiptext += "&lt;br /&gt;&lt;span class=\\'verysmallfont\\'&gt;Benutzungen: "+item[2]+"&lt;/span&gt;";
					}
					
					name = "<a "+style+" onmouseover=\"return overlib('"+tooltiptext+"',TIMEOUT,0,DELAY,400,TEXTFONTCLASS,'smallTooltip');\" onmouseout=\"return nd();\" class=\""+linkclass+"\" href=\"./main.php?module=iteminfo&amp;sess="+sess+"&amp;itemlist="+buildItemID(item)+"\">"+name+"</a>";				
					fcount = "<a "+style+" onmouseover=\"return overlib('"+tooltiptext+"',TIMEOUT,0,DELAY,400,TEXTFONTCLASS,'smallTooltip');\" onmouseout=\"return nd();\" class=\""+linkclass+"\" href=\"./main.php?module=iteminfo&amp;sess="+sess+"&amp;itemlist="+buildItemID(item)+"\">"+fcount+"</a>";
				}
				else {
					if( item[3] != 0 ) {
						name += " [quest: "+item[3]+"]";	
					}
					if( item[2] != 0 ) {
						name += " [limit: "+item[2]+"]";
					}
				}
				
				ResourceEntry res = new ResourceEntry(buildItemID(item), name, plainname, 
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
		ResourceList reslist = new ResourceList();
		
		long[] cargo = cargoObj.getCargoArray();
		List<Long[]> items = cargoObj.getItemArray();

		for( int i=0; i <= MAX_RES; i++ ) {
			if( i == Resources.ITEMS.getID() ) continue;
			
			if( echoBothSides && (this.cargo[i] == 0) && (cargo[i] == 0) ) continue;
			else if( !echoBothSides && (this.cargo[i] == 0) ) continue;
			
			ResourceID id = new WarenID(i);
			
			ResourceEntry res = new ResourceEntry(id, getResourceName(id), getResourceName(id), 
					getResourceImage(id), Common.ln(this.cargo[i]), Common.ln(cargo[i]), 
					this.cargo[i], cargo[i], this.cargo[i] - cargo[i] );
			
			if( largeImages ) {
				res.setLargeImages(false);
			}
			
			reslist.addEntry(res);
		}
		
		// Ersteinmal feststellen was fuer items wir ueberhaupt im Cargo haben
		List<ItemID> itemlist = new ArrayList<ItemID>();
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
			String sess = "";
			if( !nohtml ) {
				Context context = ContextMap.getContext();
				sess = context.getSession();
			}
			
			Collections.sort(itemlist, new ResourceIDComparator(false) );
			
			for( ItemID aitem : itemlist ) {
				if( Items.get().item(aitem.getItemID()) == null ) {
					LOG.warn("Ungueliges Item (Data: "+aitem+") entdeckt");
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
				
				String name = Items.get().item(aitem.getItemID()).getName();
				String plainname = name;
				String image = "";
				boolean large = false;
				String tooltiptext = "";
				
				if( !nohtml ) {
					if( !Items.get().item(aitem.getItemID()).getQuality().color().equals("") ) {
						style = "color:"+Items.get().item(aitem.getItemID()).getQuality().color()+";";
					}
					
					if( !style.equals("") ) {
						style = "style='"+style+"'";	
					}
					
					tooltiptext = "&lt;span class=\\'nobr\\'&gt;&lt;span "+StringEscapeUtils.escapeJavaScript(style)+" class=\\'libwarentt\\'&gt;&lt;img align=\\'left\\' src=\\'"+Items.get().item(aitem.getItemID()).getPicture()+"\\' alt=\\'\\' /&gt;"+Items.get().item(aitem.getItemID()).getName()+"&lt;/span&gt;&lt;/span&gt;";
					
					if( aitem.getQuest() != 0 ) {
						name = "<span style=\"text-decoration:underline\">"+name+"</span>";
						tooltiptext += "&lt;br /&gt;&lt;span class=\\'verysmallfont\\'&gt;Questgegenstand&lt;/span&gt;";	
					}
					if( aitem.getUses() != 0 ) {
						name = "<span style=\"font-style:italic\">"+name+"</span>";	
						tooltiptext += "&lt;br /&gt;&lt;span class=\\'verysmallfont\\'&gt;Benutzungen: "+aitem.getUses()+"&lt;/span&gt;";	
					}
					
					name = "<a "+style+" onmouseover=\"return overlib('"+tooltiptext+"',TIMEOUT,0,DELAY,400,TEXTFONTCLASS,'smallTooltip');\" onmouseout=\"return nd();\" class=\""+linkclass+"\" href=\"./main.php?module=iteminfo&amp;sess="+sess+"&amp;itemlist="+aitem+"\">"+name+"</a>";
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
					image = Items.get().item(aitem.getItemID()).getPicture();
				}
				else {
					large = true;
					image = Items.get().item(aitem.getItemID()).getLargePicture();
					if( image == null) {
						image = Items.get().item(aitem.getItemID()).getPicture();	
						large = false;
					}
				}
				
				long diff = cargo1 - cargo2;
				
				String fcargo1 = Common.ln(cargo1);
				String fcargo2 = Common.ln(cargo2);
				
				if( showmass && (Items.get().item(aitem.getItemID()).getCargo() != 1) ) {
					if( cargo1 != 0 ) {
						fcargo1 = fcargo1+" ("+(Items.get().item(aitem.getItemID()).getCargo()*cargo1)+")";
					}
					if( cargo2 != 0 ) {
						fcargo2 = fcargo2+" ("+(Items.get().item(aitem.getItemID()).getCargo()*cargo2)+")";
					}
				}
				
				if( !nohtml ) {			
					fcargo1 = "<a "+style+" onmouseover=\"return overlib('"+tooltiptext+"',TIMEOUT,0,DELAY,400,TEXTFONTCLASS,'smallTooltip');\" onmouseout=\"return nd();\" class=\""+linkclass+"\" href=\"./main.php?module=iteminfo&amp;sess="+sess+"&amp;itemlist="+aitem+"\">"+fcargo1+"</a>";
					fcargo2 = "<a "+style+" onmouseover=\"return overlib('"+tooltiptext+"',TIMEOUT,0,DELAY,400,TEXTFONTCLASS,'smallTooltip');\" onmouseout=\"return nd();\" class=\""+linkclass+"\" href=\"./main.php?module=iteminfo&amp;sess="+sess+"&amp;itemlist="+aitem+"\">"+fcargo2+"</a>";
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
		long[] cargo = subcargo.getCargoArray();
		List<Long[]> items = subcargo.getItemArray();

		for( int i=0; i <= MAX_RES; i++ ) {
			if( i == Resources.ITEMS.getID() ) continue;
			this.cargo[i] -= cargo[i];			
		}

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
		long[] cargo = addcargo.getCargoArray();
		List<Long[]> items = addcargo.getItemArray();

		for( int i=0; i <= MAX_RES; i++ ) {
			if( i == Resources.ITEMS.getID() ) continue;
			this.cargo[i] += cargo[i];			
		}

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
	 * das Ergebnis entsprechend des Rundungsmodus
	 * @param factor Der Faktor
	 * @param round Der Rundungsmodus
	 */
	public void multiply( double factor, Round round ) {
		for( int i=0; i <= MAX_RES; i++ ) {
			if( i == Resources.ITEMS.getID() ) continue;
			double val = cargo[i] * factor;
			switch( round ) {
			case NONE:
			case FLOOR:
				cargo[i] = (long)Math.floor(val);
				break;
			case ROUND:
				cargo[i] = Math.round(val);
				break;
			case CEIL:
				cargo[i] = (long)Math.ceil(val);
				break;
			}
		}
		
		if( items.size() > 0 ) {
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
					items.remove(aitem);
				}
			}
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
		Cargo retcargo = null;
		
		if( mass >= getMass() ) {
			retcargo = (Cargo)clone();
			cargo = new long[MAX_RES+1];
			items.clear();
			
			return retcargo;
		}
		retcargo = new Cargo();
		
		long currentmass = 0;

		for( int i=0; i <= MAX_RES; i++ ) {
			if( i == Resources.ITEMS.getID() ) continue;
			
			if( cargo[i] + currentmass < mass ) {
				currentmass += cargo[i];
				retcargo.getCargoArray()[i] = cargo[i];
				cargo[i] = 0;
			}
			else {
				retcargo.getCargoArray()[i] = retcargo.getCargoArray()[i] + (mass - currentmass);
				cargo[i] = cargo[i] - (mass - currentmass);
				currentmass = mass;
				break;
			}
		}
		
		if( currentmass != mass ) {
			if( items.size() > 0 ) {
				for( Long[] aitem : items ) {
					if( Items.get().item(aitem[0].intValue()).getCargo()*aitem[1] + currentmass < mass ) {
						currentmass += Items.get().item(aitem[0].intValue()).getCargo()*aitem[1];
						retcargo.getItemArray().add(aitem);
						items.remove(aitem);
					}
					else {
						Long[] newitem = aitem.clone();
						newitem[1] = (mass-currentmass)/Items.get().item(aitem[0].intValue()).getCargo();
						aitem[1] -= newitem[1];
						currentmass += Items.get().item(aitem[0].intValue()).getCargo()*newitem[1];
						retcargo.getItemArray().add(newitem);
					}
				}
			}
		}
		
		return retcargo;
	}
	
	/**
	 * Setzt die vorhandene Menge der angegebenen Resource auf 
	 * den angegebenen Wert
	 * @param resourceid Die Resourcen-ID
	 * @param count Die neue Menge
	 */
	public void setResource( ResourceID resourceid, long count ) {
		if( resourceid.isItem() ) {
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
				items.add( new Long[] {new Long(resourceid.getItemID()), count, new Long(resourceid.getUses()), new Long(resourceid.getQuest())} );
			}
		}
		else {
			cargo[resourceid.getID()] = count;
		}
	}
	
	/**
	 * Gibt das erstbeste Item mit dem angegebenen Effekt zurueck.
	 * Wenn kein Item diesen Effekt besitzt, wird <code>null</code> zurueckgegeben.
	 * @param itemeffectid Der gesuchte Item-Effekt
	 * @return Ein Item mit dem Effekt oder <code>null</code>
	 */
	public ItemCargoEntry getItemWithEffect( ItemEffect.Type itemeffectid ) {
		for( int i=0; i < items.size(); i++ ) {
			Long[] aitem = items.get(i);
			if( Items.get().item(aitem[0].intValue()).getEffect().getType() != itemeffectid ) {
				continue;
			}
			return new ItemCargoEntry(this, aitem[0].intValue(), aitem[1], aitem[2].intValue(), aitem[3].intValue());
		}
		return null;
	}
	
	/**
	 * Gibt eine Liste aller Items im Cargo mit dem gesuchten Effekt zurueck.
	 * Wenn kein Item den Effekt hat, wird eine leere Liste zurueckgegeben.
	 * @param itemeffectid Der gesuchte Item-Effekt
	 * @return Die Liste aller Items mit dem gesuchten Effekt
	 */
	public List<ItemCargoEntry> getItemsWithEffect( ItemEffect.Type itemeffectid ) {
		List<ItemCargoEntry> itemlist = new ArrayList<ItemCargoEntry>();
		
		for( int i=0; i < items.size(); i++ ) {
			Long[] aitem = items.get(i);
			if( Items.get().item(aitem[0].intValue()).getEffect().getType() != itemeffectid ) {
				continue;
			}
			itemlist.add( new ItemCargoEntry(this, aitem[0].intValue(), aitem[1], aitem[2].intValue(), aitem[3].intValue()));
		}
					
		return itemlist;
	}
	
	/**
	 * Prueft, ob der Cargo leer ist
	 * @return <code>true</code>, falls er leer ist
	 */
	public boolean isEmpty() {
		for( int i=0; i <= MAX_RES; i++ ) {
			if( i == Resources.ITEMS.getID() ) continue;
			
			if( cargo[i] > 0 ) {
				return false;	
			}	
		}
		
		for( int i=0; i < items.size(); i++ ) {
			Long[] aitem = items.get(i);
			if( aitem[1] > 0 ) {
				return false;
			}
		}	
		
		return true;
	}
	
	/**
	 * Setzt eine Option auf den angegebenen Wert
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
	
	@Override
	public Object clone() {
		try {
			Cargo cargo = (Cargo)super.clone();
			cargo.cargo = this.cargo.clone();
			cargo.orgcargo = this.orgcargo.clone();
			cargo.items = new ArrayList<Long[]>();
			for( int i=0; i < this.items.size(); i++ ) {
				cargo.items.add(i, this.items.get(i).clone());
			}
			cargo.orgitems = new ArrayList<Long[]>(this.orgitems);
			for( int i=0; i < this.orgitems.size(); i++ ) {
				cargo.orgitems.add(i, this.orgitems.get(i).clone());
			}
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
	 * Der Pfad ist bereits vollstaendig und in URL-Form
	 * 
	 * @param resid Die Resourcen-ID
	 * @return Der Pfad zum Bild
	 */
	public static String getResourceImage( ResourceID resid ) {
		if( resid.isItem() ) {
			if( Items.get().item(resid.getItemID()) != null ) {
				return Items.get().item(resid.getItemID()).getPicture();	
			}
			return "Kein passendes Item gefunden";	
		}
		Context context = ContextMap.getContext();
		if( (context != null) && (context.getActiveUser() != null) ) {
			return context.getActiveUser().getImagePath()+ResourceConfig.getResourceImage(resid.getID());
		}
		if( context != null ){
			return User.getDefaultImagePath(context.getDatabase())+ResourceConfig.getResourceImage(resid.getID());
		}
		return Configuration.getSetting("URL")+ResourceConfig.getResourceImage(resid.getID());
	}
	
	/**
	 * Gibt den Namen einer Resource zurueck
	 * @param resid Die Resourcen-ID
	 * @return Der Name
	 */
	public static String getResourceName( ResourceID resid ) {
		if( resid.isItem() ) {
			if( Items.get().item(resid.getItemID()) != null ) {
				return Items.get().item(resid.getItemID()).getName();	
			}
			return "Kein passendes Item gefunden";	
		}
		return ResourceConfig.getResourceName(resid.getID());
	}
	
	/**
	 * Gibt die Masse einer Resource in einer bestimmten Menge zurueck
	 * 
	 * @param resourceid Die Resourcen-ID
	 * @param count Die Menge
	 * @return Die Masse, die die Resource in der Menge verbraucht
	 */
	public static long getResourceMass( ResourceID resourceid, long count ) {
		long tmp = 0;
	
		if( resourceid.isItem() ) {
			if( Items.get().item(resourceid.getItemID()) != null ) {
				tmp = count * Items.get().item(resourceid.getItemID()).getCargo();
			}
		}
		else {
			tmp = count;
		}
		
		return tmp;
	}
	
	/**
	 * VERALTET - Nicht benutzen!
	 * @param rid ?
	 * @return ?
	 */
	@Deprecated
	public static Object[] getItemDataFromRID( int rid ) {
		// EMPTY
		return null;
	}
}

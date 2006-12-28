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
package net.driftingsouls.ds2.server.config;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Die Liste und Darstellungsdetails der Fraktionsseiten einer Fraktion
 * @author Christopher Jung
 *
 */
public class FactionPages {
	private Set<String> availablePages;
	private int menuSize;
	private String factionText;
	private boolean show;
	private int factionID;
	
	protected FactionPages( int factionID ) {
		this.factionID = factionID;
		this.availablePages = new LinkedHashSet<String>();
		this.menuSize = 500;
		this.show = true;
	}
	
	/**
	 * Gibt die ID der Fraktion zurueck
	 * @return Die ID
	 */
	public int getID() {
		return this.factionID;
	}
	
	/**
	 * Fuegt eine Seite zur Fraktion hinzu
	 * @param page Die ID der neuen Seite
	 */
	protected void addPage( String page ) {
		availablePages.add(page);
	}
	
	/**
	 * Prueft, ob die Fraktion die angegebene Seite besitzt
	 * @param page Die ID der Seite
	 * @return <code>true</code>, falls die Fraktion die angegebene Seite hat
	 */
	public boolean hasPage( String page ) {
		return availablePages.contains(page);	
	}
	
	/**
	 * Gibt die Liste aller Seiten der Fraktion zurueck
	 * @return Die Liste aller Seiten
	 */
	public Set<String> getPages() {
		return Collections.unmodifiableSet(availablePages);
	}
	
	/**
	 * Gibt die Startseite der Fraktion zurueck
	 * @return Die ID der Startseite oder <code>null</code>, falls die Fraktion keine Seiten besitzt
	 */
	public String getFirstPage() {
		if( !availablePages.isEmpty() ) {
			return availablePages.iterator().next();
		}
		return null;
	}
	
	/**
	 * Setzt die Menuegroesse der Fraktionsseite
	 * @param size Die neue Menuegroesse
	 */
	protected void setMenuSize( int size ) {
		this.menuSize = size;	
	}
	
	/**
	 * Gibt die Menuegroesse der Fraktion zurueck
	 * @return Die Menuegroesse
	 */
	public int getMenuSize() {
		return menuSize;	
	}
	
	/**
	 * Setzt den Beschreibungstext der Fraktion
	 * @param text Der neue Beschreibungstext
	 */
	protected void setFactionText( String text ) {
		this.factionText = text;
	}
	
	/**
	 * Gibt den Beschreibungstext der Fraktion zurueck
	 * @return Der Beschreibungstext
	 */
	public String getFactionText() {
		return this.factionText;	
	}
	
	/**
	 * Aktiviert die Fraktionsseiten
	 */
	protected void enable() {
		this.show = true;	
	}
	
	/**
	 * Deaktiviert die Fraktionsseiten
	 */
	protected void disable() {
		this.show = false;	
	}
	
	/**
	 * Prueft, ob die Fraktionsseiten aktiviert sind
	 * @return <code>true</code>, falls die Fraktionsseiten aktiviert sind
	 */
	public boolean isEnabled() {
		return this.show;
	}
	
	protected static FactionPages fromXML(Node node) throws Exception {
		int id = (int)XMLUtils.getLongAttribute(node, "id");
		
		FactionPages fac = new FactionPages(id);
		fac.setMenuSize((int)XMLUtils.getLongAttribute(node, "menusize"));
		
		String text = XMLUtils.getStringByXPath(node, "text/text()");
		fac.setFactionText(Common.trimLines(text));
		
		NodeList nodes = XMLUtils.getNodesByXPath(node, "page");
		for( int i=0; i < nodes.getLength(); i++ ) {
			String pageid = XMLUtils.getStringAttribute(nodes.item(i), "id");
			fac.addPage(pageid);
		}
		
		return fac;
	}
}

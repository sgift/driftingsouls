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

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Repraesentiert die Liste aller in DS bekannten Orden und Raenge.
 * Die Liste wird beim Start von DS aus der <code>medals.xml</code>
 * geladen.
 * 
 * @author Christopher Jung
 *
 */
public class Medals {
	private static final Log log = LogFactory.getLog(Medals.class);
	private static Medals medalList = new Medals();
	private Map<Integer, Medal> list = new LinkedHashMap<>();
	
	private Medals() {
		// EMPTY
	}

	private void addMedal( Medal mmedal ) {
		list.put(mmedal.getId(), mmedal);
	}

	
	/**
	 * Gibt die Instanz der Orden/Raenge-Liste zurueck.
	 * @return Die Medals-Listen-Instanz
	 */
	public static Medals get() {
		return medalList;
	}
	
	/**
	 * Gibt den Rang mit der angegebenen ID zurueck.
	 * Falls kein Rang mit der ID existiert wird der 
	 * Standardrang zurueckgegeben
	 * 
	 * @param id Die ID des gewuenschten Ranges
	 * @return Der Rang oder der Standardrang
	 */
	public Rang rang( int id ) {
		Rang rang = (Rang)ContextMap.getContext().getDB().get(Rang.class, id);
		if( rang == null )
		{
			rang = (Rang)ContextMap.getContext().getDB().get(Rang.class, 0);
		}
		return rang;
	}
	
	/**
	 * Gibt die Liste der Raenge zurueck.
	 * @return die Liste der Raenge
	 */
	public Map<Integer,Rang> raenge() {
		List<Rang> raenge = Common.cast(ContextMap.getContext().getDB().createCriteria(Rang.class).list());
		return new TreeMap<>(raenge.stream().collect(Collectors.toMap(Rang::getId, r -> r)));
	}
	
	/**
	 * Gibt den Orden mit der angegebenen ID zurueck.
	 * Falls kein Orden mit der ID existiert wird <code>null</code>
	 * zurueckgegeben.
	 * 
	 * @param id Die ID des gewuenschten Ordens
	 * @return Der Orden oder <code>null</code>
	 */
	public Medal medal( int id ) {
		return list.get(id);
	}
	
	/**
	 * Gibt die Liste der Orden zurueck.
	 * @return die Liste der Orden
	 */
	public Map<Integer,Medal> medals() {
		return Collections.unmodifiableMap(list);
	}
	
	static {
		/*
		 * medals.xml parsen
		 */
		try {
			Document doc = XMLUtils.readFile(Configuration.getConfigPath()+"medals.xml");
			NodeList nodes = XMLUtils.getNodesByXPath(doc, "medals/medal");
			for( int i=0; i < nodes.getLength(); i++ )
			{
				Node node = nodes.item(i);
				int id = (int) XMLUtils.getLongAttribute(node, "id");
				String name = XMLUtils.getStringAttribute(node, "name");

				Medal medal = new Medal(id, name);

				medalList.addMedal(medal);

				String adminonly = XMLUtils.getStringAttribute(node, "admin-only");
				if (adminonly != null)
				{
					medal.setAdminOnly(Boolean.parseBoolean(adminonly));
				}

				String image = XMLUtils.getStringByXPath(node, "image/text()");
				String highlight = XMLUtils.getStringByXPath(node, "highlight/text()");
				String small = XMLUtils.getStringByXPath(node, "small/text()");
				medal.setImages(image, highlight, small);
			}
		}
		catch( Exception e ) {
			log.fatal("FAILED: Kann Orden/Raenge nicht laden",e);
		}
	}
}

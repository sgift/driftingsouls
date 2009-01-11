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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;
import net.driftingsouls.ds2.server.namegenerator.NameGenerator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Repraesentiert die Rassen-Liste in DS. Die Rassen-Liste
 * wird beim Start von DS aus der Datei <code>rassen.xml</code>
 * geladen.
 * 
 * @author Christopher Jung
 *
 */
public class Rassen implements Iterable<Rasse> {
	private static final Log log = LogFactory.getLog(Rassen.class);
	private static Rassen rassenList = new Rassen();
	private Map<Integer, Rasse> list = new LinkedHashMap<Integer, Rasse>();
	
	private Rassen() {
		// EMPTY
	}

	private void addRace( Rasse rasse ) {
		list.put(rasse.getID(), rasse);
	}
	
	/**
	 * Liefert eine Instanz der Rassenliste.
	 * @return Eine Instanz der Rassenliste
	 */
	public static Rassen get() {
		return rassenList;
	}
	
	@Override
	public Iterator<Rasse> iterator() {
		return list.values().iterator();
	}
	
	/**
	 * Liefert die zu einer ID gehoerende Rasse.
	 * @param id Die ID der Rasse 
	 * @return Die zur ID gehoerende Rasse
	 */
	public Rasse rasse( int id ) {
		return list.get(id);
	}

	static {
		/*
		 * rassen.xml parsen
		 */
		try {
			Document doc = XMLUtils.readFile(Configuration.getSetting("configdir")+"rassen.xml");
			NodeList nodes = XMLUtils.getNodesByXPath(doc, "rassen/rasse");
			for( int i=0; i < nodes.getLength(); i++ ) {
				Node node = nodes.item(i);
				int id = XMLUtils.getNumberByXPath(node, "@id").intValue();
				boolean playable = "true".equals(XMLUtils.getStringByXPath(node, "@playable"));
				boolean extPlayable = "true".equals(XMLUtils.getStringByXPath(node, "@ext-playable"));
				int memberIn = -1;
				if( XMLUtils.getNodeByXPath(node, "@member-of") != null ) {
					memberIn = XMLUtils.getNumberByXPath(node, "@member-of").intValue();
				}
				
				String name = XMLUtils.getStringByXPath(node, "name/text()");
				Rasse rasse = new Rasse(id, name, playable, extPlayable, (memberIn > -1 ? rassenList.rasse(memberIn) : null) );
				
				// <description> verarbeiten
				if( XMLUtils.getStringByXPath(node, "description/text()") != null ) {
					rasse.setDescription(Common.trimLines(XMLUtils.getStringByXPath(node, "description/text()")));
				}
				
				// <head> verarbeiten
				NodeList heads = XMLUtils.getNodesByXPath(node, "head");
				for( int j=0; j < heads.getLength(); j++ ) {
					rasse.addHead(XMLUtils.getNumberByXPath(heads.item(j), "text()").intValue());
				}
				
				// <generator> verarbeiten
				NodeList generators = XMLUtils.getNodesByXPath(node, "generator");
				for( int j=0; j < generators.getLength(); j++ ) {
					String type = XMLUtils.getStringByXPath(generators.item(j), "@type");
					Rasse.GeneratorType typeNo = Rasse.GeneratorType.PERSON;
					if( "ship".equals(type) ) {
						typeNo = Rasse.GeneratorType.SHIP;
					}
					else if( !"person".equals(type) ) {
						throw new Exception("Ungueltiger Generator-Typ '"+type+"'");
					}
					
					Class<? extends NameGenerator> clazz = Class
						.forName(XMLUtils.getStringByXPath(generators.item(j),"text()"))
						.asSubclass(NameGenerator.class);
						
					rasse.setNameGenerator(typeNo, clazz.newInstance());
				}
				rassenList.addRace(rasse);
			}
		}
		catch( Exception e ) {
			log.fatal("FAILED: Kann Rassen nicht laden",e);
		}
	}
}

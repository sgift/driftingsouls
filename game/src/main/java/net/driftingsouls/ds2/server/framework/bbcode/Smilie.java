/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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
package net.driftingsouls.ds2.server.framework.bbcode;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ContextMap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Ein Smilie.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="smilies")
public class Smilie {
	@Id @GeneratedValue
	private int id;
	@Column(nullable = false)
	private String tag;
	@Column(nullable = false)
	private String image;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public Smilie() {
		this.tag = "";
		this.image = "";
	}

	/**
	 * Gibt die ID des Smilies zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Gibt das Bild des Smilies zurueck.
	 * @return Das Bild
	 */
	public String getImage() {
		return image;
	}

	/**
	 * Gibt den Tag des Smilies zurueck.
	 * @return Der Tag
	 */
	public String getTag() {
		return tag;
	}
	
	private static List<Pattern> smiliesSearch = null;
	private static List<String> smiliesReplace = null;
	private static final Object LOCK = new Object();
	
	/**
	 * Ersetzt alle bekannten Smilies in einem Text durch entsprechende Grafiken (als HTML-Code).
	 * @param text Der Text
	 * @return der Text mit den Grafiken
	 */
	public static String parseSmilies( String text ) {
		synchronized(LOCK) {
			if( smiliesSearch == null ) {
				smiliesSearch = new ArrayList<>();
				smiliesReplace = new ArrayList<>();
			
				org.hibernate.Session db = ContextMap.getContext().getDB();
				List<Smilie> smilies = Common.cast(db.createQuery("from Smilie").list());
				for( Smilie smilie : smilies ) {
					smiliesSearch.add(Pattern.compile("(?<=.\\W|\\W.|^\\W)"+Pattern.quote(smilie.getTag())+"(?=.\\W|\\W.|\\W$)"));
					smiliesReplace.add("<img style=\"border:0px\" src=\""+new ConfigService().getValue(WellKnownConfigValue.SMILIE_PATH)+"/"+smilie.getImage()+"\" alt=\""+smilie.getTag()+"\" title=\""+smilie.getTag()+"\" />");
				}
			}
		}
		
		for( int i=0; i < smiliesSearch.size(); i++ ) {
			text = smiliesSearch.get(i).matcher(' '+text+' ').replaceAll(smiliesReplace.get(i));
			text = text.substring(1, text.length()-1);
		}


		return text;
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}

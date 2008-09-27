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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.driftingsouls.ds2.server.namegenerator.NameGenerator;

/**
 * Repraesentiert eine Rasse in Drifting Souls.
 * 
 * @author Christopher Jung
 * 
 */
public class Rasse
{
	/**
	 * Enum aller Namensgeneratortypen, die eine Rasse besitzen kann
	 */
	public enum GeneratorType
	{
		/**
		 * Der Namensgenerator fuer Personennamen
		 */
		PERSON,
		/**
		 * Der Namensgenerator fuer Schiffsnamen
		 */
		SHIP;
	}

	private String name = null;
	private int id = 0;
	// Wenn es sich bei der "Rasse" um eine Gruppe wie die GCP handelt, MUSS memberIn=null sein!!!
	private Rasse memberIn = null;
	// Grundsaetzlich spielbar (Beim Registrieren auswaehlbar)
	private boolean playable = false;
	// Spaeter im Spiel spielbar (aufgrund von Fraktionsbeziehungen/RPGs) - automatisch "wahr", wenn
	// playable "wahr" ist
	private boolean playableext = false;
	// Liste aller Spieler, die diese Rasse/Fraktion kontrollieren
	private ArrayList<Integer> heads = new ArrayList<Integer>();
	// Pfad zum jeweiligen Namensgenerator fuer die verschiedenen Bereiche
	private Map<GeneratorType, NameGenerator> nameGenerator = new HashMap<GeneratorType, NameGenerator>();
	// Eine Beschreibung der Rasse - wird bei der Registrierung angezeigt
	private String description = "";

	protected Rasse(int id, String name, boolean playable)
	{
		this.name = name;
		this.id = id;
		this.playable = playable;
	}

	protected Rasse(int id, String name, boolean playable, boolean playableext, Rasse memberIn)
	{
		this(id, name, playable);
		this.memberIn = memberIn;

		if( !playable )
		{
			this.playableext = playableext;
		}
		else
		{
			this.playableext = true;
		}
	}

	/**
	 * Prueft, ob die Rasse direkt oder indirekt Mitglied in einer anderen Rasse ist
	 * 
	 * @param rasse die ID der Rasse, in der die Mitgliedschaft geprueft werden soll
	 * @return <code>true</code>, falls die Rasse Mitglied ist
	 */
	public boolean isMemberIn(int rasse)
	{
		if( rasse == -1 )
		{
			return true;
		}

		if( id == rasse )
		{
			return true;
		}

		if( (memberIn != null) && (memberIn.getID() == rasse) )
		{
			return true;
		}

		if( memberIn != null )
		{
			return memberIn.isMemberIn(rasse);
		}
		return false;
	}

	protected void addHead(int id)
	{
		if( !heads.contains(id) )
		{
			heads.add(id);
		}
	}

	/**
	 * Prueft, ob ein Spieler ein Kopf der Rasse ist
	 * 
	 * @param id die ID des Spielers
	 * @return <code>true</code>, falls der Spieler ein Kopf der Rasse ist
	 */
	public boolean isHead(int id)
	{
		return heads.contains(id);
	}

	/**
	 * Gibt die ID der Rasse zurueck
	 * 
	 * @return Die ID der Rasse
	 */
	public int getID()
	{
		return id;
	}

	/**
	 * Gibt den Namen der Rasse zurueck
	 * 
	 * @return der Name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Gibt zurueck, ob die Rasse spielbar ist, d.h. ob Spieler sich unter Angabe dieser Rasse
	 * registrieren koennen.
	 * 
	 * @return <code>true</code>, falls die Rasse spielbar ist
	 */
	public boolean isPlayable()
	{
		return playable;
	}

	/**
	 * Gibt zurueck, ob die Rasse "erweitert" spielbar ist. Diese Rassen werden im Forschungsbaum
	 * usw aufgelistet und koennen zu einem spaeteren Zeitpunkt im Spiel ausgewaehlt werden. Sie
	 * stehen jedoch nicht bei der Registrierung zur Auswahl.
	 * 
	 * @return <code>true</code>, falls die Rasse erweitert spielbar ist
	 */
	public boolean isExtPlayable()
	{
		return playableext;
	}

	/**
	 * Gibt den Pfad zur ausfuehrbaren Datei des Namensgenerators fuer den gewuenschten Typ zurueck.
	 * Der Pfad enthaelt bereits ggf notwendige Parameter. Es wird <code>null</code> zurueckgegeben,
	 * falls kein Namensgenerator fuer den Typ vorhanden ist.
	 * 
	 * @param type Der gewuenschte Namensgenerator-Typ
	 * @return Der Pfad zum Namensgenerator
	 */
	public NameGenerator getNameGenerator(GeneratorType type)
	{
		if( !nameGenerator.containsKey(type) && (memberIn != null) )
		{
			return memberIn.getNameGenerator(type);
		}
		return nameGenerator.get(type);
	}

	protected void setNameGenerator(GeneratorType type, NameGenerator generator)
	{
		nameGenerator.put(type, generator);
	}

	/**
	 * Gibt die Beschreibung der Rasse zurueck
	 * 
	 * @return Die Beschreibung der Rasse
	 */
	public String getDescription()
	{
		return description;
	}

	protected void setDescription(String description)
	{
		this.description = description;
	}

}

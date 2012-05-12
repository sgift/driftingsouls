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
package net.driftingsouls.ds2.server.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.JumpNode;

import org.hibernate.Session;

/**
 * Die allgemeine Sicht auf eine Sternenkarte ohne nutzerspezifische Anzeigen.
 * @author Christopher Jung
 *
 */
public class PublicStarmap
{

	protected Starmap map;

	/**
	 * Konstruktor.
	 * @param db Die DB-Verbindung
	 * @param system Die ID des Systems
	 */
	public PublicStarmap(Session db, StarSystem system)
	{
		this.map = createMap(db, system);
	}

	private Starmap createMap(Session db, StarSystem system)
	{
		Starmap map = (Starmap)db.get(Starmap.class, system.getID());
		if(map == null)
		{
			throw new IllegalArgumentException("The given system " + system.getID() + " does not exist.");
		}
		return map;
	}

	/**
	 * @return Alle fuer den Spieler oeffentlich sichtbaren Jumpnodes.
	 */
	public List<JumpNode> getPublicNodes()
	{
		List<JumpNode> publicNodes = new ArrayList<JumpNode>();
		Collection<JumpNode> nodes = map.getJumpNodes();
		for(JumpNode node: nodes)
		{
			if(!node.isHidden())
			{
				publicNodes.add(node);
			}
		}
		
		return publicNodes;
	}

	/**
	 * Gibt das Basisbild des Sektors zurueck. Das Bild enthaelt
	 * keine Flottenmarkierungen.
	 * @param location Der Sektor
	 * @return Das Bild als String ohne den Pfad zum Data-Verzeichnis.
	 */
	public String getSectorBaseImage(Location location)
	{
		return getBaseImage(location)+".png";
	}
	
	/**
	 * Gibt das zum Sektor passende Bild ohne Dateiendung zurueck.
	 * @param location Die Position des Sektors
	 * @return Das Bild passend zum Grundtyp des Sektors, ohne Dateiendung (z.B. "space/space")
	 */
	protected String getBaseImage(Location location)
	{
		if(isNebula(location))
		{
			return map.getNebulaMap().get(location).getImage();
		}
		List<Base> positionBases = map.getBaseMap().get(location);
		if(positionBases != null && !positionBases.isEmpty())
		{
			Base base = positionBases.get(0);
			return base.getBaseImage(location);
		}
		List<JumpNode> positionNodes = map.getNodeMap().get(location);
		if(positionNodes != null && !positionNodes.isEmpty())
		{
			for(JumpNode node: positionNodes)
			{
				if(!node.isHidden())
				{
					return "jumpnode/jumpnode";
				}
			}
			
			return "space/space";
		}
		return "space/space";
	}
	
	private boolean isNebula(Location location)
	{
		return map.isNebula(location);
	}
}
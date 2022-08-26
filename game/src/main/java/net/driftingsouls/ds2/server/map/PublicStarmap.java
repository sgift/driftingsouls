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

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.services.SingleUserRelationsService;

import java.util.*;

/**
 * Die allgemeine Sicht auf eine Sternenkarte ohne nutzerspezifische Anzeigen.
 * @author Christopher Jung
 *
 */
public class PublicStarmap
{

	protected Starmap map;
	protected HashMap<Location, ScanData> scanMap = new HashMap<>();
	//protected HashMap<Location, ScanData> nebulaScanMap = new HashMap<>();
	protected HashSet<Location> ownShipSectors = new HashSet<>();
	protected HashSet<Location> allyShipSectors = new HashSet<>();
	protected HashMap<Location, NonFriendScanData> enemyShipMap;
	protected HashMap<Location, NonFriendScanData> neutralShipMap;

	/**
	 * Konstruktor.
	 * @param systemId Die ID des Systems
	 */
	public PublicStarmap(int systemId)
	{
		this.map = new Starmap(systemId);
	}

	/**
	 * Gibt ein evt. abweichendes Basisbild des Sektors aus Sicht des Benutzers im Rahmen
	 * der spezifischen Sternenkarte zurueck. Das Bild enthaelt
	 * keine Flottenmarkierungen. Falls kein abweichendes Basisbild existiert
	 * wird <code>null</code> zurueckgegeben.
	 * @param location Der Sektor
	 * @return Die Informationen zum Bild oder <code>null</code>.
	 */
	public SectorImage getUserSectorBaseImage(Location location)
	{
		return null;
	}

	/**
	 * Gibt das Overlay-Bild des Sektors zurueck. Dieses
	 * enthaelt ausschliesslich spielerspezifische Markierungen
	 * und keinerlei Hintergrundelemente. Der Hintergrund
	 * des Bilds ist transparent.
	 * Falls keine Overlay-Daten fuer den Sektor angezeigt werden sollen
	 * wird <code>null</code> zurueckgegeben.
	 *
	 * @param location Der Sektor
	 * @return Das Bild als String ohne den Pfad zum Data-Verzeichnis oder <code>null</code>
	 */
	public SectorImage getSectorOverlayImage(Location location)
	{
		return null;
	}

	/**
	 * Gibt zurueck, ob der Sektor einen fuer den Spieler theoretisch sichtbaren Inhalt besitzt.
	 * Es spielt dabei einzig der Inhalt des Sektors eine Rolle. Nicht gerpueft wird,
	 * ob sich ein entsprechendes Schiff in scanreichweite befindet bzw ob der Spieler anderweitig
	 * den Inhalt des Sektors scannen kann.
	 * @param position Die Position
	 * @return <code>true</code>, falls der Sektor sichtbaren Inhalt aufweist.
	 */
	public boolean isHasSectorContent(Location position)
	{
		return false;
	}

	/**
	 * Gibt alle fuer den Sektor notwendigen Renderoperationen zurueck. Die Operationen
	 * sind in der Reinfolge abzuarbeiten um den Sektor korrekt darzustellen. Die Renderoperationen
	 * enthalten keine benutzerspezifischen Markierungen/Darstellungen
	 * @param location Der Sektor
	 * @return Die Renderanweisungen
	 */
	public List<RenderedSectorImage> getSectorBaseImage(Location location)
	{
		List<RenderedSectorImage> renderList = new ArrayList<>();
		renderList.add(new RenderedSectorImage("data/starmap/space/space.png", 0, 0, RenderedSectorImage.DEFAULT_MASK));

		var positionBases = map.getBaseMap().get(location);
		if(positionBases != null && !positionBases.isEmpty())
		{
			BaseData base = positionBases.get(0);
			int[] offset = base.getSectorImageOffset(location, base.getLocation());
			renderList.add(new RenderedSectorImage(base.getSectorImage(location), offset[0], offset[1], RenderedSectorImage.DEFAULT_MASK));
		}

		List<Starmap.JumpNode> positionNodes = map.getNodeMap().get(location);
		if(positionNodes != null && !positionNodes.isEmpty())
		{
			for(Starmap.JumpNode node: positionNodes)
			{
				if(!node.isHidden())
				{
					renderList.add(new RenderedSectorImage("data/starmap/jumpnode/jumpnode.png", 0, 0, RenderedSectorImage.DEFAULT_MASK));
				}
			}
		}
		if(map.isNebula(location))
		{
			double[] nebulas = new double[9];
			for( int i=0; i < 9; i++ )
			{
 				nebulas[i] = map.isNebula(new Location(location.getSystem(), location.getX()+(i%3-1), location.getY()+(i/3-1))) ? 1.0 : 0.0;
			}

			double[] mask = new double[] {
				nebulas[1]/3+nebulas[3]/3+nebulas[0]/3, nebulas[1], nebulas[2]/3+nebulas[1]/3+nebulas[5]/3,
				nebulas[3], nebulas[4], nebulas[5],
				nebulas[3]/3+nebulas[6]/3+nebulas[7]/3, nebulas[7], nebulas[7]/3+nebulas[5]/3+nebulas[8]/3
			};

			renderList.add(new RenderedSectorImage(map.getNebulaMap().get(location).getImage(), 0, 0, mask));
		}
		return renderList;
	}

	/**
	 * Gibt zurueck, ob an der gegebenen Position eine (bekannte/sichtbare)
	 * Schlacht stattfindet.
	 * @param sektor Die Position
	 * @return <code>true</code> falls dem so ist
	 */
	public boolean isSchlachtImSektor(Location sektor)
	{
		return false;
	}

	/**
	 * Gibt zurueck, ob der Sektor als Sektor mit Schiffen auf Alarmstufe
	 * Rot bzw Gelb dargestellt werden soll, d.h. ein Spieler moeglicherweise
	 * beim Einflug in den Sektor angegriffen wird.
	 * @param sektor Der Sektor
	 * @return <code>true</code>, falls dem so ist
	 */
	public boolean isRoterAlarmImSektor(Location sektor)
	{
		return false;
	}

	public Nebel.Typ getNebula(Location sektor)
	{
		var nebulas = map.getNebulaMap();
		return nebulas.getOrDefault(sektor, null);
	}

	public boolean isScanned(Location location)
	{
		return false;
	}

	public int getScanningShip(Location location)
	{
		return -1;
	}

	public boolean hasRocks(Location location) {
		return map.getRockPositions().contains(location);
	}

	protected void buildFriendlyData()
	{

	}

	public Collection<ScanData> getScanSectorData()
	{
		return scanMap.values();
	}

	protected SingleUserRelationsService UserRelationsService;
}
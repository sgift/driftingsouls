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
package net.driftingsouls.ds2.server.bases;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeInfo;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeMaxValues;
import net.driftingsouls.ds2.server.framework.Common;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Repraesentiert eine Basis-Klasse in DS.</p>
 * 
 */
@Entity
@Table(name="base_types")
public class BaseType
{
	@Id @GeneratedValue
	private int id;
	@Column(nullable = false)
	private String name;
	private int width;
	private int height;
	private int maxtiles;
	private int cargo;
	private int energy;
	@Lob
	private String terrain;
	@Lob
	private String spawnableress;
	private int size;
    @OneToMany(mappedBy="type")
    private Set<UpgradeMaxValues> upgradeMaxValues = new HashSet<>();
	@OneToMany(mappedBy="type")
	private Set<UpgradeInfo> upgradeInfos = new HashSet<>();
	private String smallImage;
	private String largeImage;
	private String starmapImage;
	
	/**
	 * Konstruktor.
	 *
	 */
	protected BaseType()
	{
		// EMPTY
	}

	/**
	 * Konstruktor.
	 * @param name Der Name des Typs
	 */
	public BaseType(String name)
	{
		this.name = name;
	}

	/**
	 * Copy-Konstruktor.
	 * @param baseType Die als Basis zu verwendende Basis-Klasse.
	 *
	 */
	public BaseType(BaseType baseType)
	{
		this.id = baseType.id;
		this.name = baseType.name;
		this.width = baseType.width;
		this.height = baseType.height;
		this.maxtiles = baseType.maxtiles;
		this.cargo = baseType.cargo;
		this.energy = baseType.energy;
		this.terrain = baseType.terrain;
		this.spawnableress = baseType.spawnableress;
		this.size = baseType.size;
		this.smallImage = baseType.smallImage;
		this.largeImage = baseType.largeImage;
		this.starmapImage = baseType.starmapImage;
	}
	
	/**
	 * Gibt die ID der Basis-Klasse zurueck.
	 * @return die ID der Basis-Klasse
	 */
	public int getId()
	{
		return this.id;
	}

	/**
	 * Gibt die Breite der Bauflaeche auf der Basis in Feldern zurueck.
	 * @return Die Breite
	 */
	public int getWidth()
	{
		return this.width;
	}
	
	/**
	 * Setzt die Breite der Bauflaeche auf der Basis.
	 * @param width Die Breite
	 */
	public void setWidth(int width)
	{
		this.width = width;
	}
	
	/**
	 * Gibt die Hoehe der Bauflaeche auf der Basis in Feldern zurueck.
	 * @return Die Hoehe
	 */
	public int getHeight()
	{
		return this.height;
	}
	
	/**
	 * Setzt die Hoehe der Bauflaeche auf der Basis.
	 * @param height Die Hoehe
	 */
	public void setHeight(int height)
	{
		this.height = height;
	}
	
	/**
	 * Gibt die benutzte Felderanzahl zurueck.
	 * @return die Felderanzahl
	 */
	public int getMaxTiles()
	{
		return this.maxtiles;
	}
	
	/**
	 * Setzt die benutzte Felderanzahl.
	 * @param maxtiles die neue Felderanzahl
	 */
	public void setMaxTiles(int maxtiles)
	{
		this.maxtiles = maxtiles;
	}
	
	/**
	 * Gibt den Namen der Basis zurueck.
	 * @return Der Name
	 */
	public String getName()
	{
		return this.name;
	}
	
	/**
	 * Gibt der Basis einen neuen Namen.
	 * @param name Der neue Name
	 */
	public void setName(String name)
	{
		this.name = name;
	}
	
	/**
	 * Gibt die erlaubten Terrain-Typen der Basis-Klasse zurueck.
	 * @return Die erlaubten Terrain-Typen der Felder
	 */
	public Integer[] getTerrain()
	{
		if(this.terrain == null || this.terrain.equals(""))
		{
			return null;
		}
		return Common.explodeToInteger(";", this.terrain);
	}
	
	/**
	 * Setzt das neue Terrain der Basis.
	 * @param terrain Das neue Terrain
	 */
	public void setTerrain(final Integer[] terrain)
	{
		this.terrain = Common.implode(";", terrain);
	}
	
	/**
	 * Gibt den Max-Cargo der Basis-Klasse zurueck.
	 * @return Der Cargo
	 */
	public int getCargo()
	{
		return cargo;
	}
	
	/**
	 * Setzt den Max-Cargo der Basis-Klasse.
	 * @param cargo Der neue Cargo
	 */
	public void setCargo(int cargo) 
	{
		this.cargo = cargo;
	}

	/**
	 * Gibt die maximale Energiemenge der Basis-Klasse zurueck.
	 * @return Die Energiemenge
	 */
	public int getEnergy()
	{
		return this.energy;
	}
	
	/**
	 * Setzt die maximale Menge der auf der Basis-Klasse vorhandenen Energie.
	 * @param e Die auf der Basis vorhandene Energie
	 */
	public void setEnergy(int e)
	{
		this.energy = e;
	}
	
	/**
	 * Gibt die zum spawn freigegebenen Ressourcen zurueck.
	 * @return Die Spawn-Ressourcen als String (itemid,chance,maxmenge)
	 */
	public String getSpawnableRess() 
	{
		if (this.spawnableress == null )
		{
			return "";
		}
		return this.spawnableress;
	}
	
	/**
	 * Setzt die zum spawn freigegebenen Ressourcen zurueck.
	 * @param spawnableRess Die Spawn-Ressourcen als String
	 */
	public void setSpawnableRess(String spawnableRess)
	{
		this.spawnableress = spawnableRess;
	}

	/**
	 * Gibt den Radius des Basentyps auf der Sternenkarte zurueck.
	 * @return Der Radius
	 */
	public int getSize()
	{
		return size;
	}

	/**
	 * Setzt den Radius des Basentyps auf der Sternenkarte.
	 * @param size Der Radius
	 */
	public void setSize(int size)
	{
		this.size = size;
	}

    /**
     * Gibt die Maximalwerte fuer den Ausbau von Basen dieses Typs zurueck.
     * @return Die Maximalwerte oder <code>null</code>, falls kein Ausbau moeglich ist
     */
    public Set<UpgradeMaxValues> getUpgradeMaxValues()
    {
        return upgradeMaxValues;
    }

    /**
     * Setzt die Maximalwerte fuer den Ausbau von Basen dieses Typs.
     * @param upgradeMaxValues Die Maximalwerte oder <code>null</code>, falls kein Ausbau moeglich ist
     */
    public void setUpgradeMaxValues(Set<UpgradeMaxValues> upgradeMaxValues)
    {
        this.upgradeMaxValues = upgradeMaxValues;
    }

	/**
	 * Gibt alle moeglichen Ausbauoptionen fuer Basen dieses Typs zurueck.
	 * @return Die Ausbauoptionen
	 */
	public Set<UpgradeInfo> getUpgradeInfos()
	{
		return upgradeInfos;
	}

	/**
	 * Setzt alle moeglichen Ausbauoptionen fuer Basen dieses Typs.
	 * @param upgradeInfos Die Ausbauoptionen
	 */
	public void setUpgradeInfos(Set<UpgradeInfo> upgradeInfos)
	{
		this.upgradeInfos = upgradeInfos;
	}

	/**
	 * Gibt den Pfad zum kleinen Bild des Basistyps zurueck, z.B. zur Verwendung als Icon in der Basisliste.
	 * @return Das Bild
	 */
	public String getSmallImage() {
		return smallImage;
	}

	/**
	 * Setzt den Pfad zum kleinen Bild des Basistyps, z.B. zur Verwendung als Icon in der Basisliste.
	 * @param smallImage Das Bild
	 */
	public void setSmallImage(String smallImage)
	{
		this.smallImage = smallImage;
	}

	/**
	 * Gibt den Pfad zum grossen Bild des Basistyps zurueck, z.B. zur Verwendung in der SRS-Ansicht.
	 * @return Das Bild
	 */
	public String getLargeImage()
	{
		return largeImage;
	}

	/**
	 * Setzt den Pfad zum grossen Bild des Basistyps, z.B. zur Verwendung in der SRS-Ansicht.
	 * @param largeImage  Das Bild
	 */
	public void setLargeImage(String largeImage)
	{
		this.largeImage = largeImage;
	}

	/**
	 * Gibt das Bild der in einem Sektor der Sternenkarte zurueck.
	 * Dabei wird die Ausdehnung beruecksichtigt. Zudem
	 * kann das zurueckgelieferte Bild mehrere Sektoren umfassen. Der korrekte
	 * Offset zur Darstellung des angefragten Sektors kann mittels
	 * {@link #getSectorImageOffset(net.driftingsouls.ds2.server.Location,net.driftingsouls.ds2.server.Location)}
	 * ermittelt werden.
	 *
	 * @param location Koordinate fuer die das Bild der Basis ermittelt werden soll.
	 * @param baseLocation Die Koordinate der Basis (Mittelpunkt)
	 * @return Der Bildstring der Basis oder einen Leerstring, wenn die Basis die Koordinaten nicht schneidet
	 */
	public String getSectorImage(Location location, Location baseLocation)
	{
		if(!location.sameSector(0, baseLocation, size))
		{
			return "";
		}

		return starmapImage;
	}

	/**
	 * Gibt den Pfad zum Bild auf der Sternenkarte zurueck. Das Bild kann mehrere Sektoren umfassen. Fuer
	 * die Abfrage zwecks Darstellung in einem Sektor sollte die Methode
	 * {@link #getSectorImage(net.driftingsouls.ds2.server.Location, net.driftingsouls.ds2.server.Location)}
	 * verwendet werden.
	 * @return Der Pfad zum Bild
	 */
	public String getStarmapImage()
	{
		return starmapImage;
	}

	/**
	 * Setzt den Pfad zum Bild auf der Sternenkarte. Das Bild kann mehrere Sektoren umfassen.
	 * @param starmapImage Der Pfad zum Bild
	 */
	public void setStarmapImage(String starmapImage)
	{
		this.starmapImage = starmapImage;
	}

	/**
	 * Ermittelt den Offset in Sektoren fuer die Darstellung des von
	 * {@link #getSectorImage(net.driftingsouls.ds2.server.Location,net.driftingsouls.ds2.server.Location)} ermittelten Bildes.
	 * Der ermittelte Offset ist immer Negativ oder <code>0</code> und stellt
	 * die Verschiebung der Grafik selbst dar (vgl. CSS-Sprites).
	 *
	 * @param location Koordinate fuer die das Bild der Basis ermittelt werden soll.
	 * @param baseLocation Die Koordinate der Basis (Mittelpunkt)
	 * @return Der Offset als Array (<code>[x,y]</code>)
	 */
	public int[] getSectorImageOffset(Location location, Location baseLocation)
	{
		if( size == 0 || !location.sameSector(0, baseLocation, size))
		{
			return new int[] {0,0};
		}

		for(int by = baseLocation.getY() - getSize(); by <= baseLocation.getY() + getSize(); by++)
		{
			for(int bx = baseLocation.getX() - getSize(); bx <= baseLocation.getX() + getSize(); bx++)
			{
				Location loc = new Location(baseLocation.getSystem(), bx, by);

				if( !baseLocation.sameSector(0, loc, getSize()))
				{
					continue;
				}

				if(location.equals(loc))
				{
					return new int[] {-bx+baseLocation.getX()-size, -by+baseLocation.getY()-size};
				}
			}
		}
		return new int[] {0,0};
	}
}

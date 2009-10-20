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
package net.driftingsouls.ds2.server.ships;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.framework.Common;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 * Ein Changeset fuer Schiffstypendaten-Aenderungen, wie sie z.B. von
 * Modulen vorgenommen werden koennen.
 * @author Christopher Jung
 *
 */
public class ShipTypeChangeset {
	
	private String nickname;
	private String picture;
	private int ru;
	private int rd;
	private int ra;
	private int rm;
	private int eps;
	private int cost;
	private int hull;
	private int panzerung;
	private int ablativeArmor;
	private long cargo;
	private int heat;
	private int crew;
	private int marines;
	private Map<String,Integer[]> weapons = new HashMap<String,Integer[]>();
	private Map<String,Integer> maxHeat;
	private int torpedoDef;
	private int shields;
	private int size;
	private int jDocks;
	private int aDocks;
	private int sensorRange;
	private int hydro;
	private int deutFactor;
	private int reCost;
	private String flags;
	private int werft;
	private int oneWayWerft;
	private String pictureMod;
	private Boolean srs;
	private int scanCost;
	private int pickingCost;
	private int minCrew;
	private double lostInEmpChance;
	
	/**
	 * Konstruktor.
	 * @param changesetString Der String mit den Changeset-Informationen
	 */
	public ShipTypeChangeset(String changesetString)
	{
		
		if( changesetString.equals(""))
		{
			throw new RuntimeException("Keine Shiptype-Changeset-Informationen vorhanden");
		}
		String[] changesets = StringUtils.split(changesetString, "|");
		for ( int i=0; i < changesets.length; i++)
		{
			String[] changeset = StringUtils.split(changesets[i], ",");
			if( changeset[0].equals("weapons"))
			{
				String[] weapon = StringUtils.split(changeset[1], "/");
				
				// Sicherstellen, dass die Waffe existiert
				Weapons.get().weapon(weapon[0]);
				
				weapons.put(weapon[0], new Integer[] {Integer.parseInt(weapon[1]), Integer.parseInt(weapon[2])});
			}
			else if( changeset[0].equals("flags"))
			{
				List<String> flagList = new ArrayList<String>();
				String[] flags = StringUtils.split(changeset[1], "/");
				for( int j=0; j< flags.length; j++)
				{
					flagList.add(flags[j]);
				}
				this.flags = Common.implode(" ", flagList);
			}
			else if( changeset[0].equals("nickname"))
			{
				this.nickname = changeset[1];
			}
			else if( changeset[0].equals("picture") ) 
			{
				this.picture = changeset[1];
			}
			else if( changeset[0].equals("ru") ) 
			{
				this.ru = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("rd") ) 
			{
				this.rd = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("ra") ) 
			{
				this.ra = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("rm") ) 
			{
				this.rm = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("eps") ) 
			{
				this.eps = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("cost") ) 
			{
				this.cost = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("hull") ) 
			{
				this.hull = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("panzerung") ) 
			{
				this.panzerung = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("ablativearmor") ) 
			{
				this.ablativeArmor = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("cargo") ) 
			{
				this.cargo = Long.parseLong(changeset[1]);
			}
			else if( changeset[0].equals("heat") ) 
			{
				this.heat = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("crew") ) 
			{
				this.crew = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("marines") ) 
			{
				this.marines = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("torpedodef") ) 
			{
				this.torpedoDef = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("shields") ) 
			{
				this.shields = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("size") ) 
			{
				this.size = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("jdocks") ) 
			{
				this.jDocks = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("adocks") ) 
			{
				this.aDocks = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("sensorrange") ) 
			{
				this.sensorRange = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("hydro") ) 
			{
				this.hydro = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("deutfactor") ) 
			{
				this.deutFactor = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("recost") ) 
			{
				this.reCost = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("werft") ) 
			{
				this.werft = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("ow_werft") ) 
			{
				this.oneWayWerft = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("picture_mod") ) 
			{
				this.pictureMod = changeset[1];
			}
			else if( changeset[0].equals("srs") ) 
			{
				this.srs = Boolean.parseBoolean(changeset[1]);
			}
			else if( changeset[0].equals("scan-cost") ) 
			{
				this.scanCost = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("picking-cost") ) 
			{
				this.pickingCost = Integer.parseInt(changeset[1]);
			}
			else if( changeset[0].equals("minCrew"))
			{
				this.minCrew = Integer.parseInt(changeset[1]);
			}
			else if (changeset[0].equals("lostInEmpChance"))
			{
				this.lostInEmpChance = Double.parseDouble(changeset[1]);
			}
			else
			{
				throw new RuntimeException("Unbekannte Changeset-Eigenschaft '"+changeset[0]+"'");
			}
		}
	}

	/**
	 * Gibt zurueck, um wieviel die externen Docks modifiziert werden.
	 * @return Die externen Docks
	 */
	public int getADocks() {
		return aDocks;
	}

	/**
	 * Gibt zurueck, um wieviel der Cargo modifiziert wird.
	 * @return Der Cargo
	 */
	public long getCargo() {
		return cargo;
	}

	/**
	 * Gibt zurueck, um wieviel die Flugkosten modifiziert werden.
	 * @return Die Flugkosten
	 */
	public int getCost() {
		return cost;
	}

	/**
	 * Gibt zurueck, um wieviel die Crew modifiziert wird.
	 * @return Die Crew
	 */
	public int getCrew() {
		return crew;
	}

	/**
	 * Gibt zurueck, um wieviel die Marines modifiziert werden sollen.
	 * @return Die Marines
	 */
	public int getMarines() {
		return marines;
	}

	/**
	 * Gibt zurueck, um wieviel der Deutfaktor modifiziert wird.
	 * @return Der Deutfaktor
	 */
	public int getDeutFactor() {
		return deutFactor;
	}

	/**
	 * Gibt zurueck, um wieviel die EPS modifiziert werden.
	 * @return Die EPS
	 */
	public int getEps() {
		return eps;
	}

	/**
	 * Gibt zurueck, welche Flags zusaetzlich gesetzt werden.
	 * @return Die neuen Flags
	 */
	public String getFlags() {
		return flags;
	}

	/**
	 * Gibt zurueck, um wieviel die Antriebsueberhitzung modifiziert wird.
	 * @return Die Antriebsueberhitzung
	 */
	public int getHeat() {
		return heat;
	}

	/**
	 * Gibt zurueck, um wieviel die Huelle modifiziert wird.
	 * @return Die Huelle
	 */
	public int getHull() {
		return hull;
	}

	/**
	 * Gibt zurueck, um wieviel die Nahrungsproduktion modifiziert wird.
	 * @return Die Nahrungsproduktion
	 */
	public int getHydro() {
		return hydro;
	}

	/**
	 * Gibt zurueck, um wieviel die Jaegerdocks modifiziert werden.
	 * @return Die Jaegerdocks
	 */
	public int getJDocks() {
		return jDocks;
	}

	/**
	 * Gibt zurueck, wie die Waffenueberhitzung modifiziert wird.
	 * @return Die Waffenueberhitzung
	 */
	public Map<String, Integer> getMaxHeat() {
		if( this.maxHeat == null ) {
			return null;
		}
		return Collections.unmodifiableMap(maxHeat);
	}

	/**
	 * Gibt den neuen Nickname zurueck.
	 * @return Der Name
	 */
	public String getNickname() {
		return nickname;
	}

	/**
	 * Gibt die Einweg-Werftdaten zurueck.
	 * @return Die Einweg-Werftdaten
	 */
	public int getOneWayWerft() {
		return oneWayWerft;
	}

	/**
	 * Gibt zurueck, um wieviel die Panzerung modifiziert wird.
	 * @return Die Panzerung
	 */
	public int getPanzerung() {
		return panzerung;
	}
	
	/**
	 * Gibt zurueck, um wieviel die ablative Panzerung modifiziert wird.
	 * @return Die ablative Panzerung
	 */
	public int getAblativeArmor() {
		return this.ablativeArmor;
	}

	/**
	 * Gibt das neue Bild zurueck.
	 * @return Das Bild
	 */
	public String getPicture() {
		return picture;
	}

	/**
	 * Gibt zurueck, um wieviel der Reaktorwert fuer Antimaterie modifiziert wird.
	 * @return Der Reaktorwert
	 */
	public int getRa() {
		return ra;
	}

	/**
	 * Gibt zurueck, um wieviel der Reaktorwert fuer Deuterium modifiziert wird.
	 * @return Der Reaktorwert
	 */
	public int getRd() {
		return rd;
	}

	/**
	 * Gibt zurueck, um wieviel die Wartungskosten modifiziert werden.
	 * @return Die Wartungskosten
	 */
	public int getReCost() {
		return reCost;
	}

	/**
	 * Gibt zurueck, um wieviel die Gesamtenergieproduktion des Reaktors modifiziert wird.
	 * @return Die Gesamtenergieproduktion
	 */
	public int getRm() {
		return rm;
	}

	/**
	 * Gibt zurueck, um wieviel der Reaktorwert fuer Uran modifiziert wird.
	 * @return Der Reaktorwert
	 */
	public int getRu() {
		return ru;
	}

	/**
	 * Gibt zurueck, um wieviel die Sensorenreichweite modifiziert wird.
	 * @return Die Sensorenreichweite
	 */
	public int getSensorRange() {
		return sensorRange;
	}

	/**
	 * Gibt zurueck, um wieviel die Schilde modifiziert werden.
	 * @return Die Schilde
	 */
	public int getShields() {
		return shields;
	}

	/**
	 * Gibt zurueck, um wieviel die Schiffsgroesse modifiziert wird.
	 * @return Die Schiffsgroesse
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Gibt zurueck, um wieviel die Torpedoabwehr modifiziert wird.
	 * @return Die Torpedoabwehr
	 */
	public int getTorpedoDef() {
		return torpedoDef;
	}

	/**
	 * Gibt die Modifikationsdaten der Waffen zurueck.
	 * @return Die Modifikationsdaten der Waffen
	 */
	public Map<String, Integer[]> getWeapons() {
		if( this.weapons == null ) {
			return null;
		}
		Map<String,Integer[]> map = new HashMap<String,Integer[]>();
		for( Entry<String,Integer[]> entry : this.weapons.entrySet() ) {
			map.put(entry.getKey(), entry.getValue().clone());
		}
		return map;
	}

	/**
	 * Gibt die neuen Werftdaten zurueck.
	 * @return Die Werftdaten
	 */
	public int getWerft() {
		return werft;
	}
	
	/**
	 * Gibt die Bildmodifikatoren zurueck.
	 * @return Die Bildmodifikatoren
	 */
	public String getPictureMods() {
		return pictureMod;
	}
	
	/**
	 * Gibt zurueck, ob SRS vorhanden sein sollen.
	 * @return <code>true</code>, falls SRS vorhanden sein sollen
	 */
	public Boolean hasSrs() {
		return srs;
	}
	
	/**
	 * Gibt zurueck, wieviel ein LRS-Scan an Energie kosten soll.
	 * @return Die Energiekosten
	 */
	public int getScanCost() {
		return scanCost;
	}

	/**
	 * Gibt zurueck, wieviel ein LRS-Sektorscan (Scannen des Inhalts eines Sektors) kosten soll.
	 * @return Die Energiekosten
	 */
	public int getPickingCost() {
		return pickingCost;
	}
	
	/**
	 * @return Crewwert bei dem das Schiff noch normal funktioniert.
	 */
	public int getMinCrew()
	{
		return this.minCrew;
	}
	
	/**
	 * Wahrscheinlichkeit, dass das Schiff sich in einem EMP-Nebel verfliegt.
	 * 
	 * @return Zahl zwischen 0 und 1.
	 */
	public double getLostInEmpChance()
	{
		return this.lostInEmpChance;
	}

	/**
	 * Wendet das Changeset auf die angegebenen Schiffstypendaten an.
	 * @param type Die Schiffstypendaten
	 * @return Die modifizierten Daten
	 */
	public ShipTypeData applyTo(ShipTypeData type) {
		return new ShipTypeDataAdapter(type, new String[0]);
	}
	
	/**
	 * Wendet das Changeset auf die angegebenen Schiffstypendaten an.
	 * @param type Die Schiffstypendaten
	 * @param replaceWeapons Die Waffen, welche ggf ersetzt werden sollen
	 * @return Die modifizierten Daten
	 */
	public ShipTypeData applyTo(ShipTypeData type, String[] replaceWeapons) {
		return new ShipTypeDataAdapter(type, replaceWeapons);
	}
	
	private class ShipTypeDataAdapter implements ShipTypeData {
		private ShipTypeData inner;
		private String[] weaponrepl;
		private volatile String flags;
		private volatile String weapons;
		private volatile String maxheat;
		private volatile String baseWeapons;
		private volatile String baseHeat;
		
		ShipTypeDataAdapter(ShipTypeData type, String[] weaponrepl) {
			this.inner = type;
			this.weaponrepl = weaponrepl;
		}

		@Override
		public Object clone() throws CloneNotSupportedException {
			// Wenn die innere Klasse immutable ist, dann ist diese Klasse ebenfalls
			// immutable -> CloneNotSupportedException
			ShipTypeData inner = (ShipTypeData)this.inner.clone();
			
			ShipTypeDataAdapter clone = (ShipTypeDataAdapter)super.clone();
			clone.inner = inner;
			clone.weaponrepl = this.weaponrepl.clone();
			
			return clone;
		}

		public int getADocks() {
			int value = inner.getADocks() + ShipTypeChangeset.this.getADocks();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		public long getCargo() {
			long value = inner.getCargo() + ShipTypeChangeset.this.getCargo();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		public int getChance4Loot() {
			return inner.getChance4Loot();
		}

		public int getCost() {
			if( getType().getCost() > 0 ) {
				int value = inner.getCost() + ShipTypeChangeset.this.getCost();
				if( value < 1 ) {
					return 1;
				}
				return value;
			}
			return inner.getCost();
		}

		public int getCrew() {
			if( getType().getCrew() > 0 ) {
				int value = inner.getCrew() + ShipTypeChangeset.this.getCrew();
				if( value < 1 ) {
					return 1;
				}
				return value;
			}
			return inner.getCrew();
		}
		
		public int getMarines() {
			if( getType().getMarines() > 0 ) {
				int value = inner.getMarines() + ShipTypeChangeset.this.getMarines();
					if( value < 1 ) {
						return 1;
					}
				return value;
			}
			return inner.getMarines();
		}

		public String getDescrip() {
			return inner.getDescrip();
		}

		public int getDeutFactor() {
			int value = inner.getDeutFactor() + ShipTypeChangeset.this.getDeutFactor();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		public int getEps() {
			int value = inner.getEps() + ShipTypeChangeset.this.getEps();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		public String getFlags() {
			if( this.flags == null ) {
				String flags = inner.getFlags();
				
				if( ShipTypeChangeset.this.getFlags() != null ) {
					String[] flagArray = StringUtils.split(ShipTypeChangeset.this.getFlags(), ' ');
					for( int i=0; i < flagArray.length; i++ ) {
						String aflag = flagArray[i];
						if( (flags.length() != 0) && (flags.indexOf(aflag) == -1) ) {
							flags += ' '+aflag;	
						}
						else if( flags.length() == 0 ) {
							flags = aflag;	
						}
					}
				}
				this.flags = flags;
			}
			return flags;
		}

		public int getGroupwrap() {
			return inner.getGroupwrap();
		}

		public int getHeat() {
			if( getType().getHeat() > 0 ) {
				int value = inner.getHeat() + ShipTypeChangeset.this.getHeat();
				if( value < 2 ) {
					return 2;
				}
				return value;
			}
			return inner.getHeat();
		}

		public int getHull() {
			int value = inner.getHull() + ShipTypeChangeset.this.getHull();
			if( value < 1 ) {
				return 1;
			}
			return value;
		}

		public int getHydro() {
			int value = inner.getHydro() + ShipTypeChangeset.this.getHydro();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		public int getJDocks() {
			int value = inner.getJDocks() + ShipTypeChangeset.this.getJDocks();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		private void calcWeaponData() {
			String[] wpnrpllist = this.weaponrepl;
			int index = 0;
			
			String wpnrpl = wpnrpllist.length > index ? wpnrpllist[index++] : null;
			
			String baseWeapons = inner.getWeapons();
			String baseHeat = inner.getMaxHeat();
			
			Map<String,String> weaponlist = Weapons.parseWeaponList(baseWeapons);
			Map<String,String> heatlist = Weapons.parseWeaponList(baseHeat);
			
			// Weapons
			Map<String,Integer[]> mod = ShipTypeChangeset.this.getWeapons();
			if( mod != null ) {
				for( Map.Entry<String, Integer[]> entry: mod.entrySet() ) {
					String aweapon = entry.getKey();
					Integer[] awpnmods = entry.getValue();
					
					int acount = awpnmods[0];
					int aheat = awpnmods[1];
					
					if( wpnrpl != null ) {		
						if( NumberUtils.toInt(weaponlist.get(wpnrpl)) > 0 ) {
							if( NumberUtils.toInt(weaponlist.get(wpnrpl)) > acount ) {
								int rplCount = NumberUtils.toInt(weaponlist.get(wpnrpl));
								int rplHeat = NumberUtils.toInt(heatlist.get(wpnrpl));
								heatlist.put(wpnrpl, Integer.toString(rplHeat - acount*(rplHeat/rplCount)));
								weaponlist.put(wpnrpl, Integer.toString(rplCount - acount));
								
								weaponlist.put(aweapon, Integer.toString(NumberUtils.toInt(weaponlist.get(aweapon)) + acount));
								heatlist.put(aweapon,  Integer.toString(NumberUtils.toInt(heatlist.get(aweapon)) + aheat));
							}
							else {
								heatlist.remove(wpnrpl);
								weaponlist.remove(wpnrpl);
								
								weaponlist.put(aweapon, Integer.toString(NumberUtils.toInt(weaponlist.get(aweapon)) + acount));
								heatlist.put(aweapon,  Integer.toString(NumberUtils.toInt(heatlist.get(aweapon)) + aheat));
							}
						}
					}
					else {
						weaponlist.put(aweapon, Integer.toString(NumberUtils.toInt(weaponlist.get(aweapon)) + acount));
						heatlist.put(aweapon,  Integer.toString(NumberUtils.toInt(heatlist.get(aweapon)) + aheat));
	
						if( NumberUtils.toInt(weaponlist.get(aweapon)) <= 0 ) {
							heatlist.remove(aweapon);
							weaponlist.remove(aweapon);
						}
					}
					
					wpnrpl = wpnrpllist.length > index ? wpnrpllist[index++] : null;
				}
			}
			
			// MaxHeat
			Map<String,Integer> modHeats = ShipTypeChangeset.this.getMaxHeat();
			if( modHeats != null ) {
				for( Map.Entry<String, Integer> entry: modHeats.entrySet() ) {
					String weapon = entry.getKey();
					Integer modheat = modHeats.get(weapon);
					if( !heatlist.containsKey(weapon) ) {
						heatlist.put(weapon, Integer.toString(modheat));
					}
					else {
						String heatweapon = heatlist.get(weapon);
						heatlist.put(weapon, Integer.toString(Integer.parseInt(heatweapon)+modheat));
						
					}
				}
			}
			
			this.baseWeapons = baseWeapons;
			this.baseHeat = baseHeat;
			this.weapons = Weapons.packWeaponList(weaponlist);
			this.maxheat = Weapons.packWeaponList(heatlist);
		}
		
		public String getMaxHeat() {
			if( (this.maxheat == null) || !inner.getMaxHeat().equals(baseHeat) ) {
				calcWeaponData();
			}
			return this.maxheat;
		}

		public String getNickname() {
			if( ShipTypeChangeset.this.getNickname() == null ) {
				return inner.getNickname();
			}
			return ShipTypeChangeset.this.getNickname();
		}

		public int getOneWayWerft() {
			if( ShipTypeChangeset.this.getOneWayWerft() == 0 ) {
				return inner.getOneWayWerft();
			}
			return ShipTypeChangeset.this.getOneWayWerft();
		}

		public int getPanzerung() {
			int value = inner.getPanzerung() + ShipTypeChangeset.this.getPanzerung();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		public String getPicture() {
			if( ShipTypeChangeset.this.getPicture() == null ) {
				return inner.getPicture();
			}
			return ShipTypeChangeset.this.getPicture();
		}

		public int getRa() {
			int value = inner.getRa() + ShipTypeChangeset.this.getRa();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		public int getRd() {
			int value = inner.getRd() + ShipTypeChangeset.this.getRd();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		public int getReCost() {
			int value = inner.getReCost() + ShipTypeChangeset.this.getReCost();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		public int getRm() {
			int value = inner.getRm() + ShipTypeChangeset.this.getRm();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		public int getRu() {
			int value = inner.getRu() + ShipTypeChangeset.this.getRu();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		public int getSensorRange() {
			int value = inner.getSensorRange() + ShipTypeChangeset.this.getSensorRange();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		public int getShields() {
			int value = inner.getShields() + ShipTypeChangeset.this.getShields();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		public int getShipClass() {
			return inner.getShipClass();
		}

		public int getShipCount() {
			return inner.getShipCount();
		}

		public int getSize() {
			if( getType().getSize() > ShipType.SMALL_SHIP_MAXSIZE ) {
				int value = inner.getSize() + ShipTypeChangeset.this.getSize();
				if( value <= ShipType.SMALL_SHIP_MAXSIZE ) {
					return ShipType.SMALL_SHIP_MAXSIZE+1;
				}
				return value;
			}

			int value = inner.getSize() + ShipTypeChangeset.this.getSize();
			if( value > ShipType.SMALL_SHIP_MAXSIZE ) {
				return 3;
			}
			if( value < 1 ) {
				return 1;
			}
			return value;
		}

		public int getTorpedoDef() {
			int value = inner.getTorpedoDef() + ShipTypeChangeset.this.getTorpedoDef();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		public int getTypeId() {
			return inner.getTypeId();
		}

		public String getTypeModules() {
			return inner.getTypeModules();
		}

		public String getWeapons() {
			if( (this.weapons == null) || !inner.getWeapons().equals(baseWeapons) ) {
				calcWeaponData();
			}
			
			return this.weapons;
		}

		public int getWerft() {
			int value = inner.getWerft() + ShipTypeChangeset.this.getWerft();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		public boolean hasFlag(String flag) {
			return getFlags().indexOf(flag) > -1;
		}

		public boolean isHide() {
			return inner.isHide();
		}

		public boolean isMilitary() {
			return getWeapons().indexOf('=') > -1;
		}

		public ShipTypeData getType() {
			return inner.getType();
		}

		public int getAblativeArmor() {
			int value = inner.getAblativeArmor() + ShipTypeChangeset.this.getAblativeArmor();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		public boolean hasSrs() {
			if( ShipTypeChangeset.this.hasSrs() == null ) {
				return inner.hasSrs();
			}
			return inner.hasSrs() && ShipTypeChangeset.this.hasSrs();
		}
		
		public int getPickingCost() {
			return ShipTypeChangeset.this.getPickingCost() + inner.getPickingCost();
		}
 

		public int getScanCost() {
			return ShipTypeChangeset.this.getScanCost() + inner.getScanCost();
		}
		
		public int getMinCrew()
		{
			return ShipTypeChangeset.this.getMinCrew() + inner.getMinCrew();
		}
		
		/**
		 * Wahrscheinlichkeit, dass das Schiff sich in einem EMP-Nebel verfliegt.
		 * 
		 * @return Zahl zwischen 0 und 1.
		 */
		public double getLostInEmpChance()
		{
			return ShipTypeChangeset.this.getLostInEmpChance() + inner.getLostInEmpChance();
		}
	}
}

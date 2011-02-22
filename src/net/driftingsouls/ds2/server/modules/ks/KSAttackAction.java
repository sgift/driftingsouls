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
package net.driftingsouls.ds2.server.modules.ks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.config.Weapon;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.config.items.effects.IEAmmo;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import net.driftingsouls.ds2.server.entities.Ammo;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;

import org.apache.commons.lang.math.RandomUtils;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Berechnet das Waffenfeuer im KS.
 * @author Christopher Jung
 *
 */
@Configurable
public class KSAttackAction extends BasicKSAction {
	private Weapon weapon;
	private BattleShip ownShip;
	private BattleShip enemyShip;
	private SQLResultRow localweapon;
	private String attmode;
	private int attcount;

	private Configuration config;



	/**
	 * Konstruktor.
	 *
	 */
	public KSAttackAction() {
		Context context = ContextMap.getContext();
		this.weapon = Weapons.get().weapon(context.getRequest().getParameterString("weapon"));

		this.requireOwnShipReady(true);

		this.ownShip = null;
		this.enemyShip = null;
		this.localweapon = null;

		this.attmode = context.getRequest().getParameterString("attmode");
		if( !this.attmode.equals("single") && !this.attmode.equals("alphastrike") && !this.attmode.equals("strafe") &&
				!this.attmode.equals("alphastrike_max") && !this.attmode.equals("strafe_max") ) {
			this.attmode = "single";	
		}

		this.attcount = context.getRequest().getParameterInt("attcount");
		if( (this.attcount <= 0) || (this.attcount > 3) ) {
			this.attcount = 3;
		}
	}

	/**
	 * Injiziert die DS-Konfiguration.
	 * @param config Die DS-Konfiguration
	 */
	@Autowired
	public void setConfiguration(Configuration config) 
	{
		this.config = config;
	}

	private int destroyShipOnly(int id, Battle battle, BattleShip eShip, boolean generateLoot, boolean generateStats) {
		
		//
		// Schiff als zerstoert makieren
		//
		
		int remove = 1; // Anzahl der zerstoerten Schiffe

		eShip.setAction(eShip.getAction() | Battle.BS_DESTROYED);
		eShip.setDestroyer(id);
		
		//
		// Ueberpruefen, ob weitere (angedockte) Schiffe zerstoert wurden
		//

		List<BattleShip> enemyShips = battle.getEnemyShips();
		for( int i=0; i < enemyShips.size(); i++ ) {
			BattleShip s = enemyShips.get(i);

			if(s.getShip().getBaseShip() != null && s.getShip().getBaseShip().getId() == eShip.getId())
			{
				remove++;
				s.setAction(s.getAction() | Battle.BS_DESTROYED);
				s.setDestroyer(id);
			}
		}

		return remove;
	}

	private void destroyShip(int id, Battle battle, BattleShip eShip) {	
		
		int remove = this.destroyShipOnly(id, battle, eShip, true, true);

		// Wurde mehr als ein Schiff zerstoert?
		if( remove > 1 ) {
			battle.logenemy( (remove-1)+" gedockte/gelandete Schiffe wurden bei der Explosion zerst&ouml;rt\n" );
			battle.logme( (remove-1)+" gedockte/gelandete Schiffe wurden bei der Explosion zerst&ouml;rt\n" );
		}
	}

	private int getTrefferWS( Battle battle, int defTrefferWS, BattleShip eShip, ShipTypeData eShipType, int defensivskill, int navskill ) {
		ShipTypeData ownShipType = this.ownShip.getTypeData();

		if( (eShip.getCrew() == 0) && (eShipType.getMinCrew() > 0) ) {
			return 100;
		}
		if( (defTrefferWS <= 0) && (eShipType.getCost() > 0) && (eShip.getShip().getEngine() > 0) ) {
			return 0;
		}

		// Das Objekt kann sich nicht bewegen - also 100% trefferws
		int trefferWS = 100;

		// Das Objekt hat einen Antrieb - also TrefferWS anpassen
		if( ( eShipType.getCost() > 0 ) && ( eShip.getShip().getEngine() > 0 ) ) {
			trefferWS = calcTWSthroughDifference(defensivskill, navskill, eShip, eShipType, defTrefferWS, ownShipType);
		} 


		if( trefferWS < 0 ) {
			trefferWS = 0;
		}
		if( trefferWS > 100 ) {
			trefferWS = 100;
		}

		// Nun die TrefferWS anteilig senken, wenn Crew/Sensoren nicht auf 100 sind
		trefferWS *= (this.ownShip.getShip().getSensors()/100d);
		if( (ownShipType.getMinCrew() > 0) && (this.ownShip.getCrew() < ownShipType.getMinCrew()) ) {
			trefferWS *= this.ownShip.getCrew()/(double)ownShipType.getMinCrew();
		}

		// Und nun die TrefferWS anteilig steigern, wenn die Gegnerische Crew/Antrie nicht auf 100 sind
		int restws = 100-trefferWS;
		trefferWS += restws*((100-eShip.getShip().getEngine())/100d);
		if( eShip.getCrew() < eShipType.getMinCrew() ) {
			trefferWS += restws*((eShipType.getMinCrew()-eShip.getCrew())/(double)eShipType.getMinCrew());
		}

		if( trefferWS < 0 ) {
			trefferWS = 0;
		}
		if( trefferWS > 100 ) {
			trefferWS = 100;
		}

		return trefferWS;
	}

	private int getSmallTrefferWS( Battle battle, int defTrefferWS, BattleShip eShip, ShipTypeData eShipType, int defensivskill, int navskill ) {
		ShipTypeData ownShipType = this.ownShip.getTypeData();

		if( (eShip.getCrew() == 0) && (eShipType.getMinCrew() > 0) ) {
			return 100;
		}
		if( (defTrefferWS <= 0) && (eShipType.getCost() > 0) && (eShip.getShip().getEngine() > 0) ) {
			return 0;
		}

		// Das Objekt kann sich nicht bewegen - also 100% trefferws
		int trefferWS = 100;

		// Das Objekt hat einen Antrieb - also TrefferWS anpassen
		if( ( eShipType.getCost() > 0 ) && ( eShip.getShip().getEngine() > 0 ) ) {
			trefferWS = calcTWSthroughDifference(defensivskill, navskill, eShip, eShipType, defTrefferWS, ownShipType);
		} 

		if( trefferWS < 0 ) {
			trefferWS = 0;
		}
		if( trefferWS > 100 ) {
			trefferWS = 100;
		}

		// Nun die TrefferWS anteilig senken, wenn Crew/Sensoren nicht auf 100 sind
		trefferWS *= (this.ownShip.getShip().getSensors()/100d);
		if( (ownShipType.getMinCrew() > 0) && (this.ownShip.getCrew() < ownShipType.getMinCrew()) ) {
			trefferWS *= this.ownShip.getCrew()/(double)ownShipType.getMinCrew();
		}

		// Und nun die TrefferWS anteilig steigern, wenn die Gegnerische Crew/Antrie nicht auf 100 sind
		int restws = 100-trefferWS;
		trefferWS += restws*((100-eShip.getShip().getEngine())/100d);
		if( eShip.getCrew() < eShipType.getMinCrew() ) {
			trefferWS += restws*((eShipType.getMinCrew()-eShip.getCrew())/(double)eShipType.getMinCrew());
		}

		if( trefferWS < 0 ) {
			trefferWS = 0;
		}
		if( trefferWS > 100 ) {
			trefferWS = 100;
		}

		return trefferWS;
	}

	private int calcTWSthroughDifference(double defensivskill, double navskill, BattleShip eShip, ShipTypeData eShipType, int defTrefferWS, ShipTypeData ownShipType) {
		double differenceSkillz = 0.0;
		double differenceSize = 0.0;
		double eSize = eShipType.getSize();
		double ownSize = ownShipType.getSize();
		if ( defensivskill > navskill ){
			differenceSkillz = ((defensivskill/navskill) - 1) * - 1;
			if ( differenceSkillz < -0.4) {
				differenceSkillz = -0.4;
			}
		}
		else if ( defensivskill < navskill ){
			differenceSkillz = (( navskill/defensivskill) - 1);
			if ( differenceSkillz > 0.4){
				differenceSkillz = 0.4;
			}
		} 
		else {
			differenceSkillz=0.0;
		}

		// Berechne Aenderung der TWS durch unterschiedliche Schiffsgroesse
		// Original  + round(($eShipType['size'] - $ownShipType['size'])*2)
		if ( eSize < ownSize ){
			differenceSize = ( (eSize/ownSize) - 1);
			if ( differenceSize < -0.4){
				differenceSize = -0.4;
			}
		} 
		else if ( ownSize < eShipType.getSize() ){
			differenceSize = ( (ownSize/eSize) -1) * -1;
			if ( differenceSize > 0.4 ){
				differenceSize = 0.4;
			}
		} 
		else {
			differenceSize=0.0;
		}

		return (int) Math.round(defTrefferWS + (defTrefferWS * differenceSize) + (defTrefferWS * differenceSkillz));
	}

	private boolean calcDamage( Battle battle, BattleShip eShip, ShipTypeData eShipType, int hit, int absSchaden, int schaden, int[] subdmgs, String prefix ) {
		boolean ship_intact = true;

		if( (prefix != null) && prefix.length() > 0 ) {
			battle.logme("\n"+prefix+":\n");	
			battle.logenemy("\n"+prefix+":\n");	
		}

		if(this.weapon.getAmmoType().length != 0){
			if ( this.localweapon.getBoolean("armor_redux")){
				int tmppanzerung = eShip.getArmor();
				if (tmppanzerung <= 0){
					tmppanzerung = 1;
				}
				schaden = Math.round(schaden/tmppanzerung);

			}
		}


		if( (hit != 0) && (eShip.getShields() > 0) ) {
			if( eShip.getShields() >= absSchaden*hit ) {
				eShip.setShields(eShip.getShields() - absSchaden*hit);
				if( eShip.getShields() == 0 ) {
					battle.logme( "+ Schilde ausgefallen\n" );
					battle.logenemy( "+ Schilde ausgefallen\n" );
				}
				else {
					battle.logme( "+ Schaden (Schilde): "+Common.ln(hit*absSchaden)+"\n" );
					battle.logenemy( "+ Schilde: "+Common.ln(hit*absSchaden)+" Schaden\n" );
				}
				hit = 0;
			}
			else {
				hit -= Math.ceil(eShip.getShields()/absSchaden);
				eShip.setShields(0);
				battle.logme( "+ Schilde ausgefallen\n" );
				battle.logenemy( "+ Schilde ausgefallen\n" );
			}
		}
		if( hit != 0 ) {
			int hulldamage = hit*schaden;

			if( eShipType.hasFlag(ShipTypes.SF_ZERSTOERERPANZERUNG) ) {
				int dmgThisTurn = eShip.getShip().getHull()-eShip.getHull()+hulldamage;
				if( dmgThisTurn / (double)eShipType.getHull() > 0.33 ) {
					int newhulldamage = (int)(eShipType.getHull()*0.33 - (eShip.getShip().getHull()-eShip.getHull()));
					battle.logme("+ Zerst&ouml;rerpanzerung absorbiert Schaden ("+Common.ln(hulldamage-newhulldamage)+" dmg)\n");
					battle.logenemy("+ Zerst&ouml;rerpanzerung absorbiert Schaden  ("+Common.ln(hulldamage-newhulldamage)+" dmg)\n");

					hulldamage = newhulldamage;
				}
			}

			//Ablative Panzerung pruefen
			int ablativeArmor = eShip.getAblativeArmor();
			if(ablativeArmor > 0)
			{
				ablativeArmor -= hulldamage;

				//Angerichteter Schaden
				int damage = eShip.getAblativeArmor() - ablativeArmor;			
				if(damage > eShip.getAblativeArmor())
				{
					damage = eShip.getAblativeArmor();
				}

				battle.logme( "+ Schaden (Ablative Panzerung): "+Common.ln(damage)+"\n" );
				battle.logenemy( "+ H&uuml;lle: "+Common.ln(damage)+" Schaden\n" );

				//Ablative Panzerung von VOR dem Treffer abziehen
				hulldamage -= eShip.getAblativeArmor();
				if(hulldamage < 0)
				{
					hulldamage = 0;
				}
				if(ablativeArmor < 0)
				{
					ablativeArmor = 0;
				}
			}
			eShip.setAblativeArmor(ablativeArmor);

			if( eShipType.hasFlag(ShipTypes.SF_GOD_MODE ) ) {
				if( eShip.getHull() - hulldamage < 1 ) {
					hulldamage = eShip.getHull() - 1;
					battle.logme("+ Schiff nicht zerst&ouml;rbar\n");
					battle.logenemy("+ Schiff nicht zerst&ouml;rbar\n");	
				}
			}

			if( eShip.getHull() - hulldamage > 0 ) {
				ship_intact = true;
			}
			else {
				ship_intact = false;	
			}

			eShip.setHull(eShip.getHull() - hulldamage);
			if( eShip.getHull() < 0 ) {
				eShip.setHull(0);
			}

			if( eShip.getHull() > 0 ) {
				//Hat die ablative Panzerung alles abgefangen?
				if(hulldamage > 0)
				{
					battle.logme( "+ Schaden (H&uuml;lle): "+Common.ln(hulldamage)+"\n" );
					battle.logenemy( "+ H&uuml;lle: "+Common.ln(hulldamage)+" Schaden\n" );
				}

				//Subsysteme - nur treffbar, wenn die ablative Panzerung auf 0 ist
				if( subdmgs != null && (subdmgs.length > 0) && ablativeArmor == 0) {
					final int ENGINE = 0;
					final int WEAPONS = 1;
					final int COMM = 2;
					final int SENSORS = 3;

					List<Integer> subsysteme = new ArrayList<Integer>();
					subsysteme.add(SENSORS);
					subsysteme.add(COMM);

					List<String> subsysteme_name = new ArrayList<String>();
					subsysteme_name.add("Sensoren");
					subsysteme_name.add("Kommunikation");

					if( eShipType.getCost() > 0 ) {
						subsysteme.add(ENGINE);
						subsysteme_name.add("Antrieb");
					}

					if( eShipType.isMilitary() ) {
						subsysteme.add(WEAPONS);
						subsysteme_name.add("Waffen");
					}

					for( int i=0; i < subdmgs.length; i++ ) {
						int subdmg = subdmgs[i];

						if( subdmg < 1 ) {
							continue;
						}

						int rnd = RandomUtils.nextInt(subsysteme.size());
						int subsys = subsysteme.get(rnd);

						int value = 0;
						switch(subsys) {
						case ENGINE:
							eShip.setEngine(Math.max(eShip.getEngine() - subdmg, 0));
							value = eShip.getEngine();
							break;
						case WEAPONS:
							eShip.setWeapons(Math.max(eShip.getWeapons() - subdmg, 0));
							value = eShip.getWeapons();
							break;
						case COMM:
							eShip.setComm(Math.max(eShip.getComm() - subdmg, 0));
							value = eShip.getComm();
							break;
						case SENSORS:
							eShip.setSensors(Math.max(eShip.getSensors() - subdmg, 0));
							value = eShip.getSensors();
							break;
						}

						if( value > 0 ) {
							battle.logme("+ "+subsysteme_name.get(rnd)+": "+Common.ln(subdmg)+" Schaden\n");
							battle.logenemy("+ "+subsysteme_name.get(rnd)+": "+Common.ln(subdmg)+" Schaden\n");
						} 
						else {
							battle.logme("+ "+subsysteme_name.get(rnd)+": ausgefallen\n");
							battle.logenemy("+ "+subsysteme_name.get(rnd)+": ausgefallen\n");
						}
					}
				}
			}
			else {
				battle.logme( "[color=red]+ Schiff zerst&ouml;rt[/color]\n" );
				battle.logenemy( "[color=red]+ Schiff zerst&ouml;rt[/color]\n" );
				if(config.getInt("DESTROYABLE_SHIPS") == 0 ) {
					if( eShip.getHull() < 1 ) {
						eShip.setHull(1);
					}
					ship_intact = true;
				}
			}
		}

		if( !ship_intact ) {
			if( (eShip.getAction() & Battle.BS_HIT) != 0 ) {
				eShip.setAction(eShip.getAction() ^ Battle.BS_HIT);
			}
			eShip.setAction(eShip.getAction() | Battle.BS_DESTROYED);
		}
		else {
			eShip.setAction(eShip.getAction() | Battle.BS_HIT);
			if( (eShip.getAction() & Battle.BS_FLUCHTNEXT) != 0 && (eShip.getEngine() == 0) && (eShipType.getCost() > 0) ) {
				eShip.setAction(eShip.getAction() ^ Battle.BS_FLUCHTNEXT);
			}
			if( (eShip.getAction() & Battle.BS_FLUCHT) != 0 && (eShip.getEngine() == 0) && (eShipType.getCost() > 0) ) {
				eShip.setAction(eShip.getAction() ^ Battle.BS_FLUCHT);
				battle.logme( "+ Flucht gestoppt\n" );
				battle.logenemy( "[color=red]+ Flucht gestoppt[/color]\n" );
			}
		}

		return ship_intact;
	}

	private SQLResultRow getWeaponData( Battle battle ) {	
		ShipTypeData ownShipType = this.ownShip.getTypeData();
		ShipTypeData enemyShipType = this.enemyShip.getTypeData();

		SQLResultRow localweapon = new SQLResultRow();

		if( enemyShipType.getSize() <= ShipType.SMALL_SHIP_MAXSIZE ) {
			localweapon.put("deftrefferws", this.weapon.getDefSmallTrefferWS());
		} 
		else {
			localweapon.put("deftrefferws", this.weapon.getDefTrefferWS());
		}
		localweapon.put("basedamage", this.weapon.getBaseDamage(ownShipType));
		localweapon.put("shielddamage", this.weapon.getShieldDamage(ownShipType));
		localweapon.put("name", this.weapon.getName());
		localweapon.put("shotsPerShot", this.weapon.getSingleShots());
		localweapon.put("subws", this.weapon.getDefSubWS());
		localweapon.put("subdamage", this.weapon.getSubDamage(ownShipType));
		localweapon.put("destroyAfter", this.weapon.hasFlag(Weapon.Flags.DESTROY_AFTER));
		localweapon.put("areadamage", this.weapon.getAreaDamage());
		localweapon.put("destroyable", this.weapon.getDestroyable() ? 1.0 : 0.0);
		localweapon.put("ad_full", this.weapon.hasFlag(Weapon.Flags.AD_FULL));
		localweapon.put("long_range", this.weapon.hasFlag(Weapon.Flags.LONG_RANGE));
		localweapon.put("very_long_range", this.weapon.hasFlag(Weapon.Flags.VERY_LONG_RANGE));

		return localweapon;
	}

	private SQLResultRow getAmmoBasedWeaponData( Battle battle ) {
		Context context = ContextMap.getContext();
		BattleShip ownShip = this.ownShip;

		ShipTypeData enemyShipType = this.enemyShip.getTypeData();
		ShipTypeData ownShipType = ownShip.getTypeData();

		final String weaponName = context.getRequest().getParameterString("weapon");

		Map<String,String> weapons = Weapons.parseWeaponList(ownShipType.getWeapons());
		int weaponCount = Integer.parseInt(weapons.get(weaponName));

		Ammo ammo = null;
		ItemCargoEntry ammoitem = null;

		// Munition
		Cargo mycargo = ownShip.getCargo();
		List<ItemCargoEntry> itemlist = null;

		if( this.weapon.hasFlag(Weapon.Flags.AMMO_SELECT) ) {
			int ammoid = context.getRequest().getParameterInt("ammoid");

			ItemCargoEntry item = null;
			itemlist = mycargo.getItemsWithEffect( ItemEffect.Type.AMMO );
			for( int i=0; i < itemlist.size(); i++ ) {
				if( itemlist.get(i).getItemID() == ammoid ) {
					item = itemlist.get(i);
					break;
				}
			}

			if( item == null ) {
				battle.logme( "Sie verf&uuml;gen nicht &uuml;ber den angegebenen Munitionstyp\n" );
				return null;
			}

			ammo = (Ammo)context.getDB().createQuery("from Ammo " +
					"where itemid=? and type in ('"+Common.implode("','",this.weapon.getAmmoType())+"')")
					.setInteger(0, ammoid)
					.iterate().next();
		} 
		else {
			itemlist = mycargo.getItemsWithEffect( ItemEffect.Type.AMMO );
			for( int i=0; i < itemlist.size(); i++ ) {
				IEAmmo effect = (IEAmmo)itemlist.get(i).getItemEffect();

				if( Common.inArray(effect.getAmmo().getType(), this.weapon.getAmmoType()) ) {
					ammo = effect.getAmmo();
					break;
				}
			}

			if( ammo == null ) {
				battle.logme( "Sie verf&uuml;gen &uuml;ber keine Munition\n" );
				return null;
			}
		}

		if( ammo == null ) {
			battle.logme("Der angegebene Munitionstyp existiert nicht\n" );
			return null;
		}

		ammoitem = null;
		for( int i=0; i < itemlist.size(); i++ ) {
			IEAmmo effect = (IEAmmo)itemlist.get(i).getItemEffect();
			if( effect.getAmmo() == ammo ) {
				ammoitem = itemlist.get(i);
			}
		}

		weaponCount = (int)(weaponCount);

		if( ammoitem.getCount() <  weaponCount*this.weapon.getSingleShots() ) {
			battle.logme( this.weapon.getName()+" k&ouml;nnen nicht abgefeuert werden, da nicht genug Munition f&uuml;r alle Gesch&uuml;tze vorhanden ist.\n" );
			return null;
		}

		battle.logme( "Feuere "+ammo.getName()+" ab...\n" );

		SQLResultRow localweapon = new SQLResultRow();
		if( enemyShipType.getSize() <= ShipType.SMALL_SHIP_MAXSIZE ) {
			localweapon.put("deftrefferws", ammo.getSmallTrefferWS());
		} 
		else {
			localweapon.put("deftrefferws", ammo.getTrefferWS());
		}
		localweapon.put("basedamage", ammo.getDamage());
		localweapon.put("shielddamage", ammo.getShieldDamage());
		localweapon.put("shotsPerShot", ammo.getShotsPerShot()*this.weapon.getSingleShots());
		localweapon.put("name", ammo.getName());
		localweapon.put("subws", ammo.getSubWS());
		localweapon.put("subdamage", ammo.getSubDamage());
		localweapon.put("destroyAfter", false);
		localweapon.put("areadamage", ammo.getAreaDamage());
		localweapon.put("destroyable", ammo.getDestroyable());
		localweapon.put("ammoitem", ammoitem);
		localweapon.put("ad_full", ammo.hasFlag(Ammo.Flags.AD_FULL));
		localweapon.put("armor_redux", ammo.hasFlag(Ammo.Flags.ARMOR_REDUX));
		localweapon.put("long_range", this.weapon.hasFlag(Weapon.Flags.LONG_RANGE));
		localweapon.put("very_long_range", this.weapon.hasFlag(Weapon.Flags.VERY_LONG_RANGE));

		return localweapon;
	}

	private int getAntiTorpTrefferWS(ShipTypeData enemyShipType, BattleShip enemyShip) {
		Cargo enemyCargo = enemyShip.getCargo();
		Context context = ContextMap.getContext();
		Map<String,String> eweapons = Weapons.parseWeaponList(enemyShipType.getWeapons());
		double antitorptrefferws = 0;
		
		for( Map.Entry<String,String> wpn : eweapons.entrySet() ) {
			final int count = Integer.parseInt(wpn.getValue());
			final Weapon weapon = Weapons.get().weapon(wpn.getKey());

			if( weapon.getTorpTrefferWS() != 0 ) {
				antitorptrefferws += weapon.getTorpTrefferWS()*count;
			}
			else if( (weapon.getAmmoType().length > 0) )
			{
				// Load possible ammo from database
				List<Ammo> ammo = Common.cast(context.getDB().createQuery("from Ammo where type in ('"+Common.implode("','", weapon.getAmmoType())+"')").list());
				// iterate through whole ammo
				for(Ammo munition: ammo)
				{
					ItemID ammoId = new ItemID(munition.getItemId());
					int ammocount = (int) enemyCargo.getResourceCount(ammoId);
					int shots = weapon.getSingleShots()*count;
					// check if there's enough ammo to fire
					if(	ammocount >= shots && munition.getTorpTrefferWS() != 0)
					{
						// increase antitorptws
						antitorptrefferws += munition.getTorpTrefferWS()*count;
						// reduce amount of ammo in cargo
						enemyCargo.setResource(ammoId, ammocount - shots);
						enemyShip.getShip().setCargo(enemyCargo);
						// stop iteration of ammo here
						// TODO maybe we should check if there's a better ammo in cargo
						break;
					}					
				}
			}

		}	
		antitorptrefferws *= (this.enemyShip.getShip().getWeapons()/100);
		
		return (int)antitorptrefferws;
	}

	private int getFighterDefense( Battle battle )
	{
		int defcount = 0;		// Anzahl zu verteidigender Schiffe
		int fighterdefcount = 0;// Gesamtpunktzahl an Bombenabwehr durch Jaeger
		int gksdefcount = 0;	// Gesamtpunktzahl an Bombenabwehr durch GKS
		int fighter = 0;		// Gesamtanzahl Jaeger
		int docks = 0;			// Gesamtanzahl Docks
		int docksuse = 0;		// Gesamtanzahl an Schiffen, welche Docks brauchen

		List<BattleShip> enemyShips = battle.getEnemyShips();
		for( int i=0; i < enemyShips.size(); i++ )
		{
			BattleShip selectedShip = enemyShips.get(i);
			ShipTypeData type = selectedShip.getTypeData();

			int typeCrew = type.getMinCrew();
			if(typeCrew <= 0)
			{
				typeCrew = 1;
			}
			double crewfactor = ((double)selectedShip.getCrew()) / ((double)typeCrew);

			//No bonus for more crew than needed
			if(crewfactor > 1.0)
			{
				crewfactor = 1.0;
			}

			// check if ship has to be defended
			if(shipHasToBeDefended(selectedShip))
			{
				defcount = defcount + 1;
			}
			
			//check if ship has torpdef
			if(shipHasTorpDef(type))
			{
				// check if ship is a GKS
				if(shipIsGKS(type))
				{
					// increase the gks-torpedo-defense
					gksdefcount = gksdefcount + (int)Math.floor(type.getTorpedoDef() * crewfactor);
				}
				else
				{
					// check if ship is landed
					if(shipIsNotLanded(selectedShip))
					{
						// increase the fighter-torpedo-defense
						fighterdefcount += (int)Math.floor(type.getTorpedoDef() * crewfactor);
						// increase number of fighters
						fighter = fighter + 1;	
					}
				}
			}
			
			// check if ship needs dock
			if(shipNeedsDock(type))
			{
				// increase number of docks needed
				docksuse = docksuse + 1;
			}

			// check if ship has docks
			if(shipHasDocks(type))
			{
				// add docks
				docks = docks + (int)Math.floor(type.getJDocks() * crewfactor);
			}
		}
		
		if( defcount == 0 )
		{
			defcount = 1;	
		}
		
		// Rechnen wir mal die endgueltige Verteidigung aus
		if (docksuse > docks)
		{
			if ( docks != 0)
			{
				docks = (int)Math.floor(docks * (fighter / docksuse));
			}
			fighterdefcount = (int)Math.floor( ( (double)fighterdefcount / (double)fighter ) * (double)docks );
		}
		int fighterdef = (int)Math.round( (double)(fighterdefcount + gksdefcount ) / (double)defcount );
		if( fighterdef > 100 )
		{
			fighterdef = 100;	
		}

		return fighterdef;
	}

	private boolean shipIsNotLanded(BattleShip selectedShip) {
		if(selectedShip.getShip().isLanded() || selectedShip.getShip().isDocked())
		{
			return false;
		}
		return true;
	}

	private boolean shipHasDocks(ShipTypeData type) {
		if(type.getJDocks() <= 0)
		{
			return false;
		}
		return true;
	}

	private boolean shipHasToBeDefended(BattleShip selectedShip) {
		if((selectedShip.getAction() & Battle.BS_JOIN) != 0 )
		{
			return false;
		}
		else if((selectedShip.getAction() & Battle.BS_SECONDROW) != 0)
		{
			return false;
		}
		else if(selectedShip.getTypeData().hasFlag( ShipTypes.SF_JAEGER ))
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	private boolean shipHasTorpDef(ShipTypeData type) {
		if(type.getTorpedoDef() <= 0)
		{
			return false;
		}
		return true;
	}

	private boolean shipNeedsDock(ShipTypeData type) {
		if(type.getShipClass() != ShipClasses.JAEGER.ordinal() && type.getShipClass() != ShipClasses.BOMBER.ordinal())
		{
			return false;
		}
		return true;
	}

	private boolean shipIsGKS(ShipTypeData type) {
		if(type.getSize() <= ShipType.SMALL_SHIP_MAXSIZE)
		{
			return false;
		}
		return true;
	}

	private List<BattleShip> getADShipList( Battle battle ) {
		int type = this.enemyShip.getShip().getType();

		// schiffe zusammensuchen
		List<BattleShip> shiplist = new ArrayList<BattleShip>();
		List<BattleShip> backup = new ArrayList<BattleShip>();
		boolean gottarget = false;

		List<BattleShip> enemyShips = battle.getEnemyShips();
		for( int i=0; i < enemyShips.size(); i++ ) {
			BattleShip eship = enemyShips.get(i);

			if( eship.getShip().getType() == type ) {
				if( eship == this.enemyShip ) {
					gottarget = true;
					continue;
				}
				else if( (eship.getAction() & Battle.BS_DESTROYED) != 0 ) {
					continue;	
				}
				else if( (eship.getAction() & Battle.BS_FLUCHT) != 0 && (this.enemyShip.getAction() & Battle.BS_FLUCHT) == 0 ) {
					continue;
				}
				else if( (eship.getAction() & Battle.BS_FLUCHT) == 0 && (this.enemyShip.getAction() & Battle.BS_FLUCHT) != 0 ) {
					continue;
				}
				else if( (eship.getAction() & Battle.BS_JOIN) != 0 ) {
					continue;	
				}
				else if(eship.getShip().isLanded()) {
					continue;	
				}
				else if(eship.isSecondRow())
				{
					continue;
				}

				shiplist.add(eship);
				if( !gottarget && (shiplist.size() > this.localweapon.getInt("areadamage")) ) {
					backup.add(shiplist.remove(0));
				}
				if( gottarget && (shiplist.size() >= this.localweapon.getInt("areadamage")*2) ) {
					break;
				}
			}	
		}

		if( shiplist.size() < this.localweapon.getInt("areadamage")*2 ) {
			for( int j=shiplist.size(); (j < this.localweapon.getInt("areadamage")*2) && !backup.isEmpty(); j++ )	{
				shiplist.add(backup.remove(backup.size()-1));
			}
		}

		final BattleShip emptyRow = new BattleShip();

		// Ein leeres Element hinzufuegen, falls wir nicht genug Elemente haben
		if( (shiplist.size() < this.localweapon.getInt("areadamage")*2) && (shiplist.size() % 2 != 0) ) {
			shiplist.add(emptyRow);
		}

		int listmiddle = shiplist.size()/2;

		List<BattleShip> areashiplist = new ArrayList<BattleShip>(shiplist.size());
		for( int i=0; i < shiplist.size()+1; i++ ) {
			areashiplist.add(emptyRow);
		}

		areashiplist.set(listmiddle, this.enemyShip);
		for( int i=1; i <= shiplist.size(); i++ ) {
			if( i % 2 == 0 ) {
				areashiplist.set(listmiddle-(i/2), shiplist.get(i-1));
			}
			else {
				areashiplist.set(listmiddle+(int)Math.ceil(i/2d), shiplist.get(i-1));
			}	
		}

		return areashiplist;
	}

	private int[] getSubDamages( int subPanzerung, int trefferWS, int subWS, double damageMod ) {
		int subDamage = (int)Math.round(this.localweapon.getInt("subdamage")*((10-subPanzerung)/10d)*damageMod);

		int hit=0;
		int[] tmpSubDmgs = new int[this.localweapon.getInt("count")*this.localweapon.getInt("shotsPerShot")];
		int totalSize = 0;

		for( int i=1; i <= this.localweapon.getInt("count")*this.localweapon.getInt("shotsPerShot"); i++) {
			int rnd = RandomUtils.nextInt(101);
			if( rnd <= trefferWS ) {
				hit++;
			}

			if( rnd <= subWS ) {
				tmpSubDmgs[totalSize++] = (int)Math.round((rnd/(double)subWS)*subDamage);
			}
		}

		// Falls nicht alle Felder benoetigt wurden, dann das Array entsprechend gekuerzt zurueckgeben
		if( totalSize < tmpSubDmgs.length ) {
			int[] subDmgs = new int[totalSize];
			System.arraycopy(tmpSubDmgs, 0, subDmgs, 0, totalSize);

			return subDmgs;
		}

		return tmpSubDmgs;
	}

	private void calcADStep( Battle battle, int trefferWS, int navskill, BattleShip aeShip, int hit, int schaden, int shieldSchaden, double damagemod ) {
		battle.logme("\n"+aeShip.getName()+" ("+aeShip.getId()+"):\n");
		battle.logenemy("\n"+aeShip.getName()+" ("+aeShip.getId()+"):\n");

		ShipTypeData aeShipType = aeShip.getTypeData();

		int[] tmpsubdmgs = null;

		if( this.localweapon.getInt("subdamage") > 0 ) {
			int tmppanzerung = aeShip.getArmor();

			int defensivskill = aeShip.getDefensiveValue();

			int subWS = this.getTrefferWS( battle, this.localweapon.getInt("subws"), aeShip, aeShipType, defensivskill, navskill );
			battle.logme( "SubsystemTWS: "+subWS+"%\n" );

			int subPanzerung = tmppanzerung;
			if( subPanzerung > 10 ) {
				subPanzerung = 10;
				battle.logme("Panzerung absorbiert Subsystemschaden\n");
			} 
			else if( subPanzerung > 0 ) {
				battle.logme("Panzerung reduziert Subsystemschaden ("+(subPanzerung*10)+"%)\n");
			}

			tmpsubdmgs = getSubDamages(subPanzerung, trefferWS, subWS, damagemod);
		}

		boolean mydamage = this.calcDamage( battle, aeShip, aeShipType, hit, (int)(shieldSchaden*damagemod), (int)(schaden*damagemod), tmpsubdmgs, "" );
		if( !mydamage && (config.getInt("DESTROYABLE_SHIPS") != 0) ) {
			this.destroyShip(this.ownShip.getOwner().getId(), battle, aeShip);
		}
	}

	private int getDamage(int damage, int offensivskill, ShipTypeData enemyShipType) {
		int schaden = (int)Math.round( (damage + damage*offensivskill/1500d) *
				(this.ownShip.getShip().getWeapons()/100d) *
				this.weapon.getBaseDamageModifier(enemyShipType));

		if( schaden < 0 ) {
			schaden = 0;
		}

		return schaden;
	}

	@Override
	public Result execute(Battle battle) throws IOException
	{
		Context context = ContextMap.getContext();
		Session db = context.getDB();

		Result result = super.execute(battle);
		if( result != Result.OK )
		{
			return result;
		}

		if( this.weapon == null )
		{
			return Result.ERROR;
		}

		// Schiff laden
		this.ownShip = battle.getOwnShip();

		ShipTypeData ownShipType = this.ownShip.getTypeData();

		Map<String,String> weaponList = Weapons.parseWeaponList(ownShipType.getWeapons());
		Map<String,String> maxheatList = Weapons.parseWeaponList(ownShipType.getMaxHeat());
		Map<String,String> heatList = Weapons.parseWeaponList(this.ownShip.getWeaponHeat());

		final String weaponName = context.getRequest().getParameterString("weapon");
		if( !weaponList.containsKey(weaponName) )
		{
			battle.logme("Ihr Schiff besitzt keine Waffen des Typs "+this.weapon.getName());
			return Result.ERROR;
		}

		int weapons = Integer.parseInt(weaponList.get(weaponName));
		int maxheat = Integer.parseInt(maxheatList.get(weaponName));
		int heat = 0;
		if( heatList.containsKey(weaponName) )
		{
			heat = Integer.parseInt(heatList.get(weaponName));
		}

		// Feststellen wie oft wird welchen Feuerloop durchlaufen sollen

		boolean firstentry = true; // Battlehistory-Log

		int sameShipLoop = 1; // Alphastrike (same ship)
		int nextShipLoop = 1; // Breitseite (cycle through ships)

		if( this.attmode.equals("alphastrike") )
		{
			sameShipLoop = 5;	
		}
		else if( this.attmode.equals("strafe") )
		{
			nextShipLoop = 5;	
		}
		else if( this.attmode.equals("alphastrike_max") )
		{
			if( weapons > 0 )
			{
				sameShipLoop = (int)((maxheat-heat)/(double)weapons);
			}
			else
			{
				sameShipLoop = 1;
			}
			if( sameShipLoop < 1 )
			{
				sameShipLoop = 1;
			}
		}
		else if( this.attmode.equals("strafe_max") )
		{
			if( weapons > 0 )
			{
				nextShipLoop = (int)((maxheat-heat)/(double)weapons);
			}
			else
			{
				nextShipLoop = 1;
			}
			if( nextShipLoop < 1 )
			{
				nextShipLoop = 1;	
			}
		}

		// Und nun checken wir mal ein wenig....

		if( (ownShipType.getMinCrew() > 0) && (this.ownShip.getCrew() < ownShipType.getMinCrew()/2d) )
		{
			battle.logme( "Nicht genug Crew um mit der Waffe "+this.weapon.getName()+" zu feuern\n" );
			return Result.ERROR;
		}

		if( (this.ownShip.getAction() & Battle.BS_DISABLE_WEAPONS) != 0 )
		{
			battle.logme( "Das Schiff kann seine Waffen in diesem Kampf nicht mehr abfeuern\n" );
			return Result.ERROR;
		}

		if( (this.ownShip.getAction() & Battle.BS_BLOCK_WEAPONS) != 0 )
		{
			battle.logme( "Sie k&ouml;nnen in dieser Runde keine Waffen mehr abfeuern\n" );
			return Result.ERROR;
		}

		boolean gotone = false;
		if( ownShipType.hasFlag(ShipTypes.SF_DROHNE) )
		{
			List<BattleShip> ownShips = battle.getOwnShips();
			for( int i=0; i < ownShips.size(); i++ )
			{
				BattleShip aship = ownShips.get(i);
				ShipTypeData ashiptype = aship.getTypeData();
				if( ashiptype.hasFlag(ShipTypes.SF_DROHNEN_CONTROLLER) )
				{
					gotone = true;
					break;	
				}
			}
		}
		else
		{
			gotone = true;	
		}

		if( !gotone )
		{
			battle.logme( "Sie ben&ouml;tigen ein Drohnen-Kontrollschiff um feuern zu k&ouml;nnen\n" );
			return Result.ERROR;
		}

		if( weapons <= 0 )
		{
			battle.logme( "Das Schiff verf&uuml;gt nicht &uuml;ber die von ihnen gew&auml;hlte Waffe ("+weaponName+")\n" );
			return Result.ERROR;
		}

		if( this.ownShip.getShip().getEnergy() < this.weapon.getECost()*weapons )
		{
			battle.logme( "Nicht genug Energie um mit der Waffe "+this.weapon.getName()+" zu feuern\n" );
			return Result.ERROR;
		}

		if(this.ownShip.getShip().isLanded())
		{
			battle.logme( "Sie k&ouml;nnen nicht mit gelandeten Schiffen feuern\n" );
			return Result.ERROR;
		}

		int oldenemyship = battle.getEnemyShipIndex();

		boolean breakFlag = false;

		// Die auessere Schleife laeuft ueber die generischen Schiffe
		// Die innere Scheife feuernt n Mal auf das gerade ausgewaehlte gegnerische Schiff

		for( int outerloop=0; outerloop < nextShipLoop; outerloop++ )
		{
			// Nun das gegnerische Schiff laden und checken
			this.enemyShip = battle.getEnemyShip();
			db.refresh(this.enemyShip, LockMode.READ);

			for( int innerloop=0; innerloop < sameShipLoop; innerloop++ )
			{
				if( (outerloop > 0) || (innerloop > 0) ) {
					battle.logme("\n[HR]");
					battle.logenemy("\n");
				}

				ShipTypeData enemyShipType = this.enemyShip.getTypeData();

				/*
				 * 	Die konkreten Waffendaten ermitteln
				 */
				SQLResultRow localweapon = null;

				if( this.weapon.getAmmoType().length > 0 )
				{
					localweapon = this.getAmmoBasedWeaponData( battle );
					if( (localweapon == null) || localweapon.isEmpty() )
					{
						breakFlag = true;
						break;
					}
				} 
				else
				{
					localweapon = this.getWeaponData( battle );
					if( (localweapon == null) || localweapon.isEmpty() )
					{
						breakFlag = true;
						break;
					}
				}

				localweapon.put("count", weapons);

				this.localweapon = localweapon;

				if( (this.ownShip.getAction() & Battle.BS_SECONDROW) != 0 && 
						!this.localweapon.getBoolean("long_range") &&
						!this.localweapon.getBoolean("very_long_range") )
				{
					battle.logme( this.weapon.getName()+" haben nicht die notwendige Reichweite, um aus der zweiten Reihe heraus abgefeuert zu werden\n" );
					breakFlag = true;
					break;
				}

				battle.logme( "Ziel: "+Battle.log_shiplink(this.enemyShip.getShip())+"\n" );

				if( heat + weapons > maxheat )
				{
					battle.logme( this.weapon.getName()+" k&ouml;nnen nicht abgefeuert werden, da diese sonst &uuml;berhitzen w&uuml;rden\n" );
					breakFlag = true;
					break;
				}

				if( this.ownShip.getShip().getEnergy() < this.weapon.getECost()*weapons )
				{
					battle.logme( "Nicht genug Energie um mit der Waffe "+this.weapon.getName()+" zu feuern\n" );
					breakFlag = true;
					break;
				}

				if( (this.enemyShip.getAction() & Battle.BS_DESTROYED) != 0 )
				{
					battle.logme( "Das angegebene Ziel ist bereits zerst&ouml;rt\n" );
					breakFlag = true;
					break;
				}

				if( (this.enemyShip.getAction() & Battle.BS_FLUCHT) != 0 && !ownShipType.hasFlag(ShipTypes.SF_ABFANGEN) )
				{
					battle.logme( "Ihr Schiff kann keine fl&uuml;chtenden Schiffe abfangen\n" );
					breakFlag = true;
					break;
				}

				if( (this.enemyShip.getAction() & Battle.BS_SECONDROW) != 0 &&
						!this.localweapon.getBoolean("very_long_range") )
				{
					battle.logme( "Ihre Waffen k&ouml;nnen das angegebene Ziel nicht erreichen\n" );
					breakFlag = true;
					break;
				}

				if( (this.enemyShip.getAction() & Battle.BS_JOIN) != 0 )
				{
					battle.logme( "Sie k&ouml;nnen nicht auf einem Schiff feuern, dass gerade erst der Schlacht beitritt\n" );
					breakFlag = true;
					break;
				}

				/*
				 * 	Anti-Torp-Verteidigungswerte ermitteln
				 */
				int fighterdef = 0;
				int antitorptrefferws = 0;

				if( this.localweapon.getDouble("destroyable") > 0 )
				{
					antitorptrefferws = this.getAntiTorpTrefferWS( enemyShipType, this.enemyShip);
					battle.logme("AntiTorp-TrefferWS: "+ this.getTWSText(antitorptrefferws) +"%\n");

					if( enemyShipType.getSize() > ShipType.SMALL_SHIP_MAXSIZE )
					{
						fighterdef = this.getFighterDefense(battle);
						if( fighterdef > 0 )
						{
							battle.logme("Verteidigung durch Schiffe: "+ this.getTWSText(fighterdef) +"%\n");	
						}	
					}
				}

				ownShipType = this.ownShip.getTypeData();
				ShipTypeData ownST = this.weapon.calcOwnShipType(ownShipType, enemyShipType);
				ShipTypeData enemyST = this.weapon.calcEnemyShipType(ownShipType, enemyShipType);

				ownShipType = ownST;
				enemyShipType = enemyST;

				int offensivskill = ownShip.getOffensiveValue();
				int navskill = ownShip.getNavigationalValue();
				int defensivskill = enemyShip.getDefensiveValue();
				
				if( battle.getCommander(ownShip.getSide()).hasFlag( User.FLAG_KS_DEBUG )) {
					battle.logme( "Offensivskill: "+offensivskill+"\n" );
					battle.logme( "Navskill: "+navskill+"\n" );
					battle.logme( "Defensivskill: "+defensivskill+"\n" );
				}

				/*
				 * 	Schadenswerte, Panzerung & TrefferWS ermitteln
				 */
				int absSchaden = this.getDamage(this.localweapon.getInt("basedamage"), offensivskill, enemyShipType);
				int shieldSchaden = this.getDamage(this.localweapon.getInt("shielddamage"), offensivskill, enemyShipType);

				int panzerung = enemyShip.getArmor();
				int schaden = absSchaden;

				int trefferWS = 0;
				if( enemyShipType.getSize() <= ShipType.SMALL_SHIP_MAXSIZE )
				{
					trefferWS = this.getSmallTrefferWS( battle, this.localweapon.getInt("deftrefferws"), this.enemyShip, enemyShipType, defensivskill, navskill );
				} 
				else
				{
					trefferWS = this.getTrefferWS( battle, this.localweapon.getInt("deftrefferws"), this.enemyShip, enemyShipType, defensivskill, navskill );
				}
				
				if( battle.getCommander(ownShip.getSide()).hasFlag( User.FLAG_KS_DEBUG )) {
					battle.logme( "Basis-TrefferWS: "+ trefferWS +"%\n");
					battle.logme( "FighterDef: "+ fighterdef +"%\n");
					battle.logme( "AntitorpTrefferWS: "+ antitorptrefferws +"%\n");
				}
				else
				{
					battle.logme( "Basis-TrefferWS: "+ this.getTWSText(trefferWS) +"\n");
				}
				trefferWS -= antitorptrefferws;
				// Minimum bei 5% bei zerstoerbaren Waffen
				if( (trefferWS - fighterdef < 5) && (fighterdef > 0) ) {
					trefferWS = 5;
				} 
				else {
					trefferWS -= fighterdef;
				}
				if( battle.getCommander(ownShip.getSide()).hasFlag( User.FLAG_KS_DEBUG )) {
					battle.logme( "TrefferWS: "+ trefferWS +"%\n" );
				}
				else
				{
					battle.logme( "TrefferWS: "+ this.getTWSText(trefferWS) +"\n");
				}

				int[] subdmgs = null;

				/*
				 * 	Subsystem-Schaden, falls notwendig, berechnen
				 */
				if( this.localweapon.getInt("subdamage") > 0 )
				{
					int subWS = this.getTrefferWS( battle, this.localweapon.getInt("subws"), this.enemyShip, enemyShipType, defensivskill, navskill );
					if( battle.getCommander(ownShip.getSide()).hasFlag( User.FLAG_KS_DEBUG )) {
						battle.logme( "SubsystemTWS: "+ subWS +"%\n" );
					}
					else
					{
						battle.logme( "SubsystemTWS: "+ this.getTWSText(subWS) +"\n");
					}
					
					int subPanzerung = panzerung;
					if( subPanzerung > 10 )
					{
						subPanzerung = 10;
						battle.logme("Panzerung absorbiert Subsystemschaden\n");
					} 
					else if( subPanzerung > 0 )
					{
						battle.logme("Panzerung reduziert Subsystemschaden ("+(subPanzerung*10)+"%)\n");
					}

					subdmgs = this.getSubDamages( subPanzerung, trefferWS, subWS, 1);
				} 

				if( schaden < 0 )
				{
					schaden = 0;
				}

				if( firstentry )
				{
					battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");
					firstentry = false;
				}

				Offizier attoffizier = Offizier.getOffizierByDest('s', ownShip.getId());
				
				/*
				 * 	Treffer berechnen
				 */
				int hit = 0;
				int def = 0;
				for( int i=1; i <= this.localweapon.getInt("count")*this.localweapon.getInt("shotsPerShot"); i++)
				{
					int rnd = RandomUtils.nextInt(101);
					if( battle.getCommander(ownShip.getSide()).hasFlag( User.FLAG_KS_DEBUG )) {
						battle.logme( i + ". Schuss: " + rnd + "%\n");
					}
					if( rnd <= trefferWS )
					{
						hit++;
						if( attoffizier != null)
						{
							int rnd2 = RandomUtils.nextInt(101);
							if( rnd2 <= 38)
							{
								attoffizier.gainExperience(Offizier.Ability.WAF, 1);
							}
							else if( rnd2 <= 76)
							{
								attoffizier.gainExperience(Offizier.Ability.COM, 1);
							}
							else
							{
								attoffizier.gainExperience(Offizier.Ability.NAV, 1);
							}
						}
					}
					if( (rnd > trefferWS) && (rnd <= trefferWS+fighterdef) && (this.localweapon.getDouble("destroyable") > 0) )
					{
						def++;
					}
				}
				battle.logme( this.weapon.getName()+": "+hit+" von "+(this.localweapon.getInt("count")*this.localweapon.getInt("shotsPerShot"))+" Sch&uuml;ssen haben getroffen\n" );
				battle.logenemy( Battle.log_shiplink(this.ownShip.getShip())+" feuert auf "+Battle.log_shiplink(this.enemyShip.getShip())+"\n+ Waffe: "+this.localweapon.getString("name")+"\n" );
				if( this.localweapon.getDouble("destroyable") > 0 && (def != 0) )
				{
					battle.logme( this.weapon.getName()+": "+def+" von "+(this.localweapon.getInt("count")*this.localweapon.getInt("shotsPerShot"))+" Sch&uuml;ssen wurden abgefangen\n" );
					battle.logenemy( "+ "+this.weapon.getName()+": "+def+" von "+(this.localweapon.getInt("count")*this.localweapon.getInt("shotsPerShot"))+" Sch&uuml;ssen wurden abgefangen\n" );
				}

				boolean savedamage = this.calcDamage( battle, this.enemyShip, enemyShipType, hit, shieldSchaden, schaden, subdmgs, "" );

				/*
				 *  Areadamage - falls notwendig - berechnen
				 */
				if( (this.localweapon.getInt("areadamage") != 0) && (hit != 0) )
				{
					List<BattleShip> areashiplist = this.getADShipList(battle);

					// In der $areashiplist ist das aktuell ausgewaehlte Schiff immer in der Mitte (abgerundet)
					int targetindex = areashiplist.size()/2;  

					// schaden anwenden
					int damagemod = 0;

					if( !this.localweapon.getBoolean("ad_full") )
					{
						damagemod = 1 / (this.localweapon.getInt("areadamage")+1);
					}

					for( int i=1; i <= this.localweapon.getInt("areadamage"); i++ )
					{
						// Es kann sein, dass die Liste nicht vollstaendig gefuellt ist (Schiffe ohne Schlacht).
						// Diese muessen wir jetzt rausfiltern
						if( (targetindex-i >= 0) && areashiplist.get(targetindex-i).getBattle() != null )
						{
							BattleShip aeShip = areashiplist.get(targetindex-i);

							this.calcADStep(battle, trefferWS, navskill, aeShip, hit, schaden, shieldSchaden, 1-i*damagemod);
						}
						if( (targetindex+i < areashiplist.size()) && areashiplist.get(targetindex+i).getBattle() != null )
						{
							BattleShip aeShip = areashiplist.get(targetindex+i);

							this.calcADStep(battle, trefferWS, navskill, aeShip, hit, schaden, shieldSchaden, 1-i*damagemod);
						}		
					}
				}

				/*
				 * 	E, Muni usw in die DB schreiben
				 */
				heat += this.localweapon.getInt("count");
				this.ownShip.getShip().setEnergy(this.ownShip.getShip().getEnergy() - this.weapon.getECost()*this.localweapon.getInt("count"));

				if( this.weapon.getAmmoType().length > 0)
				{
					Cargo mycargo = this.ownShip.getCargo();
					mycargo.substractResource( ((ItemCargoEntry)this.localweapon.get("ammoitem")).getResourceID(), this.localweapon.getInt("count")*this.weapon.getSingleShots() );
					this.ownShip.getShip().setCargo(mycargo);
				}
				
				heatList.put(weaponName, Integer.toString(heat));
				this.ownShip.getShip().setWeaponHeat(Weapons.packWeaponList(heatList));


				/*
				 *  BETAK - Check
				 */
				if( battle.getBetakStatus(battle.getOwnSide()) && !enemyShipType.isMilitary() )
				{
					battle.setBetakStatus(battle.getOwnSide(), false);
					battle.logme("[color=red][b]Sie haben die BETAK-Konvention verletzt[/b][/color]\n\n");
					battle.logenemy("[color=red][b]Die BETAK-Konvention wurde verletzt[/b][/color]\n\n");
				}

				/*
				 *	Schiff falls notwendig zerstoeren
				 */
				if( !savedamage && (config.getInt("DESTROYABLE_SHIPS") != 0) )
				{
					this.destroyShip(this.ownShip.getOwner().getId(), battle, this.enemyShip);
					int newindex = battle.getNewTargetIndex();
					if(newindex != -1)
					{
						battle.setEnemyShipIndex(newindex);
					}
					else
					{
						breakFlag = true;
					}
					this.enemyShip = battle.getEnemyShip();
				}

				/*
				 * 	Wenn das angreifende Schiff auch zerstoert werden muss tun wir das jetzt mal
				 */
				if( this.localweapon.getBoolean("destroyAfter") )
				{
					battle.logme( "[color=red]+ Angreifer zerst&ouml;rt[/color]\n" );
					battle.logenemy( "[color=red]+ Angreifer zerst&ouml;rt[/color]\n" );

					if( config.getInt("DESTROYABLE_SHIPS") != 0 )
					{
						this.destroyShipOnly(this.ownShip.getOwner().getId(), battle, this.ownShip, false, false);

						breakFlag = true;
						break;
					}
				}
			}

			if( outerloop < nextShipLoop - 1)
			{
				int newindex = battle.getNewTargetIndex();
				if( newindex == -1)
				{
					newindex = 0;
				}
				battle.setEnemyShipIndex(newindex);
			}

			if( breakFlag )
			{
				break;
			}
		}

		this.ownShip.getShip().setBattleAction(true);
		this.ownShip.setAction(this.ownShip.getAction() | Battle.BS_SHOT);

		if( !firstentry )
		{
			battle.logenemy("]]></action>\n");
		}

		if( (battle.getEnemyShip(oldenemyship).getAction() & Battle.BS_DESTROYED) == 0 )
		{
			battle.setEnemyShipIndex(oldenemyship);	
		}

		this.ownShip.getShip().recalculateShipStatus();

		return Result.OK;
	}

	private String getTWSText(int chance)
	{
		String answer = "";
		if (chance == 0)
		{
			answer = "nicht vorhanden";
		}
		else if (chance <=  25)
		{
			answer = "gering";
		}
		else if ( chance <= 50)
		{
			answer = "ausreichend";
		}
		else if ( chance <= 75)
		{
			answer = "gut";
		}
		else if ( chance <= 99)
		{
			answer = "hervorragend";
		}
		else
		{
			answer = "unfehlbar";
		}
		return answer;
	}
}

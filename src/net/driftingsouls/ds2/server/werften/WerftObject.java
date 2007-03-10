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
package net.driftingsouls.ds2.server.werften;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.cargo.modules.Module;
import net.driftingsouls.ds2.server.cargo.modules.ModuleItemModule;
import net.driftingsouls.ds2.server.cargo.modules.Modules;
import net.driftingsouls.ds2.server.config.IEDisableShip;
import net.driftingsouls.ds2.server.config.IEDraftShip;
import net.driftingsouls.ds2.server.config.IEModule;
import net.driftingsouls.ds2.server.config.ItemEffect;
import net.driftingsouls.ds2.server.config.Items;
import net.driftingsouls.ds2.server.config.ModuleSlots;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DSObject;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.PreparedQuery;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.ships.Ships.ModuleEntry;

/**
 * Basisklasse fuer alle Werfttypen in DS
 * @author Christopher Jung
 *
 */
public abstract class WerftObject extends DSObject {
	protected static final int BUILDING = 0;
	protected static final int SHIP = 1;
	
	protected int werftid = 0;
	protected String werfttag = "";
	protected int system = 0;
	protected int owner = 0;
	protected int oneway = 0;
	private int building = 0;
	private int buildItem = -1;
	private int remaining = 0;
	private boolean buildFlagschiff = false;
	private int type = 0;
	
	protected WerftObject( SQLResultRow werftdata, String werfttag, int system, int owner) {
		this.werftid = werftdata.getInt("id");
		this.werfttag = werfttag;
		this.system = system;
		this.owner = owner;
		this.type = werftdata.getInt("type");
		this.building = werftdata.getInt("building");
		this.buildItem = werftdata.getInt("item");
		this.remaining = werftdata.getInt("remaining");
		this.buildFlagschiff = werftdata.getBoolean("flagschiff");
	}
	
	/**
	 * Gibt zurueck, ob in der Werft im Moment gebaut wird
	 * @return <code>true</code>, falls gebaut wird
	 */
	public boolean isBuilding() {
		return building != 0;
	}
	
	/**
	 * Gibt den im Moment gebauten Schiffstyp zurueck
	 * @return Array mit Schiffstypdaten oder <code>null</code>
	 */
	public SQLResultRow getBuildShipType() {
		if( building > 0 ) {
			return Ships.getShipType(building, false);
		}
		
		return null;
	}
	
	/**
	 * Dekrementiert die verbliebene Bauzeit um 1
	 */
	public void decRemainingTime() {
		if( remaining <= 0 ) {
			return;
		}
		remaining--;
		ContextMap.getContext().getDatabase().update("UPDATE werften SET remaining=remaining-1 WHERE id=",getWerftID());
	}
	
	/**
	 * Inkrementiert die verbliebene Bauzeit um 1
	 */
	public void incRemainingTime() {
		remaining++;
		ContextMap.getContext().getDatabase().update("UPDATE werften SET remaining=remaining+1 WHERE id=",getWerftID());
	}
	
	/**
	 * Liefert die noch verbleibende Bauzeit
	 * @return verbleibende Bauzeit
	 */
	public int getRemainingTime() {
		return remaining;
	}
	
	/**
	 * Gibt das fuer den Bau benoetigte Item zurueck.
	 * Falls kein Item benoetigt wird, wird -1 zurueckgegeben.
	 * @return Item-ID oder -1
	 */
	public int getRequiredItem() {
		return buildItem;
	}
	
	/**
	 * Gibt den Typ der Werft zurueck
	 * @return Typ der Werft
	 */
	public int getType() {
		return type;
	}
	
	/**
	 * Beendet den Bauprozess des aktuell gebauten Schiffes erfolgreich.
	 * Sollte dies nicht moeglich sein, wird 0 zurueckgegeben.
	 * 
	 * @return die ID des gebauten Schiffes oder 0
	 */
	public int finishBuildProcess() {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		SQLResultRow shipd = this.getBuildShipType();

		int owner = this.getOwner();
		int x = this.getX();
		int y = this.getY();
		int system = this.getSystem();
					
		Cargo cargo = new Cargo();
		User auser = context.createUserObject(owner);
		
		String currentTime = Common.getIngameTime(context.get(ContextCommon.class).getTick());
		String history = "Indienststellung am "+currentTime+" durch "+auser.getName()+" ("+auser.getID()+")\n";
			
		PreparedQuery shipCreate = db.prepare("INSERT INTO ships " ,
				"(id,owner,type,x,y,system,crew,hull,cargo,e,history) " ,
				"VALUES " ,
				"('', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		shipCreate.update(owner,shipd.getInt("id"), x, y, system, shipd.getInt("crew"), shipd.getInt("hull"), cargo.save(), shipd.getInt("eps"), history);

		int id = shipCreate.insertID();

		if( shipd.getString("werft").length() > 0 ) {
			db.update("INSERT INTO werften (shipid) VALUES (",id,")");
			this.MESSAGE.get().append("\tWerft '"+shipd.getString("werft")+"' in Liste der Werften eingetragen\n");
		}
		if( this.buildFlagschiff ) {
			db.update("UPDATE users SET flagschiff=",id," WHERE id=",owner);
			this.MESSAGE.get().append("\tFlagschiff eingetragen\n");
		}
		
		// Item benutzen
		if( this.getRequiredItem() > -1 ) {
			cargo = this.getCargo(true);
			List<ItemCargoEntry> itemlist = cargo.getItem(this.getRequiredItem());
			boolean ok = false;
			for( int i=0; i < itemlist.size(); i++ ) {
				if( itemlist.get(i).getMaxUses() == 0 ) {
					ok = true;
					break;
				}
			}
			
			if( !ok ) {
				User user = context.createUserObject(this.getOwner());
				
				Cargo allyitems = null;
				if( user.getAlly() > 0 ) {
					allyitems = new Cargo( Cargo.Type.ITEMSTRING, db.first("SELECT items FROM ally WHERE id=",user.getAlly()).getString("items"));
					itemlist = allyitems.getItem(this.getRequiredItem());
					for( int i=0; i < itemlist.size(); i++ ) {
						if( itemlist.get(i).getMaxUses() == 0 ) {
							ok = true;
							break;
						}
					}
				}
				
				if( !ok ) {
					ItemCargoEntry item = null;
					String source = "";
					if( (user.getAlly() > 0) && allyitems.hasResource(new ItemID(this.getRequiredItem())) ) {
						item = allyitems.getItem(this.getRequiredItem()).get(0);
						source = "ally";
					}
					else {
						item = cargo.getItem(this.getRequiredItem()).get(0);
						source = "local";
					}
					
					item.useItem();
					
					if( source.equals("local") ) {
						this.setCargo(cargo, true);
					}
					else {
						db.update("UPDATE users t1 JOIN ally t2 ON t1.ally=t2.id SET t2.items='",allyitems.getData(Cargo.Type.ITEMSTRING),"' WHERE t1.id=",this.getOwner());
					}
				}
			}
		}
		
		Ships.recalculateShipStatus(id);
		
		db.update("UPDATE werften SET building=0,remaining=0,item=-1,flagschiff=0 WHERE id=",this.getWerftID());
		this.building = 0;
		this.remaining = 0;
		this.buildFlagschiff = false;
		this.buildItem = -1;
		
		return id;
	}
	
	/**
	 * Gibt zurueck, ob alle Voraussetzungen fuer eine Weiterfuehrung
	 * des Bauprozesses erfuellt sind. Wenn nichts gebaut wird,
	 * wird ebenfalls true zurueckgegeben.
	 * 
	 * @return <code>true</code>, falls alle Voraussetzungen erfuellt sind
	 */
	public boolean isBuildContPossible() {
		if( !this.isBuilding() ) {
			return true;
		}
		
		if( this.getRequiredItem() > -1 ) {
			Context context = ContextMap.getContext();
			Database db = context.getDatabase();
			
			Cargo cargo = this.getCargo(true);
			User user = context.createUserObject(this.getOwner());
			
			if( user.getAlly() > 0 ) {
				Cargo allyitems = new Cargo( Cargo.Type.ITEMSTRING, db.first("SELECT items FROM ally WHERE id=",user.getAlly()).getString("items"));
				cargo.addCargo( allyitems );
			}
			
			List<ItemCargoEntry> itemlist = cargo.getItem(this.getRequiredItem());
			if( itemlist.size() == 0 ) {
				return false;
			}
		}
	
		return true;
	}
	
	/**
	 * Bricht das aktuelle Bauvorhaben ab
	 */
	public void cancelBuild() {
		Database db = ContextMap.getContext().getDatabase();
		
		db.update("UPDATE werften SET building='0',remaining='0',item='-1',flagschiff='0' WHERE id='",getWerftID(),"'");
		building = 0;
		remaining = 0;
		buildItem = 0;
		buildFlagschiff = false;
	}
	
	/**
	 * Setzt das Einwegflag der Werft auf die angegebene Typen-ID.<br>
	 * Wenn das Einweg-Flag gesetzt ist, wird beim Bau die Werft in den angegebenen Typ fuer die
	 * Dauer des Baus umgewandelt
	 * @param flag Das Einweg-Flag
	 */
	public void setOneWayFlag(int flag) {
		oneway = flag;
	}

	/**
	 * Gibt das Einweg-Flag der Werft zurueck
	 * @return Das Einweg-Flag
	 */
	public int getOneWayFlag() {
		return oneway;
	}

	/**
	 * Gibt die ID des Werfteintrags zurueck
	 * @return Die ID des Werfteintrags
	 */
	public int getWerftID() {
		return werftid;
	}

	/**
	 * Gibt den Werfttag zurueck. Der Werfttag identifiziert, welche Schiffe die Werft bauen kann
	 * @return Der Werfttag
	 */
	public String getWerftTag() {
		return werfttag;
	}

	/**
	 * Gibt den Besitzer der Werft zurueck
	 * @return Die ID des Besitzers
	 */
	public int getOwner() {
		return owner;
	}

	/**
	 * Gibt das System zurueck, in dem die Werft steht
	 * @return Die ID des Systems
	 */
	public int getSystem() {
		return system;
	}
	
	/**
	 * Gibt den Werfttyp (Basis,Schiff) zurueck
	 * @return Der Werfttyp
	 */
	public abstract int getWerftType();

	/**
	 * Gibt den Cargo der Werft zurueck
	 * @param localonly Soll nur der eigene (<code>true</code>) oder auch der Cargo von gekoppelten Objekten (<code>false</code>) genommen werden?
	 * @return Der Cargo der Werft
	 */
	public abstract Cargo getCargo(boolean localonly);

	/**
	 * Schreibt den Cargo der Werft wieder in die DB
	 * @param cargo Der neue Cargo der Werft
	 * @param localonly Handelt es sich nur um den Cargo der Werft (<code>true</code>) oder auch um den Cargo von gekoppelten Objekten (<code>false</code>)?
	 */
	public abstract void setCargo(Cargo cargo, boolean localonly);

	/**
	 * Gibt die maximale Cargogroesse zurueck, den die Werft besitzen kann
	 * @param localonly Soll nur der eigene (<code>true</code>) oder auch der Lagerplatz von gekoppelten Objekten (<code>false</code>) genommen werden?
	 * @return Die maximale Cargogroesse
	 */
	public abstract long getMaxCargo(boolean localonly);

	/**
	 * Gibt die vorhandene Crew zurueck
	 * @return Die vorhandene Crew
	 */
	public abstract int getCrew();

	/**
	 * Gibt die maximale Crew der Werft zurueck
	 * @return Die maximale Crew
	 */
	public abstract int getMaxCrew();

	/**
	 * Setzt die Crew der Werft auf den angegebenen Wert
	 * @param crew Die neue Crew der Werft
	 */
	public abstract void setCrew(int crew);

	/**
	 * Gibt die vorhanene Energie der Werft zurueck
	 * @return Die Energie auf der Werft
	 */
	public abstract int getEnergy();

	/**
	 * Setzt die vorhanene Energie auf der Werft auf den neuen Wert<br>
	 * Annahme: Es kann nur weniger Energie werden - niemals mehr
	 * @param e Die neue Energie der Werft
	 */
	public abstract void setEnergy(int e);

	/**
	 * Gibt zurueck, wieviele Offiziere auf die Werft transferiert werden koennen
	 * @return Die max. Anzahl an transferierbaren Offizieren
	 */
	public abstract int canTransferOffis();

	/**
	 * Transferiert den Offizier mit der angegebenen ID auf die Werft
	 * @param offi Die ID des zu transferierenden Offiziers
	 */
	public abstract void transferOffi(int offi);

	/**
	 * Gibt die URL-Basis der Werft zurueck
	 * @return Die URL-Basis
	 */
	public abstract String getUrlBase();
	
	/**
	 * Gibt einige versteckte Formfelder zurueck fuer Werftaufrufe via Forms
	 * @return Einige versteckte Formfelder
	 */
	public abstract String getFormHidden();

	/**
	 * Gibt die X-Koordinate der Werft zurueck
	 * @return Die X-Koordinate
	 */
	public abstract int getX();

	/**
	 * Gibt die Y-Koordinate der Werft zurueck
	 * @return Die Y-Koordinate
	 */
	public abstract int getY();
	
	/**
	 * Gibt den Namen der Werft zurueck
	 * @return Der Name
	 */
	public abstract String getName();
	
	/**
	 * Gibt den Radius der Werft zurueck
	 * @return Der Radius
	 */
	public int getSize() {
		return 0;	
	}
	
	/**
	 * Entfernt ein Item-Modul aus einem Schiff. Das Item-Modul
	 * befindet sich anschiessend auf der Werft.
	 * {@link DSObject#MESSAGE} enthaelt die Hinweistexte
	 * 
	 * @param ship Array mit Schiffsdaten
	 * @param slot Modulslot, aus dem das Modul ausgebaut werden soll
	 * 
	 */
	public void removeModule( SQLResultRow ship, int slot ) {
		Database db = ContextMap.getContext().getDatabase();
		
		Map<Integer,Integer> usedslots = new HashMap<Integer,Integer>();
		ModuleEntry[] modules = Ships.getModules(ship);
		for( int i=0; i < modules.length; i++ ) {
			usedslots.put(modules[i].slot, i);
		}
				
		if( !usedslots.containsKey(slot) ) {
			MESSAGE.get().append("Es befindet sich kein Modul in diesem Slot\n");
			return;
		}
		
		SQLResultRow shiptype = Ships.getShipType( ship.getInt("type"), false );
		
		String[] aslot = null;
		
		String[] moduleslots = StringUtils.split(shiptype.getString("modules"), ';');
		for( int i=0; i < moduleslots.length; i++ ) {
			String[] data = StringUtils.split(moduleslots[i], ':');
		
			if( Integer.parseInt(data[0]) == slot ) {
				aslot = data;
				break;	
			}	
		}
		
		if( aslot == null ) {
			MESSAGE.get().append("Keinen passenden Slot gefunden\n");
			return;
		}
		
		SQLResultRow oldshiptype = (SQLResultRow)shiptype.clone();
		
		ModuleEntry module = modules[usedslots.get(slot)];
		Module moduleobj = Modules.getShipModule( module );
		if( aslot.length > 2 ) {
			moduleobj.setSlotData(aslot[2]);
		}
		
		Cargo cargo = getCargo(false);
		
		if( moduleobj instanceof ModuleItemModule ) {		
			ResourceID itemid = ((ModuleItemModule)moduleobj).getItemID();
			cargo.addResource( itemid, 1 );
		}
		Ships.removeModule( ship, module.slot, module.moduleType, module.data );
		
		moduleUpdateShipData(db, ship.getInt("id"), oldshiptype, cargo);
									
		MESSAGE.get().append("Modul ausgebaut\n");
		return;
	}

	private void moduleUpdateShipData(Database db, int shipID, SQLResultRow oldshiptype, Cargo cargo) {
		SQLResultRow ship = db.first("SELECT id,x,y,system,owner,battle,type,status,hull,shields,e,cargo,crew FROM ships WHERE id>0 AND id=",shipID);
		SQLResultRow shiptype = Ships.getShipType( ship );
		
		if( ship.getInt("hull") != shiptype.getInt("hull") ) {
			double factor = ship.getInt("hull") / (double)oldshiptype.getInt("hull");
			ship.put("hull", (int)(shiptype.getInt("hull") * factor));	
		}
				
		if( ship.getInt("hull") > shiptype.getInt("hull") ) {
			ship.put("hull", shiptype.getInt("hull"));	
		}
		
		if( ship.getInt("shields") > shiptype.getInt("shields") ) {
			ship.put("shields", shiptype.getInt("shields"));	
		}
		
		if( ship.getInt("e") > shiptype.getInt("eps") ) {
			ship.put("e", shiptype.getInt("eps"));	
		}
		
		if( ship.getInt("crew") > shiptype.getInt("crew") ) {
			ship.put("crew", shiptype.getInt("crew"));	
		}
		
		Cargo shipcargo = new Cargo( Cargo.Type.STRING, ship.getString("cargo") );
		if( shipcargo.getMass() > shiptype.getLong("cargo") ) {
			Cargo newshipcargo = shipcargo.cutCargo( shiptype.getLong("cargo") );
			if( this.getMaxCargo(false) - cargo.getMass() > 0 ) {
				Cargo addwerftcargo = shipcargo.cutCargo( this.getMaxCargo(false) - cargo.getMass() );
				cargo.addCargo( addwerftcargo );
			}
			shipcargo = newshipcargo;
		}
		
		this.setCargo( cargo, false );
		
		StringBuilder output = MESSAGE.get();
		
		int jdockcount = db.first("SELECT count(*) count FROM ships WHERE docked='l ",ship.getInt("id"),"' AND id>0").getInt("count");
		if( jdockcount > shiptype.getInt("jdocks") ) {
			SQLQuery sid = db.query("SELECT id FROM ships WHERE docked='l ",ship.getInt("id"),"' AND id>0 LIMIT ",(jdockcount-shiptype.getInt("jdocks")));
			
			int[] undockarray = new int[sid.numRows()];
			int count = 0;
			while( sid.next() ) {
				undockarray[count++] = sid.getInt("id");
			}
			sid.free();
			
			output.append((jdockcount-shiptype.getInt("jdocks"))+" gelandete Schiffe wurden gestartet\n");
					
			Ships.dock(Ships.DockMode.START, ship.getInt("owner"), ship.getInt("id"), undockarray);
		}
				
		int adockcount = db.first("SELECT count(*) count FROM ships WHERE docked='",ship.getInt("id"),"' AND id>0").getInt("count");
		if( adockcount > shiptype.getInt("adocks") ) {
			SQLQuery sid = db.query("SELECT id FROM ships WHERE docked='",ship.getInt("id"),"' AND id>0 LIMIT ",(adockcount-shiptype.getInt("adocks")));

			int[] undockarray = new int[sid.numRows()];
			int count = 0;
			while( sid.next() ) {
				undockarray[count++] = sid.getInt("id");
			}
			sid.free();
			
			output.append((adockcount-shiptype.getInt("adocks"))+" extern gedockte Schiffe wurden abgedockt\n");
			
			Ships.dock(Ships.DockMode.UNDOCK, ship.getInt("owner"), ship.getInt("id"), undockarray);
		}
		
		
		db.update("UPDATE ships SET hull=",ship.getInt("hull"),",shields=",ship.getInt("shields"),"," ,
				"e=",ship.getInt("e"),",crew=",ship.getInt("crew"),",cargo='",shipcargo.save(),"' " ,
				"WHERE id>0 AND id=",ship.getInt("id"));
						
		if( shiptype.getString("werft").length() == 0 ) {
			db.update("DELETE FROM werften WHERE shipid=",ship.getInt("id"));	
		}
		else {
			SQLResultRow wid = db.first("SELECT id FROM werften WHERE shipid=",ship.getInt("id"));
			if( wid.isEmpty() ) {
				db.update("INSERT INTO werften (shipid) VALUES ('",ship.getInt("id"),"')");	
			}	
		}
	} 
	
	//--------------------------------------------------------------------------------------------------------------
	
	/**
	 * Fuegt einem Schiff ein Item-Modul hinzu. Das Item-Modul
	 * muss auf der Werft vorhanden sein.
	 * {@link DSObject#MESSAGE} enthaelt die Hinweistexte
	 * 
	 * @param ship Array mit den Daten des Schiffes
	 * @param slot Der Slot, in den das Modul eingebaut werden soll
	 * @param item Die ID des einzubauenden Item-Moduls
	 * 
	 */
	public void addModule( SQLResultRow ship, int slot, int item ) {
		Database db = ContextMap.getContext().getDatabase();
		
		Map<Integer,Integer> usedslots = new HashMap<Integer,Integer>();
		ModuleEntry[] modules = Ships.getModules(ship);
		for( int i=0; i < modules.length; i++ ) {
			usedslots.put(modules[i].slot, i);
		}
		
		if( usedslots.containsKey(slot) ) {
			MESSAGE.get().append("Der Slot ist bereits belegt\n");
			return;
		}
		
		Cargo cargo = this.getCargo(false);
		List<ItemCargoEntry> itemlist = cargo.getItemsWithEffect( ItemEffect.Type.MODULE );
		
		SQLResultRow shiptype = Ships.getShipType( ship.getInt("type"), false );
		
		String[] aslot = null;
		
		String[] moduleslots = StringUtils.split(shiptype.getString("modules"), ';');
		for( int i=0; i < moduleslots.length; i++ ) {
			String[] data = StringUtils.split(moduleslots[i], ':');
		
			if( Integer.parseInt(data[0]) == slot ) {
				aslot = data;
				break;	
			}	
		}
		
		if( aslot == null ) {
			MESSAGE.get().append("Keinen passenden Slot gefunden\n");
			return;
		}
			
		if( (aslot == null) || !ModuleSlots.get().slot(aslot[1]).isMemberIn( ((IEModule)Items.get().item(item).getEffect()).getSlots() ) ) {
			MESSAGE.get().append("Das Item passt nicht in dieses Slot\n");
			return;
		}
		
		if( Items.get().item(item).getAccessLevel() > ContextMap.getContext().getActiveUser().getAccessLevel() ) {
			MESSAGE.get().append("Ihre Techniker wissen nichts mit dem Modul anzufangen\n");
			return;
		}
		
		ItemCargoEntry myitem = null;
	
		for( int i=0; i < itemlist.size(); i++ ) {
			if( itemlist.get(i).getItemID() == item ) {
				myitem = itemlist.get(i);	
				break;
			}	
		}
	
		if( myitem == null ) {
			MESSAGE.get().append("Kein passendes Item gefunden\n");
			return;
		}
		
		SQLResultRow oldshiptype = (SQLResultRow)shiptype.clone();
	
		Ships.addModule( ship, slot, Modules.MODULE_ITEMMODULE, Integer.toString(item) );
		cargo.substractResource( myitem.getResourceID(), 1 );
	
		moduleUpdateShipData(db, ship.getInt("id"), oldshiptype, cargo);
			
		MESSAGE.get().append("Modul eingebaut\n");
		
		return;
	}
	
	/**
	 * Berechnet den Cargo, den man beim Demontieren eines Schiffes zurueckbekommt. Er entspricht somit
	 * dem reinen Schrottwert des Schiffes :)
	 * Die aktuell geladenen Waren des Schiffes sind nicht teil des Cargos!
	 * @param ship Array mit Daten des Schiffes
	 * 
	 * @return Cargo mit den Resourcen
	 */
	public Cargo getDismantleCargo( SQLResultRow ship ) {
		Database db = ContextMap.getContext().getDatabase();
		
		SQLResultRow shiptype = Ships.getShipType( ship );
		
		SQLResultRow baubar = db.first("SELECT costs FROM ships_baubar WHERE type=",ship.getInt("type"));
			
		//Kosten berechnen
		Cargo cost = new Cargo();
		
		if( baubar.isEmpty() ) {
			double htr = ship.getInt("hull")*0.0090;
			cost.addResource( Resources.KUNSTSTOFFE, (long)(htr/15) );
			cost.addResource( Resources.TITAN, (long)(htr/5) );
			cost.addResource( Resources.ADAMATIUM, (long)(htr/10) );
			cost.addResource( Resources.PLATIN, 
					(long)(htr*( 
							Math.floor((100-ship.getInt("engine"))/2d) + 
							Math.floor((100-ship.getInt("sensors"))/4d) + 
							Math.floor((100-ship.getInt("comm"))/4d) + 
							Math.floor((100-ship.getInt("weapons"))/2d)
					)/900d) );
			cost.addResource( Resources.SILIZIUM, 
					(long)(htr*(
							Math.floor((100-ship.getInt("engine"))/6d) + 
							Math.floor((100-ship.getInt("sensors"))/2d) + 
							Math.floor((100-ship.getInt("comm"))/2d) + 
							Math.floor((100-ship.getInt("weapons"))/5d)
					)/1200d) );
			cost.addResource( Resources.KUNSTSTOFFE, 
					(long)(
							cost.getResourceCount(Resources.SILIZIUM)+
							cost.getResourceCount(Resources.PLATIN)/2d+
							cost.getResourceCount(Resources.TITAN)/3d
					)*3);
		} 
		else {
			Cargo buildcosts = new Cargo(Cargo.Type.STRING, baubar.getString("costs"));
			
			double factor = (ship.getInt("hull")/shiptype.getInt("hull"))*0.90d;
			cost.addResource( Resources.KUNSTSTOFFE, (long)(factor*buildcosts.getResourceCount(Resources.KUNSTSTOFFE)) );
			cost.addResource( Resources.TITAN, (long)(factor*buildcosts.getResourceCount(Resources.TITAN)) );
			cost.addResource( Resources.ADAMATIUM, (long)(factor*buildcosts.getResourceCount(Resources.ADAMATIUM)) );
			cost.addResource( Resources.PLATIN, 
					(long)(( factor-
							((100-ship.getInt("engine"))/200d)-
							((100-ship.getInt("sensors"))/400d)-
							((100-ship.getInt("comm"))/400d)-
							((100-ship.getInt("weapons"))/200d) 
						)*buildcosts.getResourceCount(Resources.PLATIN)) );
			cost.addResource( Resources.SILIZIUM, 
					(long)(( factor-
							((100-ship.getInt("engine"))/600d)-
							((100-ship.getInt("sensors"))/200d)-
							((100-ship.getInt("comm"))/200d)-
							((100-ship.getInt("weapons"))/500d) 
						)*buildcosts.getResourceCount(Resources.SILIZIUM)) );
			cost.addResource( Resources.XENTRONIUM, 
					(long)(( factor-
							((100-ship.getInt("engine"))/200d)-
							((100-ship.getInt("sensors"))/400d)-
							((100-ship.getInt("comm"))/400d)-
							((100-ship.getInt("weapons"))/200d) 
						)*buildcosts.getResourceCount(Resources.XENTRONIUM)) );
			cost.addResource( Resources.ISOCHIPS, 
					(long)(( factor-
							((ship.getInt("engine")-100)/600d)-
							((100-ship.getInt("sensors"))/100d)-
							((100-ship.getInt("comm"))/100d)-
							((100-ship.getInt("weapons"))/300d) 
						)*buildcosts.getResourceCount(Resources.ISOCHIPS)) );
		}
		
		if( cost.getResourceCount( Resources.PLATIN ) <  0 ) {
			cost.setResource( Resources.PLATIN, 0 );	
		}
		if( cost.getResourceCount( Resources.SILIZIUM ) <  0 ) {
			cost.setResource( Resources.SILIZIUM, 0 );	
		}
		if( cost.getResourceCount( Resources.XENTRONIUM ) <  0 ) {
			cost.setResource( Resources.XENTRONIUM, 0 );	
		}
		if( cost.getResourceCount( Resources.ISOCHIPS ) <  0 ) {
			cost.setResource( Resources.ISOCHIPS, 0 );	
		}
		
		return cost;
	}
	
	/**
	 * Demontiert ein Schiff. Es wird dabei nicht ueberprueft, ob sich Schiff
	 * und Werft im selben Sektor befinden, ob das Schiff in einem Kampf ist usw sondern
	 * nur das demontieren selbst.{@link DSObject#MESSAGE} enthaelt die Hinweistexte
	 * 
	 * @param dismantle Die ID des zu demontierenden Schiffes
	 * @param testonly Soll nur geprueft (true) oder wirklich demontiert werden (false)?
	 * @return true, wenn kein Fehler aufgetreten ist
	 */
	public boolean dismantleShip(int dismantle, boolean testonly) {	
		Database db = ContextMap.getContext().getDatabase();
		
		StringBuilder output = MESSAGE.get();
	
		SQLResultRow sd = db.first("SELECT id,hull,cargo,engine,sensors,comm,weapons,type,name,crew,status FROM ships WHERE id>0 AND id=",dismantle);
		
		Cargo scargo = new Cargo( Cargo.Type.STRING, sd.getString("cargo") );
		
		Cargo cargo = this.getCargo(false);
	
	 	long maxcargo = this.getMaxCargo(false);
	
		Cargo cost = this.getDismantleCargo( sd );
		
		Cargo newcargo = (Cargo)cargo.clone();
		long totalcargo = cargo.getMass();
	
		boolean ok = true;
		
		cost.addCargo( scargo );
		newcargo.addCargo( cost );
	
		if( cost.getMass() + totalcargo > maxcargo ) {
			output.append("Nicht gen&uuml;gend Platz f&uuml;r alle Waren\n");
			ok = false;
		}
	
		if( this.getCrew() + sd.getInt("crew") > this.getMaxCrew() ) {
			output.append("Nicht gengend Platz f&uuml;r die Crew\n");
			ok = false;
		}
		
		int maxoffis = this.canTransferOffis();
		
		SQLQuery offizierRow = db.query("SELECT id FROM offiziere WHERE dest='s ",dismantle,"' AND userid=",this.getOwner());
		if( offizierRow.numRows() > maxoffis ) {
			output.append("Nicht genug Platz f&uuml;r alle Offiziere");
			ok = false;
		}
		if( !ok ) {
			offizierRow.free();
			return false;
		}
			
		if( ok && !testonly ) {
			this.setCargo(newcargo, false);
	
			this.setCrew(this.getCrew()+sd.getInt("crew"));
			while( offizierRow.next() ) {
				this.transferOffi(offizierRow.getInt("id"));
			}
	
			Ships.destroy( dismantle );
		}
		offizierRow.free();
		
		return ok;
	}
	
	/**
	 * Die Reparaturkosten eines Schiffes
	 *
	 */
	public class RepairCosts {
		/**
		 * Die Energiekosten
		 */
		public int e;
		/**
		 * Die Resourcenkosten
		 */
		public Cargo cost;
		
		protected RepairCosts() {
			//EMPTY
		}
	}
	
	/**
	 * Berechnet die Reparaturkosten fuer ein Schiff
	 * @param ship Array mit Schiffsdaten
	 * 
	 * @return Die Reparaturkosten
	 */
	public RepairCosts getRepairCosts( SQLResultRow ship ) {
		SQLResultRow shiptype = Ships.getShipType( ship );
		
		//Kosten berechnen
		int htr = shiptype.getInt("hull")-ship.getInt("hull");
		int htrsub = (int)Math.round(shiptype.getInt("hull")*0.5d);
		
		if( htr > htrsub ) {
			htrsub = htr;
		}
		
		Cargo cost = new Cargo();
		cost.addResource( Resources.KUNSTSTOFFE, (long)(htr/55d) );
		cost.addResource( Resources.TITAN, (long)(htr/20d) );
		cost.addResource( Resources.ADAMATIUM, (long)(htr/40d) );
		cost.addResource( Resources.PLATIN, 
				(long)(htrsub/100d*(
						Math.floor(100-ship.getInt("engine")) + 
						Math.floor((100-ship.getInt("sensors"))/4d) + 
						Math.floor((100-ship.getInt("comm"))/4d) + 
						Math.floor(100-ship.getInt("weapons"))/2d
					)/106d) );
		cost.addResource( Resources.SILIZIUM, 
				(long)(htrsub/100d*(
						Math.floor((100-ship.getInt("engine"))/3) + 
						Math.floor((100-ship.getInt("sensors"))/2) + 
						Math.floor((100-ship.getInt("comm"))/2) + 
						Math.floor((100-ship.getInt("weapons"))/5)
					)/72d) );
		cost.addResource( Resources.KUNSTSTOFFE, 
				(long)((
						cost.getResourceCount(Resources.SILIZIUM)+
						cost.getResourceCount(Resources.PLATIN)/2d+
						cost.getResourceCount(Resources.TITAN)/3d
					)*0.5d) );
		int energie = (int)Math.round(
				((long)(
						cost.getResourceCount(Resources.SILIZIUM)+
						cost.getResourceCount(Resources.PLATIN)/2d+
						cost.getResourceCount(Resources.TITAN)/2d
				)*1.5d));
		
		if( energie > 900 ) {
			energie = 900;
		}
		
		RepairCosts rc = new RepairCosts();
		rc.e = energie;
		rc.cost = cost;
		
		return rc;
	}
	
	/**
	 * Repariert ein Schiff auf einer Werft.
	 * Es werden nur Dinge geprueft, die unmittelbar mit dem Repariervorgang selbst
	 * etwas zu tun haben. Die Positionen von Schiff und Werft usw werden jedoch nicht gecheckt.
	 * {@link DSObject#MESSAGE} enthaelt die Hinweistexte
	 *
	 * @param ship Array mit Schiffsdaten
	 * @param testonly Soll nur getestet (true) oder auch wirklich repariert (false) werden?
	 * 
	 * @return true, wenn kein Fehler aufgetreten ist
	 */
	public boolean repairShip(SQLResultRow ship, boolean testonly) {
		Database db = ContextMap.getContext().getDatabase();
		
		SQLResultRow shiptype = Ships.getShipType( ship );
		
		Cargo cargo = this.getCargo(false);
	
		RepairCosts rc = this.getRepairCosts(ship);
		
		Cargo newcargo = cargo;
		boolean ok = true;
		int newe = this.getEnergy();
	
		//Kosten ausgeben
		ResourceList reslist = rc.cost.compare( cargo, false );
		for( ResourceEntry res : reslist ) {
			if( res.getDiff() > 0 ) {
				ok = false;
			}
		}
		newcargo.substractCargo( rc.cost );
		
		if( rc.e > 0 ) {
			if( rc.e > newe ) {
				ok = false;
			}
			newe -= rc.e;
		}
	
	
		if( !ok ) {
			MESSAGE.get().append("Nicht gen&uuml;gend Material zur Reperatur vorhanden");
			return false;
		} 
		else if( !testonly ) {
			this.setCargo( newcargo, false );
			
			this.setEnergy(newe);
			db.update("UPDATE ships SET hull=",shiptype.getInt("hull"),",engine=100,sensors=100,comm=100,weapons=100 WHERE id>0 AND id=",ship.getInt("id"));
		}
		return true;
	}
	
	/**
	 * Liefert die Liste aller theoretisch baubaren Schiffe auf dieser Werft.
	 * Das vorhanden sein von Resourcen wird hierbei nicht beruecksichtigt.
	 * @return array mit Schiffsbaudaten (ships_baubar) sowie 
	 * 			'_item' => array( ('local' | 'ally'), $resourceid) oder '_item' => false
	 * 			zur Bestimmung ob und wenn ja welcher Bauplan benoetigt wird zum bauen
	 */
	public SQLResultRow[] getBuildShipList() {
		List<SQLResultRow> result = new ArrayList<SQLResultRow>();
		
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		User user = context.createUserObject(this.getOwner());
	
		boolean flagschiff = user.hasFlagschiffSpace();
	
		String fsquery = "";
		if( !flagschiff ) {
			fsquery = "AND t1.flagschiff=0";
		}
	
		String sysreqquery = "";
		if( !Systems.get().system(this.getSystem()).isMilitaryAllowed() ) {
			sysreqquery = "t1.systemreq=0 AND ";
		}
		String query = "SELECT t1.id,t1.race,t1.type,t1.dauer,t1.costs,t1.ekosten,t1.crew,t1.tr1,t1.tr2,t1.tr3,t1.flagschiff,t1.linfactor " +
			"FROM ships_baubar t1 JOIN ship_types t2 ON t1.type=t2.id " +
			"WHERE "+sysreqquery+" LOCATE('"+this.getWerftTag()+"',t1.werftreq) "+fsquery+" " +
			"ORDER BY t2.nickname";
			
	
		Cargo availablecargo = this.getCargo(false);
	
		Cargo allyitems = null;
		if( user.getAlly() > 0 ) {
			allyitems = new Cargo(Cargo.Type.ITEMSTRING, db.first("SELECT items FROM ally WHERE id=",user.getAlly()).getString("items"));
		}
		else {
			allyitems = new Cargo();
		}
	
		Map<Integer,Boolean> disableShips = new HashMap<Integer,Boolean>();
		
		List<ItemCargoEntry> itemlist = availablecargo.getItemsWithEffect( ItemEffect.Type.DISABLE_SHIP );
		for( int i=0; i < itemlist.size(); i++ ) {
			IEDisableShip effect = (IEDisableShip)itemlist.get(i).getItemEffect();
			disableShips.put(effect.getShipType(), true);
		}
		
		itemlist = allyitems.getItemsWithEffect( ItemEffect.Type.DISABLE_SHIP );
		for( int i=0; i < itemlist.size(); i++ ) {
			IEDisableShip effect = (IEDisableShip)itemlist.get(i).getItemEffect();
			disableShips.put(effect.getShipType(), true);
		}
		
		SQLQuery shipdataRow = db.query(query);
		while( shipdataRow.next() ) {
			if( disableShips.containsKey(shipdataRow.getInt("type")) ) {
				continue;
			}
			if( !Rassen.get().rasse(user.getRace()).isMemberIn(shipdataRow.getInt("race")) ) {
				continue;
			}

			//Forschungen checken
			if( !user.hasResearched(shipdataRow.getInt("tr1")) || 
				!user.hasResearched(shipdataRow.getInt("tr2")) || 
				!user.hasResearched(shipdataRow.getInt("tr3"))) {
				continue;
			}
			
			SQLResultRow shipdata = shipdataRow.getRow();

			Cargo costs = new Cargo( Cargo.Type.STRING, shipdata.getString("costs") );
	
			// Kosten anpassen
			if( shipdata.getInt("linfactor") > 0 ) {
				int count = db.first("SELECT count(*) count FROM ships WHERE id>0 AND type=",shipdata.getInt("type")," AND owner=",user.getID()).getInt("count");
				int count2 = db.first("SELECT count(t1.id) count FROM werften t1 JOIN bases t2 ON t1.col=t2.id WHERE t1.building=",shipdata.getInt("type")," AND t2.owner=",user.getID()).getInt("count");
				int count3 = db.first("SELECT count(t1.id) count FROM werften t1 JOIN ships t2 ON t1.shipid=t2.id WHERE t2.id>0 AND t1.building=",shipdata.getInt("type")," AND t2.owner=",user.getID()).getInt("count");
	
				count = count + count2 + count3;
				
				costs.multiply( count*shipdata.getInt("linfactor")+1, Cargo.Round.NONE );
			}
	
			shipdata.put("costs", costs);
			shipdata.put("_item", false);
			result.add(shipdata);
		}
		shipdataRow.free();
	
		//Items
		Cargo localcargo = this.getCargo(true);
		itemlist = localcargo.getItemsWithEffect( ItemEffect.Type.DRAFT_SHIP );
		for( int i=0; i < itemlist.size(); i++ ) {
			ItemCargoEntry item = itemlist.get(i);
			IEDraftShip effect = (IEDraftShip)item.getItemEffect();
	
			boolean found = false;
			for( int j=0; j < effect.getWerftReqs().length; j++ ) {
				if( this.getWerftTag().indexOf(effect.getWerftReqs()[j])  > -1 ) {
					found = true;
					break;
				}
			}
			if( !found ) {
				continue;
			}

			if( !flagschiff && effect.isFlagschiff() ) {
				continue;
			}
	
			//Forschungen checken
			if(!user.hasResearched(effect.getTechReq(1)) || !user.hasResearched(effect.getTechReq(2)) || !user.hasResearched(effect.getTechReq(3))) {
				continue;
			}

			Cargo cost = effect.getBuildCosts();
			
			// TODO: Nicht schoen
			SQLResultRow shipdata = new SQLResultRow();
			shipdata.put("id", -1);
			shipdata.put("type", effect.getShipType());
			shipdata.put("costs", cost);
			shipdata.put("linfactor", 0);
			shipdata.put("crew", effect.getCrew());
			shipdata.put("dauer", effect.getDauer());
			shipdata.put("ekosten", effect.getE());
			shipdata.put("race", effect.getRace());
			shipdata.put("systemreq", effect.hasSystemReq());
			shipdata.put("tr1", effect.getTechReq(1));
			shipdata.put("tr2", effect.getTechReq(2));
			shipdata.put("tr3", effect.getTechReq(3));
			shipdata.put("werftreq", effect.getWerftReqs());
			shipdata.put("flagschiff", effect.isFlagschiff());
			shipdata.put("_item", new Object[] {"local", item.getResourceID()});
			
			result.add(shipdata);
		}
	
		itemlist = allyitems.getItemsWithEffect( ItemEffect.Type.DRAFT_SHIP );
		for( int i=0; i < itemlist.size(); i++ ) {
			ItemCargoEntry item = itemlist.get(i);
			IEDraftShip effect = (IEDraftShip)item.getItemEffect();
	
			boolean found = false;
			for( int j=0; j < effect.getWerftReqs().length; j++ ) {
				if( this.getWerftTag().indexOf(effect.getWerftReqs()[j])  > -1 ) {
					found = true;
					break;
				}
			}
			if( !found ) {
				continue;
			}

			if( !flagschiff && effect.isFlagschiff() ) {
				continue;
			}
	
			//Forschungen checken
			if(!user.hasResearched(effect.getTechReq(1)) || !user.hasResearched(effect.getTechReq(2)) || !user.hasResearched(effect.getTechReq(3))) {
				continue;
			}

			Cargo cost = effect.getBuildCosts();
			
			// TODO: Nicht schoen
			SQLResultRow shipdata = new SQLResultRow();
			shipdata.put("id", -1);
			shipdata.put("type", effect.getShipType());
			shipdata.put("costs", cost);
			shipdata.put("linfactor", 0);
			shipdata.put("crew", effect.getCrew());
			shipdata.put("dauer", effect.getDauer());
			shipdata.put("ekosten", effect.getE());
			shipdata.put("race", effect.getRace());
			shipdata.put("systemreq", effect.hasSystemReq());
			shipdata.put("tr1", effect.getTechReq(1));
			shipdata.put("tr2", effect.getTechReq(2));
			shipdata.put("tr3", effect.getTechReq(3));
			shipdata.put("werftreq", effect.getWerftReqs());
			shipdata.put("flagschiff", effect.isFlagschiff());
			shipdata.put("_item", new Object[] {"ally", item.getResourceID()});
			
			result.add(shipdata);
		}
		
		return result.toArray(new SQLResultRow[result.size()]);
	}
	
	/**
	 * Liefert die Schiffsbaudaten zu einer Kombination aus Schiffsbau-ID und/oder Item.
	 * Die Baukosten werden falls notwendig angepasst (linear ansteigende Kosten).
	 * Wenn keine passenden Schiffsbaudaten generiert werden koennen wird ein leeres
	 * Schiffsbaudatenarray zurueckgegeben. {@link DSObject#MESSAGE} enthaelt die Hinweistexte
	 * 
	 * @param build Die Schiffsbau-ID
	 * @param item Die Item-ID
	 * 
	 * @return schiffsbaudaten
	 */
	public SQLResultRow getShipBuildData( int build, int item ) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		User user = context.createUserObject(this.getOwner());
		
		Cargo allyitems = null;
	   	if( user.getAlly() > 0 ) {
			allyitems = new Cargo(Cargo.Type.ITEMSTRING,db.first("SELECT items FROM ally WHERE id=",user.getAlly()).getString("items"));
			Cargo localcargo = this.getCargo(true);
			
			allyitems.addCargo( localcargo );
		}
		else {
			Cargo localcargo = this.getCargo(true);
			allyitems = localcargo;
		}
	
	   	SQLResultRow shipdata = null;
		if( build > 0 ) {
			shipdata = db.first("SELECT type,costs,ekosten,crew,dauer,race,systemreq,tr1,tr2,tr3,werftreq,flagschiff,linfactor FROM ships_baubar WHERE id=",build);
			shipdata.put("costs", new Cargo(Cargo.Type.STRING, shipdata.getString("costs")));
			
			// Kosten anpassen
			if( shipdata.getInt("linfactor") > 0 ) {
				int count = db.first("SELECT count(*) count FROM ships WHERE id>0 AND type=",shipdata.getInt("type")," AND owner=",user.getID()).getInt("count");
				int count2 = db.first("SELECT count(t1.id) count FROM werften t1 JOIN bases t2 ON t1.col=t2.id WHERE t1.building=",shipdata.getInt("type")," AND t2.owner=",user.getID()).getInt("count");
				int count3 = db.first("SELECT count(t1.id) count FROM werften t1 JOIN ships t2 ON t1.shipid=t2.id WHERE t2.id>0 AND t1.building=",shipdata.getInt("type")," AND t2.owner=",user.getID()).getInt("count");
		
				count = count + count2 + count3;
				((Cargo)shipdata.get("costs")).multiply( shipdata.getInt("linfactor")*count+1, Cargo.Round.NONE );
			}
		}
		else {
			int itemcount = allyitems.getItem( item ).size();
			
			if( itemcount == 0 ) {
				MESSAGE.get().append("Kein passendes Item vorhanden");
				return null;
			}
	
			if( Items.get().item(item).getEffect().getType() != ItemEffect.Type.DRAFT_SHIP ) {
			 	MESSAGE.get().append("Bei dem Item handelt es sich um keinen Schiffsbauplan");
			 	return null;
			}
			IEDraftShip effect = (IEDraftShip)Items.get().item(item).getEffect();
			shipdata = new SQLResultRow();
			shipdata.put("type", effect.getShipType());
			shipdata.put("costs", effect.getBuildCosts());
			shipdata.put("linfactor", 0);
			shipdata.put("crew", effect.getCrew());
			shipdata.put("dauer", effect.getDauer());
			shipdata.put("ekosten", effect.getE());
			shipdata.put("race", effect.getRace());
			shipdata.put("systemreq", effect.hasSystemReq());
			shipdata.put("tr1", effect.getTechReq(1));
			shipdata.put("tr2", effect.getTechReq(2));
			shipdata.put("tr3", effect.getTechReq(3));
			shipdata.put("werftreq", effect.getWerftReqs());
			shipdata.put("flagschiff", effect.isFlagschiff());
		}
		
		return shipdata;
	}
	
	/**
	 * Baut ein Schiff in der Werft auf Basis der angegebenen Schiffbau-ID und der
	 * angegebenen Item-ID (Bauplan). {@link DSObject#MESSAGE} enthaelt die Hinweistexte
	 * 
	 * @param build Schiffbau-ID
	 * @param item Item-ID
	 * @param testonly Soll nur getestet (true) oder wirklich gebaut (false) werden?
	 * @return true, wenn kein Fehler aufgetreten ist
	 */
	public boolean buildShip( int build, int item, boolean testonly ) {
		StringBuilder output = MESSAGE.get();
		
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		User user = context.createUserObject(this.getOwner());
	
		Cargo basec = this.getCargo(false);
	   	Cargo newbasec = (Cargo)basec.clone();
	
	   	Cargo allyitems = null;
	   	if( user.getAlly() > 0 ) {
			allyitems = new Cargo(Cargo.Type.ITEMSTRING,db.first("SELECT items FROM ally WHERE id=",user.getAlly()).getString("items"));

			allyitems.addCargo( this.getCargo(true) );
		}
		else {
			allyitems = this.getCargo(true);
		}
	
		SQLResultRow shipdata = this.getShipBuildData( build, item );
		if( (shipdata == null) || shipdata.isEmpty() ) {
			return false;
		}
		
		if( shipdata.getInt("type") == 0 ) {
			output.append("Der angegebene (baubare) Schiffstyp existiert nicht");
			
			return false;
		}
	
		List<ItemCargoEntry> itemlist = allyitems.getItemsWithEffect( ItemEffect.Type.DISABLE_SHIP );
		for(int i=0; i < itemlist.size(); i++ ) {
			IEDisableShip effect = (IEDisableShip)itemlist.get(i).getItemEffect();
			
			if( effect.getShipType() == shipdata.getInt("type") ) {
				output.append("Ihnen wurde der Bau dieses Schiffs verboten");
				return false;
			}
		}
	
		//Kann die aktuelle Rasse das Schiff bauen?
		if( !Rassen.get().rasse(user.getRace()).isMemberIn(shipdata.getInt("race")) ) {
			output.append("Ihre Rasse kann dieses Schiff nicht bauen");		
			return false;
		}
	
		//Kann das Schiff im aktuellen System gebaut werden?
		if( shipdata.getBoolean("systemreq") && (!Systems.get().system(this.getSystem()).isMilitaryAllowed()) ) {
			output.append("Dieses Schiff l&auml;sst sich im aktuellen System nicht bauen");			
			return false;
		}
	
		//Verfuegt der Spieler ueber alle noetigen Forschungen?
		if( !user.hasResearched(shipdata.getInt("tr1")) || !user.hasResearched(shipdata.getInt("tr2")) || !user.hasResearched(shipdata.getInt("tr3")) ) {
			output.append("Sie besitzen nicht alle zum Bau n&ouml;tigen Technologien");		
			return false;
		}
	
		//Kann das Schiff in dieser Werft gebaut werden?
		String[] werftreq = null;
		if( shipdata.get("werftreq") instanceof String ) {
			werftreq = new String[] {shipdata.getString("werftreq")};
		}
		else {
			werftreq = (String[])shipdata.get("werftreq");
		}
		
		boolean found = false;
		for( int j=0; j < werftreq.length; j++ ) {
			if( werftreq[j].indexOf(this.getWerftTag()) > -1 ) {
				found = true;
				break;
			}
		}
		if( !found ) {
			output.append("Dieses Werft ist nicht gro&szlig; genug f&uuml;r das Schiff");
			return false;
		}
	
		if( shipdata.getBoolean("flagschiff") ) {
			boolean space = user.hasFlagschiffSpace();
			if( !space ) {
				output.append("Sie k&ouml;nnen lediglich ein Flagschiff besitzen");				
				return false;
			}
		}
	
		//Resourcenbedraft angeben
		boolean ok = true;
		
		Cargo shipdataCosts = (Cargo)shipdata.get("costs");
	
	   	//Standardresourcen
		ResourceList reslist = shipdataCosts.compare( basec, false );
		for( ResourceEntry res : reslist ) {
			if( res.getDiff() > 0 ) {
				ok = false;
				break;
			}
		}
		
		newbasec.substractCargo( shipdataCosts );
	
		int frei = this.getCrew();
	
		//E-Kosten
		if( shipdata.getInt("ekosten") > this.getEnergy()) {
			ok = false;
		}
		int e = this.getEnergy() - shipdata.getInt("ekosten");
	
		//Crew
		if (shipdata.getInt("crew") > frei) {
			ok = false;
		}

		frei -= shipdata.getInt("crew");
	
		if( !ok ) {
			output.append("Nicht genug Material verf&uuml;gbar");
			
			return false;
		}  
		else if( testonly ) {
			//Auf bestaetigung des "Auftrags" durch den Spieler warten
			
			return true;
		} 
		else {
			if( this.getOneWayFlag() == 0 ) {
				this.setCargo(newbasec, false);
				this.setEnergy(e);
				this.setCrew(frei);
	
			} 
			// TODO: Ab nach ShipWerft...
			else if( this.getWerftType() == SHIP ) {
				// Einweg-Werft-Code
				
				ShipWerft werft = (ShipWerft)this;
				
				SQLResultRow newtype = Ships.getShipType( this.getOneWayFlag(), false );
				int crew = db.first("SELECT crew FROM ships WHERE id>0 AND id=", werft.getShipID()).getInt("crew");
	
				db.update("DELETE FROM ships WHERE id>0 AND id=",werft.getShipID());
				
				String currentTime = Common.getIngameTime(context.get(ContextCommon.class).getTick());
				String history = "Baubeginn am "+currentTime+" durch "+user.getName()+" ("+user.getID()+"\n";
				
				db.update("INSERT INTO ships (id,name,type,hull,e,crew,x,y,system,owner,history) VALUES ",
							"(",werft.getShipID(),",\"Baustelle\",",this.getOneWayFlag(),",",newtype.getInt("hull"),",",newtype.getInt("eps"),",",crew,",",
							this.getX(),",",this.getY(),",",this.getSystem(),",",user.getID(),",'",db.prepareString(history),"')");
	
				db.update("UPDATE werften SET type=2 WHERE id=",this.getWerftID());
	
			} 
			else {
				output.append("WARNING: UNKNOWN OW_WERFT (possible: building) in buildShip@WerftObject.php");
				
				return false;
			}
	
			/*
				Werftauftrag einstellen
			*/
	
			this.building = shipdata.getInt("type");
			this.remaining = shipdata.getInt("dauer");
			
			String werftquery = "building="+shipdata.getInt("type")+",remaining="+shipdata.getInt("dauer");
			if( shipdata.getBoolean("flagschiff") ) {
				werftquery += ",flagschiff=1";
				this.buildFlagschiff = true;
			}
			if( build == -1 ) {
				werftquery += ",item="+item;
				this.buildItem = item;
			}
	
			db.update("UPDATE werften SET ",werftquery," WHERE id=",this.getWerftID());
	
			return true;
		}
	}
}

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

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.modules.Module;
import net.driftingsouls.ds2.server.cargo.modules.Modules;
import net.driftingsouls.ds2.server.config.IEModule;
import net.driftingsouls.ds2.server.config.Item;
import net.driftingsouls.ds2.server.config.ItemEffect;
import net.driftingsouls.ds2.server.config.Items;
import net.driftingsouls.ds2.server.config.ModuleSlots;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.ships.Ships.ModuleEntry;

/**
 * Die GUI einer Werft
 * @author bktheg
 *
 */
public class WerftGUI {
	private Context context;
	private TemplateEngine t;
	
	/**
	 * Erstellt eine neue Instanz einer Werftgui auf Basis des Kontexts
	 * @param context Der Kontext
	 * @param t Das zu verwendende TemplateEngine
	 */
	public WerftGUI( Context context,TemplateEngine t ) {
		this.context = context;
		this.t = t;
	}
	
	/**
	 * Generiert die Werft-GUI fuer das angegebene Werftobjekt
	 * @param werft Das Werftobjekt
	 * @return Die GUI als String
	 */
	public String execute( WerftObject werft ) {
		Database db = context.getDatabase();
	
		int build = context.getRequest().getParameterInt("build");
		String conf = context.getRequest().getParameterString("conf");
		int ws = context.getRequest().getParameterInt("ws");
		int item = context.getRequest().getParameterInt("item");
		
		if( !t.set_file( "_WERFT.WERFTGUI", "werft.werftgui.html" ) ) {
			context.addError("Konnte das Template-Engine nicht initialisieren");
			return "";
		}
		t.set_var(	"werftgui.formhidden",	werft.getFormHidden(),
					"werftgui.urlbase",		werft.getUrlBase() );
		
		if( build != 0 ) {
			if( werft.isBuilding() ) {
				t.set_var("werftgui.msg", "Sie k&ouml;nnen nur ein Schiff zur selben Zeit bauen");
			} 
			else {
				this.out_buildShip(build, item, werft, conf);
			}
		}
		else if( ws != 0 ) {
			if( werft.isBuilding() ) {
				t.set_var("werftgui.msg", "Sie k&ouml;nnen nicht gleichzeitig ein Schiff der Werkstatt bearbeiten und eines bauen");
			}
			else {
				this.out_ws(werft, ws);
			}
		}
		else if( werft.isBuilding() ) {
			this.out_werftbuilding( werft, conf );
		} 
		else if( !werft.isBuilding() ) {
			//Resourcenliste
			this.out_ResourceList( werft );
		
			//Schiffsliste
			this.out_buildShipList( werft );
		
			this.out_wsShipList(werft);
		
			// Verbindung Base <-> Werft
			if( werft.getWerftType() == WerftObject.SHIP ) {
				int shipid = ((ShipWerft)werft).getShipID();
				int linkedbaseID = ((ShipWerft)werft).getLinkedBase();
				
				SQLResultRow shiptype = Ships.getShipType( shipid, true );
				if( shiptype.getInt("cost") == 0 ) {
					t.set_block("_WERFT.WERFTGUI", "werftgui.linkedbase.listitem", "werftgui.linkedbase.list");
		
					t.set_var(	"linkedbase.selected",	linkedbaseID == 0,
								"linkedbase.value",		"-1",
								"linkedbase.name",		(linkedbaseID != 0 ? "kein " : "")+"Ziel" );
										
					t.parse("werftgui.linkedbase.list", "werftgui.linkedbase.listitem", true);
		
					SQLQuery base = db.query("SELECT id,name FROM bases ",
								"WHERE x=",werft.getX()," AND y=",werft.getY()," AND system=",werft.getSystem()," AND owner=",werft.getOwner()," ORDER BY id");
					while( base.next() ) {
						t.set_var(	"linkedbase.selected",	(linkedbaseID == base.getInt("id")),
									"linkedbase.value",		base.getInt("id"),
									"linkedbase.name",		base.getString("name")+" ("+base.getInt("id")+")" );
						t.parse("werftgui.linkedbase.list", "werftgui.linkedbase.listitem", true);
					}
					base.free();
				}
			}
		}
		t.parse( "OUT", "_WERFT.WERFTGUI" );	
		return t.getVar("OUT");
	}

	private void out_wsShipList(WerftObject werft) {
		Database db = context.getDatabase();

		t.set_var("werftgui.wsshiplist", 1);
		t.set_block("_WERFT.WERFTGUI", "wsshiplist.listitem", "wsshiplist.list");
		
		SQLQuery ship = db.query("SELECT t1.id,t1.name,t1.type,t1.status,t1.engine,t1.sensors,t1.comm,t1.weapons,t1.hull,t1.owner,t2.name AS ownername,t2.id AS userid ",
								"FROM ships t1 JOIN users t2 ON t1.owner=t2.id ",
								"WHERE t1.id>0 AND t1.x BETWEEN ",(werft.getX()-werft.getSize())," AND ",(werft.getX()+werft.getSize())," AND t1.y BETWEEN ",(werft.getY()-werft.getSize())," AND ",(werft.getY()+werft.getSize())," AND t1.system=",werft.getSystem()," AND !LOCATE('l ',t1.docked) AND t1.battle=0 ORDER BY t2.id,t1.id");
	
		while( ship.next() ) {
			if( (werft.getWerftType() == WerftObject.SHIP) && (((ShipWerft)werft).getShipID() == ship.getInt("id")) ) {
				continue;	
			}
			
			SQLResultRow shiptype = Ships.getShipType( ship.getRow() );
			
			if( (ship.getInt("hull") < shiptype.getInt("hull")) || (ship.getInt("engine") < 100) ||
				(ship.getInt("sensors") < 100) || (ship.getInt("comm") < 100) || (ship.getInt("weapons") < 100) ) {
				t.set_var("ship.needsrepair", 1);
			}
			else {
				t.set_var("ship.needsrepair", 0);
			}
			
			String ownername = Common._title(ship.getString("ownername"));
			if( ownername.length() == 0 ) {
				ownername = "Unbekannter Spieler ("+ship.getInt("owner")+")"; 
			}
						
			t.set_var(	"ship.id",			ship.getInt("id"),
						"ship.name",		ship.getString("name"),
						"ship.owner.name",	ownername );
								
			t.parse("wsshiplist.list", "wsshiplist.listitem", true);
		}
		ship.free();
	}

	private void out_buildShipList(WerftObject werft) {		
		t.set_var("werftgui.buildshiplist", 1);
		t.set_block("_WERFT.WERFTGUI", "buildshiplist.listitem", "buildshiplist.list");
		t.set_block("buildshiplist.listitem", "buildship.res.listitem", "buildship.res.list");
		
		SQLResultRow[] shipdata = werft.getBuildShipList();

		Cargo availablecargo = werft.getCargo(false);
	
		int energy = werft.getEnergy();
		int crew = werft.getCrew();
		
		for( int i=0; i < shipdata.length; i++ ) {
			SQLResultRow ashipdata = shipdata[i];
			t.start_record();
			
			Cargo costs = new Cargo();
			costs.addCargo((Cargo)ashipdata.get("costs"));
			costs.setOption( Cargo.Option.SHOWMASS, false );
	
			if( !(ashipdata.get("_item") instanceof Boolean) ) {
				Object[] data = (Object[])ashipdata.get("_item");
				ResourceID itemdata = (ResourceID)data[1];
				
				t.set_var(	"buildship.item.id",	itemdata.getItemID(),
							"buildship.item.color",	(data[0].toString().equals("local") ? "#EECC44" : "#44EE44"),
							"buildship.item.uses",	itemdata.getUses() );
			}
			
			SQLResultRow tmptype = Ships.getShipType( ashipdata.getInt("type"), false );
			
			t.set_var(	"res.image",		Configuration.getSetting("URL")+"data/interface/time.gif",
						"res.count",		ashipdata.getInt("dauer"),
						"res.plainname",	"Dauer",
						"res.mangel",		0 );
			t.parse("buildship.res.list", "buildship.res.listitem", false);
				
			ResourceList reslist = costs.compare( availablecargo, false );
			for( ResourceEntry res : reslist ) {
				t.set_var(	"res.image",		res.getImage(),
							"res.count",		res.getCargo1(),
							"res.plainname",	res.getPlainName(),
							"res.mangel",		res.getDiff() > 0 );
				t.parse("buildship.res.list", "buildship.res.listitem", true);
			}

			t.set_var(	"res.image",		Configuration.getSetting("URL")+"data/interface/energie.gif",
						"res.count",		ashipdata.getInt("ekosten"),
						"res.plainname",	"Energie",
						"res.mangel",		energy < ashipdata.getInt("ekosten") );
			t.parse("buildship.res.list", "buildship.res.listitem", true);
	
			t.set_var(	"res.image",		Configuration.getSetting("URL")+"data/interface/besatzung.gif",
						"res.count",		ashipdata.getInt("crew"),
						"res.plainname",	"Besatzung",
						"res.mangel",		crew < ashipdata.getInt("crew") );
			t.parse("buildship.res.list", "buildship.res.listitem", true);

			t.set_var(	"buildship.id",			ashipdata.getInt("id"),
						"buildship.type.id",	ashipdata.getInt("type"),
						"buildship.flagschiff",	ashipdata.getBoolean("flagschiff"),
						"buildship.type.name",	tmptype.getString("nickname") );
			t.parse("buildshiplist.list", "buildshiplist.listitem", true);
			
			t.stop_record();
			t.clear_record();
		}
	}

	private void out_ResourceList(WerftObject werft) {		
		t.set_var("werftgui.reslist", 1);
		t.set_block("_WERFT.WERFTGUI", "reslist.res.listitem", "reslist.res.list");
		
		Cargo cargo = werft.getCargo(false);
		int frei = werft.getCrew();
	
		ResourceList reslist = cargo.getResourceList();
		for( ResourceEntry res : reslist ) {
			t.set_var(	"res.image",		res.getImage(),
						"res.plainname",	res.getPlainName(),
						"res.cargo",		res.getCargo1() );
			t.parse("reslist.res.list", "reslist.res.listitem", true);
		}
		t.set_var(	"res.image",		Configuration.getSetting("URL")+"data/interface/energie.gif",
					"res.plainname",	"Energie",
					"res.cargo",		werft.getEnergy() );
		t.parse("reslist.res.list", "reslist.res.listitem", true);
		
		t.set_var(	"res.image",		Configuration.getSetting("URL")+"data/interface/arbeitslos.gif",
					"res.plainname",	"Crew",
					"res.cargo",		frei );
		t.parse("reslist.res.list", "reslist.res.listitem", true);
	}

	private void out_werftbuilding(WerftObject werft, String conf) {
		String action = context.getRequest().getParameterString("werftact");
			
		SQLResultRow type = werft.getBuildShipType();

		if( action.equals("canclebuild") && conf.equals("ok") ) {
			t.set_var( "werftgui.building.cancel", 1 );
			werft.cancelBuild();
			return;
		}
		
		t.set_var(	"werftgui.building",		1,
					"ship.type.image",			type.getString("picture"),
					"ship.type.id",				type.getInt("id"),
					"ship.type.name",			type.getString("nickname"),
					"ship.build.remaining",		werft.getRemainingTime(),
					"ship.build.item",			werft.getRequiredItem(),
					"werftgui.building.cancel.conf",	action.equals("canclebuild") && conf.equals("ok") );

		if( werft.getRequiredItem() > -1 ) {
			t.set_var(	"ship.build.item.picture",		Items.get().item(werft.getRequiredItem()).getPicture(),
						"ship.build.item.name",			Items.get().item(werft.getRequiredItem()).getName(),
						"ship.build.item.available",	werft.isBuildContPossible() );
		}
	}

	private void out_ws(WerftObject werft, int ws) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();

		User user = context.getActiveUser();
		String sess = context.getSession();
		
		SQLResultRow ship = db.first("SELECT * FROM ships WHERE id>0 AND id=",ws);
		if( ship.isEmpty() ) {
			context.addError("Das angegebene Schiff existiert nicht", werft.getUrlBase()+"&amp;sess="+sess);		
			return;
		}
		if( (werft.getWerftType() == WerftObject.SHIP) && (((ShipWerft)werft).getShipID() == ws) ) {
			context.addError("Sie k&ouml;nnen sich nicht selbst reparieren", werft.getUrlBase()+"&amp;sess="+sess);			
			return;
		}
		if( !Location.fromResult(ship).sameSector(0, new Location(werft.getSystem(), werft.getX(), werft.getY()), werft.getSize()) ) {
			context.addError("Das Schiff befindet sich nicht im selben Sektor wie die Werft", werft.getUrlBase()+"&amp;sess="+sess);		
			return;
		}
		
		if( ship.getInt("battle") != 0 ) {
			context.addError("Das Schiff befindet sich in einer Schlacht", werft.getUrlBase()+"&amp;sess="+sess);			
			return;
		}
		
		SQLResultRow shipType = Ships.getShipType(ship);
		
		t.set_var(	"werftgui.ws",			1,
					"ship.id",				ship.getInt("id"),
					"ship.name",			ship.getString("name"),
					"ship.own",				(ship.getInt("owner") == user.getID()),
					"ship.owner.id",		ship.getInt("owner"),
					"ship.type.modules",	shipType.getString("modules") );
	
		if( ship.getInt("owner") != user.getID() ) {
			User owner = context.createUserObject(ship.getInt("owner"));
			t.set_var("ship.owner.name", Common._title(owner.getName()));
		}

		String action = context.getRequest().getParameterString("werftact");
		if( action.equals("repair") ) {
			String conf = context.getRequest().getParameterString("conf");
			
			this.out_repairShip( ship.getInt("id"), werft, conf );
		}
		else if( action.equals("dismantle") ) {
			String conf = context.getRequest().getParameterString("conf");
			
			this.out_dismantleShip( ship.getInt("id"), werft, conf );
		}
		else if( action.equals("module") ) {
			this.out_moduleShip( ship.getInt("id"), werft );
		}
		else {
			this.out_ws_info( ship );
		}
	}
	
	private String genSubColor( int value, int defvalue ) {
		if( defvalue == 0 ) {
			return "green";	
		}
		
		if( value < defvalue/2 ) {
			return "red";
		} 
		else if( value < defvalue ) {
			return "yellow";
		} 
		else {
			return "green";
		}
	}

	private void out_ws_info(SQLResultRow ship) {
		SQLResultRow shipType = Ships.getShipType(ship);
		
		t.set_var(	"werftgui.ws.showinfo",		1,
					"ship.name",				ship.getString("name"),
					"ship.type.id",				shipType.getInt("id"),
					"ship.type.picture",		shipType.getString("picture"),
					"ship.id",					ship.getInt("id"),
					"ship.hull",				ship.getInt("hull"),
					"ship.type.hull",			shipType.getInt("hull"),
					"ship.hull.color",			this.genSubColor(ship.getInt("hull"), shipType.getInt("hull")),
					"ship.panzerung",			Math.round(shipType.getInt("panzerung")*(double)ship.getInt("hull")/shipType.getInt("hull")),
					"ship.shields",				ship.getInt("shields"),
					"ship.type.shields",		shipType.getInt("shields"),
					"ship.shields.color",		this.genSubColor(ship.getInt("shields"), shipType.getInt("shields")),
					"ship.engine",				ship.getInt("engine"),
					"ship.type.engine",			( shipType.getInt("cost") > 0 ? 100 : 0 ),
					"ship.engine.color",		this.genSubColor(ship.getInt("engine"), 100),
					"ship.weapons",				ship.getInt("engine"),
					"ship.type.weapons",		( shipType.getString("weapons").indexOf("=") > -1 ? 100 : 0 ),
					"ship.weapons.color",		this.genSubColor(ship.getInt("weapons"), 100),
					"ship.comm",				ship.getInt("comm"),
					"ship.type.comm",			100,
					"ship.comm.color",			this.genSubColor(ship.getInt("comm"), 100),
					"ship.sensors",				ship.getInt("sensors"),
					"ship.type.sensors",		100,
					"ship.sensors.color",		this.genSubColor(ship.getInt("sensors"), 100),
					"ship.crew",				ship.getInt("crew"),
					"ship.type.crew",			shipType.getInt("crew"),
					"ship.crew.color",			this.genSubColor(ship.getInt("crew"), shipType.getInt("crew")),
					"ship.e",					ship.getInt("e"),
					"ship.type.e",				shipType.getInt("eps"),
					"ship.e.color",				this.genSubColor(ship.getInt("e"), shipType.getInt("eps")),
					"ship.heat",				ship.getInt("s") );
	
		Offizier offizier = Offizier.getOffizierByDest('s', ship.getInt("id"));
		if( offizier != null ) {
			t.set_var(	"ship.offizier.id",			offizier.getID(),
						"ship.offizier.rang",		offizier.getRang(),
						"ship.offizier.picture",	offizier.getPicture(),
						"ship.offizier.name",		Common._plaintitle(offizier.getName()) );
		}	
	}

	private void out_moduleShip(int shipID, WerftObject werft) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		String sess = context.getSession();
		User user = context.getActiveUser();
		
		int item = context.getRequest().getParameterInt("item");
		int slot = context.getRequest().getParameterInt("slot");
		String moduleaction = context.getRequest().getParameterString("moduleaction");
	
		SQLResultRow ship = db.first("SELECT id,x,y,system,owner,battle,type,status FROM ships WHERE id>0 AND id=",shipID);
	
		//Gehoert das Schiff dem User?
		if( (ship.getInt("owner") != user.getID()) && (user.getAccessLevel() < 100)) {
			context.addError("Das Schiff geh&ouml;rt ihnen nicht", werft.getUrlBase()+"&amp;sess="+sess);
			return;
		}
		
		SQLResultRow shiptype = Ships.getShipType( ship.getInt("type"), false );

		List<String[]> moduleslots = new ArrayList<String[]>();
		String[] mslots = StringUtils.split(shiptype.getString("modules"), ';');
		for( int i=0; i < mslots.length; i++ ) {
			String[] data = StringUtils.split(mslots[i], ':');
			moduleslots.add(data);	
		}
		
		t.set_block("_WERFT.WERFTGUI", "ws.modules.slots.listitem", "ws.modules.slots.list");
		t.set_block("ws.modules.slots.listitem", "slot.items.listitem", "slot.items.list");
		
		t.set_var(	"werftgui.ws.modules",	1,
					"ship.type.image",		shiptype.getString("picture") );
			
		// Modul einbauen
		if( (item != 0) && (slot != 0) && (Items.get().item(item) != null) ) {
			werft.addModule( ship, slot, item );
			t.set_var("ws.modules.msg", Common._plaintext(werft.MESSAGE.getMessage()));
		}
		else if( moduleaction.equals("ausbauen") && (slot != 0) ) {
			werft.removeModule( ship, slot );
			t.set_var("ws.modules.msg", Common._plaintext(werft.MESSAGE.getMessage()));
		}
		
		ModuleEntry[] modules = Ships.getModules(ship);
		Map<Integer,Integer> usedslots = new HashMap<Integer,Integer>();
		
		for( int i=0; i < modules.length; i++ ) {
			usedslots.put(modules[i].slot, i);
		}
		
		Cargo cargo = werft.getCargo(false);
		List<ItemCargoEntry> itemlist = cargo.getItemsWithEffect( ItemEffect.Type.MODULE );
	
		// Slots (Mit Belegung) ausgeben
		for( int i=0; i < moduleslots.size(); i++ ) {
			String[] aslot = moduleslots.get(i);
			
			t.set_var(	"slot.name",		ModuleSlots.get().slot(aslot[1]).getName(),
						"slot.empty",		!usedslots.containsKey(Integer.parseInt(aslot[0])),
						"slot.id",			aslot[0],
						"slot.items.list",	"" );
								
			if( usedslots.containsKey(Integer.parseInt(aslot[0])) ) {
				ModuleEntry module = modules[usedslots.get(Integer.parseInt(aslot[0]))];
				Module moduleobj = Modules.getShipModule( module );
				moduleobj.setSlotData(aslot[2]);
				
				t.set_var( "slot.module.name", moduleobj.getName() );
			}	
			else {
				for( int j=0; j < itemlist.size(); j++ ) {
					IEModule effect = (IEModule)itemlist.get(j).getItemEffect();
					if( !ModuleSlots.get().slot(aslot[1]).isMemberIn(effect.getSlots()) ) {
						continue;	
					}
					if( Items.get().item(itemlist.get(j).getItemID()).getAccessLevel() > user.getAccessLevel() ) {
						continue;
					}
					
					Item itemobj = itemlist.get(j).getItemObject();
					t.set_var(	"item.id",		itemlist.get(j).getItemID(),
								"item.name",	itemobj.getName() );

					t.parse("slot.items.list", "slot.items.listitem", true);
				}
			}
			t.parse("ws.modules.slots.list", "ws.modules.slots.listitem", true);
		}
	}

	private void out_dismantleShip(int dismantle, WerftObject werft, String conf) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		String sess = context.getSession();
		User user = context.getActiveUser();
		
		SQLResultRow ship = db.first("SELECT * FROM ships WHERE id>0 AND id=",dismantle);
		
		//Gehoert das Schiff dem User?
		if( ship.getInt("owner") != user.getID() ) {
			context.addError("Das Schiff geh&ouml;rt ihnen nicht", werft.getUrlBase()+"&amp;sess="+sess);
			return;
		}
		
		SQLResultRow shiptype = Ships.getShipType( ship );
		Cargo scargo = new Cargo( Cargo.Type.STRING, ship.getString("cargo") );
		
		t.set_block("_WERFT.WERFTGUI", "ws.dismantle.res.listitem", "ws.dismantle.res.list");
		
		t.set_var(	"werftgui.ws.dismantle",	1,
					"ship.type.image",			shiptype.getString("picture"),
					"ws.dismantle.conf",		!conf.equals("ok") );

		//Gewinn ausgeben
		Cargo cost = werft.getDismantleCargo(ship);
		
		ResourceList reslist = cost.getResourceList();
		for( ResourceEntry res : reslist ) {
			t.set_var(	"res.image",		res.getImage(),
						"res.cargo",		res.getCargo1(),
						"res.plainname",	res.getPlainName() );
			t.parse("ws.dismantle.res.list", "ws.dismantle.res.listitem", true);
		}
		
		//Waren im Laderaum ausgeben	
		reslist = scargo.getResourceList();
		for( ResourceEntry res : reslist ) {
			t.set_var(	"res.image",		res.getImage(),
						"res.cargo",		res.getCargo1(),
						"res.plainname",	res.getPlainName() );
			t.parse("ws.dismantle.available.list", "ws.dismantle.res.listitem", true);
		}

		boolean ok = werft.dismantleShip(dismantle, !conf.equals("ok"));
		if( !ok ) {
			t.set_var("ws.dismantle.error", werft.MESSAGE.getMessage() );
		}
		else {
			String msg = werft.MESSAGE.getMessage();
			if( msg.length() > 0 ) {
				t.set_var("ws.dismantle.msg", msg);
			}
		}
	}

	private void out_repairShip(int repair, WerftObject werft, String conf) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		SQLResultRow ship = db.first("SELECT * FROM ships WHERE id>0 AND id=",repair);
		
		SQLResultRow shiptype = Ships.getShipType( ship );

		t.set_block("_WERFT.WERFTGUI", "ws.repair.res.listitem", "ws.repair.res.list");

		t.set_var(	"ship.type.image",		shiptype.getString("picture"),
					"werftgui.ws.repair",	1,
					"ws.repair.conf",		!conf.equals("ok") );
		
		Cargo cargo = werft.getCargo(false);
	
		WerftObject.RepairCosts repairCost = werft.getRepairCosts(ship);
	
		//Kosten ausgeben
		ResourceList reslist = repairCost.cost.compare( cargo, false );
		for( ResourceEntry res : reslist ) {
			t.set_var(	"res.image",			res.getImage(),
						"res.plainname",		res.getPlainName(),
						"res.cargo.needed",		res.getCargo1(),
						"res.cargo.available",	res.getCargo2() );
			t.parse("ws.repair.res.list", "ws.repair.res.listitem", true);
		}
		
		if( repairCost.e > 0 ) {
			t.set_var(	"res.image",			Configuration.getSetting("URL")+"data/interface/energie.gif",
						"res.plainname",		"Energie",
						"res.cargo.needed",		repairCost.e,
						"res.cargo.available",	werft.getEnergy() );
			t.parse("ws.repair.res.list", "ws.repair.res.listitem", true);
		}
	
		boolean ok = werft.repairShip(ship, !conf.equals("ok"));
		
		if( !ok ) {
			t.set_var("ws.repair.error", werft.MESSAGE.getMessage());
		}
		else {
			String msg = werft.MESSAGE.getMessage();
			if( msg.length() > 0 ) {
				t.set_var("ws.repair.message", msg);
			}
		}
	}

	private void out_buildShip(int build, int item, WerftObject werft, String conf) {	
		Cargo cargo = werft.getCargo(false);
	
		SQLResultRow shipdata = werft.getShipBuildData( build, item );
		if( (shipdata == null) || shipdata.isEmpty() ) {
			t.set_var("werftgui.msg", "<span style=\"color:red\">"+werft.MESSAGE.getMessage()+"</span>");
			return;
		}
		
		SQLResultRow shiptype = Ships.getShipType( shipdata.getInt("type"), false );
		
		t.set_block("_WERFT.WERFTGUI", "build.res.listitem", "build.res.list");
		
		t.set_var(	"werftgui.build",	1,
					"build.type.name",	shiptype.getString("nickname"),
					"build.type.image",	shiptype.getString("picture"),
					"build.conf",		!conf.equals("ok"),
					"build.id",			build,
					"build.item.id",	item );
	
		//Resourcenbedraft angeben
		
	   	//Standardresourcen
		Cargo shipdataCosts = (Cargo)shipdata.get("costs");
		ResourceList reslist = shipdataCosts.compare( cargo, false );
		for( ResourceEntry res : reslist ) {
			t.set_var(	"res.image",			res.getImage(),
						"res.plainname",		res.getPlainName(),
						"res.cargo.available",	res.getCargo2(),
						"res.cargo.needed",		res.getCargo1(),
						"res.cargo.mangel",		res.getDiff() > 0 ? res.getDiff() : 0 );
			t.parse("build.res.list", "build.res.listitem", true);
		}
		
		int frei = werft.getCrew();
	
		//E-Kosten
		t.set_var(	"res.image",		Configuration.getSetting("URL")+"data/interface/energie.gif",
					"res.plainname",	"Energie",
					"res.cargo.available",	werft.getEnergy(),
					"res.cargo.needed",	shipdata.getInt("ekosten"),
					"res.cargo.mangel",	(shipdata.getInt("ekosten") > werft.getEnergy() ? shipdata.getInt("ekosten") - werft.getEnergy() : 0) );
		t.parse("build.othercosts.list", "build.res.listitem", true);

		//Crew
		t.set_var(	"res.image",			Configuration.getSetting("URL")+"data/interface/arbeitslos.gif",
					"res.plainname",		"Crew",
					"res.cargo.available",	frei,
					"res.cargo.needed",		shipdata.getInt("crew"),
					"res.cargo.mangel",		(shipdata.getInt("crew") > frei ? shipdata.getInt("crew") - frei : 0));
		t.parse("build.othercosts.list", "build.res.listitem", true);
		
		boolean result = werft.buildShip(build, item, !conf.equals("ok") );
	
		if( !result ) {
			t.set_var("build.error", StringUtils.replace(werft.MESSAGE.getMessage(), "\n", "<br/>\n"));
		}  
		
		return;   
	}
}

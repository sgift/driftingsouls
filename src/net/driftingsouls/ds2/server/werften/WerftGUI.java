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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.modules.Module;
import net.driftingsouls.ds2.server.cargo.modules.Modules;
import net.driftingsouls.ds2.server.config.ModuleSlots;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.Items;
import net.driftingsouls.ds2.server.config.items.effects.IEModule;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Die GUI einer Werft.
 * @author bktheg
 *
 */
@Configurable
public class WerftGUI {
	private Context context;
	private TemplateEngine t;
	
	private Configuration config;
	
    /**
     * Injiziert die DS-Konfiguration.
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config) 
    {
    	this.config = config;
    }
	
	/**
	 * Erstellt eine neue Instanz einer Werftgui auf Basis des Kontexts.
	 * @param context Der Kontext
	 * @param t Das zu verwendende TemplateEngine
	 */
	public WerftGUI( Context context,TemplateEngine t ) {
		this.context = context;
		this.t = t;
	}
	
	/**
	 * Generiert die Werft-GUI fuer das angegebene Werftobjekt.
	 * @param werft Das Werftobjekt
	 * @return Die GUI als String
	 */
	public String execute( WerftObject werft ) {
		int build = context.getRequest().getParameterInt("build");
		int ws = context.getRequest().getParameterInt("ws");
		int linkedwerft = context.getRequest().getParameterInt("linkedwerft");
		
		if( !t.setFile( "_WERFT.WERFTGUI", "werft.werftgui.html" ) ) {
			context.addError("Konnte das Template-Engine nicht initialisieren");
			return "";
		}
		
		// Werften aneinanderkoppelt
		if( linkedwerft != 0 && werft.isLinkableWerft() ) {
			org.hibernate.Session db = context.getDB();
			
			if( werft.getKomplex() != null ) {
				werft.removeFromKomplex();
			}
			
			if( linkedwerft > 0 ) {
				WerftObject targetwerft = (WerftObject)db.get(WerftObject.class, linkedwerft);
				
				if( targetwerft == null || !targetwerft.isLinkableWerft() ) {
					t.setVar("werftgui.msg", "Die Zielwerft ist ungueltigt");
				}
				else {
					if( targetwerft.getKomplex() != null ) {
						werft.addToKomplex(targetwerft.getKomplex());
					}
					else {
						werft.createKomplexWithWerft(targetwerft);
					}
				}
			}
		}
		
		final String action = context.getRequest().getParameterString("werftact");
		if( action.equals("removefromkomplex") ) {
			org.hibernate.Session db = context.getDB();
			
			WerftObject obj = (WerftObject)db.get(WerftObject.class, context.getRequest().getParameterInt("entry"));
			
			if( (obj != null) && obj.getKomplex() != null ) {
				obj.removeFromKomplex();
			}
		}
		
		if( werft.getKomplex() != null ) {
			werft = werft.getKomplex();
		}
		
		t.setVar(	"werftgui.formhidden",	werft.getFormHidden(),
				"werftgui.urlbase",		werft.getUrlBase() );
		
		// Baudialog
		if( build != 0 ) {			
			this.out_buildShip(build, werft);
		}
		// Werkstadt
		else if( ws != 0 ) 
		{
			this.out_ws(werft, ws);
		}
		// Hauptseite
		else {
			String show = context.getRequest().getParameterString("show");
			if( show.length() == 0 ) {
				show = "build";
			}
			
			t.setVar("werftgui.main", 1);
			
			WerftQueueEntry[] queue = werft.getBuildQueue();
			t.setVar(
					"werftgui.name",	werft.getWerftName(),
					"werftgui.picture",	werft.getWerftPicture(),
					"werftgui.crew",	werft.getCrew(),
					"werftgui.werftslots",	werft.getWerftSlots(),
					"werftgui.totalqueueentries",	queue.length
					);
			
			// Resourcenliste
			SQLResultRow[] shipdata = werft.getBuildShipList();
			
			Cargo costs = new Cargo();
			for( int i=0; i < shipdata.length; i++ ) {
				costs.addCargo((Cargo)shipdata[i].get("costs"));
			}
			
			this.out_ResourceList( werft, costs );
			
			if( "build".equals(show) ) {
				t.setVar("werftgui.main.build", 1);
				
				this.out_buildShipList( werft, shipdata );
			}
			else if( "repair".equals(show) ) {
				t.setVar("werftgui.main.repair", 1);
				
				this.out_wsShipList(werft);
			}
			else if( "queue".equals(show) ) {
				t.setVar("werftgui.main.queue", 1);
				
				final int position = context.getRequest().getParameterInt("entry");
				
				if( action.equals("canclebuild") ) {
					WerftQueueEntry entry = werft.getBuildQueueEntry(position);
					if( entry != null ) {
						t.setVar( "werftgui.building.cancel", 1 );
					
						werft.cancelBuild(entry);
					}
				}
				else if( action.equals("queuedown") ) {
					WerftQueueEntry entry = werft.getBuildQueueEntry(position);
					WerftQueueEntry entry2 = werft.getBuildQueueEntry(position+1);
					if( (entry != null) && (entry2 != null) ) {
						werft.swapQueueEntries(entry, entry2);
					}
				}
				else if( action.equals("queueup") ) {
					WerftQueueEntry entry = werft.getBuildQueueEntry(position);
					WerftQueueEntry entry2 = werft.getBuildQueueEntry(position-1);
					if( (entry != null) && (entry2 != null) ) {
						werft.swapQueueEntries(entry, entry2);
					}
				}
				
				if( !action.isEmpty() ) {
					queue = werft.getBuildQueue();
				}
				
				out_queueShipList(werft, queue);
			}
			else if( "options".equals(show) ) {
				t.setVar("werftgui.main.options", 1);
				
				outWerftOptions(werft);
			}
		}
		t.parse( "OUT", "_WERFT.WERFTGUI" );	
		return t.getVar("OUT");
	}

	private void outWerftOptions(WerftObject werft) {
		org.hibernate.Session db = context.getDB();
		
		if( werft instanceof WerftKomplex ) {
			t.setBlock("_WERFT.WERFTGUI", "werftgui.komplexparts.listitem", "werftgui.komplexparts.list");
			
			final WerftObject[] members = ((WerftKomplex)werft).getMembers();
			for( int i=0; i < members.length; i++ ) {
				final WerftObject member = members[i];
				
				t.setVar(
						"komplexpart.type.image",	member.getWerftPicture(),
						"komplexpart.name",	member.getWerftName(),
						"komplexpart.url",	member.getObjectUrl(),
						"komplexpart.id",	member.getWerftID(),
						"komplexpart.werftgui.formhidden",	member.getFormHidden(),
						"komplexpart.linkedbase.list",	"");
				
				if( member instanceof ShipWerft ) {
					Ship ship = ((ShipWerft)member).getShip();
					Base linkedbase = ((ShipWerft)member).getLinkedBase();
					
					ShipTypeData shiptype = ship.getTypeData();
					if( shiptype.getCost() == 0 ) {
						t.setBlock("werftgui.komplexparts.listitem", "komplexpart.linkedbase.listitem", "komplexpart.linkedbase.list");
						
						t.setVar(	"komplexpart.linkedbase.selected",	linkedbase == null,
									"komplexpart.linkedbase.value",		"-1",
									"komplexpart.linkedbase.name",		"kein Ziel" );
							
						t.parse("komplexpart.linkedbase.list", "komplexpart.linkedbase.listitem", true);

						List<?> bases = db.createQuery("from Base " +
									"where x=? and y=? and system=? and owner=? order by id")
									.setInteger(0, member.getX())
									.setInteger(1, member.getY())
									.setInteger(2, member.getSystem())
									.setEntity(3, member.getOwner())
									.list();
						for( Iterator<?> iter=bases.iterator(); iter.hasNext(); ) {
							Base base = (Base)iter.next();
							
							t.setVar(	"komplexpart.linkedbase.selected",	linkedbase == base,
										"komplexpart.linkedbase.value",		base.getId(),
										"komplexpart.linkedbase.name",		base.getName()+" ("+base.getId()+")" );
							t.parse("komplexpart.linkedbase.list", "komplexpart.linkedbase.listitem", true);
						}
					}
				}
				
				t.parse("werftgui.komplexparts.list", "werftgui.komplexparts.listitem", true);
			}

			return;
		}
		
		if( werft.isLinkableWerft() ) {
			t.setBlock("_WERFT.WERFTGUI", "werftgui.linkedwerft.listitem", "werftgui.linkedwerft.list");
					
			// Hier wird davon ausgegangen, dass nur Schiffswerften Werftkomplexe bilden
			List<?> werften = db.createQuery("from ShipWerft " +
				"where ship.x=? and ship.y=? and ship.system=? and ship.owner=? order by ship.id")
				.setInteger(0, werft.getX())
				.setInteger(1, werft.getY())
				.setInteger(2, werft.getSystem())
				.setEntity(3, werft.getOwner())
				.list();
			for( Iterator<?> iter=werften.iterator(); iter.hasNext(); ) {
				ShipWerft shipwerft = (ShipWerft)iter.next();
				
				if( shipwerft == werft ) {
					continue;
				}
				if( !shipwerft.isLinkableWerft() ) {
					continue;
				}
				
				t.setVar(
						"linkedwerft.value",	shipwerft.getWerftID(),
						"linkedwerft.name",		shipwerft.getName()+" ("+shipwerft.getShip().getId()+")" );
				t.parse("werftgui.linkedwerft.list", "werftgui.linkedwerft.listitem", true);
			}
		}
		
		// Verbindung Base <-> Werft
		if( werft instanceof ShipWerft ) {
			Ship ship = ((ShipWerft)werft).getShip();
			Base linkedbase = ((ShipWerft)werft).getLinkedBase();
			
			ShipTypeData shiptype = ship.getTypeData();
			if( shiptype.getCost() == 0 ) {
				t.setBlock("_WERFT.WERFTGUI", "werftgui.linkedbase.listitem", "werftgui.linkedbase.list");
				
				if( linkedbase != null ) {
					t.setVar(	"linkedbase.selected",	false,
								"linkedbase.value",		"-1",
								"linkedbase.name",		"kein Ziel" );
					
					t.parse("werftgui.linkedbase.list", "werftgui.linkedbase.listitem", true);
				}

				List<?> bases = db.createQuery("from Base " +
							"where x=? and y=? and system=? and owner=? order by id")
							.setInteger(0, werft.getX())
							.setInteger(1, werft.getY())
							.setInteger(2, werft.getSystem())
							.setEntity(3, werft.getOwner())
							.list();
				for( Iterator<?> iter=bases.iterator(); iter.hasNext(); ) {
					Base base = (Base)iter.next();
					
					t.setVar(	"linkedbase.selected",	(linkedbase == base),
								"linkedbase.value",		base.getId(),
								"linkedbase.name",		base.getName()+" ("+base.getId()+")" );
					t.parse("werftgui.linkedbase.list", "werftgui.linkedbase.listitem", true);
				}
			}
		}
	}

	private void out_wsShipList(WerftObject werft) {
		org.hibernate.Session db = context.getDB();

		t.setVar("werftgui.wsshiplist", 1);
		t.setBlock("_WERFT.WERFTGUI", "wsshiplist.listitem", "wsshiplist.list");
		
		List<?> ships = db.createQuery("from Ship s inner join fetch s.owner as u left join fetch s.modules " +
								"where s.id>0 and (s.x between ? and ?) and (s.y between ? and ?) and s.system=? and " +
								"locate('l ',s.docked)=0 and s.battle is null order by u.id,s.id")
								.setInteger(0, werft.getX()-werft.getSize())
								.setInteger(1, werft.getX()+werft.getSize())
								.setInteger(2, werft.getY()-werft.getSize())
								.setInteger(3, werft.getY()+werft.getSize())
								.setInteger(4, werft.getSystem())
								.list();
	
		for( Iterator<?> iter=ships.iterator(); iter.hasNext(); ) {
			Ship ship = (Ship)iter.next();
			
			if( (werft instanceof ShipWerft) && (((ShipWerft)werft).getShip() == ship) ) {
				continue;	
			}
			
			if(ship.isDamaged()) {
				t.setVar("ship.needsrepair", 1);
			}
			else {
				t.setVar("ship.needsrepair", 0);
			}
			
			String ownername = Common._title(ship.getOwner().getName());
						
			t.setVar(	"ship.id",			ship.getId(),
						"ship.name",		ship.getName(),
						"ship.owner.name",	ownername );
								
			t.parse("wsshiplist.list", "wsshiplist.listitem", true);
		}
	}

	private void out_queueShipList(WerftObject werft, WerftQueueEntry[] queue) {		
		t.setBlock("_WERFT.WERFTGUI", "queueshiplist.listitem", "queueshiplist.list");
		t.setBlock("queueshiplist.listitem", "queueship.buildcosts.listitem", "queueship.buildcosts.list");
		
		for( int i=0; i < queue.length; i++ ) {
			t.start_record();
			WerftQueueEntry entry = queue[i];

			t.setVar(
					"queueship.buildcosts.list",	"",
					"queueship.position",	entry.getPosition(),
					"queueship.type.image",	entry.getBuildShipType().getPicture(),
					"queueship.type.id",	entry.getBuildShipType().getTypeId(),
					"queueship.type.name",	entry.getBuildShipType().getNickname(),
					"queueship.remainingtotal",	werft.getTicksTillFinished(entry),
					"queueship.slots",	entry.getSlots(),
					"queueship.building",	entry.isScheduled(),
					"queueship.uppossible", i > 0,
					"queueship.downpossible",	i < queue.length-1);
			
			if( entry.getRequiredItem() != -1 ) {
				t.setVar(
						"queueship.reqitem",	true,
						"queueship.item.picture",	Items.get().item(entry.getRequiredItem()).getPicture(),
						"queueship.item.name",	Items.get().item(entry.getRequiredItem()).getName());
			}
			
			if( !entry.getCostsPerTick().isEmpty() ) {
				ResourceList reslist = entry.getCostsPerTick().getResourceList();
				for( ResourceEntry res : reslist ) {
					t.setVar(
							"res.image",	res.getImage(),
							"res.plainname",	res.getPlainName(),
							"res.name",		res.getName(),
							"res.cargo",	res.getCargo1()
					);
					
					t.parse("queueship.buildcosts.list", "queueship.buildcosts.listitem", true);
				}
			}
			
			if( entry.getEnergyPerTick() != 0 ) {
				
				t.setVar(
						"res.image",	config.get("URL")+"data/interface/energie.gif",
						"res.plainname",	"Energie",
						"res.name",		"Energie",
						"res.cargo",	entry.getEnergyPerTick()
				);
				
				t.parse("queueship.buildcosts.list", "queueship.buildcosts.listitem", true);
			}
			
			t.parse("queueshiplist.list", "queueshiplist.listitem", true);
			t.stop_record();
			t.clear_record();
		}
	}
	
	private void out_buildShipList(WerftObject werft, SQLResultRow[] shipdata) {		
		t.setVar("werftgui.buildshiplist", 1);
		t.setBlock("_WERFT.WERFTGUI", "buildshiplist.listitem", "buildshiplist.list");
		t.setBlock("buildshiplist.listitem", "buildship.res.listitem", "buildship.res.list");

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
				
				t.setVar(	"buildship.item.id",	itemdata.getItemID(),
							"buildship.item.color",	(data[0].toString().equals("local") ? "#EECC44" : "#44EE44"),
							"buildship.item.uses",	itemdata.getUses() );
			}
			
			t.setVar(	"res.image",		config.get("URL")+"data/interface/time.gif",
						"res.count",		ashipdata.getInt("dauer"),
						"res.plainname",	"Dauer",
						"res.mangel",		0 );
			t.parse("buildship.res.list", "buildship.res.listitem", false);
				
			ResourceList reslist = costs.compare( availablecargo, false );
			for( ResourceEntry res : reslist ) {
				t.setVar(	"res.image",		res.getImage(),
							"res.count",		res.getCargo1(),
							"res.plainname",	res.getPlainName(),
							"res.mangel",		res.getDiff() > 0 );
				t.parse("buildship.res.list", "buildship.res.listitem", true);
			}

			t.setVar(	"res.image",		config.get("URL")+"data/interface/energie.gif",
						"res.count",		ashipdata.getInt("ekosten"),
						"res.plainname",	"Energie",
						"res.mangel",		energy < ashipdata.getInt("ekosten") );
			t.parse("buildship.res.list", "buildship.res.listitem", true);
	
			t.setVar(	"res.image",		config.get("URL")+"data/interface/besatzung.gif",
						"res.count",		ashipdata.getInt("crew"),
						"res.plainname",	"Besatzung",
						"res.mangel",		crew < ashipdata.getInt("crew") );
			t.parse("buildship.res.list", "buildship.res.listitem", true);

			ShipTypeData shiptype = Ship.getShipType(ashipdata.getInt("type"));
			
			t.setVar(	"buildship.id",			ashipdata.getInt("id"),
						"buildship.type.id",	ashipdata.getInt("type"),
						"buildship.type.image",	shiptype.getPicture(),
						"buildship.flagschiff",	ashipdata.getBoolean("flagschiff"),
						"buildship.type.name",	shiptype.getNickname() );
			t.parse("buildshiplist.list", "buildshiplist.listitem", true);
			
			t.stop_record();
			t.clear_record();
		}
	}

	private void out_ResourceList(WerftObject werft, Cargo showonly) {		
		t.setBlock("_WERFT.WERFTGUI", "reslist.res.listitem", "reslist.res.list");
		
		Cargo cargo = werft.getCargo(false);
		int frei = werft.getCrew();
	
		ResourceList reslist = showonly.compare(cargo, false);
		for( ResourceEntry res : reslist ) {
			t.setVar(	"res.image",		res.getImage(),
						"res.plainname",	res.getPlainName(),
						"res.cargo",		res.getCargo2() );
			t.parse("reslist.res.list", "reslist.res.listitem", true);
		}
		t.setVar(	"res.image",		config.get("URL")+"data/interface/energie.gif",
					"res.plainname",	"Energie",
					"res.cargo",		werft.getEnergy() );
		t.parse("reslist.res.list", "reslist.res.listitem", true);
		
		t.setVar(	"res.image",		config.get("URL")+"data/interface/arbeitslos.gif",
					"res.plainname",	"Crew",
					"res.cargo",		frei );
		t.parse("reslist.res.list", "reslist.res.listitem", true);
	}

	private void out_ws(WerftObject werft, int ws) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		User user = (User)context.getActiveUser();
		
		Ship ship = (Ship)db.get(Ship.class, ws);
		if( (ship == null) || (ship.getId() < 0) ) {
			context.addError("Das angegebene Schiff existiert nicht", werft.getUrlBase());		
			return;
		}
		if( (werft instanceof ShipWerft) && (((ShipWerft)werft).getShipID() == ws) ) {
			context.addError("Sie k&ouml;nnen sich nicht selbst reparieren", werft.getUrlBase());			
			return;
		}
		if( !ship.getLocation().sameSector(0, werft, werft.getSize()) ) {
			context.addError("Das Schiff befindet sich nicht im selben Sektor wie die Werft", werft.getUrlBase());		
			return;
		}
		
		if( ship.getBattle() != null ) {
			context.addError("Das Schiff befindet sich in einer Schlacht", werft.getUrlBase());			
			return;
		}
		
		ShipTypeData shipType = ship.getTypeData();
		
		t.setVar(	"werftgui.ws",			1,
					"ship.id",				ship.getId(),
					"ship.name",			ship.getName(),
					"ship.own",				(ship.getOwner() == user),
					"ship.owner.id",		ship.getOwner(),
					"ship.type.modules",	shipType.getTypeModules() );
	
		if( ship.getOwner() != user ) {
			User owner = ship.getOwner();
			t.setVar("ship.owner.name", Common._title(owner.getName()));
		}

		String action = context.getRequest().getParameterString("werftact");
		if( action.equals("repair") ) {
			String conf = context.getRequest().getParameterString("conf");
			
			this.out_repairShip( ship, werft, conf );
		}
		else if( action.equals("dismantle") ) {
			String conf = context.getRequest().getParameterString("conf");
			
			this.out_dismantleShip( ship, werft, conf );
		}
		else if( action.equals("module") ) {
			this.out_moduleShip( ship, werft );
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

	private void out_ws_info(Ship ship) {
		ShipTypeData shipType = ship.getTypeData();
		
		t.setVar(	"werftgui.ws.showinfo",		1,
					"ship.name",				ship.getName(),
					"ship.type.id",				shipType.getTypeId(),
					"ship.type.picture",		shipType.getPicture(),
					"ship.id",					ship.getId(),
					"ship.hull",				ship.getHull(),
					"ship.type.hull",			shipType.getHull(),
					"ship.hull.color",			this.genSubColor(ship.getHull(), shipType.getHull()),
					"ship.panzerung",			Math.round(shipType.getPanzerung()*(double)ship.getHull()/shipType.getHull()),
					"ship.shields",				ship.getShields(),
					"ship.type.shields",		shipType.getShields(),
					"ship.shields.color",		this.genSubColor(ship.getShields(), shipType.getShields()),
					"ship.engine",				ship.getEngine(),
					"ship.type.engine",			( shipType.getCost() > 0 ? 100 : 0 ),
					"ship.engine.color",		this.genSubColor(ship.getEngine(), 100),
					"ship.weapons",				ship.getWeapons(),
					"ship.type.weapons",		( shipType.isMilitary() ? 100 : 0 ),
					"ship.weapons.color",		this.genSubColor(ship.getWeapons(), 100),
					"ship.comm",				ship.getComm(),
					"ship.type.comm",			100,
					"ship.comm.color",			this.genSubColor(ship.getComm(), 100),
					"ship.sensors",				ship.getSensors(),
					"ship.type.sensors",		100,
					"ship.sensors.color",		this.genSubColor(ship.getSensors(), 100),
					"ship.crew",				ship.getCrew(),
					"ship.type.crew",			shipType.getCrew(),
					"ship.crew.color",			this.genSubColor(ship.getCrew(), shipType.getCrew()),
					"ship.e",					ship.getEnergy(),
					"ship.type.e",				shipType.getEps(),
					"ship.e.color",				this.genSubColor(ship.getEnergy(), shipType.getEps()),
					"ship.heat",				ship.getHeat() );
	
		Offizier offizier = Offizier.getOffizierByDest('s', ship.getId());
		if( offizier != null ) {
			t.setVar(	"ship.offizier.id",			offizier.getID(),
						"ship.offizier.rang",		offizier.getRang(),
						"ship.offizier.picture",	offizier.getPicture(),
						"ship.offizier.name",		Common._plaintitle(offizier.getName()) );
		}	
	}

	private void out_moduleShip(Ship ship, WerftObject werft) {
		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();
		
		int item = context.getRequest().getParameterInt("item");
		int slot = context.getRequest().getParameterInt("slot");
		String moduleaction = context.getRequest().getParameterString("moduleaction");
	
		//Gehoert das Schiff dem User?
		if( (ship == null) || (ship.getId() < 0) || ((ship.getOwner() != user) && (user.getAccessLevel() < 100)) ) {
			context.addError("Das Schiff existiert nicht oder geh&ouml;rt nicht ihnen", werft.getUrlBase());
			return;
		}
		
		ShipTypeData shiptype = ship.getBaseType();

		List<String[]> moduleslots = new ArrayList<String[]>();
		String[] mslots = StringUtils.split(shiptype.getTypeModules(), ';');
		for( int i=0; i < mslots.length; i++ ) {
			String[] data = StringUtils.split(mslots[i], ':');
			moduleslots.add(data);	
		}
		
		t.setBlock("_WERFT.WERFTGUI", "ws.modules.slots.listitem", "ws.modules.slots.list");
		t.setBlock("ws.modules.slots.listitem", "slot.items.listitem", "slot.items.list");
		
		t.setVar(	"werftgui.ws.modules",	1,
					"ship.type.image",		shiptype.getPicture() );
			
		// Modul einbauen
		if( (item != 0) && (slot != 0) && (Items.get().item(item) != null) ) {
			werft.addModule( ship, slot, item );
			t.setVar("ws.modules.msg", Common._plaintext(werft.MESSAGE.getMessage()));
		}
		else if( moduleaction.equals("ausbauen") && (slot != 0) ) {
			werft.removeModule( ship, slot );
			t.setVar("ws.modules.msg", Common._plaintext(werft.MESSAGE.getMessage()));
		}
		
		Ship.ModuleEntry[] modules = ship.getModules();
		Map<Integer,Integer> usedslots = new HashMap<Integer,Integer>();
		
		for( int i=0; i < modules.length; i++ ) {
			usedslots.put(modules[i].slot, i);
		}
		
		Cargo cargo = werft.getCargo(false);
		List<ItemCargoEntry> itemlist = cargo.getItemsWithEffect( ItemEffect.Type.MODULE );
	
		// Slots (Mit Belegung) ausgeben
		for( int i=0; i < moduleslots.size(); i++ ) {
			String[] aslot = moduleslots.get(i);
			
			t.setVar(	"slot.name",		ModuleSlots.get().slot(aslot[1]).getName(),
						"slot.empty",		!usedslots.containsKey(Integer.parseInt(aslot[0])),
						"slot.id",			aslot[0],
						"slot.items.list",	"" );
								
			if( usedslots.containsKey(Integer.parseInt(aslot[0])) ) {
				Ship.ModuleEntry module = modules[usedslots.get(Integer.parseInt(aslot[0]))];
				Module moduleobj = Modules.getShipModule( module );
				if( aslot.length > 2 ) {
					moduleobj.setSlotData(aslot[2]);
				}
				
				t.setVar( "slot.module.name", moduleobj.getName() );
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
					t.setVar(	"item.id",		itemlist.get(j).getItemID(),
								"item.name",	itemobj.getName() );

					t.parse("slot.items.list", "slot.items.listitem", true);
				}
			}
			t.parse("ws.modules.slots.list", "ws.modules.slots.listitem", true);
		}
	}

	private void out_dismantleShip(Ship ship, WerftObject werft, String conf) {
		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();
		
		//Gehoert das Schiff dem User?
		if( (ship == null) || (ship.getId() < 0) || (ship.getOwner() != user) ) {
			context.addError("Das Schiff existiert nicht oder  geh&ouml;rt nicht ihnen", werft.getUrlBase());
			return;
		}
		
		ShipTypeData shiptype = ship.getTypeData();
		Cargo scargo = ship.getCargo();
		
		t.setBlock("_WERFT.WERFTGUI", "ws.dismantle.res.listitem", "ws.dismantle.res.list");
		
		t.setVar(	"werftgui.ws.dismantle",	1,
					"ship.type.image",			shiptype.getPicture(),
					"ws.dismantle.conf",		!conf.equals("ok") );

		//Gewinn ausgeben
		Cargo cost = werft.getDismantleCargo(ship);
		
		ResourceList reslist = cost.getResourceList();
		for( ResourceEntry res : reslist ) {
			t.setVar(	"res.image",		res.getImage(),
						"res.cargo",		res.getCargo1(),
						"res.plainname",	res.getPlainName() );
			t.parse("ws.dismantle.res.list", "ws.dismantle.res.listitem", true);
		}
		
		//Waren im Laderaum ausgeben	
		reslist = scargo.getResourceList();
		for( ResourceEntry res : reslist ) {
			t.setVar(	"res.image",		res.getImage(),
						"res.cargo",		res.getCargo1(),
						"res.plainname",	res.getPlainName() );
			t.parse("ws.dismantle.available.list", "ws.dismantle.res.listitem", true);
		}

		boolean ok = werft.dismantleShip(ship, !conf.equals("ok"));
		if( !ok ) {
			t.setVar("ws.dismantle.error", werft.MESSAGE.getMessage() );
		}
		else {
			String msg = werft.MESSAGE.getMessage();
			if( msg.length() > 0 ) {
				t.setVar("ws.dismantle.msg", msg);
			}
		}
	}

	private void out_repairShip(Ship ship, WerftObject werft, String conf) {
		Context context = ContextMap.getContext();
		
		if( (ship == null) || (ship.getId() < 0) ) {
			context.addError("Das angegebene Schiff existiert nicht oder geh&ouml;rt nicht ihnen");
			return;
		}
		
		ShipTypeData shiptype = ship.getTypeData();

		t.setBlock("_WERFT.WERFTGUI", "ws.repair.res.listitem", "ws.repair.res.list");

		t.setVar(	"ship.type.image",		shiptype.getPicture(),
					"werftgui.ws.repair",	1,
					"ws.repair.conf",		!conf.equals("ok") );
		
		Cargo cargo = werft.getCargo(false);
	
		WerftObject.RepairCosts repairCost = werft.getRepairCosts(ship);
	
		//Kosten ausgeben
		ResourceList reslist = repairCost.cost.compare( cargo, false );
		for( ResourceEntry res : reslist ) {
			t.setVar(	"res.image",			res.getImage(),
						"res.plainname",		res.getPlainName(),
						"res.cargo.needed",		res.getCargo1(),
						"res.cargo.available",	res.getCargo2() );
			t.parse("ws.repair.res.list", "ws.repair.res.listitem", true);
		}
		
		if( repairCost.e > 0 ) {
			t.setVar(	"res.image",			config.get("URL")+"data/interface/energie.gif",
						"res.plainname",		"Energie",
						"res.cargo.needed",		repairCost.e,
						"res.cargo.available",	werft.getEnergy() );
			t.parse("ws.repair.res.list", "ws.repair.res.listitem", true);
		}
	
		boolean ok = werft.repairShip(ship, !conf.equals("ok"));
		
		if( !ok ) {
			t.setVar("ws.repair.error", werft.MESSAGE.getMessage());
		}
		else {
			String msg = werft.MESSAGE.getMessage();
			if( msg.length() > 0 ) {
				t.setVar("ws.repair.message", msg);
			}
		}
	}

	private void out_buildShip(int build, WerftObject werft) {
		final int item = context.getRequest().getParameterInt("item");
		final boolean perTick = context.getRequest().getParameterInt("pertick") != 0;
		final String conf = context.getRequest().getParameterString("conf");
		
		if( perTick ) {
			t.setVar("werftgui.msg", "<span style=\"color:red\">Zahlung per Tick im Moment deaktiviert</span>");
			return;
		}
		
		Cargo cargo = werft.getCargo(false);
	
		SQLResultRow shipdata = werft.getShipBuildData( build, item );
		if( (shipdata == null) || shipdata.isEmpty() ) {
			t.setVar("werftgui.msg", "<span style=\"color:red\">"+werft.MESSAGE.getMessage()+"</span>");
			return;
		}
		
		ShipTypeData shiptype = Ship.getShipType( shipdata.getInt("type") );
		
		t.setBlock("_WERFT.WERFTGUI", "build.res.listitem", "build.res.list");
		
		t.setVar(	"werftgui.build",	1,
					"build.type.name",	shiptype.getNickname(),
					"build.type.image",	shiptype.getPicture(),
					"build.conf",		!conf.equals("ok"),
					"build.id",			build,
					"build.item.id",	item );
	
		//Resourcenbedraft angeben
		
	   	//Standardresourcen
		Cargo shipdataCosts = new Cargo((Cargo)shipdata.get("costs"));
		Cargo perTickCosts = new Cargo(shipdataCosts);
		perTickCosts.multiply(1/(double)shipdata.getInt("dauer"), Cargo.Round.CEIL);
		
		ResourceList reslist = shipdataCosts.compare( cargo, false );
		for( ResourceEntry res : reslist ) {
			t.setVar(	"res.image",			res.getImage(),
						"res.plainname",		res.getPlainName(),
						"res.cargo.pertick",	perTickCosts.getResourceCount(res.getId()),
						"res.cargo.available",	res.getCargo2(),
						"res.cargo.needed",		res.getCargo1(),
						"res.cargo.mangel",		res.getDiff() > 0 ? res.getDiff() : 0 );
			t.parse("build.res.list", "build.res.listitem", true);
		}
	
		//E-Kosten
		
		int ePerTick = (int)Math.ceil(shipdata.getInt("ekosten")/(double)shipdata.getInt("dauer"));
		
		t.setVar(	"res.image",		config.get("URL")+"data/interface/energie.gif",
					"res.plainname",	"Energie",
					"res.cargo.pertick",	ePerTick,
					"res.cargo.available",	werft.getEnergy(),
					"res.cargo.needed",	shipdata.getInt("ekosten"),
					"res.cargo.mangel",	(shipdata.getInt("ekosten") > werft.getEnergy() ? shipdata.getInt("ekosten") - werft.getEnergy() : 0) );
		t.parse("build.res.list", "build.res.listitem", true);

		// Crew
		final int frei = werft.getCrew();
		
		t.setVar(	"res.image",			config.get("URL")+"data/interface/arbeitslos.gif",
					"res.plainname",		"Crew",
					"res.cargo.pertick",	"",
					"res.cargo.available",	frei,
					"res.cargo.needed",		shipdata.getInt("crew"),
					"res.cargo.mangel",		(shipdata.getInt("crew") > frei ? shipdata.getInt("crew") - frei : 0));
		t.parse("build.othercosts.list", "build.res.listitem", true);
		
		// Werftslots
		t.setVar(	"res.image",			config.get("URL")+"data/interface/schiffinfo/werftslots.png",
					"res.plainname",		"Werftslots",
					"res.cargo.pertick",	"",
					"res.cargo.available",	werft.getWerftSlots(),
					"res.cargo.needed",		shipdata.getInt("werftslots"),
					"res.cargo.mangel",		false);
		t.parse("build.othercosts.list", "build.res.listitem", true);
		
		// Dauer
		t.setVar(	"res.image",			config.get("URL")+"data/interface/time.gif",
					"res.plainname",		"Dauer",
					"res.cargo.pertick",	"",
					"res.cargo.available",	"",
					"res.cargo.needed",		shipdata.getInt("dauer"),
					"res.cargo.mangel",		false);
		t.parse("build.othercosts.list", "build.res.listitem", true);
		
		// Testen ob Bau moeglich
		if( !conf.equals("ok") ) {
			// Sofort zahlen
			boolean result = werft.buildShip(build, item, false, true );
		
			if( !result ) {
				t.setVar("build.instant.error", StringUtils.replace(werft.MESSAGE.getMessage(), "\n", "<br/>\n"));
			}
			
			// Kosten pro Tick
			result = werft.buildShip(build, item, true, true );
			
			if( !result ) {
				t.setVar("build.pertick.error", StringUtils.replace(werft.MESSAGE.getMessage(), "\n", "<br/>\n"));
			}
		}
		// Bau ausfuehren
		else {
			boolean result = werft.buildShip(build, item, perTick, false );
			
			if( !result ) {
				t.setVar("build.error", StringUtils.replace(werft.MESSAGE.getMessage(), "\n", "<br/>\n"));
			}
		}
		
		return;   
	}
}

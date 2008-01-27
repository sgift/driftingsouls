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

import java.util.List;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.Item;
import net.driftingsouls.ds2.server.config.Items;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

class Kommandozentrale extends DefaultBuilding {
	/**
	 * Erstellt eine neue Instanz der Kommandozentrale
	 * @param row Die SQL-Ergebniszeile mit den Gebaeudedaten der Kommandozentrale
	 */
	public Kommandozentrale(SQLResultRow row) {
		super(row);
	}

	@Override
	public void cleanup(Context context, Base base) {
		super.cleanup(context, base);
		
		Database db = context.getDatabase();
		
		db.update("UPDATE bases SET owner=0,autogtuacts='' WHERE id="+base.getID());
		base.getAutoGTUActs().clear();
		base.setOwner(0);
		
		// TODO: Unschoen. Das sollte die Werft selbst machen
		db.update("UPDATE werften SET building=0,item=-1,remaining=0,flagschiff=0 WHERE col="+base.getID());
		db.update("UPDATE werften SET linked=0 WHERE linked="+base.getID());
	}

	@Override
	public String echoShortcut(Context context, Base base, int field, int building) {
		String sess = context.getSession();
		
		return "<a class=\"back\" href=\"./main.php?module=building&amp;sess="+sess+"&amp;col="+base.getID()+"&amp;field="+field+"\">[K]</a>";
	}

	@Override
	public boolean classicDesign() {
		return true;
	}

	@Override
	public boolean printHeader() {
		return false;
	}

	@Override
	public String output(Context context, TemplateEngine t, Base base, int field, int building) {
		Database db = context.getDatabase();
		User user = context.getActiveUser();
		
		String show = context.getRequest().getParameter("show");
		if( show == null ) {
			show = "";
		}
		String baction = context.getRequest().getParameterString("baction");
				
		if( !show.equals("general") && !show.equals("autogtu") ) {
			show = "general";	
		}
				
		if( !t.setFile( "_BUILDING", "buildings.kommandozentrale.html" ) ) {
			context.addError("Konnte das Template-Engine nicht initialisieren");
			return "";
		}
		
		t.setVar(	"base.name",	base.getName(),
					"base.id",		base.getID(),
					"base.field",	field,
					"base.system",	base.getSystem(),
					"base.size",	base.getSize());
		
		SQLResultRow kurseRow = db.first("SELECT kurse FROM gtu_warenkurse WHERE place='asti'");
		Cargo kurse = new Cargo( Cargo.Type.STRING, kurseRow.getString("kurse") );
		kurse.setOption( Cargo.Option.SHOWMASS, false );
		
		Cargo cargo = base.getCargo();
		
		StringBuilder message = new StringBuilder();
		
		/*
			Resourcen an die GTU verkaufen
		 */
	
		if( baction.equals("sell") ) {
			long totalRE = 0;
		
			int tick = context.get(ContextCommon.class).getTick();
			int system = base.getSystem();
			
			SQLResultRow statsRow = db.first("SELECT stats FROM stats_verkaeufe WHERE tick=",tick," AND place='asti' AND system=",system);
			Cargo stats = new Cargo();
			if( !statsRow.isEmpty() ) {
				stats = new Cargo( Cargo.Type.STRING, statsRow.getString("stats") );
			}
			
			boolean changed = false;
			
			ResourceList reslist = kurse.getResourceList();
			for( ResourceEntry res : reslist ) {			
				long tmp = context.getRequest().getParameterInt(res.getId()+"to");
				
				if( tmp > 0 ) {
					if( tmp > cargo.getResourceCount(res.getId()) ) {
						tmp = cargo.getResourceCount(res.getId());
					}
					
					if( tmp <= 0 ) {
						continue;
					}
					
					long get = (long)(tmp*(double)res.getCount1()/1000d);
					
					message.append("<img src=\""+res.getImage()+"\" alt=\"\" />"+Common.ln(tmp)+" f&uuml;r "+Common.ln(get)+" RE verkauft<br />\n");
					totalRE += get;
					
					changed = true;
					cargo.substractResource(res.getId(), tmp);
					stats.addResource(res.getId(), tmp);
				}
			}
			if( changed ) {
				db.tBegin();
				
				SQLResultRow statid = db.first("SELECT id FROM stats_verkaeufe WHERE tick='",tick,"' AND place='asti' AND system='",system,"'");
				if( statid.isEmpty() ) {
					db.update("INSERT INTO stats_verkaeufe (tick,place,system,stats) VALUES (",tick,",'asti',",system,",'",stats.save(),"')");
				}
				else {		
					db.tUpdate(1, "UPDATE stats_verkaeufe SET stats='",stats.save(),"' WHERE id='",statid.getInt("id"),"' AND stats='",stats.save(true),"'");
				}
				
				db.tUpdate(1, "UPDATE bases SET cargo='",cargo.save(),"' WHERE id="+base.getID()+" AND cargo='",cargo.save(true),"'");
				
				user.transferMoneyFrom(Faction.GTU, totalRE, "Warenverkauf Asteroid "+base.getID()+" - "+base.getName(), false, User.TRANSFER_SEMIAUTO );
				
				if( !db.tCommit() ) {
					context.addError("Fehler: Die Transaktion der Waren war nicht erfolgreich");
				}
				message.append("<br />");
			}
		}
		
		/*
			Batterien aufladen
		*/
		
		if( baction.equals("load") ) {
			long load = context.getRequest().getParameterInt("load");
			
			if( load < 0 ) {
				load *= -1;
			}
		
			int e = base.getE();
			
			if( load > e ) {
				load = e;
			} 
			if( load > cargo.getResourceCount( Resources.LBATTERIEN ) ) {
				load = cargo.getResourceCount( Resources.LBATTERIEN );
			}
			
			if( load != 0 ) {
				message.append(Common._plaintitle(base.getName())+" l&auml;dt <img src=\""+Cargo.getResourceImage(Resources.LBATTERIEN)+"\" alt=\"\" />"+load+" "+Cargo.getResourceName(Resources.LBATTERIEN)+" auf<br/ ><br />\n");
			
				cargo.substractResource( Resources.LBATTERIEN, load );
				cargo.addResource( Resources.BATTERIEN, load );
				e -= load;
			
				db.update("UPDATE bases SET e=",e,",cargo='",cargo.save(),"' WHERE id="+base.getID()+" AND cargo='",cargo.save(true),"' AND e='",base.getE(),"'");
				if( db.affectedRows() != 0 ) {
					base.setE(e);	
				}
			}
		}
		
		/*
			Batterien entladen
		*/
		
		if( baction.equals("unload") ) {
			long unload = context.getRequest().getParameterInt("unload");
			
			if( unload < 0 ) {
				unload *= -1;
			}
		
			int e = base.getE();
			
			if( unload > cargo.getResourceCount( Resources.BATTERIEN ) ) {
				unload = cargo.getResourceCount( Resources.BATTERIEN );
			}
			
			if( unload != 0 ) {
				message.append(Common._plaintitle(base.getName())+" entl&auml;dt <img src=\""+Cargo.getResourceImage(Resources.BATTERIEN)+"\" alt=\"\" />"+unload+" "+Cargo.getResourceName(Resources.BATTERIEN)+"<br/ ><br />\n");
			
				cargo.substractResource( Resources.BATTERIEN, unload );
				cargo.addResource( Resources.LBATTERIEN, unload );
				e += unload;
			
				db.update("UPDATE bases SET e=",e,",cargo='",cargo.save(),"' WHERE id="+base.getID()+" AND cargo='",cargo.save(true),"' AND e='",base.getE(),"'");
				if( db.affectedRows() != 0 ) {
					base.setE(e);	
				}
			}
		}
		
		/*
			Batterien herstellen
		*/
		
		if( baction.equals("create") ) {
			long create = context.getRequest().getParameterInt("create");
			
			if( create > cargo.getResourceCount( Resources.PLATIN ) ) {
				create = cargo.getResourceCount( Resources.PLATIN );
			}
			
			if( create != 0 ) {
				message.append(Common._plaintitle(base.getName())+" produziert <img src=\""+Cargo.getResourceImage(Resources.LBATTERIEN)+"\" alt=\"\" />"+create+" "+Cargo.getResourceName(Resources.LBATTERIEN)+"<br/ ><br />\n");
			
				cargo.substractResource( Resources.PLATIN, create );
				cargo.addResource( Resources.LBATTERIEN, create );
			
				db.update("UPDATE bases SET cargo='",cargo.save(),"' WHERE id="+base.getID()+" AND cargo='",cargo.save(true),"'");
			}
		}
		
		/*
			Allyitems an die Allianz ueberstellen
		*/
		
		if( baction.equals("item") ) {
			int item = context.getRequest().getParameterInt("item");
			
			int ally = user.getAlly();
			
			if( ally == 0 ) {
				message.append("Sie sind in keiner Allianz<br /><br />\n");
			}
			else if( Items.get().item(item) == null || !Items.get().item(item).getEffect().hasAllyEffect() ) {
				message.append("Kein passenden Itemtyp gefunden<br /><br />\n");
			}
			else if( !cargo.hasResource( new ItemID(item) ) ) {
				message.append("Kein passendes Item vorhanden<br /><br />\n");
			}
			else {
				String allyitemsStr = db.first("SELECT items FROM ally WHERE id="+ally).getString("items");
				Cargo allyitems = new Cargo( Cargo.Type.ITEMSTRING, allyitemsStr );
				allyitems.addResource( new ItemID(item), 1 );
				cargo.substractResource( new ItemID(item), 1 );
		
				db.update("UPDATE ally SET items='"+allyitems.getData( Cargo.Type.ITEMSTRING )+"' WHERE id="+ally);
				db.update("UPDATE bases SET cargo='"+cargo.save()+"' WHERE id="+base.getID());
						
				String msg = "Ich habe das Item \""+Items.get().item(item).getName()+"\" der Allianz zur Verf&uuml;gung gestellt.";
				PM.send(context, user.getId(), ally, "Item &uuml;berstellt", msg, true);
		
				message.append("Das Item wurde an die Allianz &uuml;bergeben<br /><br />\n");
			}
		}
		
		/*
			Einen Auto-Verkauf Eintrag hinzufuegen
		*/
		if( baction.equals("gtuadd") ) {
			ResourceID resid = Resources.fromString(context.getRequest().getParameterString("resid"));

			int actid = context.getRequest().getParameterInt("actid");
			int count = context.getRequest().getParameterInt("count");
			
			if( (actid >= 0) && (actid <= 1 ) && (count != 0 || (actid == 1)) ) {
				BaseStatus basedata = Base.getStatus(context, base.getID());
				Cargo stat = (Cargo)basedata.getStatus().clone();
				stat.setResource(Resources.NAHRUNG, 0);
				
				if( stat.getResourceCount(resid) != 0 && kurse.getResourceCount(resid) != 0 ) {
					base.getAutoGTUActs().add(new AutoGTUAction(resid,actid,count));
					
					db.update("UPDATE bases SET autogtuacts='"+Common.implode(";", base.getAutoGTUActs())+"' WHERE id='"+base.getID()+"'");
					
					message.append("Automatischer Verkauf von <img style=\"vertical-align:middle\" src=\""+Cargo.getResourceImage(resid)+"\" alt=\"\" />"+Cargo.getResourceName(resid)+" hinzugef&uuml;gt<br /><br />\n");
				}
			}
		}
		
		/*
			Einen Auto-Verkauf Eintrag entfernen
		*/
		if( baction.equals("gtudel") ) {
			String gtuact = context.getRequest().getParameterString("gtuact");
			
			if( gtuact.length() != 0 ) {
				List<AutoGTUAction> autoactlist = base.getAutoGTUActs();
				
				for( AutoGTUAction autoact : autoactlist ) {
					if( gtuact.equals(autoact.toString()) ) {
						autoactlist.remove(autoact);
						message.append("Eintrag entfernt<br /><br />\n");
						
						break;
					}
				}
				
				db.update("UPDATE bases SET autogtuacts='"+Common.implode(";", autoactlist)+"' WHERE id='"+base.getID()+"'");
			}
		}

		t.setVar("building.message", message.toString());
		
		if( show.equals("general") ) {
			t.setVar("show.general", 1);
			
			t.setBlock("_BUILDING", "general.itemconsign.listitem", "general.itemconsign.list");
			t.setBlock("_BUILDING", "general.shiptransfer.listitem", "general.shiptransfer.list");
			t.setBlock("_BUILDING", "general.basetransfer.listitem", "general.basetransfer.list");
			t.setBlock("_BUILDING", "general.sell.listitem", "general.sell.list");		
			
			t.setVar(	"res.batterien.image",	Cargo.getResourceImage(Resources.BATTERIEN),
						"res.lbatterien.image",	Cargo.getResourceImage(Resources.LBATTERIEN),
						"res.platin.image",		Cargo.getResourceImage(Resources.PLATIN) );
			
			List<ItemCargoEntry> itemlist = cargo.getItems();
			if( itemlist.size() != 0 ) {
				int ally = user.getAlly();
				if( ally > 0 ) {
					for( ItemCargoEntry item : itemlist ) {
						Item itemobject = item.getItemObject();
						if( itemobject.getEffect().hasAllyEffect() ) {
							t.setVar(	"item.id",		item.getItemID(),
										"item.name",	itemobject.getName() );
							t.parse("general.itemconsign.list", "general.itemconsign.listitem", true);
						}
					}
				}
			}
			
			/*
				Waren zu Schiffen/Basen im Orbit transferieren
			*/
			
			SQLQuery ship = db.query("SELECT id,name,x,y,owner FROM ships " +
					"WHERE id>0 AND x BETWEEN "+(base.getX()-base.getSize())+" AND "+(base.getX()+base.getSize())+" AND " +
							"y BETWEEN "+(base.getY()-base.getSize())+" AND "+(base.getY()+base.getSize())+" AND " +
							"system="+base.getSystem()+" AND !LOCATE('l ',docked) AND battle=0 " +
					"ORDER BY x,y,owner,id");
			if( !ship.isEmpty() ) {
				int oldx = 0;
				int oldy = 0;

				while( ship.next() ) {
					if( (oldx == 0) && (oldy == 0) ) {
						oldx = ship.getInt("x");
						oldy = ship.getInt("y");	
						if( base.getSize() != 0 ) {
							t.setVar("ship.begingroup", 1);
						}
					}
					else if( (oldx != ship.getInt("x")) || (oldy != ship.getInt("y")) ) {
						oldx = ship.getInt("x");
						oldy = ship.getInt("y");	
						if( base.getSize() != 0 ) {
							t.setVar(	"ship.begingroup",	1,
										"ship.endgroup",	1 );
						}
					}
					
					t.setVar(	"ship.id",		ship.getInt("id"),
								"ship.name",	Common._plaintitle(ship.getString("name")),
								"ship.x",		ship.getInt("x"),
								"ship.y",		ship.getInt("y") );
					
					if( ship.getInt("owner") != user.getId() ) {
						User owner = context.createUserObject(ship.getInt("owner"));
						t.setVar("ship.owner.name", owner.getPlainname());
					}
					else {
						t.setVar("ship.owner.name", "");
					}
					
					t.parse("general.shiptransfer.list", "general.shiptransfer.listitem", true);
				}
			}
			ship.free();
			
			
			SQLQuery targetbase = db.query("SELECT id,name FROM bases WHERE x="+base.getX()+" AND y="+base.getY()+" AND system="+base.getSystem()+" AND id!="+base.getID()+" AND owner='"+user.getId()+"'");
			while( targetbase.next() ) {
				t.setVar(	"targetbase.id", 	targetbase.get("id"),
							"targetbase.name",	Common._plaintitle(targetbase.getString("name")) );
				t.parse("general.basetransfer.list", "general.basetransfer.listitem", true);
			}
			targetbase.free();
			
			
			/*
				Waren verkaufen
			*/	
			ResourceList reslist = kurse.compare(cargo, false);
			for( ResourceEntry res : reslist ) {
				if( res.getCount2() == 0 ) {
					continue;
				}
				
				t.setVar(	"res.image",	res.getImage(),
							"res.name",		res.getName(),
							"res.cargo2",	res.getCargo2(),
							"res.id",		res.getId() );
				
				if( res.getCount1() <= 5 ) {
					t.setVar("res.cargo1", "Kein Bedarf");
				}
				else {
					t.setVar("res.cargo1", Common.ln(res.getCount1()/1000d)+" RE");
				}
									
				t.parse("general.sell.list", "general.sell.listitem", true);
			}
		}
		else if( show.equals("autogtu") ) {
			t.setVar("show.autogtu", 1);
			t.setBlock("_BUILDING", "autogtu.acts.listitem", "autogtu.acts.list");
			t.setBlock("_BUILDING", "autogtu.reslist.listitem", "autogtu.reslist.list");
			
			List<AutoGTUAction> autoactlist = base.getAutoGTUActs();
			for( AutoGTUAction autoact : autoactlist ) {
				if( (autoact.getActID() != 1) && (autoact.getCount() == 0) ) {
					continue;	
				}
					
				t.setVar(	"res.image",		Cargo.getResourceImage(autoact.getResID()),
							"res.name",			Cargo.getResourceName(autoact.getResID()),
							"res.sellcount"	,	Common.ln(autoact.getCount()),
							"res.action.total",	autoact.getActID() == 0,
							"res.action.limit",	autoact.getActID() == 1,
							"res.actionstring",	autoact.toString() );
				
				t.parse("autogtu.acts.list", "autogtu.acts.listitem", true);
			}
			
			BaseStatus basedata = Base.getStatus(context, base.getID());
			Cargo stat = (Cargo)basedata.getStatus().clone();
			stat.setResource( Resources.NAHRUNG, 0 );
			stat.setOption( Cargo.Option.NOHTML, true );
			ResourceList reslist = stat.compare(kurse, false);
			for( ResourceEntry res : reslist ) {
				if( (res.getCount1() > 0) && (res.getCount2() > 0) ) {
					t.setVar(	"res.id",	res.getId(),
								"res.name",	res.getName() );
					t.parse("autogtu.reslist.list", "autogtu.reslist.listitem", true);
				}	
			}
		}

		t.parse( "OUT", "_BUILDING" );	
		return t.getVar("OUT");
	}
}

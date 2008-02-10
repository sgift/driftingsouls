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
package net.driftingsouls.ds2.server.tick.regular;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.math.RandomUtils;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Rasse;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.tick.TickController;

/**
 * Berechnet NPC-Bestellungen
 * 
 * @author Christopher Jung
 *
 */
public class NPCOrderTick extends TickController {
	private static final int OFFIZIERSSCHIFF = 71;
	private static final Location DEFAULT_LOCATION = new Location(2,30,35);
	
	private int maxid;
	private StringBuilder pmcache;
	private int lastowner;
	private String currentTime;
	private Map<Integer,List<String>> offinamelist;	
	
	@Override
	protected void prepare() {
		Database db = getContext().getDatabase();
		
		this.maxid = db.first("SELECT max(id) maxid FROM ships").getInt("maxid");

		this.log("maxid : "+this.maxid);

		this.pmcache = new StringBuilder();
		this.lastowner = 0;

		this.currentTime = Common.getIngameTime(getContext().get(ContextCommon.class).getTick());

		this.offinamelist = new HashMap<Integer,List<String>>();
		for( Rasse race : Rassen.get() ) {
			offinamelist.put(race.getID(), new ArrayList<String>());
			if( race.getNameGenerator(Rasse.GENERATOR_PERSON) != null ) {
				try {
					Process p = Runtime.getRuntime().exec(race.getNameGenerator(Rasse.GENERATOR_PERSON)+" 100 \\n");
					BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
					String tmp = null;
					while( (tmp = in.readLine()) != null ) {
						offinamelist.get(race.getID()).add(tmp);
					}
					in.close();
				}
				catch( Exception e ) {
					log("FEHLER: Laden der Offiziersnamen nicht moeglich");
				}
			}
		}		
	}
	
	private String getOffiName(User user) {
		if( this.offinamelist.get(user.getRace()).size() > 0 ) {
			List<String> names = this.offinamelist.get(user.getRace());
			String name = names.get(RandomUtils.nextInt(names.size()));
			if( name.trim().length() == 0 ) {
				return "NPC-Lieferservice";
			}
			return name;
		}
		return "NPC-Lieferservice";
	}

	@Override
	protected void tick() {
		Database db = getDatabase();
		
		SQLQuery data = db.query("SELECT * FROM orders WHERE tick=1 ORDER BY user");
		while( data.next() ) {
			try {
				int owner = data.getInt("user");
				User user = (User)getContext().getDB().get(User.class, owner);
					
				if( (owner != this.lastowner) && this.pmcache.length() > 0 ) {
					PM.send(getContext(), -1, this.lastowner, "NPC-Lieferservice", this.pmcache.toString());
					pmcache.setLength(0);
				}
				lastowner = owner;
			
				int type = OFFIZIERSSCHIFF;
				if( data.getInt("type") > 0 ) {
					type = data.getInt("type");
				}
			
				SQLResultRow shipd = ShipTypes.getShipType( type, false );
			
				if( data.getInt("type") > 0 ) {
					this.log("* Order "+data.getInt("id")+" ready: "+shipd.getString("nickname")+" ("+type+") wird zu User "+data.getInt("user")+" geliefert");
				}
				else {
					this.log("* Order "+data.getInt("id")+" ready: Offizier wird mittels "+shipd.getString("nickname")+" ("+type+") wird zu User "+data.getInt("user")+" geliefert");
				}
			
				SQLResultRow base = db.first("SELECT id,x,y,system,name FROM bases WHERE owner=",data.getInt("user")," ORDER BY id");
				Location loc = DEFAULT_LOCATION;
				if( !base.isEmpty() ) {
					loc = Location.fromResult(base);
				}
				
				this.log("  Lieferung erfolgt bei "+loc);
				// Falls ein Schiff geordert wurde oder keine Basis fuer den Offizier existiert (und er braucht somit ein Schiff)...
				if( (data.getInt("type") > 0) || base.isEmpty() ) {
					this.maxid++;
				
					Cargo cargo = new Cargo();
					cargo.addResource( Resources.DEUTERIUM, shipd.getInt("rd")*10 );
					cargo.addResource( Resources.URAN, shipd.getInt("ru")*10 );
					cargo.addResource( Resources.ANTIMATERIE, shipd.getInt("ra")*10 );
				
					User auser = (User)getContext().getDB().get(User.class, owner);	
					String history = "Indienststellung am "+this.currentTime+" durch "+auser.getName()+" ("+auser.getId()+") [hide]NPC-Order[/hide]\n";
								
					db.prepare("INSERT INTO ships " ,
							"(id,owner,name,type,x,y,system,crew,hull,e,cargo,history) " ,
							"VALUES " ,
							"( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
						.update(maxid, owner, "noname", type, loc.getX(), loc.getY(), loc.getSystem(), shipd.getInt("crew"), shipd.getInt("hull"), shipd.getInt("eps"), cargo.save(), history);
					
					if( shipd.getString("werft").length() > 0 ) {
						db.update("INSERT INTO werften (shipid) VALUES ('",this.maxid,"')");
					}
				}
			
				// Es handelt sich um einen Offizier...
				if( data.getInt("type") < 0 ) {
					SQLResultRow offizier = db.first("SELECT name,rang,ing,waf,nav,sec,com FROM orders_offiziere WHERE id=",(-data.getInt("type")));
					int special = RandomUtils.nextInt(6)+1;
					
					String dest = "s "+this.maxid;
					if( !base.isEmpty() ) {
						dest = "b "+base.getInt("id");	
					}
					String name = this.getOffiName(user);
					
					db.prepare("INSERT INTO offiziere ",
								"(name,userid,rang,ing,waf,nav,sec,com,dest,spec) ",
								"VALUES ",
								"( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
						.update(name, owner, offizier.getInt("rang"), offizier.getInt("ing"), offizier.getInt("waf"), offizier.getInt("nav"), offizier.getInt("sec"), offizier.getInt("com"), dest, special);
			
					// PM-Nachricht erstellen
					pmcache.append("Der von ihnen bestellte ");
					pmcache.append(offizier.getString("name"));
					pmcache.append(" wurde geliefert.\nEr befindet sich auf ");
					
					if( base.isEmpty() ) {
						pmcache.append("einer ");
						pmcache.append(shipd.getString("nickname"));
						pmcache.append(" bei ");
					}
					else {
						pmcache.append("auf ihrer Basis ");
						pmcache.append(base.getString("name"));
						pmcache.append(" (");
						pmcache.append(base.getInt("id"));
						pmcache.append(") im Sektor ");
					}
					
					pmcache.append(loc);
					pmcache.append("\n\n");
				}
				// Es wurde nur ein Schiff geordert
				else {
					pmcache.append("Die von ihnen bestellte ");
					pmcache.append(shipd.getString("nickname"));
					pmcache.append(" wurde geliefert\nSie steht bei ");
					pmcache.append(loc);
					pmcache.append("\n\n");
				}
				
				Ships.recalculateShipStatus(this.maxid);
			
				db.update("DELETE FROM orders WHERE id=",data.getInt("id"));
				
				getContext().commit();
			}
			catch( Exception e ) {
				this.log("Order "+data.getInt("id")+" failed: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "NPCOrderTick Exception", "order: "+data.getInt("id"));
			}
		}
		data.free();
		
		if( pmcache.length() > 0 ) {
			PM.send(getContext(), -1, lastowner, "NPC-Lieferservice", pmcache.toString());
		}
		
		this.log("Verringere Wartezeit um 1...");
		db.update("UPDATE orders SET tick=tick-1");
		
		this.log("Verteile NPC-Punkte...");
		db.update("UPDATE users SET npcpunkte=npcpunkte+1 WHERE LOCATE('ordermenu',flags)");
	}

}

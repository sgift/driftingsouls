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
package net.driftingsouls.ds2.server.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Der Gebaeudebau
 * @author Christopher Jung
 * 
 * @urlparam Integer col Die Bases, auf der das Gebaeude gebaut werden soll 
 * @urlparam Integer field Die ID des Feldes, auf dem das Gebaeude gebaut werden soll
 */
public class BuildController extends DSGenerator {
	private Base base;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public BuildController(Context context) {
		super(context);
		
		setTemplate("build.html");
		
		parameterNumber("col");
		parameterNumber("field");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		Database db = getDatabase();
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		
		int col = getInteger("col");
		int field = getInteger("field");
		
		SQLResultRow baseRow = db.first("SELECT * FROM bases WHERE owner=",user.getId()," AND id=",col);
		if( baseRow.isEmpty() ) {
			addError("Die angegebene Kolonie existiert nicht", Common.buildUrl(getContext(), "default", "module", "basen"));
			
			return false;
		}
		base = new Base(baseRow);		
		
		t.setVar(	"base.id",		base.getID(),
					"base.name",	Common._plaintitle(base.getName()),
					"global.field",	field );
		
		return true;
	}

	/**
	 * Baut ein Gebaeute auf der Kolonie
	 * @urlparam Integer build Die ID des zu bauenden Gebaeudes
	 *
	 */
	public void buildAction() {
		Database db = getDatabase();
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("build");
		int build = getInteger("build");
		
		int field = getInteger("field");
		
		Building building = Building.getBuilding(db, build);
				
		if( building == null ) {
			addError("Das angegebene Geb&auml;ude existiert nicht");
			
			redirect();
			return;
		}
		
		//Darf das Gebaeude ueberhaupt gebaut werden?
		
		if( field >= base.getWidth() * base.getHeight() ) {
			addError("Und das elfte Gebot lautet: Du sollst nicht exploiten deines Spieles URL's");
			
			redirect();
			return;
		}

		//Anzahl der Gebaeude berechnen
		if( building.getPerPlanetCount() != 0 ) {
			int buildingcount = 0;
			for( int bid : base.getBebauung() ) {
				if( bid == building.getId() ) {
					buildingcount++;
				}
			}
			
			if( building.getPerPlanetCount() <= buildingcount ) {
				addError("Sie k&ouml;nnen dieses Geb&auml;de maximal "+building.getPerPlanetCount()+" Mal pro Asteriod bauen");
			
				redirect();
				return;
			}
		}
	
		//Anzahl der Gebaeude pro Spieler berechnen
		if( building.getPerUserCount() != 0 ) {
			int ownerbuildingcount = 0;
		
			SQLQuery abeb = db.query("SELECT bebauung FROM bases WHERE owner="+user.getId());
			while( abeb.next() ) {
				int[] aBebList = Common.explodeToInt("|",abeb.getString("bebauung"));
				for( int bid : aBebList ) {
					if( bid == building.getId() ) {
						ownerbuildingcount++;
					}
				}
			}
			abeb.free();
			
			if( building.getPerUserCount() <= ownerbuildingcount ) {
				addError("Sie k&ouml;nnen dieses Geb&auml;de maximal "+building.getPerUserCount()+" Mal insgesamt bauen");
			
				redirect();
				return;
			}
		}


		if( base.getBebauung()[field] != 0 ) {
			addError("Es existiert bereits ein Geb&auml;ude an dieser Stelle");
			
			redirect();
			return;
		}

		if( building.isUComplex() ) {
			List<Integer> ucbuildings = new ArrayList<Integer>();
			SQLQuery ucbid = db.query("SELECT id FROM buildings WHERE ucomplex='1'");
			while( ucbid.next() ) {
				ucbuildings.add(ucbid.getInt("id"));
			}
			ucbid.free();
	
			int c = 0;
			for( int i =0; i <= base.getWidth() * base.getHeight() -1 ; i++ ) {
				if( ucbuildings.contains(base.getBebauung()[i]) ) {
					c++;
				}
			}
	
			int grenze = (base.getWidth() * base.getHeight())/8;
	
			if( c > grenze-1 ) {
				addError("Es ist nicht m&ouml;glich, hier mehr als "+grenze+" Unterirdische Komplexe zu installieren");
			
				redirect();
				return;
			}
		}
		
		if( !user.hasResearched(building.getTechRequired()) ) {
			addError("Sie verf&uuml;gen nicht &uuml;ber alle n&ouml;tigen Forschungen um dieses Geb&auml;ude zu bauen");
			
			redirect();
			return;
		}
		
		t.setVar("show.build", 1);
		
		boolean ok = true;
		
		//noetige Resourcen berechnen/anzeigen
		Cargo basecargo = base.getCargo();
		
		t.setBlock("_BUILD", "build.res.listitem", "build.res.list");
		
		ResourceList compreslist = building.getBuildCosts().compare(basecargo, false);
		for( ResourceEntry compres : compreslist ) {
			t.setVar(	"res.image",		compres.getImage(),
						"res.owncargo",		compres.getCargo2(),
						"res.needcargo",	compres.getCargo1(),
						"res.diff",			compres.getDiff() );
			
			if( compres.getDiff() > 0 ) {
				ok = false;
			} 
			
			t.parse("build.res.list", "build.res.listitem", true);
		}

		// Alles OK -> bauen
		if( ok ) {
			base.getBebauung()[field] = build;
			String bebdb = Common.implode("|",base.getBebauung());

			// Muss das Gebaeude aufgrund von Arbeitermangel deaktiviert werden?
			if( (building.getArbeiter() > 0) && (building.getArbeiter()+base.getArbeiter() > base.getBewohner()) ) {
				base.getActive()[field] = 0;

				t.setVar("build.lowworker", 1);
			} 
			else {
				base.getActive()[field] = 1;
			}
			
			String ondb = Common.implode("|",base.getActive());;
		
			// Resourcen abziehen
			basecargo.substractCargo( building.getBuildCosts() );
		
			db.update("UPDATE bases SET active='"+ondb+"',arbeiter=arbeiter+"+building.getArbeiter()+",bebauung='"+bebdb+"',cargo='"+basecargo.save()+"' WHERE id="+base.getID()+" AND cargo='"+basecargo.save(true)+"'");
			if( db.affectedRows() != 0 ) {
				// Evt. muss das Gebaeude selbst noch ein paar Dinge erledigen
				building.build(base);
			}
		}
		
	}
	
	/**
	 * Zeigt die Liste der baubaren Gebaeude, sortiert nach Kategorien, an
	 * @urlparam Integer cat Die anzuzeigende Kategorie
	 */
	@Override
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		parameterNumber("cat");
		
		int cat = getInteger("cat");
		if( (cat < 0) || (cat > 4) ) {
			cat = 0;	
		} 
		
		t.setVar("show.buildinglist", 1);

		//Anzahl der Gebaeude berechnen
		Map<Integer,Integer> buildingcount = new HashMap<Integer,Integer>();
		for( int building : base.getBebauung() ) {
			Common.safeIntInc(buildingcount, building);
		}
	
		//Anzahl der Gebaeude pro Spieler berechnen
		Map<Integer,Integer> ownerbuildingcount = new HashMap<Integer,Integer>(buildingcount);
		
		SQLQuery aBeb = db.query("SELECT bebauung FROM bases WHERE owner="+user.getId()+" AND id!="+base.getID());
		while( aBeb.next() ) {
			int[] aBebList = Common.explodeToInt("|",aBeb.getString("bebauung"));
			for( int bid : aBebList ) {
				Common.safeIntInc(ownerbuildingcount, bid);
			}
		}
		aBeb.free();

		Cargo basecargo = base.getCargo();
	
		//Max UComplex-Gebaeude-Check
		int grenze = (base.getWidth() * base.getHeight())/8;
		
		Set<Integer> ucbuildings = new HashSet<Integer>();
		SQLQuery ucbid = db.query("SELECT id FROM buildings WHERE ucomplex='1'");
		while( ucbid.next() ) {
			ucbuildings.add(ucbid.getInt("id"));
		}
		ucbid.free();
	
		int c = 0;
		for( int bid : buildingcount.keySet() ) {
			int count = buildingcount.get(bid);
			if( ucbuildings.contains(bid) ) {
				c += count;
			}
		}

		boolean ucomplex = c <= grenze-1;

		t.setBlock("_BUILD", "buildings.listitem", "buildings.list");
		t.setBlock("buildings.listitem", "buildings.res.listitem", "buildings.res.list");
		
		//Alle Gebaeude ausgeben
		SQLQuery building = db.query("SELECT * FROM buildings WHERE category='"+cat+"' ORDER BY name");
		while( building.next() ) {
			//Existiert bereits die max. Anzahl dieses Geb. Typs auf dem Asti?
			if( (building.getInt("perplanet") != 0) && buildingcount.containsKey(building.getInt("id")) && 
				(building.getInt("perplanet") <= buildingcount.get(building.getInt("id"))) ) {
				continue;
			}
		
			if( (building.getInt("perowner") != 0) && ownerbuildingcount.containsKey(building.getInt("id")) && 
				(building.getInt("perowner") <= ownerbuildingcount.get(building.getInt("id"))) ) {
				continue;
			}
		
			if( !ucomplex && (building.getInt("ucomplex") != 0) ) {
				continue;
			}

			if( !user.hasResearched(building.getInt("techreq")) ) {
				continue;
			}
			
			Cargo buildcosts = new Cargo(Cargo.Type.STRING, building.getString("buildcosts"));
			
			boolean ok = true;
			
			ResourceList compreslist = buildcosts.compare(basecargo, false);
			for( ResourceEntry compres : compreslist ) {
				if( compres.getDiff() > 0 ) {
					ok = false;
				}
			}

			t.setVar(	"building.picture",		building.getString("picture"),
						"building.name",		Common._plaintitle(building.getString("name")),
						"building.id",			building.getInt("id"),
						"building.buildable", 	ok,
						"buildings.res.list", 	"" );

			//Kosten
			for( ResourceEntry compres : compreslist ) {
				t.setVar(	"res.image",		compres.getImage(),
							"res.cargo",		compres.getCargo1(),
							"res.diff",			compres.getDiff(),
							"res.plainname",	compres.getPlainName() );
				
				t.parse("buildings.res.list", "buildings.res.listitem", true);
			}

			t.parse("buildings.list", "buildings.listitem", true);
		}
		building.free();
	}
}

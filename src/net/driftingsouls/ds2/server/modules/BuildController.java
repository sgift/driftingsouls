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

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParamType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParams;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Der Gebaeudebau.
 * @author Christopher Jung
 */
@Module(name="build")
@UrlParams({
		@UrlParam(name="col", type=UrlParamType.NUMBER, description = "Die Bases, auf der das Gebaeude gebaut werden soll"),
		@UrlParam(name="field", type=UrlParamType.NUMBER, description = "Die ID des Feldes, auf dem das Gebaeude gebaut werden soll")
})
public class BuildController extends TemplateGenerator {
	private Base base;

	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public BuildController(Context context) {
		super(context);

		setTemplate("build.html");

		setPageTitle("Bauen");
	}

	@Override
	protected boolean validateAndPrepare(String action) {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();

		int col = getInteger("col");
		int field = getInteger("field");

		base = (Base)getDB().get(Base.class, col);
		if( (base == null) || (base.getOwner() != user) ) {
			addError("Die angegebene Kolonie existiert nicht", Common.buildUrl("default", "module", "basen"));

			return false;
		}

		t.setVar(	"base.id",		base.getId(),
					"base.name",	Common._plaintitle(base.getName()),
					"global.field",	field );

		return true;
	}

	/**
	 * Baut ein Gebaeute auf der Kolonie.
	 *
	 */
	@UrlParam(name="build", type= UrlParamType.NUMBER, description = "Die ID des zu bauenden Gebaeudes")
	@Action(ActionType.DEFAULT)
	public void buildAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();

		int build = getInteger("build");

		int field = getInteger("field");

		Building building = Building.getBuilding(build);

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
				addError("Sie k&ouml;nnen dieses Geb&auml;de maximal "+building.getPerPlanetCount()+" Mal pro Asteroid bauen");

				redirect();
				return;
			}
		}

		//Anzahl der Gebaeude pro Spieler berechnen
		if( building.getPerUserCount() != 0 ) {
			int ownerbuildingcount = 0;

			for( Base abase : user.getBases() ) {
				for( int bid : abase.getBebauung() ) {
					if( bid == building.getId() ) {
						ownerbuildingcount++;
					}
				}
			}

			if( building.getPerUserCount() <= ownerbuildingcount ) {
				addError("Sie k&ouml;nnen dieses Geb&auml;de maximal "+building.getPerUserCount()+" Mal insgesamt bauen");

				redirect();
				return;
			}
		}

		// Pruefe auf richtiges Terrain
		if( !building.hasTerrain(base.getTerrain()[field]) )
		{
			addError("Dieses Geb&auml;ude ist nicht auf diesem Terrainfeld baubar.");

			redirect();
			return;
		}

		if( base.getBebauung()[field] != 0 ) {
			addError("Es existiert bereits ein Geb&auml;ude an dieser Stelle");

			redirect();
			return;
		}

		if( building.isUComplex() ) {
			int c = 0;
			for( int i =0; i <= base.getWidth() * base.getHeight() -1 ; i++ )
			{
				Building currentBuilding = Building.getBuilding(base.getBebauung()[i]);
				if(currentBuilding != null && currentBuilding.isUComplex())
				{
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
		if( !Rassen.get().rasse(user.getRace()).isMemberIn(building.getRace()) ) {
			addError("Sie geh&ouml;ren der falschen Spezies an und k&ouml;nnen dieses Geb&auml;ude nicht selbst errichten.");
			redirect();
			return;
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
			Integer[] bebauung = base.getBebauung();
			bebauung[field] = build;
			base.setBebauung(bebauung);

			Integer[] active = base.getActive();
			// Muss das Gebaeude aufgrund von Arbeitermangel deaktiviert werden?
			if( (building.getArbeiter() > 0) && (building.getArbeiter()+base.getArbeiter() > base.getBewohner()) ) {
				active[field] = 0;

				t.setVar("build.lowworker", 1);
			}
			else {
				active[field] = 1;
			}

			// Resourcen abziehen
			basecargo.substractCargo( building.getBuildCosts() );

			base.setCargo(basecargo);
			base.setArbeiter(base.getArbeiter()+building.getArbeiter());
			base.setActive(active);

			// Evt. muss das Gebaeude selbst noch ein paar Dinge erledigen
			building.build(base, build);
		}

	}

	/**
	 * Zeigt die Liste der baubaren Gebaeude, sortiert nach Kategorien, an.
	 */
	@UrlParam(name="cat", type=UrlParamType.NUMBER, description = "Die anzuzeigende Kategorie")
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		org.hibernate.Session db = getDB();

		int cat = getInteger("cat");
		int field = getInteger("field");
		if( (cat < 0) || (cat > 4) ) {
			cat = 0;
		}

		t.setVar("show.buildinglist", 1);

		//Anzahl der Gebaeude berechnen
		Map<Integer,Integer> buildingcount = new HashMap<>();
		for( int building : base.getBebauung() ) {
			Common.safeIntInc(buildingcount, building);
		}

		//Anzahl der Gebaeude pro Spieler berechnen
		Map<Integer,Integer> ownerbuildingcount = new HashMap<>(buildingcount);

		for( Base abase : user.getBases() ) {
			if( abase.getId() == this.base.getId() )
			{
				continue;
			}
			for( int bid : abase.getBebauung() ) {
				Common.safeIntInc(ownerbuildingcount, bid);
			}
		}

		Cargo basecargo = base.getCargo();

		//Max UComplex-Gebaeude-Check
		int grenze = (base.getWidth() * base.getHeight())/8;
		int c = 0;

		Iterator<?> ucBuildingIter = db.createQuery("from Building where ucomplex=true").iterate();
		for( ; ucBuildingIter.hasNext(); ) {
			Building building = (Building)ucBuildingIter.next();
			if( buildingcount.containsKey(building.getId()) ) {
				c += buildingcount.get(building.getId());
			}
		}

		boolean ucomplex = c <= grenze-1;

		t.setBlock("_BUILD", "buildings.listitem", "buildings.list");
		t.setBlock("buildings.listitem", "buildings.res.listitem", "buildings.res.list");

		//Alle Gebaeude ausgeben
		Iterator<?> buildingIter = db.createQuery("from Building where category=:cat order by name")
			.setInteger("cat", cat)
			.iterate();
		for( ; buildingIter.hasNext(); ) {
			Building building = (Building)buildingIter.next();
			//Existiert bereits die max. Anzahl dieses Geb. Typs auf dem Asti?
			if( (building.getPerPlanetCount() != 0) && buildingcount.containsKey(building.getId()) &&
				(building.getPerPlanetCount() <= buildingcount.get(building.getId())) ) {
				continue;
			}

			if( (building.getPerUserCount() != 0) && ownerbuildingcount.containsKey(building.getId()) &&
				(building.getPerUserCount() <= ownerbuildingcount.get(building.getId())) ) {
				continue;
			}

			if( !ucomplex && building.isUComplex() ) {
				continue;
			}

			if( !user.hasResearched(building.getTechRequired()) ) {
				continue;
			}
			if( !Rassen.get().rasse(user.getRace()).isMemberIn(building.getRace()) ) {
				continue;
			}
			if( !building.hasTerrain(base.getTerrain()[field]) ) {
				continue;
			}
			Cargo buildcosts = building.getBuildCosts();

			boolean ok = true;

			ResourceList compreslist = buildcosts.compare(basecargo, false, true);
			for( ResourceEntry compres : compreslist ) {
				if( compres.getDiff() > 0 ) {
					ok = false;
				}
			}

			t.setVar(	"building.picture",		building.getPictureForRace(user.getRace()),
						"building.name",		Common._plaintitle(building.getName()),
						"building.id",			building.getId(),
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
	}
}

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
import net.driftingsouls.ds2.server.bases.BaseStatus;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.bases.Core;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateController;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ValidierungException;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.util.*;

/**
 * Verwaltung einer Basis.
 * @author Christopher Jung
 */
@Module(name="base")
public class BaseController extends TemplateController
{
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public BaseController(Context context) {
		super(context);

		setTemplate("base.html");

		setPageTitle("Basis");
	}

	private void validate(Base base) {
		User user = (User)getUser();

		if( (base == null) || (base.getOwner() != user) ) {
			throw new ValidierungException("Die angegebene Kolonie existiert nicht", Common.buildUrl("default", "module", "basen") );
		}

		base.getCargo().setOption( Cargo.Option.LINKCLASS, "schiffwaren" );

		setPageTitle(base.getName());
	}

	/**
	 * Aendert den Namen einer Basis.
	 * @param feeding Der neue Versorgungsstatus der Basis
	 */
	@Action(ActionType.DEFAULT)
	public void changeFeedingAction(@UrlParam(name="col") Base base, int feeding) {
		validate(base);

		TemplateEngine t = getTemplateEngine();

		if( feeding == 0 ) {
			base.setFeeding(false);
			t.setVar("base.message", "Versorgung abgeschaltet!");
		}
		else if( feeding == 1 ) {
			base.setFeeding(true);
			t.setVar("base.message", "Versorgung angeschaltet.");
		}
		else if( feeding == 2 ) {
			base.setLoading(false);
			t.setVar("base.message", "Automatisches auff&uuml;llen abgeschaltet!");
		}
		else if( feeding == 3 ) {
			base.setLoading(true);
			t.setVar("base.message", "Automatisches auff&uuml;llen angeschaltet.");
		}

		redirect();
	}

	/**
	 * Aendert den Versorgungsstatus einer Basis.
	 * @param newname Der neue Name der Basis
	 */
	@Action(ActionType.DEFAULT)
	public void changeNameAction(@UrlParam(name="col") Base base, String newname) {
		validate(base);

		TemplateEngine t = getTemplateEngine();

		if( newname.length() > 50 ) {
			newname = newname.substring(0,50);
		}
		if( newname.trim().isEmpty() )
		{
			newname = "Kolonie "+base.getId();
		}

		base.setName(newname);

		t.setVar("base.message", "Name zu "+Common._plaintitle(newname)+" ge&auml;ndert");

		redirect();
	}

	/**
	 * (de)aktiviert Gebaeudegruppen.
	 * @param act <code>false</code>, wenn die Gebaeude deaktiviert werden sollen. Andernfalls <code>true</code>
	 * @param buildingonoff Die ID des Gebaeudetyps, dessen Gebaeude (de)aktiviert werden sollen
	 */
	@Action(ActionType.DEFAULT)
	public void changeBuildingStatusAction(@UrlParam(name="col") Base base, boolean act, int buildingonoff) {
		validate(base);

		TemplateEngine t = getTemplateEngine();

		int bebstatus = act ? 1 : 0;

		Building building = Building.getBuilding(buildingonoff);

		// Wenn das Gebaude automatisch abschalten soll und der Besitzer
		// die entsprechenden Forschungen oder die Rasse nicht hat
		// bleibt das Gebaeude aus (Rasse != GCP)
		if( bebstatus == 1 && building.isShutDown() &&
				(!base.getOwner().hasResearched(building.getTechRequired())
						|| ((base.getOwner().getRace() != building.getRace()) && building.getRace() != 0)))
		{
			t.setVar("base.message", "<span style=\"color:red\">Sie haben nicht die notwendigen Voraussetzungen um diese Geb&auml;ude aktivieren zu k&ouml;nnen</span>");
		}
		else if( building.isDeakAble() ) {
			int count = 0;
			Integer[] active = base.getActive();

			for( int i=0; i <= base.getWidth()*base.getHeight()-1 ; i++ ) {

				if( (base.getBebauung()[i] == buildingonoff) && (active[i] != bebstatus) ) {
					if( ((bebstatus != 0) && (base.getBewohner() >= base.getArbeiter() + building.getArbeiter())) || (bebstatus == 0) ) {
						active[i] = bebstatus;

						count++;

						if( bebstatus != 0 ) {
							base.setArbeiter(base.getArbeiter()+building.getArbeiter());
						}
					}
				}
			}

			base.setActive(active);

			if( count != 0 ) {
				String result;

				if( bebstatus != 0 ) {
					result = "<span style=\"color:green\">";
				}
				else {
					result = "<span style=\"color:red\">";
				}
				result += count+" Geb&auml;ude wurde"+(count > 1 ? "n" : "")+' '+(bebstatus != 0 ? "" : "de")+"aktiviert</span>";

				t.setVar("base.message", result);
			}
		}
		else {
			t.setVar("base.message", "<span style=\"color:red\">Sie k&ouml;nnen diese Geb&auml;ude nicht deaktivieren</span>");
		}

		redirect();
	}

	@Override
	protected void printHeader() throws IOException {
		TemplateEngine t = getTemplateEngine();

		// TODO: Diese gesamte Methode gehoert eliminiert. Es muss (und wird)
		// einen Weg geben mit einem festen Satz an CSS-Stilen fuer beliebig grosse
		// Basen auszukommen
		t.setBlock("_BASE", "header", "none" );
		t.setBlock("header", "tiles.listitem", "tiles.list");

		int colId = getContext().getRequest().getParameterInt("col");
		Base base = (Base)getDB().get(Base.class, colId);
		if( base == null || base.getOwner() != getUser() )
		{
			return;
		}

		int topOffset = 5;
		int leftOffset = 5;

		for( int i = 0; i < base.getHeight(); i++ ) {
			for( int j = 0; j < base.getWidth(); j++ ) {
				int top = topOffset + ((j % 2)+ i * 2) * 22;
	   			int left = leftOffset + j * 39;

   				t.setVar(	"tile.id",		base.getWidth()*i+j,
   							"tile.top",		top,
   							"tile.left",	left );

   				t.parse("tiles.list", "tiles.listitem", true);
			}

		}

		t.parse("__HEADER","header");

		super.printHeader();
	}

	/**
	 * Zeigt die Basis an.
	 */
	@Action(ActionType.AJAX)
	public JSONObject ajaxAction(@UrlParam(name="col") Base base) {
		validate(base);

		User user = (User)getUser();

		JSONObject response = new JSONObject();
		response.accumulate("col", base.getId());

		JSONObject baseObj = new JSONObject();
		baseObj
			.accumulate("id", base.getId())
			.accumulate("name", Common._plaintitle(base.getName()))
			.accumulate("x", base.getX())
			.accumulate("y", base.getY())
			.accumulate("system", base.getSystem())
			.accumulate("feeding", base.isFeeding())
			.accumulate("loading", base.isLoading())
			.accumulate("width", base.getWidth());

		if( base.getCore() > 0 ) {
			Core core = Core.getCore(base.getCore());

			JSONObject coreObj = new JSONObject()
				.accumulate("id", core.getId())
				.accumulate("name", Common._plaintitle(core.getName()))
				.accumulate("active", base.isCoreActive());

			baseObj.accumulate("core", coreObj);
		}

		BaseStatus basedata = Base.getStatus(base);

		ResourceList reslist = base.getCargo().compare(basedata.getProduction(), true,true);
		reslist.sortByID(false);

		baseObj
			.accumulate("cargo", reslist.toJSON())
			.accumulate("cargoBilanz", -basedata.getProduction().getMass())
			.accumulate("cargoFrei", base.getMaxCargo() - base.getCargo().getMass())
			.accumulate("einheiten", base.getUnits().toJSON())
			.accumulate("energyProduced", basedata.getEnergy())
			.accumulate("energy", base.getEnergy())
			.accumulate("bewohner", base.getBewohner())
			.accumulate("arbeiter", base.getArbeiter())
			.accumulate("arbeiterErforderlich", basedata.getArbeiter())
			.accumulate("wohnraum", basedata.getLivingSpace());

		response.accumulate("base", baseObj);

		//----------------
		// Karte
		//----------------

		JSONArray mapObj = new JSONArray();

		Map<Integer,Integer> buildingonoffstatus = new TreeMap<>(new BuildingComparator());

		for( int i = 0; i < base.getWidth() * base.getHeight(); i++ ) {
			JSONObject feld = new JSONObject();

			//Leeres Feld
			if( base.getBebauung()[i] != 0 ) {
				JSONObject gebaeudeObj = new JSONObject();

				Building building = Building.getBuilding(base.getBebauung()[i]);
				base.getActive()[i] = basedata.getActiveBuildings()[i];

				if( !buildingonoffstatus.containsKey(base.getBebauung()[i]) ) {
					buildingonoffstatus.put(base.getBebauung()[i], 0);
				}
				if( building.isDeakAble() ) {
					if( buildingonoffstatus.get(base.getBebauung()[i]) == 0 ) {
						buildingonoffstatus.put( base.getBebauung()[i], base.getActive()[i] + 1 );
					}
					else if( buildingonoffstatus.get(base.getBebauung()[i]) != base.getActive()[i] + 1 ) {
						buildingonoffstatus.put(base.getBebauung()[i],-1);
					}
				}

				gebaeudeObj.accumulate("name", Common._plaintitle(building.getName()))
					.accumulate("id", building.getId())
					.accumulate("supportsJSON", building.isSupportsJson())
					.accumulate("deaktiviert", building.isDeakAble() && (base.getActive()[i] == 0))
					.accumulate("grafik", building.getPictureForRace(user.getRace()));

				feld.accumulate("gebaeude", gebaeudeObj);
			}

			feld.accumulate("feld", i)
				.accumulate("terrain", base.getTerrain()[i]);

			mapObj.add(feld);
		}

		response.accumulate("karte", mapObj);

		//----------------
		// Aktionen
		//----------------

		JSONArray buildingStatus = new JSONArray();
		for( Map.Entry<Integer,Integer> entry : buildingonoffstatus.entrySet() ) {
			int bstatus = entry.getValue();
			Building building = Building.getBuilding(entry.getKey());

			JSONObject buildingObj = new JSONObject();
			buildingObj
				.accumulate("name", Common._plaintitle(building.getName()))
				.accumulate("id", entry.getKey())
				.accumulate("aktivierbar", (bstatus == -1) || (bstatus == 1))
				.accumulate("deaktivierbar", (bstatus == -1) || (bstatus == 2));

			buildingStatus.add(buildingObj);
		}

		response.accumulate("gebaeudeStatus", buildingStatus);

		return response;
	}

	/**
	 * Zeigt die Basis an.
	 */
	@Action(ActionType.DEFAULT)
	public void defaultAction(@UrlParam(name="col") Base base) {
		validate(base);

		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();

		int mapheight = (1 + base.getHeight() * 2) * 22+25;

		t.setVar(	"base.id",				base.getId(),
					"base.name",			Common._plaintitle(base.getName()),
					"base.x",				base.getX(),
					"base.y",				base.getY(),
					"base.system",			base.getSystem(),
					"base.core",			base.getCore(),
					"base.core.active",		base.isCoreActive(),
					"base.isfeeding",		base.isFeeding(),
					"base.isloading",		base.isLoading(),
					"base.map.width",		(base.getWidth()*39+20 > 410 ? 410 : base.getWidth()*39+20),
					"base.cargo.height",	(mapheight < 280 ? "280" : mapheight),
					"base.cargo.empty",		Common.ln(base.getMaxCargo() - base.getCargo().getMass()) );

		BaseStatus basedata = Base.getStatus(base);

		//------------------
		// Core
		//------------------
		if( base.getCore() > 0 ) {
			Core core = Core.getCore(base.getCore());
			t.setVar( "core.name", Common._plaintitle(core.getName()) );
		}


		//----------------
		// Karte
		//----------------

		Map<Integer,Integer> buildingonoffstatus = new TreeMap<>(new BuildingComparator());

		t.setBlock("_BASE", "base.map.listitem", "base.map.list");

		for( int i = 0; i < base.getWidth() * base.getHeight(); i++ ) {
			t.start_record();

			String image;

			//Leeres Feld
			if( base.getBebauung()[i] == 0 ) {
				image = "data/buildings/ground"+base.getTerrain()[i]+".png";
				base.getActive()[i] = 2;
			}
			else {
				Building building = Building.getBuilding(base.getBebauung()[i]);
				base.getActive()[i] = basedata.getActiveBuildings()[i];

				if( !buildingonoffstatus.containsKey(base.getBebauung()[i]) ) {
					buildingonoffstatus.put(base.getBebauung()[i], 0);
				}
				if( building.isDeakAble() ) {

					if( buildingonoffstatus.get(base.getBebauung()[i]) == 0 ) {
						buildingonoffstatus.put( base.getBebauung()[i], base.getActive()[i] + 1 );
					}
					else if( buildingonoffstatus.get(base.getBebauung()[i]) != base.getActive()[i] + 1 ) {
						buildingonoffstatus.put(base.getBebauung()[i],-1);
					}
				}

				image = building.getPictureForRace(user.getRace());

				if( building.isDeakAble() && (base.getActive()[i] == 0) ) {
					t.setVar(	"tile.overlay",			1,
								"tile.overlay.image",	"overlay_offline.png" );
				}

				t.setVar(	"tile.building",		1,
							"tile.building.name", Common._plaintitle(building.getName()),
							"tile.building.id", building.getId(),
							"tile.building.json", building.isSupportsJson());
			}

			t.setVar(	"tile.field",			i,
						"tile.building.image",	image,
						"tile.id",				i );

			t.parse("base.map.list", "base.map.listitem", true);

			t.stop_record();
			t.clear_record();
		}

		//----------------
		// Waren
		//----------------

		base.setArbeiter(basedata.getArbeiter());

		ResourceList reslist = base.getCargo().compare(basedata.getProduction(), true,true);

		reslist.sortByID(false);

		t.setBlock("_BASE", "base.cargo.listitem", "base.cargo.list");

		for( ResourceEntry res : reslist ) {
			t.setVar(	"res.name",		res.getName(),
						"res.image",	res.getImage(),
						"res.cargo1",	res.getCargo1(),
						"res.cargo2",	res.getCargo2(),
						"res.plaincount2",	res.getCount2() );

			t.parse("base.cargo.list", "base.cargo.listitem", true);
		}

		long cstat = -basedata.getProduction().getMass();

		t.setVar(	"base.cstat",		Common.ln(cstat),
					"base.e",			Common.ln(base.getEnergy()),
					"base.estat",		Common.ln(basedata.getEnergy()),
					"base.bewohner",	Common.ln(base.getBewohner()),
					"base.arbeiter.needed",	Common.ln(basedata.getArbeiter()),
					"base.wohnraum",		Common.ln(basedata.getLivingSpace()) );

		//----------------
		// Aktionen
		//----------------

		t.setBlock("_BASE", "base.massonoff.listitem", "base.massonoff.list");


		for( Map.Entry<Integer,Integer> entry : buildingonoffstatus.entrySet() ) {
			int bstatus = entry.getValue();

			Building building = Building.getBuilding(entry.getKey());
			t.setVar(	"building.name",	Common._plaintitle(building.getName()),
						"building.id",		entry.getKey(),
						"building.allowoff",	(bstatus == -1) || (bstatus == 2),
						"building.allowon",	(bstatus == -1) || (bstatus == 1) );

			t.parse("base.massonoff.list", "base.massonoff.listitem", true);
		}

		//-----------------------------------------
		// Energieverbrauch, Bevoelkerung, Einheiten usw.
		//------------------------------------------

		if(!base.getUnits().isEmpty())
		{
			t.setBlock("_BASE","base.units.listitem","base.units.list");

			t.setVar( "base.marines", true);
			base.getUnits().echoUnitList(t, "base.units.list", "base.units.listitem");
		}

		double summeWohnen = Math.max(base.getBewohner(),basedata.getLivingSpace());
		long arbeiterProzent = Math.round(basedata.getArbeiter()/summeWohnen*100);
		long arbeitslosProzent = Math.max(Math.round((base.getBewohner()-basedata.getArbeiter())/summeWohnen*100),0);
		long wohnraumFreiProzent = Math.max(Math.round((basedata.getLivingSpace()-base.getBewohner())/summeWohnen*100),0);
		long wohnraumFehltProzent = Math.max(Math.round((base.getBewohner()-basedata.getLivingSpace())/summeWohnen*100),0);
		long prozent = arbeiterProzent+arbeitslosProzent+wohnraumFehltProzent+wohnraumFreiProzent;
		if( prozent > 100 ) {
			long remaining = prozent-100;
			long diff = Math.min(remaining,arbeiterProzent);
			arbeiterProzent -= diff;
			remaining -= diff;
			if( remaining > 0 ) {
				arbeitslosProzent -= remaining;
			}
		}

		t.setVar(
			"arbeiterProzent", arbeiterProzent,
			"arbeitslosProzent", arbeitslosProzent,
			"wohnraumFreiProzent", wohnraumFreiProzent,
			"wohnraumFehltProzent", wohnraumFehltProzent);
	}

	private static class BuildingComparator implements Comparator<Integer> {

		@Override
		public int compare(Integer o1, Integer o2) {
			int diff = Building.getBuilding(o1).getName().compareTo(Building.getBuilding(o2).getName());
			if( diff != 0 )
			{
				return diff;
			}
			return o1.compareTo(o2);
		}
	}
}

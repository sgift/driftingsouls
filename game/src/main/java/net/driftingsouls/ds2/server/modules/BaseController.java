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
import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.RedirectViewResult;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ValidierungException;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.modules.viewmodels.GebaeudeAufBasisViewModel;
import net.driftingsouls.ds2.server.modules.viewmodels.ResourceEntryViewModel;
import net.driftingsouls.ds2.server.modules.viewmodels.UnitCargoEntryViewModel;
import net.driftingsouls.ds2.server.units.UnitCargoEntry;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Verwaltung einer Basis.
 * @author Christopher Jung
 */
@Module(name="base")
public class BaseController extends Controller
{
	private TemplateViewResultFactory templateViewResultFactory;

	@Autowired
	public BaseController(TemplateViewResultFactory templateViewResultFactory) {
		this.templateViewResultFactory = templateViewResultFactory;

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
	public RedirectViewResult changeFeedingAction(@UrlParam(name = "col") Base base, int feeding) {
		validate(base);

		String message = null;
		switch (feeding)
		{
			case 0:
				base.setFeeding(false);
				message = "Versorgung abgeschaltet!";
				break;
			case 1:
				base.setFeeding(true);
				message = "Versorgung angeschaltet.";
				break;
			case 2:
				base.setLoading(false);
				message = "Automatisches auffüllen abgeschaltet!";
				break;
			case 3:
				base.setLoading(true);
				message = "Automatisches auffüllen angeschaltet.";
				break;
		}

		return new RedirectViewResult("default").withMessage(message);
	}

	/**
	 * Aendert den Versorgungsstatus einer Basis.
	 * @param newname Der neue Name der Basis
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult changeNameAction(@UrlParam(name = "col") Base base, String newname) {
		validate(base);

		if( newname.length() > 50 ) {
			newname = newname.substring(0,50);
		}
		if( newname.trim().isEmpty() )
		{
			newname = "Kolonie "+base.getId();
		}

		base.setName(newname);

		return new RedirectViewResult("default").withMessage("Name zu " + Common._plaintitle(newname) + " geändert");
	}

	/**
	 * (de)aktiviert Gebaeudegruppen.
	 * @param act <code>false</code>, wenn die Gebaeude deaktiviert werden sollen. Andernfalls <code>true</code>
	 * @param buildingonoff Die ID des Gebaeudetyps, dessen Gebaeude (de)aktiviert werden sollen
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult changeBuildingStatusAction(@UrlParam(name = "col") Base base, boolean act, int buildingonoff) {
		validate(base);

		int bebstatus = act ? 1 : 0;

		Building building = Building.getBuilding(buildingonoff);

		String message = null;
		// Wenn das Gebaude automatisch abschalten soll und der Besitzer
		// die entsprechenden Forschungen oder die Rasse nicht hat
		// bleibt das Gebaeude aus (Rasse != GCP)
		if( bebstatus == 1 && building.isShutDown() &&
				(!base.getOwner().hasResearched(building.getTechRequired())
						|| ((base.getOwner().getRace() != building.getRace()) && building.getRace() != 0)))
		{
			message = "<span style=\"color:red\">Sie haben nicht die notwendigen Voraussetzungen um diese Gebäude aktivieren zu können</span>";
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
				result += count+" Gebäude wurde"+(count > 1 ? "n" : "")+' '+(bebstatus != 0 ? "" : "de")+"aktiviert</span>";

				message = result;
			}
		}
		else {
			message = "<span style=\"color:red\">Sie können diese Gebäude nicht deaktivieren</span>";
		}

		return new RedirectViewResult("default").withMessage(message);
	}

	@ViewModel
	public static class AjaxViewModel
	{
		public static class BaseViewModel
		{
			public int id;
			public String name;
			public int x;
			public int y;
			public int system;
			public boolean feeding;
			public int width;
			public boolean loading;
			public long cargoBilanz;
			public long cargoFrei;
			public int energyProduced;
			public int energy;
			public int bewohner;
			public int arbeiter;
			public int arbeiterErforderlich;
			public int wohnraum;
			public List<ResourceEntryViewModel> cargo = new ArrayList<>();
			public List<UnitCargoEntryViewModel> einheiten = new ArrayList<>();
			public CoreViewModel core;
		}

		public static class CoreViewModel
		{
			public int id;
			public String name;
			public boolean active;
		}

		public static class FeldViewModel
		{
			public int feld;
			public Integer terrain;
			public GebaeudeAufBasisViewModel gebaeude;
		}

		public static class GebaeudeStatusViewModel
		{
			public String name;
			public int id;
			public boolean aktivierbar;
			public boolean deaktivierbar;
		}

		public int col;
		public BaseViewModel base;
		public List<FeldViewModel> karte = new ArrayList<>();
		public List<GebaeudeStatusViewModel> gebaeudeStatus = new ArrayList<>();
	}

	/**
	 * Zeigt die Basis an.
	 */
	@Action(ActionType.AJAX)
	public AjaxViewModel ajaxAction(@UrlParam(name="col") Base base) {
		validate(base);

		AjaxViewModel response = new AjaxViewModel();
		response.col = base.getId();

		AjaxViewModel.BaseViewModel baseObj = new AjaxViewModel.BaseViewModel();
		baseObj.id = base.getId();
		baseObj.name = Common._plaintitle(base.getName());
		baseObj.x = base.getX();
		baseObj.y = base.getY();
		baseObj.system = base.getSystem();
		baseObj.feeding = base.isFeeding();
		baseObj.loading = base.isLoading();
		baseObj.width = base.getWidth();

		if( base.getCore() != null ) {
			Core core = base.getCore();

			AjaxViewModel.CoreViewModel coreObj = new AjaxViewModel.CoreViewModel();
			coreObj.id = core.getId();
			coreObj.name = Common._plaintitle(core.getName());
			coreObj.active = base.isCoreActive();

			baseObj.core = coreObj;
		}

		BaseStatus basedata = Base.getStatus(base);

		ResourceList reslist = base.getCargo().compare(basedata.getProduction(), true,true);
		reslist.sortByID(false);

		for (ResourceEntry resourceEntry : reslist)
		{
			baseObj.cargo.add(ResourceEntryViewModel.map(resourceEntry));
		}
		for (UnitCargoEntry unitCargoEntry : base.getUnits().getUnitList())
		{
			baseObj.einheiten.add(UnitCargoEntryViewModel.map(unitCargoEntry));
		}

		baseObj.cargoBilanz = -basedata.getProduction().getMass();
		baseObj.cargoFrei = base.getMaxCargo() - base.getCargo().getMass();
		baseObj.energyProduced = basedata.getEnergy();
		baseObj.energy = base.getEnergy();
		baseObj.bewohner = base.getBewohner();
		baseObj.arbeiter = base.getArbeiter();
		baseObj.arbeiterErforderlich = basedata.getArbeiter();
		baseObj.wohnraum = basedata.getLivingSpace();

		response.base = baseObj;

		//----------------
		// Karte
		//----------------

		Map<Integer,Integer> buildingonoffstatus = new TreeMap<>(new BuildingComparator());

		for( int i = 0; i < base.getWidth() * base.getHeight(); i++ ) {
			AjaxViewModel.FeldViewModel feld = new AjaxViewModel.FeldViewModel();

			//Leeres Feld
			if( base.getBebauung()[i] != 0 ) {
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

				feld.gebaeude = GebaeudeAufBasisViewModel.map(building, base, i);
			}

			feld.feld = i;
			feld.terrain = base.getTerrain()[i];

			response.karte.add(feld);
		}

		//----------------
		// Aktionen
		//----------------

		for( Map.Entry<Integer,Integer> entry : buildingonoffstatus.entrySet() ) {
			int bstatus = entry.getValue();
			Building building = Building.getBuilding(entry.getKey());

			AjaxViewModel.GebaeudeStatusViewModel buildingObj = new AjaxViewModel.GebaeudeStatusViewModel();
			buildingObj.name = Common._plaintitle(building.getName());
			buildingObj.id = entry.getKey();
			buildingObj.aktivierbar = (bstatus == -1) || (bstatus == 1);
			buildingObj.deaktivierbar = (bstatus == -1) || (bstatus == 2);

			response.gebaeudeStatus.add(buildingObj);
		}

		return response;
	}

	/**
	 * Zeigt die Basis an.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction(@UrlParam(name = "col") Base base, RedirectViewResult redirect) {
		validate(base);

		TemplateEngine t = templateViewResultFactory.createFor(this);

		User user = (User)getUser();

		int mapheight = (1 + base.getHeight() * 2) * 22+25;

		t.setVar("base.id",	 base.getId(),
				"base.name", Common._plaintitle(base.getName()),
				"base.x", base.getX(),
				"base.y", base.getY(),
				"base.system", base.getSystem(),
				"base.core", base.getCore() != null ? base.getCore().getId() : 0,
				"base.core.active", base.isCoreActive(),
				"base.isfeeding", base.isFeeding(),
				"base.isloading", base.isLoading(),
				"base.map.width", base.getWidth()*39+20,
				"base.cargo.height", (mapheight < 280 ? "280" : mapheight),
				"base.cargo.empty",	Common.ln(base.getMaxCargo() - base.getCargo().getMass()),
				"base.message", redirect != null ? redirect.getMessage() : null);

		BaseStatus basedata = Base.getStatus(base);

		//------------------
		// Core
		//------------------
		if( base.getCore() != null ) {
			Core core = base.getCore();
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

			t.setVar("tile.field", i,
					"tile.building.image", image,
					"tile.id", i,
					"tile.beginrow", i%base.getWidth() == 0,
					"tile.endrow", (i+1)%base.getWidth() == 0);

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

		return t;
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

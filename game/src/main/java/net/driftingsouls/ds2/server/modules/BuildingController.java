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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.bases.AcademyBuilding;
import net.driftingsouls.ds2.server.bases.AcademyQueueEntry;
import net.driftingsouls.ds2.server.bases.AutoGTUAction;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseStatus;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.bases.Fabrik;
import net.driftingsouls.ds2.server.bases.Fabrik.ContextVars;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.Offiziere;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.Munitionsbauplan;
import net.driftingsouls.ds2.server.entities.Academy;
import net.driftingsouls.ds2.server.entities.Factory;
import net.driftingsouls.ds2.server.entities.FactoryEntry;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.Forschungszentrum;
import net.driftingsouls.ds2.server.entities.GtuWarenKurse;
import net.driftingsouls.ds2.server.entities.Kaserne;
import net.driftingsouls.ds2.server.entities.KaserneEntry;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserMoneyTransfer;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.entities.statistik.StatVerkaeufe;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.framework.authentication.JavaSession;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
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
import net.driftingsouls.ds2.server.services.BaseService;
import net.driftingsouls.ds2.server.services.BuildingService;
import net.driftingsouls.ds2.server.services.CargoService;
import net.driftingsouls.ds2.server.services.DismantlingService;
import net.driftingsouls.ds2.server.services.FleetMgmtService;
import net.driftingsouls.ds2.server.services.PmService;
import net.driftingsouls.ds2.server.services.ShipActionService;
import net.driftingsouls.ds2.server.services.ShipyardService;
import net.driftingsouls.ds2.server.services.UserService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.units.UnitType;
import net.driftingsouls.ds2.server.werften.BaseWerft;
import net.driftingsouls.ds2.server.werften.WerftGUI;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Die Gebaeudeansicht.
 * @author Christopher Jung
 */
@Module(name="building")
public class BuildingController extends Controller
{
	@PersistenceContext
	private EntityManager em;

	private final Map<String, OutputFunction> outputHandler;
	private final ShipyardService shipyardService;
	private final Rassen races;
	private final TemplateViewResultFactory templateViewResultFactory;
	private final JavaSession javaSession;
	private final BuildingService buildingService;
	private final PmService pmService;
	private final BBCodeParser bbCodeParser;
	private final UserService userService;
	private final FleetMgmtService fleetMgmtService;
	private final CargoService cargoService;
	private final DismantlingService dismantlingService;
	private final ShipActionService shipActionService;
	private final BaseService baseService;

	@ViewModel
	public static class BuildingActionViewModel
	{
		public int col;
		public int field;
		public GebaeudeAufBasisViewModel building;
		public boolean success;
		public String message;
	}

	public BuildingController(ShipyardService shipyardService, Rassen races, TemplateViewResultFactory templateViewResultFactory, JavaSession javaSession, PmService pmService, BuildingService buildingService, PmService pmService1, BBCodeParser bbCodeParser, UserService userService, FleetMgmtService fleetMgmtService, CargoService cargoService, DismantlingService dismantlingService, ShipActionService shipActionService, BaseService baseService) {
		this.shipyardService = shipyardService;
		this.races = races;
		this.templateViewResultFactory = templateViewResultFactory;
		this.javaSession = javaSession;
		this.buildingService = buildingService;
		this.pmService = pmService1;
		this.bbCodeParser = bbCodeParser;
		this.userService = userService;
		this.fleetMgmtService = fleetMgmtService;
		this.cargoService = cargoService;
		this.dismantlingService = dismantlingService;
		this.shipActionService = shipActionService;
		this.baseService = baseService;
		setPageTitle("Gebäude");

		outputHandler = Map.of(
			"net.driftingsouls.ds2.server.bases.Werft", this::shipyardOutput,
			"net.driftingsouls.ds2.server.bases.DefaultBuilding", this::defaultOutput,
			"net.driftingsouls.ds2.server.bases.ForschungszentrumBuilding", this::researchCenterOutput,
			"net.driftingsouls.ds2.server.bases.Kommandozentrale", this::commandPostOutput,
			"net.driftingsouls.ds2.server.bases.AcademyBuilding", this::academyOutput,
			"net.driftingsouls.ds2.server.bases.Kaserne", this::kasernenOutput,
			"net.driftingsouls.ds2.server.bases.Fabrik", this::factoryOutput
		);
	}

	public Building getGebaeudeFuerFeld(Base basis, int feld)
	{
		return buildingService.getBuilding(basis.getBebauung()[feld]);
	}

	public void validiereBasisUndFeld(Base basis, int feld)
	{
		User user = (User) getUser();
		if ((basis == null) || (basis.getOwner() != user))
		{
			throw new ValidierungException("Die angegebene Kolonie existiert nicht", Common.buildUrl("default", "module", "basen"));
		}

		if ((feld >= basis.getBebauung().length) || (basis.getBebauung()[feld] == 0))
		{
			throw new ValidierungException("Es existiert kein Gebäude an dieser Stelle");
		}
	}

	/**
	 * Aktiviert das Gebaeude.
	 * @param base Die Basis
	 * @param field Die ID des Feldes auf dem das Gebaeude steht
	 *
	 */
	@Action(ActionType.AJAX)
	public BuildingActionViewModel startAjaxAction(@UrlParam(name = "col") Base base, int field)
	{
		validiereBasisUndFeld(base, field);
		Building building = getGebaeudeFuerFeld(base, field);

		BuildingActionViewModel response = new BuildingActionViewModel();
		response.col = base.getId();
		response.field = field;
		response.building = GebaeudeAufBasisViewModel.map(building, base, field);

		if ((building.getArbeiter() > 0) && (building.getArbeiter() + base.getArbeiter() > base.getBewohner()))
		{
			response.success = false;
			response.message = "Nicht genügend Arbeiter vorhanden";
		}
		else if (building.isShutDown() &&
				(!base.getOwner().hasResearched(building.getTechRequired())
						|| (base.getOwner().getRace() != building.getRace() && building.getRace() != 0)))
		{
			response.success = false;
			response.message = "Sie können dieses Gebäude wegen unzureichender Voraussetzungen nicht aktivieren";
		}
		else
		{
			Integer[] active = base.getActive();
			active[field] = 1;
			base.setActive(active);

			base.setArbeiter(base.getArbeiter() + building.getArbeiter());

			response.success = true;
		}

		return response;
	}

	/**
	 * Aktiviert das Gebaeude.
	 * @param base Die Basis
	 * @param field Die ID des Feldes auf dem das Gebaeude steht
	 * @throws IOException
	 *
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult startAction(@UrlParam(name = "col") Base base, int field) throws IOException
	{
		validiereBasisUndFeld(base, field);
		Building building = getGebaeudeFuerFeld(base, field);

		Writer echo = getResponse().getWriter();

		if ((building.getArbeiter() > 0) && (building.getArbeiter() + base.getArbeiter() > base.getBewohner()))
		{
			echo.append("<span style=\"color:#ff0000\">Nicht genügend Arbeiter vorhanden</span><br /><br />\n");
		}
		else if (building.isShutDown() &&
				(!base.getOwner().hasResearched(building.getTechRequired())
						|| (base.getOwner().getRace() != building.getRace() && building.getRace() != 0)))
		{
			echo.append("<span style=\"color:#ff0000\">Sie können dieses Gebäude wegen unzureichender Voraussetzungen nicht aktivieren</span><br /><br />\n");
		}
		else
		{
			Integer[] active = base.getActive();
			active[field] = 1;
			base.setActive(active);

			base.setArbeiter(base.getArbeiter() + building.getArbeiter());

			echo.append("<span style=\"color:#00ff00\">Gebäude aktiviert</span><br /><br />\n");
		}

		return new RedirectViewResult("default");
	}

	/**
	 * Deaktiviert das Gebaeude.
	 * @param base Die Basis
	 * @param field Die ID des Feldes auf dem das Gebaeude steht
	 *
	 */
	@Action(ActionType.AJAX)
	public BuildingActionViewModel shutdownAjaxAction(@UrlParam(name = "col") Base base, int field)
	{
		validiereBasisUndFeld(base, field);
		Building building = getGebaeudeFuerFeld(base, field);

		BuildingActionViewModel response = new BuildingActionViewModel();
		response.col = base.getId();
		response.field = field;
		response.building = GebaeudeAufBasisViewModel.map(building, base, field);

		if (!building.isDeakAble())
		{
			response.success = false;
			response.message = "Sie können dieses Gebäude nicht deaktivieren";
		}
		else
		{
			Integer[] active = base.getActive();
			active[field] = 0;
			base.setActive(active);

			base.setArbeiter(base.getArbeiter() - building.getArbeiter());

			response.success = true;
		}

		return response;
	}

	/**
	 * Deaktiviert das Gebaeude.
	 * @param base Die Basis
	 * @param field Die ID des Feldes auf dem das Gebaeude steht
	 * @throws IOException
	 *
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult shutdownAction(@UrlParam(name = "col") Base base, int field) throws IOException
	{
		validiereBasisUndFeld(base, field);
		Building building = getGebaeudeFuerFeld(base, field);

		Writer echo = getResponse().getWriter();

		if (!building.isDeakAble())
		{
			echo.append("<span style=\"color:red\">Sie können dieses Gebäude nicht deaktivieren</span>\n");
		}
		else
		{
			Integer[] active = base.getActive();
			active[field] = 0;
			base.setActive(active);

			base.setArbeiter(base.getArbeiter() - building.getArbeiter());

			echo.append("<span style=\"color:#ff0000\">Gebäude deaktiviert</span><br /><br />\n");
		}

		return new RedirectViewResult("default");
	}

	@ViewModel
	public static class DemoViewModel
	{
		public static class DemoResourceEntryViewModel extends ResourceEntryViewModel
		{
			public boolean spaceMissing;
		}

		public int col;
		public int field;
		public final List<DemoResourceEntryViewModel> demoCargo = new ArrayList<>();
		public boolean success;
	}

	/**
	 * Reisst das Gebaeude ab.
	 * @param base Die Basis
	 * @param field Die ID des Feldes auf dem das Gebaeude steht
	 *
	 */
	@Action(ActionType.AJAX)
	public DemoViewModel demoAjaxAction(@UrlParam(name="col") Base base, int field) {
		validiereBasisUndFeld(base, field);
		Building building = getGebaeudeFuerFeld(base, field);

		DemoViewModel response = new DemoViewModel();
		response.col = base.getId();
		response.field = field;

		Cargo buildcosts =(Cargo)building.getBuildCosts().clone();
		buildcosts.multiply( 0.8, Cargo.Round.FLOOR );

		ResourceList reslist = buildcosts.getResourceList();
		Cargo addcargo = cargoService.cutCargo(buildcosts, base.getMaxCargo()-cargoService.getMass(base.getCargo()));

		for( ResourceEntry res : reslist ) {
			DemoViewModel.DemoResourceEntryViewModel resObj = new DemoViewModel.DemoResourceEntryViewModel();

			ResourceEntryViewModel.map(res, resObj);

			if( !addcargo.hasResource(res.getId()) ) {
				resObj.spaceMissing = true;
			}
			response.demoCargo.add(resObj);
		}

		Cargo baseCargo = base.getCargo();
		baseCargo.addCargo( addcargo );
		base.setCargo(baseCargo);

		Integer[] bebauung = base.getBebauung();

		buildingService.cleanup(building, base, bebauung[field]);

		bebauung[field] = 0;
		base.setBebauung(bebauung);

		Integer[] active = base.getActive();
		active[field] = 0;
		base.setActive(active);

		response.success = true;

		return response;
	}

	/**
	 * Reisst das Gebaeude ab.
	 * @param base Die Basis
	 * @param field Die ID des Feldes auf dem das Gebaeude steht
	 * @param conf Falls "ok" bestaetigt dies den Abriss
	 * @throws IOException
	 */
	@Action(ActionType.DEFAULT)
	public void demoAction(@UrlParam(name="col") Base base, int field, String conf) throws IOException {
		validiereBasisUndFeld(base, field);
		Building building = getGebaeudeFuerFeld(base, field);

		User user = (User)getUser();
		Writer echo = getResponse().getWriter();

		echo.append("<div class='gfxbox' style='width:470px'>");

		if( !conf.equals("ok") ) {
			echo.append("<div align=\"center\">\n");
			echo.append("<img align=\"middle\" src=\"./").append(building.getPictureForRace(user.getRace())).append("\" alt=\"\" /> ").append(Common._plaintitle(building.getName())).append("<br /><br />\n");
			echo.append("Wollen Sie dieses Gebäude wirklich demontieren?<br /><br />\n");
			echo.append("<a class=\"error\" href=\"").append(Common.buildUrl("demo", "col", base.getId(), "field", field, "conf", "ok")).append("\">demontieren</a><br /></div>");
			echo.append("</div>");

			echo.append("<br />\n");
			echo.append("<a class=\"back\" href=\"").append(Common.buildUrl("default", "module", "base", "col", base.getId())).append("\">zurück</a><br />\n");

			return;
		}

		Cargo buildcosts =(Cargo)building.getBuildCosts().clone();
		buildcosts.multiply( 0.8, Cargo.Round.FLOOR );

		echo.append("<div align=\"center\">Rückerstattung:</div><br />\n");
		ResourceList reslist = buildcosts.getResourceList();
		Cargo addcargo = cargoService.cutCargo(buildcosts, base.getMaxCargo()-cargoService.getMass(base.getCargo()));

		for( ResourceEntry res : reslist ) {
			echo.append("<img src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCargo1());
			if( !addcargo.hasResource(res.getId()) ) {
				echo.append(" - <span style=\"color:red\">Nicht genügend Platz für alle Waren</span>");
			}
			echo.append("<br />\n");
		}

		Cargo baseCargo = base.getCargo();
		baseCargo.addCargo( addcargo );
		base.setCargo(baseCargo);

		Integer[] bebauung = base.getBebauung();

		buildingService.cleanup(building, base, bebauung[field]);

		bebauung[field] = 0;
		base.setBebauung(bebauung);

		Integer[] active = base.getActive();
		active[field] = 0;
		base.setActive(active);

		echo.append("<br />\n");
		echo.append("<hr noshade=\"noshade\" size=\"1\" style=\"color:#cccccc\" /><br />\n");
		echo.append("<div align=\"center\"><span style=\"color:#ff0000\">Das Gebäude wurde demontiert</span></div>\n");
		echo.append("</div>");

		echo.append("<br />\n");
		echo.append("<a class=\"back\" href=\"").append(Common.buildUrl("default", "module", "base", "col", base.getId())).append("\">zurück</a><br />\n");
	}

	@ViewModel
	public static class AjaxViewModel {
		public int col;
		public int field;
		public boolean noJsonSupport;
		public Building.BuildingUiViewModel buildingUI;
		public GebaeudeAufBasisViewModel building;
	}

	/**
	 * Erzeugt die GUI-Daten der Basis und gibt diese als JSON-Response zurueck.
	 * @param base Die Basis
	 * @param field Die ID des Feldes auf dem das Gebaeude steht
	 * @return Die GUI-Daten
	 */
	@Action(ActionType.AJAX)
	public AjaxViewModel ajaxAction(@UrlParam(name="col") Base base, int field)
	{
		validiereBasisUndFeld(base, field);
		Building building = getGebaeudeFuerFeld(base, field);

		AjaxViewModel json = new AjaxViewModel();

		json.col = base.getId();
		json.field = field;
		json.building = GebaeudeAufBasisViewModel.map(building, base, field);

		if( !building.isSupportsJson() )
		{
			json.noJsonSupport = true;
			return json;
		}

		json.buildingUI = building.outputJson(getContext(), base, field, building.getId());

		return json;
	}

	/**
	 * Zeigt die GUI des Gebaeudes an.
	 * @param base Die Basis
	 * @param field Die ID des Feldes auf dem das Gebaeude steht
	 */
	@Action(ActionType.DEFAULT)
	public String defaultAction(@UrlParam(name = "col") Base base, int field)
	{
		validiereBasisUndFeld(base, field);
		Building building = getGebaeudeFuerFeld(base, field);

		User user = (User) getUser();
		StringBuilder echo = new StringBuilder();

		boolean classicDesign = building.classicDesign();

		if (building.printHeader())
		{
			if (!classicDesign)
			{
				echo.append("<div class='gfxbox' style='width:470px'>");

				echo.append("<div style=\"text-align:center\">\n");
				echo.append("<img style=\"vertical-align:middle\" src=\"./").append(building.getPictureForRace(user.getRace())).append("\" alt=\"\" /> ").append(Common._plaintitle(building.getName())).append("<br /></div>\n");
			}
			else
			{
				echo.append("<div>\n");
				echo.append("<span style=\"font-weight:bold\">").append(Common._plaintitle(building.getName())).append("</span><br />\n");
			}

			echo.append("Status: ");
			if (building.isActive(base, base.getActive()[field], field))
			{
				echo.append("<span style=\"color:#00ff00\">Aktiv</span><br />\n");
			}
			else
			{
				echo.append("<span style=\"color:#ff0000\">Inaktiv</span><br />\n");
			}

			echo.append("<br />");
			if (classicDesign)
			{
				echo.append("</div>");
			}
		}

		echo.append(outputHandler.getOrDefault(building.getModule(), this::delegatingOutput).output(building, base, field, building.getId()));

		if (!classicDesign)
		{
			echo.append("Aktionen: ");
		}
		else
		{
			echo.append("<div>\n");
		}

		if (building.isDeakAble())
		{
			if (base.getActive()[field] == 1)
			{
				echo.append("<a style=\"font-size:16px\" class=\"forschinfo\" href=\"").append(Common.buildUrl("shutdown", "col", base.getId(), "field", field)).append("\">deaktivieren</a>");
			}
			else
			{
				echo.append("<a style=\"font-size:16px\" class=\"forschinfo\" href=\"").append(Common.buildUrl("start", "col", base.getId(), "field", field)).append("\">aktivieren</a>");
			}

			if (classicDesign)
			{
				echo.append("<br />\n");
			}
			else
			{
				echo.append(", ");
			}
		}

		if (building.getId() != Building.KOMMANDOZENTRALE)
		{
			echo.append("<a style=\"font-size:16px\" class=\"error\" href=\"").append(Common.buildUrl("demo", "col", base.getId(), "field", field)).append("\">demontieren</a><br />");
		}
		else
		{
			echo.append("<a style=\"font-size:16px\" class=\"error\" href=\"javascript:DS.ask('Wollen Sie den Asteroiden wirklich aufgeben?','").append(Common.buildUrl("demo", "col", base.getId(), "field", field)).append("');\">Asteroid aufgeben</a><br />");
		}

		if (!classicDesign)
		{
			echo.append("<br />\n");
			echo.append("</div>");
			echo.append("<div>\n");
			echo.append("<br />\n");
		}

		echo.append("<br /><a style=\"font-size:16px\" class=\"back\" href=\"").append(Common.buildUrl("default", "module", "base", "col", base.getId())).append("\">zurück zur Basis</a><br /></div>\n");

		return echo.toString();
	}

	private String defaultOutput(Building building, Base base, int field, int buildingId) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Verbraucht:<br />\n");
		buffer.append("<div align=\"center\">\n");

		boolean entry = false;
		ResourceList reslist = building.getConsumes().getResourceList();
		for (ResourceEntry res : reslist)
		{
			buffer.append("<img src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCargo1()).append(" ");
			entry = true;
		}

		if (building.getEVerbrauch() > 0)
		{
			buffer.append("<img src=\"" + "./data/interface/energie.gif\" alt=\"\" />").append(building.getEVerbrauch()).append(" ");
			entry = true;
		}
		if (!entry)
		{
			buffer.append("-");
		}

		buffer.append("</div>\n");

		buffer.append("Produziert:<br />\n");
		buffer.append("<div align=\"center\">\n");

		entry = false;
		reslist = building.getProduces().getResourceList();
		for (ResourceEntry res : reslist)
		{
			buffer.append("<img src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCargo1()).append(" ");
			entry = true;
		}

		if (building.getEProduktion() > 0)
		{
			buffer.append("<img src=\"" + "./data/interface/energie.gif\" alt=\"\" />").append(building.getEProduktion());
			entry = true;
		}

		if (!entry)
		{
			buffer.append("-");
		}
		buffer.append("</div><br />\n");
		return buffer.toString();
	}

	private String shipyardOutput(Building building, Base base, int field, int buildingId) {
		StringBuilder response = new StringBuilder(500);

		BaseWerft werft = base.getWerft();
		if( werft == null ) {
			response.append("<a href=\"./ds?module=basen\"><span style=\"color:#ff0000; font-weight:bold\">Fehler: Die angegebene Kolonie hat keine Werft</span></a>\n");
			return response.toString();
		}

		werft.setBaseField(field);

		TemplateEngine t = templateViewResultFactory.createEmpty();
		WerftGUI werftgui = new WerftGUI( getContext(), t, shipyardService, bbCodeParser, fleetMgmtService, dismantlingService, shipActionService);
		response.append(werftgui.execute( werft ));

		response.append("<br /></div>\n");
		return response.toString();
	}

	private String loadAmmoTasks(Base base, ContextVars vars, int buildingid)
	{
		Context context = ContextMap.getContext();

		StringBuilder wfreason = new StringBuilder(100);

		if (!vars.getStats().containsKey(buildingid))
		{
			vars.getStats().put(buildingid, new Cargo());
		}
		if (!vars.getProductionstats().containsKey(buildingid))
		{
			vars.getProductionstats().put(buildingid, new Cargo());
		}
		if (!vars.getConsumptionstats().containsKey(buildingid))
		{
			vars.getConsumptionstats().put(buildingid, new Cargo());
		}

		boolean ok = true;
		Set<FactoryEntry> thisitemslist = vars.getOwneritemsbase();

		Cargo cargo = base.getCargo();

		List<ItemCargoEntry<Munitionsbauplan>> list = cargo.getItemsOfType(Munitionsbauplan.class);
		for (ItemCargoEntry<Munitionsbauplan> item : list)
		{
			FactoryEntry entry = item.getItem().getFabrikeintrag();

			thisitemslist.add(entry);
		}

		Factory wf = loadFactoryEntity(base, buildingid);

		if (wf == null)
		{
			vars.getUsedcapacity().put(buildingid, BigDecimal.valueOf(-1));

			Fabrik.getLog().warn("Basis " + base.getId() + " verfügt über keinen Fabrik-Eintrag, obwohl es eine Fabrik hat.");
			return "Basis " + base.getId() + " verfügt über keinen Fabrik-Eintrag, obwohl es eine Fabrik hat.";
		}

		Factory.Task[] plist = wf.getProduces();
		for (int i = 0; i < plist.length; i++)
		{
			int id = plist[i].getId();
			int count = plist[i].getCount();

			FactoryEntry entry = em.find(FactoryEntry.class, id);
			if (entry == null)
			{
				plist = ArrayUtils.remove(plist, i);
				i--;
				continue;
			}

			// Items ohne Plaene melden
			if ((count > 0) && !thisitemslist.contains(entry))
			{
				ok = false;
				wfreason.append("Es existieren nicht die nötigen Baupläne für ").append(entry.getName()).append("\n");
				break;
			}
		}

		if (ok)
		{
			for (Factory.Task aPlist : plist)
			{
				int id = aPlist.getId();
				int count = aPlist.getCount();

				FactoryEntry entry = em.find(FactoryEntry.class, id);

				if (!vars.getUsedcapacity().containsKey(buildingid))
				{
					vars.getUsedcapacity().put(buildingid, new BigDecimal(0, MathContext.DECIMAL32));
				}
				vars.getUsedcapacity().put(buildingid, vars.getUsedcapacity().get(buildingid).add(entry.getDauer().multiply((new BigDecimal(count)))));
				if (count > 0)
				{
					Cargo tmpcargo = new Cargo(entry.getBuildCosts());
					if (count > 1)
					{
						tmpcargo.multiply(count, Cargo.Round.NONE);
					}
					vars.getConsumptionstats().get(buildingid).addCargo(tmpcargo);
					vars.getStats().get(buildingid).substractCargo(tmpcargo);
					Cargo addCargo = entry.getProduce();
					addCargo.multiply(count, Cargo.Round.FLOOR);
					vars.getStats().get(buildingid).addCargo(addCargo);
					vars.getProductionstats().get(buildingid).addCargo(addCargo);
				}
			}
		}
		else
		{
			String basename = base.getName();
			wfreason.insert(0, "[b]" + basename + "[/b] - Die Arbeiten in der Fabrik sind zeitweise eingestellt.\nGrund:\n");
		}

		if (!vars.getUsedcapacity().containsKey(buildingid) || (vars.getUsedcapacity().get(buildingid).doubleValue() <= 0))
		{
			vars.getUsedcapacity().put(buildingid, new BigDecimal(-1));
		}

		return wfreason.toString();
	}

	private Factory loadFactoryEntity(Base base, int buildingid)
	{
		for (Factory factory : base.getFactories())
		{
			if (factory.getBuildingID() == buildingid)
			{
				return factory;
			}
		}
		return null;
	}

	private String loaddata(Base base, int buildingid)
	{
		Context context = ContextMap.getContext();

		User user = base.getOwner();

		ContextVars vars = context.get(ContextVars.class);

		if (!vars.isInit())
		{
			loadOwnerBase(user, vars);
            vars.setInit(true);
		}

        if(!vars.getBuildingidlist().contains(buildingid))
        {
            vars.getBuildingidlist().add(buildingid);
            return loadAmmoTasks(base, vars, buildingid);
        }
        return "";
	}

	private void loadOwnerBase(User user, ContextVars vars)
	{

		Context context = ContextMap.getContext();

		List<FactoryEntry> entrylist = Common.cast(em.createQuery("from FactoryEntry").list());
		for (FactoryEntry entry : entrylist)
		{
			if (!user.hasResearched(entry.getBenoetigteForschungen()))
			{
				continue;
			}
			vars.getOwneritemsbase().add(entry);
		}

		if (user.getAlly() != null)
		{
			Cargo itemlist = new Cargo(Cargo.Type.ITEMSTRING, user.getAlly().getItems());

			List<ItemCargoEntry<Munitionsbauplan>> list = itemlist.getItemsOfType(Munitionsbauplan.class);
			for (ItemCargoEntry<Munitionsbauplan> item : list)
			{
				FactoryEntry entry = item.getItem().getFabrikeintrag();

				vars.getOwneritemsbase().add(entry);
			}
		}
	}

	private int findExistingItemTask(FactoryEntry entry, List<Factory.Task> producelist) {
		int entryId = -1;
		for (int i = 0; i < producelist.size(); i++)
		{
			int aId = producelist.get(i).getId();
			if(aId == entry.getId()) {
				entryId = i;
				break;
			}
		}
		return entryId;
	}

	private int computeNewBuildCount(int count, List<Factory.Task> producelist, int entryId) {
		int currentCount;
		if(entryId == -1) {
			currentCount = 0;
		} else {
			currentCount = producelist.get(entryId).getCount();
		}
		return currentCount + count;
	}

	private void fabrikEintragButton(StringBuilder echo, FactoryEntry entry, Base base, int field, int count, String label)
	{
		echo.append("<td class=\"noBorderX\" style=\"vertical-align:top; width:30px\">\n");
		echo.append("<form action=\"./ds\" method=\"post\">\n");
		echo.append("<div>\n");
		echo.append("<input name=\"count\" type=\"hidden\" size=\"2\" value=\"").append(count).append("\" />\n");
		echo.append("<input name=\"produce\" type=\"hidden\" value=\"").append(entry.getId()).append("\" />\n");
		echo.append("<input name=\"col\" type=\"hidden\" value=\"").append(base.getId()).append("\" />\n");
		echo.append("<input name=\"field\" type=\"hidden\" value=\"").append(field).append("\" />\n");
		echo.append("<input name=\"module\" type=\"hidden\" value=\"building\" />\n");
		echo.append("<input type=\"submit\" value=\"").append(label).append("\" />\n");
		echo.append("</div>\n");
		echo.append("</form></td>\n");
	}

	private String factoryOutput(Building building, Base base, int field, int buildingId){
		User user = (User)javaSession.getUser();

		int produce = getContext().getRequest().getParameterInt("produce");
		int count   = getContext().getRequest().getParameterInt("count");

		StringBuilder echo = new StringBuilder(2000);

		Factory wf = loadFactoryEntity(base, buildingId);

		if (wf == null)
		{
			echo.append("<div style=\"color:red\">FEHLER: Diese Fabrik besitzt keinen Eintrag.<br /></div>\n");
			return echo.toString();
		}
		/*
			Liste der baubaren Items zusammenstellen
		*/

		Set<FactoryEntry> itemslist = new HashSet<>();

		Iterator<?> itemsIter = em.createQuery("from FactoryEntry").list().iterator();
		for (; itemsIter.hasNext(); )
		{
			FactoryEntry entry = (FactoryEntry) itemsIter.next();

			if (!user.hasResearched(entry.getBenoetigteForschungen()) || !entry.hasBuildingId(buildingId))
			{
				continue;
			}

			itemslist.add(entry);
		}

		Cargo cargo = base.getCargo();

		// Lokale Ammobauplaene ermitteln
		List<ItemCargoEntry<Munitionsbauplan>> itemlist = cargo.getItemsOfType(Munitionsbauplan.class);
		for (ItemCargoEntry<Munitionsbauplan> item : itemlist)
		{
			Munitionsbauplan itemobject = item.getItem();
			final FactoryEntry entry = itemobject.getFabrikeintrag();
			itemslist.add(entry);
		}

		// Moegliche Allybauplaene ermitteln
		if (user.getAlly() != null)
		{
			Cargo allyitems = new Cargo(Cargo.Type.ITEMSTRING, user.getAlly().getItems());

			itemlist = allyitems.getItemsOfType(Munitionsbauplan.class);
			for (ItemCargoEntry<Munitionsbauplan> item : itemlist)
			{
				Munitionsbauplan itemobject = item.getItem();
				final FactoryEntry entry = itemobject.getFabrikeintrag();
				itemslist.add(entry);
			}
		}

		/*
			Neue Bauauftraege behandeln
		*/

		echo.append("<div class=\"smallfont\">");
		if ((produce != 0) && (count != 0))
		{
			final FactoryEntry entry = em.find(FactoryEntry.class, produce);

			if (entry == null)
			{
				echo.append("<span style=\"color:red\">Fehler: Der angegebene Bauplan existiert nicht.</span>\n");
				return echo.toString();
			}

			if (itemslist.contains(entry))
			{
				BigDecimal usedcapacity = new BigDecimal(0, MathContext.DECIMAL32);

				Factory.Task[] plist = wf.getProduces();
				for (Factory.Task aPlist : plist)
				{
					final int aId = aPlist.getId();
					final int aCount = aPlist.getCount();

					final FactoryEntry aEntry = em.find(FactoryEntry.class, aId);
					usedcapacity = usedcapacity.add(aEntry.getDauer().multiply(new BigDecimal(aCount)));
				}
				if (usedcapacity.add(new BigDecimal(count).multiply(entry.getDauer())).doubleValue() > wf.getCount())
				{
					BigDecimal availableCap = usedcapacity.multiply(new BigDecimal(-1)).add(new BigDecimal(wf.getCount()));
					count = availableCap.divide(entry.getDauer(), RoundingMode.DOWN).intValue();
				}

				if (count != 0)
				{
					List<Factory.Task> producelist = new ArrayList<>(Arrays.asList(wf.getProduces()));

					// clean up entries which shouldn't exist anyway
					producelist.removeIf(task -> {
						int aId = task.getId();
						int ammoCount = task.getCount();

						FactoryEntry aEntry = em.find(FactoryEntry.class, aId);

						return aEntry == null || ammoCount <= 0;
					});

					int entryId = findExistingItemTask(entry, producelist);
					int newCount = computeNewBuildCount(count, producelist, entryId);

					if (entryId != -1) {
						producelist.remove(entryId);
					}
					if(newCount > 0) {
						producelist.add(new Factory.Task(entry.getId(), newCount));
					}

					wf.setProduces(producelist.toArray(new Factory.Task[0]));

					echo.append(Math.abs(count)).append(" ").append(entry.getName()).append(" wurden ").append((count >= 0 ? "hinzugefügt" : "abgezogen")).append("<br /><br />");
				}
			}
			else
			{
				echo.append("Sie haben nicht alle benötigten Forschungen für ").append(entry.getName()).append("<br /><br />");
			}
		}

		/*
			Aktuelle Bauauftraege ermitteln
		*/
		// Warum BigDecimal? Weil 0.05 eben nicht 0.05000000074505806 ist (Ungenauigkeit von double/float)....
		BigDecimal usedcapacity = new BigDecimal(0, MathContext.DECIMAL32);
		Map<FactoryEntry, Integer> productlist = new HashMap<>();
		Cargo consumes = new Cargo();
		Cargo produceCargo = new Cargo();
		consumes.setOption(Cargo.Option.SHOWMASS, false);
		produceCargo.setOption(Cargo.Option.SHOWMASS, false);

		if (wf.getProduces().length > 0)
		{
			Factory.Task[] plist = wf.getProduces();
			for (Factory.Task aPlist : plist)
			{
				final int id = aPlist.getId();
				final int ammoCount = aPlist.getCount();

				FactoryEntry entry = em.find(FactoryEntry.class, id);

				if (!itemslist.contains(entry))
				{
					echo.append("WARNUNG: Ungültiges Item >").append(entry.getId()).append("< (count: ").append(ammoCount).append(") in der Produktionsliste entdeckt.<br />\n");
					continue;
				}

				usedcapacity = usedcapacity.add(entry.getDauer().multiply(new BigDecimal(ammoCount)));

				if (ammoCount > 0)
				{
					Cargo tmpcargo = new Cargo(entry.getBuildCosts());
					Cargo prodcargo = new Cargo(entry.getProduce());
					if (ammoCount > 1)
					{
						tmpcargo.multiply(ammoCount, Cargo.Round.NONE);
						prodcargo.multiply(ammoCount, Cargo.Round.NONE);
					}
					consumes.addCargo(tmpcargo);
					produceCargo.addCargo(prodcargo);
				}
				productlist.put(entry, ammoCount);
			}
		}
		echo.append("</div>\n");

		/*
			Ausgabe: Verbrauch, Auftraege, Liste baubarer Munitionstypen
		*/
		echo.append("<div class='gfxbox' style='width:1100px'>");

		echo.append("<img style=\"vertical-align:middle\" src=\"./data/interface/time.gif\" alt=\"Zeiteinheiten\" />").append(Common.ln(usedcapacity)).append("/").append(wf.getCount()).append(" ausgelastet<br />\n");
		echo.append("Verbrauch: ");
		ResourceList reslist = consumes.getResourceList();
		for (ResourceEntry res : reslist)
		{
			echo.append("<img style=\"vertical-align:middle\" src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCargo1()).append("&nbsp;");
		}
		echo.append("<br/>");
		echo.append("Produktion: ");
		reslist = produceCargo.getResourceList();
		for (ResourceEntry res : reslist)
		{
			echo.append("<img style=\"vertical-align:middle\" src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCargo1()).append("&nbsp;");
		}
		echo.append("<br /><br />\n");
		echo.append("<table class=\"noBorderX\" cellpadding=\"2\">");
		echo.append("<tr>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:20px\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"font-weight:bold\">Kosten</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"font-weight:bold\">Produktion</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:130px\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:30px\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:30px\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:30px\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:30px\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:30px\">&nbsp;</td>\n");
		echo.append("</tr>");

		List<FactoryEntry> entries = Common.cast(em.createQuery("from FactoryEntry").list());

		for (FactoryEntry entry : entries)
		{
			if (!itemslist.contains(entry))
			{
				continue;
			}


			echo.append("<tr>\n");
			if (productlist.containsKey(entry))
			{
				echo.append("<td class=\"noBorderX\" valign=\"top\">").append(productlist.get(entry)).append("x</td>\n");
			}
			else
			{
				echo.append("<td class=\"noBorderX\" valign=\"top\">-</td>\n");
			}

			echo.append("<td class=\"noBorderX\" valign=\"top\">\n");
			echo.append("<img style=\"vertical-align:middle\" src=\"./data/interface/time.gif\" alt=\"Dauer\" />").append(Common.ln(entry.getDauer())).append(" \n");

			Cargo buildcosts = new Cargo(entry.getBuildCosts());
			buildcosts.setOption(Cargo.Option.SHOWMASS, false);
			reslist = buildcosts.getResourceList();
			for (ResourceEntry res : reslist)
			{
				echo.append("<span class=\"nobr\"><img style=\"vertical-align:middle\" src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCargo1()).append("</span>\n");
			}

			echo.append("</td>\n");
			echo.append("<td class=\"noBorderX\" valign=\"top\">\n");

			Cargo produceCosts = new Cargo(entry.getProduce());
			produceCosts.setOption(Cargo.Option.SHOWMASS, false);
			reslist = produceCosts.getResourceList();
			for (ResourceEntry res : reslist)
			{
				echo.append("<span class=\"nobr\"><img style=\"vertical-align:middle\" src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCargo1()).append("</span>\n");
			}

			echo.append("</td>\n");
			echo.append("<td class=\"noBorderX\" style=\"vertical-align:top; width:130px\">\n");
			echo.append("<form action=\"./ds\" method=\"post\">\n");
			echo.append("<div>\n");
			echo.append("<input name=\"count\" type=\"text\" size=\"2\" value=\"0\" />\n");
			echo.append("<input name=\"produce\" type=\"hidden\" value=\"").append(entry.getId()).append("\" />\n");
			echo.append("<input name=\"col\" type=\"hidden\" value=\"").append(base.getId()).append("\" />\n");
			echo.append("<input name=\"field\" type=\"hidden\" value=\"").append(field).append("\" />\n");
			echo.append("<input name=\"module\" type=\"hidden\" value=\"building\" />\n");
			echo.append("<input type=\"submit\" value=\"herstellen\" />\n");
			echo.append("</div>\n");
			echo.append("</form></td>\n");

			fabrikEintragButton(echo, entry, base, field, productlist.containsKey(entry) ? -productlist.get(entry) : 0, "reset");
			fabrikEintragButton(echo, entry, base, field, 1, "+ 1");
			fabrikEintragButton(echo, entry, base, field, 5, "+ 5");
			fabrikEintragButton(echo, entry, base, field, -1, "- 1");
			fabrikEintragButton(echo, entry, base, field, -5, "- 5");

			echo.append("</tr>\n");
		}

		echo.append("</table><br />\n");
		echo.append("</div>");
		echo.append("<div><br /></div>\n");

		return echo.toString();
	}


	private String kasernenOutput(Building building, Base base, int field, int buildingId) {
		User user = (User)javaSession.getUser();
		int cancel = getContext().getRequest().getParameterInt("cancel");
		int queueid = getContext().getRequest().getParameterInt("queueid");
		int newunit = getContext().getRequest().getParameterInt("unitid");
		int newcount = getContext().getRequest().getParameterInt("count");

		Kaserne kaserne = base.getKaserne();
		if( kaserne == null ) {
			buildInternal(base);
			kaserne = base.getKaserne();
		}
		TemplateViewResultFactory templateViewResultFactory = getContext().getBean(TemplateViewResultFactory.class, null);
		TemplateEngine t = templateViewResultFactory.createEmpty();
		if( !t.setFile( "_BUILDING", "buildings.kaserne.html" ) ) {
			getContext().addError("Konnte das Template-Engine nicht initialisieren");
			return "";
		}
		if( cancel == 1 && queueid > 0 )
		{
			KaserneEntry entry = kaserne.getEntryById(queueid);
			if(entry == null)
			{
				entry.deleteQueueEntry();
			}

		}

		t.setVar(
				"base.name",	base.getName(),
				"base.id",		base.getId(),
				"base.field",	field );

		//---------------------------------
		// Eine neue Einheit ausbilden
		//---------------------------------

		if( newunit != 0 && newcount > 0) {
			UnitType unittype = em.find(UnitType.class, newunit);

			Cargo cargo = new Cargo(base.getCargo());
			Cargo buildcosts = unittype.getBuildCosts();
			BigInteger konto = user.getKonto();
			StringBuilder msg = new StringBuilder();

			boolean ok = true;

			for(ResourceEntry res : buildcosts.getResourceList())
			{
				// Wenn nicht alles im eigenen Cargo da ist
				if( !cargo.hasResource(res.getId(), res.getCount1()*newcount) )
				{
					// Handelt es sich um Geld
					if(res.getId().equals(Resources.RE))
					{
						// Genug Geld auf dem Konto
						if(konto.intValue() >= res.getCount1()*newcount - cargo.getResourceCount(res.getId()))
						{
							// Fresse Cargo leer danach das Konto
							konto = konto.subtract(BigInteger.valueOf( res.getCount1()*newcount - cargo.getResourceCount(res.getId()) ));
							cargo.setResource(res.getId(), 0);
						}
						else
						{
							// Mensch sind wir echt sooo pleite?
							ok = false;
							msg.append("Sie haben nicht genug ").append(res.getPlainName()).append("<br />");
						}

					}
					else
					{
						// Es handelt sich nicht um Geld und wir haben nicht genug.
						ok = false;
						msg.append("Sie haben nicht genug ").append(res.getName()).append("<br />");
					}
				}
				else
				{
					// Wir haben genug
					cargo.substractResource(res.getId(), res.getCount1()*newcount);
				}
			}

			if( ok ) {
				msg.append(newcount).append(" ").append(unittype.getName()).append(" werden ausgebildet.");

				base.setCargo(cargo);
				user.setKonto(konto);

				kaserne.addEntry(unittype, newcount);
			}
			if(msg.length() > 0)
			{
				t.setVar( "kaserne.message", msg.toString());
			}
		}

		//-----------------------------------------------
		// werden gerade Einheiten ausgebildet? Bauschlange anzeigen!
		//-----------------------------------------------

		if( kaserne.isBuilding() ) {
			t.setVar(
					"kaserne.show.training", 1);

			t.setBlock("_BUILDING", "kaserne.training.listitem", "kaserne.training.list");

			for( KaserneEntry entry : kaserne.getQueueEntries() )
			{
				UnitType unittype = entry.getUnit();

				if(unittype == null)
				{
					t.setVar("kaserne.message", "Unbekannte Einheit gefunden");
				}

				t.setVar(	"trainunit.id", 		unittype.getId(),
							"trainunit.name", 		unittype.getName(),
							"trainunit.menge", 		entry.getCount(),
							"trainunit.remaining",	entry.getRemaining(),
							"trainunit.queue.id",	entry.getId() );

				t.parse("kaserne.training.list", "kaserne.training.listitem", true);
			}
		}

		//--------------------------------------------------
		// Ausbildbare Einheiten anzeigen
		//--------------------------------------------------

		t.setBlock("_BUILDING", "kaserne.unitlist.listitem", "kaserne.unitlist.list");

		List<UnitType> unitlist = em.createQuery("from Unittype ", UnitType.class).getResultList();

		for(UnitType unittype : unitlist)
		{
			if(user.hasResearched(unittype.getRes()))
			{
				StringBuilder buildingcosts = new StringBuilder();
				Cargo buildcosts = unittype.getBuildCosts();

				for(ResourceEntry res : buildcosts.getResourceList())
				{
					buildingcosts.append(" <span class='nobr'><img style=\"vertical-align:middle\" src=\"").append(res.getImage()).append("\" alt=\"").append(res.getPlainName()).append("\" title=\"").append(res.getPlainName()).append("\" />").append(res.getCargo1()).append("</span>");
				}

				t.setVar( 	"unit.id", 			unittype.getId(),
						"unit.name", 		unittype.getName(),
						"unit.picture", 	unittype.getPicture(),
						"unit.dauer", 		unittype.getDauer(),
						"unit.buildcosts", 	buildingcosts.toString().trim());

				t.parse("kaserne.unitlist.list", "kaserne.unitlist.listitem", true);
			}
		}

		t.parse( "OUT", "_BUILDING" );
		return t.getVar("OUT");
	}

	private String researchCenterOutput(Building building, Base base, int field, int buildingId) {
		int research = getContext().getRequest().getParameterInt("res");
		String confirm = getContext().getRequest().getParameterString("conf");
		String kill = getContext().getRequest().getParameterString("kill");
		String show = getContext().getRequest().getParameterString("show");
		if( !show.equals("oldres") ) {
			show = "newres";
		}

		StringBuilder echo = new StringBuilder(2000);

		Forschungszentrum fz = base.getForschungszentrum();
		if( fz == null ) {
			buildInternal(base);
			fz = base.getForschungszentrum();
		}

		echo.append("<table class=\"show\" cellspacing=\"2\" cellpadding=\"2\">\n");
		echo.append("<tr><td class=\"noBorderS\" style=\"text-align:center;font-size:12px\">Forschungszentrum<br />").append(base.getName()).append("</td><td class=\"noBorder\">&nbsp;</td>\n");

		//Neue Forschung & Bereits erforscht
		echo.append("<td class=\"noBorderS\">\n");

		echo.append("<div class='gfxbox' style='width:480px;text-align:center'>");

		echo.append("<a class=\"forschinfo\" href=\"./ds?module=building&amp;col=").append(base.getId()).append("&amp;field=").append(field).append("&amp;show=newres\">Neue Forschung</a>&nbsp;\n");
		echo.append("&nbsp;|&nbsp;&nbsp;<a class=\"forschinfo\" href=\"./ds?module=building&amp;col=").append(base.getId()).append("&amp;field=").append(field).append("&amp;show=oldres\">Bereits erforscht</a>\n");

		echo.append("</div>");

		echo.append("</td>\n");
		echo.append("</tr>\n");
		echo.append("<tr>\n");
		echo.append("<td colspan=\"3\" class=\"noBorderS\">\n");
		echo.append("<br />\n");

		echo.append("<div class='gfxbox' style='width:610px'>");

		if( (kill.length() != 0) || (research != 0) ) {
			if( kill.length() != 0 ) {
				killResearch(echo, fz, field, confirm);
			}
			if( research != 0 ) {
				doResearch(echo, fz, research, field, confirm );
			}
		}
		else if( show.equals("newres") ) {
			if( !currentResearch(echo, fz, field ) ) {
				possibleResearch(echo, fz, field );
			}
		}
		else {
			alreadyResearched(echo );
		}

		echo.append("</div>");

		echo.append("<br />\n");
		echo.append("</td>\n");
		echo.append("</tr>\n");

		echo.append("</table>");
		return echo.toString();
	}


	private String commandPostOutput(Building building, Base base, int field, int buildingId) {
		User user = (User)ContextMap.getContext().getActiveUser();

		String show = ContextMap.getContext().getRequest().getParameter("show");
		if( show == null ) {
			show = "";
		}
		String baction = ContextMap.getContext().getRequest().getParameterString("baction");

		if( !show.equals("general") && !show.equals("autogtu") ) {
			show = "general";
		}

		TemplateViewResultFactory templateViewResultFactory = ContextMap.getContext().getBean(TemplateViewResultFactory.class, null);
		TemplateEngine t = templateViewResultFactory.createEmpty();
		if( !t.setFile( "_BUILDING", "buildings.kommandozentrale.html" ) )
		{
			ContextMap.getContext().addError("Konnte das Template-Engine nicht initialisieren");
			return "";
		}

		t.setVar(	"base.name",	base.getName(),
			"base.id",		base.getId(),
			"base.field",	field,
			"base.system",	base.getSystem(),
			"base.size",	base.getSize());

		GtuWarenKurse kurseRow = em.find(GtuWarenKurse.class, "asti");
		Cargo kurse = new Cargo(kurseRow.getKurse());
		kurse.setOption( Cargo.Option.SHOWMASS, false );

		Cargo cargo = new Cargo(base.getCargo());

		StringBuilder message = new StringBuilder();

		/*
			Resourcen an die GTU verkaufen
		 */

		if( baction.equals("sell") ) {
			BigInteger totalRE = BigInteger.ZERO;

			int tick = ContextMap.getContext().get(ContextCommon.class).getTick();
			int system = base.getSystem();

			Optional<StatVerkaeufe> statsRow = em.createQuery("from StatVerkaeufe where tick=:tick and place=:place and system=:sys", StatVerkaeufe.class)
				.setParameter("tick", tick)
				.setParameter("place", "asti")
				.setParameter("sys", system)
				.getResultStream().findAny();
			Cargo stats = statsRow.map(sr -> new Cargo(sr.getStats())).orElse(new Cargo());

			boolean changed = false;

			ResourceList reslist = kurse.getResourceList();
			for( ResourceEntry res : reslist ) {
				long tmp = ContextMap.getContext().getRequest().getParameterInt(res.getId()+"to");

				if( tmp > 0 ) {
					if( tmp > cargo.getResourceCount(res.getId()) ) {
						tmp = cargo.getResourceCount(res.getId());
					}

					if( tmp <= 0 ) {
						continue;
					}

					BigDecimal get = BigDecimal.valueOf(tmp).multiply(BigDecimal.valueOf(res.getCount1() / 1000d));

					message.append("<img src=\"").append(res.getImage()).append("\" alt=\"\" />").append(Common.ln(tmp)).append(" f&uuml;r ").append(Common.ln(get)).append(" RE verkauft<br />\n");
					totalRE = totalRE.add(get.toBigInteger());

					changed = true;
					cargo.substractResource(res.getId(), tmp);
					stats.addResource(res.getId(), tmp);
				}
			}
			if( changed ) {
				statsRow.ifPresentOrElse(sr -> sr.setStats(stats),() -> {
					var row = new StatVerkaeufe(tick, system, "asti");
					row.setStats(stats);
					em.persist(row);
				});

				base.setCargo(cargo);
				user.transferMoneyFrom(Faction.GTU, totalRE, "Warenverkauf Asteroid "+base.getId()+" - "+base.getName(), false, UserMoneyTransfer.Transfer.SEMIAUTO );

				message.append("<br />");
			}
		}

		/*
			Allyitems an die Allianz ueberstellen
		*/

		if( baction.equals("item") ) {
			int itemid = ContextMap.getContext().getRequest().getParameterInt("item");

			Ally ally = user.getAlly();
			Item item = em.find(Item.class, itemid);

			if( ally == null ) {
				message.append("Sie sind in keiner Allianz<br /><br />\n");
			}
			else if( item == null || item.getEffect().hasAllyEffect() ) {
				message.append("Kein passenden Itemtyp gefunden<br /><br />\n");
			}
			else if( !cargo.hasResource( new ItemID(itemid) ) ) {
				message.append("Kein passendes Item vorhanden<br /><br />\n");
			}
			else {
				Cargo allyitems = new Cargo( Cargo.Type.ITEMSTRING, ally.getItems() );
				allyitems.addResource( new ItemID(itemid), 1 );
				cargo.substractResource( new ItemID(itemid), 1 );

				ally.setItems(allyitems.save());
				base.setCargo(cargo);

				String msg = "Ich habe das Item \""+item.getName()+"\" der Allianz zur Verfügung gestellt.";
				pmService.sendToAlly(user, ally, "Item &uuml;berstellt", msg);

				message.append("Das Item wurde an die Allianz übergeben<br /><br />\n");
			}
		}

		/*
			Einen Auto-Verkauf Eintrag hinzufuegen
		*/
		if( baction.equals("gtuadd") ) {
			ResourceID resid = Resources.fromString(ContextMap.getContext().getRequest().getParameterString("resid"));

			int actid = ContextMap.getContext().getRequest().getParameterInt("actid");
			int count = ContextMap.getContext().getRequest().getParameterInt("count");

			if( (actid >= 0) && (actid <= 1 ) && (count != 0 || (actid == 1)) ) {
				BaseStatus basedata = baseService.getStatus(base);
				Cargo stat = (Cargo)basedata.getProduction().clone();
				stat.setResource(Resources.NAHRUNG, 0);

				if( stat.getResourceCount(resid) != 0 && kurse.getResourceCount(resid) != 0 ) {
					List<AutoGTUAction> acts = base.getAutoGTUActs();
					acts.add(new AutoGTUAction(resid,actid,count));
					base.setAutoGTUActs(acts);

					message.append("Automatischer Verkauf von <img style=\"vertical-align:middle\" src=\"").append(Cargo.getResourceImage(resid)).append("\" alt=\"\" />").append(Cargo.getResourceName(resid)).append(" hinzugef&uuml;gt<br /><br />\n");
				}
			}
		}

		/*
			Einen Auto-Verkauf Eintrag entfernen
		*/
		if( baction.equals("gtudel") ) {
			String gtuact = ContextMap.getContext().getRequest().getParameterString("gtuact");

			if( gtuact.length() != 0 ) {
				List<AutoGTUAction> autoactlist = base.getAutoGTUActs();

				for( AutoGTUAction autoact : autoactlist ) {
					if( gtuact.equals(autoact.toString()) ) {
						autoactlist.remove(autoact);
						message.append("Eintrag entfernt<br /><br />\n");

						break;
					}
				}
				base.setAutoGTUActs(autoactlist);
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

			List<ItemCargoEntry<Item>> itemlist = cargo.getItemEntries();
			if( itemlist.size() != 0 ) {
				Ally ally = user.getAlly();
				if( ally != null ) {
					for( ItemCargoEntry<Item> item : itemlist ) {
						Item itemobject = item.getItem();
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

			List<Ship> ships = em.createQuery("from Ship " +
				"where id>:minid and (x between :minx and :maxx) and " +
				"(y between :miny and :maxy) and " +
				"system= :sys and locate('l ',docked)=0 and battle is null " +
				"order by x,y,owner.id,id", Ship.class)
				.setParameter("minid", 0)
				.setParameter("minx", base.getX()-base.getSize())
				.setParameter("maxx", base.getX()+base.getSize())
				.setParameter("miny", base.getY()-base.getSize())
				.setParameter("maxy", base.getY()+base.getSize())
				.setParameter("sys", base.getSystem())
				.getResultList();
			if( !ships.isEmpty() ) {
				Location oldLoc = null;

				for (Object ship1 : ships)
				{
					Ship ship = (Ship) ship1;

					if (oldLoc == null)
					{
						oldLoc = ship.getLocation();

						if (base.getSize() != 0)
						{
							t.setVar("ship.begingroup", 1);
						}
					}
					else if (!oldLoc.equals(ship.getLocation()))
					{
						oldLoc = ship.getLocation();

						if (base.getSize() != 0)
						{
							t.setVar("ship.begingroup", 1,
								"ship.endgroup", 1);
						}
					}
					else
					{
						t.setVar("ship.begingroup", 0,
							"ship.endgroup", 0);
					}

					t.setVar("ship.id", ship.getId(),
						"ship.name", Common._plaintitle(ship.getName()),
						"ship.x", ship.getX(),
						"ship.y", ship.getY());

					if (ship.getOwner().getId() != user.getId())
					{
						t.setVar("ship.owner.name", Common.escapeHTML(ship.getOwner().getPlainname()));
					}
					else
					{
						t.setVar("ship.owner.name", "");
					}

					t.parse("general.shiptransfer.list", "general.shiptransfer.listitem", true);
				}
			}

			List<Base> targetbases = em.createQuery("from Base where x= :x and y= :y and system= :sys and id!= :id and owner= :owner", Base.class)
				.setParameter("x", base.getX())
				.setParameter("y", base.getY())
				.setParameter("sys", base.getSystem())
				.setParameter("id", base.getId())
				.setParameter("owner", base.getOwner())
				.getResultList();
			for (Object targetbase1 : targetbases)
			{
				Base targetbase = (Base) targetbase1;

				t.setVar("targetbase.id", targetbase.getId(),
					"targetbase.name", Common._plaintitle(targetbase.getName()));
				t.parse("general.basetransfer.list", "general.basetransfer.listitem", true);
			}


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
		else {
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

			BaseStatus basedata = baseService.getStatus(base);
			Cargo stat = (Cargo)basedata.getProduction().clone();
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

	private String delegatingOutput(Building building, Base base, int field, int buildingId) {
		//TODO: Temporary, all output should be done by controller, but for the moment fall back to legacy behaviour for subclasses not yet converted
		return building.output(getContext(), base, field, buildingId);
	}

	private void killResearch(StringBuilder echo, Forschungszentrum fz, int field, String conf) {
		if( !conf.equals("ok") ) {
			echo.append("<div style=\"text-align:center\">\n");
			echo.append("Wollen sie die Forschung wirklich abbrechen?<br />\n");
			echo.append("Achtung: Es erfolgt keine R&uuml;ckerstattung der Resourcen!<br /><br />\n");
			echo.append("<a class=\"error\" href=\"./ds?module=building&amp;col=").append(fz.getBase().getId()).append("&amp;field=").append(field).append("&amp;kill=yes&amp;conf=ok\">Forschung abbrechen</a><br />\n");
			echo.append("</div>\n");
			return;
		}

		fz.setForschung(null);
		fz.setDauer(0);

		echo.append("<div style=\"text-align:center;color:red;font-weight:bold\">\n");
		echo.append("Forschung abgebrochen<br />\n");
		echo.append("</div>");
	}

	private void doResearch(StringBuilder echo, Forschungszentrum fz, int researchid, int field, String conf) {
		User user = (User)javaSession.getUser();

		Base base = fz.getBase();

		Forschung tech = Forschung.getInstance(researchid);
		boolean ok = true;

		if( !races.rasse(user.getRace()).isMemberIn(tech.getRace()) ) {
			echo.append("<a class=\"error\" href=\"./ds?module=base&amp;col=").append(base.getId()).append("\">Fehler: Diese Forschung kann von ihrer Rasse nicht erforscht werden</a>\n");
			return;
		}

		Cargo techCosts = tech.getCosts();
		techCosts.setOption( Cargo.Option.SHOWMASS, false );

		// Muss der User die Forschung noch best?tigen?
		if( !conf.equals("ok") ) {
			echo.append("<div style=\"text-align:center\">\n");
			echo.append(Common._plaintitle(tech.getName())).append("<br /><img style=\"vertical-align:middle\" src=\"./data/interface/time.gif\" alt=\"Dauer\" />").append(tech.getTime()).append(" ");
			echo.append("<img style=\"vertical-align:middle\" src=\"./data/interface/forschung/specpoints.gif\" alt=\"Spezialisierungskosten\" />").append(tech.getSpecializationCosts()).append(" ");
			ResourceList reslist = techCosts.getResourceList();
			for( ResourceEntry res : reslist ) {
				echo.append("<img style=\"vertical-align:middle\" src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCargo1()).append(" ");
			}

			echo.append("<br /><br />\n");
			echo.append("<a class=\"ok\" href=\"./ds?module=building&amp;col=").append(base.getId()).append("&amp;field=").append(field).append("&amp;res=").append(researchid).append("&amp;conf=ok\">Erforschen</a></span><br />\n");
			echo.append("</div>\n");

			return;
		}

		// Wird bereits im Forschungszentrum geforscht?
		if( fz.getForschung() != null ) {
			ok = false;
		}

		// Besitzt der Spieler alle fuer die Forschung noetigen Forschungen?
		if( !user.hasResearched(tech.getBenoetigteForschungen()) )
		{
			ok = false;
		}

		if(user.getFreeSpecializationPoints() < tech.getSpecializationCosts())
		{
			ok = false;
		}

		if( !ok ) {
			echo.append("<a class=\"error\" href=\"./ds?module=base&amp;col=").append(base.getId()).append("\">Fehler: Forschung kann nicht durchgef&uuml;hrt werden</a>\n");
			return;
		}

		// Alles bis hierhin ok -> nun zu den Resourcen!
		Cargo cargo = new Cargo(base.getCargo());
		ok = true;

		ResourceList reslist = techCosts.compare( cargo, false, false, true );
		for( ResourceEntry res : reslist ) {
			if( res.getDiff() > 0 ) {
				echo.append("<span style=\"color:red\">Nicht genug <img style=\"vertical-align:middle\" src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getName()).append("</span><br />\n");
				ok = false;
			}
		}

		// Alles OK -> Forschung starten!!!
		if( ok ) {
			cargo.substractCargo( techCosts );
			echo.append("<div style=\"text-align:center;color:green\">\n");
			echo.append(Common._plaintitle(tech.getName())).append(" wird erforscht<br />\n");
			echo.append("</div>\n");

			fz.setForschung(tech);
			fz.setDauer(tech.getTime());
			base.setCargo(cargo);
		}
	}

	private boolean currentResearch(StringBuilder echo, Forschungszentrum fz, int field ) {
		Forschung tech = fz.getForschung();
		if( tech != null ) {
			echo.append("<img style=\"float:left;border:0px\" src=\"").append(tech.getImage()).append("\" alt=\"\" />");
			echo.append("Erforscht: <a class=\"forschinfo\" href=\"./ds?module=forschinfo&amp;res=").append(tech.getID()).append("\">").append(Common._plaintitle(tech.getName())).append("</a>\n");
			echo.append("[<a class=\"error\" href=\"./ds?module=building&amp;col=").append(fz.getBase().getId()).append("&amp;field=").append(field).append("&amp;kill=yes\">x</a>]<br />\n");
			echo.append("Dauer: noch <img style=\"vertical-align:middle\" src=\"./data/interface/time.gif\" alt=\"\" />").append(fz.getDauer()).append(" Runden\n");
			echo.append("<br /><br />\n");
			return true;
		}
		return false;
	}

	private void buildInternal(Base base)
	{
		Context context = ContextMap.getContext();
		if( context == null ) {
			throw new RuntimeException("No Context available");
		}
		if( base.getForschungszentrum() == null )
		{
			Forschungszentrum fz = new Forschungszentrum(base);
			em.persist(fz);

			base.setForschungszentrum(fz);
		}
	}

	private void possibleResearch(StringBuilder echo, Forschungszentrum fz, int field) {
		User user = (User)javaSession.getUser();

		echo.append("M&ouml;gliche Forschungen: <br />\n");
		echo.append("<table class=\"noBorderX\" width=\"100%\">\n");
		echo.append("<tr>\n");
		echo.append("<td class=\"noBorderX\">Name</td>\n");
		echo.append("<td class=\"noBorderX\">Kosten</td>\n");
		echo.append("</tr>\n");

		Base base = fz.getBase();
		Cargo cargo = base.getCargo();

		List<Integer> researches = new ArrayList<>();
		List<Forschungszentrum> researchList = em.createQuery("from Forschungszentrum " +
			"where forschung is not null and base.owner=:owner", Forschungszentrum.class)
			.setParameter("owner", user)
			.getResultList();
		for (Forschungszentrum aFz : researchList)
		{
			if (aFz.getForschung() != null)
			{
				researches.add(aFz.getForschung().getID());
			}
		}

		boolean first = true;

		List<Forschung> forschungen = em.createQuery("from Forschung order by name", Forschung.class).getResultList();
		for (Forschung tech: forschungen)
		{
			if (!races.rasse(user.getRace()).isMemberIn(tech.getRace()))
			{
				continue;
			}
			if (researches.contains(tech.getID()))
			{
				continue;
			}
			if (user.hasResearched(tech))
			{
				continue;
			}

			if (user.getFreeSpecializationPoints() < tech.getSpecializationCosts())
			{
				continue;
			}

			boolean ok = true;

			if( !user.hasResearched(tech.getBenoetigteForschungen()) )
			{
				ok = false;
			}

			if (ok)
			{
				if (!first)
				{
					echo.append("<tr><td colspan=\"2\" class=\"noBorderX\"><hr style=\"height:1px; border:0px; background-color:#606060; color:#606060\" /></td></tr>\n");
				} else
				{
					first = false;
				}

				echo.append("<tr>\n");
				echo.append("<td class=\"noBorderX\" style=\"width:60%\">\n");
				if (!userService.isNoob(user) || !tech.hasFlag(Forschung.FLAG_DROP_NOOB_PROTECTION))
				{
					echo.append("<a class=\"forschinfo\" href=\"./ds?module=building&amp;col=").append(base.getId()).append("&amp;field=").append(field).append("&amp;res=").append(tech.getID()).append("\">").append(Common._plaintitle(tech.getName())).append("</a>\n");
				}
				else
				{
					echo.append("<a class=\"forschinfo\" " + "href=\"javascript:DS.ask(" + "'Achtung!\\nWenn Sie diese Technologie erforschen verlieren sie den GCP-Schutz. Dies bedeutet, dass Sie sowohl angreifen als auch angegriffen werden k&ouml;nnen'," + "'./ds?module=building&amp;col=").append(base.getId()).append("&amp;field=").append(field).append("&amp;res=").append(tech.getID()).append("'").append(")\">").append(Common._plaintitle(tech.getName())).append("</a>\n");
				}
				echo.append("<a class=\"forschinfo\" href=\"./ds?module=forschinfo&amp;res=").append(tech.getID()).append("\"><img style=\"border:0px;vertical-align:middle\" src=\"./data/interface/forschung/info.gif\" alt=\"?\" /></a>\n");
				echo.append("&nbsp;&nbsp;");
				echo.append("</td>\n");

				echo.append("<td class=\"noBorderX\">");
				echo.append("<img style=\"vertical-align:middle\" src=\"./data/interface/time.gif\" alt=\"Dauer\" />").append(tech.getTime()).append(" ");
				echo.append("<img style=\"vertical-align:middle\" src=\"./data/interface/forschung/specpoints.gif\" alt=\"Spezialisierungskosten\" />").append(tech.getSpecializationCosts()).append(" ");

				Cargo costs = tech.getCosts();
				costs.setOption(Cargo.Option.SHOWMASS, false);

				ResourceList reslist = costs.compare(cargo, false, false, true);
				for (ResourceEntry res : reslist)
				{
					if (res.getDiff() > 0)
					{
						echo.append("<img style=\"vertical-align:middle\" src=\"").append(res.getImage()).append("\" alt=\"\" /><span style=\"color:red\">").append(res.getCargo1()).append("</span> ");
					} else
					{
						echo.append("<img style=\"vertical-align:middle\" src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCargo1()).append(" ");
					}
				}

				echo.append("</td></tr>\n");
			}
		}

		echo.append("</table><br />\n");
	}

	private String academyOutput(Building building, Base base, int field, int buildingId) {
		AcademyBuilding academyBuilding = (AcademyBuilding)building;

		TemplateEngine t = templateViewResultFactory.createEmpty();

		int siliziumcosts = new ConfigService().getValue(WellKnownConfigValue.NEW_OFF_SILIZIUM_COSTS);
		int nahrungcosts = new ConfigService().getValue(WellKnownConfigValue.NEW_OFF_NAHRUNG_COSTS);
		int dauercosts = new ConfigService().getValue(WellKnownConfigValue.OFF_DAUER_COSTS);
		int maxoffstotrain = new ConfigService().getValue(WellKnownConfigValue.MAX_OFFS_TO_TRAIN);

		int newo = getContext().getRequest().getParameterInt("newo");
		int train = getContext().getRequest().getParameterInt("train");
		int off = getContext().getRequest().getParameterInt("off");
		int up = getContext().getRequest().getParameterInt("up");
		int down = getContext().getRequest().getParameterInt("down");
		int cancel = getContext().getRequest().getParameterInt("cancel");
		int queueid = getContext().getRequest().getParameterInt("queueid");
		String conf = getContext().getRequest().getParameterString("conf");

		if( !t.setFile( "_BUILDING", "buildings.academy.html" ) ) {
			getContext().addError("Konnte das Template-Engine nicht initialisieren");
			return "";
		}

		Academy academy = base.getAcademy();
		if( academy == null ) {
			getContext().addError("Diese Akademie verf&uuml;gt &uuml;ber keinen Akademie-Eintrag in der Datenbank");
			return "";
		}


		t.setVar(
			"base.name",	base.getName(),
			"base.id",		base.getId(),
			"base.field",	field,
			"academy.actualbuilds", academy.getNumberScheduledQueueEntries(),
			"academy.maxbuilds", maxoffstotrain);

		//--------------------------------
		// Als erstes ueberpruefen wir, ob eine Aktion durchgefuehrt wurde
		//--------------------------------
		if( up == 1 && queueid > 0 )
		{
			if( queueid == 1)
			{
				t.setVar(
					"academy.message", "<font color=\"red\">Vielen Dank fuer diesen URL-Hack.<br />Ihre Anfrage wurde soeben mitsamt Ihrer Spieler-ID an die Admins geschickt.<br />Wir wuenschen noch einen angenehmen Tag!</font>"
				);
			}
			else
			{
				AcademyQueueEntry thisentry = academy.getQueueEntryById(queueid);
				if(thisentry != null && thisentry.getPosition() > 0 )
				{
					AcademyQueueEntry upperentry = academy.getQueueEntryByPosition(thisentry.getPosition()-1);

					thisentry.setPosition(thisentry.getPosition()-1);

					if( upperentry != null )
					{
						upperentry.setPosition(upperentry.getPosition()+1);
					}
				}
			}
		}
		if( down == 1 && queueid > 0 )
		{
			AcademyQueueEntry thisentry = academy.getQueueEntryById(queueid );
			//Es kann ja sein, dass der Eintrag gar nicht mehr existiert.
			if(thisentry != null)
			{
				if( thisentry.isLastPosition() ) {
					t.setVar(
						"academy.message", "<font color=\"red\">Vielen Dank fuer diesen URL-Hack.<br />Ihre Anfrage wurde soeben mitsamt Ihrer Spieler-ID an die Admins geschickt.<br />Wir wuenschen noch einen angenehmen Tag!</font>"
					);
				}
				else
				{
					AcademyQueueEntry upperentry = academy.getQueueEntryByPosition(thisentry.getPosition()+1);
					thisentry.setPosition(thisentry.getPosition()+1);
					if( upperentry != null ) {
						upperentry.setPosition(upperentry.getPosition()-1);
					}
				}
			}
		}
		if( cancel == 1 && queueid > 0 )
		{
			AcademyQueueEntry thisentry = academy.getQueueEntryById(queueid);
			//Es kann ja sein, dass der Eintrag gar nicht mehr existiert.
			if(thisentry != null)
			{
				int offid = thisentry.getTraining();
				thisentry.deleteQueueEntry();
				academy.rescheduleQueue();
				if(offid > 0 )
				{
					if( !academy.isOffizierScheduled(offid))
					{
						Offizier offizier = Offizier.getOffizierByID(offid);
						offizier.setTraining(false);
					}
				}
				if( academy.getNumberScheduledQueueEntries() == 0 ) {
					academy.setTrain(false);
				}
			}
		}

		//---------------------------------
		// Einen neuen Offizier ausbilden
		//---------------------------------

		if( newo != 0 ) {
			t.setVar("academy.show.trainnewoffi", 1);

			Cargo cargo = new Cargo(base.getCargo());

			boolean ok = true;
			if( cargo.getResourceCount( Resources.SILIZIUM ) < siliziumcosts ) {
				t.setVar("trainnewoffi.error", "Nicht genug Silizium");
				ok = false;
			}
			if( cargo.getResourceCount( Resources.NAHRUNG ) < nahrungcosts ) {
				t.setVar("trainnewoffi.error", "Nicht genug Nahrung");
				ok = false;
			}

			if( ok ) {
				t.setVar("trainnewoffi.train", 1);

				cargo.substractResource( Resources.SILIZIUM, siliziumcosts);
				cargo.substractResource( Resources.NAHRUNG, nahrungcosts);
				academy.setTrain(true);
				AcademyQueueEntry entry = new AcademyQueueEntry(academy, -newo, dauercosts);
				base.setCargo(cargo);
				em.persist(entry);
				academy.addQueueEntry(entry);
			}
		}

		//--------------------------------------
		// "Upgrade" eines Offiziers durchfuehren
		//--------------------------------------

		if( (train != 0) && (off != 0) ) {
			Offizier offizier = Offizier.getOffizierByID(off);
			//Auch hier kann es sein dass der Offizier nicht existiert.
			if(offizier != null )
			{
				if( offizier.getStationiertAufBasis() != null && offizier.getStationiertAufBasis().getId() == base.getId() ) {
					int sk = academyBuilding.getUpgradeCosts(academy, 0, offizier, train);
					int nk = academyBuilding.getUpgradeCosts(academy, 1, offizier, train);
					int dauer = academyBuilding.getUpgradeCosts(academy, 2, offizier, train);

					t.setVar(
						"academy.show.trainoffi", 1,
						"trainoffi.id",			offizier.getID(),
						"trainoffi.trainid",	train,
						"offizier.name",		Common._plaintext(offizier.getName()),
						"offizier.train.dauer",		dauer,
						"offizier.train.nahrung", 	nk,
						"offizier.train.silizium",	sk,
						"resource.nahrung.image",	Cargo.getResourceImage(Resources.NAHRUNG),
						"resource.silizium.image",	Cargo.getResourceImage(Resources.SILIZIUM));

					if( train == 1 ) {
						t.setVar("offizier.train.ability", "Technik");
					}
					else if( train == 2 ) {
						t.setVar("offizier.train.ability", "Waffen");
					}
					else if( train == 3 ) {
						t.setVar("offizier.train.ability", "Navigation");
					}
					else if( train == 4 ) {
						t.setVar("offizier.train.ability", "Sicherheit");
					}
					else if( train == 5 ) {
						t.setVar("offizier.train.ability", "Kommandoeffizienz");
					}

					Cargo cargo = new Cargo(base.getCargo());
					boolean ok = true;
					if( cargo.getResourceCount( Resources.SILIZIUM ) < sk) {
						t.setVar("trainoffi.error", "Nicht genug Silizium");
						ok = false;
					}
					if( cargo.getResourceCount( Resources.NAHRUNG ) < nk ) {
						t.setVar("trainoffi.error", "Nicht genug Nahrung");
						ok = false;
					}

					if( !conf.equals("ok") ) {
						t.setVar("trainoffi.conf",	1);
						t.parse( "OUT", "_BUILDING" );
						return t.getVar("OUT");
					}

					if( ok ) {
						t.setVar("trainoffi.train", 1);

						cargo.substractResource( Resources.SILIZIUM, sk );
						cargo.substractResource( Resources.NAHRUNG, nk );

						AcademyQueueEntry entry = new AcademyQueueEntry(academy,offizier.getID(),dauer,train);

						offizier.setTraining(true);
						base.setCargo(cargo);
						em.persist(entry);
						academy.addQueueEntry(entry);
						academy.setTrain(true);
						academy.rescheduleQueue();

						t.setVar("academy.actualbuilds", academy.getNumberScheduledQueueEntries());

						t.parse( "OUT", "_BUILDING" );
						return t.getVar("OUT");
					}
				}
			}
		}

		//--------------------------------
		// Dann berechnen wir die Ausbildungsschlange neu
		//--------------------------------
		academy.rescheduleQueue();

		t.setVar("academy.actualbuilds", academy.getNumberScheduledQueueEntries());

		em.flush();
		//-----------------------------------------------
		// werden gerade Offiziere ausgebildet? Bauschlange anzeigen!
		//-----------------------------------------------

		if( academy.getTrain() ) {
			t.setVar(
				"academy.show.training", 1);

			t.setBlock("_BUILDING", "academy.training.listitem", "academy.training.list");

			List<AcademyQueueEntry> entries = new ArrayList<>(academy.getQueueEntries());
			entries.sort(new AcademyBuilding.AcademyQueueEntryComparator());
			for( AcademyQueueEntry entry : entries )
			{
				if( entry.getTraining() > 0 )
				{
					Offizier offi = Offizier.getOffizierByID(entry.getTraining());
					if( offi != null )
					{
						t.setVar(
							"trainoffizier.id", entry.getTraining(),
							"trainoffizier.name", offi.getName(),
							"trainoffizier.attribute", AcademyBuilding.getAttributes().get(entry.getTrainingType()),
							"trainoffizier.offi", true,
							"trainoffizier.picture", offi.getPicture()
						);
					}
				}
				else
				{
					t.setVar(
						"trainoffizier.attribute", "Neuer Offizier",
						"trainoffizier.name", Common._plaintext(AcademyBuilding.getOfficers().get(-entry.getTraining())),
						"trainoffizier.offi", false
					);
				}
				t.setVar(
					"trainoffizier.remain", entry.getRemainingTime(),
					"trainoffizier.build", entry.isScheduled(),
					"trainoffizier.queue.id", entry.getId(),
					"trainoffizier.showup", true,
					"trainoffizier.showdown", true
				);

				if( entry.getPosition() == 1 )
				{
					t.setVar(
						"trainoffizier.showup", false
					);
				}
				if( entry.isLastPosition() )
				{
					t.setVar(
						"trainoffizier.showdown", false
					);
				}

				t.parse("academy.training.list", "academy.training.listitem", true);

			}
		}

		//---------------------------------
		// Liste: Neue Offiziere ausbilden
		//---------------------------------

		t.setVar(
			"academy.show.trainnew",	1,
			"resource.silizium.image",	Cargo.getResourceImage(Resources.SILIZIUM),
			"resource.nahrung.image",	Cargo.getResourceImage(Resources.NAHRUNG),
			"resource.silizium.costs", siliziumcosts,
			"resource.nahrung.costs", nahrungcosts,
			"dauer.costs", dauercosts
		);

		t.setBlock("_BUILDING", "academy.trainnew.listitem", "academy.trainnew.list");

		for( Offiziere.Offiziersausbildung offi : Offiziere.LIST.values() ) {
			t.setVar(
				"offizier.id",		offi.getId(),
				"offizier.name",	Common._title(bbCodeParser, offi.getName()),
				"offizier.ing",		offi.getAbility(Offizier.Ability.ING),
				"offizier.waf",		offi.getAbility(Offizier.Ability.WAF),
				"offizier.nav",		offi.getAbility(Offizier.Ability.NAV),
				"offizier.sec",		offi.getAbility(Offizier.Ability.SEC),
				"offizier.com",		offi.getAbility(Offizier.Ability.COM));

			t.parse("academy.trainnew.list", "academy.trainnew.listitem", true);
		}


		//---------------------------------
		// Liste: "Upgrade" von Offizieren
		//---------------------------------

		t.setVar(
			"academy.show.offilist", 1,
			"offilist.allowactions", 1);

		t.setBlock("_BUILDING", "academy.offilistausb.listitem", "academy.offilistausb.list");

		List<Offizier> offiziere = Offizier.getOffiziereByDest(base);
		for( Offizier offi : offiziere ) {
			if( !offi.isTraining() )
			{
				continue;
			}
			t.setVar(
				"offizier.picture",	offi.getPicture(),
				"offizier.id",		offi.getID(),
				"offizier.name",	Common._plaintitle(offi.getName()),
				"offizier.ing",		offi.getAbility(Offizier.Ability.ING),
				"offizier.waf",		offi.getAbility(Offizier.Ability.WAF),
				"offizier.nav",		offi.getAbility(Offizier.Ability.NAV),
				"offizier.sec",		offi.getAbility(Offizier.Ability.SEC),
				"offizier.com",		offi.getAbility(Offizier.Ability.COM),
				"offizier.special",	offi.getSpecial().getName() );

			t.parse("academy.offilistausb.list", "academy.offilistausb.listitem", true);
		}

		t.setBlock("_BUILDING", "academy.offilist.listitem", "academy.offilist.list");

		offiziere = Offizier.getOffiziereByDest(base);
		for( Offizier offi : offiziere )
		{
			if( offi.isTraining() )
			{
				continue;
			}
			t.setVar(
				"offizier.picture",	offi.getPicture(),
				"offizier.id",		offi.getID(),
				"offizier.name",	Common._plaintitle(offi.getName()),
				"offizier.ing",		offi.getAbility(Offizier.Ability.ING),
				"offizier.waf",		offi.getAbility(Offizier.Ability.WAF),
				"offizier.nav",		offi.getAbility(Offizier.Ability.NAV),
				"offizier.sec",		offi.getAbility(Offizier.Ability.SEC),
				"offizier.com",		offi.getAbility(Offizier.Ability.COM),
				"offizier.special",	offi.getSpecial().getName() );

			t.parse("academy.offilist.list", "academy.offilist.listitem", true);
		}

		t.parse( "OUT", "_BUILDING" );
		return t.getVar("OUT");
	}

	private void alreadyResearched(StringBuilder echo) {
		User user = (User)javaSession.getUser();

		echo.append("<table class=\"noBorderX\">");
		echo.append("<tr><td class=\"noBorderX\" align=\"left\">Bereits erforscht:</td></tr>\n");

		List<Forschung> research = em.createQuery("from Forschung order by id", Forschung.class).getResultList();
		for(Forschung tech: research) {
			if( tech.isVisibile(user) && user.hasResearched(tech) ) {
				echo.append("<tr><td class=\"noBorderX\">\n");
				echo.append("<a class=\"forschinfo\" href=\"./ds?module=forschinfo&amp;res=").append(tech.getID()).append("\">").append(Common._plaintitle(tech.getName())).append("</a>");
				echo.append("</td><td class=\"noBorderX\"><img src=\"./data/interface/forschung/specpoints.gif\" alt=\"Spezialisierungskosten\">").append(tech.getSpecializationCosts()).append("</td>");
				echo.append("</tr>\n");
			}
		}
		echo.append("</table><br />");
	}

	/**
	 * A method that knows how to print a specific building type, e.g. net.driftingsouls.ds2.server.bases.Werft.
	 */
	@FunctionalInterface
	private interface OutputFunction {
		String output(Building building, Base base, int field, int buildingId);
	}
}

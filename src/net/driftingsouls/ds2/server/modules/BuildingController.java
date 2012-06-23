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

import java.io.IOException;
import java.io.Writer;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Die Gebaeudeansicht.
 * @author Christopher Jung
 *
 * @urlparam Integer col Die ID der Basis
 * @urlparam Integer field Die ID des Felds, dessen Gebaeude angezeigt werden soll
 *
 */
@Configurable
@Module(name="building")
public class BuildingController extends TemplateGenerator {
	private Base base;
	private Building building;

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
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public BuildingController(Context context) {
		super(context);

		parameterNumber("col");
		parameterNumber("field");

		setPageTitle("Gebäude");
	}

	@Override
	protected boolean validateAndPrepare(String action) {
		User user = (User)getUser();

		int col = getInteger("col");
		int field = getInteger("field");

		base = (Base)getDB().get(Base.class, col);
		if( (base == null) || (base.getOwner() != user) ) {
			addError("Die angegebene Kolonie existiert nicht", Common.buildUrl("default", "module", "basen"));

			return false;
		}

		if( (field >= base.getBebauung().length) || (base.getBebauung()[field] == 0) ) {
			addError("Es existiert kein Geb&auml;ude an dieser Stelle");

			return false;
		}

		building = Building.getBuilding(base.getBebauung()[field]);

		return true;
	}

	/**
	 * Aktiviert das Gebaeude.
	 * @throws IOException
	 *
	 */
	@Action(ActionType.AJAX)
	public JSONObject startAjaxAction()
	{
		int field = getInteger("field");
		JSONObject response = new JSONObject();
		response.accumulate("col", base.getId());
		response.accumulate("field", field);

		JSONObject buildingObj = new JSONObject();
		buildingObj.accumulate("id", building.getId());
		buildingObj.accumulate("name", Common._plaintitle(building.getName()));
		buildingObj.accumulate("picture", building.getPicture());
		buildingObj.accumulate("active", building.isActive( base, base.getActive()[field], field ));
		buildingObj.accumulate("deakable", building.isDeakAble());
		buildingObj.accumulate("kommandozentrale", building.getId() == Building.KOMMANDOZENTRALE);
		buildingObj.accumulate("type", building.getClass().getSimpleName());

		response.accumulate("building", buildingObj);

		if( (building.getArbeiter() > 0) && (building.getArbeiter() + base.getArbeiter() > base.getBewohner()) ) {
			response.accumulate("success", false);
			response.accumulate("message", "Nicht genügend Arbeiter vorhanden");
		}
		else if( building.isShutDown() &&
				(!base.getOwner().hasResearched(building.getTechRequired())
						|| (base.getOwner().getRace() != building.getRace() && building.getRace() != 0)))
		{
			response.accumulate("success", false);
			response.accumulate("message", "Sie können dieses Geb&auml;ude wegen unzureichenden Voraussetzungen nicht aktivieren");
		}
		else {
			Integer[] active = base.getActive();
			active[field] = 1;
			base.setActive(active);

			base.setArbeiter(base.getArbeiter() + building.getArbeiter());

			response.accumulate("success", true);
		}

		return response;
	}

	/**
	 * Aktiviert das Gebaeude.
	 * @throws IOException
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void startAction() throws IOException {
		int field = getInteger("field");
		Writer echo = getResponse().getWriter();

		if( (building.getArbeiter() > 0) && (building.getArbeiter() + base.getArbeiter() > base.getBewohner()) ) {
			echo.append("<span style=\"color:#ff0000\">Nicht gen&uuml;gend Arbeiter vorhanden</span><br /><br />\n");
		}
		else if( building.isShutDown() &&
				(!base.getOwner().hasResearched(building.getTechRequired())
						|| (base.getOwner().getRace() != building.getRace() && building.getRace() != 0)))
		{
			echo.append("<span style=\"color:#ff0000\">Sie k&ouml;nnen dieses Geb&auml;ude wegen unzureichenden Voraussetzungen nicht aktivieren</span><br /><br />\n");
		}
		else {
			Integer[] active = base.getActive();
			active[field] = 1;
			base.setActive(active);

			base.setArbeiter(base.getArbeiter() + building.getArbeiter());

			echo.append("<span style=\"color:#00ff00\">Geb&auml;ude aktiviert</span><br /><br />\n");
		}

		redirect();
	}

	/**
	 * Deaktiviert das Gebaeude.
	 *
	 */
	@Action(ActionType.AJAX)
	public JSONObject shutdownAjaxAction() {
		int field = getInteger("field");
		JSONObject response = new JSONObject();
		response.accumulate("col", base.getId());
		response.accumulate("field", field);

		JSONObject buildingObj = new JSONObject();
		buildingObj.accumulate("id", building.getId());
		buildingObj.accumulate("name", Common._plaintitle(building.getName()));
		buildingObj.accumulate("picture", building.getPicture());
		buildingObj.accumulate("active", building.isActive( base, base.getActive()[field], field ));
		buildingObj.accumulate("deakable", building.isDeakAble());
		buildingObj.accumulate("kommandozentrale", building.getId() == Building.KOMMANDOZENTRALE);
		buildingObj.accumulate("type", building.getClass().getSimpleName());

		response.accumulate("building", buildingObj);

		if( !building.isDeakAble() ) {
			response.accumulate("success", false);
			response.accumulate("message", "Sie können dieses Gebäude nicht deaktivieren");
		}
		else {
			Integer[] active = base.getActive();
			active[field] = 0;
			base.setActive(active);

			base.setArbeiter(base.getArbeiter() - building.getArbeiter());

			response.accumulate("success", true);
		}

		return response;
	}

	/**
	 * Deaktiviert das Gebaeude.
	 * @throws IOException
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void shutdownAction() throws IOException {
		int field = getInteger("field");
		Writer echo = getResponse().getWriter();

		if( !building.isDeakAble() ) {
			echo.append("<span style=\"color:red\">Sie k&ouml;nnen dieses Geb&auml;ude nicht deaktivieren</span>\n");
		}
		else {
			Integer[] active = base.getActive();
			active[field] = 0;
			base.setActive(active);

			base.setArbeiter(base.getArbeiter() - building.getArbeiter());

			echo.append("<span style=\"color:#ff0000\">Geb&auml;ude deaktiviert</span><br /><br />\n");
		}

		redirect();
	}

	/**
	 * Reisst das Gebaeude ab.
	 *
	 */
	@Action(ActionType.AJAX)
	public JSONObject demoAjaxAction() {
		int field = getInteger("field");

		JSONObject response = new JSONObject();
		response.accumulate("col", base.getId());
		response.accumulate("field", field);

		Cargo buildcosts =(Cargo)building.getBuildCosts().clone();
		buildcosts.multiply( 0.8, Cargo.Round.FLOOR );

		ResourceList reslist = buildcosts.getResourceList();
		Cargo addcargo = buildcosts.cutCargo(base.getMaxCargo()-base.getCargo().getMass());

		JSONArray c = new JSONArray();

		for( ResourceEntry res : reslist ) {
			JSONObject resObj = res.toJSON();
			if( !addcargo.hasResource(res.getId()) ) {
				resObj.accumulate("spaceMissing", true);
			}
			c.add(resObj);
		}

		response.accumulate("demoCargo", c);

		Cargo baseCargo = base.getCargo();
		baseCargo.addCargo( addcargo );
		base.setCargo(baseCargo);

		Integer[] bebauung = base.getBebauung();

		building.cleanup( getContext(), base, bebauung[field] );

		bebauung[field] = 0;
		base.setBebauung(bebauung);

		Integer[] active = base.getActive();
		active[field] = 0;
		base.setActive(active);

		response.accumulate("success", true);

		return response;
	}

	/**
	 * Reisst das Gebaeude ab.
	 * @throws IOException
	 * @urlparam String conf Falls "ok" bestaetigt dies den Abriss
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void demoAction() throws IOException {
		int field = getInteger("field");
		Writer echo = getResponse().getWriter();

		parameterString("conf");
		String conf = getString("conf");

		echo.append(Common.tableBegin(430,"left"));

		if( !conf.equals("ok") ) {
			echo.append("<div align=\"center\">\n");
			echo.append("<img align=\"middle\" src=\""+config.get("URL")+building.getPicture()+"\" alt=\"\" /> "+Common._plaintitle(building.getName())+"<br /><br />\n");
			echo.append("Wollen sie dieses Geb&auml;ude wirklich abreissen?<br /><br />\n");
			echo.append("<a class=\"error\" href=\""+Common.buildUrl("demo", "col", base.getId(), "field", field, "conf", "ok")+"\">abreissen</a><br /></div>");
			echo.append(Common.tableEnd());

			echo.append("<br />\n");
			echo.append("<a class=\"back\" href=\""+Common.buildUrl("default", "module", "base", "col", base.getId())+"\">zur&uuml;ck</a><br />\n");

			return;
		}

		Cargo buildcosts =(Cargo)building.getBuildCosts().clone();
		buildcosts.multiply( 0.8, Cargo.Round.FLOOR );

		echo.append("<div align=\"center\">R&uuml;ckerstattung:</div><br />\n");
		ResourceList reslist = buildcosts.getResourceList();
		Cargo addcargo = buildcosts.cutCargo(base.getMaxCargo()-base.getCargo().getMass());

		for( ResourceEntry res : reslist ) {
			echo.append("<img src=\""+res.getImage()+"\" alt=\"\" />"+res.getCargo1());
			if( !addcargo.hasResource(res.getId()) ) {
				echo.append(" - <span style=\"color:red\">Nicht genug Platz f&uuml;r alle Waren</span>");
			}
			echo.append("<br />\n");
		}

		Cargo baseCargo = base.getCargo();
		baseCargo.addCargo( addcargo );
		base.setCargo(baseCargo);

		Integer[] bebauung = base.getBebauung();

		building.cleanup( getContext(), base, bebauung[field] );

		bebauung[field] = 0;
		base.setBebauung(bebauung);

		Integer[] active = base.getActive();
		active[field] = 0;
		base.setActive(active);

		echo.append("<br />\n");
		echo.append("<hr noshade=\"noshade\" size=\"1\" style=\"color:#cccccc\" /><br />\n");
		echo.append("<div align=\"center\"><span style=\"color:#ff0000\">Das Geb&auml;ude wurde demontiert</span></div>\n");
		echo.append(Common.tableEnd());

		echo.append("<br />\n");
		echo.append("<a class=\"back\" href=\""+Common.buildUrl("default", "module", "base", "col", base.getId())+"\">zur&uuml;ck</a><br />\n");
	}

	@Action(ActionType.AJAX)
	public JSONObject ajaxAction()
	{
		JSONObject json = new JSONObject();

		int field = getInteger("field");
		json.accumulate("col", base.getId());
		json.accumulate("field", field);

		JSONObject buildingObj = new JSONObject();
		buildingObj.accumulate("id", building.getId());
		buildingObj.accumulate("name", Common._plaintitle(building.getName()));
		buildingObj.accumulate("picture", building.getPicture());
		buildingObj.accumulate("active", building.isActive( base, base.getActive()[field], field ));
		buildingObj.accumulate("deakable", building.isDeakAble());
		buildingObj.accumulate("kommandozentrale", building.getId() == Building.KOMMANDOZENTRALE);
		buildingObj.accumulate("type", building.getClass().getSimpleName());

		json.accumulate("building", buildingObj);

		if( !building.isSupportsJson() )
		{
			json.accumulate("noJsonSupport", true);
			return json;
		}

		json.accumulate("buildingUI", building.outputJson(getContext(), base, field, building.getId()));

		return json;
	}

	/**
	 * Zeigt die GUI des Gebaeudes an.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		try {
			int field = getInteger("field");
			Writer echo = getResponse().getWriter();

			boolean classicDesign = building.classicDesign();

			if( building.printHeader() ) {
				if( !classicDesign ) {
					echo.append(Common.tableBegin(430, "left"));

					echo.append("<div style=\"text-align:center\">\n");
					echo.append("<img style=\"vertical-align:middle\" src=\""+config.get("URL")+building.getPicture()+"\" alt=\"\" /> "+Common._plaintitle(building.getName())+"<br /></div>\n");
				}
				else {
					echo.append("<div>\n");
					echo.append("<span style=\"font-weight:bold\">"+Common._plaintitle(building.getName())+"</span><br />\n");
				}

				echo.append("Status: ");
				if( building.isActive( base, base.getActive()[field], field ) ) {
					echo.append("<span style=\"color:#00ff00\">Aktiv</span><br />\n");
				}
				else {
					echo.append("<span style=\"color:#ff0000\">Inaktiv</span><br />\n");
				}

				echo.append("<br />");
				if( classicDesign ) {
					echo.append("</div>");
				}
			}

			echo.append(building.output( getContext(), getTemplateEngine(), base, field, building.getId() ));

			if( !classicDesign ) {
				echo.append("Aktionen: ");
			}
			else {
				echo.append("<div>\n");
			}

			if( building.isDeakAble() ) {
				if( base.getActive()[field] == 1 ) {
					echo.append("<a style=\"font-size:16px\" class=\"forschinfo\" href=\""+Common.buildUrl("shutdown", "col", base.getId() , "field", field)+"\">deaktivieren</a>");
				}
				else {
					echo.append("<a style=\"font-size:16px\" class=\"forschinfo\" href=\""+Common.buildUrl("start", "col", base.getId() , "field", field)+"\">aktivieren</a>");
				}

				if( classicDesign ) {
					echo.append("<br />\n");
				}
				else {
					echo.append(", ");
				}
			}

			if( building.getId() != Building.KOMMANDOZENTRALE ) {
				echo.append("<a style=\"font-size:16px\" class=\"error\" href=\""+Common.buildUrl("demo", "col", base.getId() , "field", field)+"\">abreissen</a><br />");
			}
			else {
				echo.append("<a style=\"font-size:16px\" class=\"error\" href=\"javascript:ask(\'Wollen sie den Asteroiden wirklich aufgeben?\',\'"+Common.buildUrl("demo", "col", base.getId() , "field", field)+"\');\">Asteroid aufgeben</a><br />");
			}

			if( !classicDesign ) {
				echo.append("<br />\n");
				echo.append(Common.tableEnd());
				echo.append("<div>\n");
				echo.append("<br />\n");
			}

			echo.append("<br /><a style=\"font-size:16px\" class=\"back\" href=\""+Common.buildUrl("default", "module", "base" , "col", base.getId())+"\">zur&uuml;ck zur Basis</a><br /></div>\n");
		}
		catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
}

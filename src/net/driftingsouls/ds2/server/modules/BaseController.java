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
import java.util.LinkedHashMap;
import java.util.Map;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseStatus;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.bases.Core;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Verwaltung einer Basis
 * @author Christopher Jung
 *
 * @urlparam Integer col Die ID der Basis
 */
@Configurable
public class BaseController extends TemplateGenerator {
	private Base base;
	
	private Configuration config;
	
    /**
     * Injiziert die DS-Konfiguration
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config) 
    {
    	this.config = config;
    }
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public BaseController(Context context) {
		super(context);
		
		setTemplate("base.html");
		
		parameterNumber("col");
		
		setPageTitle("Basis");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		User user = (User)getUser();
		
		int col = getInteger("col");
		
		base = (Base)getDB().get(Base.class, col);
		if( (base == null) || (base.getOwner() != user) ) {
			addError("Die angegebene Kolonie existiert nicht", Common.buildUrl("default", "module", "basen") );
			
			return false;
		}
		
		base.getCargo().setOption( Cargo.Option.LINKCLASS, "schiffwaren" );
		
		setPageTitle(Common._plaintitle(base.getName()));
		
		return true;	
	}
	
	/**
	 * Transferiert Nahrung von/in den Nahrungspool
	 * @urlparam Integer nahrung Die Menge der zu transferierenden Nahrung. Negative Werte transferieren Nahrung aus den Pool.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void transferNahrungAction() {
		final int NAHRUNG_CHECKOUT_FACTOR = config.getInt("NAHRUNG_CHECKOUT_FACTOR");
		
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();

		parameterNumber("nahrung");
		long count = getInteger("nahrung");
	
		if( (count > 0) && (count > base.getCargo().getResourceCount(Resources.NAHRUNG)) ) {
			count = base.getCargo().getResourceCount(Resources.NAHRUNG);
		}
	
		Cargo usercargo = new Cargo( Cargo.Type.STRING, user.getCargo());

		if( (count < 0) && (-count*NAHRUNG_CHECKOUT_FACTOR > usercargo.getResourceCount(Resources.NAHRUNG)) ) {
			count = -usercargo.getResourceCount(Resources.NAHRUNG)/NAHRUNG_CHECKOUT_FACTOR;
		}
		
		if( (count < 0) && (-count + base.getCargo().getMass() > base.getMaxCargo()) ) {
			count = -base.getMaxCargo()+base.getCargo().getMass();	
		} 
		
		if( count == 0 ) {
			redirect();
			return;	
		}
		Cargo cargo = new Cargo(base.getCargo());
		
		cargo.substractResource( Resources.NAHRUNG, count );
		usercargo.addResource( Resources.NAHRUNG, count*NAHRUNG_CHECKOUT_FACTOR );
	
		user.setCargo(usercargo.save());
		base.setCargo(cargo);
	
		t.setVar("base.message", "<img src=\""+Cargo.getResourceImage(Resources.NAHRUNG)+"\" alt=\"\" />"+Math.abs(count)+" transferiert" );
	
		redirect();
	}
	
	/**
	 * Aendert den Namen einer Basis
	 * @urlparam String newname Der neue Name der Basis
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void changeNameAction() {
		TemplateEngine t = getTemplateEngine();
		
		parameterString("newname");
		String newname = getString("newname");
		if( newname.length() > 50 ) {
			newname = newname.substring(0,50);
		}
	
		base.setName(newname);
		
		t.setVar("base.message", "Name zu "+Common._plaintitle(newname)+" ge&auml;ndert");
		
		redirect();
	}
	
	/**
	 * (de)aktiviert Gebaeudegruppen
	 * @urlparam Integer act 0, wenn die Gebaeude deaktiviert werden sollen. Andernfalls 1
	 * @urlparam Integer buildingoff Die ID des Gebaeudetyps, dessen Gebaeude (de)aktiviert werden sollen
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void changeBuildingStatusAction() {
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("act");
		parameterNumber("buildingonoff");
		
		int act = getInteger("act");
		int buildingonoff = getInteger("buildingonoff");
		
		int bebstatus = 0;
		if( act == 1 ) {
			bebstatus = 1;
		}
		
		Building building = Building.getBuilding(buildingonoff);
		if( building.isDeakAble() ) {
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
				String result = "";
				
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
	protected void printHeader(String action) throws IOException {
		TemplateEngine t = getTemplateEngine();
			
		t.setBlock("_BASE", "header", "none" );
		t.setBlock("header", "tiles.listitem", "tiles.list");
		
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
	
		super.printHeader(action);
	}

	/**
	 * Zeigt die Basis an
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		
		StringBuilder tooltiptext = new StringBuilder(50);
		tooltiptext.append(Common.tableBegin(300, "center").replace('"', '\''));
		tooltiptext.append("<form action='./ds' method='post'>");
		tooltiptext.append("Name: <input name='newname' type='text' size='15' maxlength='50' value='");
		StringBuilder tooltiptext2 = new StringBuilder(50);
		tooltiptext2.append("' />");
		tooltiptext2.append("<input name='col' type='hidden' value='"+base.getId()+"' />");
		tooltiptext2.append("<input name='module' type='hidden' value='base' />");
		tooltiptext2.append("<input name='action' type='hidden' value='changeName' />");
		tooltiptext2.append("&nbsp;<input type='submit' value='rename' /><br />");
		tooltiptext2.append("</form>");
		tooltiptext2.append(Common.tableEnd().replace('"', '\''));

		String tooltip = StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(tooltiptext.toString()))+
			"'+basename+'"+
			StringEscapeUtils.escapeJavaScript( StringEscapeUtils.escapeHtml(tooltiptext2.toString()) );
		
		tooltip = tooltip.replace("<","&lt;");
		tooltip = tooltip.replace(">","&gt;");
		
		int mapheight = (1 + base.getHeight() * 2) * 22+25;
		
		t.setVar(	"base.id",				base.getId(),
					"base.name",			Common._plaintitle(base.getName()),
					"base.x",				base.getX(),
					"base.y",				base.getY(),
					"base.system",			base.getSystem(),
					"base.renametooltip",	tooltip,
					"base.core",			base.getCore(),
					"base.core.active",		base.isCoreActive(),
					"base.map.width",		(base.getWidth()*39+20 > 410 ? 410 : base.getWidth()*39+20),
					"base.cargo.height",	(mapheight < 280 ? "280" : mapheight),
					"base.cargo.empty",		Common.ln(base.getMaxCargo() - base.getCargo().getMass()) );
					
		BaseStatus basedata = Base.getStatus(getContext(), base.getId());
					
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
	
		Map<Integer,Integer> buildingonoffstatus = new LinkedHashMap<Integer,Integer>();
		
		t.setBlock("_BASE", "base.map.listitem", "base.map.list");

		for( int i = 0; i < base.getWidth() * base.getHeight(); i++ ) {	
			t.start_record();

			String image = "";
			
			//Leeres Feld
			if( base.getBebauung()[i] == 0 ) {
				image = "data/buildings/ground"+base.getTerrain()[i]+".png";
				base.getActive()[i] = 2;
			} 
			else {
				Building building = Building.getBuilding(base.getBebauung()[i]);
				base.getActive()[i] = basedata.getActiveBuildings()[i];
				
				if( building.isDeakAble() ) {
					if( !buildingonoffstatus.containsKey(base.getBebauung()[i]) ) { 
						buildingonoffstatus.put(base.getBebauung()[i], 0);
					}
					if( buildingonoffstatus.get(base.getBebauung()[i]) == 0 ) {
						buildingonoffstatus.put( base.getBebauung()[i], base.getActive()[i] + 1 );
					}
					else if( buildingonoffstatus.get(base.getBebauung()[i]) != base.getActive()[i] + 1 ) {
						buildingonoffstatus.put(base.getBebauung()[i],-1);
					}
				}
	
				image = building.getPicture();

				if( building.isDeakAble() && (base.getActive()[i] == 0) ) {
					t.setVar(	"tile.overlay",			1,
								"tile.overlay.image",	"overlay_offline.png" );
				}

				t.setVar(	"tile.building",		1,
							"tile.building.name", Common._plaintitle(building.getName()) );
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
		
		int bue = base.getBewohner() - basedata.getArbeiter();
		int wue = basedata.getBewohner() - base.getBewohner();
		
		ResourceList reslist = base.getCargo().compare(basedata.getStatus(), true);
		if( basedata.getStatus().getResourceCount(Resources.NAHRUNG) == 0 && !base.getCargo().hasResource(Resources.NAHRUNG) ) {
			reslist.addEntry(
					new ResourceEntry(
							Resources.NAHRUNG, 
							Cargo.getResourceName(Resources.NAHRUNG),	// Name
							Cargo.getResourceName(Resources.NAHRUNG),	// PlainName
							Cargo.getResourceImage(Resources.NAHRUNG),
							"0",	// Cargo1
							"0",	// Cargo2
							0,		// Count1
							0,		// Count2
							0));	// Diff
		}
		reslist.sortByID(false);
		
		t.setBlock("_BASE", "base.cargo.listitem", "base.cargo.list");
		
		for( ResourceEntry res : reslist ) {
			t.setVar(	"res.name",		res.getName(),
						"res.image",	res.getImage(),
						"res.cargo1",	res.getCargo1(),
						"res.cargo2",	res.getCargo2(),
						"res.plaincount2",	res.getCount2(),
						"res.nahrungspeciallink", "" );
			
			if( res.getId().equals(Resources.NAHRUNG) ) {
				tooltiptext = new StringBuilder(100);
				tooltiptext.append(Common.tableBegin(300,"center").replace('"', '\''));
				tooltiptext.append("<form action='./ds' method='post'>");
				tooltiptext.append("<div>");
				tooltiptext.append("Nahrung transferieren: <input name='nahrung' type='text' size='6' value='"+res.getCount1()+"' /><br />");
				tooltiptext.append("<input name='col' type='hidden' value='"+base.getId()+"' />");
				tooltiptext.append("<input name='module' type='hidden' value='base' />");
				tooltiptext.append("<input name='action' type='hidden' value='transferNahrung' />");
				tooltiptext.append("&nbsp;<input type='submit' value='ok' /><br />");
				tooltiptext.append("</div>");
				tooltiptext.append("</form>");
				tooltiptext.append(Common.tableEnd().replace('"', '\''));

				tooltip = StringEscapeUtils.escapeJavaScript( StringEscapeUtils.escapeHtml(tooltiptext.toString()) );

				t.setVar("res.nahrungspeciallink", tooltip);
			}
			t.parse("base.cargo.list", "base.cargo.listitem", true);
		}
		
		basedata.getStatus().setResource( Resources.NAHRUNG, 0 ); // Nahrung landet nicht im lokalen Cargo...
		long cstat = -basedata.getStatus().getMass();
		
		t.setVar(	"base.cstat",		Common.ln(cstat),
					"base.e",			base.getEnergy(),
					"base.estat",		basedata.getE(),
					"base.bewohner",	base.getBewohner(),
					"base.arbeiter.needed",	basedata.getArbeiter(),
					"base.wohnraum",		basedata.getBewohner() );
		
		//----------------
		// Aktionen
		//----------------

		t.setBlock("_BASE", "base.massonoff.listitem", "base.massonoff.list");

		for( int bid : buildingonoffstatus.keySet() ) {
			int bstatus = buildingonoffstatus.get(bid);
			if( bstatus == 0 ) {
				continue;
			}

			Building building = Building.getBuilding(bid);
			t.setVar(	"building.name",	Common._plaintitle(building.getName()),
						"building.id",		bid,
						"building.allowoff",	(bstatus == -1) || (bstatus == 2),
						"building.allowon",	(bstatus == -1) || (bstatus == 1) );

			t.parse("base.massonoff.list", "base.massonoff.listitem", true);
		}
		
		//-----------------------------------------
		// Energieverbrauch, Bevoelkerung usw.
		//------------------------------------------
		
		String baseimg = config.get("URL")+"data/interface/energie2";
		int e = basedata.getE();
		
		if( e < 0 ) {
			baseimg = config.get("URL")+"data/interface/nenergie2";
			e = -e;
		}	

		int e_x = e / 10;
		e %= 10;
		int e_v = e / 5;
		e %= 5;

		t.setBlock("_BASE", "base.estat.listitem", "base.estat.list");
		
		t.setVar("estat.image", baseimg+"_x.gif");
		for( ;e_x > 0; e_x-- ) {
			t.parse("base.estat.list", "base.estat.listitem", true);
		}
		
		t.setVar("estat.image", baseimg+"_v.gif");
		for( ;e_v > 0; e_v-- ) {
			t.parse("base.estat.list", "base.estat.listitem", true);
		}
		t.setVar("estat.image", baseimg+".gif");
		for( ;e > 0; e-- ) {
			t.parse("base.estat.list", "base.estat.listitem", true);
		}
		
		Map<Integer,String> bevstats = new LinkedHashMap<Integer,String>();

		if( basedata.getBewohner() >= base.getBewohner() ) {
			bevstats.put(basedata.getArbeiter()/10, "arbeiter.gif");
			bevstats.put(bue/10, "arbeitslos.gif");
			bevstats.put(wue/10, "frei.gif");
		}	
		else {
			int free = basedata.getBewohner()-basedata.getArbeiter();
			bevstats.put(basedata.getArbeiter()/10, "arbeiter.gif");
			bevstats.put(free/10, "arbeitslos.gif");
			bevstats.put(bue/10, "narbeiter.gif");
		}
		
		t.setBlock("_BASE", "base.bev.listitem", "base.bev.list");
		
		for( int bevstat : bevstats.keySet() ) {
			String image = bevstats.get(bevstat);
			t.setVar("bev.image", image);
			for( ;bevstat > 0; bevstat-- ) {
				t.parse("base.bev.list", "base.bev.listitem", true);
			}
		}
	}
}

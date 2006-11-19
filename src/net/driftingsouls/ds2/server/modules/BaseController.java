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
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Verwaltung einer Basis
 * @author Christopher Jung
 *
 * @urlparam Integer col Die ID der Basis
 */
public class BaseController extends DSGenerator {	
	private Base base;
	private int retryCount = 0;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public BaseController(Context context) {
		super(context);
		
		setTemplate("base.html");
		
		parameterNumber("col");	
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		Database db = getDatabase();
		User user = getUser();
		
		int col = getInteger("col");
		
		SQLResultRow baseRow = db.first("SELECT * FROM bases WHERE owner='",user.getID(),"' AND id='",col,"'");
		if( baseRow.isEmpty() ) {
			addError("Die angegebene Kolonie existiert nicht", Common.buildUrl(getContext(), "default", "module", "basen") );
			
			return false;
		}
		base = new Base(baseRow);		
		
		base.getCargo().setOption( Cargo.Option.LINKCLASS, "schiffwaren" );
		
		return true;	
	}
	
	/**
	 * Transferiert Nahrung von/in den Nahrungspool
	 * @urlparam Integer nahrung Die Menge der zu transferierenden Nahrung. Negative Werte transferieren Nahrung aus den Pool.
	 *
	 */
	public void transferNahrungAction() {		
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		parameterNumber("nahrung");
		long count = getInteger("nahrung");
	
		if( (count > 0) && (count > base.getCargo().getResourceCount(Resources.NAHRUNG)) ) {
			count = base.getCargo().getResourceCount(Resources.NAHRUNG);
		}
	
		Cargo usercargo = new Cargo( Cargo.Type.STRING, user.getCargo());

		if( (count < 0) && (-count*100 > usercargo.getResourceCount(Resources.NAHRUNG)) ) {
			count = -usercargo.getResourceCount(Resources.NAHRUNG)/100;
		}
		
		if( (count < 0) && (-count + base.getCargo().getMass() > base.getMaxCargo()) ) {
			count = -base.getMaxCargo()+base.getCargo().getMass();	
		} 
		
		if( count == 0 ) {
			redirect();
			return;	
		}
		Cargo cargo = (Cargo)base.getCargo().clone();
		
		cargo.substractResource( Resources.NAHRUNG, count );
		usercargo.addResource( Resources.NAHRUNG, count*100 );
	
		db.tBegin(true);
		user.setCargo(usercargo.save(), usercargo.save(true));
		db.tUpdate(1,"UPDATE bases SET cargo='",cargo.save(),"' WHERE id='",base.getID(),"' AND cargo='",cargo.save(true),"'");
		
		if( !db.tCommit() ) {
			if( retryCount < 3 ) {
				retryCount++;
				redirect("transferNahrung");
					
				return;
			}	
			addError("Konnte Nahrung nicht ordnunggem&auml;&szlig; transferieren");
				
			redirect();
			return;
		}
		base.put("cargo", cargo);
	
		t.set_var("base.message", "<img src=\""+Cargo.getResourceImage(Resources.NAHRUNG)+"\" alt=\"\" />"+Math.abs(count)+" 100er Pakete transferiert" );
	
		redirect();
	}
	
	/**
	 * Aendert den Namen einer Basis
	 * @urlparam String newname Der neue Name der Basis
	 *
	 */
	public void changeNameAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		parameterString("newname");
		String newname = getString("newname");
	
		db.prepare("UPDATE bases SET name= ? WHERE id= ? ").update(newname, base.getID());
		
		t.set_var("base.message", "Name zu "+Common._plaintitle(newname)+" ge&auml;ndert");
		base.put("name", newname);
		
		redirect();
	}
	
	/**
	 * (de)aktiviert Gebaeudegruppen
	 * @urlparam Integer act 0, wenn die Gebaeude deaktiviert werden sollen. Andernfalls 1
	 * @urlparam Integer buildingoff Die ID des Gebaeudetyps, dessen Gebaeude (de)aktiviert werden sollen
	 *
	 */
	public void changeBuildingStatusAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("act");
		parameterNumber("buildingonoff");
		
		int act = getInteger("act");
		int buildingonoff = getInteger("buildingonoff");
		
		int bebstatus = 0;
		if( act == 1 ) {
			bebstatus = 1;
		}
		
		Building building = Building.getBuilding(db, buildingonoff);
		if( building.isDeakAble() ) {
			int count = 0;
			
			for( int i=0; i <= base.getWidth()*base.getHeight()-1 ; i++ ) {
				
				if( (base.getBebauung()[i] == buildingonoff) && (base.getActive()[i] != bebstatus) ) {
					if( ((bebstatus != 0) && (base.getBewohner() >= base.getArbeiter() + building.getArbeiter())) || (bebstatus == 0) ) {
						base.getActive()[i] = bebstatus;
						
						count++;
					
						if( bebstatus != 0 ) {
							base.put("arbeiter", base.getArbeiter()+building.getArbeiter());
						}
					}
				}	
			}
		
			if( count != 0 ) {
				db.update("UPDATE bases SET active='"+Common.implode("|",base.getActive())+"',arbeiter="+base.getArbeiter()+" WHERE id='"+base.getID()+"'");
				
				String result = "";
				
				if( bebstatus != 0 ) {
					result = "<span style=\"color:green\">";
				}
				else {
					result = "<span style=\"color:red\">";
				}
				result += count+" Geb&auml;ude wurde"+(count > 1 ? "n" : "")+' '+(bebstatus != 0 ? "" : "de")+"aktiviert</span>";
				
				t.set_var("base.message", result);
			}
		}
		else {
			t.set_var("base.message", "<span style=\"color:red\">Sie k&ouml;nnen diese Geb&auml;ude nicht deaktivieren</span>");
		}
	
		redirect();
	}

	/**
	 * Zeigt die Basis an
	 */
	@Override
	public void defaultAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		t.set_block("_BASE", "header", "none" );
		t.set_block("header", "tiles.listitem", "tiles.list");
		
		int topOffset = 5;
		int leftOffset = 5;

		for( int i = 0; i < base.getHeight(); i++ ) {			
			for( int j = 0; j < base.getWidth(); j++ ) {				
				int top = topOffset + ((j % 2)+ i * 2) * 22;
	   			int left = leftOffset + j * 39;
	 	  		   				
   				t.set_var(	"tile.id",		base.getWidth()*i+j,
   							"tile.top",		top,
   							"tile.left",	left );
   				
   				t.parse("tiles.list", "tiles.listitem", true);
			}

		}
		
		t.parse("__HEADER","header");
		
		StringBuilder tooltiptext = new StringBuilder(50);
		tooltiptext.append(Common.tableBegin(300, "center").replace('"', '\''));
		tooltiptext.append("<form action='./main.php' method='post'>");
		tooltiptext.append("Name: <input name='newname' type='text' size='15' value='");
		StringBuilder tooltiptext2 = new StringBuilder(50);
		tooltiptext2.append("' />");
		tooltiptext2.append("<input name='col' type='hidden' value='"+base.getID()+"' />");
		tooltiptext2.append("<input name='sess' type='hidden' value='"+getString("sess")+"' />");
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
		
		t.set_var(	"base.id",				base.getID(),
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
					
		BaseStatus basedata = Base.getStatus(getContext(), base.getID());
					
		//------------------
		// Core
		//------------------
		if( base.getCore() > 0 ) {
			Core core = Core.getCore(db, base.getCore());
			t.set_var( "core.name", Common._plaintitle(core.getName()) );
		}
		
		
		//----------------
		// Karte
		//----------------
	
		Map<Integer,Integer> buildingonoffstatus = new LinkedHashMap<Integer,Integer>();
		
		t.set_block("_BASE", "base.map.listitem", "base.map.list");

		for( int i = 0; i < base.getWidth() * base.getHeight(); i++ ) {	
			t.start_record();

			String image = "";
			
			//Leeres Feld
			if( base.getBebauung()[i] == 0 ) {
				image = "data/buildings/ground"+base.getTerrain()[i]+".png";
				base.getActive()[i] = 2;
			} 
			else {
				Building building = Building.getBuilding(db, base.getBebauung()[i]);
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
					t.set_var(	"tile.overlay",			1,
								"tile.overlay.image",	"overlay_offline.png" );
				}

				t.set_var(	"tile.building",		1,
							"tile.building.name", Common._plaintitle(building.getName()) );
			}
			
			t.set_var(	"tile.field",			i,
						"tile.building.image",	image,
						"tile.id",				i );
			
			t.parse("base.map.list", "base.map.listitem", true);
			
			t.stop_record();
			t.clear_record();
		}
		
		//----------------
		// Waren
		//----------------

		db.update( "UPDATE bases SET arbeiter='"+basedata.getArbeiter()+"' WHERE id='"+base.getID()+"'");
		base.put("arbeiter", basedata.getArbeiter());
		
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
		
		t.set_block("_BASE", "base.cargo.listitem", "base.cargo.list");
		
		for( ResourceEntry res : reslist ) {
			t.set_var(	"res.name",		res.getName(),
						"res.image",	res.getImage(),
						"res.cargo1",	res.getCargo1(),
						"res.cargo2",	res.getCargo2(),
						"res.plaincount2",	res.getCount2(),
						"res.nahrungspeciallink", "" );
			
			if( res.getId().equals(Resources.NAHRUNG) ) {
				tooltiptext = new StringBuilder(100);
				tooltiptext.append(Common.tableBegin(300,"center").replace('"', '\''));
				tooltiptext.append("<form action='./main.php' method='post'>");
				tooltiptext.append("<div>");
				tooltiptext.append("Nahrung in 100er Paketen transferieren: <input name='nahrung' type='text' size='6' value='"+res.getCount1()+"' /><br />");
				tooltiptext.append("<input name='col' type='hidden' value='"+base.getID()+"' />");
				tooltiptext.append("<input name='sess' type='hidden' value='"+getString("sess")+"' />");
				tooltiptext.append("<input name='module' type='hidden' value='base' />");
				tooltiptext.append("<input name='action' type='hidden' value='transferNahrung' />");
				tooltiptext.append("&nbsp;<input type='submit' value='ok' /><br />");
				tooltiptext.append("</div>");
				tooltiptext.append("</form>");
				tooltiptext.append(Common.tableEnd().replace('"', '\''));

				tooltip = StringEscapeUtils.escapeJavaScript( StringEscapeUtils.escapeHtml(tooltiptext.toString()) );

				t.set_var("res.nahrungspeciallink", tooltip);
			}
			t.parse("base.cargo.list", "base.cargo.listitem", true);
		}
		
		basedata.getStatus().setResource( Resources.NAHRUNG, 0 ); // Nahrung landet nicht im lokalen Cargo...
		long cstat = -basedata.getStatus().getMass();
		
		t.set_var(	"base.cstat",		Common.ln(cstat),
					"base.e",			base.getE(),
					"base.estat",		basedata.getE(),
					"base.bewohner",	base.getBewohner(),
					"base.arbeiter.needed",	basedata.getArbeiter(),
					"base.wohnraum",		basedata.getBewohner() );
		
		//----------------
		// Aktionen
		//----------------

		t.set_block("_BASE", "base.massonoff.listitem", "base.massonoff.list");

		for( int bid : buildingonoffstatus.keySet() ) {
			int bstatus = buildingonoffstatus.get(bid);
			if( bstatus == 0 ) {
				continue;
			}

			Building building = Building.getBuilding(db, bid);
			t.set_var(	"building.name",	Common._plaintitle(building.getName()),
						"building.id",		bid,
						"building.allowoff",	(bstatus == -1) || (bstatus == 2),
						"building.allowon",	(bstatus == -1) || (bstatus == 1) );

			t.parse("base.massonoff.list", "base.massonoff.listitem", true);
		}
		
		//-----------------------------------------
		// Energieverbrauch, Bevoelkerung usw.
		//------------------------------------------
		
		String baseimg = Configuration.getSetting("URL")+"data/interface/energie2";
		int e = basedata.getE();
		
		if( e < 0 ) {
			baseimg = Configuration.getSetting("URL")+"data/interface/nenergie2";
			e = -e;
		}	

		int e_x = e / 10;
		e %= 10;
		int e_v = e / 5;
		e %= 5;

		t.set_block("_BASE", "base.estat.listitem", "base.estat.list");
		
		// Dieses dumme Konstrukt braucht der IE
		int breakafter =(base.getWidth()*39+20+500)/16;
		
		int i = 0;

		t.set_var("estat.image", baseimg+"_x.gif");
		for( ;e_x > 0; e_x-- ) {
			if( i == 0 ) {
				t.set_var("estat.break", 0);
			}
			i++;
			if( i > breakafter ) {
				t.set_var("estat.break", 1);
				i = 0;	
			}
			t.parse("base.estat.list", "base.estat.listitem", true);
		}
		
		t.set_var("estat.image", baseimg+"_v.gif");
		for( ;e_v > 0; e_v-- ) {
			if( i == 0 ) {
				t.set_var("estat.break", 0);
			}
			i++;
			if( i > breakafter ) {
				t.set_var("estat.break", 1);
				i = 0;	
			}
			t.parse("base.estat.list", "base.estat.listitem", true);
		}
		t.set_var("estat.image", baseimg+".gif");
		for( ;e > 0; e-- ) {
			if( i == 0 ) {
				t.set_var("estat.break", 0);
			}
			i++;
			if( i > breakafter ) {
				t.set_var("estat.break", 1);
				i = 0;	
			}
			t.parse("base.estat.list", "base.estat.listitem", true);
		}
		
		i = 0;
		breakafter =(base.getWidth()*39+20+500)/8;
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
		
		t.set_block("_BASE", "base.bev.listitem", "base.bev.list");
		
		for( int bevstat : bevstats.keySet() ) {
			String image = bevstats.get(bevstat);
			t.set_var("bev.image", image);
			for( ;bevstat > 0; bevstat-- ) {
				if( i == 0 ) {
					t.set_var("bev.break", 0);
				}
				i++;
				if( i > breakafter ) {
					t.set_var("bev.break", 1);
					i = 0;	
				}
				t.parse("base.bev.list", "base.bev.listitem", true);
			}
		}
	}
}

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

import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Required;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.bases.Core;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Die Liste aller baubaren Gebaeude und Cores.
 * @author Christopher Jung
 * 
 * @urlparam Integer col Die ID der Basis, auf die der zurueck-Link zeigen soll
 * @urlparam Integer field Die ID des Feldes, dessen Gebaeude der zurueck-Link ansteuern soll
 *
 */
@Configurable
public class BuildingsController extends TemplateGenerator {
	
	private Configuration config;
	
	/**
	 * Injiziert die DS-Konfiguration.
	 * @param config Die DS-Konfiguration
	 */
	@Autowired
	@Required
	public void setConfig(Configuration config)
	{
		this.config = config;
	}

	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public BuildingsController(Context context) {
		super(context);
		
		setTemplate("buildings.html");
		
		parameterNumber("col");	
		parameterNumber("field");
		
		setPageTitle("Geb&auml;ude");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		org.hibernate.Session db = getDB();
		User user = (User)getUser();
		
		int col = getInteger("col");
		
		if( col != 0 ) {
			Base chk = (Base)db.get(Base.class, col);

			if( (chk == null) || (chk.getOwner() != user) ) {
				addError("Die angegebene Kolonie existiert nicht");
				
				return false;
			}
		}
		
		return true;
	}

	/**
	 * Zeigt die Liste aller baubaren Gebaeude und Cores an.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		int userrasse = user.getRace();
		
	
		t.setBlock("_BUILDINGS", "buildings.listitem", "buildings.list");
		t.setBlock("buildings.listitem", "building.buildcosts.listitem", "building.buildcosts.list");
		t.setBlock("buildings.listitem", "building.produces.listitem", "building.produces.list");
		t.setBlock("buildings.listitem", "building.consumes.listitem", "building.consumes.list");
		
		Iterator<?> buildingIter = db.createQuery("from Building order by name").iterate();
		for( ; buildingIter.hasNext(); ) {
			Building building = (Building)buildingIter.next();
			if( !user.hasResearched(building.getTechRequired()) ) {
				continue;
			}
			
			t.setVar(	"building.name",	Common._plaintitle(building.getName()),
						"building.picture",	building.getPicture(),
						"building.arbeiter",	building.getArbeiter(),
						"building.bewohner",	building.getBewohner() );
			
			ResourceList reslist = building.getBuildCosts().getResourceList();
			Resources.echoResList( t, reslist, "building.buildcosts.list" );
		
			reslist = building.getConsumes().getResourceList();
			Resources.echoResList( t, reslist, "building.consumes.list" );
	
			if( building.getEVerbrauch() > 0 ) {
				t.setVar(	"res.image",	config.get("URL")+"data/interface/energie.gif",
							"res.cargo",	building.getEVerbrauch(),
							"res.plainname",	"Energie" );
							
				t.parse("building.consumes.list","building.consumes.listitem",true);
			}

			reslist = building.getProduces().getResourceList();
			Resources.echoResList( t, reslist, "building.produces.list" );

			if( building.getEProduktion() > 0 ) {
				t.setVar(	"res.image",		config.get("URL")+"data/interface/energie.gif",
							"res.cargo",		building.getEProduktion(),
							"res.plainname",	"Energie" );
									
				t.parse("building.produces.list","building.produces.listitem",true);
			}
			
			// Weitere Infos generieren (spezies, max per Basis/Acc & UComplex)
			StringBuilder addinfo = new StringBuilder(20);
			
			
			int buildingrasse = building.getRace();
			
			if( buildingrasse == 0 ) {
				addinfo.append("<span style=\"color:#FFFFFF; font-weight:normal\">"+Rassen.get().rasse(buildingrasse).getName()+" <br /></span>"); 
			}
			else if( userrasse == buildingrasse ) {
				addinfo.append("<span style=\"color:#00FF00; font-weight:normal\">"+Rassen.get().rasse(buildingrasse).getName()+" <br /></span>");
			}
			else {
				addinfo.append("<span style=\"color:#FF0000; font-weight:normal\">"+Rassen.get().rasse(buildingrasse).getName()+" <br /></span>");
			}
 
			
			if( building.isUComplex() ) {
				addinfo.append("Unterirdischer Komplex<br />");	
			}
			
			if( building.getPerPlanetCount() != 0 ) {

				addinfo.append("Max. "+building.getPerPlanetCount()+"x pro Basis<br />");
			}
			
			if( building.getPerUserCount() != 0 ) {

				addinfo.append("Max. "+building.getPerUserCount()+"x pro Account");
			}
			
			t.setVar("building.addinfo", addinfo);
			
			t.parse("buildings.list", "buildings.listitem", true);
		}
		
		//
		// Cores
		//
		
		t.setBlock("_BUILDINGS", "cores.listitem", "cores.list");
		t.setBlock("cores.listitem", "core.buildcosts.listitem", "core.buildcosts.list");
		t.setBlock("cores.listitem", "core.produces.listitem", "core.produces.list");
		t.setBlock("cores.listitem", "core.consumes.listitem", "core.consumes.list");
		

		Iterator<?> coreIter = db.createQuery("from Core order by name,astiType").iterate();
		for( ; coreIter.hasNext(); ) {
			Core core = (Core)coreIter.next();
			if( !user.hasResearched(core.getTechRequired()) ) {
				continue;
			}
			
			t.setVar(	"core.astitype",	core.getAstiType(),
						"core.name",		Common._plaintitle(core.getName()),
						"core.arbeiter",	core.getArbeiter(),
						"core.bewohner",	core.getBewohner() );
			
			ResourceList reslist = core.getBuildCosts().getResourceList();
			Resources.echoResList( t, reslist, "core.buildcosts.list" );
		
			reslist = core.getConsumes().getResourceList();
			Resources.echoResList( t, reslist, "core.consumes.list" );
	
			if( core.getEVerbrauch() > 0 ) {
				t.setVar(	"res.image",	config.get("URL")+"data/interface/energie.gif",
							"res.cargo",	core.getEVerbrauch(),
							"res.plainname",	"Energie" );
							
				t.parse("core.consumes.list","core.consumes.listitem",true);
			}

			reslist = core.getProduces().getResourceList();
			Resources.echoResList( t, reslist, "core.produces.list" );

			if( core.getEProduktion() > 0 ) {
				t.setVar(	"res.image",		config.get("URL")+"data/interface/energie.gif",
							"res.cargo",		core.getEProduktion(),
							"res.plainname",	"Energie" );
									
				t.parse("core.produces.list","core.produces.listitem",true);
			}
	
			t.parse("cores.list","cores.listitem",true);
		}
		
		t.setVar(	"base.id",		getInteger("col"),
					"base.field",	getInteger("field") );
	}
}

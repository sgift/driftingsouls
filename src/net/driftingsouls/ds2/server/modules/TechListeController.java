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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.Forschung;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.Rasse;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Zeigt die Liste der erforschten/nicht erforschten/erforschbaren Technologien
 * an.
 * @author Christopher Jung
 *
 * @urlparam Integer rasse Die Rasse, deren Technologien angezeigt werden sollen
 */
public class TechListeController extends DSGenerator {
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public TechListeController(Context context) {
		super(context);
		parameterNumber("rasse");
		
		setTemplate("techliste.html");	
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		// TODO Auto-generated method stub
		return true;
	}
	
	/**
	 * Zeigt die Techliste an
	 */
	@Override
	public void defaultAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
		int rasse = getInteger("rasse");
		
		if( rasse == 0 ) {
			rasse = user.getRace();
		}
		else if( !Rassen.get().rasse(rasse).isExtPlayable() ) {
			rasse = user.getRace();
		}

		StringBuilder rassenliste = new StringBuilder(100);
		rassenliste.append(Common.tableBegin(300,"center").replace('"', '\''));
		
		for( Rasse aRasse : Rassen.get() ) {
			if( aRasse.isExtPlayable() ) {
				rassenliste.append("<a href='"+Common.buildUrl(getContext(), "default", "rasse", aRasse.getID())+"'>");
				rassenliste.append("<img style='border:0px' src='"+Configuration.getSetting("URL")+"data/interface/rassen/"+aRasse.getID()+".png' />");
				rassenliste.append("</a>");		
			}
		}
		rassenliste.append(Common.tableEnd().replace('"', '\''));
		
		String rassenlisteStr = StringEscapeUtils.escapeJavaScript(rassenliste.toString().replace(">", "&gt;").replace("<", "&lt;"));
		
		t.set_var(	"race.name",	Rassen.get().rasse(rasse).getName(),
					"race.list",	rassenlisteStr );

		Map<Integer,Forschung>  researched = new LinkedHashMap<Integer,Forschung>();
		Map<Integer,Forschung>  researchable = new LinkedHashMap<Integer,Forschung>();
		Map<Integer,Forschung>  notResearchable = new LinkedHashMap<Integer,Forschung>();
		Map<Integer,Forschung>  invisible = new LinkedHashMap<Integer,Forschung>();
		Map<Integer,Forschung>  researchCache = new LinkedHashMap<Integer,Forschung>();

		//Alle Forschungen durchgehen
		SQLQuery forschung = db.query("SELECT id FROM forschungen ORDER BY name");
		while( forschung.next() ) {
			Forschung f = Forschung.getInstance(forschung.getInt("id"));
			researchCache.put(f.getID(), f);
			
			if( !Rassen.get().rasse(rasse).isMemberIn(f.getRace()) ) {
				continue;	
			}	

			// Status der Forschung (erforscht/verfuegbar/nicht verfuegbar) ermitteln
			if( user.hasResearched(f.getID()) ) {
				researched.put(f.getID(), f);
			} 
			else if( user.hasResearched(f.getRequiredResearch(1)) && user.hasResearched(f.getRequiredResearch(2)) && 
					user.hasResearched(f.getRequiredResearch(3)) ) {
				researchable.put(f.getID(), f);
			} 
			if( !f.isVisibile() ) {
				if( user.getAccessLevel() >= 20 ) {
					invisible.put(f.getID(), f);
				}
			}
			else {
				notResearchable.put(f.getID(), f);
			}
		}
		forschung.free();

		t.set_block("_TECHLISTE","tech.listitem","none");

		t.set_block("_TECHLISTE","tech.researchable.listitem","none");
		t.set_block("tech.researchable.listitem","res.listitem", "res.list" );

		Map<String,Map<Integer,Forschung>> keys = new LinkedHashMap<String,Map<Integer,Forschung>>();
		keys.put("researched", researched);
		keys.put("researchable", researchable);
		keys.put("notResearchable", notResearchable);
		keys.put("invisible", invisible);

		Map<Integer,Integer> currentResearches = new HashMap<Integer,Integer>();
		SQLQuery resRow = db.query("SELECT t1.forschung,t1.dauer FROM fz AS t1,bases AS t2 WHERE t1.forschung>0 AND t1.col=t2.id AND t2.owner=",user.getID());
		while( resRow.next() ) {
			currentResearches.put(resRow.getInt("forschung"), resRow.getInt("dauer"));
		}
		resRow.free();

		for( String mykey : keys.keySet() ) {
			Map<Integer,Forschung> var = keys.get(mykey);
			
			int count = 0;
	
			if( var.size() == 0 ) {
				continue;	
			}
	
			String prefix = "";
			if( mykey.equals("researchable") ) {
				prefix = "researchable.";	
			}
	
			for( Forschung result : var.values() ) {
				t.set_var(	"tech.id",			result.getID(),
						  	"tech.name",		Common._title(result.getName()),
						  	"tech.dauer",		result.getTime(),
						  	"res.list",			"",
						  	"tech.remaining",	currentResearches.get(result.getID()) );

				// Kosten der Forschung ausgeben
				Cargo costs = new Cargo(Cargo.Type.STRING, result.getCosts());
				costs.setOption(Cargo.Option.SHOWMASS, false);

				ResourceList reslist = costs.getResourceList();
				int respos = 0;
		
				for( ResourceEntry res : reslist ) {
					t.set_var(	"waren",			"",
								"warenb",			res.getImage(),
								"tech.cost",		res.getCargo1(),
							  	"tech.cost.space",	(respos < reslist.size() - 1 ? 1 : 0 ) );
							  			
					t.parse("res.list","res.listitem",true);
			
					respos++;
				}
	
				boolean resentry = false;
	
				// Benoetigte Forschungen ausgeben
				if( !mykey.equals("researchable") ) {
					for( int i=1; i <= 3; i++ ) {
						if( result.getRequiredResearch(i) > 0) {
							String req = researchCache.get(result.getRequiredResearch(i)).getName();
							
							t.set_var(	"tech.req"+i+".id",		result.getRequiredResearch(i),
										"tech.req"+i+".name",	req );
											  
							resentry = true;
						}
						else if( (result.getRequiredResearch(i) < 0) && (user.getAccessLevel() >= 20) ) {
							t.set_var(	"tech.req"+i+".id",		"1",
										"tech.req"+i+".name",	"<span style=\"color:#C7C7C7; font-weight:normal\">### Nicht erf&uuml;llbar</span>");
											  
							resentry = true;
						}
						else {
							t.set_var(	"tech.req"+i+".id",		"",
										"tech.req"+i+".name",	"");
						}
					}
				}
				if( !resentry ) { 
					t.set_var("tech.noreq",1);
				}
				else {
					t.set_var("tech.noreq",0);
				}
		
				count++;
		
				if( count % 3 == 0 ) {
					t.set_var( "tech.newline", 1 );	
				}
				else {
					t.set_var( "tech.newline", 0 );	
				}

				t.parse("tech."+mykey+".list","tech."+prefix+"listitem",true);
			}
		}
	}
}

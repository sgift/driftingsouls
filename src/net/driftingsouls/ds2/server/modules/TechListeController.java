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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.Rasse;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.Forschungszentrum;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Zeigt die Liste der erforschten/nicht erforschten/erforschbaren Technologien
 * an.
 * @author Christopher Jung
 *
 * @urlparam Integer rasse Die Rasse, deren Technologien angezeigt werden sollen
 */
@Configurable
@Module(name="techliste")
public class TechListeController extends TemplateGenerator {
	private Configuration config;

	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public TechListeController(Context context) {
		super(context);
		parameterNumber("rasse");

		setTemplate("techliste.html");

		setPageTitle("Forschungen");
	}

    /**
     * Injiziert die DS-Konfiguration.
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config)
    {
    	this.config = config;
    }

	@Override
	protected boolean validateAndPrepare(String action) {
		// EMPTY
		return true;
	}

	/**
	 * Zeigt die Techliste an.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();

		int rasse = getInteger("rasse");

		if( rasse == 0 )
		{
			rasse = user.getRace();
		}
		else if( Rassen.get().rasse(rasse) == null )
		{
			rasse = user.getRace();
		}
		else if( !Rassen.get().rasse(rasse).isExtPlayable() )
		{
			rasse = user.getRace();
		}

		StringBuilder rassenliste = new StringBuilder(100);

		for( Rasse aRasse : Rassen.get() ) {
			if( aRasse.isExtPlayable() ) {
				rassenliste.append("<a href='"+Common.buildUrl("default", "rasse", aRasse.getID())+"'>");
				rassenliste.append("<img style='border:0px' src='"+config.get("URL")+"data/interface/rassen/"+aRasse.getID()+".png' />");
				rassenliste.append("</a>");
			}
		}

		String rassenlisteStr = rassenliste.toString();

		t.setVar(	"race.name",	Rassen.get().rasse(rasse).getName(),
					"race.list",	rassenlisteStr );

		Map<Integer,Forschung>  researched = new LinkedHashMap<Integer,Forschung>();
		Map<Integer,Forschung>  researchable = new LinkedHashMap<Integer,Forschung>();
		Map<Integer,Forschung>  notResearchable = new LinkedHashMap<Integer,Forschung>();
		Map<Integer,Forschung>  invisible = new LinkedHashMap<Integer,Forschung>();

		//Alle Forschungen durchgehen
		final Iterator<?> forschungIter = db.createQuery("from Forschung order by name")
			.iterate();
		while( forschungIter.hasNext() ) {
			Forschung f = (Forschung)forschungIter.next();

			if( !Rassen.get().rasse(rasse).isMemberIn(f.getRace()) ) {
				continue;
			}

			// Status der Forschung (erforscht/verfuegbar/nicht verfuegbar) ermitteln
			if( f.isVisibile(user) && user.hasResearched(f.getID()) ) {
				researched.put(f.getID(), f);
			}
			else if( f.isVisibile(user) && user.hasResearched(f.getRequiredResearch(1)) && user.hasResearched(f.getRequiredResearch(2)) &&
					user.hasResearched(f.getRequiredResearch(3)) ) {
				researchable.put(f.getID(), f);
			}
			else if( !f.isVisibile(user) ) {
				if( hasPermission("forschung", "allesSichtbar") ) {
					invisible.put(f.getID(), f);
				}
			}
			else {
				notResearchable.put(f.getID(), f);
			}
		}

		t.setBlock("_TECHLISTE","tech.listitem","none");

		t.setBlock("_TECHLISTE","tech.researchable.listitem","none");
		t.setBlock("tech.researchable.listitem","res.listitem", "res.list" );

		Map<String,Map<Integer,Forschung>> keys = new LinkedHashMap<String,Map<Integer,Forschung>>();
		keys.put("researched", researched);
		keys.put("researchable", researchable);
		keys.put("notResearchable", notResearchable);
		keys.put("invisible", invisible);

		Map<Integer,Integer> currentResearches = new HashMap<Integer,Integer>();
		List<?> resList = db.createQuery("from Forschungszentrum where forschung is not null and base.owner=:owner")
			.setEntity("owner", user)
			.list();
		for( Iterator<?> iter=resList.iterator(); iter.hasNext(); ) {
			Forschungszentrum fz = (Forschungszentrum)iter.next();
			currentResearches.put(fz.getForschung().getID(), fz.getDauer());
		}

		for( Map.Entry<String, Map<Integer, Forschung>> entry: keys.entrySet() ) {
			String mykey = entry.getKey();
			Map<Integer,Forschung> var = entry.getValue();

			int count = 0;

			if( var.size() == 0 ) {
				continue;
			}

			String prefix = "";
			if( mykey.equals("researchable") ) {
				prefix = "researchable.";
			}

			for( Forschung result : var.values() ) {
				t.setVar(	"tech.id",			result.getID(),
							"tech.image",		result.getImage(),
						  	"tech.name",		Common._title(result.getName()),
						  	"tech.dauer",		result.getTime(),
						  	"res.list",			"",
						  	"tech.remaining",	currentResearches.get(result.getID()),
						  	"tech.specpoints",	result.getSpecializationCosts());

				// Kosten der Forschung ausgeben
				Cargo costs = result.getCosts();
				costs.setOption(Cargo.Option.SHOWMASS, false);

				ResourceList reslist = costs.getResourceList();
				int respos = 0;

				for( ResourceEntry res : reslist ) {
					t.setVar(	"waren",			"",
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
							String req = Forschung.getInstance(result.getRequiredResearch(i)).getName();

							t.setVar(	"tech.req"+i+".id",		result.getRequiredResearch(i),
										"tech.req"+i+".name",	req );

							resentry = true;
						}
						else if( (result.getRequiredResearch(i) < 0) && hasPermission("forschung", "allesSichtbar") ) {
							t.setVar(	"tech.req"+i+".id",		"1",
										"tech.req"+i+".name",	"<span style=\"color:#C7C7C7; font-weight:normal\">### Nicht erf&uuml;llbar</span>");

							resentry = true;
						}
						else {
							t.setVar(	"tech.req"+i+".id",		"",
										"tech.req"+i+".name",	"");
						}
					}
				}
				if( !resentry ) {
					t.setVar("tech.noreq",1);
				}
				else {
					t.setVar("tech.noreq",0);
				}

				count++;

				if( count % 3 == 0 ) {
					t.setVar( "tech.newline", 1 );
				}
				else {
					t.setVar( "tech.newline", 0 );
				}

				t.parse("tech."+mykey+".list","tech."+prefix+"listitem",true);
			}
		}
	}
}

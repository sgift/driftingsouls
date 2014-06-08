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

import net.driftingsouls.ds2.server.WellKnownPermission;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.Forschungszentrum;
import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Controller;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Zeigt die Liste der erforschten/nicht erforschten/erforschbaren Technologien
 * an.
 * @author Christopher Jung
 *
 */
@Module(name="techliste")
public class TechListeController extends Controller
{
	private TemplateViewResultFactory templateViewResultFactory;

	@Autowired
	public TechListeController(TemplateViewResultFactory templateViewResultFactory)
	{
		this.templateViewResultFactory = templateViewResultFactory;
		setPageTitle("Forschungen");
	}

	/**
	 * Zeigt die Techliste an.
	 * @param rasse Die Rasse, deren Technologien angezeigt werden sollen
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction(int rasse) {
		org.hibernate.Session db = getDB();
		TemplateEngine t = templateViewResultFactory.createFor(this);
		User user = (User)getUser();

		if( rasse == 0 )
		{
			rasse = user.getRace();
		}
		else if( Rassen.get().rasse(rasse) == null )
		{
			rasse = user.getRace();
		}
		else if( !Rassen.get().rasse(rasse).isExtPlayable() && !Rassen.get().rasse(rasse).isPlayable() )
		{
			rasse = user.getRace();
		}

		StringBuilder rassenliste = new StringBuilder(100);

		for( Rasse aRasse : Rassen.get() ) {
			if( aRasse.isExtPlayable() || aRasse.isPlayable() ) {
				rassenliste.append("<a href='").append(Common.buildUrl("default", "rasse", aRasse.getId())).append("'>");
				rassenliste.append("<img style='border:0px' src='./data/interface/rassen/").append(aRasse.getId()).append(".png' />");
				rassenliste.append("</a>");
			}
		}

		String rassenlisteStr = rassenliste.toString();

		t.setVar(	"race.name",	Rassen.get().rasse(rasse).getName(),
					"race.list",	rassenlisteStr );

		Map<Integer,Forschung>  researched = new LinkedHashMap<>();
		Map<Integer,Forschung>  researchable = new LinkedHashMap<>();
		Map<Integer,Forschung>  notResearchable = new LinkedHashMap<>();
		Map<Integer,Forschung>  invisible = new LinkedHashMap<>();

		//Alle Forschungen durchgehen
		gruppiereForschungenNachStatus(rasse, db, user, researched, researchable, notResearchable, invisible);

		t.setBlock("_TECHLISTE","tech.listitem","none");

		t.setBlock("_TECHLISTE","tech.researchable.listitem","none");
		t.setBlock("tech.researchable.listitem","res.listitem", "res.list" );

		Map<String,Collection<Forschung>> keys = new LinkedHashMap<>();
		keys.put("researched", researched.values());
		keys.put("researchable", researchable.values());
		keys.put("notResearchable", notResearchable.values());
		keys.put("invisible", invisible.values());

		Map<Integer, Integer> currentResearches = ermittleLaufendeForschungen(db, user);

		for( Map.Entry<String, Collection<Forschung>> entry: keys.entrySet() ) {
			String mykey = entry.getKey();
			Collection<Forschung> var = entry.getValue();

			gruppeVonForschungenAnzeigen(t, currentResearches, mykey, var);
		}

		return t;
	}

	private void gruppeVonForschungenAnzeigen(TemplateEngine t, Map<Integer, Integer> currentResearches, String gruppenname, Collection<Forschung> forschungsliste)
	{
		int count = 0;

		if( forschungsliste.size() == 0 ) {
			return;
		}

		String prefix = "";
		if( gruppenname.equals("researchable") ) {
			prefix = "researchable.";
		}

		for( Forschung result : forschungsliste) {
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
			if( !gruppenname.equals("researchable") ) {
				for( int i=1; i <= 3; i++ ) {
					Forschung forschung = result.getRequiredResearch(i);
					if( forschung != null && (forschung.isVisibile((User)getUser()) || hasPermission(WellKnownPermission.FORSCHUNG_ALLES_SICHTBAR)) ) {
						String req = forschung.getName();

						t.setVar(	"tech.req"+i+".id", forschung,
									"tech.req"+i+".name",	req );

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

			t.parse("tech."+ gruppenname +".list","tech."+prefix+"listitem",true);
		}
	}

	private Map<Integer, Integer> ermittleLaufendeForschungen(Session db, User user)
	{
		Map<Integer,Integer> currentResearches = new HashMap<>();
		List<?> resList = db.createQuery("from Forschungszentrum where forschung is not null and base.owner=:owner")
			.setEntity("owner", user)
			.list();
		for (Object aResList : resList)
		{
			Forschungszentrum fz = (Forschungszentrum) aResList;
			currentResearches.put(fz.getForschung().getID(), fz.getDauer());
		}
		return currentResearches;
	}

	private void gruppiereForschungenNachStatus(int rasse, Session db, User user, Map<Integer, Forschung> researched, Map<Integer, Forschung> researchable, Map<Integer, Forschung> notResearchable, Map<Integer, Forschung> invisible)
	{
		final Iterator<?> forschungIter = db.createQuery("from Forschung order by name")
			.iterate();
		while( forschungIter.hasNext() ) {
			Forschung f = (Forschung)forschungIter.next();

			if( !Rassen.get().rasse(rasse).isMemberIn(f.getRace()) ) {
				continue;
			}

			// Status der Forschung (erforscht/verfuegbar/nicht verfuegbar) ermitteln
			if( f.isVisibile(user) && user.hasResearched(f) ) {
				researched.put(f.getID(), f);
			}
			else if( f.isVisibile(user) && user.hasResearched(f.getBenoetigteForschungen()) ) {
				researchable.put(f.getID(), f);
			}
			else if( !f.isVisibile(user) ) {
				if( hasPermission(WellKnownPermission.FORSCHUNG_ALLES_SICHTBAR) ) {
					invisible.put(f.getID(), f);
				}
			}
			else {
				notResearchable.put(f.getID(), f);
			}
		}
	}
}

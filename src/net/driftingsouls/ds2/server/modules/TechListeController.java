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
import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.modules.viewmodels.ResourceEntryViewModel;
import org.hibernate.Session;

import java.util.ArrayList;
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
	@ViewModel
	public static class RasseViewModel
	{
		public int id;
		public String picture;
	}

	@ViewModel
	public static class TechListeViewModel
	{
		public List<RasseViewModel> auswaehlbareRassen = new ArrayList<>();
		public String rassenName;
		public List<ForschungViewModel> erforscht = new ArrayList<>();
		public List<ForschungViewModel> erforschbar = new ArrayList<>();
		public List<ForschungViewModel> nichtErforscht = new ArrayList<>();
		public List<ForschungViewModel> unsichtbar = new ArrayList<>();
	}

	@ViewModel
	public static class BenoetigteForschungViewModel
	{
		public int id;
		public String name;
	}

	@ViewModel
	public static class ForschungViewModel
	{
		public int id;
		public String image;
		public String name;
		public int dauer;
		public Integer verbleibendeDauer;
		public int spezialisierungspunkte;
		public List<ResourceEntryViewModel> kosten = new ArrayList<>();
		public List<BenoetigteForschungViewModel> benoetigteForschungen = new ArrayList<>();
	}

	@Action(ActionType.AJAX)
	public TechListeViewModel defaultAction(int rasse)
	{
		org.hibernate.Session db = getDB();
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

		TechListeViewModel viewModel = new TechListeViewModel();
		for( Rasse aRasse : Rassen.get() ) {
			if( aRasse.isExtPlayable() || aRasse.isPlayable() )
			{
				RasseViewModel rasseViewModel = new RasseViewModel();
				rasseViewModel.id = aRasse.getId();
				rasseViewModel.picture = "./data/interface/rassen/" + aRasse.getId() + ".png";
				viewModel.auswaehlbareRassen.add(rasseViewModel);
			}
		}

		viewModel.rassenName = Rassen.get().rasse(rasse).getName();

		Map<Integer,Forschung> researched = new LinkedHashMap<>();
		Map<Integer,Forschung> researchable = new LinkedHashMap<>();
		Map<Integer,Forschung> notResearchable = new LinkedHashMap<>();
		Map<Integer,Forschung> invisible = new LinkedHashMap<>();

		//Alle Forschungen durchgehen
		gruppiereForschungenNachStatus(rasse, db, user, researched, researchable, notResearchable, invisible);

		Map<Integer, Integer> currentResearches = ermittleLaufendeForschungen(db, user);

		viewModel.erforscht = gruppeVonForschungenAnzeigen(currentResearches, "researched", researched.values());
		viewModel.erforschbar = gruppeVonForschungenAnzeigen(currentResearches, "researchable", researchable.values());
		viewModel.nichtErforscht = gruppeVonForschungenAnzeigen(currentResearches, "notResearchable", notResearchable.values());
		viewModel.unsichtbar = gruppeVonForschungenAnzeigen(currentResearches, "invisible", invisible.values());

		return viewModel;
	}

	private List<ForschungViewModel> gruppeVonForschungenAnzeigen(Map<Integer, Integer> currentResearches, String gruppenname, Collection<Forschung> forschungsliste)
	{
		List<ForschungViewModel> result = new ArrayList<>();
		if( forschungsliste.size() == 0 ) {
			return result;
		}

		for( Forschung forschung : forschungsliste) {
			ForschungViewModel viewModel = new ForschungViewModel();

			viewModel.id = forschung.getID();
			viewModel.image = forschung.getImage();
			viewModel.name = Common._title(forschung.getName());
			viewModel.dauer = forschung.getTime();
			viewModel.verbleibendeDauer = currentResearches.get(forschung.getID());
			viewModel.spezialisierungspunkte = forschung.getSpecializationCosts();

			// Kosten der Forschung ausgeben
			Cargo costs = forschung.getCosts();
			costs.setOption(Cargo.Option.SHOWMASS, false);

			ResourceList reslist = costs.getResourceList();

			for( ResourceEntry res : reslist ) {
				viewModel.kosten.add(ResourceEntryViewModel.map(res));
			}

			// Benoetigte Forschungen ausgeben
			if( !gruppenname.equals("researchable") ) {
				for( int i=1; i <= 3; i++ ) {
					Forschung benoetigteForschung = forschung.getRequiredResearch(i);
					if( benoetigteForschung != null && (benoetigteForschung.isVisibile((User)getUser()) || hasPermission(WellKnownPermission.FORSCHUNG_ALLES_SICHTBAR)) )
					{
						BenoetigteForschungViewModel benoetigteForschungViewModel = new BenoetigteForschungViewModel();
						benoetigteForschungViewModel.id = benoetigteForschung.getID();
						benoetigteForschungViewModel.name = benoetigteForschung.getName();
						viewModel.benoetigteForschungen.add(benoetigteForschungViewModel);
					}
				}
			}

			result.add(viewModel);
		}

		return result;
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

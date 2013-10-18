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

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.entities.ally.AllyPosten;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParam;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import java.util.List;

/**
 * Zeigt die Liste der Allianzen sowie Allianzdetails.
 * @author Christopher Jung
 *
 */
@Module(name="allylist")
public class AllyListController extends TemplateGenerator {
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public AllyListController(Context context) {
		super(context);
		
		setTemplate("allylist.html");
		
		setPageTitle("Allianzliste");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();

		if( user.getAlly() != null ) {
			t.setVar(	"user.ally.name",		Common._title(user.getAlly().getName()),
						"user.ally.president",	(user.getId() == user.getAlly().getPresident().getId() ));
			
			addPageMenuEntry("Allgemeines", Common.buildUrl("default"));
			addPageMenuEntry("Mitglieder", Common.buildUrl("showMembers"));
			if( user.getId() == user.getAlly().getPresident().getId() ) {
				addPageMenuEntry("Einstellungen", Common.buildUrl("showAllySettings"));
				addPageMenuEntry("Posten", Common.buildUrl("showPosten"));
			}
			addPageMenuEntry("Kaempfe", Common.buildUrl("showBattles"));
			addPageMenuEntry("Allianzen auflisten", Common.buildUrl("default", "module", "allylist"));
			addPageMenuEntry("Austreten", Common.buildUrl("part"));
		}
		else {
			addPageMenuEntry("Allianz beitreten", Common.buildUrl("defaultNoAlly"));
			addPageMenuEntry("Allianz gruenden", Common.buildUrl("showCreateAlly"));
			addPageMenuEntry("Allianzen auflisten", Common.buildUrl("default", "module", "allylist"));
		}
					
		return true;
	}

	/**
	 * Setzt die Beziehungen des Spielers zu allen Mitgliedern der Allianz.
	 *
	 * @param ally     Die Allianz
	 * @param relation Die neue Beziehung
	 */
	@Action(ActionType.DEFAULT)
	public void changeRelationAction(@UrlParam(name="details") Ally ally, User.Relation relation) {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();

		if( ally == null ) {
			addError("Die angegebene Allianz existiert nicht");
			redirect();
			return;
		}
		
		if( (user.getAlly() != null) && (ally.getId() == user.getAlly().getId()) ) {
			addError("Sie k&ouml;nnen nicht die Beziehungen zu sich selbst &auml;ndern");
			redirect("details");
			return;
		}

		List<User> allymembers = ally.getMembers();
		for( User allymember : allymembers ) {
			user.setRelation(allymember.getId(), relation);
		}

		t.setVar("ally.message", "Beziehungsstatus ge&auml;ndert");	 
		
		redirect("details");
	}

	/**
	 * Setzt die Beziehungen der Allianz des Spielers zur ausgewaehlten Allianz.
	 * @param ally Die Allianz
	 * @param relation Die neue Beziehung
	 */
	@Action(ActionType.DEFAULT)
	public void changeRelationAllyAction(@UrlParam(name="details") Ally ally, User.Relation relation) {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();

		if( user.getAlly() == null ) {
			addError("Sie sind in keiner Allianz");
			redirect("details");
			return;
		}

		if( ally == null ) {
			addError("Die angegebene Allianz existiert nicht");
			redirect();
			return;
		}
		
		if( ally.getId() == user.getAlly().getId() ) {
			addError("Sie k&ouml;nnen nicht die Beziehungen zu sich selbst &auml;ndern");
			redirect("details");
			return;
		}
		
		if( user.getAlly().getPresident().getId() != user.getId() ) {
			addError("Sie sind nicht der Pr&auml;sident der Allianz");
			redirect("details");
			return;
		}

		List<User> users = user.getAlly().getMembers();
		for( User auser : users ) {
			List<User> allymembers = ally.getMembers();
			for( User allymember : allymembers ) {
				auser.setRelation(allymember.getId(), relation);
			}
		}
			
		t.setVar("ally.message", "Beziehungsstatus ge&auml;ndert");
		
		redirect("details");
	}
	
	/**
	 * Zeigt die Informationen zu einer Allianz an.
	 * @param ally Die Allianz
	 */
	@Action(ActionType.DEFAULT)
	public void detailsAction(@UrlParam(name="details") Ally ally) {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();

		if( ally == null ) {
			t.setVar( "ally.message", "Die angegebene Allianz existiert nicht" );
			
			return;
		}
		
		if( user.getAlly() != ally ) {
			t.setVar("user.changerelations", 1);
			
			if( (user.getAlly() != null) && (user.getAlly() != ally) ) {
				if( user.getAlly().getPresident() == user ) {		
					t.setVar("user.allyrelationchange", 1);
				}
			}
		}
		
		t.setVar("allylist.showally", ally.getId());

		User presi = ally.getPresident();
	
		t.setVar(	"ally",					ally,
					"ally.name",			Common._title(ally.getName()),
					"ally.description",		Common._text(ally.getDescription()),
					"ally.items.list",		"",
					"ally.pname",			Common._plaintitle(ally.getPname()),
					"ally.president.name",	Common._title(presi.getName()),
					"ally.minister.list",	"",
					"ally.addmembers.list",	"" );
	
		// Allitems ausgeben
	
		if( ally.getItems().length() > 0 ) {
			t.setBlock( "_ALLYLIST", "ally.items.listitem", "ally.items.list" );
	
			Cargo itemlist = new Cargo( Cargo.Type.ITEMSTRING, ally.getItems() );
			ResourceList reslist = itemlist.getResourceList();
			Resources.echoResList( t, reslist, "ally.items.list" );
		}
	
		// Minister ausgeben
		t.setBlock( "_ALLYLIST", "ally.minister.listitem", "ally.minister.list" );
		
		List<?> posten = getDB().createQuery("from AllyPosten as ap left join fetch ap.user " +
				"where ap.ally= :ally")
			.setEntity("ally", ally)
			.list();
		for (Object aPosten : posten)
		{
			AllyPosten aposten = (AllyPosten) aPosten;

			if (aposten.getUser() == null)
			{
				continue;
			}

			t.setVar("ally.minister.posten", Common._plaintitle(aposten.getName()),
					"ally.minister.id", aposten.getUser().getId(),
					"ally.minister.name", Common._title(aposten.getUser().getName()));

			t.parse("ally.minister.list", "ally.minister.listitem", true);
		}
	
		// Weitere Mitglieder ausgeben
		List<?> allymembers = getDB().createQuery("from User " +
				"where ally= :ally and " +
						"id!= :presidentId and " +
						"allyposten is null")
			.setEntity("ally", ally)
			.setInteger("presidentId", ally.getPresident().getId())
			.list();
		if( allymembers.size() > 0 ) {
			t.setBlock( "_ALLYLIST", "ally.addmembers.listitem", "ally.addmembers.list" );

			for (Object allymember1 : allymembers)
			{
				User allymember = (User) allymember1;

				t.setVar("ally.addmembers.name", Common._title(allymember.getName()),
						"ally.addmembers.id", allymember.getId());

				t.parse("ally.addmembers.list", "ally.addmembers.listitem", true);
			}
		}
	}
	
	/**
	 * Zeigt die Liste der Allianzen in DS an.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		
		t.setVar("allylist.showlist", 1);	
		
		t.setBlock( "_ALLYLIST", "allylist.ally.listitem", "allylist.ally.list" );
	
		List<?> allies = getDB().createQuery("from Ally order by founded").list();
		for (Object ally1 : allies)
		{
			Ally ally = (Ally) ally1;

			String name = "<a class=\"forschinfo\" href=\"" + Common.buildUrl("details", "details", ally.getId()) + "\">" + Common._title(ally.getName()) + "</a>";

			if (ally.getHp().length() > 0)
			{
				name += " <a class=\"forschinfo\" target=\"_blank\" href=\"" + ally.getHp() + "\">[HP]</a>";
			}

			t.setVar("allylist.ally", ally,
					"allylist.ally.name", name);

			t.parse("allylist.ally.list", "allylist.ally.listitem", true);
		}
	}
}

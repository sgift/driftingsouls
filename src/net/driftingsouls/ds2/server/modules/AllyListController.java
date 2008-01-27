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
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.UserIterator;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Zeigt die Liste der Allianzen sowie Allianzdetails
 * @author Christopher Jung
 *
 */
public class AllyListController extends DSGenerator {
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public AllyListController(Context context) {
		super(context);
		
		setTemplate("allylist.html");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		if( user.getAlly() != 0 ) {
			SQLResultRow ally = db.first("SELECT name,president FROM ally WHERE id="+user.getAlly());
	
			t.setVar(	"user.ally.name",		Common._title(ally.getString("name")),
						"user.ally.president",	(user.getId() == ally.getInt("president") ));
		}
					
		return true;
	}
	
	/**
	 * Setzt die Beziehungen des Spielers zu allen Mitgliedern der Allianz
	 * @urlparam Integer details Die ID der Allianz
	 * @urlparam Integer relation Die neue Beziehung. 1 fuer feindlich, 2 fuer freundlich und neural bei allen anderen Werten
	 *
	 */
	public void changeRelationAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		this.parameterNumber("details");
		int details = getInteger("details");
		SQLResultRow ally = db.first("SELECT * FROM ally WHERE id=",details);
		
		if( ally.isEmpty() ) {
			addError("Die angegebene Allianz existiert nicht");
			redirect();
			return;
		}
		
		if( ally.getInt("id") == user.getAlly() ) {
			addError("Sie k&ouml;nnen nicht die Beziehungen zu sich selbst &auml;ndern");
			redirect("details");
			return;
		}
		
		parameterNumber("relation");
		int relation = getInteger("relation");
		
		User.Relation rel = User.Relation.NEUTRAL;
		switch( relation ) {
		case 1: 
			rel = User.Relation.ENEMY;
			break;
		case 2:
			rel = User.Relation.FRIEND;
			break;
		}
		
		SQLQuery allymember = db.query("SELECT id FROM users WHERE ally=",ally.getInt("id"));
		while( allymember.next() ) {
			user.setRelation(allymember.getInt("id"), rel);
		}
		allymember.free();

		t.setVar("ally.message", "Beziehungsstatus ge&auml;ndert");	 
		
		redirect("details");
	}

	/**
	 * Setzt die Beziehungen der Allianz des Spielers zur ausgewaehlten Allianz
	 * @urlparam Integer details Die ID der Allianz
	 * @urlparam Integer relation Die neue Beziehung. 1 fuer feindlich, 2 fuer freundlich und neural bei allen anderen Werten
	 */
	public void changeRelationAllyAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		if( user.getAlly() == 0 ) {
			addError("Sie sind in keiner Allianz");
			redirect("details");
			return;
		}
		
		parameterNumber("details");
		int details = getInteger("details");
		SQLResultRow ally = db.first("SELECT * FROM ally WHERE id=",details);
		
		if( ally.isEmpty() ) {
			addError("Die angegebene Allianz existiert nicht");
			redirect();
			return;
		}
		
		if( ally.getInt("id") == user.getAlly() ) {
			addError("Sie k&ouml;nnen nicht die Beziehungen zu sich selbst &auml;ndern");
			redirect("details");
			return;
		}
		
		int allypresi = db.first("SELECT president FROM ally WHERE id=",user.getAlly()).getInt("president");
		if( allypresi != user.getId() ) {
			addError("Sie sind nicht der Pr&auml;sident der Allianz");
			redirect("details");
			return;
		}
		
		parameterNumber("relation");
		int relation = getInteger("relation");
		User.Relation rel = User.Relation.NEUTRAL;
		switch( relation ) {
		case 1: 
			rel = User.Relation.ENEMY;
			break;
		case 2:
			rel = User.Relation.FRIEND;
			break;
		}
		
		db.tBegin();
		UserIterator iter = getContext().createUserIterator("SELECT * FROM users WHERE ally=",user.getAlly());
		for( User auser : iter ) {
			SQLQuery allymember = db.query("SELECT id FROM users WHERE ally=",ally.getInt("id"));
			while( allymember.next() ) {
				auser.setRelation(allymember.getInt("id"), rel);
			}
			allymember.free();
		}
		iter.free();
		db.tCommit();
			
		t.setVar("ally.message", "Beziehungsstatus ge&auml;ndert");
		
		redirect("details");
	}
	
	/**
	 * Zeigt die Informationen zu einer Allianz an
	 * @urlparam Integer details Die ID der anzuzeigenden Allianz
	 *
	 */
	public void detailsAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		parameterNumber("details");
		int details = getInteger("details");
		
		SQLResultRow ally = db.first("SELECT * FROM ally WHERE id=",details);
		if( ally.isEmpty() ) {
			t.setVar( "ally.message", "Die angegebene Allianz existiert nicht" );
			
			return;
		}
		
		if( user.getAlly() != ally.getInt("id") ) {
			t.setVar("user.changerelations", 1);
			
			if( (user.getAlly() != 0) && (user.getAlly() != ally.getInt("id")) ) {
				int allypresi = getDatabase().first("SELECT president FROM ally WHERE id=",user.getAlly()).getInt("president");
		
				if( allypresi == user.getId() ) {		
					t.setVar("user.allyrelationchange", 1);
				}
			}
		}
		
		t.setVar("allylist.showally", details);	

		User presi = getContext().createUserObject(ally.getInt("president"));
		int membercount = db.first("SELECT count(*) count FROM users WHERE ally=",ally.getInt("id")).getInt("count");
	
		t.setVar(	"ally.id",				ally.getInt("id"), 
					"ally.name",			Common._title(ally.getString("name")),
					"ally.description",		Common._text(ally.getString("description")),
					"ally.founded",			ally.getString("founded"),
					"ally.wonBattles",		ally.getInt("wonBattles"),
					"ally.lostBattles",		ally.getInt("lostBattles"),
					"ally.destroyedShips",	ally.getInt("destroyedShips"),
					"ally.lostShips",		ally.getInt("lostShips"),
					"ally.membercount",		membercount,
					"ally.items.list",		"",
					"ally.pname",			Common._plaintitle(ally.getString("pname")),
					"ally.president.id",	ally.getInt("president"),
					"ally.president.name",	Common._title(presi.getName()),
					"ally.minister.list",	"",
					"ally.addmembers.list",	"" );
	
		// Allitems ausgeben
	
		if( ally.getString("items").length() > 0 ) {
			t.setBlock( "_ALLYLIST", "ally.items.listitem", "ally.items.list" );
	
			Cargo itemlist = new Cargo( Cargo.Type.ITEMSTRING, ally.getString("items") );
			ResourceList reslist = itemlist.getResourceList();
			Resources.echoResList( t, reslist, "ally.items.list" );
		}
	
		// Minister ausgeben
		t.setBlock( "_ALLYLIST", "ally.minister.listitem", "ally.minister.list" );
		
		SQLQuery posten = db.query("SELECT t1.id,t1.name,t2.name username,t2.id as userid FROM ally_posten t1 JOIN users t2 ON t1.id=t2.allyposten WHERE t1.ally=",ally.getInt("id")," AND t2.ally=",ally.getInt("id"));
		while( posten.next() ) {
			t.setVar(	"ally.minister.posten",	Common._plaintitle(posten.getString("name")),
						"ally.minister.id",		posten.getInt("userid"),
						"ally.minister.name",	Common._title(posten.getString("username")) );
		
			t.parse( "ally.minister.list", "ally.minister.listitem", true );
		}
		posten.free();
	
		// Weitere Mitglieder ausgeben
		SQLQuery allymember = db.query("SELECT id, name FROM users WHERE ally=",ally.getInt("id")," AND id!=",ally.getInt("president")," AND allyposten=0");
		if( allymember.numRows() > 0 ) {
			t.setBlock( "_ALLYLIST", "ally.addmembers.listitem", "ally.addmembers.list" );
		
			while( allymember.next() ) {
				t.setVar(	"ally.addmembers.name",	Common._title(allymember.getString("name")),
							"ally.addmembers.id",	allymember.getInt("id") );
			
				t.parse( "ally.addmembers.list", "ally.addmembers.listitem", true );
			}
		}
		allymember.free();
	}
	
	/**
	 * Zeigt die Liste der Allianzen in DS an
	 */
	@Override
	public void defaultAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		t.setVar("allylist.showlist", 1);	
		
		t.setBlock( "_ALLYLIST", "allylist.ally.listitem", "allylist.ally.list" );
	
		SQLQuery ally = db.query("SELECT id,name,hp FROM ally ORDER BY founded");
		while( ally.next() ) {
			String name = "<a class=\"forschinfo\" href=\""+Common.buildUrl(getContext(), "details", "details", ally.getInt("id"))+"\">"+Common._title(ally.getString("name"))+"</a>";

			if( ally.getString("hp").length() > 0 ) {
				name += " <a class=\"forschinfo\" target=\"_blank\" href=\""+ally.getString("hp")+"\">[HP]</a>";
			}
		
			t.setVar(	"allylist.ally.id",		ally.getInt("id"),
						"allylist.ally.name",	name );
								
			t.parse( "allylist.ally.list", "allylist.ally.listitem", true );
		}
		ally.free();
	}
}

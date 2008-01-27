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

import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.config.Medals;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Zeigt das Profil eines Benutzers an
 * 
 * @author Christopher Jung
 * @urlparam Integer user Die ID des anzuzeigenden Benutzers
 *
 */
public class UserProfileController extends DSGenerator {
	private User user = null;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public UserProfileController(Context context) {
		super(context);
		
		setTemplate("userprofile.html");
		
		parameterNumber("user");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		
		User user = getUser();
		
		User auser = getContext().createUserObject(getInteger("user"));
		if( (auser.getId() == 0) || (auser.hasFlag(User.FLAG_HIDE) && (user.getAccessLevel() < 20)) ) {
			addError( "Ihnen ist kein Benutzer unter der angegebenen ID bekannt", Common.buildUrl(getContext(), "default", "module", "ueber") );
			
			return false;	
		}
		
		this.user = auser;
		
		return true;
	}

	/**
	 * Setzt die Beziehung des Users mit dem aktuell angezeigtem User
	 * @urlparam Integer relation Die neue Beziehung. 1 fuer feindlich, 2 fuer freundlich und neural bei allen anderen Werten
	 */
	public void changeRelationAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		
		if( this.user.getId() == user.getId() ) {
			redirect();
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
		
		user.setRelation(this.user.getId(), rel);
		t.setVar("userprofile.message", "Beziehungsstatus ge&auml;ndert");	
		
		redirect();
	}
	
	/**
	 * Setzt die Beziehung aller User der Ally des aktiven Users mit dem aktuell angezeigtem User.
	 * Die Operation kann nur vom Allianzpraesidenten ausgefuehrt werden.
	 * @urlparam Integer relation Die neue Beziehung. 1 fuer feindlich, 2 fuer freundlich und neural bei allen anderen Werten
	 */
	public void changeRelationAllyAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
	
		if( user.getAlly() == 0 ) {
			addError("Sie sind in keiner Allianz");
			redirect();
			return;
		}
		
		if( user.getAlly() == this.user.getAlly() ) {
			addError("Sie befinden sich in der selben Allianz");
			redirect();
			return;
		}
		
		int allypresi = db.first("SELECT president FROM ally WHERE id='",user.getAlly(),"'").getInt("president");
		if( allypresi != user.getId() ) {
			addError("Sie sind nicht der Pr&auml;sident der Allianz");
			redirect();
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
		
		SQLQuery allymember = db.query("SELECT id FROM users WHERE ally='",user.getAlly(),"'");
		while( allymember.next() ) {
			User auser = getContext().createUserObject(allymember.getInt("id"));
			auser.setRelation(this.user.getId(), rel);
		}
		allymember.free();
		
		t.setVar("userprofile.message", "Beziehungsstatus ge&auml;ndert");	
		
		redirect();
	}
	
	/**
	 * Zeigt die Daten des angegebenen Benutzers an
	 */
	@Override
	public void defaultAction() {		
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
		this.user.setTemplateVars(t);
		
		if( this.user.getAlly() != 0 ) {
			SQLResultRow ally = getDatabase().first("SELECT name,president,pname FROM ally WHERE id='",this.user.getAlly(),"'");
			t.setVar(	"user.ally.name",	Common._title(ally.getString("name")),
						"user.ally.id",		this.user.getAlly() );
			
			String pstatus = "";
			if( ally.getInt("president") == this.user.getId() ) {
				pstatus = "<span style=\"font-weight:bold; font-style:italic\">"+Common._plaintitle(ally.getString("pname"))+"</span>";
			}
			
			if( this.user.getAllyPosten() != 0 ) {
				String postenname = getDatabase().first("SELECT name FROM ally_posten WHERE id='"+this.user.getAllyPosten()+"'").getString("name");
				t.setVar("user.ally.position", (pstatus.length() != 0 ? pstatus+", " : "")+Common._plaintitle(postenname) );				
			}
			else {
				t.setVar("user.ally.position", pstatus );
			}
		}
		
		if( (user.getAlly() != 0) && (user.getAlly() != this.user.getAlly()) ) {
			int allypresi = getDatabase().first("SELECT president FROM ally WHERE id='",user.getAlly(),"'").getInt("president");
		
			if( allypresi == user.getId() ) {		
				t.setVar("user.allyrelationchange", 1);
			}
		}
		
		if( user.getId() != this.user.getId() ) {
			if( (user.getAlly() == 0) || (user.getAlly() !=  this.user.getAlly()) ) {
				User.Relation relation = user.getRelation(this.user.getId());
			
				if( relation == User.Relation.ENEMY ) {
					t.setVar( "relation.enemy", 1 );
				}
				else if( relation == User.Relation.NEUTRAL ) {
					t.setVar( "relation.neutral", 1 );	
				}
				else {
					t.setVar( "relation.friend", 1 );	
				}
			}
		}
		
		t.setVar(	"user.name",		Common._title(this.user.getName()),
					"user.rasse.name",	Rassen.get().rasse(this.user.getRace()).getName(),
					"user.rang",		this.user.getRang(),
					"user.rang.name",	Medals.get().rang(this.user.getRang()).getName(),
					"user.signupdate",	(this.user.getSignup() > 0 ? Common.date("d.m.Y H:i:s",this.user.getSignup()) : "schon immer" ));
		
		// Beziehung		
		String relname = "neutral";
		String relcolor = "#c7c7c7";
		if( user.getId() != this.user.getId() ) {
			User.Relation relation = this.user.getRelation(user.getId());
			switch( relation ) {
			case ENEMY:
				relname = "feindlich";
				relcolor = "#E00000";
				break;
			case FRIEND:
				relname = "freundlich";
				relcolor = "#00E000";
				break;
			}
		}
		
		t.setVar(	"user.relation",		relname,
					"user.relation.color",	relcolor );
							
		// History
		t.setBlock("_USERPROFILE", "history.listitem", "history.list");
		if( this.user.getHistory().length() != 0 ) {
			String[] history = StringUtils.split(StringUtils.replace(this.user.getHistory(),"\r\n", "\n"), "\n" );
		
			for( int i=0; i < history.length; i++ ) {
				t.setVar( "history.line", Common._title(history[i], new String[0]) );
			
				t.parse("history.list", "history.listitem", true);	
			}
		}
		
		// Orden					
		t.setBlock("_USERPROFILE", "medals.listitem", "medals.list");
		
		if( this.user.getMedals().length() != 0 ) {
			int[] medals = Common.explodeToInt(";", this.user.getMedals());
			
			for( int i=0; i < medals.length; i++ ) {
				int medal = medals[i];
				if( Medals.get().medal(medal) == null ) {
					continue;	
				}
				t.setVar(	"medal.index",				i,
							"medal.image",				Medals.get().medal(medal).getImage(Medal.IMAGE_NORMAL),
							"medal.image.highlight",	Medals.get().medal(medal).getImage(Medal.IMAGE_HIGHLIGHT) );
							
				t.parse("medals.list", "medals.listitem", true);
			}
		}			 
	}
}

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

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Ansicht eines bestimmten Offiziers. Die Ansicht bietet eine Uebersicht
 * ueber die Daten des Offiziers. Der Spieler hat die Moeglichkeit
 * den Namen des Offiziers zu aendern.
 * @author Christopher Jung
 *
 */
@Module(name="choff")
public class ChoffController extends TemplateGenerator {
	private Offizier offizier;
	
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public ChoffController(Context context) {
		super(context);
		
		setTemplate("choff.html");
		
		parameterNumber("off");
		
		setPageTitle("Offizier");
	}
	
	@Override
	protected boolean validateAndPrepare( String action ) {
		User user = (User)getUser();
		
		int off = getInteger("off");

		Offizier offizier = (Offizier)getDB().get(Offizier.class, off);		
		if( offizier == null ) {
			addError("Der angegebene Offizier ist ung&uuml;ltig", Common.buildUrl("default", "module", "ueber") );
			
			return false;
		}

		if( offizier.getOwner() != user ) {
			addError("Dieser Offizier untersteht nicht ihrem Kommando", Common.buildUrl("default", "module", "ueber") );
			
			return false;
		}
		
		this.offizier = offizier;

		return true;	
	}
	
	/**
	 * Benennt einen Offizier um.
	 * @urlparam String name Der neue Name des Offiziers 
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void renameAction()
    {
		TemplateEngine t = getTemplateEngine();

		parameterString("name");
		
		String name = getString("name");
		if( name.length() != 0 )
        {
            int MAX_NAME_LENGTH = 60; //See db/offiziere_create.sql
            if(name.length() > MAX_NAME_LENGTH)
            {
                t.setVar("choff.message", "<span style=\"color:red\">Der eingegebene Name ist zu lang (maximal "+ MAX_NAME_LENGTH +" Zeichen)</span>");
            }
            else
            {
                offizier.setName(name);
                t.setVar("choff.message", "Der Name wurde in "+Common._plaintitle(name)+" ge&auml;ndert");
            }
		}
		else
        {
			t.setVar("choff.message", "<span style=\"color:red\">Sie m&uuml;ssen einen Namen angeben</span>");
		}
	
		redirect();	
	}
	
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();

		t.setVar(	"offizier.id",			offizier.getID(),
					"offizier.name",		Common._plaintitle(offizier.getName()),
					"offizier.picture",		offizier.getPicture(),
					"offizier.ability.ing",	offizier.getAbility( Offizier.Ability.ING ),
					"offizier.ability.waf",	offizier.getAbility( Offizier.Ability.WAF ),
					"offizier.ability.nav",	offizier.getAbility( Offizier.Ability.NAV ),
					"offizier.ability.sec",	offizier.getAbility( Offizier.Ability.SEC ),
					"offizier.ability.com",	offizier.getAbility( Offizier.Ability.COM ),
					"offizier.special",		offizier.getSpecial().getName(),
					"base.id",				offizier.getStationiertAufBasis() != null ? offizier.getStationiertAufBasis().getId() : 0,
					"ship.id",				offizier.getStationiertAufSchiff() != null ? offizier.getStationiertAufSchiff().getId() : 0 );
	}


}

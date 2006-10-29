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
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Ansicht eines bestimmten Offiziers. Die Ansicht bietet eine Uebersicht
 * ueber die Daten des Offiziers. Der Spieler hat die Moeglichkeit
 * den Namen des Offiziers zu aendern.
 * @author Christopher Jung
 *
 */
public class ChoffController extends DSGenerator {
	private Offizier offizier;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public ChoffController(Context context) {
		super(context);
		
		setTemplate("choff.html");
		
		parameterNumber("off");		
	}
	
	@Override
	protected boolean validateAndPrepare( String action ) {
		Database db = getDatabase();
		User user = getUser();
		
		int off = getInteger("off");

		SQLResultRow offizierRow = db.first("SELECT * FROM offiziere WHERE id=",off);		
		if( offizierRow.isEmpty() ) {
			addError("Der angegebene Offizier ist ung&uuml;ltig", Common.buildUrl(getContext(), "default", "module", "ueber") );
			
			return false;
		}

		Offizier offizier = new Offizier( offizierRow );

		if( offizier.getOwner() != user.getID() ) {
			addError("Dieser Offizier untersteht nicht ihrem Kommando", Common.buildUrl(getContext(), "default", "module", "ueber") );
			
			return false;
		}
		
		this.offizier = offizier;

		return true;	
	}
	
	/**
	 * Benennt einen Offizier um
	 * @urlparam String name Der neue Name des Offiziers 
	 *
	 */
	public void renameAction() {
		TemplateEngine t = getTemplateEngine();

		parameterString("name");
		
		String name = getString("name");
		if( name.length() != 0 ) {
			offizier.setName(name);
			offizier.save();
			
			t.set_var("choff.message", "Der Name wurde in "+Common._plaintitle(name)+" ge&auml;ndert");
		}
		else {
			t.set_var("choff.message", "<span style=\"color:red\">Sie m&uuml;ssen einen Namen angeben</span>");
		}
	
		redirect();	
	}
	
	@Override
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		
		String[] dest = offizier.getDest();
		
		t.set_var(	"offizier.id",			offizier.getID(),
					"offizier.name",		Common._plaintitle(offizier.getName()),
					"offizier.picture",		offizier.getPicture(),
					"offizier.ability.ing",	offizier.getAbility( Offizier.Ability.ING ),
					"offizier.ability.waf",	offizier.getAbility( Offizier.Ability.WAF ),
					"offizier.ability.nav",	offizier.getAbility( Offizier.Ability.NAV ),
					"offizier.ability.sec",	offizier.getAbility( Offizier.Ability.SEC ),
					"offizier.ability.com",	offizier.getAbility( Offizier.Ability.COM ),
					"offizier.special",		offizier.getSpecial(),
					"base.id",				(dest[0].equals("b") || dest[0].equals("t") ? dest[1] : 0),
					"ship.id",				(dest[0].equals("s") ? dest[1] : 0) );
	}

}

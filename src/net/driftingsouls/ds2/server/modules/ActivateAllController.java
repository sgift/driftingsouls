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

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.bases.Core;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * (De)aktivierung aller Gebaeude auf einer Basis
 * @author Christopher Jung
 *
 */
public class ActivateAllController extends DSGenerator {
	private Base base = null;
	
	public ActivateAllController(Context context) {
		super(context);
		
		setTemplate("activateall.html");
		
		parameterNumber("col");
		parameterNumber("deaconly");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		Database db = getDatabase();
		User user = getUser();
		
		int col = getInteger("col");
		
		//Existiert die Basis?
		SQLResultRow base = db.first("SELECT * FROM bases WHERE owner='",user.getID(),"' AND id='",col,"'");
		if( base.isEmpty() ) {
			addError("Die angegebene Kolonie existiert nicht");
			return false;
		}
				
		this.base = new Base(base);

		return true;	
	}
	
	@Override
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		t.set_var("base.id", base.getID());
		
		int deakOnly = getInteger("deaconly");
		if( deakOnly != 0 ) {
			t.set_block("_ACTIVATEALL", "deak.listitem", "deak.list");	
		}
		else {
			t.set_block("_ACTIVATEALL", "activate.listitem", "activate.list");	
		}
		
		/*
			Alle Gebaeude deaktivieren
		*/
		String query = "";
		Core core = null;
		
		if( base.getCore() != 0 ) {
			core = Core.getCore(db, base.getCore());
			base.put("arbeiter", base.getArbeiter() - core.getArbeiter());
			query = "coreactive=0,";
			if( deakOnly != 0 ) {
				t.set_var("deak.name", Common._plaintitle(core.getName()) );
				t.parse("deak.list", "deak.listitem", true);
			}
		} 

		for( int i=0; i < base.getWidth()*base.getHeight(); i++ ) {
			if( (base.getBebauung()[i] != 0) && (base.getActive()[i] == 1 ) ) {
				Building building = Building.getBuilding(db, base.getBebauung()[i]);
				
				if( building.isDeakAble() ) {
					base.getActive()[i] = 0;
					base.put("arbeiter", base.getArbeiter() - building.getArbeiter());
					
					if( deakOnly != 0 ) {
						t.set_var("deak.name", Common._plaintitle(building.getName()) );
						t.parse("deak.list", "deak.listitem", true);
					}
				} 
			}
		}

		String ondb = Common.implode( "|", base.getActive() );
		query += "active='"+ondb+"'";

		db.update("UPDATE bases SET ",query,",arbeiter='"+this.base.getArbeiter()+"' WHERE id='"+base.getID()+"'");
		
		/*
			Falls gewuenscht, nun alle Gebaeude nacheinander aktivieren
		*/
		if( deakOnly == 0 ) {
			query = "";

			if( base.getCore() != 0 ) {
				if( base.getBewohner() >= base.getArbeiter()+core.getArbeiter() ) {
					base.put("arbeiter", base.getArbeiter() + core.getArbeiter());
					query = "coreactive=1,";
					t.set_var(	"activate.name",	Common._plaintitle(core.getName()),
								"activate.success",	1 );
				} 
				else {
					t.set_var(	"activate.name",	Common._plaintitle(core.getName()),
								"activate.success",	0 );
				}
				t.parse("activate.list", "activate.listitem", true);
			}

			for( int i=0; i < base.getWidth()*base.getHeight(); i++ ) {
				if( base.getBebauung()[i] != 0 ) {
					Building building = Building.getBuilding(db, base.getBebauung()[i]);
					
					if( building.isDeakAble() && (base.getBewohner() >= base.getArbeiter()+building.getArbeiter()) ) {
						this.base.getActive()[i] = 1;
						this.base.put("arbeiter", base.getArbeiter() + building.getArbeiter());
						
						t.set_var(	"activate.name",	Common._plaintitle(building.getName()),
									"activate.success",	1 );
					} 
					else if( building.isDeakAble() ) {
						t.set_var(	"activate.name",	Common._plaintitle(building.getName()),
									"activate.success",	0 );
					}				
					t.parse("activate.list", "activate.listitem", true);
				}
			}
			ondb = Common.implode("|",base.getActive());
			query += "active='"+ondb+"'";
			db.update("UPDATE bases SET ",query,",arbeiter='",base.getArbeiter(),"' WHERE id='",base.getID(),"'");
		} 
	}

}

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
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * (De)aktivierung aller Gebaeude auf einer Basis
 * @author Christopher Jung
 * 
 * @urlparam Integer col Die ID der Basis
 * @urlparam Integer deaconly != 0, falls die Gebaeude/Cores nur deaktiviert, nicht aber aktiviert werden sollen
 *
 */
public class ActivateAllController extends TemplateGenerator {
	private Base base = null;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public ActivateAllController(Context context) {
		super(context);
		
		setTemplate("activateall.html");
		
		parameterNumber("col");
		parameterNumber("deaconly");
		
		setPageTitle("Alles Aktivieren");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		User user = (User)getUser();
		
		int col = getInteger("col");
		
		//Existiert die Basis?
		Base base = (Base)getDB().get(Base.class, col);
		if( (base == null) || (base.getOwner() != user) ) {
			addError("Die angegebene Kolonie existiert nicht");
			return false;
		}
				
		this.base = base;

		return true;	
	}
	
	/**
	 * (de)aktiviert die Gebaeude/Cores
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		
		t.setVar("base.id", base.getId());
		
		int deakOnly = getInteger("deaconly");
		if( deakOnly != 0 ) {
			t.setBlock("_ACTIVATEALL", "deak.listitem", "deak.list");	
		}
		else {
			t.setBlock("_ACTIVATEALL", "activate.listitem", "activate.list");	
		}
		
		/*
			Alle Gebaeude deaktivieren
		*/
		Core core = Core.getCore(base.getCore());
		
		if( (base.getCore() != 0) && base.isCoreActive() ) {
			base.setArbeiter(base.getArbeiter() - core.getArbeiter());
			base.setCoreActive(false);

			if( deakOnly != 0 ) {
				t.setVar("deak.name", Common._plaintitle(core.getName()) );
				t.parse("deak.list", "deak.listitem", true);
			}
		}

		Integer[] ondb = base.getActive();
		for( int i=0; i < base.getWidth()*base.getHeight(); i++ ) {
			if( (base.getBebauung()[i] != 0) && (ondb[i] == 1 ) ) {
				Building building = Building.getBuilding(base.getBebauung()[i]);
				
				if( building.isDeakAble() ) {
					ondb[i] = 0;
					base.setArbeiter(base.getArbeiter() - building.getArbeiter());
					
					if( deakOnly != 0 ) {
						t.setVar("deak.name", Common._plaintitle(building.getName()) );
						t.parse("deak.list", "deak.listitem", true);
					}
				} 
			}
		}

		base.setActive(ondb);
		
		/*
			Falls gewuenscht, nun alle Gebaeude nacheinander aktivieren
		*/
		if( deakOnly == 0 ) {
			if( base.getCore() != 0 ) {
				if( base.getBewohner() >= base.getArbeiter()+core.getArbeiter() ) {
					base.setArbeiter(base.getArbeiter() + core.getArbeiter());
					base.setCoreActive(true);
					
					t.setVar(	"activate.name",	Common._plaintitle(core.getName()),
								"activate.success",	1 );
				} 
				else {
					t.setVar(	"activate.name",	Common._plaintitle(core.getName()),
								"activate.success",	0 );
				}
				t.parse("activate.list", "activate.listitem", true);
			}

			for( int i=0; i < base.getWidth()*base.getHeight(); i++ ) {
				if( base.getBebauung()[i] != 0 ) {
					Building building = Building.getBuilding(base.getBebauung()[i]);
					
					if( building.isDeakAble() && (base.getBewohner() >= base.getArbeiter()+building.getArbeiter()) ) {
						ondb[i] = 1;
						this.base.setArbeiter(base.getArbeiter() + building.getArbeiter());
						
						t.setVar(	"activate.name",	Common._plaintitle(building.getName()),
									"activate.success",	1 );
					} 
					else if( building.isDeakAble() ) {
						t.setVar(	"activate.name",	Common._plaintitle(building.getName()),
									"activate.success",	0 );
					}				
					t.parse("activate.list", "activate.listitem", true);
				}
			}
			
			base.setActive(ondb);
		} 
	}

}

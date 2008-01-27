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
import net.driftingsouls.ds2.server.bases.Core;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Verwaltung der Core eines Asteroiden
 * @author Christopher Jung
 * 
 * @urlparam Integer col Die ID des Asteroiden, dessen Core verwaltet werden soll
 */
public class CoreController extends DSGenerator {
	private Base base = null;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public CoreController(Context context) {
		super(context);
		
		setTemplate("core.html");
		
		parameterNumber("col");		
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		Database db = getDatabase();
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		
		int col = getInteger("col");
		
		SQLResultRow base = db.first("SELECT * FROM bases WHERE owner=",user.getID()," AND id=",col);
		if( base.isEmpty() ) {
			addError( "Die angegebene Kolonie existiert nicht", Common.buildUrl(getContext(), "default", "module", "basen") );
			
			return false;
		}
		
		t.setVar( "base.id", base.getInt("id") );
							
		this.base = new Base(base);
		
		return true;	
	}

	/**
	 * Baut eine neue Core auf dem Asteroiden, sofern noch keine Core auf dem 
	 * Asteroiden vorhanden ist
	 * @urlparam Integer build Die ID der zu bauenden Core
	 *
	 */
	public void buildAction() {
		Database db = getDatabase();
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("build");
		int build = getInteger("build");
		
		if( base.getCore() > 0 ) {
			addError("Sie k&ouml;nnen nur eine Core pro Asteroid bauen", Common.buildUrl(getContext(), "default", "module", "base", "col", base.getID()));
			setTemplate("");
			
			return;
		}
		
		Core core = Core.getCore(db, build);
		if( core == null ) {
			addError("Der angegebene Core-Typ existiert nicht", Common.buildUrl(getContext(), "default", "module", "base", "col", base.getID()));
			setTemplate("");
			
			return;
		}
		
		if( !user.hasResearched(core.getTechRequired()) ) {
			addError("Sie haben nicht alle ben&ouml;tigten Forschungen", Common.buildUrl(getContext(), "default", "module", "base", "col", base.getID()));
			setTemplate("");
			
			return;
		}
	
		if( core.getAstiType() != base.getKlasse() ) {
			addError("Diese Core passt nicht in diesen Asteroiden rein", Common.buildUrl(getContext(), "default", "module", "base", "col", base.getID()));
			setTemplate("");
			
			return;
		}
		
		Cargo costs = core.getBuildCosts();
		
		t.setVar("core.build", 1);

		//Benoetigte Res ueberpruefen
		Cargo cargo = base.getCargo();
	
		t.setBlock("_CORE", "build.res.listitem", "build.res.list");
	
		boolean ok = true;
		ResourceList reslist = costs.compare( cargo, false );
		for( ResourceEntry res : reslist ) {
			if( res.getDiff() > 0 ) {
				ok = false;	
			}
			t.setVar(	"res.image",			res.getImage(),
						"res.cargo.available",	res.getCargo2(),
						"res.cargo.needed",		res.getCargo1(),
						"res.missing",			res.getDiff() > 0 ? res.getDiff() : 0 );

			t.parse("build.res.list", "build.res.listitem", true);
		}

		// Genuegend Res vorhanden -> Bauen
		if( ok ) {
			base.setCore(build);
		
			base.setCoreActive(false);
		
			if( core.getArbeiter()+base.getArbeiter() > base.getBewohner() ) {
				t.setVar( "build.message", "<span style=\"color:#ff0000\">Nicht gen&uuml;gend Arbeiter</span>" );
			} 
			else {
				base.setCoreActive(true);
				t.setVar( "build.message", "<span style=\"color:#00ff00\">aktiviert</span>" );
			}
			cargo.substractCargo( costs );
	
			base.setCargo(cargo);
			db.tBegin();
			db.update("UPDATE bases " +
					"SET core=",build,",cargo='",cargo.save(),"',coreactive=0 " +
					"WHERE id=",base.getID()," AND core='0' AND cargo='",cargo.save(true),"'");
			
			if( base.isCoreActive() ) {
				db.update("UPDATE bases " +
						"SET coreactive='1',arbeiter=arbeiter+'",core.getArbeiter(),"',bewohner=bewohner+'",core.getBewohner(),"' " +
						"WHERE id=",base.getID()," AND arbeiter='",base.getArbeiter(),"' AND coreactive='0' AND bewohner='",base.getBewohner(),"'");
				
				base.setArbeiter(base.getArbeiter() + core.getArbeiter());
				base.setBewohner(base.getBewohner() + core.getBewohner());
			}
			if( !db.tCommit() ) {
				addError("Beim bauen der Core ist ein Fehler aufgetreten. Bitte versuchen sie es sp&auml;ter erneut");
			}
		}
		
		redirect();
	}
	
	/**
	 * Deaktiviert die Core auf dem Asteroiden, sofern sie noch nicht deaktiviert ist
	 */
	public void deactivateAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		if( !base.isCoreActive() ) {
			redirect();
			return;
		}
		
		Core core = Core.getCore(db, base.getCore());

		db.update("UPDATE bases " +
				"SET coreactive='0',arbeiter=arbeiter-'",core.getArbeiter(),"' " +
				"WHERE id=",base.getID()," AND arbeiter='",base.getArbeiter(),"' AND bewohner='",base.getBewohner(),"' AND coreactive='1'");
		
		if( db.affectedRows() == 0 ) {
			t.setVar("core.message", "<span class=\"error\">Deaktivierung fehlgeschlagen. Bitte versuchen sie es sp&auml;ter erneut</span>");
			redirect();
			return;
		}
		
		base.setArbeiter(base.getArbeiter() - core.getArbeiter());
		base.setCoreActive(false);
		
		t.setVar( "core.message", "<span class=\"error\">Core deaktiviert</span>" );
		
		redirect();
	}
	
	/**
	 * Aktiviert die Core auf dem Asteroiden, sofern sie noch nicht aktiviert ist und 
	 * die Anzahl der freien Arbeiter dazu ausreicht.
	 */
	public void activateAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		if( base.isCoreActive() ) {
			redirect();
			return;
		}
		
		Core core = Core.getCore(db, base.getCore());
		if( core.getArbeiter()+base.getArbeiter() > base.getBewohner() ) {
			t.setVar( "core.message", "<span style=\"color:#ff0000\">Nicht gen&uuml;gend Arbeiter</span>" );
		} 
		else {
			db.update("UPDATE bases " +
					"SET coreactive='1',arbeiter=arbeiter+'",core.getArbeiter(),"' " +
					"WHERE id=",base.getID()," AND arbeiter='",base.getArbeiter(),"' AND coreactive='0' AND bewohner='",base.getBewohner(),"'");
			
			if( db.affectedRows() == 0 ) {
				t.setVar("core.message", "<span class=\"error\">Aktivierung fehlgeschlagen. Bitte versuchen sie es sp&auml;ter erneut</span>");
				redirect();
				return;
			}
			
			base.setArbeiter(base.getArbeiter() + core.getArbeiter());
			base.setCoreActive(true);
		
			t.setVar( "core.message", "<span class=\"ok\">Core aktiviert</span>" );
		}
		
		redirect();
	}
	
	private void showCore() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		Core core = Core.getCore(db, base.getCore());
		
		t.setVar(	"core.astitype",	core.getAstiType(),
					"core.name",		Common._plaintitle(core.getName()),
					"core.activated",	base.isCoreActive(),
					"core.ever",		core.getEVerbrauch(),
					"core.eprodu",		core.getEProduktion() );

		Cargo produces = core.getProduces();
		Cargo consumes = core.getConsumes();

		t.setBlock("_CORE", "res.listitem", "consumes.res.list");
		
		ResourceList reslist = consumes.getResourceList();
		for( ResourceEntry res : reslist ) {
			t.setVar(	"res.image",	res.getImage(),
						"res.cargo",	res.getCargo1() );
								
			t.parse("consumes.res.list", "res.listitem", true);
		}

		reslist = produces.getResourceList();
		for( ResourceEntry res : reslist ) {
			t.setVar(	"res.image",	res.getImage(),
						"res.cargo",	res.getCargo1() );
								
			t.parse("produces.res.list", "res.listitem", true);
		}
	}
	
	private void showCoreBuildList() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
		// Keine Core vorhanden
		Cargo cargo = base.getCargo();

		t.setBlock("_CORE", "cores.listitem", "cores.list");

		SQLQuery coreID = db.query("SELECT id FROM cores WHERE astitype='",base.getKlasse(),"'");
		while( coreID.next() ) {
			Core core = Core.getCore(db, coreID.getInt("id"));
			
			if( !user.hasResearched(core.getTechRequired()) ) {
				continue;
			}

			Cargo costs = core.getBuildCosts();
			Cargo produces = core.getProduces();
			Cargo consumes = core.getConsumes();

			boolean buildable = true;
			ResourceList reslist = costs.compare(cargo, false);
			for( ResourceEntry res : reslist ) {
				if( res.getDiff() > 0 ) {
					buildable = false;
				}
			}
		
			t.setVar(	"core.isbuildable",		buildable,
						"core.ever",			core.getEVerbrauch(), 
						"core.name",			Common._plaintitle(core.getName()),
						"core.id",				core.getID(),
						"core.eprodu",			core.getEProduktion(),
						"core.arbeiter",		core.getArbeiter(),
						"core.bewohner",		core.getBewohner(),
						"costs.res.list",		"",
						"consumes.res.list",	"",
						"produces.res.list",	"" );

			t.setBlock("cores.listitem", "costs.res.listitem", "costs.res.list");

			for( ResourceEntry res : reslist ) { 
				t.setVar(	"res.red",		res.getDiff() > 0,
							"res.image",	res.getImage(),
							"res.cargo",	res.getCargo1() );
  				
				t.parse("costs.res.list", "costs.res.listitem", true);
			}
		
			reslist = consumes.getResourceList();
			for( ResourceEntry res : reslist ) { 
				t.setVar(	"res.image",	res.getImage(),
							"res.cargo",	res.getCargo1() );
  				
  				t.parse("consumes.res.list", "costs.res.listitem", true);
			}
		
			reslist = produces.getResourceList();
			for( ResourceEntry res : reslist ) { 
				t.setVar(	"res.image",	res.getImage(),
							"res.cargo",	res.getCargo1() );
  				
  				t.parse("produces.res.list", "costs.res.listitem", true);
			}
			t.parse("cores.list", "cores.listitem", true);
		}
		coreID.free();
	}
	
	/**
	 * Zeigt entweder die Liste aller auf dem Asteroiden im Moment 
	 * baubaren Cores an (wenn noch keine Core gebaut wurde) oder
	 * die Daten zur aktuellen Core
	 */
	@Override
	public void defaultAction() {	
		TemplateEngine t = getTemplateEngine();
		t.setVar( "base.core", base.getCore() );
		
		if( base.getCore() > 0 ) {
			showCore();
		} 
		else {
			showCoreBuildList();
		}
	}
}

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

import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.Items;
import net.driftingsouls.ds2.server.config.ResourceConfig;
import net.driftingsouls.ds2.server.entities.Handel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Zeigt aktive Handelsangebote an und ermoeglicht das Erstellen eigener Handelsangebote
 * @author Christopher Jung
 *
 */
public class HandelController extends TemplateGenerator {

	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public HandelController(Context context) {
		super(context);
		
		setTemplate("handel.html");
		
		setPageTitle("Handel");
		addPageMenuEntry("Angebote", Common.buildUrl("default"));
		addPageMenuEntry("neues Angebot", Common.buildUrl("add"));
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		return true;
	}

	/**
	 * Speichert ein neues Handelsangebot in der Datenbank
	 * @urlparam String comm Die Beschreibung
	 * @urlparam Integer ($warenid|"i"+$itemid)+"need" Benoetigte Waren
	 * @urlparam Integer ($warenid|"i"+$itemid)+"have" Angebotene Waren
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void enterAction() {
		org.hibernate.Session db = getDB();
		
		parameterString("comm");
		String comm = getString("comm");
		
		boolean needeverything = false;
		boolean haveeverything = false;
		Cargo need = new Cargo();
		Cargo have = new Cargo();

	
		// Egal - "-1" (Spezialfall)
		parameterNumber("-1need");			
		long needcount = getInteger("-1need");
		long havecount = 0;
		
		if( needcount <= 0 ) {
			parameterNumber("-1have");
			havecount = getInteger("-1have");
			
			if( havecount > 0 ) {
				haveeverything = true; 
			}
		}
		else {
			needeverything = true;
		}
		
		
		ResourceList reslist = Resources.RESOURCE_LIST.getResourceList();
		for( ResourceEntry res : reslist ) {
			String name = "";
			
			if( res.getId().isItem() ) {
				if( !Items.get().item(res.getId().getItemID()).getHandel() ) {
					continue;
				}
				name = "i"+res.getId().getItemID();
			}
			else {
				if( ResourceConfig.getResourceHidden(res.getId().getID()) ) {
					continue;
				}
				name = Integer.toString(res.getId().getID());
			}
					
			parameterNumber(name+"need");			
			needcount = getInteger(name+"need");
			havecount = 0;
			
			if( needcount <= 0 ) {
				parameterNumber(name+"have");
				havecount = getInteger(name+"have");
			}
						
			if( needcount > 0 ) {
				need.addResource(res.getId(), needcount);
			}
			if( havecount > 0 ) {
				have.addResource(res.getId(), havecount);
			}
		}
		
		Handel entry = new Handel((User)getUser());
		entry.setKommentar(comm);
		
		if( !needeverything ) {
			entry.setSucht(need.save());	
		}
		
		if( !haveeverything ) {
			entry.setBietet(have.save());	
		}

		db.persist(entry);

		redirect();
	}
	
	/**
	 * Zeigt die Seite zur Eingabe eines Handelsangebots an 
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void addAction() {
		TemplateEngine t = getTemplateEngine();
		
		t.setVar("handel.add", 1);
		
		t.setBlock("_HANDEL", "addresources.listitem", "addresources.list");
		
		ResourceList reslist = Resources.RESOURCE_LIST.getResourceList();
		for( ResourceEntry res : reslist ) {
			if( res.getId().isItem() ) {
				if( !Items.get().item(res.getId().getItemID()).getHandel() ) {
					continue;
				}
			}
			else {
				if( ResourceConfig.getResourceHidden(res.getId().getID()) ) {
					continue;
				}
			}
			
			t.setVar(	"res.id",		(res.getId().isItem() ? "i"+res.getId().getItemID() : res.getId().getID()),
						"res.name",		res.getName(),
						"res.image",	res.getImage() );
			
			t.parse("addresources.list", "addresources.listitem", true);
		}
	}

	/**
	 * Loescht ein Handelsangebot
	 * @urlparam Integer del Die ID des zu loeschenden Handelsangebots
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void deleteAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		
		parameterNumber("del");
		int del = getInteger("del");
		
		Handel entry = (Handel)db.get(Handel.class, del);
		if( (entry != null) && (entry.getWho().equals(user) || (user.getAccessLevel() >= 20) || user.hasFlag(User.FLAG_MODERATOR_HANDEL)) ) {
			db.delete(entry);
			t.setVar("handel.message", "Angebot gel&ouml;scht");
		} 
		else {
			addError("Sie haben keine Berechtigung das Angebot zu l&ouml;schen");
		}
		
		redirect();
	}
	
	/**
	 * Zeigt die vorhandenen Handelsangebote an
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {		
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		t.setVar("handel.view", 1);
		
		int count = 0;
		
		t.setBlock("_HANDEL", "angebote.listitem", "angebote.list");
		t.setBlock("angebote.listitem", "angebot.want.listitem", "angebot.want.list");
		t.setBlock("angebote.listitem", "angebot.need.listitem", "angebot.need.list");
		
		List entryList = db.createQuery("from Handel " +
				"where who.vaccount=0 or who.wait4vac!=0 order by time desc")
			.list();
		for( Iterator iter=entryList.iterator(); iter.hasNext(); ) {
			Handel entry = (Handel)iter.next();

			t.setVar(	"angebot.want.list",	"",
						"angebot.need.list",	"" );
			
			for( int i = 0; i <= 1; i++ ) {
				String line = (i == 1 ? entry.getBietet() : entry.getSucht());
				
				if( !line.equals("-1") ) {
					Cargo cargo = new Cargo( Cargo.Type.STRING, line );
					cargo.setOption( Cargo.Option.SHOWMASS, false );
					cargo.setOption( Cargo.Option.LINKCLASS, "handelwaren");
					
					ResourceList reslist = cargo.getResourceList();
					if( i == 0 ) {
						Resources.echoResList( t, reslist, "angebot.want.list");
					}
					else {
						Resources.echoResList( t, reslist, "angebot.need.list");
					}
				}
				else {
					t.setVar(	"res.cargo",	1,
								"res.id",		-1,
								"res.image",	Configuration.getSetting("URL")+"data/interface/handel/open.gif");
					if( i == 0 ) {
						t.parse("angebot.want.list", "angebot.want.listitem", true);
					}
					else {
						t.parse("angebot.need.list", "angebot.need.listitem", true);
					}
				}
			}
			
			t.setVar(	"angebot.id",			entry.getId(),
						"angebot.owner",		entry.getWho().getId(),
						"angebot.owner.name",	Common._title(entry.getWho().getName()),
						"angebot.date",			Common.date("d.m.Y H:i:s",entry.getTime()),
						"angebot.description",	Common._text(entry.getKommentar()),
						"angebot.description.overflow",	Common._text(entry.getKommentar()).length() > 220,
						"angebot.newline",		(count % 3 == 0),
						"angebot.endline",		(count % 3 == 0) && (count > 0),
						"angebot.showdelete",	entry.getWho().equals(user) || (user.getAccessLevel() >= 20) || user.hasFlag(User.FLAG_MODERATOR_HANDEL) );

			count++;

			t.parse("angebote.list","angebote.listitem", true);
		}
		
		t.setBlock("_HANDEL", "emptyangebote.listitem", "emptyangebote.list");
		while( count % 3 != 0 ) {
			t.parse("emptyangebote.list", "emptyangebote.listitem", true);
			count++;
		}	
	}
}

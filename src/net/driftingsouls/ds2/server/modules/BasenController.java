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

import java.util.Map;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseStatus;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import org.apache.commons.lang.ArrayUtils;

/**
 * Die Basenliste
 * @author Christopher Jung
 *
 */
public class BasenController extends DSGenerator {
	@SuppressWarnings("unchecked")
	private static final Map<String,String> ordmapper = ArrayUtils.toMap( new String[][]
		{	{"id", "id"}, 
			{"name", "name"},
			{"type", "type"}, 
			{"sys", "system,x,y"},
			{"bew", "bewohner"},
			{"e", "e"} } );
	
	public BasenController(Context context) {
		super(context);
		
		setTemplate("basen.html");
		
		parameterNumber("l");	
		parameterString("ord");
		parameterNumber("order");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		return true;
	}
	
	@Override
	public void defaultAction() {
		Database db = getDatabase();
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		
		String ord = user.getUserValue("TBLORDER/basen/order");
		int order = Integer.parseInt(user.getUserValue("TBLORDER/basen/order_mode"));
		int l = Integer.parseInt(user.getUserValue("TBLORDER/basen/showcargo"));
		if( !getString("ord").equals("") && !ord.equals(getString("ord")) ) {
			ord = getString("ord");
			user.setUserValue("TBLORDER/basen/order", ord );
		}

		if( (getInteger("order") != 0) && (order != getInteger("order")) ) {
			order = getInteger("order");
			user.setUserValue("TBLORDER/basen/order_mode", Integer.toString(order));
		}
		
		if( (getInteger("l") != 0) && (l != getInteger("l")) ) {
			l = getInteger("l");
			user.setUserValue("TBLORDER/basen/showcargo", Integer.toString(l));
		}
		
		t.set_var(	"global.l",		l,
					"global.order",	ord,
					"global.omode", order );
	
		
		String ow = "";
		String om = "";
		if( ordmapper.containsKey(ord) ) {
			ow = ordmapper.get(ord);
		}
		else {
			ow = "id";	
		}
			
		if( order == 1 ) {
			om = "DESC";
		}
		else {
			om = "ASC";
		}
		
		Cargo usercargo = new Cargo( Cargo.Type.STRING, user.getCargo() );
		
		t.set_block("_BASEN", "bases.listitem", "bases.list");
		t.set_block("bases.listitem", "bases.mangel.listitem", "bases.mangel.list");
		t.set_block("bases.listitem", "bases.cargo.listitem", "bases.cargo.list");
		
		SQLQuery base = db.query("SELECT * FROM bases WHERE owner=",user.getID()," ORDER BY ",ow," ",om);
		while( base.next() ) {
			BaseStatus basedata = Base.getStatus(getContext(),base.getRow());
			
			t.set_var( "base.id"		, base.get("id"),
					"base.klasse"	, base.get("klasse"),
					"base.name"		, Common._plaintitle(base.getString("name")),
					"base.system"	, base.get("system"),
					"base.x"		, base.get("x"),
					"base.y"		, base.get("y"),
					"base.bewohner"	, base.get("bewohner"),
					"base.e"		, base.get("e"),
					"base.e.diff"	, basedata.getE(),
					"bases.mangel.list"	, "",
					"bases.cargo.list"	, "" );
			
			/*
				Mangel + Runden anzeigen
			*/
	
			Cargo cargo = new Cargo( Cargo.Type.STRING, base.getString("cargo") );
			cargo.addResource( Resources.NAHRUNG, usercargo.getResourceCount( Resources.NAHRUNG ) );
	
			ResourceList reslist = basedata.getStatus().getResourceList();
			for( ResourceEntry res : reslist ) {
				if( res.getCount1() < 0 ) {
					long rounds = -cargo.getResourceCount(res.getId())/res.getCount1();
					t.set_var(	"mangel.rounds",	rounds,
								"mangel.image",		res.getImage(),
								"mangel.plainname",	res.getPlainName() );
					
					t.parse("bases.mangel.list", "bases.mangel.listitem", true);
				}
			}
			
			cargo.substractResource( Resources.NAHRUNG, usercargo.getResourceCount( Resources.NAHRUNG ) );
			
			/*
				Cargo anzeigen
			*/
			
			if( l == 1 ) {
				t.set_var("bases.cargo.empty", Common.ln(base.getLong("maxcargo")-cargo.getMass()));
				
				reslist = cargo.getResourceList();
				Resources.echoResList(t, reslist, "bases.cargo.list");
			}
			
			
			/*
				Links auf die einzelnen Gebaeude anzeigen
			*/

			StringBuilder shortcuts = new StringBuilder(10);

			for( Integer bid : basedata.getBuildingLocations().keySet() ) {
				Building building = Building.getBuilding(db, bid);
		
				shortcuts.append(building.echoShortcut( getContext(), base.getInt("id"), basedata.getBuildingLocations().get(bid), bid ));
				shortcuts.append(" ");
			}
									
			t.set_var("base.shortcuts", shortcuts);
									
			t.parse("bases.list", "bases.listitem", true);
		}
		base.free();
	}

}

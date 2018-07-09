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
package net.driftingsouls.ds2.server.cargo;

import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Liste von gaengigen Resourcen sowie einigen Hilfsfunktionen fuer Resourcen-IDs.
 * @author Christopher Jung
 *
 */
public class Resources {
	/**
	 * Die Resource Nahrung.
	 */
	public static final ResourceID NAHRUNG = new ItemID(16);
	/**
	 * Die Resource Deuterium.
	 */
	public static final ResourceID DEUTERIUM = new ItemID(17);
	/**
	 * Die Resource Kunststoffe.
	 */
	public static final ResourceID KUNSTSTOFFE = new ItemID(18);
	/**
	 * Die Resource Titan.
	 */
	public static final ResourceID TITAN = new ItemID(19);
	/**
	 * Die Resource Uran.
	 */
	public static final ResourceID URAN = new ItemID(20);
	/**
	 * Die Resource Antimaterie.
	 */
	public static final ResourceID ANTIMATERIE = new ItemID(21);
	/**
	 * Die Resource Adamatium.
	 */
	public static final ResourceID ADAMATIUM = new ItemID(22);
	/**
	 * Die Resource Platin.
	 */
	public static final ResourceID PLATIN = new ItemID(23);
	/**
	 * Die Resource Silizium.
	 */
	public static final ResourceID SILIZIUM = new ItemID(24);
	/**
	 * Die Resource Xentronium.
	 */
	public static final ResourceID XENTRONIUM = new ItemID(25);
	/**
	 * Die Resource Erz.
	 */
	public static final ResourceID ERZ = new ItemID(26);
	/**
	 * Die Resource Isochips.
	 */
	public static final ResourceID ISOCHIPS = new ItemID(35);
	/**
	 * Die Resource Batterien.
	 */
	public static final ResourceID BATTERIEN = new ItemID(36);
	/**
	 * Die Resource Leere Batterien.
	 */
	public static final ResourceID LBATTERIEN = new ItemID(37);
	/**
	 * Die Resource Antarit.
	 */
	public static final ResourceID ANTARIT = new ItemID(27);
	/**
	 * Die Resource Shivanische Artefakte.
	 */
	public static final ResourceID SHIVARTE = new ItemID(38);
	/**
	 * Die Resource Artefakte der Uralten.
	 */
	public static final ResourceID ANCIENTARTE = new ItemID(39);
	/**
	 * Die Resource Boese Admins.
	 */
	public static final ResourceID BOESERADMIN = new ItemID(40);
	/**
	 * Die Resource RE.
	 */
	public static final ResourceID RE = new ItemID(6);
	
	private static volatile Cargo resourceList;
	
	/**
	 * Gibt einen Cargo zurueck, in dem jede Resource genau einmal vorkommt. 
	 * Items sind in der Form ohne Questbindung und mit unbegrenzter Nutzbarkeit vorhanden.
	 * @return Der Cargo
	 */
	public static Cargo getResourceList()
	{
		// double-checked locking idiom - seit Java 5 funktionsfaehig
		if( resourceList == null )
		{
			synchronized(Resources.class)
			{
				if( resourceList == null )
				{
					initResourceList();
				}
			}
		}
		
		return resourceList;
	}
	
	private static void initResourceList() {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		Cargo resList = new Cargo();
		
		List<Item> items = Common.cast(db.createQuery("from Item").list());
		
		for( Item item : items )
		{
			resList.addResource(new ItemID(item.getID()), 1);
		}
		
		resourceList = new UnmodifiableCargo(resList);
	}

	/**
	 * Wandelt einen String in eine Resourcen-ID um.
	 * Es werden sowohl normale Waren alsauch Items beruecksichtigt.
	 * 
	 * @param rid Der String
	 * @return die Resourcen-ID
	 */
	public static ResourceID fromString(String rid) {
		if( rid == null ) {
			return null;
		}
		if( rid.equals("") ) {
			return null;
		}
		return ItemID.fromString(rid);
	}

	public static String resourceListToBBCode(ResourceList reslist)
	{
		return resourceListToBBCode(reslist, "\n");
	}

	public static String resourceListToBBCode(ResourceList reslist, String separator)
	{
		return reslist.stream().map(res -> "[resource="+res.getId().toString()+"]"+res.getCount1()+"[/resource]").collect(Collectors.joining(separator));
	}
	
	/**
	 * Gibt die <code>ResourceList</code> via TemplateEngine aus. Ein Item des TemplateBlocks
	 * muss den Namen templateBlock+"item" haben.
	 * 
	 * @param t Das TemplateEngine
	 * @param reslist Die ResourceList
	 * @param templateblock Der Name des betreffenden TemplateBlocks
	 */
	public static void echoResList( TemplateEngine t, ResourceList reslist, String templateblock) {
		echoResList(t,reslist,templateblock,templateblock+"item");
	}
	
	/**
	 * Gibt die <code>ResourceList</code> via TemplateEngine aus.
	 * @param t Das TemplateEngine
	 * @param reslist Die ResourceList
	 * @param templateblock Der Name des betreffenden TemplateBlocks
	 * @param templateitem Der Name eines Items des TemplateBlocks
	 */
	public static void echoResList( TemplateEngine t, ResourceList reslist, String templateblock, String templateitem ) {
		t.setVar(templateblock,"");
		
		for( ResourceEntry res : reslist ) {
			t.setVar(	"res.image",		res.getImage(),
						"res.cargo",		res.getCargo1(),
						"res.cargo1",		res.getCargo1(),
						"res.cargo2",		res.getCargo2(),
						"res.name",			res.getName(),
						"res.plainname",	res.getPlainName() );
			
			t.parse(templateblock,templateitem,true);
		}
	}
}

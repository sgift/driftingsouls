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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.modules.admin.AdminMenuEntry;
import net.driftingsouls.ds2.server.modules.admin.AdminPlugin;

/**
 * Der Admin
 * @author Christopher Jung
 * 
 * @urlparam Integer act Die auszufuehrende Aktions-ID (ein Plugin wird ueber Seiten- und Aktions-ID identifiziert)
 * @urlparam String page Die anzuzeigende Seite (ID der Seite)
 * @urlparam Integer cleanpage Falls != 0 werden keine zusaetzlichen GUI-Elemente angezeigt
 * @urlparam String namedplugin Der Name des Plugins, welches angezeigt werden soll (Alternative zu Seiten- und Aktions-ID)
 */
public class AdminController extends DSGenerator {
	/**
	 * Der Name der Resource, welche die Liste der Admin-Plugins enthaelt (ein Klassenname pro Zeile)
	 */
	public static final String PLUGIN_LIST = "META-INF/driftingsouls/"+AdminController.class.getName();
	
	private static class MenuEntry {
		String name;
		List<MenuEntry> actions = new ArrayList<MenuEntry>();
		Class<? extends AdminPlugin> cls;
		
		MenuEntry( String name ) {
			this.name = name;
		}
	}
	
	Map<String,MenuEntry> menu = new LinkedHashMap<String,MenuEntry>();
	Set<String> validPlugins = new HashSet<String>();
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public AdminController(Context context) {
		super(context);
		
		parameterNumber("act");
		parameterString("page");
		parameterNumber("cleanpage");
		parameterString("namedplugin");	
	}
	
	private void addMenuEntry( Class<? extends AdminPlugin> cls, String menuentry, String submenuentry ) {
		if( !this.menu.containsKey(menuentry) ) {
			this.menu.put(menuentry, new MenuEntry(menuentry));
		}
		
		MenuEntry entry = new MenuEntry(submenuentry);
		entry.cls = cls;
		this.menu.get(menuentry).actions.add(entry);
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		User user = this.getUser();
		
		if( user.getAccessLevel() < 30 ) {
			addError("Sie sind nicht berechtigt diese Seite aufzurufen");
			return false;
		}
		
		try {
			Enumeration<URL> e = getClass().getClassLoader().getResources(PLUGIN_LIST);
			while( e.hasMoreElements() ) {
				URL elem = e.nextElement();
				BufferedReader reader = new BufferedReader(new InputStreamReader(elem.openStream()));
				String line = null;
				while( (line = reader.readLine()) != null ) {
					if( validPlugins.contains(line) ) {
						continue;
					}
					Class<? extends AdminPlugin> aClass = Class.forName(line).asSubclass(AdminPlugin.class);
					validPlugins.add(line);
					
					AdminMenuEntry adminMenuEntry = aClass.getAnnotation(AdminMenuEntry.class);
					
					addMenuEntry(aClass, adminMenuEntry.category(), adminMenuEntry.name());
				}
			}	
		}
		catch( IOException e ) {
			addError("Konnte Admin-Plugins nicht verarbeiten: "+e);
		}
		catch( ClassNotFoundException e ) {
			addError("Konnte Admin-Plugins nicht verarbeiten: "+e);
		}
		
		if( this.getInteger("cleanpage") > 0 ) {
			this.setDisableDebugOutput(true);	
		}
		
		return true;
	}

	/**
	 * Zeigt die Gui an und fuehrt ein Admin-Plugin (sofern ausgewaehlt) aus
	 */
	@Override
	public void defaultAction() {	
		int cleanpage = getInteger("cleanpage");
		int act = getInteger("act");
		String page = getString("page");
		String sess = getString("sess");
		String namedplugin = getString("namedplugin");
		
		StringBuffer echo = getContext().getResponse().getContent();
		if( cleanpage == 0 ) {
			echo.append("<div align=\"center\">\n");
			echo.append(Common.tableBegin( 700, "center" ));
			echo.append("<table class=\"noBorderX\" width=\"700\"><tr><td align=\"center\" class=\"noBorderX\">\n");
			boolean first = true;
			for( MenuEntry entry : this.menu.values() ) {
				if( first ) {
					echo.append("<a class=\"forschinfo\" href=\"./main.php?module=admin&sess="+sess+"&page="+entry.name+"\">"+entry.name+"</a>\n");
					first = false;
				}
				else
					echo.append(" | <a class=\"forschinfo\" href=\"./main.php?module=admin&sess="+sess+"&page="+entry.name+"\">"+entry.name+"</a>\n");
			}
			echo.append("</td></tr></table>\n");
			echo.append(Common.tableEnd());
			echo.append("</div><br />\n");
		}
		
		if( page.length() > 0 || namedplugin.length() > 0 ) {
			if( cleanpage == 0 ) {
				echo.append("<table class=\"noBorder\"><tr><td class=\"noBorder\" valign=\"top\">\n");
		
				echo.append(Common.tableBegin( 220, "left" ));
				echo.append("<table class=\"noBorderX\" width=\"100%\">\n");
				echo.append("<tr><td align=\"center\" class=\"noBorderX\">Aktionen:</td></tr>\n");
				if( this.menu.containsKey(page) && (this.menu.get(page).actions.size() > 0) ) {
					List<MenuEntry> actions = this.menu.get(page).actions;
					for( int i=0; i < actions.size(); i++ ) {
						echo.append("<tr><td align=\"left\" class=\"noBorderX\"><a class=\"forschinfo\" href=\"./main.php?module=admin&sess="+sess+"&page="+page+"&act="+(i+1)+"\">"+actions.get(i).name+"</a></td></tr>\n");
					}
				}
				echo.append("</table>\n");
				echo.append(Common.tableEnd());
		
				echo.append("</td><td class=\"noBorder\" valign=\"top\" width=\"40\">&nbsp;&nbsp;&nbsp;</td>\n");
				echo.append("<td class=\"noBorder\" valign=\"top\">\n");
			}
			if( act > 0 ) {
				if( this.menu.containsKey(page) && (this.menu.get(page).actions.size() > 0) ) {
					List<MenuEntry> actions = this.menu.get(page).actions;
					if( act <= actions.size() ) {
						Class<? extends AdminPlugin> cls = actions.get(act-1).cls;
						try {
							AdminPlugin plugin;
							plugin = cls.newInstance();
							plugin.output(this, page, act);
						}
						catch( InstantiationException e ) {
							addError("Fehler beim Aufruf des Admin-Plugins: "+e);
						}
						catch( IllegalAccessException e ) {
							addError("Fehler beim Aufruf des Admin-Plugins: "+e);
						}
					}
				}
			}
			else if( (namedplugin.length() > 0) && (validPlugins.contains(namedplugin)) ) {
				try {
					Class<? extends AdminPlugin> aClass = Class.forName(namedplugin).asSubclass(AdminPlugin.class);
					AdminPlugin plugin = aClass.newInstance();
					plugin.output(this, page, act);
				}
				catch( Exception e ) {
					addError("Fehler beim Aufruf des Admin-Plugins: "+e);
				}
			}

			if( cleanpage == 0 ) {
				echo.append("</td></tr></table>");
			}
		}
	}
}

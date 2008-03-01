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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
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
	private static ServiceLoader<AdminPlugin> pluginLoader = ServiceLoader.load(AdminPlugin.class);
	
	private static class MenuEntry implements Comparable<MenuEntry> {
		String name;
		List<MenuEntry> actions = new ArrayList<MenuEntry>();
		Class<? extends AdminPlugin> cls;
		
		MenuEntry( String name ) {
			this.name = name;
		}

		public int compareTo(MenuEntry o) {
			return name.compareTo(o.name);
		}
		
		@Override
		public boolean equals(Object object)
		{
			if(object == null)
			{
				return false;
			}
			
			if(object.getClass() != this.getClass())
			{
				return false;
			}
			
			MenuEntry other = (MenuEntry) object;
			return this.name.equals(other.name);
		}
	}
	
	TreeMap<String,MenuEntry> menu = new TreeMap<String,MenuEntry>();
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
		User user = (User)this.getUser();
		
		if( user.getAccessLevel() < 30 ) {
			addError("Sie sind nicht berechtigt diese Seite aufzurufen");
			return false;
		}
		
		for( AdminPlugin plugin : pluginLoader ) {
			Class<? extends AdminPlugin> cls = plugin.getClass();
			
			if( validPlugins.contains(cls.getName()) ) {
				continue;
			}
			validPlugins.add(cls.getName());
					
			AdminMenuEntry adminMenuEntry = cls.getAnnotation(AdminMenuEntry.class);
				
			addMenuEntry(cls, adminMenuEntry.category(), adminMenuEntry.name());
		}	
		
		if( this.getInteger("cleanpage") > 0 ) {
			this.setDisableDebugOutput(true);
			this.setDisablePageMenu(true);
		}
		
		return true;
	}
	
	/**
	 * Fuehrt ein Admin-Plugin aus
	 */
	@Action(ActionType.AJAX)
	public void ajaxAction() {
		int act = getInteger("act");
		String page = getString("page");
		String namedplugin = getString("namedplugin");
		
		if( page.length() > 0 || namedplugin.length() > 0 ) {
			if( act > 0 ) {
				callPlugin(page, act);
			}
			else if( (namedplugin.length() > 0) && (validPlugins.contains(namedplugin)) ) {
				callNamedPlugin(namedplugin);
			}
		}
	}

	/**
	 * Zeigt die Gui an und fuehrt ein Admin-Plugin (sofern ausgewaehlt) aus
	 */
	@Override
	@Action(ActionType.DEFAULT)
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
					echo.append("<a class=\"forschinfo\" href=\"./ds?module=admin&sess="+sess+"&page="+entry.name+"\">"+entry.name+"</a>\n");
					first = false;
				}
				else
					echo.append(" | <a class=\"forschinfo\" href=\"./ds?module=admin&sess="+sess+"&page="+entry.name+"\">"+entry.name+"</a>\n");
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
						echo.append("<tr><td align=\"left\" class=\"noBorderX\"><a class=\"forschinfo\" href=\"./ds?module=admin&sess="+sess+"&page="+page+"&act="+(i+1)+"\">"+actions.get(i).name+"</a></td></tr>\n");
					}
				}
				echo.append("</table>\n");
				echo.append(Common.tableEnd());
		
				echo.append("</td><td class=\"noBorder\" valign=\"top\" width=\"40\">&nbsp;&nbsp;&nbsp;</td>\n");
				echo.append("<td class=\"noBorder\" valign=\"top\">\n");
			}
			if( act > 0 ) {
				callPlugin(page, act);
			}
			else if( (namedplugin.length() > 0) && (validPlugins.contains(namedplugin)) ) {
				callNamedPlugin(namedplugin);
			}

			if( cleanpage == 0 ) {
				echo.append("</td></tr></table>");
			}
		}
	}

	private void callNamedPlugin(String namedplugin) {
		try {
			int act = 0;
			String page = "";
			
			Class<? extends AdminPlugin> aClass = null;
			for( String aPage : this.menu.keySet() ) {
				List<MenuEntry> actions = this.menu.get(aPage).actions;
				for( int aAction=0; aAction < actions.size(); aAction++ ) {
					if( actions.get(aAction).cls.getName().equals(namedplugin) ) {
						aClass = actions.get(aAction).cls;
						page = aPage;
						act = aAction+1;
						break;
					}
				}
			}

			AdminPlugin plugin = aClass.newInstance();
			plugin.output(this, page, act);
		}
		catch( Exception e ) {
			addError("Fehler beim Aufruf des Admin-Plugins: "+e);
			e.printStackTrace();
		}
	}

	private void callPlugin(String page, int act) {
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
}

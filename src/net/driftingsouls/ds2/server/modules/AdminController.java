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

import net.driftingsouls.ds2.server.framework.AnnotationUtils;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Controller;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ValidierungException;
import net.driftingsouls.ds2.server.modules.admin.AdminMenuEntry;
import net.driftingsouls.ds2.server.modules.admin.AdminPlugin;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * Der Admin.
 *
 * @author Christopher Jung
 */
@Module(name = "admin")
public class AdminController extends Controller
{
	private static final List<Class<? extends AdminPlugin>> plugins = new ArrayList<>();

	static
	{
		SortedSet<Class<?>> entityClasses = AnnotationUtils.INSTANCE.findeKlassenMitAnnotation(AdminMenuEntry.class);
		for (Class<?> cls : entityClasses)
		{
			final Class<? extends AdminPlugin> adminPluginClass = cls.asSubclass(AdminPlugin.class);
			plugins.add(adminPluginClass);
		}
	}

	private static class MenuEntry implements Comparable<MenuEntry>
	{
		String name;
		SortedSet<MenuEntry> actions = new TreeSet<>();
		Class<? extends AdminPlugin> cls;

		MenuEntry(String name)
		{
			this.name = name;
		}

		@Override
		public int compareTo(MenuEntry o)
		{
			return name.compareTo(o.name);
		}

		@Override
		public boolean equals(Object object)
		{
			if (object == null)
			{
				return false;
			}

			if (object.getClass() != this.getClass())
			{
				return false;
			}

			MenuEntry other = (MenuEntry) object;
			return this.name.equals(other.name);
		}

		@Override
		public int hashCode()
		{
			return this.name.hashCode();
		}
	}

	TreeMap<String, MenuEntry> menu = new TreeMap<>();
	Set<String> validPlugins = new HashSet<>();

	/**
	 * Konstruktor.
	 *
	 * @param context Der zu verwendende Kontext
	 */
	public AdminController(Context context)
	{
		super(context);
	}

	private void addMenuEntry(Class<? extends AdminPlugin> cls, String menuentry, String submenuentry)
	{
		if (!this.menu.containsKey(menuentry))
		{
			this.menu.put(menuentry, new MenuEntry(menuentry));
		}

		MenuEntry entry = new MenuEntry(submenuentry);
		entry.cls = cls;
		this.menu.get(menuentry).actions.add(entry);
	}

	@Override
	protected boolean validateAndPrepare()
	{
		if (!hasPermission("admin", "sichtbar"))
		{
			throw new ValidierungException("Sie sind nicht berechtigt diese Seite aufzurufen");
		}

		for (Class<? extends AdminPlugin> cls : plugins)
		{

			if (validPlugins.contains(cls.getName()))
			{
				continue;
			}
			if (!hasPermission("admin", cls.getSimpleName()))
			{
				continue;
			}
			validPlugins.add(cls.getName());

			AdminMenuEntry adminMenuEntry = cls.getAnnotation(AdminMenuEntry.class);

			addMenuEntry(cls, adminMenuEntry.category(), adminMenuEntry.name());
		}

		return true;
	}

	/**
	 * Fuehrt ein Admin-Plugin aus.
	 *
	 * @param act Die ID der Aktion auf einer Seite
	 * @param page Der Name der Seite
	 * @param namedplugin Der exakte Pluginname falls ein bestimmtes Adminplugin ausgefuehrt werden soll
	 */
	@Action(ActionType.AJAX)
	public void ajaxAction(int act, String page, String namedplugin)
	{
		if (page.length() > 0 || namedplugin.length() > 0)
		{
			if (act > 0)
			{
				callPlugin(page, act);
			}
			else if ((namedplugin.length() > 0) && (validPlugins.contains(namedplugin)))
			{
				callNamedPlugin(namedplugin);
			}
		}
	}

	/**
	 * Zeigt die Gui an und fuehrt ein Admin-Plugin (sofern ausgewaehlt) aus.
	 *
	 * @param act Die ID der Aktion auf einer Seite
	 * @param page Der Name der Seite
	 * @param namedplugin Der exakte Pluginname falls ein bestimmtes Adminplugin ausgefuehrt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public void defaultAction(int act, String page, String namedplugin) throws IOException
	{
		Writer echo = getContext().getResponse().getWriter();
		echo.append("<div id='admin'>\n");
		echo.append("<div class='gfxbox' style='width:900px;text-align:center;margin:0px auto'>\n");
		echo.append("<ui class='menu'>");
		for (MenuEntry entry : this.menu.values())
		{
			echo.append("<li><a class=\"forschinfo\" href=\"./ds?module=admin&page=").append(entry.name).append("\">").append(entry.name).append("</a></li>\n");
		}
		echo.append("</ul>\n");
		echo.append("</div><br />\n");


		if (page.length() > 0 || namedplugin.length() > 0)
		{
			echo.append("<table class=\"noBorder\"><tr><td class=\"noBorder\" valign=\"top\">\n");

			echo.append("<div class='gfxbox' style='width:250px'>");
			echo.append("<table width=\"100%\">\n");
			echo.append("<tr><td align=\"center\">Aktionen:</td></tr>\n");
			if (this.menu.containsKey(page) && (this.menu.get(page).actions.size() > 0))
			{
				SortedSet<MenuEntry> actions = this.menu.get(page).actions;
				int index = 1;
				for (MenuEntry entry : actions)
				{
					echo.append("<tr><td align=\"left\"><a class=\"forschinfo\" href=\"./ds?module=admin&page=" + page + "&act=" + (index++) + "\">" + entry.name + "</a></td></tr>\n");
				}
			}
			echo.append("</table>\n");
			echo.append("</div>");

			echo.append("</td><td class=\"noBorder\" valign=\"top\" width=\"40\">&nbsp;&nbsp;&nbsp;</td>\n");
			echo.append("<td class=\"noBorder\" valign=\"top\">\n");
		}
		if (act > 0)
		{
			callPlugin(page, act);
		}
		else if ((namedplugin.length() > 0) && (validPlugins.contains(namedplugin)))
		{
			callNamedPlugin(namedplugin);
		}

		echo.append("</td></tr></table>");
		echo.append("</div>");
	}

	private void callNamedPlugin(String namedplugin)
	{
		try
		{
			int act = 0;
			String page = "";

			Class<? extends AdminPlugin> aClass = null;
			for (String aPage : this.menu.keySet())
			{
				int index = 1;
				SortedSet<MenuEntry> actions = this.menu.get(aPage).actions;
				for (MenuEntry entry : actions)
				{
					if (entry.cls.getName().equals(namedplugin))
					{
						aClass = entry.cls;
						page = aPage;
						act = index;
						break;
					}
					index++;
				}
			}

			AdminPlugin plugin = aClass.newInstance();
			getContext().autowireBean(plugin);
			plugin.output(this, page, act);
		}
		catch (IOException | RuntimeException e)
		{
			throw new ValidierungException("Fehler beim Aufruf des Admin-Plugins: " + e);
		}
		catch (ReflectiveOperationException e)
		{
			e.printStackTrace();
			throw new ValidierungException("Fehler beim Aufruf des Admin-Plugins: " + e);

		}
	}

	private void callPlugin(String page, int act)
	{
		if (this.menu.containsKey(page) && (this.menu.get(page).actions.size() > 0))
		{
			SortedSet<MenuEntry> actions = this.menu.get(page).actions;
			if (act <= actions.size())
			{
				int index = 1;
				MenuEntry calledEntry = null;
				for (MenuEntry entry : actions)
				{
					if (act == index++)
					{
						calledEntry = entry;
						break;
					}
				}
				Class<? extends AdminPlugin> cls = calledEntry.cls;
				try
				{
					AdminPlugin plugin;
					plugin = cls.newInstance();
					getContext().autowireBean(plugin);
					plugin.output(this, page, act);
				}
				catch (ReflectiveOperationException e)
				{
					throw new ValidierungException("Fehler beim Aufruf des Admin-Plugins: " + e);
				}
				catch (IOException e)
				{
					throw new ValidierungException("Fehler beim Aufruf des Admin-Plugins: " + e);
				}
			}
		}
	}
}

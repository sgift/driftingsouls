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

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.framework.AnnotationUtils;
import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ValidierungException;
import net.driftingsouls.ds2.server.modules.admin.AdminMenuEntry;
import net.driftingsouls.ds2.server.modules.admin.AdminPlugin;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditPlugin8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntitySelectionViewModel;
import net.driftingsouls.ds2.server.modules.admin.editoren.JqGridSortOrder;
import net.driftingsouls.ds2.server.modules.admin.editoren.JqGridTableDataViewModel;
import net.driftingsouls.ds2.server.modules.admin.editoren.JqGridViewModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Der Admin.
 *
 * @author Christopher Jung
 */
@Module(name = "admin")
public class AdminController extends Controller
{
	private static final Logger LOG = LogManager.getLogger(AdminController.class);

	private static final List<Class<?>> plugins = new ArrayList<>();

	static
	{
		SortedSet<Class<?>> entityClasses = AnnotationUtils.INSTANCE.findeKlassenMitAnnotation(AdminMenuEntry.class);
		plugins.addAll(entityClasses.stream().filter(cls -> AdminPlugin.class.isAssignableFrom(cls) || EntityEditor.class.isAssignableFrom(cls)).collect(Collectors.toList()));
	}

	private static class MenuCategory implements Comparable<MenuCategory>
	{
		String name;
		SortedSet<MenuEntry> actions = new TreeSet<>();

		MenuCategory(String name)
		{
			this.name = name;
		}

		@Override
		public int compareTo(@Nonnull MenuCategory o)
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

			MenuCategory other = (MenuCategory) object;
			return this.name.equals(other.name);
		}

		public boolean containsNamedPlugin(String namedPlugin)
		{
			return this.actions.stream().anyMatch((a) -> a.cls.getName().equals(namedPlugin));
		}

		@Override
		public int hashCode()
		{
			return this.name.hashCode();
		}
	}

	private static class MenuEntry implements Comparable<MenuEntry>
	{
		String name;
		Class<?> cls;

		MenuEntry(String name)
		{
			this.name = name;
		}

		@Override
		public int compareTo(@Nonnull MenuEntry o)
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

	private NavigableMap<String, MenuCategory> menu = new TreeMap<>();
	private Set<String> validPlugins = new HashSet<>();

	/**
	 * Konstruktor.
	 *
	 */
	public AdminController()
	{
		super();
	}

	private void addMenuEntry(Class<?> cls, String menuentry, String submenuentry)
	{
		if (!this.menu.containsKey(menuentry))
		{
			this.menu.put(menuentry, new MenuCategory(menuentry));
		}

		MenuEntry entry = new MenuEntry(submenuentry);
		entry.cls = cls;
		this.menu.get(menuentry).actions.add(entry);
	}

	@Override
	protected boolean validateAndPrepare()
	{
		if (!hasPermission(WellKnownAdminPermission.SICHTBAR))
		{
			throw new ValidierungException("Sie sind nicht berechtigt diese Seite aufzurufen");
		}

		for (Class<?> cls : plugins)
		{

			if (validPlugins.contains(cls.getName()))
			{
				continue;
			}
			AdminMenuEntry adminMenuEntry = cls.getAnnotation(AdminMenuEntry.class);
			if (!hasPermission(adminMenuEntry.permission()))
			{
				continue;
			}
			validPlugins.add(cls.getName());

			addMenuEntry(cls, adminMenuEntry.category(), adminMenuEntry.name());
		}

		return true;
	}

	/**
	 * Fuehrt ein Admin-Plugin aus.
	 *
	 * @param namedplugin Der exakte Pluginname falls ein bestimmtes Adminplugin ausgefuehrt werden soll
	 */
	@Action(ActionType.AJAX)
	public String ajaxAction(String namedplugin)
	{
		if ((namedplugin.length() > 0) && (validPlugins.contains(namedplugin)))
		{
			return callNamedPlugin(namedplugin);
		}
		return null;
	}

	@Action(ActionType.BINARY)
	public void binaryAction(String namedplugin)
	{
		if ((namedplugin.length() > 0) && (validPlugins.contains(namedplugin)))
		{
			callNamedPlugin(namedplugin);
		}
	}

	@ViewModel
	public static class EntityPluginOverviewViewModel
	{
		public JqGridViewModel table;
		public EntitySelectionViewModel entitySelection;
	}

	@Action(ActionType.AJAX)
	public EntityPluginOverviewViewModel entityPluginOverviewAction(String namedplugin, int page, int rows)
	{
		if (namedplugin.isEmpty() || !validPlugins.contains(namedplugin))
		{
			throw new ValidierungException("Ungueltiges Plugin: '"+namedplugin+"'");
		}

		try
		{
			AdminPlugin plugin = instantiate(Class.forName(namedplugin));
			if( !(plugin instanceof EditPlugin8) )
			{
				throw new ValidierungException("Fuer dieses Plugin koennen keine Tabellendaten generiert werden: '"+namedplugin+"'");
			}

			EditPlugin8 eplugin = (EditPlugin8)plugin;
			EntityPluginOverviewViewModel model = new EntityPluginOverviewViewModel();
			model.entitySelection = eplugin.generateEntitySelectionViewModel();
			model.table = eplugin.generateEntityTableModel();
			return model;
		}
		catch (ReflectiveOperationException e)
		{
			LOG.warn("Fehler beim Aufruf des Admin-Plugins " + namedplugin, e);
			throw new ValidierungException("Fehler beim Aufruf des Admin-Plugins: " + e);
		}
	}

	@Action(ActionType.AJAX)
	public JqGridTableDataViewModel tableDataAction(String namedplugin, int page, int rows, String sidx, String sord)
	{
		if (namedplugin.isEmpty() || !validPlugins.contains(namedplugin))
		{
			throw new ValidierungException("Ungueltiges Plugin: '"+namedplugin+"'");
		}

		try
		{
			AdminPlugin plugin = instantiate(Class.forName(namedplugin));
			if( !(plugin instanceof EditPlugin8) )
			{
				throw new ValidierungException("Fuer dieses Plugin koennen keine Tabellendaten generiert werden: '"+namedplugin+"'");
			}

			return ((EditPlugin8)plugin).generateTableData(page,
					rows,
					sidx != null && !sidx.isEmpty() ? sidx : null,
					sord != null && !sord.isEmpty() ? JqGridSortOrder.valueOf(sord.toUpperCase()) : null);
		}
		catch (ReflectiveOperationException e)
		{
			LOG.warn("Fehler beim Aufruf des Admin-Plugins " + namedplugin, e);
			throw new ValidierungException("Fehler beim Aufruf des Admin-Plugins: " + e);
		}
	}

	/**
	 * Zeigt die Gui an und fuehrt ein Admin-Plugin (sofern ausgewaehlt) aus.
	 *
	 * @param namedplugin Der exakte Pluginname falls ein bestimmtes Adminplugin ausgefuehrt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public void defaultAction(String namedplugin) throws IOException
	{
		Writer echo = getContext().getResponse().getWriter();
		echo.append("<div id='admin'>\n");
		echo.append("<div class='gfxbox treemenu'>\n");
		echo.append("<ul>");
		for (MenuCategory category : this.menu.values())
		{
			echo.append("<li ").append(category.containsNamedPlugin(namedplugin) ? "class='expanded'" : "").append(">");
			echo.append("<p>").append(category.name);
			echo.append("</p><ul>\n");
			for (MenuEntry entry : category.actions)
			{
				boolean active = entry.cls.getName().equals(namedplugin);
				echo.append("<li>");
				if(EntityEditor.class.isAssignableFrom(entry.cls) )
				{
					echo.append("<img src='data/interface/admin/editor.png' />");
					echo.append("<a class=\"forschinfo ").append(active?"active" : "").append("\" href=\"#\" data-namedplugin=\"").append(entry.cls.getCanonicalName()).append("\" onclick=\"Admin.openEntityEditor('").append(entry.cls.getCanonicalName()).append("');\">").append(entry.name).append("</a></li>\n");
				}
				else
				{
					echo.append("<img src='data/interface/admin/tool.png' />");
					echo.append("<a class=\"forschinfo ").append(active?"active" : "").append("\" href=\"./ds?module=admin&namedplugin=").append(entry.cls.getCanonicalName()).append("\">").append(entry.name).append("</a></li>\n");
				}

			}
			echo.append("</ul></li>");
		}
		echo.append("</ul>\n");
		echo.append("</div><br /><div id=\"adminplugin\">\n");

		try
		{
			if ((namedplugin.length() > 0) && (validPlugins.contains(namedplugin)))
			{
				String content = callNamedPlugin(namedplugin);
				echo.append(content);
			}
		}
		finally
		{
			echo.append("</div></div>");
			echo.append("<script type='text/javascript'>$(document).ready(function() {Admin.initMenu();});</script>");
		}
	}

	private String callNamedPlugin(String namedplugin)
	{
		try
		{
			Class<?> aClass = Class.forName(namedplugin);

			AdminPlugin plugin = instantiate(aClass);
			StringBuilder output = new StringBuilder();
			plugin.output(output);
			return output.toString();
		}
		catch (IOException | RuntimeException | ReflectiveOperationException e)
		{
			LOG.warn("Fehler beim Aufruf des Admin-Plugins "+namedplugin, e);
			throw new ValidierungException("Fehler beim Aufruf des Admin-Plugins: " + e);
		}
	}

	private AdminPlugin instantiate(Class<?> aClass) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		if( AdminPlugin.class.isAssignableFrom(aClass) )
		{
			AdminPlugin plugin = aClass.asSubclass(AdminPlugin.class).getDeclaredConstructor().newInstance();
			getContext().autowireBean(plugin);
			return plugin;
		}
		EntityEditor<?> editor = aClass.asSubclass(EntityEditor.class).getDeclaredConstructor().newInstance();
		EditPlugin8<?> plugin = new EditPlugin8<>(editor);
		getContext().autowireBean(plugin);
		return plugin;
	}
}
